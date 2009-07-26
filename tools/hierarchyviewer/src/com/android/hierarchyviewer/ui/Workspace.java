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

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.hierarchyviewer.device.DeviceBridge;
import com.android.hierarchyviewer.device.Window;
import com.android.hierarchyviewer.laf.UnifiedContentBorder;
import com.android.hierarchyviewer.scene.CaptureLoader;
import com.android.hierarchyviewer.scene.ViewHierarchyLoader;
import com.android.hierarchyviewer.scene.ViewHierarchyScene;
import com.android.hierarchyviewer.scene.ViewManager;
import com.android.hierarchyviewer.scene.ViewNode;
import com.android.hierarchyviewer.scene.WindowsLoader;
import com.android.hierarchyviewer.scene.ProfilesLoader;
import com.android.hierarchyviewer.util.OS;
import com.android.hierarchyviewer.util.WorkerThread;
import com.android.hierarchyviewer.ui.action.ShowDevicesAction;
import com.android.hierarchyviewer.ui.action.RequestLayoutAction;
import com.android.hierarchyviewer.ui.action.InvalidateAction;
import com.android.hierarchyviewer.ui.action.CaptureNodeAction;
import com.android.hierarchyviewer.ui.action.RefreshWindowsAction;
import com.android.hierarchyviewer.ui.action.StopServerAction;
import com.android.hierarchyviewer.ui.action.StartServerAction;
import com.android.hierarchyviewer.ui.action.ExitAction;
import com.android.hierarchyviewer.ui.action.LoadGraphAction;
import com.android.hierarchyviewer.ui.action.SaveSceneAction;
import com.android.hierarchyviewer.ui.util.PngFileFilter;
import com.android.hierarchyviewer.ui.util.IconLoader;
import com.android.hierarchyviewer.ui.model.PropertiesTableModel;
import com.android.hierarchyviewer.ui.model.ViewsTreeModel;
import com.android.hierarchyviewer.ui.model.ProfilesTableModel;
import org.jdesktop.swingworker.SwingWorker;
import org.netbeans.api.visual.graph.layout.TreeGraphLayout;
import org.netbeans.api.visual.model.ObjectSceneEvent;
import org.netbeans.api.visual.model.ObjectSceneEventType;
import org.netbeans.api.visual.model.ObjectSceneListener;
import org.netbeans.api.visual.model.ObjectState;

import javax.imageio.ImageIO;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.JTree;
import javax.swing.Box;
import javax.swing.JTextField;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.image.BufferedImage;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.concurrent.ExecutionException;

public class Workspace extends JFrame {
    private JLabel viewCountLabel;
    private JSlider zoomSlider;
    private JSplitPane sideSplitter;
    private JSplitPane mainSplitter;
    private JTable propertiesTable;
    private JTable profilingTable;
    private JComponent pixelPerfectPanel;
    private JTree pixelPerfectTree;
    private ScreenViewer screenViewer;

    private JPanel extrasPanel;
    private LayoutRenderer layoutView;

    private JScrollPane sceneScroller;
    private JComponent sceneView;

    private ViewHierarchyScene scene;

    private ActionMap actionsMap;
    private JPanel mainPanel;
    private JProgressBar progress;
    private JToolBar buttonsPanel;

    private JComponent deviceSelector;
    private DevicesTableModel devicesTableModel;
    private WindowsTableModel windowsTableModel;

    private IDevice currentDevice;
    private Window currentWindow = Window.FOCUSED_WINDOW;

    private JButton displayNodeButton;
    private JButton invalidateButton;
    private JButton requestLayoutButton;
    private JButton loadButton;
    private JButton startButton;
    private JButton stopButton;
    private JButton showDevicesButton;
    private JButton refreshButton;
    private JToggleButton graphViewButton;
    private JToggleButton pixelPerfectViewButton;
    private JMenuItem saveMenuItem;
    private JMenuItem showDevicesMenuItem;
    private JMenuItem loadMenuItem;
    private JMenuItem startMenuItem;
    private JMenuItem stopMenuItem;
    private JTable devices;
    private JTable windows;
    private JLabel minZoomLabel;
    private JLabel maxZoomLabel;
    private JTextField filterText;
    private JLabel filterLabel;

    public Workspace() {
        super("Hierarchy Viewer");

        buildActions();
        add(buildMainPanel());
        setJMenuBar(buildMenuBar());

        devices.changeSelection(0, 0, false, false);
        currentDeviceChanged();

        pack();
    }

    private void buildActions() {
        actionsMap = new ActionMap();
        actionsMap.put(ExitAction.ACTION_NAME, new ExitAction(this));
        actionsMap.put(ShowDevicesAction.ACTION_NAME, new ShowDevicesAction(this));
        actionsMap.put(LoadGraphAction.ACTION_NAME, new LoadGraphAction(this));
        actionsMap.put(SaveSceneAction.ACTION_NAME, new SaveSceneAction(this));
        actionsMap.put(StartServerAction.ACTION_NAME, new StartServerAction(this));
        actionsMap.put(StopServerAction.ACTION_NAME, new StopServerAction(this));
        actionsMap.put(InvalidateAction.ACTION_NAME, new InvalidateAction(this));
        actionsMap.put(RequestLayoutAction.ACTION_NAME, new RequestLayoutAction(this));
        actionsMap.put(CaptureNodeAction.ACTION_NAME, new CaptureNodeAction(this));
        actionsMap.put(RefreshWindowsAction.ACTION_NAME, new RefreshWindowsAction(this));
    }

    private JComponent buildMainPanel() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(buildToolBar(), BorderLayout.PAGE_START);
        mainPanel.add(deviceSelector = buildDeviceSelector(), BorderLayout.CENTER);
        mainPanel.add(buildStatusPanel(), BorderLayout.SOUTH);

        mainPanel.setPreferredSize(new Dimension(950, 800));

        return mainPanel;
    }

    private JComponent buildGraphPanel() {
        sceneScroller = new JScrollPane();
        sceneScroller.setBorder(null);

        mainSplitter = new JSplitPane();
        mainSplitter.setResizeWeight(1.0);
        mainSplitter.setContinuousLayout(true);
        if (OS.isMacOsX() && OS.isLeopardOrLater()) {
            mainSplitter.setBorder(new UnifiedContentBorder());
        }

        mainSplitter.setLeftComponent(sceneScroller);
        mainSplitter.setRightComponent(buildSideSplitter());

        return mainSplitter;
    }

    private JComponent buildDeviceSelector() {
        JPanel panel = new JPanel(new GridBagLayout());
        if (OS.isMacOsX() && OS.isLeopardOrLater()) {
            panel.setBorder(new UnifiedContentBorder());
        }

        devicesTableModel = new DevicesTableModel();
        for (IDevice device : DeviceBridge.getDevices()) {
            DeviceBridge.setupDeviceForward(device);
            devicesTableModel.addDevice(device);
        }
        DeviceBridge.startListenForDevices(devicesTableModel);

        devices = new JTable(devicesTableModel);
        devices.getSelectionModel().addListSelectionListener(new DeviceSelectedListener());
        devices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        devices.setBorder(null);
        JScrollPane devicesScroller = new JScrollPane(devices);
        devicesScroller.setBorder(null);
        panel.add(devicesScroller, new GridBagConstraints(0, 0, 1, 1, 0.5, 1.0,
                GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0),
                0, 0));

        windowsTableModel = new WindowsTableModel();
        windowsTableModel.setVisible(false);

        windows = new JTable(windowsTableModel);
        windows.getSelectionModel().addListSelectionListener(new WindowSelectedListener());
        windows.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        windows.setBorder(null);
        JScrollPane windowsScroller = new JScrollPane(windows);
        windowsScroller.setBorder(null);
        panel.add(windowsScroller, new GridBagConstraints(2, 0, 1, 1, 0.5, 1.0,
                GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0),
                0, 0));

        return panel;
    }

    private JComponent buildSideSplitter() {
        propertiesTable = new JTable();
        propertiesTable.setModel(new DefaultTableModel(new Object[][] { },
                new String[] { "Property", "Value" }));
        propertiesTable.setBorder(null);
        propertiesTable.getTableHeader().setBorder(null);

        JScrollPane tableScroller = new JScrollPane(propertiesTable);
        tableScroller.setBorder(null);

        profilingTable = new JTable();
        profilingTable.setModel(new DefaultTableModel(new Object[][] {
                { " " , " " }, { " " , " " }, { " " , " " } },
                new String[] { "Operation", "Duration (ms)" }));
        profilingTable.setBorder(null);
        profilingTable.getTableHeader().setBorder(null);

        JScrollPane firstTableScroller = new JScrollPane(profilingTable);
        firstTableScroller.setBorder(null);

        setVisibleRowCount(profilingTable, 5);
        firstTableScroller.setMinimumSize(profilingTable.getPreferredScrollableViewportSize());

        JSplitPane tablesSplitter = new JSplitPane();
        tablesSplitter.setBorder(null);
        tablesSplitter.setOrientation(JSplitPane.VERTICAL_SPLIT);
        tablesSplitter.setResizeWeight(0);
        tablesSplitter.setLeftComponent(firstTableScroller);
        tablesSplitter.setBottomComponent(tableScroller);
        tablesSplitter.setContinuousLayout(true);

        sideSplitter = new JSplitPane();
        sideSplitter.setBorder(null);
        sideSplitter.setOrientation(JSplitPane.VERTICAL_SPLIT);
        sideSplitter.setResizeWeight(0.5);
        sideSplitter.setLeftComponent(tablesSplitter);
        sideSplitter.setBottomComponent(null);
        sideSplitter.setContinuousLayout(true);

        return sideSplitter;
    }

    private JPanel buildStatusPanel() {
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BorderLayout());

        JPanel leftSide = new JPanel();
        leftSide.setOpaque(false);
        leftSide.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 5));
        leftSide.add(Box.createHorizontalStrut(6));

        ButtonGroup group = new ButtonGroup();

        graphViewButton = new JToggleButton(IconLoader.load(getClass(),
                "/images/icon-graph-view.png"));
        graphViewButton.setSelectedIcon(IconLoader.load(getClass(),
                "/images/icon-graph-view-selected.png"));
        graphViewButton.putClientProperty("JButton.buttonType", "segmentedTextured");
        graphViewButton.putClientProperty("JButton.segmentPosition", "first");
        graphViewButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                toggleGraphView();
            }
        });
        group.add(graphViewButton);
        leftSide.add(graphViewButton);

        pixelPerfectViewButton = new JToggleButton(IconLoader.load(getClass(),
                "/images/icon-pixel-perfect-view.png"));
        pixelPerfectViewButton.setSelectedIcon(IconLoader.load(getClass(),
                "/images/icon-pixel-perfect-view-selected.png"));
        pixelPerfectViewButton.putClientProperty("JButton.buttonType", "segmentedTextured");
        pixelPerfectViewButton.putClientProperty("JButton.segmentPosition", "last");
        pixelPerfectViewButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                togglePixelPerfectView();
            }
        });
        group.add(pixelPerfectViewButton);
        leftSide.add(pixelPerfectViewButton);

        graphViewButton.setSelected(true);

        filterText = new JTextField(20);
        filterText.putClientProperty("JComponent.sizeVariant", "small");
        filterText.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateFilter(e);
            }

            public void removeUpdate(DocumentEvent e) {
                updateFilter(e);
            }

            public void changedUpdate(DocumentEvent e) {
                updateFilter(e);
            }
        });

        filterLabel = new JLabel("Filter by class or id:");
        filterLabel.putClientProperty("JComponent.sizeVariant", "small");
        filterLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));

        leftSide.add(filterLabel);
        leftSide.add(filterText);

        minZoomLabel = new JLabel();
        minZoomLabel.setText("20%");
        minZoomLabel.putClientProperty("JComponent.sizeVariant", "small");
        minZoomLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        leftSide.add(minZoomLabel);

        zoomSlider = new JSlider();
        zoomSlider.putClientProperty("JComponent.sizeVariant", "small");
        zoomSlider.setMaximum(200);
        zoomSlider.setMinimum(20);
        zoomSlider.setValue(100);
        zoomSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                zoomSliderStateChanged(evt);
            }
        });
        leftSide.add(zoomSlider);

        maxZoomLabel = new JLabel();
        maxZoomLabel.putClientProperty("JComponent.sizeVariant", "small");
        maxZoomLabel.setText("200%");
        leftSide.add(maxZoomLabel);

        viewCountLabel = new JLabel();
        viewCountLabel.setText("0 views");
        viewCountLabel.putClientProperty("JComponent.sizeVariant", "small");
        viewCountLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        leftSide.add(viewCountLabel);

        statusPanel.add(leftSide, BorderLayout.LINE_START);

        JPanel rightSide = new JPanel();
        rightSide.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        rightSide.setLayout(new FlowLayout(FlowLayout.RIGHT));

        progress = new JProgressBar();
        progress.setVisible(false);
        progress.setIndeterminate(true);
        progress.putClientProperty("JComponent.sizeVariant", "mini");
        progress.putClientProperty("JProgressBar.style", "circular");
        rightSide.add(progress);

        statusPanel.add(rightSide, BorderLayout.LINE_END);

        hideStatusBarComponents();

        return statusPanel;
    }

    private void hideStatusBarComponents() {
        viewCountLabel.setVisible(false);
        zoomSlider.setVisible(false);
        minZoomLabel.setVisible(false);
        maxZoomLabel.setVisible(false);
        filterLabel.setVisible(false);
        filterText.setVisible(false);
    }

    private JToolBar buildToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        startButton = new JButton();
        startButton.setAction(actionsMap.get(StartServerAction.ACTION_NAME));
        startButton.putClientProperty("JButton.buttonType", "segmentedTextured");
        startButton.putClientProperty("JButton.segmentPosition", "first");
        toolBar.add(startButton);

        stopButton = new JButton();
        stopButton.setAction(actionsMap.get(StopServerAction.ACTION_NAME));
        stopButton.putClientProperty("JButton.buttonType", "segmentedTextured");
        stopButton.putClientProperty("JButton.segmentPosition", "middle");
        toolBar.add(stopButton);

        refreshButton = new JButton();
        refreshButton.setAction(actionsMap.get(RefreshWindowsAction.ACTION_NAME));
        refreshButton.putClientProperty("JButton.buttonType", "segmentedTextured");
        refreshButton.putClientProperty("JButton.segmentPosition", "last");
        toolBar.add(refreshButton);

        showDevicesButton = new JButton();
        showDevicesButton.setAction(actionsMap.get(ShowDevicesAction.ACTION_NAME));
        showDevicesButton.putClientProperty("JButton.buttonType", "segmentedTextured");
        showDevicesButton.putClientProperty("JButton.segmentPosition", "first");
        toolBar.add(showDevicesButton);
        showDevicesButton.setEnabled(false);

        loadButton = new JButton();
        loadButton.setAction(actionsMap.get(LoadGraphAction.ACTION_NAME));
        loadButton.putClientProperty("JButton.buttonType", "segmentedTextured");
        loadButton.putClientProperty("JButton.segmentPosition", "last");
        toolBar.add(loadButton);

        displayNodeButton = new JButton();
        displayNodeButton.setAction(actionsMap.get(CaptureNodeAction.ACTION_NAME));
        displayNodeButton.putClientProperty("JButton.buttonType", "segmentedTextured");
        displayNodeButton.putClientProperty("JButton.segmentPosition", "first");
        toolBar.add(displayNodeButton);

        invalidateButton = new JButton();
        invalidateButton.setAction(actionsMap.get(InvalidateAction.ACTION_NAME));
        invalidateButton.putClientProperty("JButton.buttonType", "segmentedTextured");
        invalidateButton.putClientProperty("JButton.segmentPosition", "middle");
        toolBar.add(invalidateButton);

        requestLayoutButton = new JButton();
        requestLayoutButton.setAction(actionsMap.get(RequestLayoutAction.ACTION_NAME));
        requestLayoutButton.putClientProperty("JButton.buttonType", "segmentedTextured");
        requestLayoutButton.putClientProperty("JButton.segmentPosition", "last");
        toolBar.add(requestLayoutButton);

        return toolBar;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu();
        JMenu viewMenu = new JMenu();
        JMenu viewHierarchyMenu = new JMenu();
        JMenu serverMenu = new JMenu();

        saveMenuItem = new JMenuItem();
        JMenuItem exitMenuItem = new JMenuItem();

        showDevicesMenuItem = new JMenuItem();

        loadMenuItem = new JMenuItem();

        startMenuItem = new JMenuItem();
        stopMenuItem = new JMenuItem();

        fileMenu.setText("File");

        saveMenuItem.setAction(actionsMap.get(SaveSceneAction.ACTION_NAME));
        fileMenu.add(saveMenuItem);

        exitMenuItem.setAction(actionsMap.get(ExitAction.ACTION_NAME));
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        viewMenu.setText("View");

        showDevicesMenuItem.setAction(actionsMap.get(ShowDevicesAction.ACTION_NAME));
        showDevicesMenuItem.setEnabled(false);
        viewMenu.add(showDevicesMenuItem);

        menuBar.add(viewMenu);

        viewHierarchyMenu.setText("Hierarchy");

        loadMenuItem.setAction(actionsMap.get(LoadGraphAction.ACTION_NAME));
        viewHierarchyMenu.add(loadMenuItem);

        menuBar.add(viewHierarchyMenu);

        serverMenu.setText("Server");

        startMenuItem.setAction(actionsMap.get(StartServerAction.ACTION_NAME));
        serverMenu.add(startMenuItem);

        stopMenuItem.setAction(actionsMap.get(StopServerAction.ACTION_NAME));
        serverMenu.add(stopMenuItem);

        menuBar.add(serverMenu);

        return menuBar;
    }

    private JComponent buildPixelPerfectPanel() {
        JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        pixelPerfectTree = new JTree(new Object[0]);
        pixelPerfectTree.setBorder(null);
        pixelPerfectTree.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        pixelPerfectTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent event) {
                ViewNode node = (ViewNode) event.getPath().getLastPathComponent();
                screenViewer.select(node);
            }
        });

        JScrollPane scroller = new JScrollPane(pixelPerfectTree);
        scroller.setBorder(null);
        scroller.getViewport().setBorder(null);

        splitter.setContinuousLayout(true);
        splitter.setLeftComponent(scroller);
        splitter.setRightComponent(buildPixelPerfectViewer(splitter));
        splitter.setBorder(null);

        if (OS.isMacOsX() && OS.isLeopardOrLater()) {
            splitter.setBorder(new UnifiedContentBorder());
        }

        return splitter;
    }

    private JComponent buildPixelPerfectViewer(JSplitPane splitter) {
        screenViewer = new ScreenViewer(this, currentDevice, splitter.getDividerSize());
        return screenViewer;
    }

    private void toggleGraphView() {
        showStatusBarComponents();

        screenViewer.stop();
        mainPanel.remove(pixelPerfectPanel);
        mainPanel.add(mainSplitter, BorderLayout.CENTER);

        validate();
        repaint();
    }

    private void showStatusBarComponents() {
        viewCountLabel.setVisible(true);
        zoomSlider.setVisible(true);
        minZoomLabel.setVisible(true);
        maxZoomLabel.setVisible(true);
        filterLabel.setVisible(true);
        filterText.setVisible(true);
    }

    private void togglePixelPerfectView() {
        if (pixelPerfectPanel == null) {
            pixelPerfectPanel = buildPixelPerfectPanel();
            showPixelPerfectTree();
        } else {
            screenViewer.start();
        }

        hideStatusBarComponents();

        mainPanel.remove(mainSplitter);
        mainPanel.add(pixelPerfectPanel, BorderLayout.CENTER);

        validate();
        repaint();
    }

    private void zoomSliderStateChanged(ChangeEvent evt) {
        JSlider slider = (JSlider) evt.getSource();
        if (sceneView != null) {
            scene.setZoomFactor(slider.getValue() / 100.0d);
            sceneView.repaint();
        }
    }

    private void showProperties(ViewNode node) {
        propertiesTable.setModel(new PropertiesTableModel(node));
    }

    private void updateProfiles(double[] profiles) {
        profilingTable.setModel(new ProfilesTableModel(profiles));
        setVisibleRowCount(profilingTable, profiles.length + 1);
    }

    public static void setVisibleRowCount(JTable table, int rows) {
        int height = 0;
        for (int row = 0; row < rows; row++) {
            height += table.getRowHeight(row);
        }

        Dimension size = new Dimension(table.getPreferredScrollableViewportSize().width, height);
        table.setPreferredScrollableViewportSize(size);
        table.revalidate();
    }

    private void showPixelPerfectTree() {
        if (pixelPerfectTree == null) {
            return;
        }
        pixelPerfectTree.setModel(new ViewsTreeModel(scene.getRoot()));
        pixelPerfectTree.setCellRenderer(new ViewsTreeCellRenderer());
        expandAll(pixelPerfectTree, true);

    }

    private static void expandAll(JTree tree, boolean expand) {
        ViewNode root = (ViewNode) tree.getModel().getRoot();
        expandAll(tree, new TreePath(root), expand);
    }

    private static void expandAll(JTree tree, TreePath parent, boolean expand) {
        // Traverse children
        ViewNode node = (ViewNode)parent.getLastPathComponent();
        if (node.children != null) {
            for (ViewNode n : node.children) {
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }

        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }

    private void createGraph(ViewHierarchyScene scene) {
        scene.addObjectSceneListener(new SceneFocusListener(),
                ObjectSceneEventType.OBJECT_FOCUS_CHANGED);

        if (mainSplitter == null) {
            mainPanel.remove(deviceSelector);
            mainPanel.add(buildGraphPanel(), BorderLayout.CENTER);
            showDevicesButton.setEnabled(true);
            showDevicesMenuItem.setEnabled(true);
            graphViewButton.setEnabled(true);
            pixelPerfectViewButton.setEnabled(true);

            showStatusBarComponents();
        }

        sceneView = scene.createView();
        sceneView.addMouseListener(new NodeClickListener());
        sceneView.addMouseWheelListener(new WheelZoomListener());
        sceneScroller.setViewportView(sceneView);

        if (extrasPanel != null) {
            sideSplitter.remove(extrasPanel);
        }
        sideSplitter.setBottomComponent(buildExtrasPanel());

        mainSplitter.setDividerLocation(getWidth() - mainSplitter.getDividerSize() -
                buttonsPanel.getPreferredSize().width);

        saveMenuItem.setEnabled(true);
        showPixelPerfectTree();

        updateStatus();
        layoutScene();
    }

    private void layoutScene() {
        TreeGraphLayout<ViewNode, String> layout =
                new TreeGraphLayout<ViewNode, String>(scene, 50, 50, 70, 30, true);
        layout.layout(scene.getRoot());
    }

    private void updateStatus() {
        viewCountLabel.setText("" + scene.getNodes().size() + " views");
        zoomSlider.setEnabled(scene.getNodes().size() > 0);
    }

    private JPanel buildExtrasPanel() {
        extrasPanel = new JPanel(new BorderLayout());
        JScrollPane p = new JScrollPane(layoutView = new LayoutRenderer(scene, sceneView));
        JScrollBar b = p.getVerticalScrollBar();
        b.setUnitIncrement(10);
        extrasPanel.add(p);
        extrasPanel.add(scene.createSatelliteView(), BorderLayout.SOUTH);
        extrasPanel.add(buildLayoutViewControlButtons(), BorderLayout.NORTH);
        return extrasPanel;
    }

    private JComponent buildLayoutViewControlButtons() {
        buttonsPanel = new JToolBar();
        buttonsPanel.setFloatable(false);

        ButtonGroup group = new ButtonGroup();

        JToggleButton white = new JToggleButton("On White");
        toggleColorOnSelect(white);
        white.putClientProperty("JButton.buttonType", "segmentedTextured");
        white.putClientProperty("JButton.segmentPosition", "first");
        white.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                layoutView.setBackground(Color.WHITE);
                layoutView.setForeground(Color.BLACK);
            }
        });
        group.add(white);
        buttonsPanel.add(white);

        JToggleButton black = new JToggleButton("On Black");
        toggleColorOnSelect(black);
        black.putClientProperty("JButton.buttonType", "segmentedTextured");
        black.putClientProperty("JButton.segmentPosition", "last");
        black.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                layoutView.setBackground(Color.BLACK);
                layoutView.setForeground(Color.WHITE);
            }
        });
        group.add(black);
        buttonsPanel.add(black);

        black.setSelected(true);

        JCheckBox showExtras = new JCheckBox("Show Extras");
        showExtras.putClientProperty("JComponent.sizeVariant", "small");
        showExtras.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                layoutView.setShowExtras(((JCheckBox) e.getSource()).isSelected());
            }
        });
        buttonsPanel.add(showExtras);

        return buttonsPanel;
    }

    private void showCaptureWindow(ViewNode node, String captureParams, Image image) {
        if (image != null) {
            layoutView.repaint();

            JFrame frame = new JFrame(captureParams);
            JPanel panel = new JPanel(new BorderLayout());

            final CaptureRenderer label = new CaptureRenderer(new ImageIcon(image), node);
            label.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

            final JPanel solidColor = new JPanel(new BorderLayout());
            solidColor.setBackground(Color.BLACK);
            solidColor.add(label);

            JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);

            ButtonGroup group = new ButtonGroup();

            JToggleButton white = new JToggleButton("On White");
            toggleColorOnSelect(white);
            white.putClientProperty("JButton.buttonType", "segmentedTextured");
            white.putClientProperty("JButton.segmentPosition", "first");
            white.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    solidColor.setBackground(Color.WHITE);
                }
            });
            group.add(white);
            toolBar.add(white);

            JToggleButton black = new JToggleButton("On Black");
            toggleColorOnSelect(black);
            black.putClientProperty("JButton.buttonType", "segmentedTextured");
            black.putClientProperty("JButton.segmentPosition", "last");
            black.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    solidColor.setBackground(Color.BLACK);
                }
            });
            group.add(black);
            toolBar.add(black);

            black.setSelected(true);

            JCheckBox showExtras = new JCheckBox("Show Extras");
            showExtras.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    label.setShowExtras(((JCheckBox) e.getSource()).isSelected());
                }
            });
            toolBar.add(showExtras);

            panel.add(toolBar, BorderLayout.NORTH);
            panel.add(solidColor);
            frame.add(panel);

            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(Workspace.this);
            frame.setVisible(true);
        }
    }

    private void reset() {
        currentDevice = null;
        currentWindow = null;
        currentDeviceChanged();
        windowsTableModel.setVisible(false);
        windowsTableModel.clear();

        showDevicesSelector();
    }

    public void showDevicesSelector() {
        if (mainSplitter != null) {
            if (pixelPerfectPanel != null) {
                screenViewer.start();
            }
            mainPanel.remove(graphViewButton.isSelected() ? mainSplitter : pixelPerfectPanel);
            mainPanel.add(deviceSelector, BorderLayout.CENTER);
            pixelPerfectPanel = mainSplitter = null;
            graphViewButton.setSelected(true);

            hideStatusBarComponents();

            saveMenuItem.setEnabled(false);
            showDevicesMenuItem.setEnabled(false);
            showDevicesButton.setEnabled(false);
            displayNodeButton.setEnabled(false);
            invalidateButton.setEnabled(false);
            requestLayoutButton.setEnabled(false);
            graphViewButton.setEnabled(false);
            pixelPerfectViewButton.setEnabled(false);

            if (currentDevice != null) {
                if (!DeviceBridge.isViewServerRunning(currentDevice)) {
                    DeviceBridge.startViewServer(currentDevice);
                }
                loadWindows().execute();
                windowsTableModel.setVisible(true);
            }

            validate();
            repaint();
        }
    }

    private void currentDeviceChanged() {
        if (currentDevice == null) {
            startButton.setEnabled(false);
            startMenuItem.setEnabled(false);
            stopButton.setEnabled(false);
            stopMenuItem.setEnabled(false);
            refreshButton.setEnabled(false);
            saveMenuItem.setEnabled(false);
            loadButton.setEnabled(false);
            displayNodeButton.setEnabled(false);
            invalidateButton.setEnabled(false);
            graphViewButton.setEnabled(false);
            pixelPerfectViewButton.setEnabled(false);
            requestLayoutButton.setEnabled(false);
            loadMenuItem.setEnabled(false);
        } else {
            loadMenuItem.setEnabled(true);
            checkForServerOnCurrentDevice();
        }
    }

    private void checkForServerOnCurrentDevice() {
        if (DeviceBridge.isViewServerRunning(currentDevice)) {
            startButton.setEnabled(false);
            startMenuItem.setEnabled(false);
            stopButton.setEnabled(true);
            stopMenuItem.setEnabled(true);
            loadButton.setEnabled(true);
            refreshButton.setEnabled(true);
        } else {
            startButton.setEnabled(true);
            startMenuItem.setEnabled(true);
            stopButton.setEnabled(false);
            stopMenuItem.setEnabled(false);
            loadButton.setEnabled(false);
            refreshButton.setEnabled(false);
        }
    }

    public void cleanupDevices() {
        for (IDevice device : devicesTableModel.getDevices()) {
            DeviceBridge.removeDeviceForward(device);
        }
    }

    private static void toggleColorOnSelect(JToggleButton button) {
        if (!OS.isMacOsX() || !OS.isLeopardOrLater()) {
            return;
        }

        button.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                JToggleButton button = (JToggleButton) event.getSource();
                if (button.isSelected()) {
                    button.setForeground(Color.WHITE);
                } else {
                    button.setForeground(Color.BLACK);
                }
            }
        });
    }

    private void updateFilter(DocumentEvent e) {
        final Document document = e.getDocument();
        try {
            updateFilteredNodes(document.getText(0, document.getLength()));
        } catch (BadLocationException e1) {
            e1.printStackTrace();
        }
    }

    private void updateFilteredNodes(String filterText) {
        final ViewNode root = scene.getRoot();
        try {
            final Pattern pattern = Pattern.compile(filterText, Pattern.CASE_INSENSITIVE);
            filterNodes(pattern, root);
        } catch (PatternSyntaxException e) {
            filterNodes(null, root);
        }
        repaint();
    }

    private void filterNodes(Pattern pattern, ViewNode root) {
        root.filter(pattern);

        for (ViewNode node : root.children) {
            filterNodes(pattern, node);
        }
    }

    public void beginTask() {
        progress.setVisible(true);
    }

    public void endTask() {
        progress.setVisible(false);
    }

    public SwingWorker<?, ?> showNodeCapture() {
        if (scene.getFocusedObject() == null) {
            return null;
        }
        return new CaptureNodeTask();
    }

    public SwingWorker<?, ?> startServer() {
        return new StartServerTask();
    }

    public SwingWorker<?, ?> stopServer() {
        return new StopServerTask();
    }

    public SwingWorker<?, ?> loadWindows() {
        return new LoadWindowsTask();
    }

    public SwingWorker<?, ?> loadGraph() {
        return new LoadGraphTask();
    }

    public SwingWorker<?, ?> invalidateView() {
        if (scene.getFocusedObject() == null) {
            return null;
        }
        return new InvalidateTask();
    }

    public SwingWorker<?, ?> requestLayout() {
        if (scene.getFocusedObject() == null) {
            return null;
        }
        return new RequestLayoutTask();
    }

    public SwingWorker<?, ?> saveSceneAsImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new PngFileFilter());
        int choice = chooser.showSaveDialog(sceneView);
        if (choice == JFileChooser.APPROVE_OPTION) {
            return new SaveSceneTask(chooser.getSelectedFile());
        } else {
            return null;
        }
    }

    private class InvalidateTask extends SwingWorker<Object, Void> {
        private String captureParams;

        private InvalidateTask() {
            captureParams = scene.getFocusedObject().toString();
            beginTask();
        }

        @Override
        @WorkerThread
        protected Object doInBackground() throws Exception {
            ViewManager.invalidate(currentDevice, currentWindow, captureParams);
            return null;
        }

        @Override
        protected void done() {
            endTask();
        }
    }

    private class RequestLayoutTask extends SwingWorker<Object, Void> {
        private String captureParams;

        private RequestLayoutTask() {
            captureParams = scene.getFocusedObject().toString();
            beginTask();
        }

        @Override
        @WorkerThread
        protected Object doInBackground() throws Exception {
            ViewManager.requestLayout(currentDevice, currentWindow, captureParams);
            return null;
        }

        @Override
        protected void done() {
            endTask();
        }
    }

    private class CaptureNodeTask extends SwingWorker<Image, Void> {
        private String captureParams;
        private ViewNode node;

        private CaptureNodeTask() {
            node = (ViewNode) scene.getFocusedObject();
            captureParams = node.toString();
            beginTask();
        }

        @Override
        @WorkerThread
        protected Image doInBackground() throws Exception {
            node.image = CaptureLoader.loadCapture(currentDevice, currentWindow, captureParams);
            return node.image;
        }

        @Override
        protected void done() {
            try {
                Image image = get();
                showCaptureWindow(node, captureParams, image);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } finally {
                endTask();
            }
        }
    }

    private class LoadWindowsTask extends SwingWorker<Window[], Void> {
        private LoadWindowsTask() {
            beginTask();
        }

        @Override
        @WorkerThread
        protected Window[] doInBackground() throws Exception {
            return WindowsLoader.loadWindows(currentDevice);
        }

        @Override
        protected void done() {
            try {
                windowsTableModel.clear();
                windowsTableModel.addWindows(get());
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                endTask();
            }
        }
    }

    private class StartServerTask extends SwingWorker<Object, Void> {
        public StartServerTask() {
            beginTask();
        }

        @Override
        @WorkerThread
        protected Object doInBackground() {
            DeviceBridge.startViewServer(currentDevice);
            return null;
        }

        @Override
        protected void done() {
            new LoadWindowsTask().execute();
            windowsTableModel.setVisible(true);
            checkForServerOnCurrentDevice();
            endTask();
        }
    }

    private class StopServerTask extends SwingWorker<Object, Void> {
        public StopServerTask() {
            beginTask();
        }

        @Override
        @WorkerThread
        protected Object doInBackground() {
            DeviceBridge.stopViewServer(currentDevice);
            return null;
        }

        @Override
        protected void done() {
            windowsTableModel.setVisible(false);
            windowsTableModel.clear();
            checkForServerOnCurrentDevice();
            endTask();
        }
    }

    private class LoadGraphTask extends SwingWorker<double[], Void> {
        public LoadGraphTask() {
            beginTask();
        }

        @Override
        @WorkerThread
        protected double[] doInBackground() {
            scene = ViewHierarchyLoader.loadScene(currentDevice, currentWindow);
            return ProfilesLoader.loadProfiles(currentDevice, currentWindow,
                    scene.getRoot().toString());
        }

        @Override
        protected void done() {
            try {
                createGraph(scene);
                updateProfiles(get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } finally {
                endTask();
            }
        }
    }

    private class SaveSceneTask extends SwingWorker<Object, Void> {
        private File file;

        private SaveSceneTask(File file) {
            this.file = file;
            beginTask();
        }

        @Override
        @WorkerThread
        protected Object doInBackground() {
            if (sceneView == null) {
                return null;
            }

            try {
                BufferedImage image = new BufferedImage(sceneView.getWidth(),
                        sceneView.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = image.createGraphics();
                sceneView.paint(g2);
                g2.dispose();
                ImageIO.write(image, "PNG", file);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        protected void done() {
            endTask();
        }
    }

    private class SceneFocusListener implements ObjectSceneListener {

        public void objectAdded(ObjectSceneEvent arg0, Object arg1) {
        }

        public void objectRemoved(ObjectSceneEvent arg0, Object arg1) {
        }

        public void objectStateChanged(ObjectSceneEvent arg0, Object arg1,
                ObjectState arg2, ObjectState arg3) {
        }

        public void selectionChanged(ObjectSceneEvent e, Set<Object> previousSelection,
                Set<Object> newSelection) {
        }

        public void highlightingChanged(ObjectSceneEvent arg0, Set<Object> arg1, Set<Object> arg2) {
        }

        public void hoverChanged(ObjectSceneEvent arg0, Object arg1, Object arg2) {
        }

        public void focusChanged(ObjectSceneEvent e, Object oldFocus, Object newFocus) {
            displayNodeButton.setEnabled(true);
            invalidateButton.setEnabled(true);
            requestLayoutButton.setEnabled(true);

            Set<Object> selection = new HashSet<Object>();
            selection.add(newFocus);
            scene.setSelectedObjects(selection);

            showProperties((ViewNode) newFocus);
            layoutView.repaint();
        }
    }

    private class NodeClickListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                showNodeCapture().execute();
            }
        }
    }

    private class WheelZoomListener implements MouseWheelListener {
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (zoomSlider != null) {
                int val = zoomSlider.getValue();
                val -= e.getWheelRotation() * 10;
                zoomSlider.setValue(val);
            }
        }
    }
    private class DevicesTableModel extends DefaultTableModel implements
            AndroidDebugBridge.IDeviceChangeListener {

        private ArrayList<IDevice> devices;

        private DevicesTableModel() {
            devices = new ArrayList<IDevice>();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Object getValueAt(int row, int column) {
            return devices.get(row);
        }

        @Override
        public String getColumnName(int column) {
            return "Devices";
        }

        @WorkerThread
        public void deviceConnected(final IDevice device) {
            DeviceBridge.setupDeviceForward(device);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    addDevice(device);
                }
            });
        }

        @WorkerThread
        public void deviceDisconnected(final IDevice device) {
            DeviceBridge.removeDeviceForward(device);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    removeDevice(device);
                }
            });
        }

        public void addDevice(IDevice device) {
            if (!devices.contains(device)) {
                devices.add(device);
                fireTableDataChanged();
            }
        }

        public void removeDevice(IDevice device) {
            if (device.equals(currentDevice)) {
                reset();
            }

            if (devices.contains(device)) {
                devices.remove(device);
                fireTableDataChanged();
            }
        }

        @WorkerThread
        public void deviceChanged(IDevice device, int changeMask) {
            if ((changeMask & IDevice.CHANGE_STATE) != 0 &&
                    device.isOnline()) {
                // if the device state changed and it's now online, we set up its port forwarding.
                DeviceBridge.setupDeviceForward(device);
            } else if (device == currentDevice && (changeMask & IDevice.CHANGE_CLIENT_LIST) != 0) {
                // if the changed device is the current one and the client list changed, we update
                // the UI.
                loadWindows().execute();
                windowsTableModel.setVisible(true);
            }
        }

        @Override
        public int getRowCount() {
            return devices == null ? 0 : devices.size();
        }

        public IDevice getDevice(int index) {
            return index < devices.size() ? devices.get(index) : null;
        }

        public IDevice[] getDevices() {
            return devices.toArray(new IDevice[devices.size()]);
        }
    }

    private static class WindowsTableModel extends DefaultTableModel {
        private ArrayList<Window> windows;
        private boolean visible;

        private WindowsTableModel() {
            windows = new ArrayList<Window>();
            windows.add(Window.FOCUSED_WINDOW);
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public String getColumnName(int column) {
            return "Windows";
        }

        @Override
        public Object getValueAt(int row, int column) {
            return windows.get(row);
        }

        @Override
        public int getRowCount() {
            return !visible || windows == null ? 0 : windows.size();
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
            fireTableDataChanged();
        }

        public void addWindow(Window window) {
            windows.add(window);
            fireTableDataChanged();
        }

        public void addWindows(Window[] windowsList) {
            //noinspection ManualArrayToCollectionCopy
            for (Window window : windowsList) {
                windows.add(window);
            }
            fireTableDataChanged();
        }

        public void clear() {
            windows.clear();
            windows.add(Window.FOCUSED_WINDOW);
        }

        public Window getWindow(int index) {
            return windows.get(index);
        }
    }

    private class DeviceSelectedListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }

            int row = devices.getSelectedRow();
            if (row >= 0) {
                currentDevice = devicesTableModel.getDevice(row);
                currentDeviceChanged();
                if (currentDevice != null) {
                    if (!DeviceBridge.isViewServerRunning(currentDevice)) {
                        DeviceBridge.startViewServer(currentDevice);
                        checkForServerOnCurrentDevice();
                    }
                    loadWindows().execute();
                    windowsTableModel.setVisible(true);
                }
            } else {
                currentDevice = null;
                currentDeviceChanged();
                windowsTableModel.setVisible(false);
                windowsTableModel.clear();
            }
        }
    }

    private class WindowSelectedListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }

            int row = windows.getSelectedRow();
            if (row >= 0) {
                currentWindow = windowsTableModel.getWindow(row);
            } else {
                currentWindow = Window.FOCUSED_WINDOW;
            }
        }
    }

    private static class ViewsTreeCellRenderer extends DefaultTreeCellRenderer {
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                boolean expanded, boolean leaf, int row, boolean hasFocus) {

            final String name = ((ViewNode) value).name;
            value = name.substring(name.lastIndexOf('.') + 1, name.lastIndexOf('@'));
            return super.getTreeCellRendererComponent(tree, value, selected, expanded,
                    leaf, row, hasFocus);
        }
    }
}
