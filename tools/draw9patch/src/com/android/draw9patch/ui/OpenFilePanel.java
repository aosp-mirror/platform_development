package com.android.draw9patch.ui;

import com.android.draw9patch.graphics.GraphicsUtilities;

import javax.swing.JComponent;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.io.IOException;
import java.net.URL;/*
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

class OpenFilePanel extends JComponent {
    private BufferedImage dropHere;

    OpenFilePanel(MainFrame mainFrame) {
        setOpaque(false);
        loadSupportImage();
        setTransferHandler(new ImageTransferHandler(mainFrame));
    }

    private void loadSupportImage() {
        try {
            URL resource = getClass().getResource("/images/drop.png");
            dropHere = GraphicsUtilities.loadCompatibleImage(resource);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        int x = (getWidth() - dropHere.getWidth()) / 2;
        int y = (getHeight() - dropHere.getHeight()) / 2;

        g.drawImage(dropHere, x, y, null);
    }

}
