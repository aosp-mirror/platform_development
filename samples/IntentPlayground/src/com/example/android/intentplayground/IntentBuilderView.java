/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.example.android.intentplayground;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Displays options to build an intent with different configurations of flags
 * and target activities, and allows the user to launch an activity with the built intent.
 */
public class IntentBuilderView extends FrameLayout implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "IntentBuilderView";
    protected final int TAG_FLAG = R.id.tag_flag;
    protected final int TAG_SUGGESTED = R.id.tag_suggested;
    protected ComponentName mActivityToLaunch;
    private boolean mVerifyMode;
    private ColorStateList mSuggestTint;
    private ColorStateList mDefaultTint;
    private LinearLayout mLayout;
    private Context mContext;
    private LayoutInflater mInflater;
    private List<RadioButton> mRadioButtons;

    /**
     * Constructs a new IntentBuilderView, in the specified mode.
     *
     * @param context The context of the activity that holds this view.
     * @param mode    The mode to launch in (if null, default mode turns suggestions off). Passing
     *                {@link BaseActivity.Mode} will turn on suggestions
     *                by default.
     */
    public IntentBuilderView(@NonNull Context context, BaseActivity.Mode mode) {
        super(context);
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mLayout = (LinearLayout) mInflater.inflate(R.layout.view_build_intent,
                this /* root */, false /* attachToRoot */);
        addView(mLayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        mActivityToLaunch = new ComponentName(context,
                TaskAffinity1Activity.class);
        mSuggestTint = context.getColorStateList(R.color.suggested_checkbox);
        mDefaultTint = context.getColorStateList(R.color.default_checkbox);
        mVerifyMode = mode != null && mode == BaseActivity.Mode.VERIFY;
        setTag(BaseActivity.BUILDER_VIEW);
        setId(R.id.build_intent_container);
        setBackground(context.getResources().getDrawable(R.drawable.card_background,
                null /*theme*/));
        setupViews();
    }

    private Class<?> getClass(String name) {
        String fullName = mContext.getPackageName().concat(".").concat(name);
        try {
            return Class.forName(fullName);
        } catch (ClassNotFoundException e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void setupViews() {
        PackageInfo packInfo;

        // Retrieve activities and their manifest flags
        PackageManager pm = mContext.getPackageManager();
        try {
            packInfo = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(mContext,
                    "Cannot find activities, this should never happen " + e.toString(),
                    Toast.LENGTH_SHORT).show();
            throw new RuntimeException(e);
        }
        List<ActivityInfo> activities = Arrays.asList(packInfo.activities);
        Map<ActivityInfo, List<String>> activityToFlags = new HashMap<>();
        activities.forEach(activityInfo ->
                activityToFlags.put(activityInfo, FlagUtils.getActivityFlags(activityInfo)));

        // Get handles to views
        LinearLayout flagBuilderLayout = mLayout.findViewById(R.id.build_intent_flags);
        RadioGroup activityRadios = mLayout.findViewById(R.id.radioGroup_launchMode);
        // Populate views with text
        fillCheckBoxLayout(flagBuilderLayout, FlagUtils.intentFlagsByCategory(),
                R.layout.section_header, R.id.header_title, R.layout.checkbox_list_item,
                R.id.checkBox_item);

        // Add radios for activity combos
        List<RadioButton> radioButtons = new ArrayList<>();
        activityToFlags.entrySet().stream()
                .sorted(Comparator.comparing(
                        activityEntry -> nameOfActivityInfo(activityEntry.getKey())))
                .forEach(activityEntry -> {
                    ActivityInfo activityInfo = activityEntry.getKey();
                    List<String> manifestFlags = activityEntry.getValue();

                    LinearLayout actRadio = (LinearLayout) mInflater
                            .inflate(R.layout.activity_radio_list_item, null /* root */);
                    RadioButton rb = actRadio.findViewById(R.id.radio_launchMode);
                    rb.setText(activityInfo.name.substring(activityInfo.name.lastIndexOf('.') + 1));
                    rb.setTag(activityInfo);
                    ((TextView) actRadio.findViewById(R.id.activity_desc)).setText(
                            manifestFlags.stream().collect(Collectors.joining("\n")));
                    rb.setOnClickListener(this);
                    activityRadios.addView(actRadio);
                    radioButtons.add(rb);
                });
        ((CompoundButton) mLayout.findViewById(R.id.suggestion_switch))
                .setOnCheckedChangeListener(this);
        mRadioButtons = radioButtons;
    }


    private String nameOfActivityInfo(ActivityInfo activityInfo) {
        return activityInfo.name.substring(activityInfo.name.lastIndexOf('.') + 1);
    }

    /**
     * Fills the {@link ViewGroup} with a list separated by section
     *
     * @param layout            The layout to fill
     * @param categories        A map of category names to list items within that category
     * @param categoryLayoutRes the layout resource of the category header view
     * @param categoryViewId    the resource id of the category {@link TextView} within the layout
     * @param itemLayoutRes     the layout resource of the list item view
     * @param itemViewId        the resource id of the item {@link TextView} within the item layout
     */
    private void fillCheckBoxLayout(ViewGroup layout, Map<String, List<String>> categories,
            int categoryLayoutRes, int categoryViewId, int itemLayoutRes, int itemViewId) {
        layout.removeAllViews();
        for (String category : categories.keySet()) {
            View categoryLayout = mInflater.inflate(categoryLayoutRes, layout,
                    false /* attachToRoot */);
            TextView categoryView = categoryLayout.findViewById(categoryViewId);
            categoryView.setText(category);
            layout.addView(categoryLayout);
            for (String item : categories.get(category)) {
                View itemLayout = mInflater.inflate(itemLayoutRes, layout,
                        false /* attachToRoot */);
                CheckBox itemView = itemLayout.findViewById(itemViewId);
                IntentFlag flag = FlagUtils.getFlagForString(item);
                itemView.setTag(TAG_FLAG, flag);
                itemView.setText(item);
                itemView.setOnCheckedChangeListener(this);
                layout.addView(itemLayout);
            }
        }
    }

    @Override
    public void onClick(View view) {
        // Handles selection of target activity
        if (view instanceof RadioButton) {
            ActivityInfo tag = (ActivityInfo) view.getTag();
            mActivityToLaunch = new ComponentName(mContext,
                    getClass(tag.name.substring(tag.name.lastIndexOf(".") + 1)));
            mRadioButtons.stream().filter(rb -> rb != view)
                    .forEach(rb -> rb.setChecked(false));
        }
    }

    public Intent currentIntent() {
        LinearLayout flagBuilder = mLayout.findViewById(R.id.build_intent_flags);
        Intent intent = new Intent();
        // Gather flags from flag builder checkbox list
        childrenOfGroup(flagBuilder, CheckBox.class)
                .forEach(checkbox -> {
                    int flagVal = FlagUtils.flagValue(checkbox.getText().toString());
                    if (checkbox.isChecked()) {
                        intent.addFlags(flagVal);
                    } else {
                        intent.removeFlags(flagVal);
                    }
                });
        intent.setComponent(mActivityToLaunch);
        return intent;
    }


    public boolean startForResult() {
        RadioButton startNormal = mLayout.findViewById(R.id.start_normal);
        return !startNormal.isChecked();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        int buttonId = compoundButton.getId();
        if (buttonId == R.id.checkBox_item) {
            // A checkbox was checked/unchecked
            IntentFlag flag = (IntentFlag) compoundButton.getTag(TAG_FLAG);
            if (flag != null && mVerifyMode) {
                refreshConstraints();
                if (checked) {
                    suggestFlags(flag);
                    selectFlags(flag.getRequests());
                } else {
                    clearSuggestions();
                }
            }
        } else if (buttonId == R.id.suggestion_switch) {
            // Suggestions were turned on/off
            clearSuggestions();
            mVerifyMode = checked;
            if (mVerifyMode) {
                refreshConstraints();
                getCheckedFlags().forEach(this::suggestFlags);
            } else {
                enableAllFlags();
            }
        }
    }

    private void refreshConstraints() {
        enableAllFlags();
        getCheckedFlags().forEach(flag -> disableFlags(flag.getConflicts()));
    }

    private void suggestFlags(IntentFlag flag) {
        clearSuggestions();
        List<String> suggestions = flag.getComplements().stream().map(IntentFlag::getName)
                .collect(Collectors.toList());
        getAllCheckBoxes().stream()
                .filter(box -> hasSuggestion(suggestions, box))
                .forEach(box -> {
                    box.setButtonTintList(mSuggestTint);
                    box.setTag(TAG_SUGGESTED, true);
                });
    }

    private boolean hasSuggestion(List<String> suggestions, CheckBox box) {
        IntentFlag flag = (IntentFlag) box.getTag(TAG_FLAG);
        if (flag != null) {
            return suggestions.contains(flag.getName());
        } else {
            Log.w(TAG, "Unknown flag: " + box.getText());
            return false;
        }
    }

    private void clearSuggestions() {
        getAllCheckBoxes().forEach(box -> box.setButtonTintList(mDefaultTint));
    }

    /**
     * Clears all of the checkboxes in this builder.
     */
    public void clearFlags() {
        getAllCheckBoxes().forEach(box -> box.setChecked(false));
    }

    private List<CheckBox> getAllCheckBoxes() {
        View layout = mLayout;
        ViewGroup flagBuilder = (LinearLayout) layout.findViewById(R.id.build_intent_flags);
        List<CheckBox> checkBoxes = new LinkedList<>();
        for (int i = 0; i < flagBuilder.getChildCount(); i++) {
            View child = flagBuilder.getChildAt(i);
            if (child instanceof CheckBox) {
                checkBoxes.add((CheckBox) child);
            }
        }
        return checkBoxes;
    }

    /**
     * Retrieve children of a certain type from a {@link ViewGroup}.
     *
     * @param group the ViewGroup to retrieve children from.
     */
    protected static <T> List<T> childrenOfGroup(ViewGroup group, Class<T> viewType) {
        List<T> list = new LinkedList<>();
        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (viewType.isAssignableFrom(v.getClass())) list.add(viewType.cast(v));
        }
        return list;
    }

    /**
     * Selects the checkboxes for the given list of flags.
     *
     * @param flags A list of mIntent flags to select.
     */
    public void selectFlags(List<String> flags) {
        getAllCheckBoxes().forEach(box -> {
            if (flags.contains(box.getText())) {
                box.setChecked(true);
            }
        });
    }

    /**
     * Selects the checkboxes for the given list of flags.
     *
     * @param flags A list of mIntent flags to select.
     */
    public void selectFlags(Collection<IntentFlag> flags) {
        selectFlags(flags.stream().map(IntentFlag::getName).collect(Collectors.toList()));
    }

    private void enableAllFlags() {
        getAllCheckBoxes().forEach(box -> box.setEnabled(true));
    }

    private Collection<CheckBox> getChecked() {
        return getAllCheckBoxes().stream().filter(CompoundButton::isChecked)
                .collect(Collectors.toList());
    }

    private Collection<IntentFlag> getCheckedFlags() {
        return getChecked().stream().map(checkBox -> (IntentFlag) checkBox.getTag(TAG_FLAG))
                .collect(Collectors.toList());
    }

    private void disableFlags(Collection<IntentFlag> flags) {
        flags.forEach(flag -> getCheckBox(flag).setEnabled(false));
    }

    private CheckBox getCheckBox(IntentFlag flag) {
        return getAllCheckBoxes().stream().filter(box -> flag.getName().equals(box.getText()))
                .findFirst().orElse(null);
    }

    /**
     * A functional interface that represents the action to take upon the user pressing the launch
     * button within this view.
     */
    public interface OnLaunchCallback {
        void launchActivity(Intent intent, boolean forResult);
    }
}
