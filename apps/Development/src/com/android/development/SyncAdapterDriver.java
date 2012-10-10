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

package com.android.development;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.pm.RegisteredServicesCache;
import android.content.pm.RegisteredServicesCacheListener;
import android.content.SyncAdapterType;
import android.content.ISyncAdapter;
import android.content.ISyncContext;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.SyncResult;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ListView;
import android.util.AttributeSet;
import android.provider.Settings;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.view.View;
import android.view.LayoutInflater;

import java.util.Collection;

public class SyncAdapterDriver extends Activity
        implements RegisteredServicesCacheListener<SyncAdapterType>,
        AdapterView.OnItemClickListener {
    private Spinner mSyncAdapterSpinner;

    private Button mBindButton;
    private Button mUnbindButton;
    private TextView mBoundAdapterTextView;
    private Button mStartSyncButton;
    private Button mCancelSyncButton;
    private TextView mStatusTextView;
    private Object[] mSyncAdapters;
    private SyncAdaptersCache mSyncAdaptersCache;
    private final Object mSyncAdaptersLock = new Object();

    private static final int DIALOG_ID_PICK_ACCOUNT = 1;
    private ListView mAccountPickerView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSyncAdaptersCache = new SyncAdaptersCache(this);
        setContentView(R.layout.sync_adapter_driver);

        mSyncAdapterSpinner = (Spinner) findViewById(R.id.sync_adapters_spinner);
        mBindButton = (Button) findViewById(R.id.bind_button);
        mUnbindButton = (Button) findViewById(R.id.unbind_button);
        mBoundAdapterTextView = (TextView) findViewById(R.id.bound_adapter_text_view);

        mStartSyncButton = (Button) findViewById(R.id.start_sync_button);
        mCancelSyncButton = (Button) findViewById(R.id.cancel_sync_button);

        mStatusTextView = (TextView) findViewById(R.id.status_text_view);

        getSyncAdapters();
        mSyncAdaptersCache.setListener(this, null /* Handler */);
    }

    private void getSyncAdapters() {
        Collection<RegisteredServicesCache.ServiceInfo<SyncAdapterType>> all =
                mSyncAdaptersCache.getAllServices(UserHandle.myUserId());
        synchronized (mSyncAdaptersLock) {
            mSyncAdapters = new Object[all.size()];
            String[] names = new String[mSyncAdapters.length];
            int i = 0;
            for (RegisteredServicesCache.ServiceInfo<SyncAdapterType> item : all) {
                mSyncAdapters[i] = item;
                names[i] = item.type.authority + " - " + item.type.accountType;
                i++;
            }

            ArrayAdapter<String> adapter =
                    new ArrayAdapter<String>(this,
                    R.layout.sync_adapter_item, names);
            mSyncAdapterSpinner.setAdapter(adapter);
        }
    }

    void updateUi() {
        boolean isBound;
        boolean hasServiceConnection;
        synchronized (mServiceConnectionLock) {
            hasServiceConnection = mActiveServiceConnection != null;
            isBound = hasServiceConnection && mActiveServiceConnection.mBoundSyncAdapter != null;
        }
        mStartSyncButton.setEnabled(isBound);
        mCancelSyncButton.setEnabled(isBound);
        mBindButton.setEnabled(!hasServiceConnection);
        mUnbindButton.setEnabled(hasServiceConnection);
    }

    public void startSyncSelected(View view) {
        synchronized (mServiceConnectionLock) {
            ISyncAdapter syncAdapter = null;
            if (mActiveServiceConnection != null) {
                syncAdapter = mActiveServiceConnection.mBoundSyncAdapter;
            }

            if (syncAdapter != null) {
                removeDialog(DIALOG_ID_PICK_ACCOUNT);

                mAccountPickerView = (ListView) LayoutInflater.from(this).inflate(
                        R.layout.account_list_view, null);
                mAccountPickerView.setOnItemClickListener(this);
                Account accounts[] = AccountManager.get(this).getAccountsByType(
                        mActiveServiceConnection.mSyncAdapter.type.accountType);
                String[] accountNames = new String[accounts.length];
                for (int i = 0; i < accounts.length; i++) {
                    accountNames[i] = accounts[i].name;
                }
                ArrayAdapter<String> adapter =
                        new ArrayAdapter<String>(SyncAdapterDriver.this,
                        android.R.layout.simple_list_item_1, accountNames);
                mAccountPickerView.setAdapter(adapter);

                showDialog(DIALOG_ID_PICK_ACCOUNT);
            }
        }
        updateUi();
    }

    private void startSync(String accountName) {
        synchronized (mServiceConnectionLock) {
            ISyncAdapter syncAdapter = null;
            if (mActiveServiceConnection != null) {
                syncAdapter = mActiveServiceConnection.mBoundSyncAdapter;
            }

            if (syncAdapter != null) {
                try {
                    mStatusTextView.setText(
                            getString(R.string.status_starting_sync_format, accountName));
                    Account account = new Account(accountName,
                            mActiveServiceConnection.mSyncAdapter.type.accountType);
                    syncAdapter.startSync(mActiveServiceConnection,
                            mActiveServiceConnection.mSyncAdapter.type.authority,
                            account, new Bundle());
                } catch (RemoteException e) {
                    mStatusTextView.setText(
                            getString(R.string.status_remote_exception_while_starting_sync));
                }
            }
        }
        updateUi();
    }

    public void cancelSync(View view) {
        synchronized (mServiceConnectionLock) {
            ISyncAdapter syncAdapter = null;
            if (mActiveServiceConnection != null) {
                syncAdapter = mActiveServiceConnection.mBoundSyncAdapter;
            }

            if (syncAdapter != null) {
                try {
                    mStatusTextView.setText(getString(R.string.status_canceled_sync));
                    syncAdapter.cancelSync(mActiveServiceConnection);
                } catch (RemoteException e) {
                    mStatusTextView.setText(
                            getString(R.string.status_remote_exception_while_canceling_sync));
                }
            }
        }
        updateUi();
    }

    public void onServiceChanged(SyncAdapterType type, int userId, boolean removed) {
        getSyncAdapters();
    }

    @Override
    protected Dialog onCreateDialog(final int id) {
        if (id == DIALOG_ID_PICK_ACCOUNT) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.select_account_to_sync);
            builder.setInverseBackgroundForced(true);
            builder.setView(mAccountPickerView);
            return builder.create();
        }
        return super.onCreateDialog(id);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        TextView item = (TextView) view;
        final String accountName = item.getText().toString();
        dismissDialog(DIALOG_ID_PICK_ACCOUNT);
        startSync(accountName);
    }

    private class MyServiceConnection extends ISyncContext.Stub implements ServiceConnection {
        private volatile ISyncAdapter mBoundSyncAdapter;
        final RegisteredServicesCache.ServiceInfo<SyncAdapterType> mSyncAdapter;

        public MyServiceConnection(
                RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapter) {
            mSyncAdapter = syncAdapter;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            mBoundSyncAdapter = ISyncAdapter.Stub.asInterface(service);
            final SyncAdapterType type = mActiveServiceConnection.mSyncAdapter.type;
            mBoundAdapterTextView.setText(getString(R.string.binding_connected_format,
                    type.authority, type.accountType));
            updateUi();
        }

        public void onServiceDisconnected(ComponentName name) {
            mBoundAdapterTextView.setText(getString(R.string.binding_not_connected));
            mBoundSyncAdapter = null;
            updateUi();
        }

        public void sendHeartbeat() {
            runOnUiThread(new Runnable() {
                public void run() {
                    uiThreadSendHeartbeat();
                }
            });
        }

        public void uiThreadSendHeartbeat() {
            mStatusTextView.setText(getString(R.string.status_received_heartbeat));
        }

        public void uiThreadOnFinished(SyncResult result) {
            if (result.hasError()) {
                mStatusTextView.setText(
                        getString(R.string.status_sync_failed_format, result.toString()));
            } else {
                mStatusTextView.setText(
                        getString(R.string.status_sync_succeeded_format, result.toString()));
            }
        }

        public void onFinished(final SyncResult result) throws RemoteException {
            runOnUiThread(new Runnable() {
                public void run() {
                    uiThreadOnFinished(result);
                }
            });
        }
    }

    final Object mServiceConnectionLock = new Object();
    MyServiceConnection mActiveServiceConnection;

    public void initiateBind(View view) {
        synchronized (mServiceConnectionLock) {
            if (mActiveServiceConnection != null) {
                mStatusTextView.setText(getString(R.string.status_already_bound));
                return;
            }

            RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapter =
                    getSelectedSyncAdapter();
            if (syncAdapter == null) {
                mStatusTextView.setText(getString(R.string.status_sync_adapter_not_selected));
                return;
            }

            mActiveServiceConnection = new MyServiceConnection(syncAdapter);

            Intent intent = new Intent();
            intent.setAction("android.content.SyncAdapter");
            intent.setComponent(syncAdapter.componentName);
            intent.putExtra(Intent.EXTRA_CLIENT_LABEL,
                    com.android.internal.R.string.sync_binding_label);
            intent.putExtra(Intent.EXTRA_CLIENT_INTENT, PendingIntent.getActivity(
                    this, 0, new Intent(Settings.ACTION_SYNC_SETTINGS), 0));
            if (!bindService(intent, mActiveServiceConnection, Context.BIND_AUTO_CREATE)) {
                mBoundAdapterTextView.setText(getString(R.string.binding_bind_failed));
                mActiveServiceConnection = null;
                return;
            }
            mBoundAdapterTextView.setText(getString(R.string.binding_waiting_for_connection));
        }
        updateUi();
    }

    public void initiateUnbind(View view) {
        synchronized (mServiceConnectionLock) {
            if (mActiveServiceConnection == null) {
                return;
            }
            mBoundAdapterTextView.setText("");
            unbindService(mActiveServiceConnection);
            mActiveServiceConnection = null;
        }
        updateUi();
    }

    private RegisteredServicesCache.ServiceInfo<SyncAdapterType> getSelectedSyncAdapter() {
        synchronized (mSyncAdaptersLock) {
            final int position = mSyncAdapterSpinner.getSelectedItemPosition();
            if (position == AdapterView.INVALID_POSITION) {
                return null;
            }
            try {
                //noinspection unchecked
                return (RegisteredServicesCache.ServiceInfo<SyncAdapterType>)
                        mSyncAdapters[position];
            } catch (Exception e) {
                return null;
            }
        }
    }

    static class SyncAdaptersCache extends RegisteredServicesCache<SyncAdapterType> {
        private static final String SERVICE_INTERFACE = "android.content.SyncAdapter";
        private static final String SERVICE_META_DATA = "android.content.SyncAdapter";
        private static final String ATTRIBUTES_NAME = "sync-adapter";

        SyncAdaptersCache(Context context) {
            super(context, SERVICE_INTERFACE, SERVICE_META_DATA, ATTRIBUTES_NAME, null);
        }

        public SyncAdapterType parseServiceAttributes(Resources res,
                String packageName, AttributeSet attrs) {
            TypedArray sa = res.obtainAttributes(attrs,
                    com.android.internal.R.styleable.SyncAdapter);
            try {
                final String authority =
                        sa.getString(com.android.internal.R.styleable.SyncAdapter_contentAuthority);
                final String accountType =
                        sa.getString(com.android.internal.R.styleable.SyncAdapter_accountType);
                if (authority == null || accountType == null) {
                    return null;
                }
                final boolean userVisible = sa.getBoolean(
                        com.android.internal.R.styleable.SyncAdapter_userVisible, true);
                final boolean supportsUploading = sa.getBoolean(
                        com.android.internal.R.styleable.SyncAdapter_supportsUploading, true);
                final boolean isAlwaysSyncable = sa.getBoolean(
                        com.android.internal.R.styleable.SyncAdapter_isAlwaysSyncable, false);
                final boolean allowParallelSyncs = sa.getBoolean(
                        com.android.internal.R.styleable.SyncAdapter_allowParallelSyncs, false);
                final String settingsActivity =
                        sa.getString(com.android.internal.R.styleable
                                .SyncAdapter_settingsActivity);
                return new SyncAdapterType(authority, accountType, userVisible, supportsUploading,
                        isAlwaysSyncable, allowParallelSyncs, settingsActivity);
            } finally {
                sa.recycle();
            }
        }
    }
}
