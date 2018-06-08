/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.example.android.apis.content;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import com.example.android.apis.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Demonstration of package installation and uninstallation using the original (non-Session)
 * package installation API that uses {@link Intent#ACTION_INSTALL_PACKAGE}.
 *
 * @see InstallApkSessionApi for a demo of the newer Session API.
 */
public class InstallApk extends Activity {
    static final int REQUEST_INSTALL = 1;
    static final int REQUEST_UNINSTALL = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.install_apk);

        // Watch for button clicks.
        Button button = (Button)findViewById(R.id.unknown_source);
        button.setOnClickListener(mUnknownSourceListener);
        button = (Button)findViewById(R.id.my_source);
        button.setOnClickListener(mMySourceListener);
        button = (Button)findViewById(R.id.uninstall);
        button.setOnClickListener(mUninstallListener);
        button = (Button)findViewById(R.id.uninstall_result);
        button.setOnClickListener(mUninstallResultListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_INSTALL) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Install succeeded!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Install canceled!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Install Failed!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_UNINSTALL) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Uninstall succeeded!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Uninstall canceled!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Uninstall Failed!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private OnClickListener mUnknownSourceListener = new OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(getApkUri("HelloActivity.apk"));
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        }
    };

    private OnClickListener mMySourceListener = new OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(getApkUri("HelloActivity.apk"));
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME,
                    getApplicationInfo().packageName);
            startActivityForResult(intent, REQUEST_INSTALL);
        }
    };

    private OnClickListener mUninstallListener = new OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
            intent.setData(Uri.parse(
                    "package:com.example.android.helloactivity"));
            startActivity(intent);
        }
    };

    private OnClickListener mUninstallResultListener = new OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
            intent.setData(Uri.parse(
                    "package:com.example.android.helloactivity"));
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            startActivityForResult(intent, REQUEST_UNINSTALL);
        }
    };

    /**
     * Returns a Uri pointing to the APK to install.
     */
    private Uri getApkUri(String assetName) {
        // Before N, a MODE_WORLD_READABLE file could be passed via the ACTION_INSTALL_PACKAGE
        // Intent. Since N, MODE_WORLD_READABLE files are forbidden, and a FileProvider is
        // recommended.
        boolean useFileProvider = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;

        // Copy the given asset out into a file so that it can be installed.
        // Returns the path to the file.
        String tempFilename = "tmp.apk";
        byte[] buffer = new byte[16384];
        int fileMode = useFileProvider ? Context.MODE_PRIVATE : Context.MODE_WORLD_READABLE;

        try (InputStream is = getAssets().open(assetName);
            FileOutputStream fout = openFileOutput(tempFilename, fileMode)) {
            int n;
            while ((n=is.read(buffer)) >= 0) {
                fout.write(buffer, 0, n);
            }
        } catch (IOException e) {
            Log.i("InstallApk", "Failed to write temporary APK file", e);
        }

        if (useFileProvider) {
            File toInstall = new File(this.getFilesDir(), tempFilename);
            return FileProvider.getUriForFile(
                    this, "com.example.android.apis.installapkprovider", toInstall);
        } else {
            return Uri.fromFile(getFileStreamPath(tempFilename));
        }
    }
}
