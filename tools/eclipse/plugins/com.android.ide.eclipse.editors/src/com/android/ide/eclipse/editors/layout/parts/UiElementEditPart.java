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

package com.android.ide.eclipse.editors.layout.parts;

import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.editors.uimodel.IUiUpdateListener;
import com.android.ide.eclipse.editors.uimodel.UiElementNode;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.SelectionEditPolicy;
import org.eclipse.gef.tools.SelectEditPartTracker;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.List;

/**
 * An {@link EditPart} for a {@link UiElementNode}.
 */
public abstract class UiElementEditPart extends AbstractGraphicalEditPart
    implements IUiUpdateListener {
    
    public UiElementEditPart(UiElementNode uiElementNode) {
        setModel(uiElementNode);
    }

    //-------------------------
    // Derived classes must define these

    abstract protected void hideSelection();
    abstract protected void showSelection();

    //-------------------------
    // Base class overrides
    
    @Override
    public DragTracker getDragTracker(Request request) {
        return new SelectEditPartTracker(this);
    }

    @Override
    protected void createEditPolicies() {
        installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new SelectionEditPolicy() {
            @Override
            protected void hideSelection() {
                UiElementEditPart.this.hideSelection();
            }

            @Override
            protected void showSelection() {
                UiElementEditPart.this.showSelection();
            }
        });
        // TODO add editing policies
    }
    
    /* (non-javadoc)
     * Returns a List containing the children model objects.
     * Must not return null, instead use the super which returns an empty list.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected List getModelChildren() {
        return getUiNode().getUiChildren();
    }

    @Override
    public void activate() {
        super.activate();
        getUiNode().addUpdateListener(this);
    }
    
    @Override
    public void deactivate() {
        super.deactivate();
        getUiNode().removeUpdateListener(this);
    }

    @Override
    protected void refreshVisuals() {
        if (getFigure().getParent() != null) {
            ((GraphicalEditPart) getParent()).setLayoutConstraint(this, getFigure(), getBounds());
        }
        
        // update the visuals of the children as well
        refreshChildrenVisuals();
    }
    
    protected void refreshChildrenVisuals() {
        if (children != null) {
            for (Object child : children) {
                if (child instanceof UiElementEditPart) {
                    UiElementEditPart childPart = (UiElementEditPart)child;
                    childPart.refreshVisuals();
                }
            }
        }
    }
    
    //-------------------------
    // IUiUpdateListener implementation

    public void uiElementNodeUpdated(UiElementNode ui_node, UiUpdateState state) {
        // TODO: optimize by refreshing only when needed
        switch(state) {
        case ATTR_UPDATED:
            refreshVisuals();
            break;
        case CHILDREN_CHANGED:
            refreshChildren();
            
            // new children list, need to update the layout
            refreshVisuals();
            break;
        case CREATED:
            refreshVisuals();
            break;
        case DELETED:
            // pass
            break;
        }
    }

    //-------------------------
    // Local methods

    /** @return The object model casted to an {@link UiElementNode} */
    protected final UiElementNode getUiNode() {
        return (UiElementNode) getModel();
    }
    
    protected final ElementDescriptor getDescriptor() {
        return getUiNode().getDescriptor();
    }
    
    protected final UiElementEditPart getEditPartParent() {
        EditPart parent = getParent();
        if (parent instanceof UiElementEditPart) {
            return (UiElementEditPart)parent; 
        }
        return null;
    }
    
    /**
     * Returns a given XML attribute.
     * @param attrbName The local name of the attribute.
     * @return the attribute as a {@link String}, if it exists, or <code>null</code>
     */
    protected final String getStringAttr(String attrName) {
        UiElementNode uiNode = getUiNode();
        if (uiNode.getXmlNode() != null) {
            Node xmlNode = uiNode.getXmlNode();
            if (xmlNode != null) {
                NamedNodeMap nodeAttributes = xmlNode.getAttributes();
                if (nodeAttributes != null) {
                    Node attr = nodeAttributes.getNamedItemNS(
                            AndroidConstants.NS_RESOURCES, attrName);
                    if (attr != null) {
                        return attr.getNodeValue();
                    }
                }
            }
        }
        return null;
    }
    
    protected final Rectangle getBounds() {
        UiElementNode model = (UiElementNode)getModel();
        
        Object editData = model.getEditData();
        if (editData instanceof Rectangle) {
            return (Rectangle)editData;  // return a copy?
        }

        // return a dummy rect
        return new Rectangle(0, 0, 0, 0);
    }
}
