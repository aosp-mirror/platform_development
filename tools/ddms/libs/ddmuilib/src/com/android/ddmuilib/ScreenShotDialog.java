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

import com.android.ddmlib.Device;
import com.android.ddmlib.Log;
import com.android.ddmlib.RawImage;

import org.eclipse.swt.SWT;
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
    private Device mDevice;


    /**
     * Create with default style.
     */
    public ScreenShotDialog(Shell parent) {
        this(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    }

    /**
     * Create with app-defined style.
     */
    public ScreenShotDialog(Shell parent, int style) {
        super(parent, style);
    }

    /**
     * Prepare and display the dialog.
     * @param device The {@link Device} from which to get the screenshot.
     */
    public void open(Device device) {
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

        shell.setLayout(new GridLayout(3, true));

        // title/"capturing" label
        mBusyLabel = new Label(shell, SWT.NONE);
        mBusyLabel.setText("Preparing...");
        data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        data.horizontalSpan = 3;
        mBusyLabel.setLayoutData(data);

        // space for the image
        mImageLabel = new Label(shell, SWT.BORDER);
        data = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        data.horizontalSpan = 3;
        mImageLabel.setLayoutData(data);
        Display display = shell.getDisplay();
        mImageLabel.setImage(ImageHelper.createPlaceHolderArt(display, 50, 50, display.getSystemColor(SWT.COLOR_BLUE)));

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

        shell.setDefaultButton(done);
    }

    /*
     * Capture a new image from the device.
     */
    private void updateDeviceImage(Shell shell) {
        mBusyLabel.setText("Capturing...");     // no effect

        shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));

        Image image = getDeviceImage();
        if (image == null) {
            Display display = shell.getDisplay();
            image = ImageHelper.createPlaceHolderArt(display, 320, 240, display.getSystemColor(SWT.COLOR_BLUE));
            mSave.setEnabled(false);
            mBusyLabel.setText("Screen not available");
        } else {
            mSave.setEnabled(true);
            mBusyLabel.setText("Captured image:");
        }

        mImageLabel.setImage(image);
        mImageLabel.pack();
        shell.pack();

        // there's no way to restore old cursor; assume it's ARROW
        shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
    }

    /*
     * Grab an image from an ADB-connected device.
     */
    private Image getDeviceImage() {
        RawImage rawImage;

        try {
            rawImage = mDevice.getScreenshot();
        }
        catch (IOException ioe) {
            Log.w("ddms", "Unable to get frame buffer: " + ioe.getMessage());
            return null;
        }

        // device/adb not available?
        if (rawImage == null)
            return null;

        // convert raw data to an Image
        assert rawImage.bpp == 16;
        PaletteData palette = new PaletteData(0xf800, 0x07e0, 0x001f);
        ImageData imageData = new ImageData(rawImage.width, rawImage.height,
            rawImage.bpp, palette, 1, rawImage.data);

        return new Image(getParent().getDisplay(), imageData);
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

            Log.i("ddms", "Saving image to " + fileName);
            ImageData imageData = mImageLabel.getImage().getImageData();

            try {
                WritePng.savePng(fileName, imageData);
            }
            catch (IOException ioe) {
                Log.w("ddms", "Unable to save " + fileName + ": " + ioe);
            }

            if (false) {
                ImageLoader loader = new ImageLoader();
                loader.data = new ImageData[] { imageData };
                // PNG writing not available until 3.3?  See bug at:
                //  https://bugs.eclipse.org/bugs/show_bug.cgi?id=24697
                // GIF writing only works for 8 bits
                // JPEG uses lossy compression
                // BMP has screwed-up colors
                loader.save(fileName, SWT.IMAGE_JPEG);
            }
        }
    }

}

