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

package com.android.customlocale;


import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Displays the list of system locales as well as maintain a custom list of user
 * locales. The user can select a locale and apply it or it can create or remove
 * a custom locale.
 */
public class CustomLocaleActivity extends ListActivity {

    private static final String CUSTOM_LOCALES_SEP = " ";
    private static final String CUSTOM_LOCALES = "custom_locales";
    private static final String KEY_CUSTOM = "custom";
    private static final String KEY_NAME = "name";
    private static final String KEY_CODE = "code";

    private static final String TAG = "LocaleSetup";
    private static final boolean DEBUG = true;

    /** Request code returned when the NewLocaleDialog activity finishes. */
    private static final int UPDATE_LIST = 42;
    /** Menu item id for applying a locale */
    private static final int MENU_APPLY = 43;
    /** Menu item id for removing a custom locale */
    private static final int MENU_REMOVE = 44;

    /** List view displaying system and custom locales. */
    private ListView mListView;
    /** Textview used to display current locale */
    private TextView mCurrentLocaleTextView;
    /** Private shared preferences of this activity. */
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mPrefs = getPreferences(MODE_PRIVATE);

        Button newLocaleButton = (Button) findViewById(R.id.new_locale);

        newLocaleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(CustomLocaleActivity.this, NewLocaleDialog.class);
                startActivityForResult(i, UPDATE_LIST);
            }
        });

        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setFocusable(true);
        mListView.setFocusableInTouchMode(true);
        mListView.requestFocus();
        registerForContextMenu(mListView);
        setupLocaleList();

        mCurrentLocaleTextView = (TextView) findViewById(R.id.current_locale);
        displayCurrentLocale();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == UPDATE_LIST && resultCode == RESULT_OK && data != null) {
            String locale = data.getExtras().getString(NewLocaleDialog.INTENT_EXTRA_LOCALE);
            if (locale != null && locale.length() > 0) {
                // Get current custom locale list
                String customLocales = mPrefs.getString(CUSTOM_LOCALES, null);

                // Update
                if (customLocales == null) {
                    customLocales = locale;
                } else {
                    customLocales += CUSTOM_LOCALES_SEP + locale;
                }

                // Save prefs
                if (DEBUG) {
                    Log.d(TAG, "add/customLocales: " + customLocales);
                }
                mPrefs.edit().putString(CUSTOM_LOCALES, customLocales).commit();

                Toast.makeText(this, "Added custom locale: " + locale, Toast.LENGTH_SHORT).show();

                // Update list view
                setupLocaleList();

                // Find the item to select it in the list view
                ListAdapter a = mListView.getAdapter();
                for (int i = 0; i < a.getCount(); i++) {
                    Object o = a.getItem(i);
                    if (o instanceof Map<?, ?>) {
                        String code = ((Map<String, String>) o).get(KEY_CODE);
                        if (code != null && code.equals(locale)) {
                            mListView.setSelection(i);
                            break;
                        }
                    }
                }

                if (data.getExtras().getBoolean(NewLocaleDialog.INTENT_EXTRA_SELECT)) {
                    selectLocale(locale);
                }
            }
        }
    }

    private void setupLocaleList() {
        if (DEBUG) {
            Log.d(TAG, "Update locate list");
        }

        ArrayList<Map<String, String>> data = new ArrayList<Map<String, String>>();

        // Insert all system locales
        String[] locales = getAssets().getLocales();
        for (String locale : locales) {
            Locale loc = new Locale(locale);

            Map<String, String> map = new HashMap<String, String>(1);
            map.put(KEY_CODE, locale);
            map.put(KEY_NAME, loc.getDisplayName());
            data.add(map);
        }
        locales = null;

        // Insert all custom locales
        String customLocales = mPrefs.getString(CUSTOM_LOCALES, "");
        if (DEBUG) {
            Log.d(TAG, "customLocales: " + customLocales);
        }
        for (String locale : customLocales.split(CUSTOM_LOCALES_SEP)) {
            if (locale != null && locale.length() > 0) {
                Locale loc = new Locale(locale);

                Map<String, String> map = new HashMap<String, String>(1);
                map.put(KEY_CODE, locale);
                map.put(KEY_NAME, loc.getDisplayName() + " [Custom]");
                // the presence of the "custom" key marks it as custom.
                map.put(KEY_CUSTOM, "");
                data.add(map);
            }
        }

        // Sort all locales by code
        Collections.sort(data, new Comparator<Map<String, String>>() {
            public int compare(Map<String, String> lhs, Map<String, String> rhs) {
                return lhs.get(KEY_CODE).compareTo(rhs.get(KEY_CODE));
            }
        });

        // Update the list view adapter
        mListView.setAdapter(new SimpleAdapter(this, data, R.layout.list_item, new String[] {
                KEY_CODE, KEY_NAME}, new int[] {R.id.locale_code, R.id.locale_name}));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (menuInfo instanceof AdapterContextMenuInfo) {
            int position = ((AdapterContextMenuInfo) menuInfo).position;
            Object o = mListView.getItemAtPosition(position);
            if (o instanceof Map<?, ?>) {
                String locale = ((Map<String, String>) o).get(KEY_CODE);
                String custom = ((Map<String, String>) o).get(KEY_CUSTOM);

                if (custom == null) {
                    menu.setHeaderTitle("System Locale");
                    menu.add(0, MENU_APPLY, 0, "Apply");
                } else {
                    menu.setHeaderTitle("Custom Locale");
                    menu.add(0, MENU_APPLY, 0, "Apply");
                    menu.add(0, MENU_REMOVE, 0, "Remove");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onContextItemSelected(MenuItem item) {

        String pendingLocale = null;
        boolean is_custom = false;

        ContextMenuInfo menuInfo = item.getMenuInfo();
        if (menuInfo instanceof AdapterContextMenuInfo) {
            int position = ((AdapterContextMenuInfo) menuInfo).position;
            Object o = mListView.getItemAtPosition(position);
            if (o instanceof Map<?, ?>) {
                pendingLocale = ((Map<String, String>) o).get(KEY_CODE);
                is_custom = ((Map<String, String>) o).get(KEY_CUSTOM) != null;
            }
        }

        if (pendingLocale == null) {
            // should never happen
            return super.onContextItemSelected(item);
        }

        if (item.getItemId() == MENU_REMOVE) {
            // Get current custom locale list
            String customLocales = mPrefs.getString(CUSTOM_LOCALES, "");

            if (DEBUG) {
                Log.d(TAG, "Remove " + pendingLocale + " from custom locales: " + customLocales);
            }

            // Update
            StringBuilder sb = new StringBuilder();
            for (String locale : customLocales.split(CUSTOM_LOCALES_SEP)) {
                if (locale != null && locale.length() > 0 && !locale.equals(pendingLocale)) {
                    if (sb.length() > 0) {
                        sb.append(CUSTOM_LOCALES_SEP);
                    }
                    sb.append(locale);
                }
            }
            String newLocales = sb.toString();
            if (!newLocales.equals(customLocales)) {
                // Save prefs
                mPrefs.edit().putString(CUSTOM_LOCALES, customLocales).commit();

                Toast.makeText(this, "Removed custom locale: " + pendingLocale, Toast.LENGTH_SHORT)
                        .show();
            }

        } else if (item.getItemId() == MENU_APPLY) {
            selectLocale(pendingLocale);
        }

        return super.onContextItemSelected(item);
    }

    private void selectLocale(String locale) {
        if (DEBUG) {
            Log.d(TAG, "Select locale " + locale);
        }

        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            Configuration config = am.getConfiguration();

            Locale loc = null;

            String[] langCountry = locale.split("_");
            if (langCountry.length == 2) {
                loc = new Locale(langCountry[0], langCountry[1]);
            } else {
                loc = new Locale(locale);
            }
            
            config.locale = loc;

            // indicate this isn't some passing default - the user wants this
            // remembered
            config.userSetLocale = true;

            am.updateConfiguration(config);

            Toast.makeText(this, "Select locale: " + locale, Toast.LENGTH_SHORT).show();
        } catch (RemoteException e) {
            if (DEBUG) {
                Log.e(TAG, "Select locale failed", e);
            }
        }
    }

    private void displayCurrentLocale() {
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            Configuration config = am.getConfiguration();

            if (config.locale != null) {
                String text = String.format("%s - %s",
                        config.locale.toString(),
                        config.locale.getDisplayName());
                mCurrentLocaleTextView.setText(text);
            }
        } catch (RemoteException e) {
            if (DEBUG) {
                Log.e(TAG, "get current locale failed", e);
            }
        }
    }
}
