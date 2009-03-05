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

import com.android.hierarchyviewer.scene.ViewHierarchyScene;
import com.android.hierarchyviewer.scene.ViewNode;

import javax.swing.JComponent;
import javax.swing.BorderFactory;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

class LayoutRenderer extends JComponent {
    private static final int EMULATED_SCREEN_WIDTH = 320;
    private static final int EMULATED_SCREEN_HEIGHT = 480;
    private static final int SCREEN_MARGIN = 24;

    private boolean showExtras;
    private ViewHierarchyScene scene;
    private JComponent sceneView;

    LayoutRenderer(ViewHierarchyScene scene, JComponent sceneView) {
        this.scene = scene;
        this.sceneView = sceneView;

        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        setBackground(Color.BLACK);
        setForeground(Color.WHITE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                selectChild(event.getX(), event.getY());
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(EMULATED_SCREEN_WIDTH + SCREEN_MARGIN,
                EMULATED_SCREEN_HEIGHT + SCREEN_MARGIN);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        Insets insets = getInsets();
        g.clipRect(insets.left, insets.top,
                getWidth() - insets.left - insets.right,
                getHeight() - insets.top - insets.bottom);

        if (scene == null) {
            return;
        }

        ViewNode root = scene.getRoot();
        if (root == null) {
            return;
        }

        int x = (getWidth() - insets.left - insets.right - root.width) / 2;
        int y = (getHeight() - insets.top - insets.bottom - root.height) / 2;
        g.translate(insets.left + x, insets.top + y);

        g.setColor(getForeground());
        g.drawRect(root.left, root.top, root.width - 1, root.height - 1);
        g.clipRect(root.left - 1, root.top - 1, root.width + 1, root.height + 1);
        drawChildren(g, root, -root.scrollX, -root.scrollY);

        Set<?> selection = scene.getSelectedObjects();
        if (selection.size() > 0) {
            ViewNode node = (ViewNode) selection.iterator().next();
            g.setColor(Color.RED);
            Graphics s = g.create();
            ViewNode p = node.parent;
            while (p != null) {
                s.translate(p.left - p.scrollX, p.top - p.scrollY);
                p = p.parent;
            }
            if (showExtras && node.image != null) {
                s.drawImage(node.image, node.left, node.top, null);
            }
            s.drawRect(node.left, node.top, node.width - 1, node.height - 1);
            s.dispose();
        }

        g.translate(-insets.left - x, -insets.top - y);
    }

    private void drawChildren(Graphics g, ViewNode root, int x, int y) {
        g.translate(x, y);
        for (ViewNode node : root.children) {
            if (!node.willNotDraw) {
                g.drawRect(node.left, node.top, node.width - 1, node.height - 1);
            }

            if (node.children.size() > 0) {
                drawChildren(g, node,
                        node.left - node.parent.scrollX,
                        node.top - node.parent.scrollY);
            }
        }
        g.translate(-x, -y);
    }

    public void setShowExtras(boolean showExtras) {
        this.showExtras = showExtras;
        repaint();
    }

    private void selectChild(int x, int y) {

        if (scene == null) {
            return;
        }

        ViewNode root = scene.getRoot();
        if (root == null) {
            return;
        }

        Insets insets = getInsets();

        int xoffset = (getWidth() - insets.left - insets.right - root.width) / 2 + insets.left + 1;
        int yoffset = (getHeight() - insets.top - insets.bottom - root.height) / 2 + insets.top + 1;

        x -= xoffset;
        y -= yoffset;
        if (x >= 0 && x < EMULATED_SCREEN_WIDTH && y >= 0 && y < EMULATED_SCREEN_HEIGHT) {
            ViewNode hit = findChild(root, root, x, y);
            scene.setFocusedObject(hit);
            sceneView.repaint();
        }
    }

    private ViewNode findChild(ViewNode root, ViewNode besthit, int x, int y) {
        ViewNode hit = besthit;
        for (ViewNode node : root.children) {

            if (node.left <= x && x < node.left + node.width &&
                node.top <= y && y < node.top + node.height) {
                if (node.width <= hit.width && node.height <= hit.height) {
                    hit = node;
                }
            }

            if (node.children.size() > 0) {
                hit = findChild(node, hit,
                        x - (node.left - node.parent.scrollX),
                        y - (node.top - node.parent.scrollY));
            }
        }
        return hit;
    }
}
