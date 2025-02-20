/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.dashboard;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Settings.SimSettingsActivity;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerListHelper;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.Indexable;
import com.android.settings.Utils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.SettingsDrawerActivity;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.drawer.TileUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base fragment for dashboard style UI containing a list of static and dynamic setting items.
 */
public abstract class DashboardFragment extends SettingsPreferenceFragment
        implements SettingsDrawerActivity.CategoryListener, Indexable,
        SummaryLoader.SummaryConsumer {
    private static final String TAG = "DashboardFragment";

    private final Map<Class, List<AbstractPreferenceController>> mPreferenceControllers =
            new ArrayMap<>();
    private final Set<String> mDashboardTilePrefKeys = new ArraySet<>();

    private DashboardFeatureProvider mDashboardFeatureProvider;
    private DashboardTilePlaceholderPreferenceController mPlaceholderPreferenceController;
    private boolean mListeningToCategoryChange;
    private SummaryLoader mSummaryLoader;
    private boolean isEnableWifiDisplay = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mDashboardFeatureProvider = FeatureFactory.getFactory(context).
                getDashboardFeatureProvider(context);
        isEnableWifiDisplay = context.getResources().getBoolean(com.android.internal.R.bool.config_enableWifiDisplay);
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        // Load preference controllers from code
        final List<AbstractPreferenceController> controllersFromCode =
                createPreferenceControllers(context);
        // Load preference controllers from xml definition
        final List<BasePreferenceController> controllersFromXml = PreferenceControllerListHelper
                .getPreferenceControllersFromXml(context, getPreferenceScreenResId());
        // Filter xml-based controllers in case a similar controller is created from code already.
        final List<BasePreferenceController> uniqueControllerFromXml =
                PreferenceControllerListHelper.filterControllers(
                        controllersFromXml, controllersFromCode);

        // Add unique controllers to list.
        if (controllersFromCode != null) {
            controllers.addAll(controllersFromCode);
        }
        controllers.addAll(uniqueControllerFromXml);

        // And wire up with lifecycle.
        final Lifecycle lifecycle = getLifecycle();
        uniqueControllerFromXml
                .stream()
                .filter(controller -> controller instanceof LifecycleObserver)
                .forEach(
                        controller -> lifecycle.addObserver((LifecycleObserver) controller));

        mPlaceholderPreferenceController =
                new DashboardTilePlaceholderPreferenceController(context);
        controllers.add(mPlaceholderPreferenceController);
        for (AbstractPreferenceController controller : controllers) {
            addPreferenceController(controller);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // Set ComparisonCallback so we get better animation when list changes.
        getPreferenceManager().setPreferenceComparisonCallback(
                new PreferenceManager.SimplePreferenceComparisonCallback());
        if (icicle != null) {
            // Upon rotation configuration change we need to update preference states before any
            // editing dialog is recreated (that would happen before onResume is called).
            updatePreferenceStates();
        }
    }

    @Override
    public void onCategoriesChanged() {
        final DashboardCategory category =
                mDashboardFeatureProvider.getTilesForCategory(getCategoryKey());
        if (category == null) {
            return;
        }
        refreshDashboardTiles(getLogTag());
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        refreshAllPreferences(getLogTag());
    }

    @Override
    public void onStart() {
        super.onStart();
        final DashboardCategory category =
                mDashboardFeatureProvider.getTilesForCategory(getCategoryKey());
        if (category == null) {
            return;
        }
        if (mSummaryLoader != null) {
            // SummaryLoader can be null when there is no dynamic tiles.
            mSummaryLoader.setListening(true);
        }
        final Activity activity = getActivity();
        if (activity instanceof SettingsDrawerActivity) {
            mListeningToCategoryChange = true;
            ((SettingsDrawerActivity) activity).addCategoryListener(this);
        }
    }

    @Override
    public void notifySummaryChanged(Tile tile) {
        final String key = mDashboardFeatureProvider.getDashboardKeyForTile(tile);
        final Preference pref = getPreferenceScreen().findPreference(key);
        if (pref == null) {
            Log.d(getLogTag(),
                    String.format("Can't find pref by key %s, skipping update summary %s/%s",
                            key, tile.title, tile.summary));
            return;
        }
        pref.setSummary(tile.summary);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferenceStates();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        Collection<List<AbstractPreferenceController>> controllers =
                mPreferenceControllers.values();
        // If preference contains intent, log it before handling.
        mMetricsFeatureProvider.logDashboardStartIntent(
                getContext(), preference.getIntent(), getMetricsCategory());
        // Give all controllers a chance to handle click.
        for (List<AbstractPreferenceController> controllerList : controllers) {
            for (AbstractPreferenceController controller : controllerList) {
                if (controller.handlePreferenceTreeClick(preference)) {
                    return true;
                }
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mSummaryLoader != null) {
            // SummaryLoader can be null when there is no dynamic tiles.
            mSummaryLoader.setListening(false);
        }
        if (mListeningToCategoryChange) {
            final Activity activity = getActivity();
            if (activity instanceof SettingsDrawerActivity) {
                ((SettingsDrawerActivity) activity).remCategoryListener(this);
            }
            mListeningToCategoryChange = false;
        }
    }

    @Override
    protected abstract int getPreferenceScreenResId();

    protected <T extends AbstractPreferenceController> T use(Class<T> clazz) {
        List<AbstractPreferenceController> controllerList = mPreferenceControllers.get(clazz);
        if (controllerList != null) {
            if (controllerList.size() > 1) {
                Log.w(TAG, "Multiple controllers of Class " + clazz.getSimpleName()
                        + " found, returning first one.");
            }
            return (T) controllerList.get(0);
        }

        return null;
    }

    protected void addPreferenceController(AbstractPreferenceController controller) {
        if (mPreferenceControllers.get(controller.getClass()) == null) {
            mPreferenceControllers.put(controller.getClass(), new ArrayList<>());
        }
        mPreferenceControllers.get(controller.getClass()).add(controller);
    }

    /**
     * Returns the CategoryKey for loading {@link DashboardCategory} for this fragment.
     */
    @VisibleForTesting
    public String getCategoryKey() {
        return DashboardFragmentRegistry.PARENT_TO_CATEGORY_KEY_MAP.get(getClass().getName());
    }

    /**
     * Get the tag string for logging.
     */
    protected abstract String getLogTag();

    /**
     * Get a list of {@link AbstractPreferenceController} for this fragment.
     */
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return null;
    }

    /**
     * Returns true if this tile is wifiDisplay
     */
    protected boolean isContainsWifiDisplay(String key) {
        if (key != null) {
            if (key.contains("WifiDisplaySettingsActivity")) {
                Log.d(TAG, "isContainsWifiDisplay true");
                return true;
            }
        }
        return false;
    }


    /**
     * Returns true if this tile should be displayed
     */
    protected boolean displayTile(Tile tile) {
        return true;
    }

    @VisibleForTesting
    boolean tintTileIcon(Tile tile) {
        if (tile.icon == null) {
            return false;
        }
        // First check if the tile has set the icon tintable metadata.
        final Bundle metadata = tile.metaData;
        if (metadata != null
                && metadata.containsKey(TileUtils.META_DATA_PREFERENCE_ICON_TINTABLE)) {
            return metadata.getBoolean(TileUtils.META_DATA_PREFERENCE_ICON_TINTABLE);
        }
        final String pkgName = getContext().getPackageName();
        // If this drawable is coming from outside Settings, tint it to match the color.
        return pkgName != null && tile.intent != null
                && !pkgName.equals(tile.intent.getComponent().getPackageName());
    }

    /**
     * Displays resource based tiles.
     */
    private void displayResourceTiles() {
        final int resId = getPreferenceScreenResId();
        if (resId <= 0) {
            return;
        }
        addPreferencesFromResource(resId);
        final PreferenceScreen screen = getPreferenceScreen();
        mPreferenceControllers.values().stream().flatMap(Collection::stream).forEach(
                controller -> controller.displayPreference(screen));
    }

    /**
     * Update state of each preference managed by PreferenceController.
     */
    protected void updatePreferenceStates() {
        final PreferenceScreen screen = getPreferenceScreen();
        Collection<List<AbstractPreferenceController>> controllerLists =
                mPreferenceControllers.values();
        for (List<AbstractPreferenceController> controllerList : controllerLists) {
            for (AbstractPreferenceController controller : controllerList) {
                if (!controller.isAvailable()) {
                    continue;
                }
                final String key = controller.getPreferenceKey();

                final Preference preference = screen.findPreference(key);
                if (preference == null) {
                    Log.d(TAG, String.format("Cannot find preference with key %s in Controller %s",
                            key, controller.getClass().getSimpleName()));
                    continue;
                }
                controller.updateState(preference);
            }
        }
    }

    /**
     * Refresh all preference items, including both static prefs from xml, and dynamic items from
     * DashboardCategory.
     */
    private void refreshAllPreferences(final String TAG) {
        // First remove old preferences.
        if (getPreferenceScreen() != null) {
            // Intentionally do not cache PreferenceScreen because it will be recreated later.
            getPreferenceScreen().removeAll();
        }

        // Add resource based tiles.
        displayResourceTiles();

        refreshDashboardTiles(TAG);
    }

    /**
     * Refresh preference items backed by DashboardCategory.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    void refreshDashboardTiles(final String TAG) {
        final PreferenceScreen screen = getPreferenceScreen();

        final DashboardCategory category =
                mDashboardFeatureProvider.getTilesForCategory(getCategoryKey());
        if (category == null) {
            Log.d(TAG, "NO dashboard tiles for " + TAG);
            return;
        }
        final List<Tile> tiles = category.getTiles();
        if (tiles == null) {
            Log.d(TAG, "tile list is empty, skipping category " + category.title);
            return;
        }
        // Create a list to track which tiles are to be removed.
        final List<String> remove = new ArrayList<>(mDashboardTilePrefKeys);

        // There are dashboard tiles, so we need to install SummaryLoader.
        if (mSummaryLoader != null) {
            mSummaryLoader.release();
        }
        final Context context = getContext();
        mSummaryLoader = new SummaryLoader(getActivity(), getCategoryKey());
        mSummaryLoader.setSummaryConsumer(this);
        final TypedArray a = context.obtainStyledAttributes(new int[] {
                android.R.attr.colorControlNormal});
        final int tintColor = a.getColor(0, context.getColor(android.R.color.white));
        a.recycle();
        // UNISOC: Bug 968965 Hide SimSettings according to userId.
        final boolean isOwner = (UserHandle.myUserId() == UserHandle.USER_OWNER);
        // Install dashboard tiles.
        int size = tiles.size();
        for (int i = 0; i < size; i++) {
            Tile tile = tiles.get(i);
            final String key = mDashboardFeatureProvider.getDashboardKeyForTile(tile);
            if (TextUtils.isEmpty(key)) {
                Log.d(TAG, "tile does not contain a key, skipping " + tile);
                continue;
            }
            /* UNISOC: Bug 968965 Hide SimSettings according to userId. @{ */
            if (key.endsWith(SimSettingsActivity.class.getSimpleName()) && !isOwner) {
                continue;
            }
            /* @} */

            Log.d(TAG, "isEnableWifiDisplay = " + isEnableWifiDisplay + "; isContainsWifiDisplay(key) = " + isContainsWifiDisplay(key));
            if (!isEnableWifiDisplay || Utils.WCN_DISABLED) {
                if (isContainsWifiDisplay(key)) {
                    continue;
                }
            }

            if (!displayTile(tile)) {
                continue;
            }
            if (tintTileIcon(tile)) {
                tile.icon.setTint(tintColor);
            }
            if (mDashboardTilePrefKeys.contains(key)) {
                // Have the key already, will rebind.
                final Preference preference = screen.findPreference(key);
                mDashboardFeatureProvider.bindPreferenceToTile(getActivity(), getMetricsCategory(),
                        preference, tile, key, mPlaceholderPreferenceController.getOrder());
            } else {
                // Don't have this key, add it.
                final Preference pref = new Preference(getPrefContext());
                mDashboardFeatureProvider.bindPreferenceToTile(getActivity(), getMetricsCategory(),
                        pref, tile, key, mPlaceholderPreferenceController.getOrder());
                screen.addPreference(pref);
                mDashboardTilePrefKeys.add(key);
            }
            remove.remove(key);
        }
        // Finally remove tiles that are gone.
        for (String key : remove) {
            mDashboardTilePrefKeys.remove(key);
            final Preference preference = screen.findPreference(key);
            if (preference != null) {
                screen.removePreference(preference);
            }
        }
        mSummaryLoader.setListening(true);
    }
}
