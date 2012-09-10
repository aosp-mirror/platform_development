/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.example.android.apis.app;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import com.example.android.apis.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Presentation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * <h3>Presentation Activity</h3>
 *
 * <p>
 * This demonstrates how to create an activity that shows some content
 * on a secondary display using a {@link Presentation}.
 * </p><p>
 * The activity automatically creates and displays a {@link Presentation} whenever
 * a new non-default display is detected.  You can try this out by using
 * the "Simulate secondary displays" feature in Development Settings to create
 * a few simulated secondary displays.
 * </p>
 */
public class PresentationActivity extends Activity
        implements OnCheckedChangeListener, OnClickListener {
    private final String TAG = "PresentationActivity";

    // The content that we want to show on the presentation.
    private static final int[] CHANNELS = new int[] {
        R.drawable.sample_4, R.drawable.frantic, R.drawable.beach,
        R.drawable.photo1, R.drawable.photo2, R.drawable.photo3,
        R.drawable.photo4, R.drawable.photo5, R.drawable.photo6,
    };

    private DisplayManager mDisplayManager;
    private DisplayListAdapter mDisplayListAdapter;
    private ListView mListView;
    private int mNextChannelNumber;

    // All active presentations indexed by display id.
    private final SparseArray<DemoPresentation> mActivePresentations =
            new SparseArray<DemoPresentation>();

    /**
     * Initialization of the Activity after it is first created.  Must at least
     * call {@link android.app.Activity#setContentView setContentView()} to
     * describe what is to be displayed in the screen.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);

        // Get the display manager service.
        mDisplayManager = (DisplayManager)getSystemService(Context.DISPLAY_SERVICE);

        // See assets/res/any/layout/presentation_activity.xml for this
        // view layout definition, which is being set here as
        // the content of our screen.
        setContentView(R.layout.presentation_activity);

        mDisplayListAdapter = new DisplayListAdapter(this);
        mListView = (ListView)findViewById(R.id.display_list);
        mListView.setAdapter(mDisplayListAdapter);
    }

    @Override
    protected void onResume() {
        // Be sure to call the super class.
        super.onResume();

        // Update our list of displays on resume.
        mDisplayListAdapter.updateContents();

        // Register to receive events from the display manager.
        mDisplayManager.registerDisplayListener(mDisplayListener, null);
    }

    @Override
    protected void onPause() {
        // Be sure to call the super class.
        super.onPause();

        // Unregister from the display manager.
        mDisplayManager.unregisterDisplayListener(mDisplayListener);

        // Dismiss all of our presentations.
        Log.d(TAG, "Activity is being paused.  Dimissing all active presentation.");
        for (int i = 0; i < mActivePresentations.size(); i++) {
            Presentation presentation = mActivePresentations.valueAt(i);
            presentation.dismiss();
        }
        mActivePresentations.clear();
    }

    /**
     * Shows a {@link Presentation} on the specified display.
     */
    private void showPresentation(Display display) {
        // Do nothing if there is already a presentation showing on the display.
        if (mActivePresentations.get(display.getDisplayId()) != null) {
            return;
        }

        Log.d(TAG, "Showing presentation on display " + display.getDisplayId() + ".");

        int channelNumber = mNextChannelNumber;
        mNextChannelNumber = (mNextChannelNumber + 1) % CHANNELS.length;

        final DemoPresentation presentation = new DemoPresentation(this, display, channelNumber);
        mActivePresentations.put(display.getDisplayId(), presentation);
        presentation.show();
        presentation.setOnDismissListener(mOnDismissListener);
    }

    /**
     * Hides a {@link Presentation} on the specified display.
     */
    private void hidePresentation(Display display) {
        final int displayId = display.getDisplayId();
        DemoPresentation presentation = mActivePresentations.get(displayId);
        if (presentation != null) {
            Log.d(TAG, "Dimissing presentation on display " + displayId + ".");
            presentation.dismiss();
            // presentation will be removed from mActivePresentations in onDismiss().
            return;
        }
    }

    @Override
    /** Presentation CheckBox */
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final Display display = (Display)buttonView.getTag();
        if (isChecked) {
            showPresentation(display);
        } else {
            hidePresentation(display);
        }
    }

    @Override
    /** Info Button */
    public void onClick(View v) {
        Context context = v.getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final Display display = (Display)v.getTag();
        Resources r = context.getResources();
        AlertDialog alert = builder
                .setTitle(r.getString(
                        R.string.presentation_alert_info_text, display.getDisplayId()))
                .setMessage(display.toString())
                .setNeutralButton(R.string.presentation_alert_dismiss_text,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                    })
                .create();
        alert.show();
    }

    /**
     * Listens for displays to be added, changed or removed.
     * We use it to update the list and show a new {@link Presentation} when a
     * display is connected.
     *
     * Note that we don't bother dismissing the {@link Presentation} when a
     * display is removed, although we could.  The presentation API takes care
     * of doing that automatically for us.
     */
    private final DisplayManager.DisplayListener mDisplayListener =
            new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
            Log.d(TAG, "Display " + displayId + " added.");
            mDisplayListAdapter.updateContents();

            Display display = mDisplayManager.getDisplay(displayId);
            if (display != null) {
                showPresentation(display);
            }
        }

        @Override
        public void onDisplayChanged(int displayId) {
            Log.d(TAG, "Display " + displayId + " changed.");
            mDisplayListAdapter.updateContents();
        }

        @Override
        public void onDisplayRemoved(int displayId) {
            Log.d(TAG, "Display " + displayId + " removed.");
            mDisplayListAdapter.updateContents();
        }
    };

    /**
     * Listens for when presentations are dismissed.
     */
    private final DialogInterface.OnDismissListener mOnDismissListener =
            new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
            DemoPresentation presentation = (DemoPresentation)dialog;
            int displayId = presentation.getDisplay().getDisplayId();
            Log.d(TAG, "Presentation on display " + displayId + " was dismissed.");
            mActivePresentations.remove(displayId);

            // Uncheck the checkbox once removed.
            final int numChildren = mListView.getChildCount();
            for (int i = 0; i < numChildren; i++) {
                CheckBox cb =
                        (CheckBox)mListView.getChildAt(i).findViewById(R.id.checkbox_presentation);
                Display cbDisplay = (Display)cb.getTag();
                if (cbDisplay.getDisplayId() == displayId) {
                    cb.setChecked(false);
                    break;
                }
            }
        }
    };

    /**
     * List adapter.
     * Shows information about all displays.
     */
    private final class DisplayListAdapter extends ArrayAdapter<Display> {
        final Context mContext;

        public DisplayListAdapter(Context context) {
            super(context, R.layout.presentation_list_item);
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View v;
            if (convertView == null) {
                v = ((Activity) mContext).getLayoutInflater().inflate(
                        R.layout.presentation_list_item, null);
            } else {
                v = convertView;
            }

            final Display display = getItem(position);

            CheckBox cb = (CheckBox)v.findViewById(R.id.checkbox_presentation);
            cb.setTag(display);
            cb.setOnCheckedChangeListener(PresentationActivity.this);
            onCheckedChanged(cb, cb.isChecked());

            TextView tv = (TextView)v.findViewById(R.id.display_id);
            tv.setText(v.getContext().getResources().getString(
                    R.string.presentation_display_id_text, display.getDisplayId()));

            Button b = (Button)v.findViewById(R.id.info);
            b.setTag(display);
            b.setOnClickListener(PresentationActivity.this);

            return v;
        }

        /**
         * Update the contents of the display list adapter to show
         * information about all current displays.
         */
        public void updateContents() {
            clear();

            Display[] displays = mDisplayManager.getDisplays();
            addAll(displays);

            Log.d(TAG, "There are currently " + displays.length + " displays connected.");
            for (Display display : displays) {
                Log.d(TAG, "  " + display);
            }
        }
    }

    /**
     * The presentation to show on the secondary display.
     *
     * Note that this display may have different metrics from the display on which
     * the main activity is showing so we must be careful to use the presentation's
     * own {@link Context} whenever we load resources.
     */
    private final class DemoPresentation extends Presentation {
        // Specifies the content that we want to show in this presentation.
        private final int mChannelNumber;

        public DemoPresentation(Context context, Display display, int channelNumber) {
            super(context, display);
            mChannelNumber = channelNumber;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            // Be sure to call the super class.
            super.onCreate(savedInstanceState);

            // Get the resources for the context of the presentation.
            // Notice that we are getting the resources from the context of the presentation.
            Resources r = getContext().getResources();

            // Inflate the layout.
            setContentView(R.layout.presentation_content);

            // Show a text message to describe what's going on.
            TextView text = (TextView)findViewById(R.id.text);
            text.setText(r.getString(R.string.presentation_channel_text, mChannelNumber + 1,
                    getDisplay().getDisplayId()));

            // Show a n image for visual interest.
            ImageView image = (ImageView)findViewById(R.id.image);
            image.setImageDrawable(r.getDrawable(CHANNELS[mChannelNumber]));

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);

            // Set the background to a random gradient.
            Point p = new Point();
            getDisplay().getSize(p);
            drawable.setGradientRadius(Math.max(p.x, p.y) / 2);
            drawable.setColors(new int[] {
                ((int) (Math.random() * Integer.MAX_VALUE)) | 0xFF000000,
                ((int) (Math.random() * Integer.MAX_VALUE)) | 0xFF000000
            });
            findViewById(android.R.id.content).setBackground(drawable);
        }
    }
}
