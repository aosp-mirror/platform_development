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

package com.android.sdkuilib.repository;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

class ProgressTask extends Dialog
    implements ITaskMonitor             //$hide$ (hide from SWT designer)
    {

    private boolean mCancelRequested;
    private boolean mCloseRequested;
    private boolean mAutomaticallyCloseOnTaskCompletion = true;


    // UI fields
    private Shell mDialogShell;
    private Composite mRootComposite;
    private Label mLabel;
    private ProgressBar mProgressBar;
    private Button mCancelButton;
    private Text mResultText;


    /**
     * Create the dialog.
     * @param parent Parent container
     */
    public ProgressTask(Shell parent) {
        super(parent, SWT.APPLICATION_MODAL);
    }

    /**
     * Open the dialog and blocks till it gets closed
     */
    public void open() {
        createContents();
        mDialogShell.open();
        mDialogShell.layout();
        Display display = getParent().getDisplay();

        startTask();    //$hide$ (hide from SWT designer)

        while (!mDialogShell.isDisposed() && !mCloseRequested) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        if (!mDialogShell.isDisposed()) {
            mDialogShell.close();
        }
    }

    /**
     * Create contents of the dialog.
     */
    private void createContents() {
        mDialogShell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        mDialogShell.setLayout(new GridLayout(1, false));
        mDialogShell.setSize(450, 300);
        mDialogShell.setText(getText());

        mRootComposite = new Composite(mDialogShell, SWT.NONE);
        mRootComposite.setLayout(new GridLayout(2, false));
        mRootComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        mLabel = new Label(mRootComposite, SWT.NONE);
        mLabel.setText("Task");
        mLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        mProgressBar = new ProgressBar(mRootComposite, SWT.NONE);
        mProgressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        mCancelButton = new Button(mRootComposite, SWT.NONE);
        mCancelButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mCancelButton.setText("Cancel");

        mCancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mCancelRequested = true;
                mCancelButton.setEnabled(false);
            }
        });

        mResultText = new Text(mRootComposite,
                SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL | SWT.MULTI);
        mResultText.setEditable(true);
        mResultText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        mResultText.setVisible(false);
    }

    // -- End of UI, Start of internal logic ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    public interface ThreadTask {
        public abstract void PerformTask(ITaskMonitor monitor);
    }

    private ThreadTask mTask;

    /**
     * Creates a new {@link ProgressTask} with the given title.
     * The given task will execute in a separate thread (not the UI thread).
     *
     * This blocks till the thread ends.
     */
    public static ProgressTask start(Shell parent, String title, ThreadTask task) {
        ProgressTask t = new ProgressTask(parent);
        t.setText(title);
        t.setTask(task);
        t.open();
        return t;
    }

    /**
     * Sets the description in the current task dialog.
     * This method can be invoke from a non-UI thread.
     */
    public void setDescription(final String description) {
        mDialogShell.getDisplay().asyncExec(new Runnable() {
            public void run() {
                if (!mLabel.isDisposed()) {
                    mLabel.setText(description);
                }
            }
        });
    }

    /**
     * Sets the description in the current task dialog.
     * This method can be invoke from a non-UI thread.
     */
    public void setResult(final String result) {
        mAutomaticallyCloseOnTaskCompletion = false;
        mDialogShell.getDisplay().asyncExec(new Runnable() {
            public void run() {
                if (!mResultText.isDisposed()) {
                    mResultText.setVisible(true);
                    mResultText.setText(result);
                }
            }
        });
    }

    /**
     * Sets the max value of the progress bar.
     * This method can be invoke from a non-UI thread.
     *
     * @see ProgressBar#setMaximum(int)
     */
    public void setProgressMax(final int max) {
        mDialogShell.getDisplay().asyncExec(new Runnable() {
            public void run() {
                if (!mProgressBar.isDisposed()) {
                    mProgressBar.setMaximum(max);
                }
            }
        });
    }

    /**
     * Increments the current value of the progress bar.
     *
     * This method can be invoked from a non-UI thread.
     */
    public void incProgress(final int delta) {
        mDialogShell.getDisplay().asyncExec(new Runnable() {
            public void run() {
                if (!mProgressBar.isDisposed()) {
                    mProgressBar.setSelection(mProgressBar.getSelection() + delta);
                }
            }
        });
    }

    /**
     * Returns true if the "Cancel" button was selected.
     * It is up to the task thread to pool this and exit.
     */
    public boolean cancelRequested() {
        return mCancelRequested;
    }

    /** Sets the task that will execute in a separate thread. */
    private void setTask(ThreadTask task) {
        mTask = task;
    }

    /**
     * Starts the task from {@link #setTask(ThreadTask)} in a separate thread.
     * When the task completes, set {@link #mCloseRequested} to end the dialog loop.
     */
    private void startTask() {
        if (mTask != null) {
            new Thread(getText()) {
                @Override
                public void run() {
                    mTask.PerformTask(ProgressTask.this);
                    if (mAutomaticallyCloseOnTaskCompletion) {
                        mCloseRequested = true;
                    }
                }
            }.start();
        }
    }

    // End of hiding from SWT Designer
    //$hide<<$
}
