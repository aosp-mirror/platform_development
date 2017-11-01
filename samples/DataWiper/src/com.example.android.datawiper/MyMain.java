/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.example.android.datawiper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MyMain extends Activity {
    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshUi();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        refreshUi();
    }

    private void refreshUi() {
        final boolean isEnabled = MyAdmin.isEnabled(this);
        ((TextView) findViewById(R.id.add_as_admin)).setText(
                isEnabled ? "Disable Admin" : "Enable Admin");
        findViewById(R.id.wipe_data).setEnabled(isEnabled);
    }

    public void onAddAsAdmin(View v) {
        if (MyAdmin.isEnabled(this)) {
            MyAdmin.disable(this);
        } else {
            enableAdmin();
        }
        refreshUi();
    }

    private void enableAdmin() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, MyAdmin.getComponent(this));
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
    }

    public static void restart(Context context) {
        Intent intent = new Intent().setComponent(new ComponentName(context, MyMain.class))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                 | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public void onWipeData(View v) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("WARNING")
                .setMessage("This will wipe the device. Are you sure?  Do you want to proceed?"
                    + "THIS CANNOT BE UNDONE.")
                .setCancelable(true)
                .setPositiveButton("WIPE NOW", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.w("DataWiper", "Wiping...");
                        wipe();
                    }
                })
                .show();
    }

    private void wipe() {
        ((DevicePolicyManager) (getSystemService(Context.DEVICE_POLICY_SERVICE)))
                .wipeData(0);
    }
}
