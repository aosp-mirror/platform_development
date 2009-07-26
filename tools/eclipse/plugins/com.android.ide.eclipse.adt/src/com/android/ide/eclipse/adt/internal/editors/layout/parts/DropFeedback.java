/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.editors.layout.parts;

import com.android.ide.eclipse.adt.internal.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.layout.LayoutConstants;
import com.android.ide.eclipse.adt.internal.editors.layout.LayoutEditor.UiEditorActions;
import com.android.ide.eclipse.adt.internal.editors.layout.parts.UiLayoutEditPart.HighlightInfo;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Utility methods used when dealing with dropping EditPart on the GLE.
 * <p/>
 * This class uses some temporary static storage to avoid excessive allocations during
 * drop operations. It is expected to only be invoked from the main UI thread with no
 * concurrent access.
 */
class DropFeedback {

    private static final int TOP    = 0;
    private static final int LEFT   = 1;
    private static final int BOTTOM = 2;
    private static final int RIGHT  = 3;
    private static final int MAX_DIR = RIGHT;
    
    private static final int sOppositeDirection[] = { BOTTOM, RIGHT, TOP, LEFT };

    private static final UiElementEditPart sTempClosests[] = new UiElementEditPart[4];
    private static final int sTempMinDists[] = new int[4];
    

    /**
     * Target information computed from a drop on a RelativeLayout.
     * We need only one instance of this and it is sRelativeInfo.
     */
    private static class RelativeInfo {
        /** The two target parts 0 and 1. They can be null, meaning a border is used.
         *  The direction from part 0 to 1 is always to-the-right or to-the-bottom. */
        final UiElementEditPart targetParts[] = new UiElementEditPart[2];
        /** Direction from the anchor part to the drop point. */
        int direction;
        /** The index of the "anchor" part, i.e. the closest one selected by the drop.
         *  This can be either 0 or 1. The corresponding part can be null. */
        int anchorIndex;
    }

    /** The single RelativeInfo used to compute results from a drop on a RelativeLayout */
    private static final RelativeInfo sRelativeInfo = new RelativeInfo();
    /** A temporary array of 2 {@link UiElementEditPart} to avoid allocations. */
    private static final UiElementEditPart sTempTwoParts[] = new UiElementEditPart[2];
    

    private DropFeedback() {
    }

    
    //----- Package methods called by users of this helper class -----
    
    
    /**
     * This method is used by {@link ElementCreateCommand#execute()} when a new item
     * needs to be "dropped" in the current XML document. It creates the new item using
     * the given descriptor as a child of the given parent part.
     * 
     * @param parentPart The parent part.
     * @param descriptor The descriptor for the new XML element.
     * @param where      The drop location (in parent coordinates)
     * @param actions    The helper that actually modifies the XML model.
     */
    static void addElementToXml(UiElementEditPart parentPart,
            ElementDescriptor descriptor, Point where,
            UiEditorActions actions) {
        
        String layoutXmlName = getXmlLocalName(parentPart);
        RelativeInfo info = null;
        UiElementEditPart sibling = null;
        
        // TODO consider merge like a vertical layout
        // TODO consider TableLayout like a linear
        if (LayoutConstants.LINEAR_LAYOUT.equals(layoutXmlName)) {
            sibling = findLinearTarget(parentPart, where)[1];
            
        } else if (LayoutConstants.RELATIVE_LAYOUT.equals(layoutXmlName)) {
            info = findRelativeTarget(parentPart, where, sRelativeInfo);
            if (info != null) {
                sibling = info.targetParts[info.anchorIndex];
                sibling = getNextUiSibling(sibling);
            }
        }

        if (actions != null) {
            UiElementNode uiSibling = sibling != null ? sibling.getUiNode() : null;
            UiElementNode uiParent = parentPart.getUiNode();
            UiElementNode uiNode = actions.addElement(uiParent, uiSibling, descriptor,
                    false /*updateLayout*/);
            
            if (LayoutConstants.ABSOLUTE_LAYOUT.equals(layoutXmlName)) {
                adjustAbsoluteAttributes(uiNode, where);
            } else if (LayoutConstants.RELATIVE_LAYOUT.equals(layoutXmlName)) {
                adustRelativeAttributes(uiNode, info);
            }
        }
    }

    /**
     * This method is used by {@link UiLayoutEditPart#showDropTarget(Point)} to compute
     * highlight information when a drop target is moved over a valid drop area.
     * <p/>
     * Since there are no "out" parameters in Java, all the information is returned
     * via the {@link HighlightInfo} structure passed as parameter. 
     * 
     * @param parentPart    The parent part, always a layout.
     * @param highlightInfo A structure where result is stored to perform highlight.
     * @param where         The target drop point, in parent's coordinates
     * @return The {@link HighlightInfo} structured passed as a parameter, for convenience.
     */
    static HighlightInfo computeDropFeedback(UiLayoutEditPart parentPart,
            HighlightInfo highlightInfo,
            Point where) {
        String layoutType = getXmlLocalName(parentPart);
        
        if (LayoutConstants.ABSOLUTE_LAYOUT.equals(layoutType)) {
            highlightInfo.anchorPoint = where;
            
        } else if (LayoutConstants.LINEAR_LAYOUT.equals(layoutType)) {
            boolean isVertical = isVertical(parentPart);

            highlightInfo.childParts = findLinearTarget(parentPart, where);
            computeLinearLine(parentPart, isVertical, highlightInfo);
            
        } else if (LayoutConstants.RELATIVE_LAYOUT.equals(layoutType)) {

            RelativeInfo info = findRelativeTarget(parentPart, where, sRelativeInfo);
            if (info != null) {
                highlightInfo.childParts = sRelativeInfo.targetParts;
                computeRelativeLine(parentPart, info, highlightInfo);
            }
        }
        
        return highlightInfo;
    }
    
    
    //----- Misc utilities -----
    
    /**
     * Returns the next UI sibling of this part, i.e. the element which is just after in
     * the UI/XML order in the same parent. Returns null if there's no such part.
     * <p/>
     * Note: by "UI sibling" here we mean the sibling in the UiNode hierarchy. By design the
     * UiNode model has the <em>exact</em> same order as the XML model. This has nothing to do
     * with the "user interface" order that you see on the rendered Android layouts (e.g. for
     * LinearLayout they are the same but for AbsoluteLayout or RelativeLayout the UI/XML model
     * order can be vastly different from the user interface order.)
     */
    private static UiElementEditPart getNextUiSibling(UiElementEditPart part) {
        if (part != null) {
            UiElementNode uiNode = part.getUiNode();
            if (uiNode != null) {
                uiNode = uiNode.getUiNextSibling();
            }
            if (uiNode != null) {
                for (Object childPart : part.getParent().getChildren()) {
                    if (childPart instanceof UiElementEditPart &&
                            ((UiElementEditPart) childPart).getUiNode() == uiNode) {
                        return (UiElementEditPart) childPart;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the XML local name of the ui node associated with this edit part or null.
     */
    private static String getXmlLocalName(UiElementEditPart editPart) {
        UiElementNode uiNode = editPart.getUiNode();
        if (uiNode != null) {
            ElementDescriptor desc = uiNode.getDescriptor();
            if (desc != null) {
                return desc.getXmlLocalName();
            }
        }
        return null;
    }

    /**
     * Adjusts the attributes of a new node dropped in an AbsoluteLayout.
     * 
     * @param uiNode The new node being dropped.
     * @param where  The drop location (in parent coordinates)
     */
    private static void adjustAbsoluteAttributes(final UiElementNode uiNode, final Point where) {
        if (where == null) {
            return;
        }
        uiNode.getEditor().editXmlModel(new Runnable() {
            public void run() {
                uiNode.setAttributeValue(LayoutConstants.ATTR_LAYOUT_X,
                        String.format(LayoutConstants.VALUE_N_DIP, where.x),
                        false /* override */);
                uiNode.setAttributeValue(LayoutConstants.ATTR_LAYOUT_Y,
                        String.format(LayoutConstants.VALUE_N_DIP, where.y),
                        false /* override */);

                uiNode.commitDirtyAttributesToXml();
            }
        });
    }

    /**
     * Adjusts the attributes of a new node dropped in a RelativeLayout:
     * <ul>
     * <li> anchor part: the one the user selected (or the closest) and to which the new one
     *      will "attach". The anchor part can be null, either because the layout is currently
     *      empty or the user is attaching to an existing empty border.
     * <li> direction: the direction from the anchor part to the drop point. That's also the
     *      direction from the anchor part to the new part. 
     * <li> the new node; it is created either after the anchor for right or top directions
     *      or before the anchor for left or bottom directions. This means the new part can 
     *      reference the id of the anchor part. 
     * </ul>
     * 
     * Several cases:
     * <ul>
     * <li> set:  layout_above/below/toLeftOf/toRightOf to point to the anchor.
     * <li> copy: layout_centerHorizontal for top/bottom directions
     * <li> copy: layout_centerVertical for left/right directions.
     * <li> copy: layout_above/below/toLeftOf/toRightOf for the orthogonal direction
     *            (i.e. top/bottom or left/right.)
     * </ul>
     * 
     * @param uiNode The new node being dropped.
     * @param info   The context computed by {@link #findRelativeTarget(UiElementEditPart, Point, RelativeInfo)}.
     */
    private static void adustRelativeAttributes(final UiElementNode uiNode, RelativeInfo info) {
        if (uiNode == null || info == null) {
            return;
        }
        
        final UiElementEditPart anchorPart = info.targetParts[info.anchorIndex];  // can be null       
        final int direction = info.direction;
        
        uiNode.getEditor().editXmlModel(new Runnable() {
            public void run() {
                HashMap<String, String> map = new HashMap<String, String>();

                UiElementNode anchorUiNode = anchorPart != null ? anchorPart.getUiNode() : null;
                String anchorId = anchorUiNode != null
                                    ? anchorUiNode.getAttributeValue("id")          //$NON-NLS-1$
                                    : null;

                if (anchorId == null) {
                    anchorId = DescriptorsUtils.getFreeWidgetId(anchorUiNode);
                    anchorUiNode.setAttributeValue("id", anchorId, true /*override*/); //$NON-NLS-1$
                }
                
                if (anchorId != null) {
                    switch(direction) {
                    case TOP:
                        map.put(LayoutConstants.ATTR_LAYOUT_ABOVE, anchorId);
                        break;
                    case BOTTOM:
                        map.put(LayoutConstants.ATTR_LAYOUT_BELOW, anchorId);
                        break;
                    case LEFT:
                        map.put(LayoutConstants.ATTR_LAYOUT_TO_LEFT_OF, anchorId);
                        break;
                    case RIGHT:
                        map.put(LayoutConstants.ATTR_LAYOUT_TO_RIGHT_OF, anchorId);
                        break;
                    }

                    switch(direction) {
                    case TOP:
                    case BOTTOM:
                        map.put(LayoutConstants.ATTR_LAYOUT_CENTER_HORIZONTAL,
                                anchorUiNode.getAttributeValue(
                                        LayoutConstants.ATTR_LAYOUT_CENTER_HORIZONTAL));

                        map.put(LayoutConstants.ATTR_LAYOUT_TO_LEFT_OF,
                                anchorUiNode.getAttributeValue(
                                        LayoutConstants.ATTR_LAYOUT_TO_LEFT_OF));
                        map.put(LayoutConstants.ATTR_LAYOUT_TO_RIGHT_OF,
                                anchorUiNode.getAttributeValue(
                                        LayoutConstants.ATTR_LAYOUT_TO_RIGHT_OF));
                        break;
                    case LEFT:
                    case RIGHT:
                        map.put(LayoutConstants.ATTR_LAYOUT_CENTER_VERTICAL,
                                anchorUiNode.getAttributeValue(
                                        LayoutConstants.ATTR_LAYOUT_CENTER_VERTICAL));
                        map.put(LayoutConstants.ATTR_LAYOUT_ALIGN_BASELINE,
                                anchorUiNode.getAttributeValue(
                                        LayoutConstants.ATTR_LAYOUT_ALIGN_BASELINE));
                        
                        map.put(LayoutConstants.ATTR_LAYOUT_ABOVE,
                                anchorUiNode.getAttributeValue(LayoutConstants.ATTR_LAYOUT_ABOVE));
                        map.put(LayoutConstants.ATTR_LAYOUT_BELOW,
                                anchorUiNode.getAttributeValue(LayoutConstants.ATTR_LAYOUT_BELOW));
                        break;
                    }
                } else {
                    // We don't have an anchor node. Assume we're targeting a border and align
                    // to the parent.
                    switch(direction) {
                    case TOP:
                        map.put(LayoutConstants.ATTR_LAYOUT_ALIGN_PARENT_TOP,
                                LayoutConstants.VALUE_TRUE);
                        break;
                    case BOTTOM:
                        map.put(LayoutConstants.ATTR_LAYOUT_ALIGN_PARENT_BOTTOM,
                                LayoutConstants.VALUE_TRUE);
                        break;
                    case LEFT:
                        map.put(LayoutConstants.ATTR_LAYOUT_ALIGN_PARENT_LEFT,
                                LayoutConstants.VALUE_TRUE);
                        break;
                    case RIGHT:
                        map.put(LayoutConstants.ATTR_LAYOUT_ALIGN_PARENT_RIGHT,
                                LayoutConstants.VALUE_TRUE);
                        break;
                    }
                }
                
                for (Entry<String, String> entry : map.entrySet()) {
                    uiNode.setAttributeValue(entry.getKey(), entry.getValue(), true /* override */);
                }
                uiNode.commitDirtyAttributesToXml();
            }
        });
    }


    //----- LinearLayout --------

    /**
     * For a given parent edit part that MUST represent a LinearLayout, finds the
     * element before which the location points.
     * <p/>
     * This computes the edit part that corresponds to what will be the "next sibling" of the new
     * element.
     * <p/>
     * It returns null if it can't be determined, in which case the element will be added at the
     * end of the parent child list.
     * 
     * @return The edit parts that correspond to what will be the "prev" and "next sibling" of the
     *         new element. The previous sibling can be null if adding before the first element.
     *         The next sibling can be null if adding after the last element.
     */
    private static UiElementEditPart[] findLinearTarget(UiElementEditPart parent, Point point) {
        // default orientation is horizontal
        boolean isVertical = isVertical(parent);
        
        int target = isVertical ? point.y : point.x;
        
        UiElementEditPart prev = null;
        UiElementEditPart next = null;

        for (Object child : parent.getChildren()) {
            if (child instanceof UiElementEditPart) {
                UiElementEditPart childPart = (UiElementEditPart) child;
                Point p = childPart.getBounds().getCenter();
                int middle = isVertical ? p.y : p.x;
                if (target < middle) {
                    next = childPart;
                    break;
                }
                prev = childPart;
            }
        }
        
        sTempTwoParts[0] = prev;
        sTempTwoParts[1] = next;
        return sTempTwoParts;
    }

    /**
     * Computes the highlight line between two parts.
     * <p/>
     * The two parts are listed in HighlightInfo.childParts[2]. Any of the parts
     * can be null.
     * The result is stored in HighlightInfo.
     * <p/>
     * Caller must clear the HighlightInfo as appropriate before this call.
     * 
     * @param parentPart    The parent part, always a layout.
     * @param isVertical    True for vertical parts, thus computing an horizontal line.
     * @param highlightInfo The in-out highlight info.
     */
    private static void computeLinearLine(UiLayoutEditPart parentPart,
            boolean isVertical, HighlightInfo highlightInfo) {
        Rectangle r = parentPart.getBounds();

        if (isVertical) {
            Point p = null;
            UiElementEditPart part = highlightInfo.childParts[0];
            if (part != null) {
                p = part.getBounds().getBottom();
            } else {
                part = highlightInfo.childParts[1];
                if (part != null) {
                    p = part.getBounds().getTop();
                }
            }
            if (p != null) {
                // horizontal line with middle anchor point
                highlightInfo.tempPoints[0].setLocation(0, p.y);
                highlightInfo.tempPoints[1].setLocation(r.width, p.y);
                highlightInfo.linePoints = highlightInfo.tempPoints;
                highlightInfo.anchorPoint = p.setLocation(r.width / 2, p.y);
            }
        } else {
            Point p = null;
            UiElementEditPart part = highlightInfo.childParts[0];
            if (part != null) {
                p = part.getBounds().getRight();
            } else {
                part = highlightInfo.childParts[1];
                if (part != null) {
                    p = part.getBounds().getLeft();
                }
            }
            if (p != null) {
                // vertical line with middle anchor point
                highlightInfo.tempPoints[0].setLocation(p.x, 0);
                highlightInfo.tempPoints[1].setLocation(p.x, r.height);
                highlightInfo.linePoints = highlightInfo.tempPoints;
                highlightInfo.anchorPoint = p.setLocation(p.x, r.height / 2);
            }
        }
    }

    /**
     * Returns true if the linear layout is marked as vertical.
     * 
     * @param parent The a layout part that must be a LinearLayout 
     * @return True if the linear layout has a vertical orientation attribute.
     */
    private static boolean isVertical(UiElementEditPart parent) {
        String orientation = parent.getStringAttr("orientation");     //$NON-NLS-1$
        boolean isVertical = "vertical".equals(orientation) ||        //$NON-NLS-1$ 
                             "1".equals(orientation);                 //$NON-NLS-1$
        return isVertical;
    }

    
    //----- RelativeLayout --------

    /**
     * Finds the "target" relative layout item for the drop operation & feedback.
     * <p/>
     * If the drop point is exactly on a current item, simply returns the side the drop will occur
     * compared to the center of that element. For the actual XML, we'll need to insert *after*
     * that element to make sure that referenced are defined in the right order.
     * In that case the result contains two elements, the second one always being on the right or
     * bottom side of the first one. When insert in XML, we want to insert right before that
     * second element or at the end of the child list if the second element is null.
     * <p/>
     * If the drop point is not exactly on a current element, find the closest in each
     * direction and align with the two closest of these.
     * 
     * @return null if we fail to find anything (such as there are currently no items to compare
     *         with); otherwise fills the {@link RelativeInfo} and return it.
     */
    private static RelativeInfo findRelativeTarget(UiElementEditPart parent,
            Point point,
            RelativeInfo outInfo) {
        
        for (int i = 0; i < 4; i++) {
            sTempMinDists[i] = Integer.MAX_VALUE;
            sTempClosests[i] = null;
        }

        
        for (Object child : parent.getChildren()) {
            if (child instanceof UiElementEditPart) {
                UiElementEditPart childPart = (UiElementEditPart) child;
                Rectangle r = childPart.getBounds();
                if (r.contains(point)) {
                    
                    float rx = ((float)(point.x - r.x) / (float)r.width ) - 0.5f;
                    float ry = ((float)(point.y - r.y) / (float)r.height) - 0.5f;

                    /*   TOP
                     *  \   /
                     *   \ /
                     * L  X  R
                     *   / \
                     *  /   \
                     *   BOT
                     */

                    int index = 0;
                    if (Math.abs(rx) >= Math.abs(ry)) {
                        if (rx < 0) {
                            outInfo.direction = LEFT;
                            index = 1;
                        } else {
                            outInfo.direction = RIGHT;
                        }
                    } else {
                        if (ry < 0) {
                            outInfo.direction = TOP;
                            index = 1;
                        } else {
                            outInfo.direction = BOTTOM;
                        }
                    }

                    outInfo.anchorIndex = index;
                    outInfo.targetParts[index] = childPart;
                    outInfo.targetParts[1 - index] = findClosestPart(childPart,
                            outInfo.direction);

                    return outInfo;
                }
                
                computeClosest(point, childPart, sTempClosests, sTempMinDists, TOP);
                computeClosest(point, childPart, sTempClosests, sTempMinDists, LEFT);
                computeClosest(point, childPart, sTempClosests, sTempMinDists, BOTTOM);
                computeClosest(point, childPart, sTempClosests, sTempMinDists, RIGHT);
            }
        }
        
        UiElementEditPart closest = null;
        int minDist = Integer.MAX_VALUE;
        int minDir = -1;
        
        for (int i = 0; i <= MAX_DIR; i++) {
            if (sTempClosests[i] != null && sTempMinDists[i] < minDist) {
                closest = sTempClosests[i];
                minDist = sTempMinDists[i];
                minDir = i;
            }
        }
        
        if (closest != null) {
            int index = 0;
            switch(minDir) {
            case TOP:
            case LEFT:
                index = 0;
                break;
            case BOTTOM:
            case RIGHT:
                index = 1;
                break;
            }
            outInfo.anchorIndex = index;
            outInfo.targetParts[index] = closest;
            outInfo.targetParts[1 - index] = findClosestPart(closest, sOppositeDirection[minDir]);
            outInfo.direction = sOppositeDirection[minDir];
            return outInfo;
        }

        return null;
    }

    /**
     * Computes the highlight line for a drop on a RelativeLayout.
     * <p/>
     * The line is always placed on the side of the anchor part indicated by the
     * direction. The direction always point from the anchor part to the drop point.
     * <p/>
     * If there's no anchor part, use the other one with a reversed direction.
     * <p/>
     * On output, this updates the {@link HighlightInfo}.
     */
    private static void computeRelativeLine(UiLayoutEditPart parentPart,
            RelativeInfo relInfo,
            HighlightInfo highlightInfo) {

        UiElementEditPart[] parts = relInfo.targetParts;
        int dir = relInfo.direction;
        int index = relInfo.anchorIndex;
        UiElementEditPart part = parts[index];

        if (part == null) {
            dir = sOppositeDirection[dir];
            part = parts[1 - index];
        }
        if (part == null) {
            // give up if both parts are null
            return;
        }

        Rectangle r = part.getBounds();
        Point p = null;
        switch(dir) {
        case TOP:
            p = r.getTop();
            break;
        case BOTTOM:
            p = r.getBottom();
            break;
        case LEFT:
            p = r.getLeft();
            break;
        case RIGHT:
            p = r.getRight();
            break;
        }

        highlightInfo.anchorPoint = p;

        r = parentPart.getBounds();
        switch(dir) {
        case TOP:
        case BOTTOM:
            // horizontal line with middle anchor point
            highlightInfo.tempPoints[0].setLocation(0, p.y);
            highlightInfo.tempPoints[1].setLocation(r.width, p.y);
            highlightInfo.linePoints = highlightInfo.tempPoints;
            highlightInfo.anchorPoint = p;
            break;
        case LEFT:
        case RIGHT:
            // vertical line with middle anchor point
            highlightInfo.tempPoints[0].setLocation(p.x, 0);
            highlightInfo.tempPoints[1].setLocation(p.x, r.height);
            highlightInfo.linePoints = highlightInfo.tempPoints;
            highlightInfo.anchorPoint = p;
            break;
        }
    }

    /**
     * Given a certain reference point (drop point), computes the distance to the given
     * part in the given direction. For example if direction is top, only accepts parts which
     * bottom is above the reference point, computes their distance and then updates the
     * current minimal distances and current closest parts arrays accordingly.
     */
    private static void computeClosest(Point refPoint,
            UiElementEditPart compareToPart,
            UiElementEditPart[] currClosests,
            int[] currMinDists,
            int direction) {
        Rectangle r = compareToPart.getBounds();

        Point p = null;
        boolean usable = false;
        
        switch(direction) {
        case TOP:
            p = r.getBottom();
            usable = p.y <= refPoint.y;
            break;
        case BOTTOM:
            p = r.getTop();
            usable = p.y >= refPoint.y;
            break;
        case LEFT:
            p = r.getRight();
            usable = p.x <= refPoint.x;
            break;
        case RIGHT:
            p = r.getLeft();
            usable = p.x >= refPoint.x;
            break;
        }

        if (usable) {
            int d = p.getDistance2(refPoint);
            if (d < currMinDists[direction]) {
                currMinDists[direction] = d;
                currClosests[direction] = compareToPart;
            }
        }
    }

    /**
     * Given a reference parts, finds the closest part in the parent in the given direction.
     * For example if direction is top, finds the closest sibling part which is above the
     * reference part and non-overlapping (they can touch.)
     */
    private static UiElementEditPart findClosestPart(UiElementEditPart referencePart,
            int direction) {
        if (referencePart == null || referencePart.getParent() == null) {
            return null;
        }
        
        Rectangle r = referencePart.getBounds();
        Point ref = null;
        switch(direction) {
        case TOP:
            ref = r.getTop();
            break;
        case BOTTOM:
            ref = r.getBottom();
            break;
        case LEFT:
            ref = r.getLeft();
            break;
        case RIGHT:
            ref = r.getRight();
            break;
        }
        
        int minDist = Integer.MAX_VALUE;
        UiElementEditPart closestPart = null;
        
        for (Object childPart : referencePart.getParent().getChildren()) {
            if (childPart != referencePart && childPart instanceof UiElementEditPart) {
                r = ((UiElementEditPart) childPart).getBounds();
                Point p = null;
                boolean usable = false;
                
                switch(direction) {
                case TOP:
                    p = r.getBottom();
                    usable = p.y <= ref.y;
                    break;
                case BOTTOM:
                    p = r.getTop();
                    usable = p.y >= ref.y;
                    break;
                case LEFT:
                    p = r.getRight();
                    usable = p.x <= ref.x;
                    break;
                case RIGHT:
                    p = r.getLeft();
                    usable = p.x >= ref.x;
                    break;
                }

                if (usable) {
                    int d = p.getDistance2(ref);
                    if (d < minDist) {
                        minDist = d;
                        closestPart = (UiElementEditPart) childPart;
                    }
                }
            }
        }
        
        return closestPart;
    }

}
