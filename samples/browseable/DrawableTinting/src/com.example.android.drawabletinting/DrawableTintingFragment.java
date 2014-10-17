/*
* Copyright 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.android.drawabletinting;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.example.android.common.logger.Log;

/**
 * Sample that shows tinting of Drawables programmatically and of Drawable resources in XML.
 * Tinting is set on a nine-patch drawable through the "tint" and "tintMode" parameters.
 * A color state list is referenced as the tint color, which  defines colors for different
 * states of a View (for example disabled/enabled, focused, pressed or selected).
 * Programmatically, tinting is applied to a Drawable through its "setColorFilter" method, with
 * a reference to a color and a PorterDuff blend mode. The color and blend mode can be
 * changed from the UI.
 *
 * @see android.graphics.drawable.Drawable#setColorFilter(int, android.graphics.PorterDuff.Mode)
 * @see android.graphics.drawable.Drawable#setTint(android.content.res.ColorStateList, android.graphics.PorterDuff.Mode)
 */
public class DrawableTintingFragment extends Fragment {

    /**
     * String that identifies logging output from this Fragment.
     */
    private static final String TAG = "DrawableTintingFragment";

    /**
     * Image that tinting is applied to programmatically.
     */
    private ImageView mImage;

    /**
     * Seekbar for alpha component of tinting color.
     */
    private SeekBar mAlphaBar;
    /**
     * Seekbar for red component of tinting color.
     */
    private SeekBar mRedBar;
    /**
     * Seekbar for green bar of tinting color.
     */
    private SeekBar mGreenBar;
    /**
     * Seekbar for blue bar of tinting color.
     */
    private SeekBar mBlueBar;

    /**
     * Text label for alpha component seekbar.
     */
    private TextView mAlphaText;
    /**
     * Text label for red component seekbar.
     */
    private TextView mRedText;
    /**
     * Text label for green component seekbar.
     */
    private TextView mGreenText;
    /**
     * Text label for blue component seekbar.
     */
    private TextView mBlueText;

    /**
     * Selector for blend type for color tinting.
     */
    private Spinner mBlendSpinner;

    /**
     * Computed color for tinting of drawable.
     */
    private int mHintColor;

    /**
     * Selected color tinting mode.
     */
    private PorterDuff.Mode mMode;

    /**
     * Identifier for state of blend mod spinner in state bundle.
     */
    private static final String STATE_BLEND = "DRAWABLETINTING_BLEND";
    /**
     * Identifier for state of alpha seek bar in state bundle.
     */
    private static final String STATE_ALPHA = "DRAWABLETINTING_ALPHA";
    /**
     * Identifier for state of red seek bar in state bundle.
     */
    private static final String STATE_RED = "DRAWABLETINTING_RED";
    /**
     * Identifier for state of green seek bar in state bundle.
     */
    private static final String STATE_GREEN = "DRAWABLETINTING_GREEN";
    /**
     * Identifier for state of blue seek bar in state bundle.
     */
    private static final String STATE_BLUE = "DRAWABLETINTING_BLUE";

    /**
     * Available tinting modes. Note that this array must be kept in sync with the
     * <code>blend_modes</code> string array that provides labels for these modes.
     */
    private static final PorterDuff.Mode[] MODES = new PorterDuff.Mode[]{
            PorterDuff.Mode.ADD,
            PorterDuff.Mode.CLEAR,
            PorterDuff.Mode.DARKEN,
            PorterDuff.Mode.DST,
            PorterDuff.Mode.DST_ATOP,
            PorterDuff.Mode.DST_IN,
            PorterDuff.Mode.DST_OUT,
            PorterDuff.Mode.DST_OVER,
            PorterDuff.Mode.LIGHTEN,
            PorterDuff.Mode.MULTIPLY,
            PorterDuff.Mode.OVERLAY,
            PorterDuff.Mode.SCREEN,
            PorterDuff.Mode.SRC,
            PorterDuff.Mode.SRC_ATOP,
            PorterDuff.Mode.SRC_IN,
            PorterDuff.Mode.SRC_OUT,
            PorterDuff.Mode.SRC_OVER,
            PorterDuff.Mode.XOR
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tinting_fragment, null);

        // Set a drawable as the image to display
        mImage = (ImageView) v.findViewById(R.id.image);
        mImage.setImageResource(R.drawable.btn_default_normal_holo);

        // Get text labels and seekbars for the four color components: ARGB
        mAlphaBar = (SeekBar) v.findViewById(R.id.alphaSeek);
        mAlphaText = (TextView) v.findViewById(R.id.alphaText);
        mGreenBar = (SeekBar) v.findViewById(R.id.greenSeek);
        mGreenText = (TextView) v.findViewById(R.id.greenText);
        mRedBar = (SeekBar) v.findViewById(R.id.redSeek);
        mRedText = (TextView) v.findViewById(R.id.redText);
        mBlueText = (TextView) v.findViewById(R.id.blueText);
        mBlueBar = (SeekBar) v.findViewById(R.id.blueSeek);

        // Set a listener to update tinted image when selections have changed
        mAlphaBar.setOnSeekBarChangeListener(mSeekBarListener);
        mRedBar.setOnSeekBarChangeListener(mSeekBarListener);
        mGreenBar.setOnSeekBarChangeListener(mSeekBarListener);
        mBlueBar.setOnSeekBarChangeListener(mSeekBarListener);


        // Set up the spinner for blend mode selection from a string array resource
        mBlendSpinner = (Spinner) v.findViewById(R.id.blendSpinner);
        SpinnerAdapter sa = ArrayAdapter.createFromResource(getActivity(),
                R.array.blend_modes, android.R.layout.simple_spinner_dropdown_item);
        mBlendSpinner.setAdapter(sa);
        // Set a listener to update the tinted image when a blend mode is selected
        mBlendSpinner.setOnItemSelectedListener(mBlendListener);
        // Select the first item
        mBlendSpinner.setSelection(0);
        mMode = MODES[0];

        if (savedInstanceState != null) {
            // Restore the previous state if this fragment has been restored
            mBlendSpinner.setSelection(savedInstanceState.getInt(STATE_BLEND));
            mAlphaBar.setProgress(savedInstanceState.getInt(STATE_ALPHA));
            mRedBar.setProgress(savedInstanceState.getInt(STATE_RED));
            mGreenBar.setProgress(savedInstanceState.getInt(STATE_GREEN));
            mBlueBar.setProgress(savedInstanceState.getInt(STATE_BLUE));
        }

        // Apply the default blend mode and color
        updateTint(getColor(), getTintMode());

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "state saved.");
        outState.putInt(STATE_BLEND, mBlendSpinner.getSelectedItemPosition());
        outState.putInt(STATE_ALPHA, mAlphaBar.getProgress());
        outState.putInt(STATE_RED, mRedBar.getProgress());
        outState.putInt(STATE_GREEN, mGreenBar.getProgress());
        outState.putInt(STATE_BLUE, mBlueBar.getProgress());
    }

    /**
     * Computes the {@link Color} value from selection on ARGB sliders.
     *
     * @return color computed from selected ARGB values
     */
    public int getColor() {
        final int alpha = mAlphaBar.getProgress();
        final int red = mRedBar.getProgress();
        final int green = mGreenBar.getProgress();
        final int blue = mBlueBar.getProgress();

        return Color.argb(alpha, red, green, blue);
    }

    /**
     * Returns the {@link android.graphics.PorterDuff.Mode} for the selected tint mode option.
     *
     * @return selected tint mode
     */
    public PorterDuff.Mode getTintMode() {
        return MODES[mBlendSpinner.getSelectedItemPosition()];
    }

    /**
     * Update the tint of the image with the color set in the seekbars and selected blend mode.
     * The seekbars are set to a maximum of 255, with one for each of the four components of the
     * ARGB color. (Alpha, Red, Green, Blue.) Once a color has been computed using
     * {@link Color#argb(int, int, int, int)}, it is set togethe with the blend mode on the background
     * image using
     * {@link android.widget.ImageView#setColorFilter(int, android.graphics.PorterDuff.Mode)}.
     */
    public void updateTint(int color, PorterDuff.Mode mode) {
        // Set the color hint of the image: ARGB
        mHintColor = color;

        // Set the color tint mode based on the selection of the Spinner
        mMode = mode;

        // Log selection
        Log.d(TAG, String.format("Updating tint with color [ARGB: %d,%d,%d,%d] and mode [%s]",
                Color.alpha(color), Color.red(color), Color.green(color), Color.blue(color),
                mode.toString()));

        // Apply the color tint for the selected tint mode
        mImage.setColorFilter(mHintColor, mMode);

        // Update the text for each label with the value of each channel
        mAlphaText.setText(getString(R.string.value_alpha, Color.alpha(color)));
        mRedText.setText(getString(R.string.value_red, Color.red(color)));
        mGreenText.setText(getString(R.string.value_green, Color.green(color)));
        mBlueText.setText(getString(R.string.value_blue, Color.blue(color)));
    }

    /**
     * Listener that updates the tint when a blend mode is selected.
     */
    private AdapterView.OnItemSelectedListener mBlendListener =
            new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    // Selected a blend mode and update the tint of image
                    updateTint(getColor(), getTintMode());
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }

            };

    /**
     * Seekbar listener that updates the tinted color when the progress bar has changed.
     */
    private SeekBar.OnSeekBarChangeListener mSeekBarListener =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    // Update the tinted color from all selections in the UI
                    updateTint(getColor(), getTintMode());
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            };
}
