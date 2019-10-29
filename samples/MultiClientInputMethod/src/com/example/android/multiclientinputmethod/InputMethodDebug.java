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

package com.example.android.multiclientinputmethod;

import android.view.WindowManager;

import com.android.internal.inputmethod.StartInputFlags;

import java.util.StringJoiner;

/**
 * Provides useful methods for debugging.
 */
final class InputMethodDebug {

    /**
     * Not intended to be instantiated.
     */
    private InputMethodDebug() {
    }

    /**
     * Converts soft input flags to {@link String} for debug logging.
     *
     * @param softInputMode integer constant for soft input flags.
     * @return {@link String} message corresponds for the given {@code softInputMode}.
     */
    public static String softInputModeToString(int softInputMode) {
        final StringJoiner joiner = new StringJoiner("|");
        final int state = softInputMode & WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE;
        final int adjust = softInputMode & WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST;
        final boolean isForwardNav =
                (softInputMode & WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION) != 0;

        switch (state) {
            case WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED:
                joiner.add("STATE_UNSPECIFIED");
                break;
            case WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED:
                joiner.add("STATE_UNCHANGED");
                break;
            case WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN:
                joiner.add("STATE_HIDDEN");
                break;
            case WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN:
                joiner.add("STATE_ALWAYS_HIDDEN");
                break;
            case WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE:
                joiner.add("STATE_VISIBLE");
                break;
            case WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE:
                joiner.add("STATE_ALWAYS_VISIBLE");
                break;
            default:
                joiner.add("STATE_UNKNOWN(" + state + ")");
                break;
        }

        switch (adjust) {
            case WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED:
                joiner.add("ADJUST_UNSPECIFIED");
                break;
            case WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE:
                joiner.add("ADJUST_RESIZE");
                break;
            case WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN:
                joiner.add("ADJUST_PAN");
                break;
            case WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING:
                joiner.add("ADJUST_NOTHING");
                break;
            default:
                joiner.add("ADJUST_UNKNOWN(" + adjust + ")");
                break;
        }

        if (isForwardNav) {
            // This is a special bit that is set by the system only during the window navigation.
            joiner.add("IS_FORWARD_NAVIGATION");
        }

        return joiner.setEmptyValue("(none)").toString();
    }

    /**
     * Converts start input flags to {@link String} for debug logging.
     *
     * @param startInputFlags integer constant for start input flags.
     * @return {@link String} message corresponds for the given {@code startInputFlags}.
     */
    public static String startInputFlagsToString(int startInputFlags) {
        final StringJoiner joiner = new StringJoiner("|");
        if ((startInputFlags & StartInputFlags.VIEW_HAS_FOCUS) != 0) {
            joiner.add("VIEW_HAS_FOCUS");
        }
        if ((startInputFlags & StartInputFlags.IS_TEXT_EDITOR) != 0) {
            joiner.add("IS_TEXT_EDITOR");
        }
        if ((startInputFlags & StartInputFlags.INITIAL_CONNECTION) != 0) {
            joiner.add("INITIAL_CONNECTION");
        }

        return joiner.setEmptyValue("(none)").toString();
    }
}
