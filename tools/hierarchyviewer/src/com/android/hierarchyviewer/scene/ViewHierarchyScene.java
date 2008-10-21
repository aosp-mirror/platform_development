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

package com.android.hierarchyviewer.scene;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.anchor.AnchorFactory;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.graph.GraphScene;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Widget;

public class ViewHierarchyScene extends GraphScene<ViewNode, String> {
    private ViewNode root;
    private LayerWidget widgetLayer;
    private LayerWidget connectionLayer;

    private WidgetAction moveAction = ActionFactory.createMoveAction();

    public ViewHierarchyScene() {
        widgetLayer = new LayerWidget(this);
        connectionLayer = new LayerWidget(this);

        addChild(widgetLayer);
        addChild(connectionLayer);
    }
    
    public ViewNode getRoot() {
        return root;
    }
    
    void setRoot(ViewNode root) {
        this.root = root;
    }

    @Override
    protected Widget attachNodeWidget(ViewNode node) {
        Widget widget = createBox(node.name, node.id);
        widget.getActions().addAction(createSelectAction());
        widget.getActions().addAction(moveAction);
        widgetLayer.addChild(widget);
        return widget;
    }

    private Widget createBox(String node, String id) {
        Widget box = new GradientWidget(this);
        box.setLayout(LayoutFactory.createVerticalFlowLayout());
        box.setBorder(BorderFactory.createLineBorder(2, Color.BLACK));
        box.setOpaque(true);

        LabelWidget label = new LabelWidget(this);
        label.setFont(getDefaultFont().deriveFont(Font.PLAIN, 12.0f));
        label.setLabel(getShortName(node));
        label.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));
        label.setAlignment(LabelWidget.Alignment.CENTER);

        box.addChild(label);
        
        label = new LabelWidget(this);
        label.setFont(getDefaultFont().deriveFont(Font.PLAIN, 10.0f));
        label.setLabel(getAddress(node));
        label.setBorder(BorderFactory.createEmptyBorder(3, 6, 0, 6));
        label.setAlignment(LabelWidget.Alignment.CENTER);
        
        box.addChild(label);
        
        label = new LabelWidget(this);
        label.setFont(getDefaultFont().deriveFont(Font.PLAIN, 10.0f));
        label.setLabel(id);
        label.setBorder(BorderFactory.createEmptyBorder(3, 6, 6, 6));
        label.setAlignment(LabelWidget.Alignment.CENTER);
        
        box.addChild(label);

        return box;
    }
    
    private static String getAddress(String name) {
        String[] nameAndHashcode = name.split("@");
        return "@" + nameAndHashcode[1];
    }
    
    private static String getShortName(String name) {
        String[] nameAndHashcode = name.split("@");
        String[] packages = nameAndHashcode[0].split("\\.");
        return packages[packages.length - 1];
    }

    @Override
    protected Widget attachEdgeWidget(String edge) {
        ConnectionWidget connectionWidget = new ConnectionWidget(this);
        connectionLayer.addChild(connectionWidget);
        return connectionWidget;
    }

    @Override
    protected void attachEdgeSourceAnchor(String edge, ViewNode oldSourceNode, ViewNode sourceNode) {
        final ConnectionWidget connection = (ConnectionWidget) findWidget(edge);
        final Widget source = findWidget(sourceNode);
        connection.bringToBack();
        source.bringToFront();
        connection.setSourceAnchor(AnchorFactory.createRectangularAnchor(source));
    }

    @Override
    protected void attachEdgeTargetAnchor(String edge, ViewNode oldTargetNode, ViewNode targetNode) {
        final ConnectionWidget connection = (ConnectionWidget) findWidget(edge);
        final Widget target = findWidget(targetNode);
        connection.bringToBack();
        target.bringToFront();
        connection.setTargetAnchor(AnchorFactory.createRectangularAnchor(target));
    }
    
    private static class GradientWidget extends Widget {
        public static final GradientPaint BLUE_EXPERIENCE = new GradientPaint(
                new Point2D.Double(0, 0),
                new Color(168, 204, 241),
                new Point2D.Double(0, 1),
                new Color(44, 61, 146));
        public static final GradientPaint MAC_OSX_SELECTED = new GradientPaint(
                new Point2D.Double(0, 0),
                new Color(81, 141, 236),
                new Point2D.Double(0, 1),
                new Color(36, 96, 192));
        public static final GradientPaint MAC_OSX = new GradientPaint(
                new Point2D.Double(0, 0),
                new Color(167, 210, 250),
                new Point2D.Double(0, 1),
                new Color(99, 147, 206));
        public static final GradientPaint AERITH = new GradientPaint(
                new Point2D.Double(0, 0),
                Color.WHITE,
                new Point2D.Double(0, 1),
                new Color(64, 110, 161));
        public static final GradientPaint GRAY = new GradientPaint(
                new Point2D.Double(0, 0),
                new Color(226, 226, 226),
                new Point2D.Double(0, 1),
                new Color(250, 248, 248));
        public static final GradientPaint RED_XP = new GradientPaint(
                new Point2D.Double(0, 0),
                new Color(236, 81, 81),
                new Point2D.Double(0, 1),
                new Color(192, 36, 36));
        public static final GradientPaint NIGHT_GRAY = new GradientPaint(
                new Point2D.Double(0, 0),
                new Color(102, 111, 127),
                new Point2D.Double(0, 1),
                new Color(38, 45, 61));
        public static final GradientPaint NIGHT_GRAY_LIGHT = new GradientPaint(
                new Point2D.Double(0, 0),
                new Color(129, 138, 155),
                new Point2D.Double(0, 1),
                new Color(58, 66, 82));

        private static Color UNSELECTED = Color.BLACK;
        private static Color SELECTED = Color.WHITE;

        private boolean isSelected = false;
        private GradientPaint gradient = MAC_OSX_SELECTED;

        public GradientWidget(ViewHierarchyScene scene) {
            super(scene);
        }

        @Override
        protected void notifyStateChanged(ObjectState previous, ObjectState state) {
            super.notifyStateChanged(previous, state);
            isSelected = state.isSelected() || state.isFocused() || state.isWidgetFocused();

            for (Widget child : getChildren()) {
                child.setForeground(isSelected ? SELECTED : UNSELECTED);
            }

            repaint();
        }

        @Override
        protected void paintBackground() {
            super.paintBackground();

            Graphics2D g2 = getGraphics();
            Rectangle bounds = getBounds();
            
            if (!isSelected) {
                g2.setColor(Color.WHITE);
            } else {
                g2.setPaint(new GradientPaint(bounds.x, bounds.y, gradient.getColor1(),
                        bounds.x, bounds.x + bounds.height, gradient.getColor2()));
            }
            g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }
}
