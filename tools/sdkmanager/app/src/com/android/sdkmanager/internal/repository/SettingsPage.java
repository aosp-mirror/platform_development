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

package com.android.sdkmanager.internal.repository;

import com.android.prefs.AndroidLocation;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdkuilib.internal.repository.ISettingsPage;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/*
 * TODO list
 * - The window should probably set a callback to be notified when settings are changed.
 * - Actually use the settings.
 */

public class SettingsPage extends Composite implements ISettingsPage {

    private static final String SETTINGS_FILENAME = "androidtool.cfg"; //$NON-NLS-1$

    /** Java system setting picked up by {@link URL} for http proxy port */
    private static final String JAVA_HTTP_PROXY_PORT = "http.proxyPort";        //$NON-NLS-1$
    /** Java system setting picked up by {@link URL} for http proxy host */
    private static final String JAVA_HTTP_PROXY_HOST = "http.proxyHost";        //$NON-NLS-1$

    private Group mProxySettingsGroup;
    private Group mPlaceholderGroup;
    private Button mApplyButton;
    private Label mSomeMoreSettings;
    private Label mProxyServerLabel;
    private Label mProxyPortLabel;
    private Text mProxyServerText;
    private Text mProxyPortText;

    /**
     * Create the composite.
     * @param parent The parent of the composite.
     */
    public SettingsPage(Composite parent) {
        super(parent, SWT.BORDER);

        createContents(this);

        mProxySettingsGroup = new Group(this, SWT.NONE);
        mProxySettingsGroup.setText("Proxy Settings");
        mProxySettingsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        mProxySettingsGroup.setLayout(new GridLayout(2, false));

        mProxyServerLabel = new Label(mProxySettingsGroup, SWT.NONE);
        mProxyServerLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mProxyServerLabel.setText("HTTP Proxy Server");

        mProxyServerText = new Text(mProxySettingsGroup, SWT.BORDER);
        mProxyServerText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        mProxyPortLabel = new Label(mProxySettingsGroup, SWT.NONE);
        mProxyPortLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mProxyPortLabel.setText("HTTP Proxy Port");

        mProxyPortText = new Text(mProxySettingsGroup, SWT.BORDER);
        mProxyPortText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        mPlaceholderGroup = new Group(this, SWT.NONE);
        mPlaceholderGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        mPlaceholderGroup.setText("Placeholder");
        mPlaceholderGroup.setLayout(new GridLayout(1, false));

        mSomeMoreSettings = new Label(mPlaceholderGroup, SWT.NONE);
        mSomeMoreSettings.setText("Some more settings here");

        mApplyButton = new Button(this, SWT.NONE);
        mApplyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onApplySelected(); //$hide$
            }
        });
        mApplyButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mApplyButton.setText("Save && Apply");

        postCreate();  //$hide$
    }

    private void createContents(Composite parent) {
        parent.setLayout(new GridLayout(1, false));
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    /**
     * Called by the constructor right after {@link #createContents(Composite)}.
     */
    private void postCreate() {
    }

    private void onApplySelected() {
        applySettings();
        saveSettings();
    }

    /**
     * Update Java system properties for the HTTP proxy.
     */
    public void applySettings() {
        Properties props = System.getProperties();
        props.put(JAVA_HTTP_PROXY_HOST, mProxyServerText.getText());
        props.put(JAVA_HTTP_PROXY_PORT, mProxyPortText.getText());
    }

    /**
     * Saves settings.
     */
    private void saveSettings() {
        Properties props = new Properties();
        props.put(JAVA_HTTP_PROXY_HOST, mProxyServerText.getText());
        props.put(JAVA_HTTP_PROXY_PORT, mProxyPortText.getText());


        FileOutputStream fos = null;
        try {
            String folder = AndroidLocation.getFolder();
            File f = new File(folder, SETTINGS_FILENAME);

            fos = new FileOutputStream(f);

            props.store( fos, "## Settings for Android Tool");  //$NON-NLS-1$

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
     * Load settings and puts them in the UI.
     */
    public void loadSettings() {
        FileInputStream fis = null;
        try {
            String folder = AndroidLocation.getFolder();
            File f = new File(folder, SETTINGS_FILENAME);
            if (f.exists()) {
                fis = new FileInputStream(f);

                Properties props = new Properties();
                props.load(fis);

                mProxyServerText.setText(props.getProperty(JAVA_HTTP_PROXY_HOST, "")); //$NON-NLS-1$
                mProxyPortText.setText(props.getProperty(JAVA_HTTP_PROXY_PORT, ""));   //$NON-NLS-1$
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


    // End of hiding from SWT Designer
    //$hide<<$
}
