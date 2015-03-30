/*
 * Copyright 2015 Google Inc. All rights reserved.
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

package com.example.android.xyztouristattractions.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.CardFrame;
import android.support.wearable.view.CardScrollView;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.xyztouristattractions.R;
import com.example.android.xyztouristattractions.common.Attraction;
import com.example.android.xyztouristattractions.common.Constants;
import com.example.android.xyztouristattractions.service.UtilityService;

import java.util.ArrayList;

/**
 * This adapter backs the main GridViewPager component found in
 * {@link com.example.android.xyztouristattractions.ui.AttractionsActivity}.
 */
public class AttractionsGridPagerAdapter extends GridPagerAdapter
        implements GridViewPager.OnPageChangeListener {

    public static final int FADE_IN_TIME_MS = 250;
    public static final int FADE_OUT_TIME_MS = 500;
    private static final int GRID_COLUMN_COUNT = 5;
    private static final int FADE_OUT_DELAY_MS = 1500;
    private static final int PAGER_PRIMARY_IMAGE_COLUMN = 0;
    private static final int PAGER_SECONDARY_IMAGE_COLUMN = 1;
    private static final int PAGER_DESCRIPTION_COLUMN = 2;
    private static final int PAGER_NAVIGATE_ACTION_COLUMN = 3;
    private static final int PAGER_OPEN_ACTION_COLUMN = 4;

    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private ArrayList<Attraction> mAttractions;
    private Rect mInsets = new Rect();
    private DelayedHide mDelayedHide = new DelayedHide();
    private OnChromeFadeListener mOnChromeFadeListener;

    public AttractionsGridPagerAdapter(
            Context context, ArrayList<Attraction> attractions) {
        super();
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
        mAttractions = attractions;
    }

    public void setData(ArrayList<Attraction> attractions) {
        mAttractions = attractions;
    }

    public void setInsets(Rect insets) {
        mInsets = insets;
    }

    @Override
    public int getRowCount() {
        return (mAttractions != null && mAttractions.size() > 0) ? mAttractions.size() : 1;
    }

    @Override
    public int getColumnCount(int i) {
        return GRID_COLUMN_COUNT;
    }

    @Override
    protected Object instantiateItem(ViewGroup container, int row, final int column) {
        if (mAttractions != null && mAttractions.size() > 0) {
            final Attraction attraction = mAttractions.get(row);
            switch (column) {
                case PAGER_PRIMARY_IMAGE_COLUMN:
                case PAGER_SECONDARY_IMAGE_COLUMN:
                    // Two pages of full screen images, one with the attraction name
                    // and one with the distance to the attraction
                    final View view = mLayoutInflater.inflate(
                            R.layout.gridpager_fullscreen_image, container, false);
                    ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
                    TextView textView = (TextView) view.findViewById(R.id.textView);
                    FrameLayout overlayTextLayout =
                            (FrameLayout) view.findViewById(R.id.overlaytext);

                    mDelayedHide.add(overlayTextLayout);
                    view.setOnClickListener(mDelayedHide);

                    FrameLayout.LayoutParams params =
                            (FrameLayout.LayoutParams) textView.getLayoutParams();
                    params.bottomMargin = params.bottomMargin + mInsets.bottom;
                    params.leftMargin = mInsets.left;
                    params.rightMargin = mInsets.right;
                    textView.setLayoutParams(params);

                    if (column == PAGER_PRIMARY_IMAGE_COLUMN) {
                        imageView.setImageBitmap(attraction.image);
                        textView.setText(attraction.name);
                    } else {
                        imageView.setImageBitmap(attraction.secondaryImage);
                        if (TextUtils.isEmpty(attraction.distance)) {
                            overlayTextLayout.setVisibility(View.GONE);
                        } else {
                            textView.setText(mContext.getString(
                                    R.string.map_caption, attraction.distance));
                        }
                    }
                    container.addView(view);
                    return view;
                case PAGER_DESCRIPTION_COLUMN:
                    // The description card page
                    CardScrollView cardScrollView = (CardScrollView) mLayoutInflater.inflate(
                            R.layout.gridpager_card, container, false);
                    TextView descTextView = (TextView) cardScrollView.findViewById(R.id.textView);
                    descTextView.setText(attraction.description);
                    cardScrollView.setCardGravity(Gravity.BOTTOM);
                    cardScrollView.setExpansionEnabled(true);
                    cardScrollView.setExpansionDirection(CardFrame.EXPAND_DOWN);
                    cardScrollView.setExpansionFactor(10);
                    container.addView(cardScrollView);
                    return cardScrollView;
                case PAGER_NAVIGATE_ACTION_COLUMN:
                    // The navigate action
                    final WatchViewStub navStub = (WatchViewStub) mLayoutInflater.inflate(
                            R.layout.gridpager_action, container, false);

                    navStub.setOnClickListener(getStartActionClickListener(
                            attraction, Constants.START_NAVIGATION_PATH,
                            ConfirmationActivity.SUCCESS_ANIMATION));

                    navStub.setOnLayoutInflatedListener(
                            new WatchViewStub.OnLayoutInflatedListener() {
                        @Override
                        public void onLayoutInflated(WatchViewStub watchViewStub) {
                            ImageView imageView = (ImageView) navStub.findViewById(R.id.imageView);
                            imageView.setImageResource(R.drawable.ic_full_directions_walking);
                            TextView textView = (TextView) navStub.findViewById(R.id.textView);
                            textView.setText(R.string.action_navigate);
                        }
                    });

                    container.addView(navStub);
                    return navStub;
                case PAGER_OPEN_ACTION_COLUMN:
                    // The "open on device" action
                    final WatchViewStub openStub = (WatchViewStub) mLayoutInflater.inflate(
                            R.layout.gridpager_action, container, false);

                    openStub.setOnClickListener(getStartActionClickListener(
                            attraction, Constants.START_ATTRACTION_PATH,
                            ConfirmationActivity.OPEN_ON_PHONE_ANIMATION));

                    openStub.setOnLayoutInflatedListener(
                            new WatchViewStub.OnLayoutInflatedListener() {
                        @Override
                        public void onLayoutInflated(WatchViewStub watchViewStub) {
                            ImageView imageView = (ImageView) openStub.findViewById(R.id.imageView);
                            imageView.setImageResource(R.drawable.ic_full_open_on_device);
                            TextView textView = (TextView) openStub.findViewById(R.id.textView);
                            textView.setText(R.string.action_open);
                        }
                    });

                    container.addView(openStub);
                    return openStub;
            }
        }
        return new View(mContext);
    }

    @Override
    public Drawable getBackgroundForPage(int row, int column) {
        if (column == 0) {
            return new ColorDrawable(0); // Empty black drawable
        }
        if (mAttractions.size() > 0 && mAttractions.get(row).image != null) {
            return new BitmapDrawable(mContext.getResources(), mAttractions.get(row).image);
        }
        return super.getBackgroundForPage(row, column);
    }

    @Override
    protected void destroyItem(ViewGroup viewGroup, int row, int column, Object object) {
        mDelayedHide.remove((View) object);
        viewGroup.removeView((View)object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void onPageScrolled(int posX, int posY, float posOffsetX, float posOffsetY,
                               int posOffsetPixelsX, int posOffsetPixelsY) {}

    @Override
    public void onPageSelected(int row, int col) {}

    @Override
    public void onPageScrollStateChanged(int state) {
        mDelayedHide.show();
    }

    /**
     * Use the Wear Message API to execute an action. Clears local and remote notifications and
     * also runs a confirmation animation before finishing the Wear activity.
     *
     * @param attraction The attraction to start the action on
     * @param pathName The Wear Message API pathname
     * @param confirmAnimationType The confirmation animation type from ConfirmationActivity
     */
    private void startAction(Attraction attraction, String pathName, int confirmAnimationType) {
        Intent intent = new Intent(mContext, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, confirmAnimationType);
        mContext.startActivity(intent);

        UtilityService.clearNotification(mContext);
        UtilityService.clearRemoteNotifications(mContext);
        UtilityService.startDeviceActivity(mContext, pathName, attraction.name, attraction.city);

        ((Activity)mContext).finish();
    }

    /**
     * Helper method to generate the OnClickListener for the attraction actions.
     */
    private View.OnClickListener getStartActionClickListener(final Attraction attraction,
            final String pathName, final int confirmAnimationType) {
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAction(attraction, pathName, confirmAnimationType);
            }
        };
        return clickListener;
    }

    public void setOnChromeFadeListener(OnChromeFadeListener listener) {
        mOnChromeFadeListener = listener;
    }

    public interface OnChromeFadeListener {
        abstract void onChromeFadeIn();
        abstract void onChromeFadeOut();
    }

    /**
     * Helper class to fade out views based on a delay and fade them back in if needed as well.
     */
    private class DelayedHide implements View.OnClickListener {

        ArrayList<View> hideViews = new ArrayList<View>(GRID_COLUMN_COUNT);
        Handler mHideHandler = new Handler();
        boolean mIsHidden = false;

        Runnable mHideRunnable = new Runnable() {
            @Override
            public void run() {
                hide();
            }
        };

        void add(View newView) {
            hideViews.add(newView);
            delayedHide();
        }

        void remove(View removeView) {
            hideViews.remove(removeView);
        }

        void show() {
            mIsHidden = false;
            if (mOnChromeFadeListener != null) {
                mOnChromeFadeListener.onChromeFadeIn();
            }
            for (View view : hideViews) {
                if (view != null) {
                    view.animate().alpha(1).setDuration(FADE_IN_TIME_MS).start();
                }
            }
            delayedHide();
        }

        void hide() {
            mIsHidden = true;
            mHideHandler.removeCallbacks(mHideRunnable);
            if (mOnChromeFadeListener != null) {
                mOnChromeFadeListener.onChromeFadeOut();
            }
            for (int i=0; i<hideViews.size(); i++) {
                if (hideViews.get(i) != null) {
                    hideViews.get(i).animate().alpha(0).setDuration(FADE_OUT_TIME_MS).start();
                }
            }
        }

        void delayedHide() {
            mHideHandler.removeCallbacks(mHideRunnable);
            mHideHandler.postDelayed(mHideRunnable, FADE_OUT_DELAY_MS);
        }

        @Override
        public void onClick(View v) {
            if (mIsHidden) {
                show();
            } else {
                hide();
            }
        }
    }
}
