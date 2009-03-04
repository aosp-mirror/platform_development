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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.GradientPaint;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.BorderLayout;
import javax.swing.JPanel;

class GradientPanel extends JPanel {
    private static final int DARK_BLUE = 0x202737;

    GradientPanel() {
        super(new BorderLayout());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Rectangle clip = g2.getClipBounds();
        Paint paint = g2.getPaint();

        g2.setPaint(new GradientPaint(0.0f, getHeight() * 0.22f, new Color(DARK_BLUE),
                                      0.0f, getHeight() * 0.9f, Color.BLACK));
        g2.fillRect(clip.x, clip.y, clip.width, clip.height);

        g2.setPaint(paint);
    }
}
