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

import com.android.draw9patch.graphics.GraphicsUtilities;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.BorderFactory;
import javax.swing.JSlider;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JCheckBox;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JSplitPane;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.Graphics2D;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.TexturePaint;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.AWTEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.AWTEventListener;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

class ImageEditorPanel extends JPanel {
    private static final String EXTENSION_9PATCH = ".9.png";
    private static final int DEFAULT_ZOOM = 8;
    private static final float DEFAULT_SCALE = 2.0f;

    private String name;
    private BufferedImage image;
    private boolean is9Patch;

    private ImageViewer viewer;
    private StretchesViewer stretchesViewer;
    private JLabel xLabel;
    private JLabel yLabel;

    private TexturePaint texture;    

    private List<Rectangle> patches;
    private List<Rectangle> horizontalPatches;
    private List<Rectangle> verticalPatches;
    private List<Rectangle> fixed;
    private boolean verticalStartWithPatch;
    private boolean horizontalStartWithPatch;

    private Pair<Integer> horizontalPadding;
    private Pair<Integer> verticalPadding;    

    ImageEditorPanel(MainFrame mainFrame, BufferedImage image, String name) {
        this.image = image;
        this.name = name;

        setTransferHandler(new ImageTransferHandler(mainFrame));

        checkImage();

        setOpaque(false);
        setLayout(new BorderLayout());

        loadSupport();
        buildImageViewer();
        buildStatusPanel();
    }

    private void loadSupport() {
        try {
            URL resource = getClass().getResource("/images/checker.png");
            BufferedImage checker = GraphicsUtilities.loadCompatibleImage(resource);
            texture = new TexturePaint(checker, new Rectangle2D.Double(0, 0,
                    checker.getWidth(), checker.getHeight()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void buildImageViewer() {
        viewer = new ImageViewer();

        JSplitPane splitter = new JSplitPane();
        splitter.setContinuousLayout(true);
        splitter.setResizeWeight(0.8);
        splitter.setBorder(null);

        JScrollPane scroller = new JScrollPane(viewer);
        scroller.setOpaque(false);
        scroller.setBorder(null);
        scroller.getViewport().setBorder(null);
        scroller.getViewport().setOpaque(false);

        splitter.setLeftComponent(scroller);
        splitter.setRightComponent(buildStretchesViewer());

        add(splitter);
    }

    private JComponent buildStretchesViewer() {
        stretchesViewer = new StretchesViewer();
        JScrollPane scroller = new JScrollPane(stretchesViewer);
        scroller.setBorder(null);
        scroller.getViewport().setBorder(null);
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        return scroller;
    }

    private void buildStatusPanel() {
        JPanel status = new JPanel(new GridBagLayout());
        status.setOpaque(false);

        JLabel label = new JLabel();
        label.setForeground(Color.WHITE);
        label.setText("Zoom: ");
        label.putClientProperty("JComponent.sizeVariant", "small");
        status.add(label, new GridBagConstraints(0, 0, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_END, GridBagConstraints.NONE,
                new Insets(0, 6, 0, 0), 0, 0));

        label = new JLabel();
        label.setForeground(Color.WHITE);
        label.setText("100%");
        label.putClientProperty("JComponent.sizeVariant", "small");
        status.add(label, new GridBagConstraints(1, 0, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_END, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        JSlider zoomSlider = new JSlider(1, 16, DEFAULT_ZOOM);
        zoomSlider.setSnapToTicks(true);
        zoomSlider.putClientProperty("JComponent.sizeVariant", "small");
        zoomSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                viewer.setZoom(((JSlider) evt.getSource()).getValue());
            }
        });
        status.add(zoomSlider, new GridBagConstraints(2, 0, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        JLabel maxZoomLabel = new JLabel();
        maxZoomLabel.setForeground(Color.WHITE);
        maxZoomLabel.putClientProperty("JComponent.sizeVariant", "small");
        maxZoomLabel.setText("800%");
        status.add(maxZoomLabel, new GridBagConstraints(3, 0, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        label = new JLabel();
        label.setForeground(Color.WHITE);
        label.setText("Patch scale: ");
        label.putClientProperty("JComponent.sizeVariant", "small");
        status.add(label, new GridBagConstraints(0, 1, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 6, 0, 0), 0, 0));

        label = new JLabel();
        label.setForeground(Color.WHITE);
        label.setText("2x");
        label.putClientProperty("JComponent.sizeVariant", "small");
        status.add(label, new GridBagConstraints(1, 1, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_END, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        zoomSlider = new JSlider(200, 600, (int) (DEFAULT_SCALE * 100.0f));
        zoomSlider.setSnapToTicks(true);
        zoomSlider.putClientProperty("JComponent.sizeVariant", "small");
        zoomSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                stretchesViewer.setScale(((JSlider) evt.getSource()).getValue() / 100.0f);
            }
        });
        status.add(zoomSlider, new GridBagConstraints(2, 1, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        maxZoomLabel = new JLabel();
        maxZoomLabel.setForeground(Color.WHITE);
        maxZoomLabel.putClientProperty("JComponent.sizeVariant", "small");
        maxZoomLabel.setText("6x");
        status.add(maxZoomLabel, new GridBagConstraints(3, 1, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        JCheckBox showLock = new JCheckBox("Show lock");
        showLock.setOpaque(false);
        showLock.setForeground(Color.WHITE);
        showLock.setSelected(true);
        showLock.putClientProperty("JComponent.sizeVariant", "small");
        showLock.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                viewer.setLockVisible(((JCheckBox) event.getSource()).isSelected());
            }
        });
        status.add(showLock, new GridBagConstraints(4, 0, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 12, 0, 0), 0, 0));

        JCheckBox showPatches = new JCheckBox("Show patches");
        showPatches.setOpaque(false);
        showPatches.setForeground(Color.WHITE);
        showPatches.putClientProperty("JComponent.sizeVariant", "small");
        showPatches.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                viewer.setPatchesVisible(((JCheckBox) event.getSource()).isSelected());
            }
        });
        status.add(showPatches, new GridBagConstraints(4, 1, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 12, 0, 0), 0, 0));

        JCheckBox showPadding = new JCheckBox("Show content");
        showPadding.setOpaque(false);
        showPadding.setForeground(Color.WHITE);
        showPadding.putClientProperty("JComponent.sizeVariant", "small");
        showPadding.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                stretchesViewer.setPaddingVisible(((JCheckBox) event.getSource()).isSelected());
            }
        });
        status.add(showPadding, new GridBagConstraints(5, 0, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                new Insets(0, 12, 0, 0), 0, 0));

        status.add(Box.createHorizontalGlue(), new GridBagConstraints(6, 0, 1, 1, 1.0f, 1.0f,
                GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

        label = new JLabel("X: ");
        label.setForeground(Color.WHITE);
        label.putClientProperty("JComponent.sizeVariant", "small");
        status.add(label, new GridBagConstraints(7, 0, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_END, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        xLabel = new JLabel("0px");
        xLabel.setForeground(Color.WHITE);
        xLabel.putClientProperty("JComponent.sizeVariant", "small");
        status.add(xLabel, new GridBagConstraints(8, 0, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_END, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 6), 0, 0));

        label = new JLabel("Y: ");
        label.setForeground(Color.WHITE);
        label.putClientProperty("JComponent.sizeVariant", "small");
        status.add(label, new GridBagConstraints(7, 1, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_END, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        yLabel = new JLabel("0px");
        yLabel.setForeground(Color.WHITE);
        yLabel.putClientProperty("JComponent.sizeVariant", "small");
        status.add(yLabel, new GridBagConstraints(8, 1, 1, 1, 0.0f, 0.0f,
                GridBagConstraints.LINE_END, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 6), 0, 0));

        add(status, BorderLayout.SOUTH);
    }

    private void checkImage() {
        is9Patch = name.endsWith(EXTENSION_9PATCH);
        if (!is9Patch) {
            convertTo9Patch();
        } else {
            ensure9Patch();
        }
    }

    private void ensure9Patch() {
        int width = image.getWidth();
        int height = image.getHeight();
        for (int i = 0; i < width; i++) {
            int pixel = image.getRGB(i, 0);
            if (pixel != 0 && pixel != 0xFF000000) {
                image.setRGB(i, 0, 0);
            }
            pixel = image.getRGB(i, height - 1);
            if (pixel != 0 && pixel != 0xFF000000) {
                image.setRGB(i, height - 1, 0);
            }
        }
        for (int i = 0; i < height; i++) {
            int pixel = image.getRGB(0, i);
            if (pixel != 0 && pixel != 0xFF000000) {
                image.setRGB(0, i, 0);
            }
            pixel = image.getRGB(width - 1, i);
            if (pixel != 0 && pixel != 0xFF000000) {
                image.setRGB(width - 1, i, 0);
            }
        }
    }

    private void convertTo9Patch() {
        BufferedImage buffer = GraphicsUtilities.createTranslucentCompatibleImage(
                image.getWidth() + 2, image.getHeight() + 2);

        Graphics2D g2 = buffer.createGraphics();
        g2.drawImage(image, 1, 1, null);
        g2.dispose();

        image = buffer;
        name = name.substring(0, name.lastIndexOf('.')) + ".9.png";
    }

    File chooseSaveFile() {
        if (is9Patch) {
            return new File(name);
        } else {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new PngFileFilter());
            int choice = chooser.showSaveDialog(this);
            if (choice == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (!file.getAbsolutePath().endsWith(EXTENSION_9PATCH)) {
                    String path = file.getAbsolutePath();
                    if (path.endsWith(".png")) {
                        path = path.substring(0, path.lastIndexOf(".png")) + EXTENSION_9PATCH;
                    } else {
                        path = path + EXTENSION_9PATCH;
                    }
                    name = path;
                    is9Patch = true;
                    return new File(path);
                }
                is9Patch = true;
                return file;
            }
        }
        return null;
    }

    RenderedImage getImage() {
        return image;
    }

    private class StretchesViewer extends JPanel {
        private static final int MARGIN = 24;

        private StretchView horizontal;
        private StretchView vertical;
        private StretchView both;

        private Dimension size;

        private float horizontalPatchesSum;
        private float verticalPatchesSum;

        private boolean showPadding;

        StretchesViewer() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));

            horizontal = new StretchView();
            vertical = new StretchView();
            both = new StretchView();

            setScale(DEFAULT_SCALE);
            
            add(vertical, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                    GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
            add(horizontal, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                    GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
            add(both, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                    GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(texture);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }

        void setScale(float scale) {
            int patchWidth = image.getWidth() - 2;
            int patchHeight = image.getHeight() - 2;

            int scaledWidth = (int) (patchWidth * scale);
            int scaledHeight = (int) (patchHeight * scale);

            horizontal.scaledWidth = scaledWidth;
            vertical.scaledHeight = scaledHeight;
            both.scaledWidth = scaledWidth;
            both.scaledHeight = scaledHeight;

            size = new Dimension(scaledWidth, scaledHeight);

            computePatches();
        }

        void computePatches() {
            boolean measuredWidth = false;
            boolean endRow = true;

            int remainderHorizontal = 0;
            int remainderVertical = 0;

            if (fixed.size() > 0) {
                int start = fixed.get(0).y;
                for (Rectangle rect : fixed) {
                    if (rect.y > start) {
                        endRow = true;
                        measuredWidth = true;
                    }
                    if (!measuredWidth) {
                        remainderHorizontal += rect.width;
                    }
                    if (endRow) {
                        remainderVertical += rect.height;
                        endRow = false;
                        start = rect.y;
                    }
                }
            }

            horizontal.remainderHorizontal = horizontal.scaledWidth - remainderHorizontal;
            vertical.remainderHorizontal = vertical.scaledWidth - remainderHorizontal;
            both.remainderHorizontal = both.scaledWidth - remainderHorizontal;

            horizontal.remainderVertical = horizontal.scaledHeight - remainderVertical;
            vertical.remainderVertical = vertical.scaledHeight - remainderVertical;
            both.remainderVertical = both.scaledHeight - remainderVertical;

            horizontalPatchesSum = 0;
            if (horizontalPatches.size() > 0) {
                int start = -1;
                for (Rectangle rect : horizontalPatches) {
                    if (rect.x > start) {
                        horizontalPatchesSum += rect.width;
                        start = rect.x;
                    }
                }
            } else {
                int start = -1;
                for (Rectangle rect : patches) {
                    if (rect.x > start) {
                        horizontalPatchesSum += rect.width;
                        start = rect.x;
                    }
                }
            }

            verticalPatchesSum = 0;
            if (verticalPatches.size() > 0) {
                int start = -1;
                for (Rectangle rect : verticalPatches) {
                    if (rect.y > start) {
                        verticalPatchesSum += rect.height;
                        start = rect.y;
                    }
                }
            } else {
                int start = -1;
                for (Rectangle rect : patches) {
                    if (rect.y > start) {
                        verticalPatchesSum += rect.height;
                        start = rect.y;
                    }
                }
            }

            setSize(size);
            ImageEditorPanel.this.validate();
            repaint();
        }

        void setPaddingVisible(boolean visible) {
            showPadding = visible;
            repaint();
        }

        private class StretchView extends JComponent {
            private final Color PADDING_COLOR = new Color(0.37f, 0.37f, 1.0f, 0.5f);

            int scaledWidth;
            int scaledHeight;

            int remainderHorizontal;
            int remainderVertical;

            StretchView() {
                scaledWidth = image.getWidth();
                scaledHeight = image.getHeight();
            }

            @Override
            protected void paintComponent(Graphics g) {
                int x = (getWidth() - scaledWidth) / 2;
                int y = (getHeight() - scaledHeight) / 2;

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.translate(x, y);

                x = 0;
                y = 0;

                if (patches.size() == 0) {
                    g.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
                    g2.dispose();
                    return;
                }

                int fixedIndex = 0;
                int horizontalIndex = 0;
                int verticalIndex = 0;
                int patchIndex = 0;

                boolean hStretch;
                boolean vStretch;

                float vWeightSum = 1.0f;
                float vRemainder = remainderVertical;

                vStretch = verticalStartWithPatch;
                while (y < scaledHeight - 1) {
                    hStretch = horizontalStartWithPatch;

                    int height = 0;
                    float vExtra = 0.0f;

                    float hWeightSum = 1.0f;
                    float hRemainder = remainderHorizontal;

                    while (x < scaledWidth - 1) {
                        Rectangle r;
                        if (!vStretch) {
                            if (hStretch) {
                                r = horizontalPatches.get(horizontalIndex++);
                                float extra = r.width / horizontalPatchesSum;
                                int width = (int) (extra * hRemainder / hWeightSum);
                                hWeightSum -= extra;
                                hRemainder -= width;
                                g.drawImage(image, x, y, x + width, y + r.height, r.x, r.y,
                                        r.x + r.width, r.y + r.height, null);
                                x += width;
                            } else {
                                r = fixed.get(fixedIndex++);
                                g.drawImage(image, x, y, x + r.width, y + r.height, r.x, r.y,
                                        r.x + r.width, r.y + r.height, null);
                                x += r.width;
                            }
                            height = r.height;
                        } else {
                            if (hStretch) {
                                r = patches.get(patchIndex++);
                                vExtra = r.height / verticalPatchesSum;
                                height = (int) (vExtra * vRemainder / vWeightSum);
                                float extra = r.width / horizontalPatchesSum;
                                int width = (int) (extra * hRemainder / hWeightSum);
                                hWeightSum -= extra;
                                hRemainder -= width;
                                g.drawImage(image, x, y, x + width, y + height, r.x, r.y,
                                        r.x + r.width, r.y + r.height, null);
                                x += width;
                            } else {
                                r = verticalPatches.get(verticalIndex++);
                                vExtra = r.height / verticalPatchesSum;
                                height = (int) (vExtra * vRemainder / vWeightSum);
                                g.drawImage(image, x, y, x + r.width, y + height, r.x, r.y,
                                        r.x + r.width, r.y + r.height, null);
                                x += r.width;
                            }
                            
                        }
                        hStretch = !hStretch;
                    }
                    x = 0;
                    y += height;
                    if (vStretch) {
                        vWeightSum -= vExtra;
                        vRemainder -= height;
                    }
                    vStretch = !vStretch;
                }

                if (showPadding) {
                    g.setColor(PADDING_COLOR);
                    g.fillRect(horizontalPadding.first, verticalPadding.first,
                            scaledWidth - horizontalPadding.first - horizontalPadding.second,
                            scaledHeight - verticalPadding.first - verticalPadding.second);
                }

                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return size;
            }
        }
    }

    private class ImageViewer extends JComponent {
        private final Color CORRUPTED_COLOR = new Color(1.0f, 0.0f, 0.0f, 0.7f);
        private final Color LOCK_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.7f);
        private final Color STRIPES_COLOR = new Color(1.0f, 0.0f, 0.0f, 0.5f);
        private final Color BACK_COLOR = new Color(0xc0c0c0);
        private final Color HELP_COLOR = new Color(0xffffe1);
        private final Color PATCH_COLOR = new Color(1.0f, 0.37f, 0.99f, 0.5f);
        private final Color PATCH_ONEWAY_COLOR = new Color(0.37f, 1.0f, 0.37f, 0.5f);

        private static final float STRIPES_WIDTH = 4.0f;
        private static final double STRIPES_SPACING = 6.0;
        private static final int STRIPES_ANGLE = 45;

        private int zoom;
        private boolean showPatches;
        private boolean showLock = true;

        private Dimension size;

        private boolean locked;

        private int[] row;
        private int[] column;

        private int lastPositionX;
        private int lastPositionY;
        private int currentButton;
        private boolean showCursor;

        private JLabel helpLabel;
        private boolean eraseMode;

        private JButton checkButton;
        private List<Rectangle> corruptedPatches;
        private boolean showBadPatches;

        private JPanel helpPanel;

        ImageViewer() {
            setLayout(new GridBagLayout());
            helpPanel = new JPanel(new BorderLayout());
            helpPanel.setBorder(new EmptyBorder(0, 6, 0, 6));
            helpPanel.setBackground(HELP_COLOR);
            helpLabel = new JLabel("Press Shift to erase pixels");
            helpLabel.putClientProperty("JComponent.sizeVariant", "small");            
            helpPanel.add(helpLabel, BorderLayout.WEST);
            checkButton = new JButton("Show bad patches");
            checkButton.putClientProperty("JComponent.sizeVariant", "small");
            checkButton.putClientProperty("JButton.buttonType", "roundRect");
            helpPanel.add(checkButton, BorderLayout.EAST);

            add(helpPanel, new GridBagConstraints(0, 0, 1, 1,
                    1.0f, 1.0f, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 0), 0, 0));

            setOpaque(true);

            setZoom(DEFAULT_ZOOM);
            findPatches();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    // Store the button here instead of retrieving it again in MouseDragged
                    // below, because on linux, calling MouseEvent.getButton() for the drag
                    // event returns 0, which appears to be technically correct (no button
                    // changed state).
                    currentButton = event.isShiftDown() ? MouseEvent.BUTTON3 : event.getButton();
                    paint(event.getX(), event.getY(), currentButton);
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent event) {
                    if (!checkLockedRegion(event.getX(), event.getY())) {
                        // use the stored button, see note above
                        paint(event.getX(), event.getY(),  currentButton);
                    }
                }

                @Override
                public void mouseMoved(MouseEvent event) {
                    checkLockedRegion(event.getX(), event.getY());
                }
            });
            Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
                public void eventDispatched(AWTEvent event) {
                    enableEraseMode((KeyEvent) event);                    
                }
            }, AWTEvent.KEY_EVENT_MASK);

            checkButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    if (!showBadPatches) {
                        findBadPatches();
                        checkButton.setText("Hide bad patches");
                    } else {
                        checkButton.setText("Show bad patches");
                        corruptedPatches = null;
                    }
                    repaint();
                    showBadPatches = !showBadPatches;
                }
            });
        }

        private void findBadPatches() {
            corruptedPatches = new ArrayList<Rectangle>();

            for (Rectangle patch : patches) {
                if (corruptPatch(patch)) {
                    corruptedPatches.add(patch);
                }
            }

            for (Rectangle patch : horizontalPatches) {
                if (corruptHorizontalPatch(patch)) {
                    corruptedPatches.add(patch);
                }
            }

            for (Rectangle patch : verticalPatches) {
                if (corruptVerticalPatch(patch)) {
                    corruptedPatches.add(patch);
                }
            }
        }

        private boolean corruptPatch(Rectangle patch) {
            int[] pixels = GraphicsUtilities.getPixels(image, patch.x, patch.y,
                    patch.width, patch.height, null);

            if (pixels.length > 0) {
                int reference = pixels[0];
                for (int pixel : pixels) {
                    if (pixel != reference) {
                        return true;
                    }
                }
            }

            return false;
        }

        private boolean corruptHorizontalPatch(Rectangle patch) {
            int[] reference = new int[patch.height];
            int[] column = new int[patch.height];
            reference = GraphicsUtilities.getPixels(image, patch.x, patch.y,
                    1, patch.height, reference);

            for (int i = 1; i < patch.width; i++) {
                column = GraphicsUtilities.getPixels(image, patch.x + i, patch.y,
                        1, patch.height, column);
                if (!Arrays.equals(reference, column)) {
                    return true;
                }
            }

            return false;
        }

        private boolean corruptVerticalPatch(Rectangle patch) {
            int[] reference = new int[patch.width];
            int[] row = new int[patch.width];
            reference = GraphicsUtilities.getPixels(image, patch.x, patch.y,
                    patch.width, 1, reference);

            for (int i = 1; i < patch.height; i++) {
                row = GraphicsUtilities.getPixels(image, patch.x, patch.y + i, patch.width, 1, row);
                if (!Arrays.equals(reference, row)) {
                    return true;
                }
            }

            return false;
        }

        private void enableEraseMode(KeyEvent event) {
            boolean oldEraseMode = eraseMode;
            eraseMode = event.isShiftDown();
            if (eraseMode != oldEraseMode) {
                if (eraseMode) {
                    helpLabel.setText("Release Shift to draw pixels");
                } else {
                    helpLabel.setText("Press Shift to erase pixels");
                }
            }
        }

        private void paint(int x, int y, int button) {
            int color;
            switch (button) {
                case MouseEvent.BUTTON1:
                    color = 0xFF000000;
                    break;
                case MouseEvent.BUTTON3:
                    color = 0;
                    break;
                default:
                    return;
            }

            int left = (getWidth() - size.width) / 2;
            int top = (helpPanel.getHeight() + getHeight() - size.height) / 2;

            x = (x - left) / zoom;
            y = (y - top) / zoom;

            int width = image.getWidth();
            int height = image.getHeight();
            if (((x == 0 || x == width - 1) && (y > 0 && y < height - 1)) ||
                    ((x > 0 && x < width - 1) && (y == 0 || y == height - 1))) {
                image.setRGB(x, y, color);
                findPatches();
                stretchesViewer.computePatches();
                if (showBadPatches) {
                    findBadPatches();
                }
                repaint();
            }
        }

        private boolean checkLockedRegion(int x, int y) {
            int oldX = lastPositionX;
            int oldY = lastPositionY;
            lastPositionX = x;
            lastPositionY = y;

            int left = (getWidth() - size.width) / 2;
            int top = (helpPanel.getHeight() + getHeight() - size.height) / 2;

            x = (x - left) / zoom;
            y = (y - top) / zoom;

            int width = image.getWidth();
            int height = image.getHeight();

            xLabel.setText(Math.max(0, Math.min(x, width - 1)) + " px");
            yLabel.setText(Math.max(0, Math.min(y, height - 1)) + " px");

            boolean previousLock = locked;
            locked = x > 0 && x < width - 1 && y > 0 && y < height - 1;

            boolean previousCursor = showCursor;
            showCursor = ((x == 0 || x == width - 1) && (y > 0 && y < height - 1)) ||
                    ((x > 0 && x < width - 1) && (y == 0 || y == height - 1));

            if (locked != previousLock) {
                repaint();
            } else if (showCursor || (showCursor != previousCursor)) {
                Rectangle clip = new Rectangle(lastPositionX - 1 - zoom / 2,
                        lastPositionY - 1 - zoom / 2, zoom + 2, zoom + 2);
                clip = clip.union(new Rectangle(oldX - 1 - zoom / 2,
                        oldY - 1 - zoom / 2, zoom + 2, zoom + 2));
                repaint(clip);
            }

            return locked;
        }

        @Override
        protected void paintComponent(Graphics g) {
            int x = (getWidth() - size.width) / 2;
            int y = (helpPanel.getHeight() + getHeight() - size.height) / 2;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(BACK_COLOR);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.translate(x, y);
            g2.setPaint(texture);
            g2.fillRect(0, 0, size.width, size.height);
            g2.scale(zoom, zoom);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(image, 0, 0, null);

            if (showPatches) {
                g2.setColor(PATCH_COLOR);
                for (Rectangle patch : patches) {
                    g2.fillRect(patch.x, patch.y, patch.width, patch.height);
                }
                g2.setColor(PATCH_ONEWAY_COLOR);
                for (Rectangle patch : horizontalPatches) {
                    g2.fillRect(patch.x, patch.y, patch.width, patch.height);
                }
                for (Rectangle patch : verticalPatches) {
                    g2.fillRect(patch.x, patch.y, patch.width, patch.height);
                }
            }

            if (corruptedPatches != null) {
                g2.setColor(CORRUPTED_COLOR);
                g2.setStroke(new BasicStroke(3.0f / zoom));
                for (Rectangle patch : corruptedPatches) {
                    g2.draw(new RoundRectangle2D.Float(patch.x - 2.0f / zoom, patch.y - 2.0f / zoom,
                            patch.width + 2.0f / zoom, patch.height + 2.0f / zoom,
                            6.0f / zoom, 6.0f / zoom));
                }
            }

            if (showLock && locked) {
                int width = image.getWidth();
                int height = image.getHeight();

                g2.setColor(LOCK_COLOR);
                g2.fillRect(1, 1, width - 2, height - 2);

                g2.setColor(STRIPES_COLOR);
                g2.translate(1, 1);
                paintStripes(g2, width - 2, height - 2);
                g2.translate(-1, -1);
            }

            g2.dispose();

            if (showCursor) {
                Graphics cursor = g.create();
                cursor.setXORMode(Color.WHITE);
                cursor.setColor(Color.BLACK);
                cursor.drawRect(lastPositionX - zoom / 2, lastPositionY - zoom / 2, zoom, zoom);
                cursor.dispose();
            }
        }

        private void paintStripes(Graphics2D g, int width, int height) {
            //draws pinstripes at the angle specified in this class
            //and at the given distance apart
            Shape oldClip = g.getClip();
            Area area = new Area(new Rectangle(0, 0, width, height));
            if(oldClip != null) {
                area = new Area(oldClip);
            }
            area.intersect(new Area(new Rectangle(0,0,width,height)));
            g.setClip(area);

            g.setStroke(new BasicStroke(STRIPES_WIDTH));

            double hypLength = Math.sqrt((width * width) +
                    (height * height));

            double radians = Math.toRadians(STRIPES_ANGLE);
            g.rotate(radians);

            double spacing = STRIPES_SPACING;
            spacing += STRIPES_WIDTH;
            int numLines = (int)(hypLength / spacing);

            for (int i=0; i<numLines; i++) {
                double x = i * spacing;
                Line2D line = new Line2D.Double(x, -hypLength, x, hypLength);
                g.draw(line);
            }
            g.setClip(oldClip);
        }

        @Override
        public Dimension getPreferredSize() {
            return size;
        }

        void setZoom(int value) {
            int width = image.getWidth();
            int height = image.getHeight();

            zoom = value;
            size = new Dimension(width * zoom, height * zoom);

            setSize(size);
            ImageEditorPanel.this.validate();
            repaint();
        }

        void setPatchesVisible(boolean visible) {
            showPatches = visible;
            findPatches();
            repaint();
        }

        private void findPatches() {
            int width = image.getWidth();
            int height = image.getHeight();

            row = GraphicsUtilities.getPixels(image, 0, 0, width, 1, row);
            column = GraphicsUtilities.getPixels(image, 0, 0, 1, height, column);

            boolean[] result = new boolean[1];
            Pair<List<Pair<Integer>>> left = getPatches(column, result);
            verticalStartWithPatch = result[0];

            result = new boolean[1];
            Pair<List<Pair<Integer>>> top = getPatches(row, result);
            horizontalStartWithPatch = result[0];

            fixed = getRectangles(left.first, top.first);
            patches = getRectangles(left.second, top.second);

            if (fixed.size() > 0) {
                horizontalPatches = getRectangles(left.first, top.second);
                verticalPatches = getRectangles(left.second, top.first);
            } else {
                if (top.first.size() > 0) {
                    horizontalPatches = new ArrayList<Rectangle>(0);
                    verticalPatches = getVerticalRectangles(top.first);
                } else if (left.first.size() > 0) {
                    horizontalPatches = getHorizontalRectangles(left.first);
                    verticalPatches = new ArrayList<Rectangle>(0);
                } else {
                    horizontalPatches = verticalPatches = new ArrayList<Rectangle>(0);
                }
            }

            row = GraphicsUtilities.getPixels(image, 0, height - 1, width, 1, row);
            column = GraphicsUtilities.getPixels(image, width - 1, 0, 1, height, column);

            top = getPatches(row, result);
            horizontalPadding = getPadding(top.first);

            left = getPatches(column, result);
            verticalPadding = getPadding(left.first);
        }

        private List<Rectangle> getVerticalRectangles(List<Pair<Integer>> topPairs) {
            List<Rectangle> rectangles = new ArrayList<Rectangle>();
            for (Pair<Integer> top : topPairs) {
                int x = top.first;
                int width = top.second - top.first;

                rectangles.add(new Rectangle(x, 1, width, image.getHeight() - 2));
            }
            return rectangles;
        }

        private List<Rectangle> getHorizontalRectangles(List<Pair<Integer>> leftPairs) {
            List<Rectangle> rectangles = new ArrayList<Rectangle>();
            for (Pair<Integer> left : leftPairs) {
                int y = left.first;
                int height = left.second - left.first;

                rectangles.add(new Rectangle(1, y, image.getWidth() - 2, height));
            }
            return rectangles;
        }

        private Pair<Integer> getPadding(List<Pair<Integer>> pairs) {
            if (pairs.size() == 0) {
                return new Pair<Integer>(0, 0);
            } else if (pairs.size() == 1) {
                if (pairs.get(0).first == 1) {
                    return new Pair<Integer>(pairs.get(0).second - pairs.get(0).first, 0);
                } else {
                    return new Pair<Integer>(0, pairs.get(0).second - pairs.get(0).first);                    
                }
            } else {
                int index = pairs.size() - 1;
                return new Pair<Integer>(pairs.get(0).second - pairs.get(0).first,
                        pairs.get(index).second - pairs.get(index).first);
            }
        }

        private List<Rectangle> getRectangles(List<Pair<Integer>> leftPairs,
                List<Pair<Integer>> topPairs) {
            List<Rectangle> rectangles = new ArrayList<Rectangle>();
            for (Pair<Integer> left : leftPairs) {
                int y = left.first;
                int height = left.second - left.first;
                for (Pair<Integer> top : topPairs) {
                    int x = top.first;
                    int width = top.second - top.first;

                    rectangles.add(new Rectangle(x, y, width, height));
                }
            }
            return rectangles;
        }

        private Pair<List<Pair<Integer>>> getPatches(int[] pixels, boolean[] startWithPatch) {
            int lastIndex = 1;
            int lastPixel = pixels[1];
            boolean first = true;

            List<Pair<Integer>> fixed = new ArrayList<Pair<Integer>>();
            List<Pair<Integer>> patches = new ArrayList<Pair<Integer>>();

            for (int i = 1; i < pixels.length - 1; i++) {
                int pixel = pixels[i];
                if (pixel != lastPixel) {
                    if (lastPixel == 0xFF000000) {
                        if (first) startWithPatch[0] = true;
                        patches.add(new Pair<Integer>(lastIndex, i));
                    } else {
                        fixed.add(new Pair<Integer>(lastIndex, i));
                    }
                    first = false;

                    lastIndex = i;
                    lastPixel = pixel;
                }
            }
            if (lastPixel == 0xFF000000) {
                if (first) startWithPatch[0] = true;
                patches.add(new Pair<Integer>(lastIndex, pixels.length - 1));
            } else {
                fixed.add(new Pair<Integer>(lastIndex, pixels.length - 1));
            }

            if (patches.size() == 0) {
                patches.add(new Pair<Integer>(1, pixels.length - 1));
                startWithPatch[0] = true;
                fixed.clear();
            }

            return new Pair<List<Pair<Integer>>>(fixed, patches);
        }

        void setLockVisible(boolean visible) {
            showLock = visible;
            repaint();
        }
    }

    static class Pair<E> {
        E first;
        E second;

        Pair(E first, E second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public String toString() {
            return "Pair[" + first + ", " + second + "]";
        }
    }
}
