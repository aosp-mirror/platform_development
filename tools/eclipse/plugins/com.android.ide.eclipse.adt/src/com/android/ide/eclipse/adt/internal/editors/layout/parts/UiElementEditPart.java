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

import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.uimodel.IUiUpdateListener;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.sdklib.SdkConstants;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.LayoutEditPolicy;
import org.eclipse.gef.editpolicies.SelectionEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.DropRequest;
import org.eclipse.gef.tools.SelectEditPartTracker;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.List;

/**
 * An {@link EditPart} for a {@link UiElementNode}.
 *
 * @since GLE1
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
        /*
         * This is no longer needed, as a selection edit policy is set by the parent layout.
         * Leave this code commented out right now, I'll want to play with this later.
         *
        installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE,
                new NonResizableSelectionEditPolicy(this));
         */
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
    public final UiElementNode getUiNode() {
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
     * @param attrName The local name of the attribute.
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
                            SdkConstants.NS_RESOURCES, attrName);
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

        if (editData != null) {
            // assert with fully qualified class name to prevent import changes to another
            // Rectangle class.
            assert (editData instanceof org.eclipse.draw2d.geometry.Rectangle);

            return (Rectangle)editData;
        }

        // return a dummy rect
        return new Rectangle(0, 0, 0, 0);
    }

    /**
     * Returns the EditPart that should be used as the target for the specified Request.
     * <p/>
     * For instance this is called during drag'n'drop with a CreateRequest.
     * <p/>
     * Reject being a target for elements which descriptor does not allow children.
     *
     * {@inheritDoc}
     */
    @Override
    public EditPart getTargetEditPart(Request request) {
        if (request != null && request.getType() == RequestConstants.REQ_CREATE) {
            // Reject being a target for elements which descriptor does not allow children.
            if (!getUiNode().getDescriptor().hasChildren()) {
                return null;
            }
        }
        return super.getTargetEditPart(request);
    }

    /**
     * Used by derived classes {@link UiDocumentEditPart} and {@link UiLayoutEditPart}
     * to accept drag'n'drop of new items from the palette.
     *
     * @param layoutEditPart The layout edit part where this policy is installed. It can
     *        be either a {@link UiDocumentEditPart} or a {@link UiLayoutEditPart}.
     */
    protected void installLayoutEditPolicy(final UiElementEditPart layoutEditPart) {
        // This policy indicates how elements can be constrained by the layout.
        // TODO Right now we use the XY layout policy since our constraints are
        // handled by the android rendering engine rather than GEF. Tweak as
        // appropriate.
        installEditPolicy(EditPolicy.LAYOUT_ROLE,  new LayoutEditPolicy() {

            /**
             * We don't allow layout children to be resized yet.
             * <p/>
             * Typical choices would be:
             * <ul>
             * <li> ResizableEditPolicy, to allow for selection, move and resize.
             * <li> NonResizableEditPolicy, to allow for selection, move but not resize.
             * <li> SelectionEditPolicy to allow for only selection.
             * </ul>
             * <p/>
             * TODO: make this depend on the part layout. For an AbsoluteLayout we should
             * probably use a NonResizableEditPolicy and SelectionEditPolicy for the rest.
             * Whether to use ResizableEditPolicy or NonResizableEditPolicy should depend
             * on the child in an AbsoluteLayout.
             */
            @Override
            protected EditPolicy createChildEditPolicy(EditPart child) {
                if (child instanceof UiElementEditPart) {
                    return new NonResizableSelectionEditPolicy((UiElementEditPart) child);
                }
                return null;
            }

            @Override
            protected Command getCreateCommand(CreateRequest request) {
                // We store the ElementDescriptor in the request.factory.type
                Object newType = request.getNewObjectType();
                if (newType instanceof ElementDescriptor) {
                    Point where = request.getLocation().getCopy();
                    Point origin = getLayoutContainer().getClientArea().getLocation();
                    where.translate(origin.getNegated());

                    // The host is the EditPart where this policy is installed,
                    // e.g. this UiElementEditPart.
                    EditPart host = getHost();
                    if (host instanceof UiElementEditPart) {

                        return new ElementCreateCommand((ElementDescriptor) newType,
                                (UiElementEditPart) host,
                                where);
                    }
                }

                return null;
            }

            @Override
            protected Command getMoveChildrenCommand(Request request) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void showLayoutTargetFeedback(Request request) {
                super.showLayoutTargetFeedback(request);

                // for debugging
                // System.out.println("target: " + request.toString() + " -- " + layoutEditPart.getUiNode().getBreadcrumbTrailDescription(false));

                if (layoutEditPart instanceof UiLayoutEditPart &&
                        request instanceof DropRequest) {
                    Point where = ((DropRequest) request).getLocation().getCopy();
                    Point origin = getLayoutContainer().getClientArea().getLocation();
                    where.translate(origin.getNegated());

                    ((UiLayoutEditPart) layoutEditPart).showDropTarget(where);
                }
            }

            @Override
            protected void eraseLayoutTargetFeedback(Request request) {
                super.eraseLayoutTargetFeedback(request);
                if (layoutEditPart instanceof UiLayoutEditPart) {
                    ((UiLayoutEditPart) layoutEditPart).hideDropTarget();
                }
            }

            @Override
            protected IFigure createSizeOnDropFeedback(CreateRequest createRequest) {
                // TODO understand if this is useful for us or remove
                return super.createSizeOnDropFeedback(createRequest);
            }

        });
    }

    protected static class NonResizableSelectionEditPolicy extends SelectionEditPolicy {

        private final UiElementEditPart mEditPart;

        public NonResizableSelectionEditPolicy(UiElementEditPart editPart) {
            mEditPart = editPart;
        }

        @Override
        protected void hideSelection() {
            mEditPart.hideSelection();
        }

        @Override
        protected void showSelection() {
            mEditPart.showSelection();
        }
    }
}
