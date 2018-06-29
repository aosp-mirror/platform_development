/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.dumpviewer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.android.dumpviewer.R.id;
import com.android.dumpviewer.pickers.PackageNamePicker;
import com.android.dumpviewer.pickers.PickerActivity;
import com.android.dumpviewer.pickers.ProcessNamePicker;
import com.android.dumpviewer.utils.Exec;
import com.android.dumpviewer.utils.GrepHelper;
import com.android.dumpviewer.utils.History;
import com.android.dumpviewer.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class DumpActivity extends AppCompatActivity {
    public static final String TAG = "DumpViewer";

    private static final int MAX_HISTORY_SIZE = 32;
    private static final String SHARED_PREF_NAME = "prefs";

    private static final int MAX_RESULT_SIZE = 1 * 1024 * 1024;

    private static final int CODE_MULTI_PICKER = 1;

    private final Handler mHandler = new Handler();

    private WebView mWebView;
    private AutoCompleteTextView mAcCommandLine;
    private AutoCompleteTextView mAcBeforeContext;
    private AutoCompleteTextView mAcAfterContext;
    private AutoCompleteTextView mAcHead;
    private AutoCompleteTextView mAcTail;
    private AutoCompleteTextView mAcPattern;
    private AutoCompleteTextView mAcSearchQuery;
    private CheckBox mIgnoreCaseGrep;
    private CheckBox mShowLast;

    private Button mExecuteButton;
    private Button mNextButton;
    private Button mPrevButton;

    private Button mOpenButton;
    private Button mCloseButton;

    private Button mMultiPickerButton;
    private Button mRePickerButton;

    private ViewGroup mHeader1;

    private AsyncTask<Void, Void, String> mRunningTask;

    private SharedPreferences mPrefs;
    private History mCommandHistory;
    private History mRegexpHistory;
    private History mSearchHistory;

    private long mLastCollapseTime;

    private GrepHelper mGrepHelper;

    private static final List<String> DEFAULT_COMMANDS = Arrays.asList(new String[]{
            "dumpsys activity",
            "dumpsys activity activities",
            "dumpsys activity broadcasts",
            "dumpsys activity broadcasts history",
            "dumpsys activity services",
            "dumpsys activity starter",
            "dumpsys activity processes",
            "dumpsys activity recents",
            "dumpsys activity lastanr-traces",
            "dumpsys alarm",
            "dumpsys appops",
            "dumpsys backup",
            "dumpsys battery",
            "dumpsys bluetooth_manager",
            "dumpsys content",
            "dumpsys deviceidle",
            "dumpsys device_policy",
            "dumpsys jobscheduler",
            "dumpsys location",
            "dumpsys meminfo -a",
            "dumpsys netpolicy",
            "dumpsys notification",
            "dumpsys package",
            "dumpsys power",
            "dumpsys procstats",
            "dumpsys settings",
            "dumpsys shortcut",
            "dumpsys usagestats",
            "dumpsys user",

            "dumpsys activity service com.android.systemui/.SystemUIService",
            "dumpsys activity provider com.android.providers.contacts/.ContactsProvider2",
            "dumpsys activity provider com.android.providers.contacts/.CallLogProvider",
            "dumpsys activity provider com.android.providers.calendar.CalendarProvider2",

            "logcat -v uid -b main",
            "logcat -v uid -b all",
            "logcat -v uid -b system",
            "logcat -v uid -b crash",
            "logcat -v uid -b radio",
            "logcat -v uid -b events"
    });

    private InputMethodManager mImm;

    private EditText mLastFocusedEditBox;

    private boolean mDoScrollWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dump);

        mGrepHelper = GrepHelper.getHelper();

        mImm = getSystemService(InputMethodManager.class);

        ((TextView) findViewById(R.id.grep_label)).setText(mGrepHelper.getCommandName());

        mWebView = findViewById(R.id.webview);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);

        mHeader1 = findViewById(R.id.header1);

        mExecuteButton = findViewById(R.id.start);
        mExecuteButton.setOnClickListener(this::onStartClicked);
        mNextButton = findViewById(R.id.find_next);
        mNextButton.setOnClickListener(this::onFindNextClicked);
        mPrevButton = findViewById(R.id.find_prev);
        mPrevButton.setOnClickListener(this::onFindPrevClicked);

        mOpenButton = findViewById(R.id.open_header);
        mOpenButton.setOnClickListener(this::onOpenHeaderClicked);
        mCloseButton = findViewById(R.id.close_header);
        mCloseButton.setOnClickListener(this::onCloseHeaderClicked);

        mMultiPickerButton = findViewById(id.multi_picker);
        mMultiPickerButton.setOnClickListener(this::onMultiPickerClicked);
        mRePickerButton = findViewById(id.re_picker);
        mRePickerButton.setOnClickListener(this::onRePickerClicked);

        mAcCommandLine = findViewById(R.id.commandline);
        mAcAfterContext = findViewById(R.id.afterContext);
        mAcBeforeContext = findViewById(R.id.beforeContext);
        mAcHead = findViewById(R.id.head);
        mAcTail = findViewById(R.id.tail);
        mAcPattern = findViewById(R.id.pattern);
        mAcSearchQuery = findViewById(R.id.search);

        mIgnoreCaseGrep = findViewById(R.id.ignore_case);
        mShowLast = findViewById(R.id.scroll_to_bottm);

        mPrefs = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        mCommandHistory = new History(mPrefs, "command_history", MAX_HISTORY_SIZE);
        mCommandHistory.load();
        mRegexpHistory = new History(mPrefs, "regexp_history", MAX_HISTORY_SIZE);
        mRegexpHistory.load();
        mSearchHistory = new History(mPrefs, "search_history", MAX_HISTORY_SIZE);
        mSearchHistory.load();

        setupAutocomplete(mAcBeforeContext, "0", "1", "2", "3", "5", "10");
        setupAutocomplete(mAcAfterContext, "0", "1", "2", "3", "5", "10");
        setupAutocomplete(mAcHead, "0", "100", "1000", "2000");
        setupAutocomplete(mAcTail, "0", "100", "1000", "2000");

        mAcCommandLine.setOnKeyListener(this::onAutocompleteKey);
        mAcPattern.setOnKeyListener(this::onAutocompleteKey);
        mAcSearchQuery.setOnKeyListener(this::onAutocompleteKey);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Apparently we need a small delay for it to work.
                mHandler.postDelayed(DumpActivity.this::onContentLoaded, 200);
            }
        });
        refreshHistory();

        loadSharePrefs();

        refreshUi();
    }

    private void refreshUi() {
        final boolean canExecute = getCommandLine().length() > 0;
        final boolean canSearch = mAcSearchQuery.getText().length() > 0;

        mExecuteButton.setEnabled(canExecute);
        mNextButton.setEnabled(canSearch);
        mPrevButton.setEnabled(canSearch);

        if (mHeader1.getVisibility() == View.VISIBLE) {
            mCloseButton.setVisibility(View.VISIBLE);
            mOpenButton.setVisibility(View.GONE);
        } else {
            mOpenButton.setVisibility(View.VISIBLE);
            mCloseButton.setVisibility(View.GONE);
        }
    }

    private void saveSharePrefs() {
    }

    private void loadSharePrefs() {
    }

    @Override
    protected void onPause() {
        saveSharePrefs();
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == CODE_MULTI_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                insertPickedString(PickerActivity.getSelectedString(data));
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setupAutocomplete(AutoCompleteTextView target, List<String> values) {
        setupAutocomplete(target, values.toArray(new String[values.size()]));
    }

    private void setupAutocomplete(AutoCompleteTextView target, String... values) {
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.dropdown_item_1, values);
        target.setAdapter(adapter);
        target.setOnClickListener((v) -> ((AutoCompleteTextView) v).showDropDown());
        target.setOnFocusChangeListener(this::onAutocompleteFocusChanged);
        target.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        refreshUi();
                    }
                });
    }

    public boolean onAutocompleteKey(View view, int keyCode, KeyEvent keyevent) {
        if (keyevent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
            if (view == mAcSearchQuery) {
                doFindNextOrPrev(true);
            } else {
                doStartCommand();
            }
            return true;
        }
        return false;
    }

    private void onAutocompleteFocusChanged(View v, boolean hasFocus) {
        if ((System.currentTimeMillis() - mLastCollapseTime) < 300) {
            // Hack: We don't want to open the pop up because the focus changed because of
            // collapsing, so we suppress it.
            return;
        }
        if (hasFocus) {
            if (v == mAcCommandLine || v == mAcPattern || v == mAcSearchQuery) {
                mLastFocusedEditBox = (EditText) v;
            }
            final AutoCompleteTextView target = (AutoCompleteTextView) v;
            Utils.sMainHandler.post(() -> {
                if (!isDestroyed() && !target.isPopupShowing()) {
                    try {
                        target.showDropDown();
                    } catch (Exception e) {
                    }
                }
            });
        }
    }

    private void insertPickedString(String s) {
        insertText((mLastFocusedEditBox != null ? mLastFocusedEditBox :  mAcCommandLine), s);
    }

    private void hideIme() {
        mImm.hideSoftInputFromWindow(mAcCommandLine.getWindowToken(), 0);
    }

    private void refreshHistory() {
        // Command line autocomplete.
        final List<String> commands = new ArrayList<>(128);
        mCommandHistory.addAllTo(commands);
        commands.addAll(DEFAULT_COMMANDS);

        setupAutocomplete(mAcCommandLine, commands);

        // Regexp autocomplete
        final List<String> patterns = new ArrayList<>(MAX_HISTORY_SIZE);
        mRegexpHistory.addAllTo(patterns);
        setupAutocomplete(mAcPattern, patterns);

        // Search autocomplete
        final List<String> queries = new ArrayList<>(MAX_HISTORY_SIZE);
        mSearchHistory.addAllTo(queries);
        setupAutocomplete(mAcSearchQuery, queries);
    }

    private String getCommandLine() {
        return mAcCommandLine.getText().toString().trim();
    }

    private void setText(String format, Object... args) {
        mHandler.post(() -> setText(String.format(format, args)));
    }

    private void setText(String text) {
        Log.v(TAG, "Trying to set string to webview: length=" + text.length());
        mHandler.post(() -> {
            // TODO Don't do it on the main thread.
            final StringBuilder sb = new StringBuilder(text.length() * 2);
            sb.append("<html><body");
            sb.append(" style=\"white-space: nowrap;\"");
            sb.append("><pre>\n");
            char c;
            for (int i = 0; i < text.length(); i++) {
                c = text.charAt(i);
                switch (c) {
                    case '\n':
                        sb.append("<br>");
                        break;
                    case '<':
                        sb.append("&lt;");
                        break;
                    case '>':
                        sb.append("&gt;");
                        break;
                    case '&':
                        sb.append("&amp;");
                        break;
                    case '\'':
                        sb.append("&#39;");
                        break;
                    case '"':
                        sb.append("&quot;");
                        break;
                    case '#':
                        sb.append("%23");
                        break;
                    default:
                        sb.append(c);
                }
            }
            sb.append("</pre></body></html>\n");

            mWebView.loadData(sb.toString(), "text/html", null);
        });
    }

    private void insertText(EditText edit, String value) {
        final int start = Math.max(edit.getSelectionStart(), 0);
        final int end = Math.max(edit.getSelectionEnd(), 0);
        edit.getText().replace(Math.min(start, end), Math.max(start, end),
                value, 0, value.length());
    }

    private void onContentLoaded() {
        if (!mDoScrollWebView) {
            return;
        }
        mDoScrollWebView = false;
        if (mShowLast == null) {
            return;
        }
        if (mShowLast.isChecked()) {
            mWebView.pageDown(true /* toBottom */);
        } else {
            mWebView.pageUp(true /* toTop */);
        }
    }

    public void onFindNextClicked(View v) {
        doFindNextOrPrev(true);
    }

    public void onFindPrevClicked(View v) {
        doFindNextOrPrev(false);
    }

    private void onOpenHeaderClicked(View v) {
        toggleHeader();
    }

    private void onCloseHeaderClicked(View v) {
        mLastCollapseTime = System.currentTimeMillis();
        toggleHeader();
    }

    private void toggleHeader() {
        mHeader1.setVisibility(mHeader1.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        refreshUi();
    }

    private void onRePickerClicked(View view) {
        showRegexpPicker();
    }

    private void onMultiPickerClicked(View view) {
        showMultiPicker();
    }

    String mLastQuery;

    private void doFindNextOrPrev(boolean next) {
        final String query = mAcSearchQuery.getText().toString();
        if (query.length() == 0) {
            return;
        }
        hideIme();

        mSearchHistory.add(query);

        if (query.equals(mLastQuery)) {
            mWebView.findNext(next);
        } else {
            mWebView.findAllAsync(query);
        }
        mLastQuery = query;
    }

    public void onStartClicked(View v) {
        doStartCommand();
    }

    public void doStartCommand() {
        if (mRunningTask != null) {
            mRunningTask.cancel(true);
        }
        final String command = getCommandLine();
        if (command.length() > 0) {
            startCommand(command);
        }
    }

    private void startCommand(String command) {
        hideIme();

        mCommandHistory.add(command);
        mRegexpHistory.add(mAcPattern.getText().toString().trim());
        (mRunningTask = new Dumper(command)).execute();
        refreshHistory();
    }

    private class Dumper extends AsyncTask<Void, Void, String> {
        final String command;
        final AtomicBoolean mTimedOut = new AtomicBoolean();

        public Dumper(String command) {
            this.command = command;
        }

        @Override
        protected String doInBackground(Void... voids) {
            if (Settings.Global.getInt(getContentResolver(), Global.ADB_ENABLED, 0) != 1) {
                return "Please enable ADB (aka \"USB Debugging\" in developer options)";
            }

            final ByteArrayOutputStream out = new ByteArrayOutputStream(1024 * 1024);
            try {
                try (InputStream is = Exec.runForStream(
                        buildCommandLine(command),
                        DumpActivity.this::setText,
                        () -> mTimedOut.set(true),
                        (e) -> {throw new RuntimeException(e.getMessage(), e);},
                        30)) {
                    final byte[] buf = new byte[1024 * 16];
                    int read;
                    int written = 0;
                    while ((read = is.read(buf)) >= 0) {
                        out.write(buf, 0, read);
                        written += read;
                        if (written >= MAX_RESULT_SIZE) {
                            out.write("\n[Result too long; omitted]".getBytes());
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                if (mTimedOut.get()) {
                    setText("Command timed out");
                } else {
                    setText("Caught exception: %s\n%s", e.getMessage(),
                            Log.getStackTraceString(e));
                }
                return null;
            }

            return out.toString();
        }

        @Override
        protected void onCancelled(String s) {
            mRunningTask = null;
        }

        @Override
        protected void onPostExecute(String s) {
            mRunningTask = null;
            if (s != null) {
                if (s.length() == 0) {
                    setText("[No result]");
                } else {
                    mDoScrollWebView = true;
                    setText(s);
                }
            }
        }

    }

    private static final Pattern sLogcat = Pattern.compile("^logcat(\\s|$)");

    private String buildCommandLine(String command) {
        final StringBuilder sb = new StringBuilder(128);
        if (sLogcat.matcher(command).find()) {
            // Make sure logcat command always has -d.
            sb.append("logcat -d ");
            sb.append(command.substring(7));
        } else {
            sb.append(command);
        }

        final int before = Utils.parseInt(mAcBeforeContext.getText().toString(), 0);
        final int after = Utils.parseInt(mAcAfterContext.getText().toString(), 0);
        final int head = Utils.parseInt(mAcHead.getText().toString(), 0);
        final int tail = Utils.parseInt(mAcTail.getText().toString(), 0);

        // Don't trim regexp. Sometimes you want to search for spaces.
        final String regexp = mAcPattern.getText().toString();
        final boolean ignoreCase = mIgnoreCaseGrep.isChecked();

        if (regexp.length() > 0) {
            sb.append(" | ");
            mGrepHelper.buildCommand(sb, regexp, before, after, ignoreCase);
        }
        if (head > 0) {
            sb.append(" | head -n ");
            sb.append(head);
        }
        if (tail > 0) {
            sb.append(" | tail -n ");
            sb.append(tail);
        }
        sb.append(" 2>&1");
        return sb.toString();
    }

    // Show regex picker
    private void showRegexpPicker() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
        builderSingle.setTitle("Insert meta character");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.select_dialog_item, mGrepHelper.getMetaCharacters());

        builderSingle.setNegativeButton("cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        builderSingle.setAdapter(arrayAdapter, (dialog, which) -> {
            final String item = arrayAdapter.getItem(which);
            // Only use the first token
            final String[] vals = item.split(" ");

            insertText(mAcPattern, vals[0]);
            dialog.dismiss();
        });
        builderSingle.show();
    }

    private static final String[] sMultiPickerTargets = {
            "Package name",
//            "Process name", // Not implemented yet.
    };

    private void showMultiPicker() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
        builderSingle.setTitle("Find and insert...");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.select_dialog_item, sMultiPickerTargets);

        builderSingle.setNegativeButton("cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        builderSingle.setAdapter(arrayAdapter, (dialog, which) -> {
            Class<?> activity;
            switch (which) {
                case 0:
                    activity = PackageNamePicker.class;
                    break;
                case 1:
                    activity = ProcessNamePicker.class;
                    break;
                default:
                    throw new RuntimeException("BUG: Unknown item selected");
            }
            final Intent i = new Intent().setComponent(new ComponentName(this, activity));
            startActivityForResult(i, CODE_MULTI_PICKER);

            dialog.dismiss();
        });
        builderSingle.show();
    }
}
