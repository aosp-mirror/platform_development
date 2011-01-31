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

package com.android.customlocale2;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.IActivityManager;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Displays the list of system locales as well as maintain a custom list of user
 * locales. The user can select a locale and apply it or it can create or remove
 * a custom locale.
 */
public class CustomLocaleActivity extends ListActivity {

    private static final String TAG = "CustomLocale";
    private static final boolean DEBUG = true;

    private static final int DLG_REMOVE_ID = 0;

    private static final String CUSTOM_LOCALES_SEP = " ";
    private static final String CUSTOM_LOCALES = "custom_locales";

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

    private Button mRemoveLocaleButton;
    private Button mSelectLocaleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mPrefs = getPreferences(MODE_PRIVATE);

        Button addLocaleButton = (Button) findViewById(R.id.add_new_locale_button);
        mRemoveLocaleButton = (Button) findViewById(R.id.remove_locale_button);
        mSelectLocaleButton = (Button) findViewById(R.id.select_locale_button);

        addLocaleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onAddNewLocale();
            }
        });

        mRemoveLocaleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DLG_REMOVE_ID);
            }
        });

        mSelectLocaleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSelectLocale();
            }
        });

        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setItemsCanFocus(false);
        mListView.setFocusable(true);
        mListView.setFocusableInTouchMode(true);
        mListView.requestFocus();
        setupLocaleList();

        mCurrentLocaleTextView = (TextView) findViewById(R.id.current_locale);
        displayCurrentLocale();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLocaleButtons();
    }

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

                Toast.makeText(this,
                               getString(R.string.added_custom_locale_1s, locale),
                               Toast.LENGTH_SHORT)
                     .show();

                // Update list view
                setupLocaleList();

                // Find the item to select it in the list view
                checkLocaleInList(locale);

                if (data.getExtras().getBoolean(NewLocaleDialog.INTENT_EXTRA_SELECT)) {
                    changeSystemLocale(locale);
                }
            }
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        updateLocaleButtons();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (menuInfo instanceof AdapterContextMenuInfo) {
            int position = ((AdapterContextMenuInfo) menuInfo).position;
            Object o = mListView.getItemAtPosition(position);
            if (o instanceof LocaleInfo) {
                if (((LocaleInfo) o).isCustom()) {
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

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DLG_REMOVE_ID) {
            return createRemoveLocaleDialog();
        }
        return super.onCreateDialog(id);
    }

    //--- private parts ---

    private void setupLocaleList() {
        if (DEBUG) {
            Log.d(TAG, "Update locate list");
        }

        ArrayList<LocaleInfo> data = new ArrayList<LocaleInfo>();

        // Insert all system locales
        String[] locales = getAssets().getLocales();
        for (String locale : locales) {
            if (locale != null && locale.length() > 0) {
                Locale loc = new Locale(locale);
                data.add(new LocaleInfo(locale, loc.getDisplayName()));
            }
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
                data.add(new LocaleInfo(
                                locale,
                                loc.getDisplayName(),
                                true /*custom*/));
            }
        }

        // Sort all locales by code
        Collections.sort(data, new Comparator<LocaleInfo>() {
            public int compare(LocaleInfo lhs, LocaleInfo rhs) {
                return lhs.getLocale().compareTo(rhs.getLocale());
            }
        });

        // Update the list view adapter
        mListView.setAdapter(new ArrayAdapter<LocaleInfo>(
                        this,
                        android.R.layout.simple_list_item_single_choice,
                        data));
        updateLocaleButtons();
    }

    private void changeSystemLocale(String locale) {
        if (ChangeLocale.changeSystemLocale(locale)) {
            Toast.makeText(this,
                            getString(R.string.select_locale_1s, locale),
                            Toast.LENGTH_SHORT)
                 .show();
        }
    }

    private void displayCurrentLocale() {
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            Configuration config = am.getConfiguration();

            if (config.locale != null) {
                String text = String.format("%1$s - %2$s",
                        config.locale.toString(),
                        config.locale.getDisplayName());
                mCurrentLocaleTextView.setText(text);

                checkLocaleInList(config.locale.toString());
            }
        } catch (RemoteException e) {
            if (DEBUG) {
                Log.e(TAG, "get current locale failed", e);
            }
        }
    }

    /** Find the locale by code to select it in the list view */
    private void checkLocaleInList(String locale) {
        ListAdapter a = mListView.getAdapter();
        for (int i = 0; i < a.getCount(); i++) {
            Object o = a.getItem(i);
            if (o instanceof LocaleInfo) {
                String code = ((LocaleInfo) o).getLocale();
                if (code != null && code.equals(locale)) {
                    mListView.setSelection(i);
                    mListView.clearChoices();
                    mListView.setItemChecked(i, true);
                    updateLocaleButtons();
                    break;
                }
            }
        }
    }

    private LocaleInfo getCheckedLocale() {
        int pos = mListView.getCheckedItemPosition();
        ListAdapter a = mListView.getAdapter();
        int n = a.getCount();
        if (pos >= 0 && pos < n) {
            Object o = a.getItem(pos);
            if (o instanceof LocaleInfo) {
                return (LocaleInfo) o;
            }
        }

        return null;
    }

    /** Update the Select/Remove buttons based on the currently checked locale. */
    private void updateLocaleButtons() {
        LocaleInfo info = getCheckedLocale();
        if (info != null) {
            // Enable it
            mSelectLocaleButton.setEnabled(true);
            mSelectLocaleButton.setText(
                getString(R.string.select_locale_1s_button, info.getLocale()));

            // Enable the remove button only for custom locales and set the tag to the locale
            mRemoveLocaleButton.setEnabled(info.isCustom());
        } else {
            // If nothing is selected, disable the buttons
            mSelectLocaleButton.setEnabled(false);
            mSelectLocaleButton.setText(R.string.select_locale_button);

            mRemoveLocaleButton.setEnabled(false);
        }
    }

    /** Invoked by the button "Add new..." */
    private void onAddNewLocale() {
        Intent i = new Intent(CustomLocaleActivity.this, NewLocaleDialog.class);
        startActivityForResult(i, UPDATE_LIST);
    }

    /** Invoked by the button Select / mSelectLocaleButton */
    private void onSelectLocale() {
        LocaleInfo info = getCheckedLocale();
        if (info != null) {
            changeSystemLocale(info.getLocale());
        }
    }

    /**
     * Invoked by the button Remove / mRemoveLocaleButton.
     * Creates a dialog to ask for confirmation before actually remove the custom locale.
     */
    private Dialog createRemoveLocaleDialog() {

        LocaleInfo info = getCheckedLocale();
        final String localeToRemove = info == null ? "<error>" : info.getLocale();

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setMessage(getString(R.string.confirm_remove_locale_1s, localeToRemove));
        b.setCancelable(false);
        b.setPositiveButton(R.string.confirm_remove_locale_yes,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    removeCustomLocale(localeToRemove);
                    dismissDialog(DLG_REMOVE_ID);
                }
        });
        b.setNegativeButton(R.string.confirm_remove_locale_no,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismissDialog(DLG_REMOVE_ID);
                }
        });

        return b.create();
    }

    private void removeCustomLocale(String localeToRemove) {
        // Get current custom locale list
        String oldLocales = mPrefs.getString(CUSTOM_LOCALES, "");

        if (DEBUG) {
            Log.d(TAG, "Remove " + localeToRemove + " from custom locales: " + oldLocales);
        }

        // Update
        StringBuilder sb = new StringBuilder();
        for (String locale : oldLocales.split(CUSTOM_LOCALES_SEP)) {
            if (locale != null && locale.length() > 0 && !locale.equals(localeToRemove)) {
                if (sb.length() > 0) {
                    sb.append(CUSTOM_LOCALES_SEP);
                }
                sb.append(locale);
            }
        }

        String newLocales = sb.toString();
        if (!newLocales.equals(oldLocales)) {
            // Save prefs
            boolean ok = mPrefs.edit().putString(CUSTOM_LOCALES, newLocales).commit();
            if (DEBUG) {
                Log.d(TAG, "Prefs commit:" + Boolean.toString(ok) + ". Saved: " + newLocales);
            }

            Toast.makeText(this,
                            getString(R.string.removed_custom_locale_1s, localeToRemove),
                            Toast.LENGTH_SHORT)
                 .show();

            // Update list view
            setupLocaleList();
        }
    }

    /**
     * Immutable structure that holds the information displayed by a list view item.
     */
    private static class LocaleInfo {
        private final String mLocale;
        private final String mDisplayName;
        private final boolean mIsCustom;

        public LocaleInfo(String locale, String displayName, boolean isCustom) {
            mLocale = locale;
            mDisplayName = displayName;
            mIsCustom = isCustom;
        }

        public LocaleInfo(String locale, String displayName) {
            this(locale, displayName, false /*custom*/);
        }

        public String getLocale() {
            return mLocale;
        }

        public boolean isCustom() {
            return mIsCustom;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(mLocale)
              .append(" - ")
              .append(mDisplayName);
            if (mIsCustom) {
                sb.append(" [Custom]");
            }
            return sb.toString();
        }
    }
}
