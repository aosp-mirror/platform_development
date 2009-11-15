/* //device/tools/ddms/src/com/android/ddms/DeviceCommandDialog.java
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package com.android.ddms;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.Log;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;


/**
 * Execute a command on an ADB-attached device and save the output.
 *
 * There are several ways to do this.  One is to run a single command
 * and show the output.  Another is to have several possible commands and
 * let the user click a button next to the one (or ones) they want.  This
 * currently uses the simple 1:1 form.
 */
public class DeviceCommandDialog extends Dialog {

    public static final int DEVICE_STATE = 0;
    public static final int APP_STATE = 1;
    public static final int RADIO_STATE = 2;
    public static final int LOGCAT = 3;

    private String mCommand;
    private String mFileName;

    private Label mStatusLabel;
    private Button mCancelDone;
    private Button mSave;
    private Text mText;
    private Font mFont = null;
    private boolean mCancel;
    private boolean mFinished;


    /**
     * Create with default style.
     */
    public DeviceCommandDialog(String command, String fileName, Shell parent) {
        // don't want a close button, but it seems hard to get rid of on GTK
        // keep it on all platforms for consistency
        this(command, fileName, parent,
            SWT.DIALOG_TRIM | SWT.BORDER | SWT.APPLICATION_MODAL | SWT.RESIZE);
    }

    /**
     * Create with app-defined style.
     */
    public DeviceCommandDialog(String command, String fileName, Shell parent,
        int style)
    {
        super(parent, style);
        mCommand = command;
        mFileName = fileName;
    }

    /**
     * Prepare and display the dialog.
     * @param currentDevice
     */
    public void open(IDevice currentDevice) {
        Shell parent = getParent();
        Shell shell = new Shell(parent, getStyle());
        shell.setText("Remote Command");

        mFinished = false;
        mFont = findFont(shell.getDisplay());
        createContents(shell);

        // Getting weird layout behavior under Linux when Text is added --
        // looks like text widget has min width of 400 when FILL_HORIZONTAL
        // is used, and layout gets tweaked to force this.  (Might be even
        // more with the scroll bars in place -- it wigged out when the
        // file save dialog was invoked.)
        shell.setMinimumSize(500, 200);
        shell.setSize(800, 600);
        shell.open();

        executeCommand(shell, currentDevice);

        Display display = parent.getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }

        if (mFont != null)
            mFont.dispose();
    }

    /*
     * Create a text widget to show the output and some buttons to
     * manage things.
     */
    private void createContents(final Shell shell) {
        GridData data;

        shell.setLayout(new GridLayout(2, true));

        shell.addListener(SWT.Close, new Listener() {
            public void handleEvent(Event event) {
                if (!mFinished) {
                    Log.d("ddms", "NOT closing - cancelling command");
                    event.doit = false;
                    mCancel = true;
                }
            }
        });

        mStatusLabel = new Label(shell, SWT.NONE);
        mStatusLabel.setText("Executing '" + shortCommandString() + "'");
        data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        data.horizontalSpan = 2;
        mStatusLabel.setLayoutData(data);

        mText = new Text(shell, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        mText.setEditable(false);
        mText.setFont(mFont);
        data = new GridData(GridData.FILL_BOTH);
        data.horizontalSpan = 2;
        mText.setLayoutData(data);

        // "save" button
        mSave = new Button(shell, SWT.PUSH);
        mSave.setText("Save");
        data = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        data.widthHint = 80;
        mSave.setLayoutData(data);
        mSave.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                saveText(shell);
            }
        });
        mSave.setEnabled(false);

        // "cancel/done" button
        mCancelDone = new Button(shell, SWT.PUSH);
        mCancelDone.setText("Cancel");
        data = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        data.widthHint = 80;
        mCancelDone.setLayoutData(data);
        mCancelDone.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!mFinished)
                    mCancel = true;
                else
                    shell.close();
            }
        });
    }

    /*
     * Figure out what font to use.
     *
     * Returns "null" if we can't figure it out, which SWT understands to
     * mean "use default system font".
     */
    private Font findFont(Display display) {
        String fontStr = PrefsDialog.getStore().getString("textOutputFont");
        if (fontStr != null) {
            FontData fdat = new FontData(fontStr);
            if (fdat != null)
                return new Font(display, fdat);
        }
        return null;
    }


    /*
     * Callback class for command execution.
     */
    class Gatherer extends Thread implements IShellOutputReceiver {
        public static final int RESULT_UNKNOWN = 0;
        public static final int RESULT_SUCCESS = 1;
        public static final int RESULT_FAILURE = 2;
        public static final int RESULT_CANCELLED = 3;

        private Shell mShell;
        private String mCommand;
        private Text mText;
        private int mResult;
        private IDevice mDevice;

        /**
         * Constructor; pass in the text widget that will receive the output.
         * @param device
         */
        public Gatherer(Shell shell, IDevice device, String command, Text text) {
            mShell = shell;
            mDevice = device;
            mCommand = command;
            mText = text;
            mResult = RESULT_UNKNOWN;

            // this is in outer class
            mCancel = false;
        }

        /**
         * Thread entry point.
         */
        @Override
        public void run() {

            if (mDevice == null) {
                Log.w("ddms", "Cannot execute command: no device selected.");
                mResult = RESULT_FAILURE;
            } else {
                try {
                    mDevice.executeShellCommand(mCommand, this);
                    if (mCancel)
                        mResult = RESULT_CANCELLED;
                    else
                        mResult = RESULT_SUCCESS;
                }
                catch (IOException ioe) {
                    Log.w("ddms", "Remote exec failed: " + ioe.getMessage());
                    mResult = RESULT_FAILURE;
                }
            }

            mShell.getDisplay().asyncExec(new Runnable() {
                public void run() {
                    updateForResult(mResult);
                }
            });
        }

        /**
         * Called by executeRemoteCommand().
         */
        public void addOutput(byte[] data, int offset, int length) {

            Log.v("ddms", "received " + length + " bytes");
            try {
                final String text;
                text = new String(data, offset, length, "ISO-8859-1");

                // add to text widget; must do in UI thread
                mText.getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        mText.append(text);
                    }
                });
            }
            catch (UnsupportedEncodingException uee) {
                uee.printStackTrace();      // not expected
            }
        }

        public void flush() {
            // nothing to flush.
        }

        /**
         * Called by executeRemoteCommand().
         */
        public boolean isCancelled() {
            return mCancel;
        }
    };

    /*
     * Execute a remote command, add the output to the text widget, and
     * update controls.
     *
     * We have to run the command in a thread so that the UI continues
     * to work.
     */
    private void executeCommand(Shell shell, IDevice device) {
        Gatherer gath = new Gatherer(shell, device, commandString(), mText);
        gath.start();
    }

    /*
     * Update the controls after the remote operation completes.  This
     * must be called from the UI thread.
     */
    private void updateForResult(int result) {
        if (result == Gatherer.RESULT_SUCCESS) {
            mStatusLabel.setText("Successfully executed '"
                + shortCommandString() + "'");
            mSave.setEnabled(true);
        } else if (result == Gatherer.RESULT_CANCELLED) {
            mStatusLabel.setText("Execution cancelled; partial results below");
            mSave.setEnabled(true);     // save partial
        } else if (result == Gatherer.RESULT_FAILURE) {
            mStatusLabel.setText("Failed");
        }
        mStatusLabel.pack();
        mCancelDone.setText("Done");
        mFinished = true;
    }

    /*
     * Allow the user to save the contents of the text dialog.
     */
    private void saveText(Shell shell) {
        FileDialog dlg = new FileDialog(shell, SWT.SAVE);
        String fileName;

        dlg.setText("Save output...");
        dlg.setFileName(defaultFileName());
        dlg.setFilterPath(PrefsDialog.getStore().getString("lastTextSaveDir"));
        dlg.setFilterNames(new String[] {
            "Text Files (*.txt)"
        });
        dlg.setFilterExtensions(new String[] {
            "*.txt"
        });

        fileName = dlg.open();
        if (fileName != null) {
            PrefsDialog.getStore().setValue("lastTextSaveDir",
                                            dlg.getFilterPath());

            Log.d("ddms", "Saving output to " + fileName);

            /*
             * Convert to 8-bit characters.
             */
            String text = mText.getText();
            byte[] ascii;
            try {
                ascii = text.getBytes("ISO-8859-1");
            }
            catch (UnsupportedEncodingException uee) {
                uee.printStackTrace();
                ascii = new byte[0];
            }

            /*
             * Output data, converting CRLF to LF.
             */
            try {
                int length = ascii.length;

                FileOutputStream outFile = new FileOutputStream(fileName);
                BufferedOutputStream out = new BufferedOutputStream(outFile);
                for (int i = 0; i < length; i++) {
                    if (i < length-1 &&
                        ascii[i] == 0x0d && ascii[i+1] == 0x0a)
                    {
                        continue;
                    }
                    out.write(ascii[i]);
                }
                out.close();        // flush buffer, close file
            }
            catch (IOException ioe) {
                Log.w("ddms", "Unable to save " + fileName + ": " + ioe);
            }
        }
    }


    /*
     * Return the shell command we're going to use.
     */
    private String commandString() {
        return mCommand;

    }

    /*
     * Return a default filename for the "save" command.
     */
    private String defaultFileName() {
        return mFileName;
    }

    /*
     * Like commandString(), but length-limited.
     */
    private String shortCommandString() {
        String str = commandString();
        if (str.length() > 50)
            return str.substring(0, 50) + "...";
        else
            return str;
    }
}

