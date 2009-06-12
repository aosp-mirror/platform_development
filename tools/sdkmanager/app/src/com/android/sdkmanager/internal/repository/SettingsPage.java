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

import java.util.Properties;


public class SettingsPage extends Composite implements ISettingsPage {

    // data members
    private SettingsChangedCallback mSettingsChangedCallback;

    // UI widgets
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

    /** Loads settings from the given {@link Properties} container and update the page UI. */
    public void loadSettings(Properties in_settings) {
        mProxyServerText.setText(in_settings.getProperty(JAVA_HTTP_PROXY_HOST, "")); //$NON-NLS-1$
        mProxyPortText.setText(in_settings.getProperty(JAVA_HTTP_PROXY_PORT, ""));   //$NON-NLS-1$
    }

    /** Called by the application to retrieve settings from the UI and store them in
     * the given {@link Properties} container. */
    public void retrieveSettings(Properties out_settings) {
        out_settings.put(JAVA_HTTP_PROXY_HOST, mProxyServerText.getText());
        out_settings.put(JAVA_HTTP_PROXY_PORT, mProxyPortText.getText());
    }

    /**
     * Called by the application to give a callback that the page should invoke when
     * settings must be applied. The page does not apply the settings itself, instead
     * it notifies the application.
     */
    public void setOnSettingsChanged(SettingsChangedCallback settingsChangedCallback) {
        mSettingsChangedCallback = settingsChangedCallback;
    }

    /**
     * Notify the application that settings have changed.
     */
    private void onApplySelected() {
        if (mSettingsChangedCallback != null) {
            mSettingsChangedCallback.onSettingsChanged(this);
        }
    }

    // End of hiding from SWT Designer
    //$hide<<$
}
