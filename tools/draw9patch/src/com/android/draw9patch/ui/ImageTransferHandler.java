/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.draw9patch.ui;

import javax.swing.TransferHandler;
import javax.swing.JComponent;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.net.MalformedURLException;

class ImageTransferHandler extends TransferHandler {
    private final MainFrame mainFrame;

    ImageTransferHandler(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public boolean importData(JComponent component, Transferable transferable) {
        try {
            Object data = transferable.getTransferData(DataFlavor.javaFileListFlavor);
            //noinspection unchecked
            final File file = ((List<File>) data).get(0);
            mainFrame.open(file).execute();
        } catch (UnsupportedFlavorException e) {
            return false;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public boolean canImport(JComponent component, DataFlavor[] dataFlavors) {
        for (DataFlavor flavor : dataFlavors) {
            if (flavor.isFlavorJavaFileListType()) {
                return true;
            }
        }
        return false;
    }
}
