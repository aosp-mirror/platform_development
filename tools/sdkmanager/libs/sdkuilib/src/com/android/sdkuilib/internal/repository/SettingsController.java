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
import com.android.sdklib.ISdkLog;

import org.eclipse.jface.dialogs.MessageDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Controller class to get settings values. Settings are kept in-memory.
 * Users of this class must first load the settings before changing them and save
 * them when modified.
 * <p/>
 * Settings are enumerated by constants in {@link ISettingsPage}.
 */
public class SettingsController {

    private static final String SETTINGS_FILENAME = "androidtool.cfg"; //$NON-NLS-1$

    private final Properties mProperties = new Properties();

    private ISettingsPage mSettingsPage;

    private final UpdaterData mUpdaterData;

    public SettingsController(UpdaterData updaterData) {
        mUpdaterData = updaterData;
    }

    //--- Access to settings ------------

    /**
     * Returns the value of the {@link ISettingsPage#KEY_FORCE_HTTP} setting.
     * @see ISettingsPage#KEY_FORCE_HTTP
     */
    public boolean getForceHttp() {
        return Boolean.parseBoolean(mProperties.getProperty(ISettingsPage.KEY_FORCE_HTTP));
    }

    /**
     * Returns the value of the {@link ISettingsPage#KEY_ASK_ADB_RESTART} setting.
     * @see ISettingsPage#KEY_ASK_ADB_RESTART
     */
    public boolean getAskBeforeAdbRestart() {
        String value = mProperties.getProperty(ISettingsPage.KEY_ASK_ADB_RESTART);
        if (value == null) {
            return true;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * Returns the value of the {@link ISettingsPage#KEY_SHOW_UPDATE_ONLY} setting.
     * @see ISettingsPage#KEY_SHOW_UPDATE_ONLY
     */
    public boolean getShowUpdateOnly() {
        String value = mProperties.getProperty(ISettingsPage.KEY_SHOW_UPDATE_ONLY);
        if (value == null) {
            return true;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * Sets the value of the {@link ISettingsPage#KEY_SHOW_UPDATE_ONLY} setting.
     * @param enabled True if only compatible update items should be shown.
     * @see ISettingsPage#KEY_SHOW_UPDATE_ONLY
     */
    public void setShowUpdateOnly(boolean enabled) {
        setSetting(ISettingsPage.KEY_SHOW_UPDATE_ONLY, enabled);
    }

    /**
     * Returns the value of the {@link ISettingsPage#KEY_MONITOR_DENSITY} setting
     * @see ISettingsPage#KEY_MONITOR_DENSITY
     */
    public int getMonitorDensity() {
        String value = mProperties.getProperty(ISettingsPage.KEY_MONITOR_DENSITY, null);
        if (value == null) {
            return -1;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Sets the value of the {@link ISettingsPage#KEY_MONITOR_DENSITY} setting.
     * @param density the density of the monitor
     * @see ISettingsPage#KEY_MONITOR_DENSITY
     */
    public void setMonitorDensity(int density) {
        mProperties.setProperty(ISettingsPage.KEY_MONITOR_DENSITY, Integer.toString(density));
    }

    /**
     * Internal helper to set a boolean setting.
     */
    private void setSetting(String key, boolean value) {
        mProperties.setProperty(key, Boolean.toString(value));
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
        String path = null;
        try {
            String folder = AndroidLocation.getFolder();
            File f = new File(folder, SETTINGS_FILENAME);
            path = f.getPath();
            if (f.exists()) {
                fis = new FileInputStream(f);

                mProperties.load(fis);

                // Properly reformat some settings to enforce their default value when missing.
                setShowUpdateOnly(getShowUpdateOnly());
                setSetting(ISettingsPage.KEY_ASK_ADB_RESTART, getAskBeforeAdbRestart());
            }

        } catch (Exception e) {
            ISdkLog log = mUpdaterData.getSdkLog();
            if (log != null) {
                log.error(e, "Failed to load settings from '%1$s'", path);
            }
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
        String path = null;
        try {
            String folder = AndroidLocation.getFolder();
            File f = new File(folder, SETTINGS_FILENAME);
            path = f.getPath();

            fos = new FileOutputStream(f);

            mProperties.store( fos, "## Settings for Android Tool");  //$NON-NLS-1$

        } catch (Exception e) {
            ISdkLog log = mUpdaterData.getSdkLog();

            if (log != null) {
                log.error(e, "Failed to save settings at '%1$s'", path);
            }

            // This is important enough that we want to really nag the user about it
            String reason = null;

            if (e instanceof FileNotFoundException) {
                reason = "File not found";
            } else if (e instanceof AndroidLocationException) {
                reason = ".android folder not found, please define ANDROID_SDK_HOME";
            } else if (e.getMessage() != null) {
                reason = String.format("%1$s: %2$s", e.getClass().getSimpleName(), e.getMessage());
            } else {
                reason = e.getClass().getName();
            }

            MessageDialog.openInformation(mUpdaterData.getWindowShell(),
                    "SDK Manager Settings",
                    String.format(
                        "The Android SDK and AVD Manager failed to save its settings (%1$s) at %2$s",
                        reason, path));

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
     * This updates Java system properties for the HTTP proxy.
     */
    private void onSettingsChanged() {
        if (mSettingsPage == null) {
            return;
        }

        mSettingsPage.retrieveSettings(mProperties);
        applySettings();
        saveSettings();

        // In case the HTTP/HTTPS setting change, force sources to be reloaded
        // (this only refreshes sources that the user has already tried to open.)
        mUpdaterData.refreshSources(false /*forceFetching*/);
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
