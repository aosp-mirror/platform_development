/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdkuilib.internal.repository;

import com.android.prefs.AndroidLocation;
import com.android.prefs.AndroidLocation.AndroidLocationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 */
public class SettingsController {

    private static final String SETTINGS_FILENAME = "androidtool.cfg"; //$NON-NLS-1$

    private final Properties mProperties = new Properties();

    private ISettingsPage mSettingsPage;

    public SettingsController() {
    }

    //--- Access to settings ------------

    public boolean getForceHttp() {
        return Boolean.parseBoolean(mProperties.getProperty(ISettingsPage.KEY_FORCE_HTTP));
    }

    public boolean getShowUpdateOnly() {
        String value = mProperties.getProperty(ISettingsPage.KEY_SHOW_UPDATE_ONLY);
        if (value == null) {
            return true;
        }
        return Boolean.parseBoolean(value);
    }

    public void setShowUpdateOnly(boolean enabled) {
        mProperties.setProperty(ISettingsPage.KEY_SHOW_UPDATE_ONLY, Boolean.toString(enabled));
    }

    //--- Controller methods -------------

    /**
     * Associate the given {@link ISettingsPage} with this {@link SettingsController}.
     *
     * This loads the current properties into the setting page UI.
     * It then associates the SettingsChanged callback with this controller.
     */
    public void setSettingsPage(ISettingsPage settingsPage) {

        mSettingsPage = settingsPage;
        mSettingsPage.loadSettings(mProperties);

        settingsPage.setOnSettingsChanged(new ISettingsPage.SettingsChangedCallback() {
            public void onSettingsChanged(ISettingsPage page) {
                SettingsController.this.onSettingsChanged();
            }
        });
    }

    /**
     * Load settings from the settings file.
     */
    public void loadSettings() {
        FileInputStream fis = null;
        try {
            String folder = AndroidLocation.getFolder();
            File f = new File(folder, SETTINGS_FILENAME);
            if (f.exists()) {
                fis = new FileInputStream(f);

                mProperties.load(fis);
            }

        } catch (AndroidLocationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Saves settings to the settings file.
     */
    public void saveSettings() {

        FileOutputStream fos = null;
        try {
            String folder = AndroidLocation.getFolder();
            File f = new File(folder, SETTINGS_FILENAME);

            fos = new FileOutputStream(f);

            mProperties.store( fos, "## Settings for Android Tool");  //$NON-NLS-1$

        } catch (AndroidLocationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * When settings have changed: retrieve the new settings, apply them and save them.
     *
     * This updats Java system properties for the HTTP proxy.
     */
    private void onSettingsChanged() {
        if (mSettingsPage == null) {
            return;
        }

        mSettingsPage.retrieveSettings(mProperties);
        applySettings();
        saveSettings();
    }

    /**
     * Applies the current settings.
     */
    public void applySettings() {
        Properties props = System.getProperties();
        props.setProperty(ISettingsPage.KEY_HTTP_PROXY_HOST,
                mProperties.getProperty(ISettingsPage.KEY_HTTP_PROXY_HOST, "")); //$NON-NLS-1$
        props.setProperty(ISettingsPage.KEY_HTTP_PROXY_PORT,
                mProperties.getProperty(ISettingsPage.KEY_HTTP_PROXY_PORT, ""));   //$NON-NLS-1$
    }

}
