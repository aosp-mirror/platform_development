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

package com.android.hierarchyviewer.ui;

import com.android.hierarchyviewer.scene.ViewNode;

import javax.swing.*;
import java.awt.*;

class CaptureRenderer extends JLabel {
    private ViewNode node;
    private boolean showExtras;

    CaptureRenderer(ImageIcon icon, ViewNode node) {
        super(icon);
        this.node = node;
        setBackground(Color.BLACK);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();

        if (node.hasMargins) {
            d.width += node.marginLeft + node.marginRight;
            d.height += node.marginTop + node.marginBottom;
        }

        return d;
    }

    public void setShowExtras(boolean showExtras) {
        this.showExtras = showExtras;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Icon icon = getIcon();
        int width = icon.getIconWidth();
        int height = icon.getIconHeight();

        int x = (getWidth() - width) / 2;
        int y = (getHeight() - height) / 2;

        icon.paintIcon(this, g, x, y);

        if (showExtras) {
            g.translate(x, y);
            g.setXORMode(Color.WHITE);
            if ((node.paddingBottom | node.paddingLeft |
                    node.paddingTop | node.paddingRight) != 0) {
                g.setColor(Color.RED);
                g.drawRect(node.paddingLeft, node.paddingTop,
                        width - node.paddingRight - node.paddingLeft,
                        height - node.paddingBottom - node.paddingTop);
            }
            if (node.baseline != -1) {
                g.setColor(Color.BLUE);
                g.drawLine(0, node.baseline, width, node.baseline);
            }
            if (node.hasMargins && (node.marginLeft | node.marginBottom |
                    node.marginRight | node.marginRight) != 0) {
                g.setColor(Color.BLACK);
                g.drawRect(-node.marginLeft, -node.marginTop,
                        node.marginLeft + width + node.marginRight,
                        node.marginTop + height + node.marginBottom);
            }
            g.translate(-x, -y);
        }
    }
}
