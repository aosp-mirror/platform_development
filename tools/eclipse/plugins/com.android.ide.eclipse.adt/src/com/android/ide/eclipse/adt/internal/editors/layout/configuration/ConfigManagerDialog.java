/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.editors.layout.configuration;

import com.android.ddmuilib.TableHelper;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.ide.eclipse.adt.internal.sdk.LayoutDevice;
import com.android.ide.eclipse.adt.internal.sdk.LayoutDeviceManager;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.sdkuilib.ui.GridDialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Dialog to view the layout devices with action button to create/edit/delete/copy layout devices
 * and configs.
 *
 */
public class ConfigManagerDialog extends GridDialog {

    private final static String COL_NAME = AdtPlugin.PLUGIN_ID + ".configmanager.name"; //$NON-NLS-1$
    private final static String COL_CONFIG = AdtPlugin.PLUGIN_ID + ".configmanager.config"; //$NON-NLS-1$

    /**
     * enum to represent the different origin of the layout devices.
     */
    private static enum DeviceType {
        DEFAULT("Default"),
        ADDON("Add-on"),
        CUSTOM("Custom");

        private final String mDisplay;

        DeviceType(String display) {
            mDisplay = display;
        }

        String getDisplayString() {
            return mDisplay;
        }
    }

    /**
     * simple class representing the tree selection with the proper types.
     */
    private static class DeviceSelection {
        public DeviceSelection(DeviceType type, LayoutDevice device,
                Entry<String, FolderConfiguration> entry) {
            this.type = type;
            this.device = device;
            this.entry = entry;
        }

        final DeviceType type;
        final LayoutDevice device;
        final Entry<String, FolderConfiguration> entry;
    }

    private final LayoutDeviceManager mManager;

    private TreeViewer mTreeViewer;
    private Button mNewButton;
    private Button mEditButton;
    private Button mCopyButton;
    private Button mDeleteButton;

    /**
     * Content provider of the {@link TreeViewer}. The expected input is
     * {@link LayoutDeviceManager}.
     *
     */
    private final static class DeviceContentProvider implements ITreeContentProvider {
        private final static DeviceType[] sCategory = new DeviceType[] {
            DeviceType.DEFAULT, DeviceType.ADDON, DeviceType.CUSTOM
        };

        private LayoutDeviceManager mLayoutDeviceManager;

        public DeviceContentProvider() {
        }

        public Object[] getElements(Object inputElement) {
            return sCategory;
        }

        public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof DeviceType) {
                if (DeviceType.DEFAULT.equals(parentElement)) {
                    return mLayoutDeviceManager.getDefaultLayoutDevices().toArray();
                } else if (DeviceType.ADDON.equals(parentElement)) {
                    return mLayoutDeviceManager.getAddOnLayoutDevice().toArray();
                } else if (DeviceType.CUSTOM.equals(parentElement)) {
                    return mLayoutDeviceManager.getUserLayoutDevices().toArray();
                }
            } else if (parentElement instanceof LayoutDevice) {
                LayoutDevice device = (LayoutDevice)parentElement;
                return device.getConfigs().entrySet().toArray();
            }

            return null;
        }

        public Object getParent(Object element) {
            // parent cannot be computed. this is fine.
            return null;
        }

        public boolean hasChildren(Object element) {
            if (element instanceof DeviceType) {
                if (DeviceType.DEFAULT.equals(element)) {
                    return mLayoutDeviceManager.getDefaultLayoutDevices().size() > 0;
                } else if (DeviceType.ADDON.equals(element)) {
                    return mLayoutDeviceManager.getAddOnLayoutDevice().size() > 0;
                } else if (DeviceType.CUSTOM.equals(element)) {
                    return mLayoutDeviceManager.getUserLayoutDevices().size() > 0;
                }
            } else if (element instanceof LayoutDevice) {
                LayoutDevice device = (LayoutDevice)element;
                return device.getConfigs().size() > 0;
            }

            return false;
        }


        public void dispose() {
            // nothing to dispose
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput instanceof LayoutDeviceManager) {
                mLayoutDeviceManager = (LayoutDeviceManager)newInput;
                return;
            }

            // when the dialog closes we get null input
            if (newInput != null) {
                throw new IllegalArgumentException(
                        "ConfigContentProvider requires input to be LayoutDeviceManager");
            }
        }
    }

    /**
     * Label provider for the {@link TreeViewer}.
     * Supported elements are {@link DeviceType}, {@link LayoutDevice}, and {@link Entry} (where
     * the key is a {@link String} object, and the value is a {@link FolderConfiguration} object).
     *
     */
    private final static class DeviceLabelProvider implements ITableLabelProvider {

        public String getColumnText(Object element, int columnIndex) {
            if (element instanceof DeviceType) {
                if (columnIndex == 0) {
                    return ((DeviceType)element).getDisplayString();
                }
            } else if (element instanceof LayoutDevice) {
                if (columnIndex == 0) {
                    return ((LayoutDevice)element).getName();
                }
            } else if (element instanceof Entry<?, ?>) {
                if (columnIndex == 0) {
                    return (String)((Entry<?,?>)element).getKey();
                } else {
                    return ((Entry<?,?>)element).getValue().toString();
                }
            }
            return null;
        }

        public Image getColumnImage(Object element, int columnIndex) {
            // no image
            return null;
        }

        public void addListener(ILabelProviderListener listener) {
            // no listener
        }

        public void removeListener(ILabelProviderListener listener) {
            // no listener
        }

        public void dispose() {
            // nothing to dispose
        }

        public boolean isLabelProperty(Object element, String property) {
            return false;
        }
    }

    protected ConfigManagerDialog(Shell parentShell) {
        super(parentShell, 2, false);
        mManager = Sdk.getCurrent().getLayoutDeviceManager();
    }

    @Override
    protected int getShellStyle() {
        return super.getShellStyle() | SWT.RESIZE;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Device Configurations");
    }

    @Override
    public void createDialogContent(final Composite parent) {
        GridData gd;
        GridLayout gl;

        Tree tree = new Tree(parent, SWT.SINGLE | SWT.FULL_SELECTION);
        tree.setLayoutData(gd = new GridData(GridData.FILL_BOTH));
        gd.widthHint = 700;

        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);
        TableHelper.createTreeColumn(tree, "Name", SWT.LEFT, 150, COL_NAME,
                AdtPlugin.getDefault().getPreferenceStore());
        TableHelper.createTreeColumn(tree, "Configuration", SWT.LEFT, 500, COL_CONFIG,
                AdtPlugin.getDefault().getPreferenceStore());

        mTreeViewer = new TreeViewer(tree);
        mTreeViewer.setContentProvider(new DeviceContentProvider());
        mTreeViewer.setLabelProvider(new DeviceLabelProvider());
        mTreeViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
        mTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                setEnabled(getSelection());
            }
        });

        Composite buttons = new Composite(parent, SWT.NONE);
        buttons.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        buttons.setLayout(gl = new GridLayout());
        gl.marginHeight = gl.marginWidth = 0;

        mNewButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
        mNewButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mNewButton.setText("New...");
        mNewButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DeviceSelection selection = getSelection();

                ConfigEditDialog dlg = new ConfigEditDialog(parent.getShell(), null);
                if (selection.device != null) {
                    dlg.setDeviceName(selection.device.getName());
                    dlg.setXDpi(selection.device.getXDpi());
                    dlg.setYDpi(selection.device.getYDpi());
                }
                if (selection.entry != null) {
                    dlg.setConfigName(selection.entry.getKey());
                    dlg.setConfig(selection.entry.getValue());
                }

                if (dlg.open() == Window.OK) {
                    String deviceName = dlg.getDeviceName();
                    String configName = dlg.getConfigName();
                    FolderConfiguration config = new FolderConfiguration();
                    dlg.getConfig(config);

                    // first if there was no original device, we create one.
                    // Because the new button is disabled when something else than "custom" is
                    // selected, we always add to the user devices without checking.
                    LayoutDevice d;
                    if (selection.device == null) {
                        // FIXME: this doesn't check if the device name is taken.
                        d = mManager.addUserDevice(deviceName, dlg.getXDpi(), dlg.getYDpi());
                    } else {
                        // search for it.
                        d = mManager.getUserLayoutDevice(deviceName);
                    }

                    if (d != null) {
                        // then if there was no config, we add it, otherwise we edit it
                        // (same method that adds/replace a config).
                        // FIXME this doesn't check if the name was already taken.
                        mManager.addUserConfiguration(d, configName, config);

                        mTreeViewer.refresh();
                        select(d, configName);
                    }
                }
            }
        });

        mEditButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
        mEditButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mEditButton.setText("Edit...");
        mEditButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DeviceSelection selection = getSelection();
                ConfigEditDialog dlg = new ConfigEditDialog(parent.getShell(), null);
                dlg.setDeviceName(selection.device.getName());
                dlg.setXDpi(selection.device.getXDpi());
                dlg.setYDpi(selection.device.getYDpi());
                dlg.setConfigName(selection.entry.getKey());
                dlg.setConfig(selection.entry.getValue());

                if (dlg.open() == Window.OK) {
                    String deviceName = dlg.getDeviceName();
                    String configName = dlg.getConfigName();
                    FolderConfiguration config = new FolderConfiguration();
                    dlg.getConfig(config);

                    // replace the device if needed.
                    // FIXME: this doesn't check if the replacement name doesn't exist already.
                    LayoutDevice d = mManager.replaceUserDevice(selection.device, deviceName,
                            dlg.getXDpi(), dlg.getYDpi());

                    // and add/replace the config
                    mManager.replaceUserConfiguration(d, selection.entry.getKey(), configName,
                            config);

                    mTreeViewer.refresh();
                    select(d, configName);
                }
            }
        });

        mCopyButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
        mCopyButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mCopyButton.setText("Copy");
        mCopyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DeviceSelection selection = getSelection();

                // is the source a default/add-on device, or are we copying a full device?
                // if so the target device is a new device.
                LayoutDevice targetDevice = selection.device;
                if (selection.type == DeviceType.DEFAULT || selection.type == DeviceType.ADDON ||
                        selection.entry == null) {
                    // create a new device
                    targetDevice = mManager.addUserDevice(
                            selection.device.getName() + " Copy", // new name
                            selection.device.getXDpi(),
                            selection.device.getYDpi());
                }

                String newConfigName = null; // name of the single new config. used for the select.

                // are we copying the full device?
                if (selection.entry == null) {
                    // get the config from the origin device
                    Map<String, FolderConfiguration> configs = selection.device.getConfigs();

                    // and copy them in the target device
                    for (Entry<String, FolderConfiguration> entry : configs.entrySet()) {
                        // we need to make a copy of the config object, or it could be modified
                        // in default/addon by editing the version in the new device.
                        FolderConfiguration copy = new FolderConfiguration();
                        copy.set(entry.getValue());

                        // the name can stay the same since we are copying a full device
                        // and the target device has its own new name.
                        mManager.addUserConfiguration(targetDevice, entry.getKey(), copy);
                    }
                } else {
                    // only copy the config. target device is not the same as the selection, don't
                    // change the config name as we already changed the name of the device.
                    newConfigName = (selection.device != targetDevice) ?
                            selection.entry.getKey() : selection.entry.getKey() + " Copy";

                    // copy of the config
                    FolderConfiguration copy = new FolderConfiguration();
                    copy.set(selection.entry.getValue());

                    // and create the config
                    mManager.addUserConfiguration(targetDevice, newConfigName, copy);
                }

                mTreeViewer.refresh();

                select(targetDevice, newConfigName);
            }
        });

        mDeleteButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
        mDeleteButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mDeleteButton.setText("Delete");
        mDeleteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DeviceSelection selection = getSelection();

                if (selection.entry != null) {
                    mManager.removeUserConfiguration(selection.device, selection.entry.getKey());
                } else if (selection.device != null) {
                    mManager.removeUserDevice(selection.device);
                }

                mTreeViewer.refresh();

                // either select the device (if we removed a entry, or the top custom node if
                // we removed a device)
                select(selection.entry != null ? selection.device : null, null);
            }
        });

        Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.horizontalSpan = 2;

        mTreeViewer.setInput(mManager);
        setEnabled(null); // no selection at the start
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // we only want an OK button.
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    /**
     * Returns a {@link DeviceSelection} object representing the selected path in the
     * {@link TreeViewer}
     */
    private DeviceSelection getSelection() {
        // get the selection paths
        TreeSelection selection = (TreeSelection)mTreeViewer.getSelection();
        TreePath[] paths =selection.getPaths();

        if (paths.length == 0) {
            return null;
        }

        TreePath pathSelection = paths[0];

        DeviceType type = (DeviceType)pathSelection.getFirstSegment();
        LayoutDevice device = null;
        Entry<String, FolderConfiguration> entry = null;
        switch (pathSelection.getSegmentCount()) {
            case 2: // layout device is selected
                device = (LayoutDevice)pathSelection.getLastSegment();
                break;
            case 3: // config is selected
                device = (LayoutDevice)pathSelection.getSegment(1);
                entry = (Entry<String, FolderConfiguration>)pathSelection.getLastSegment();
        }

        return new DeviceSelection(type, device, entry);
    }

    /**
     * Enables/disables the action button based on the {@link DeviceSelection}.
     * @param selection the selection
     */
    protected void setEnabled(DeviceSelection selection) {
        if (selection == null) {
            mNewButton.setEnabled(false);
            mEditButton.setEnabled(false);
            mCopyButton.setEnabled(false);
            mDeleteButton.setEnabled(false);
        } else {
            switch (selection.type) {
                case DEFAULT:
                case ADDON:
                    // only allow copy if device is not null
                    mNewButton.setEnabled(false);
                    mEditButton.setEnabled(false);
                    mDeleteButton.setEnabled(false);
                    mCopyButton.setEnabled(selection.device != null);
                    break;
                case CUSTOM:
                    mNewButton.setEnabled(true); // always true to create new devices.
                    mEditButton.setEnabled(selection.entry != null); // only edit config for now

                    boolean enabled = selection.device != null; // need at least selected device
                    mDeleteButton.setEnabled(enabled);          // for delete and copy buttons
                    mCopyButton.setEnabled(enabled);
                    break;
            }
        }
    }

    /**
     * Selects a device and optionally a config. Because this is meant to show newly created/edited
     * device/config, it'll only do so for {@link DeviceType#CUSTOM} devices.
     * @param device the device to select
     * @param configName the config to select (optional)
     */
    private void select(LayoutDevice device, String configName) {
        Object[] path;
        if (device == null) {
            // select the "custom" node
            path = new Object[] { DeviceType.CUSTOM };
        } else if (configName == null) {
            // this is the easy case. no config to select
            path = new Object[] { DeviceType.CUSTOM, device };
        } else {
            // this is more complex. we have the configName, but the tree contains Entry<?,?>
            // Look for the entry.
            Entry<?, ?> match = null;
            for (Entry<?, ?> entry : device.getConfigs().entrySet()) {
                if (entry.getKey().equals(configName)) {
                    match = entry;
                    break;
                }
            }

            if (match != null) {
                path = new Object[] { DeviceType.CUSTOM, device, match };
            } else {
                path = new Object[] { DeviceType.CUSTOM, device };
            }
        }

        mTreeViewer.setSelection(new TreeSelection(new TreePath(path)), true /*reveal*/);
    }
}
