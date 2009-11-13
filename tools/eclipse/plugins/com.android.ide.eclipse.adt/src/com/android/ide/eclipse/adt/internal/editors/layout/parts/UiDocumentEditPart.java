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

import com.android.ide.eclipse.adt.internal.editors.uimodel.UiDocumentNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

import org.eclipse.draw2d.AbstractBackground;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.editpolicies.RootComponentEditPolicy;
import org.eclipse.gef.requests.DropRequest;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Graphical edit part for the root document.
 * <p/>
 * It acts as a simple container.
 *
 * @since GLE1
 */
public class UiDocumentEditPart extends UiElementEditPart {

    private Display mDisplay;
    private FreeformLayer mLayer;
    private ImageBackground mImage;
    private Label mChild = null;

    final static class ImageBackground extends AbstractBackground {

        private BufferedImage mBufferedImage;
        private Image mImage;

        ImageBackground() {
        }

        ImageBackground(BufferedImage image, Display display) {
            setImage(image, display);
        }

        @Override
        public void paintBackground(IFigure figure, Graphics graphics, Insets insets) {
            if (mImage != null) {
                Rectangle rect = getPaintRectangle(figure, insets);
                graphics.drawImage(mImage, rect.x, rect.y);
            }
        }

        void setImage(BufferedImage image, Display display) {
            if (image != null) {
                int[] data = ((DataBufferInt)image.getData().getDataBuffer()).getData();

                ImageData imageData = new ImageData(image.getWidth(), image.getHeight(), 32,
                      new PaletteData(0x00FF0000, 0x0000FF00, 0x000000FF));

                imageData.setPixels(0, 0, data.length, data, 0);

                mImage = new Image(display, imageData);
            } else {
                mImage = null;
            }
        }

        BufferedImage getBufferedImage() {
            return mBufferedImage;
        }
    }

    public UiDocumentEditPart(UiDocumentNode uiDocumentNode, Display display) {
        super(uiDocumentNode);
        mDisplay = display;
    }

    @Override
    protected IFigure createFigure() {
        mLayer = new FreeformLayer();
        mLayer.setLayoutManager(new FreeformLayout());

        mLayer.setOpaque(true);
        mLayer.setBackgroundColor(ColorConstants.lightGray);

        return mLayer;
    }

    @Override
    protected void refreshVisuals() {
        UiElementNode model = (UiElementNode)getModel();

        Object editData = model.getEditData();
        if (editData instanceof BufferedImage) {
            BufferedImage image = (BufferedImage)editData;

            if (mImage == null || image != mImage.getBufferedImage()) {
                mImage = new ImageBackground(image, mDisplay);
            }

            mLayer.setBorder(mImage);

            if (mChild != null && mChild.getParent() == mLayer) {
                mLayer.remove(mChild);
            }
        } else if (editData instanceof String) {
            mLayer.setBorder(null);
            if (mChild == null) {
                mChild = new Label();
            }
            mChild.setText((String)editData);

            if (mChild != null && mChild.getParent() != mLayer) {
                mLayer.add(mChild);
            }
            Rectangle bounds = mChild.getTextBounds();
            bounds.x = bounds.y = 0;
            mLayer.setConstraint(mChild, bounds);
        }

        // refresh the children as well
        refreshChildrenVisuals();
    }

    @Override
    protected void hideSelection() {
        // no selection at this level.
    }

    @Override
    protected void showSelection() {
        // no selection at this level.
    }

    @Override
    protected void createEditPolicies() {
        super.createEditPolicies();

        // This policy indicates this a root component that cannot be removed
        installEditPolicy(EditPolicy.COMPONENT_ROLE, new RootComponentEditPolicy());

        installLayoutEditPolicy(this);
    }

    /**
     * Returns the EditPart that should be used as the target for the specified Request.
     * For instance this is called during drag'n'drop with a CreateRequest.
     * <p/>
     * For the root document, we want the first child edit part to the be the target
     * since an XML document can have only one root element.
     *
     * {@inheritDoc}
     */
    @Override
    public EditPart getTargetEditPart(Request request) {
        if (request != null && request.getType() == RequestConstants.REQ_CREATE) {
            // We refuse the drop if it's not in the bounds of the document.
            if (request instanceof DropRequest) {
                Point where = ((DropRequest) request).getLocation().getCopy();
                UiElementNode uiNode = getUiNode();
                if (uiNode instanceof UiDocumentNode) {
                    // Take the bounds of the background image as the valid drop zone
                    Object editData = uiNode.getEditData();
                    if (editData instanceof BufferedImage) {
                        BufferedImage image = (BufferedImage)editData;
                        int w = image.getWidth();
                        int h = image.getHeight();
                        if (where.x > w || where.y > h) {
                            return null;
                        }
                    }

                }
            }

            // For the root document, we want the first child edit part to the be the target
            // since an XML document can have only one root element.
            if (getChildren().size() > 0) {
                Object o = getChildren().get(0);
                if (o instanceof EditPart) {
                    return ((EditPart) o).getTargetEditPart(request);
                }
            }
        }
        return super.getTargetEditPart(request);
    }
}
