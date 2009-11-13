/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ddmuilib;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.RawImage;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import java.io.IOException;


/**
 * Gather a screen shot from the device and save it to a file.
 */
public class ScreenShotDialog extends Dialog {

    private Label mBusyLabel;
    private Label mImageLabel;
    private Button mSave;
    private IDevice mDevice;
    private RawImage mRawImage;
    private Clipboard mClipboard;


    /**
     * Create with default style.
     */
    public ScreenShotDialog(Shell parent) {
        this(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        mClipboard = new Clipboard(parent.getDisplay());
    }

    /**
     * Create with app-defined style.
     */
    public ScreenShotDialog(Shell parent, int style) {
        super(parent, style);
    }

    /**
     * Prepare and display the dialog.
     * @param device The {@link IDevice} from which to get the screenshot.
     */
    public void open(IDevice device) {
        mDevice = device;

        Shell parent = getParent();
        Shell shell = new Shell(parent, getStyle());
        shell.setText("Device Screen Capture");

        createContents(shell);
        shell.pack();
        shell.open();

        updateDeviceImage(shell);

        Display display = parent.getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }

    }

    /*
     * Create the screen capture dialog contents.
     */
    private void createContents(final Shell shell) {
        GridData data;

        final int colCount = 5;

        shell.setLayout(new GridLayout(colCount, true));

        // "refresh" button
        Button refresh = new Button(shell, SWT.PUSH);
        refresh.setText("Refresh");
        data = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        data.widthHint = 80;
        refresh.setLayoutData(data);
        refresh.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateDeviceImage(shell);
            }
        });

        // "rotate" button
        Button rotate = new Button(shell, SWT.PUSH);
        rotate.setText("Rotate");
        data = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        data.widthHint = 80;
        rotate.setLayoutData(data);
        rotate.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (mRawImage != null) {
                    mRawImage = mRawImage.getRotated();
                    updateImageDisplay(shell);
                }
            }
        });

        // "save" button
        mSave = new Button(shell, SWT.PUSH);
        mSave.setText("Save");
        data = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        data.widthHint = 80;
        mSave.setLayoutData(data);
        mSave.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                saveImage(shell);
            }
        });

        Button copy = new Button(shell, SWT.PUSH);
        copy.setText("Copy");
        copy.setToolTipText("Copy the screenshot to the clipboard");
        data = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        data.widthHint = 80;
        copy.setLayoutData(data);
        copy.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                copy();
            }
        });


        // "done" button
        Button done = new Button(shell, SWT.PUSH);
        done.setText("Done");
        data = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        data.widthHint = 80;
        done.setLayoutData(data);
        done.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                shell.close();
            }
        });

        // title/"capturing" label
        mBusyLabel = new Label(shell, SWT.NONE);
        mBusyLabel.setText("Preparing...");
        data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        data.horizontalSpan = colCount;
        mBusyLabel.setLayoutData(data);

        // space for the image
        mImageLabel = new Label(shell, SWT.BORDER);
        data = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        data.horizontalSpan = colCount;
        mImageLabel.setLayoutData(data);
        Display display = shell.getDisplay();
        mImageLabel.setImage(ImageHelper.createPlaceHolderArt(
                display, 50, 50, display.getSystemColor(SWT.COLOR_BLUE)));


        shell.setDefaultButton(done);
    }

    /**
     * Copies the content of {@link #mImageLabel} to the clipboard.
     */
    private void copy() {
        mClipboard.setContents(
                new Object[] {
                        mImageLabel.getImage().getImageData()
                }, new Transfer[] {
                        ImageTransfer.getInstance()
                });
    }

    /**
     * Captures a new image from the device, and display it.
     */
    private void updateDeviceImage(Shell shell) {
        mBusyLabel.setText("Capturing...");     // no effect

        shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));

        mRawImage = getDeviceImage();

        updateImageDisplay(shell);
    }

    /**
     * Updates the display with {@link #mRawImage}.
     * @param shell
     */
    private void updateImageDisplay(Shell shell) {
        Image image;
        if (mRawImage == null) {
            Display display = shell.getDisplay();
            image = ImageHelper.createPlaceHolderArt(
                    display, 320, 240, display.getSystemColor(SWT.COLOR_BLUE));

            mSave.setEnabled(false);
            mBusyLabel.setText("Screen not available");
        } else {
            // convert raw data to an Image.
            PaletteData palette = new PaletteData(
                    mRawImage.getRedMask(),
                    mRawImage.getGreenMask(),
                    mRawImage.getBlueMask());

            ImageData imageData = new ImageData(mRawImage.width, mRawImage.height,
                    mRawImage.bpp, palette, 1, mRawImage.data);
            image = new Image(getParent().getDisplay(), imageData);

            mSave.setEnabled(true);
            mBusyLabel.setText("Captured image:");
        }

        mImageLabel.setImage(image);
        mImageLabel.pack();
        shell.pack();

        // there's no way to restore old cursor; assume it's ARROW
        shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
    }

    /**
     * Grabs an image from an ADB-connected device and returns it as a {@link RawImage}.
     */
    private RawImage getDeviceImage() {
        try {
            return mDevice.getScreenshot();
        }
        catch (IOException ioe) {
            Log.w("ddms", "Unable to get frame buffer: " + ioe.getMessage());
            return null;
        }
    }

    /*
     * Prompt the user to save the image to disk.
     */
    private void saveImage(Shell shell) {
        FileDialog dlg = new FileDialog(shell, SWT.SAVE);
        String fileName;

        dlg.setText("Save image...");
        dlg.setFileName("device.png");
        dlg.setFilterPath(DdmUiPreferences.getStore().getString("lastImageSaveDir"));
        dlg.setFilterNames(new String[] {
            "PNG Files (*.png)"
        });
        dlg.setFilterExtensions(new String[] {
            "*.png" //$NON-NLS-1$
        });

        fileName = dlg.open();
        if (fileName != null) {
            DdmUiPreferences.getStore().setValue("lastImageSaveDir", dlg.getFilterPath());

            Log.d("ddms", "Saving image to " + fileName);
            ImageData imageData = mImageLabel.getImage().getImageData();

            try {
                ImageLoader loader = new ImageLoader();
                loader.data = new ImageData[] { imageData };
                loader.save(fileName, SWT.IMAGE_PNG);
            }
            catch (SWTException e) {
                Log.w("ddms", "Unable to save " + fileName + ": " + e.getMessage());
            }
        }
    }

}

