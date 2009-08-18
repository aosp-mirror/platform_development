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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
    private Group mMiscGroup;
    private Button mApplyButton;
    private Label mProxyServerLabel;
    private Label mProxyPortLabel;
    private Text mProxyServerText;
    private Text mProxyPortText;
    private Button mForceHttpCheck;
    private Button mAskAdbRestartCheck;

    private ModifyListener mSetApplyDirty = new ModifyListener() {
        public void modifyText(ModifyEvent e) {
            mApplyButton.setEnabled(true);
        }
    };


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
        String tooltip = "The DNS name or IP of the HTTP proxy server to use. " +
                         "When empty, no HTTP proxy is used.";
        mProxyServerLabel.setToolTipText(tooltip);

        mProxyServerText = new Text(mProxySettingsGroup, SWT.BORDER);
        mProxyServerText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        mProxyServerText.addModifyListener(mSetApplyDirty);
        mProxyServerText.setToolTipText(tooltip);

        mProxyPortLabel = new Label(mProxySettingsGroup, SWT.NONE);
        mProxyPortLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mProxyPortLabel.setText("HTTP Proxy Port");
        tooltip = "The port of the HTTP proxy server to use. " +
                  "When empty, the default for HTTP or HTTPS is used.";
        mProxyPortLabel.setToolTipText(tooltip);

        mProxyPortText = new Text(mProxySettingsGroup, SWT.BORDER);
        mProxyPortText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        mProxyPortText.addModifyListener(mSetApplyDirty);
        mProxyPortText.setToolTipText(tooltip);

        mMiscGroup = new Group(this, SWT.NONE);
        mMiscGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        mMiscGroup.setText("Misc");
        mMiscGroup.setLayout(new GridLayout(2, false));

        mForceHttpCheck = new Button(mMiscGroup, SWT.CHECK);
        mForceHttpCheck.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        mForceHttpCheck.setText("Force https://... sources to be fetched using http://...");
        mForceHttpCheck.setToolTipText("If you are not able to connect to the official Android repository " +
                "using HTTPS, enable this setting to force accessing it via HTTP.");
        mForceHttpCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onForceHttpSelected();  //$hide$
            }
        });

        mAskAdbRestartCheck = new Button(mMiscGroup, SWT.CHECK);
        mAskAdbRestartCheck.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        mAskAdbRestartCheck.setText("Ask before restarting ADB");
        mAskAdbRestartCheck.setToolTipText("When checked, the user will be asked for permission " +
                "to restart ADB after updating an addon-on package or a tool package.");
        mAskAdbRestartCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onForceHttpSelected();  //$hide$
            }
        });

        mApplyButton = new Button(this, SWT.NONE);
        mApplyButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mApplyButton.setText("Save && Apply");
        mApplyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onApplySelected(); //$hide$
            }
        });

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
        mProxyServerText.setText(in_settings.getProperty(KEY_HTTP_PROXY_HOST, ""));  //$NON-NLS-1$
        mProxyPortText.setText(  in_settings.getProperty(KEY_HTTP_PROXY_PORT, ""));  //$NON-NLS-1$
        mForceHttpCheck.setSelection(Boolean.parseBoolean(in_settings.getProperty(KEY_FORCE_HTTP)));
        mAskAdbRestartCheck.setSelection(Boolean.parseBoolean(in_settings.getProperty(KEY_ASK_ADB_RESTART)));

        // We loaded fresh settings so there's nothing dirty to apply
        mApplyButton.setEnabled(false);
    }

    /** Called by the application to retrieve settings from the UI and store them in
     * the given {@link Properties} container. */
    public void retrieveSettings(Properties out_settings) {
        out_settings.setProperty(KEY_HTTP_PROXY_HOST, mProxyServerText.getText());
        out_settings.setProperty(KEY_HTTP_PROXY_PORT, mProxyPortText.getText());
        out_settings.setProperty(KEY_FORCE_HTTP,
                Boolean.toString(mForceHttpCheck.getSelection()));
        out_settings.setProperty(KEY_ASK_ADB_RESTART,
                Boolean.toString(mAskAdbRestartCheck.getSelection()));
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
     * Callback invoked when user presses the "Save and Apply" button.
     * Notify the application that settings have changed.
     */
    private void onApplySelected() {
        if (mSettingsChangedCallback != null) {
            mSettingsChangedCallback.onSettingsChanged(this);
            mApplyButton.setEnabled(false);
        }
    }

    /**
     * Callback invoked when the users presses the Force HTTPS checkbox.
     */
    private void onForceHttpSelected() {
        mSetApplyDirty.modifyText(null);
    }

    // End of hiding from SWT Designer
    //$hide<<$
}
