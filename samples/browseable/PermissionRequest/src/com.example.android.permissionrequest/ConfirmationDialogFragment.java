/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.example.android.permissionrequest;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;

/**
 * Prompts the user to confirm permission request.
 */
public class ConfirmationDialogFragment extends DialogFragment {

    private static final String ARG_RESOURCES = "resources";

    /**
     * Creates a new instance of ConfirmationDialogFragment.
     *
     * @param resources The list of resources requested by PermissionRequeste.
     * @return A new instance.
     */
    public static ConfirmationDialogFragment newInstance(String[] resources) {
        ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putStringArray(ARG_RESOURCES, resources);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] resources = getArguments().getStringArray(ARG_RESOURCES);
        return new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.confirmation, TextUtils.join("\n", resources)))
                .setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((Listener) getParentFragment()).onConfirmation(false);
                    }
                })
                .setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((Listener) getParentFragment()).onConfirmation(true);
                    }
                })
                .create();
    }

    /**
     * Callback for the user's response.
     */
    public interface Listener {

        /**
         * Called when the PermissoinRequest is allowed or denied by the user.
         *
         * @param allowed True if the user allowed the request.
         */
        public void onConfirmation(boolean allowed);
    }

}
