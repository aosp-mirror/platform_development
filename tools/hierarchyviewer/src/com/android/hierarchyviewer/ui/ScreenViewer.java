package com.android.hierarchyviewer.ui;

import com.android.ddmlib.Device;
import com.android.ddmlib.RawImage;
import com.android.hierarchyviewer.util.WorkerThread;
import com.android.hierarchyviewer.scene.ViewNode;
import com.android.hierarchyviewer.ui.util.PngFileFilter;
import com.android.hierarchyviewer.ui.util.IconLoader;

import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.imageio.ImageIO;

import org.jdesktop.swingworker.SwingWorker;

import java.io.IOException;
import java.io.File;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.FlowLayout;
import java.awt.AlphaComposite;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.concurrent.ExecutionException;

class ScreenViewer extends JPanel implements ActionListener {
    private final Workspace workspace;
    private final Device device;

    private GetScreenshotTask task;
    private BufferedImage image;
    private int[] scanline;
    private volatile boolean isLoading;

    private BufferedImage overlay;    
    private AlphaComposite overlayAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);

    private ScreenViewer.LoupeStatus status;
    private ScreenViewer.LoupeViewer loupe;
    private ScreenViewer.Crosshair crosshair;

    private int zoom = 8;
    private int y = 0;

    private Timer timer;
    private ViewNode node;

    private JSlider zoomSlider;

    ScreenViewer(Workspace workspace, Device device, int spacing) {
        setLayout(new BorderLayout());
        setOpaque(false);

        this.workspace = workspace;
        this.device = device;

        timer = new Timer(5000, this);
        timer.setInitialDelay(0);
        timer.setRepeats(true);

        JPanel panel = buildViewerAndControls();
        add(panel, BorderLayout.WEST);

        JPanel loupePanel = buildLoupePanel(spacing);
        add(loupePanel, BorderLayout.CENTER);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                timer.start();                
            }
        });
    }

    private JPanel buildLoupePanel(int spacing) {
        loupe = new LoupeViewer();
        loupe.addMouseWheelListener(new WheelZoomListener());
        CrosshairPanel crosshairPanel = new CrosshairPanel(loupe);

        JPanel loupePanel = new JPanel(new BorderLayout());
        loupePanel.add(crosshairPanel);
        status = new LoupeStatus();
        loupePanel.add(status, BorderLayout.SOUTH);

        loupePanel.setBorder(BorderFactory.createEmptyBorder(0, spacing, 0, 0));
        return loupePanel;
    }

    private class WheelZoomListener implements MouseWheelListener {
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (zoomSlider != null) {
                int val = zoomSlider.getValue();
                val -= e.getWheelRotation() * 2;
                zoomSlider.setValue(val);
            }
        }
    }

    private JPanel buildViewerAndControls() {
        JPanel panel = new JPanel(new GridBagLayout());
        crosshair = new Crosshair(new ScreenshotViewer());
        crosshair.addMouseWheelListener(new WheelZoomListener());
        panel.add(crosshair,
                new GridBagConstraints(0, y++, 2, 1, 1.0f, 0.0f,
                    GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
        buildSlider(panel, "Overlay:", "0%", "100%", 0, 100, 30, 1).addChangeListener(
                new ChangeListener() {
                    public void stateChanged(ChangeEvent event) {
                        float opacity = ((JSlider) event.getSource()).getValue() / 100.0f;
                        overlayAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
                        repaint();
                    }
        });
        buildOverlayExtraControls(panel);
        buildSlider(panel, "Refresh Rate:", "1s", "40s", 1, 40, 5, 1).addChangeListener(
                new ChangeListener() {
                    public void stateChanged(ChangeEvent event) {
                        int rate = ((JSlider) event.getSource()).getValue() * 1000;
                        timer.setDelay(rate);
                        timer.setInitialDelay(0);
                        timer.restart();
                    }
        });
        zoomSlider = buildSlider(panel, "Zoom:", "2x", "24x", 2, 24, 8, 2);
        zoomSlider.addChangeListener(
                new ChangeListener() {
                    public void stateChanged(ChangeEvent event) {
                        zoom = ((JSlider) event.getSource()).getValue();
                        loupe.clearGrid = true;
                        loupe.moveToPoint(crosshair.crosshair.x, crosshair.crosshair.y);
                        repaint();
                    }
        });
        panel.add(Box.createVerticalGlue(),
                new GridBagConstraints(0, y++, 2, 1, 1.0f, 1.0f,
                    GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
        return panel;
    }

    private void buildOverlayExtraControls(JPanel panel) {
        JPanel extras = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        JButton loadOverlay = new JButton("Load...");
        loadOverlay.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                SwingWorker<?, ?> worker = openOverlay();
                if (worker != null) {
                    worker.execute();
                }
            }
        });
        extras.add(loadOverlay);

        JCheckBox showInLoupe = new JCheckBox("Show in Loupe");
        showInLoupe.setSelected(false);
        showInLoupe.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                loupe.showOverlay = ((JCheckBox) event.getSource()).isSelected();
                loupe.repaint();
            }
        });
        extras.add(showInLoupe);

        panel.add(extras, new GridBagConstraints(1, y++, 1, 1, 1.0f, 0.0f,
                    GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0));
    }

    public SwingWorker<?, ?> openOverlay() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new PngFileFilter());
        int choice = chooser.showOpenDialog(this);
        if (choice == JFileChooser.APPROVE_OPTION) {
            return new OpenOverlayTask(chooser.getSelectedFile());
        } else {
            return null;
        }
    }

    private JSlider buildSlider(JPanel panel, String title, String minName, String maxName,
            int min, int max, int value, int tick) {
        panel.add(new JLabel(title), new GridBagConstraints(0, y, 1, 1, 1.0f, 0.0f,
                    GridBagConstraints.LINE_END, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 6), 0, 0));
        JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        sliderPanel.add(new JLabel(minName));
        JSlider slider = new JSlider(min, max, value);
        slider.setMinorTickSpacing(tick);
        slider.setMajorTickSpacing(tick);
        slider.setSnapToTicks(true);
        sliderPanel.add(slider);
        sliderPanel.add(new JLabel(maxName));
        panel.add(sliderPanel, new GridBagConstraints(1, y++, 1, 1, 1.0f, 0.0f,
                    GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0));
        return slider;
    }

    void stop() {
        timer.stop();
    }

    void start() {
        timer.start();
    }

    void select(ViewNode node) {
        this.node = node;
        repaint();
    }

    class LoupeViewer extends JComponent {
        private final Color lineColor = new Color(1.0f, 1.0f, 1.0f, 0.3f);

        private int width;
        private int height;
        private BufferedImage grid;
        private int left;
        private int top;
        public boolean clearGrid;

        private final Rectangle clip = new Rectangle();
        private boolean showOverlay = false;

        LoupeViewer() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    moveToPoint(event);
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent event) {
                    moveToPoint(event);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (isLoading) {
                return;
            }

            g.translate(-left, -top);

            if (image != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2.scale(zoom, zoom);
                g2.drawImage(image, 0, 0, null);
                if (overlay != null && showOverlay) {
                    g2.setComposite(overlayAlpha);
                    g2.drawImage(overlay, 0, image.getHeight() - overlay.getHeight(), null);
                }
                g2.dispose();
            }

            int width = getWidth();
            int height = getHeight();

            Graphics2D g2 = null;
            if (width != this.width || height != this.height) {
                this.width = width;
                this.height = height;

                grid = new BufferedImage(width + zoom + 1, height + zoom + 1,
                        BufferedImage.TYPE_INT_ARGB);
                clearGrid = true;
                g2 = grid.createGraphics();
            } else if (clearGrid) {
                g2 = grid.createGraphics();
                g2.setComposite(AlphaComposite.Clear);
                g2.fillRect(0, 0, grid.getWidth(), grid.getHeight());
                g2.setComposite(AlphaComposite.SrcOver);
            }

            if (clearGrid) {
                clearGrid = false;

                g2.setColor(lineColor);
                width += zoom;
                height += zoom;

                for (int x = zoom; x <= width; x += zoom) {
                    g2.drawLine(x, 0, x, height);
                }

                for (int y = 0; y <= height; y += zoom) {
                    g2.drawLine(0, y, width, y);
                }

                g2.dispose();
            }

            if (image != null) {
                g.getClipBounds(clip);
                g.clipRect(0, 0, image.getWidth() * zoom + 1, image.getHeight() * zoom + 1);
                g.drawImage(grid, clip.x - clip.x % zoom, clip.y - clip.y % zoom, null);
            }

            g.translate(left, top);
        }

        void moveToPoint(MouseEvent event) {
            int x = Math.max(0, Math.min((event.getX() + left) / zoom, image.getWidth() - 1));
            int y = Math.max(0, Math.min((event.getY() + top) / zoom, image.getHeight() - 1));
            moveToPoint(x, y);
            crosshair.moveToPoint(x, y);
        }

        void moveToPoint(int x, int y) {
            left = x * zoom - width / 2 + zoom / 2;
            top = y * zoom - height / 2 + zoom / 2;
            repaint();
        }
    }

    class LoupeStatus extends JPanel {
        private JLabel xLabel;
        private JLabel yLabel;
        private JLabel rLabel;
        private JLabel gLabel;
        private JLabel bLabel;
        private JLabel hLabel;
        private ScreenViewer.LoupeStatus.ColoredSquare square;
        private Color color;

        LoupeStatus() {
            setOpaque(true);
            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

            square = new ColoredSquare();
            add(square, new GridBagConstraints(0, 0, 1, 2, 0.0f, 0.0f,
                    GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 12), 0, 0 ));

            JLabel label;

            add(label = new JLabel("#ffffff"), new GridBagConstraints(0, 2, 1, 1, 0.0f, 0.0f,
                    GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 12), 0, 0 ));
            label.setForeground(Color.WHITE);
            hLabel = label;

            add(label = new JLabel("R:"), new GridBagConstraints(1, 0, 1, 1, 0.0f, 0.0f,
                    GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                    new Insets(0, 6, 0, 6), 0, 0 ));
            label.setForeground(Color.WHITE);
            add(label = new JLabel("255"), new GridBagConstraints(2, 0, 1, 1, 0.0f, 0.0f,
                    GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 12), 0, 0 ));
            label.setForeground(Color.WHITE);
            rLabel = label;

            add(label = new JLabel("G:"), new GridBagConstraints(1, 1, 1, 1, 0.0f, 0.0f,
                    GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                    new Insets(0, 6, 0, 6), 0, 0 ));
            label.setForeground(Color.WHITE);
            add(label = new JLabel("255"), new GridBagConstraints(2, 1, 1, 1, 0.0f, 0.0f,
                    GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 12), 0, 0 ));
            label.setForeground(Color.WHITE);
            gLabel = label;

            add(label = new JLabel("B:"), new GridBagConstraints(1, 2, 1, 1, 0.0f, 0.0f,
                    GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                    new Insets(0, 6, 0, 6), 0, 0 ));
            label.setForeground(Color.WHITE);
            add(label = new JLabel("255"), new GridBagConstraints(2, 2, 1, 1, 0.0f, 0.0f,
                    GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 12), 0, 0 ));
            label.setForeground(Color.WHITE);
            bLabel = label;

            add(label = new JLabel("X:"), new GridBagConstraints(3, 0, 1, 1, 0.0f, 0.0f,
                    GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                    new Insets(0, 6, 0, 6), 0, 0 ));
            label.setForeground(Color.WHITE);
            add(label = new JLabel("0 px"), new GridBagConstraints(4, 0, 1, 1, 0.0f, 0.0f,
                    GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 12), 0, 0 ));
            label.setForeground(Color.WHITE);
            xLabel = label;

            add(label = new JLabel("Y:"), new GridBagConstraints(3, 1, 1, 1, 0.0f, 0.0f,
                    GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                    new Insets(0, 6, 0, 6), 0, 0 ));
            label.setForeground(Color.WHITE);
            add(label = new JLabel("0 px"), new GridBagConstraints(4, 1, 1, 1, 0.0f, 0.0f,
                    GridBagConstraints.LINE_START, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 12), 0, 0 ));
            label.setForeground(Color.WHITE);
            yLabel = label;

            add(Box.createHorizontalGlue(), new GridBagConstraints(5, 0, 1, 1, 1.0f, 0.0f,
                    GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0 ));
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        void showPixel(int x, int y) {
            xLabel.setText(x + " px");
            yLabel.setText(y + " px");

            int pixel = image.getRGB(x, y);
            color = new Color(pixel);
            hLabel.setText("#" + Integer.toHexString(pixel));
            rLabel.setText(String.valueOf((pixel >> 16) & 0xff));
            gLabel.setText(String.valueOf((pixel >>  8) & 0xff));
            bLabel.setText(String.valueOf((pixel      ) & 0xff));

            square.repaint();
        }

        private class ColoredSquare extends JComponent {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width = 60;
                d.height = 30;
                return d;
            }

            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(color);
                g.fillRect(0, 0, getWidth(), getHeight());

                g.setColor(Color.WHITE);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);                
            }
        }
    }

    class Crosshair extends JPanel {
        // magenta = 0xff5efe
        private final Color crosshairColor = new Color(0x00ffff);
        Point crosshair = new Point();
        private int width;
        private int height;

        Crosshair(ScreenshotViewer screenshotViewer) {
            setOpaque(true);
            setLayout(new BorderLayout());
            add(screenshotViewer);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    moveToPoint(event);
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent event) {
                    moveToPoint(event);
                }
            });
        }

        void moveToPoint(int x, int y) {
            crosshair.x = x;
            crosshair.y = y;
            status.showPixel(crosshair.x, crosshair.y);
            repaint();
        }

        private void moveToPoint(MouseEvent event) {
            crosshair.x = Math.max(0, Math.min(image.getWidth() - 1, event.getX()));
            crosshair.y = Math.max(0, Math.min(image.getHeight() - 1, event.getY()));
            loupe.moveToPoint(crosshair.x, crosshair.y);
            status.showPixel(crosshair.x, crosshair.y);

            repaint();
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);

            if (crosshair == null || width != getWidth() || height != getHeight()) {
                width = getWidth();
                height = getHeight();
                crosshair = new Point(width / 2, height / 2);
            }

            g.setColor(crosshairColor);

            g.drawLine(crosshair.x, 0, crosshair.x, height);
            g.drawLine(0, crosshair.y, width, crosshair.y);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    class ScreenshotViewer extends JComponent {
        private final Color boundsColor = new Color(0xff5efe);

        ScreenshotViewer() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());

            if (isLoading) {
                return;
            }

            if (image != null) {
                g.drawImage(image, 0, 0, null);
                if (overlay != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setComposite(overlayAlpha);
                    g2.drawImage(overlay, 0, image.getHeight() - overlay.getHeight(), null);
                }
            }

            if (node != null) {
                Graphics s = g.create();
                s.setColor(boundsColor);
                ViewNode p = node.parent;
                while (p != null) {
                    s.translate(p.left - p.scrollX, p.top - p.scrollY);
                    p = p.parent;
                }
                s.drawRect(node.left, node.top, node.width - 1, node.height - 1);
                s.translate(node.left, node.top);

                s.setXORMode(Color.WHITE);
                if ((node.paddingBottom | node.paddingLeft |
                        node.paddingTop | node.paddingRight) != 0) {
                    s.setColor(Color.BLACK);
                    s.drawRect(node.paddingLeft, node.paddingTop,
                            node.width - node.paddingRight - node.paddingLeft - 1,
                            node.height - node.paddingBottom - node.paddingTop - 1);
                }
                if (node.hasMargins && (node.marginLeft | node.marginBottom |
                        node.marginRight | node.marginRight) != 0) {
                    s.setColor(Color.BLACK);
                    s.drawRect(-node.marginLeft, -node.marginTop,
                            node.marginLeft + node.width + node.marginRight - 1,
                            node.marginTop + node.height + node.marginBottom - 1);
                }

                s.dispose();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            if (image == null) {
                return new Dimension(320, 480);
            }
            return new Dimension(image.getWidth(), image.getHeight());
        }
    }

    private class CrosshairPanel extends JPanel {
        private final Color crosshairColor = new Color(0xff5efe);
        private final Insets insets = new Insets(0, 0, 0, 0);

        CrosshairPanel(LoupeViewer loupe) {
            setLayout(new BorderLayout());
            add(loupe);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);

            g.setColor(crosshairColor);

            int width = getWidth();
            int height = getHeight();

            getInsets(insets);

            int x = (width - insets.left - insets.right) / 2;
            int y = (height - insets.top - insets.bottom) / 2;

            g.drawLine(insets.left + x, insets.top, insets.left + x, height - insets.bottom);
            g.drawLine(insets.left, insets.top + y, width - insets.right, insets.top + y);
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Color.BLACK);
            Insets insets = getInsets();
            g.fillRect(insets.left, insets.top, getWidth() - insets.left - insets.right,
                    getHeight() - insets.top - insets.bottom);
        }
    }

    public void actionPerformed(ActionEvent event) {
        if (task != null && !task.isDone()) {
            return;
        }
        task = new GetScreenshotTask();
        task.execute();
    }

    private class GetScreenshotTask extends SwingWorker<Boolean, Void> {
        private GetScreenshotTask() {
            workspace.beginTask();
        }

        @Override
        @WorkerThread
        protected Boolean doInBackground() throws Exception {
            RawImage rawImage;
            try {
                rawImage = device.getScreenshot();
            } catch (IOException ioe) {
                return false;
            }

            boolean resize = false;
            isLoading = true;
            try {
                if (rawImage != null && rawImage.bpp == 16) {
                    if (image == null || rawImage.width != image.getWidth() ||
                            rawImage.height != image.getHeight()) {
                        image = new BufferedImage(rawImage.width, rawImage.height,
                                BufferedImage.TYPE_INT_ARGB);
                        scanline = new int[rawImage.width];
                        resize = true;
                    }

                    byte[] buffer = rawImage.data;
                    int index = 0;
                    for (int y = 0 ; y < rawImage.height ; y++) {
                        for (int x = 0 ; x < rawImage.width ; x++) {
                            int value = buffer[index++] & 0x00FF;
                            value |= (buffer[index++] << 8) & 0x0FF00;

                            int r = ((value >> 11) & 0x01F) << 3;
                            int g = ((value >> 5) & 0x03F) << 2;
                            int b = ((value     ) & 0x01F) << 3;

                            scanline[x] = 0xFF << 24 | r << 16 | g << 8 | b;
                        }
                        image.setRGB(0, y, rawImage.width, 1, scanline,
                                0, rawImage.width);
                    }
                }
            } finally {
                isLoading = false;
            }

            return resize;
        }

        @Override
        protected void done() {
            workspace.endTask();
            try {
                if (get()) {
                    validate();
                    crosshair.crosshair = new Point(image.getWidth() / 2,
                            image.getHeight() / 2);
                    status.showPixel(image.getWidth() / 2, image.getHeight() / 2);
                    loupe.moveToPoint(image.getWidth() / 2, image.getHeight() / 2);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            repaint();
        }
    }

    private class OpenOverlayTask extends SwingWorker<BufferedImage, Void> {
        private File file;

        private OpenOverlayTask(File file) {
            this.file = file;
            workspace.beginTask();
        }

        @Override
        @WorkerThread
        protected BufferedImage doInBackground() {
            try {
                return IconLoader.toCompatibleImage(ImageIO.read(file));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        protected void done() {
            try {
                overlay = get();
                repaint();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } finally {
                workspace.endTask();
            }
        }
    }
}
