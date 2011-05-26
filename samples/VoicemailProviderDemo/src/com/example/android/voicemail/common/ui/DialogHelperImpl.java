/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.example.android.voicemail.common.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

/**
 * Uses an {@link Activity} to show Dialogs.
 * <p>
 * Instantiate this class inside your Activity.
 *
 * <pre class="code">
 * private final DialogHelperImpl mDialogHelper = new DialogHelperImpl(this);
 * </pre>
 * <p>
 * Override your Activity's onCreateDialog(int, Bundle) method, as follows:
 *
 * <pre class="code">
 * &#064;Override
 * protected Dialog onCreateDialog(int id, Bundle bundle) {
 *     return mDialogHelper.handleOnCreateDialog(id, bundle);
 * }
 * </pre>
 * <p>
 * Now you can pass mDialogHelper around as a {@link DialogHelper} interface, and code that wants to
 * show a Dialog can call a method like this:
 *
 * <pre class="code">
 * mDialogHelper.showErrorMessageDialog(&quot;An exception occurred!&quot;, e);
 * </pre>
 * <p>
 * If you want more flexibility, and want to mix this implementation with your own dialogs, then you
 * should do something like this in your Activity:
 *
 * <pre class="code">
 * &#064;Override
 * protected Dialog onCreateDialog(int id, Bundle bundle) {
 *     switch (id) {
 *         case ID_MY_OTHER_DIALOG:
 *             return new AlertDialog.Builder(this)
 *                     .setTitle(&quot;something&quot;)
 *                     .create();
 *         default:
 *             return mDialogHelper.handleOnCreateDialog(id, bundle);
 *     }
 * }
 * </pre>
 *
 * Just be careful that you don't pick any IDs that conflict with those used by this class (which
 * are documented in the public static final fields).
 */
public class DialogHelperImpl implements DialogHelper {
    public static final int DIALOG_ID_EXCEPTION = 88953588;

    private static final String KEY_EXCEPTION = "exception";
    private static final String KEY_TITLE = "title";

    private final Activity mActivity;

    public DialogHelperImpl(Activity activity) {
        mActivity = activity;
    }

    @Override
    public void showErrorMessageDialog(int titleId, Exception exception) {
        showErrorMessageDialog(mActivity.getString(titleId), exception);
    }

    @Override
    public void showErrorMessageDialog(String title, Exception exception) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_TITLE, title);
        bundle.putSerializable(KEY_EXCEPTION, exception);
        mActivity.showDialog(DIALOG_ID_EXCEPTION, bundle);
    }

    /**
     * You should call this method from your Activity's onCreateDialog(int, Bundle) method.
     */
    public Dialog handleOnCreateDialog(int id, Bundle args) {
        if (id == DIALOG_ID_EXCEPTION) {
            Exception exception = (Exception) args.getSerializable(KEY_EXCEPTION);
            String title = args.getString(KEY_TITLE);
            return new AlertDialog.Builder(mActivity)
                    .setTitle(title)
                    .setMessage(convertExceptionToErrorMessage(exception))
                    .setCancelable(true)
                    .create();
        }
        return null;
    }

    private String convertExceptionToErrorMessage(Exception exception) {
        StringBuilder sb = new StringBuilder().append(exception.getClass().getSimpleName());
        if (exception.getMessage() != null) {
            sb.append("\n");
            sb.append(exception.getMessage());
        }
        return sb.toString();
    }
}
