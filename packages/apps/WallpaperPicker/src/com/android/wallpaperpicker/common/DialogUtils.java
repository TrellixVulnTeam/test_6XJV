package com.android.wallpaperpicker.common;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.android.wallpaperpicker.FeatureOption;
import com.android.wallpaperpicker.R;
import com.android.wallpaperpicker.WallpaperUtils;

/**
 * Utility class used to show dialogs for things like picking which wallpaper to set.
 */
public class DialogUtils {

    private static final String TAG = "DialogUtils";

    /**
     * Calls cropTask.execute(), once the user has selected which wallpaper to set. On pre-N
     * devices, the prompt is not displayed since there is no API to set the lockscreen wallpaper.
     *
     * TODO: Don't use CropAndSetWallpaperTask on N+, because the new API will handle cropping instead.
     */
    public static void executeCropTaskAfterPrompt(
            Context context, final AsyncTask<Integer, ?, ?> cropTask,
            DialogInterface.OnCancelListener onCancelListener) {
        if (FeatureOption.SPRD_ENABLE_LOCK_WALLPAPER && Utilities.ATLEAST_NOUGAT) {
            AlertDialog dlg = new AlertDialog.Builder(context)
                    .setTitle(R.string.wallpaper_instructions)
                    .setItems(R.array.which_wallpaper_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedItemIndex) {
                        if (Utilities.DEBUG) {
                            Log.d(TAG, "executeCropTaskAfterPrompt: selectedItemIndex = " + selectedItemIndex);
                        }
                        int whichWallpaper;
                        if (selectedItemIndex == 0) {
                            whichWallpaper = WallpaperManagerCompat.FLAG_SET_SYSTEM;
                        } else if (selectedItemIndex == 1) {
                            whichWallpaper = WallpaperManagerCompat.FLAG_SET_LOCK;
                        } else {
                            whichWallpaper = WallpaperManagerCompat.FLAG_SET_SYSTEM
                                    | WallpaperManagerCompat.FLAG_SET_LOCK;
                        }
                        cropTask.execute(whichWallpaper);
                    }
                })
                .setOnCancelListener(onCancelListener)
                .create();

            final Resources res = dlg.getContext().getResources();
            final ListView listPanel = dlg.getListView();
            listPanel.post(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < listPanel.getChildCount(); i++) {
                        View v = listPanel.getChildAt(i);
                        if (v instanceof TextView) {
                            ((TextView) v).setGravity(WallpaperUtils.getGravity(res));
                        }
                    }
                }
            });
            dlg.show();
        } else {
            if (Utilities.DEBUG) {
                Log.d(TAG, "executeCropTaskAfterPrompt");
            }
            // The FLAG_SET_SYSTEM param will result to migrating the system wallpaper file to the
            // lock wallpaper file when lock wallpaper file don't exist.
            // So make the param to be FLAG_SET_SYSTEM | FLAG_SET_LOCK here.
            cropTask.execute(WallpaperManagerCompat.FLAG_SET_SYSTEM
                    | WallpaperManagerCompat.FLAG_SET_LOCK);
        }
    }
}
