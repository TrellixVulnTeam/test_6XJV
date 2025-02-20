/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.statusbar.phone;

import static com.android.systemui.Interpolators.ALPHA_IN;
import static com.android.systemui.Interpolators.ALPHA_OUT;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;

import android.view.View.AccessibilityDelegate;
import com.android.systemui.plugins.statusbar.phone.NavBarButtonProvider.ButtonInterface;
import com.android.systemui.statusbar.policy.KeyButtonDrawable;

import java.util.ArrayList;
import android.util.Log;

/**
 * Dispatches common view calls to multiple views.  This is used to handle
 * multiples of the same nav bar icon appearing.
 */
public class ButtonDispatcher {
    private final static int FADE_DURATION_IN = 150;
    private final static int FADE_DURATION_OUT = 100;

    private final ArrayList<View> mViews = new ArrayList<>();

    private final int mId;

    private View.OnClickListener mClickListener;
    private View.OnTouchListener mTouchListener;
    private View.OnLongClickListener mLongClickListener;
    private View.OnHoverListener mOnHoverListener;
    private Boolean mLongClickable;
    private Float mAlpha;
    private Float mDarkIntensity;
    private Integer mVisibility = -1;
    private Boolean mDelayTouchFeedback;
    private KeyButtonDrawable mImageDrawable;
    private View mCurrentView;
    private boolean mVertical;
    private ValueAnimator mFadeAnimator;
    private AccessibilityDelegate mAccessibilityDelegate;

    private final ValueAnimator.AnimatorUpdateListener mAlphaListener = animation ->
            setAlpha((float) animation.getAnimatedValue());

    private final AnimatorListenerAdapter mFadeListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            /** UNISOC bug956451*/
            Log.d("ButtonDispatcher", "onAnimationEnd getAlpha:"+getAlpha());
            if(mVisibility != View.INVISIBLE){
                setVisibility( getAlpha()== 1 ? View.VISIBLE : View.INVISIBLE);
            }
        }
    };

    public ButtonDispatcher(int id) {
        mId = id;
    }

    void clear() {
        mViews.clear();
    }

    void addView(View view) {
        mViews.add(view);
        view.setOnClickListener(mClickListener);
        view.setOnTouchListener(mTouchListener);
        view.setOnLongClickListener(mLongClickListener);
        view.setOnHoverListener(mOnHoverListener);
        if (mLongClickable != null) {
            view.setLongClickable(mLongClickable);
        }
        if (mAlpha != null) {
            view.setAlpha(mAlpha);
        }
        if (mVisibility != null && mVisibility != -1) {
            view.setVisibility(mVisibility);
        }
        if (mAccessibilityDelegate != null) {
            view.setAccessibilityDelegate(mAccessibilityDelegate);
        }
        if (view instanceof ButtonInterface) {
            final ButtonInterface button = (ButtonInterface) view;
            if (mDarkIntensity != null) {
                button.setDarkIntensity(mDarkIntensity);
            }
            if (mImageDrawable != null) {
                button.setImageDrawable(mImageDrawable);
            }
            if (mDelayTouchFeedback != null) {
                button.setDelayTouchFeedback(mDelayTouchFeedback);
            }
            button.setVertical(mVertical);
        }
    }

    public int getId() {
        return mId;
    }

    public int getVisibility() {
        return mVisibility != null ? mVisibility : View.VISIBLE;
    }

    public boolean isVisible() {
        return getVisibility() == View.VISIBLE;
    }

    public float getAlpha() {
        return mAlpha != null ? mAlpha : 1;
    }

    public KeyButtonDrawable getImageDrawable() {
        return mImageDrawable;
    }

    public void setImageDrawable(KeyButtonDrawable drawable) {
        mImageDrawable = drawable;
        final int N = mViews.size();
        for (int i = 0; i < N; i++) {
            if (mViews.get(i) instanceof ButtonInterface) {
                ((ButtonInterface) mViews.get(i)).setImageDrawable(mImageDrawable);
            }
        }
    }

    public void setVisibility(int visibility) {
        if (mVisibility == visibility) return;
        mVisibility = visibility;
        final int N = mViews.size();
        for (int i = 0; i < N; i++) {
            mViews.get(i).setVisibility(mVisibility);
        }
    }

    public void abortCurrentGesture() {
        // This seems to be an instantaneous thing, so not going to persist it.
        final int N = mViews.size();
        for (int i = 0; i < N; i++) {
            if (mViews.get(i) instanceof ButtonInterface) {
                ((ButtonInterface) mViews.get(i)).abortCurrentGesture();
            }
        }
    }

    public void setAlpha(float alpha) {
        setAlpha(alpha, false /* animate */);
    }

    public void setAlpha(float alpha, boolean animate) {
        if (animate) {
            if (mFadeAnimator != null) {
                mFadeAnimator.cancel();
            }
            mFadeAnimator = ValueAnimator.ofFloat(getAlpha(), alpha);
            mFadeAnimator.setDuration(getAlpha() < alpha? FADE_DURATION_IN : FADE_DURATION_OUT);
            mFadeAnimator.setInterpolator(getAlpha() < alpha ? ALPHA_IN : ALPHA_OUT);
            mFadeAnimator.addListener(mFadeListener);
            mFadeAnimator.addUpdateListener(mAlphaListener);
            mFadeAnimator.start();
            setVisibility(View.VISIBLE);
        } else {
            mAlpha = alpha;
            final int N = mViews.size();
            for (int i = 0; i < N; i++) {
                mViews.get(i).setAlpha(alpha);
            }
        }
    }

    public void setDarkIntensity(float darkIntensity) {
        mDarkIntensity = darkIntensity;
        final int N = mViews.size();
        for (int i = 0; i < N; i++) {
            if (mViews.get(i) instanceof ButtonInterface) {
                ((ButtonInterface) mViews.get(i)).setDarkIntensity(darkIntensity);
            }
        }
    }

    public void setDelayTouchFeedback(boolean delay) {
        mDelayTouchFeedback = delay;
        final int N = mViews.size();
        for (int i = 0; i < N; i++) {
            if (mViews.get(i) instanceof ButtonInterface) {
                ((ButtonInterface) mViews.get(i)).setDelayTouchFeedback(delay);
            }
        }
    }

    public void setOnClickListener(View.OnClickListener clickListener) {
        mClickListener = clickListener;
        final int N = mViews.size();
        for (int i = 0; i < N; i++) {
            mViews.get(i).setOnClickListener(mClickListener);
        }
    }

    public void setOnTouchListener(View.OnTouchListener touchListener) {
        mTouchListener = touchListener;
        final int N = mViews.size();
        for (int i = 0; i < N; i++) {
            mViews.get(i).setOnTouchListener(mTouchListener);
        }
    }

    public void setLongClickable(boolean isLongClickable) {
        mLongClickable = isLongClickable;
        final int N = mViews.size();
        for (int i = 0; i < N; i++) {
            mViews.get(i).setLongClickable(mLongClickable);
        }
    }

    public void setOnLongClickListener(View.OnLongClickListener longClickListener) {
        mLongClickListener = longClickListener;
        final int N = mViews.size();
        for (int i = 0; i < N; i++) {
            mViews.get(i).setOnLongClickListener(mLongClickListener);
        }
    }

    public void setOnHoverListener(View.OnHoverListener hoverListener) {
        mOnHoverListener = hoverListener;
        final int N = mViews.size();
        for (int i = 0; i < N; i++) {
            mViews.get(i).setOnHoverListener(mOnHoverListener);
        }
    }

    public void setAccessibilityDelegate(AccessibilityDelegate delegate) {
        mAccessibilityDelegate = delegate;
        final int N = mViews.size();
        for (int i = 0; i < N; i++) {
            mViews.get(i).setAccessibilityDelegate(delegate);
        }
    }

    public void setClickable(boolean clickable) {
        abortCurrentGesture();
        final int N = mViews.size();
        for (int i = 0; i < N; i++) {
            mViews.get(i).setClickable(clickable);
        }
    }

    public ArrayList<View> getViews() {
        return mViews;
    }

    public View getCurrentView() {
        return mCurrentView;
    }

    public void setCurrentView(View currentView) {
        mCurrentView = currentView.findViewById(mId);
    }

    public void setVertical(boolean vertical) {
        mVertical = vertical;
        final int N = mViews.size();
        for (int i = 0; i < N; i++) {
            final View view = mViews.get(i);
            if (view instanceof ButtonInterface) {
                ((ButtonInterface) view).setVertical(vertical);
            }
        }
    }
}
