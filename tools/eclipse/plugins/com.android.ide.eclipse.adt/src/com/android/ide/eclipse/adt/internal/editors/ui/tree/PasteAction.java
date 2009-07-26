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

package com.android.ide.eclipse.adt.internal.editors.ui.tree;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.AndroidEditor;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiDocumentNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.xml.core.internal.document.NodeContainer;
import org.w3c.dom.Node;


/**
 * Provides Paste operation for the tree nodes
 */
public class PasteAction extends Action {
    private UiElementNode mUiNode;
    private final AndroidEditor mEditor;
    private final Clipboard mClipboard;

    public PasteAction(AndroidEditor editor, Clipboard clipboard, UiElementNode ui_node) {
        super("Paste");
        mEditor = editor;
        mClipboard = clipboard;

        ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
        setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
        setHoverImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
        setDisabledImageDescriptor(
                images.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE_DISABLED));

        mUiNode = ui_node;
    }

    /**
     * Performs the paste operation.
     */
    @Override
    public void run() {
        super.run();
        
        final String data = (String) mClipboard.getContents(TextTransfer.getInstance());
        if (data != null) {
            IStructuredModel model = mEditor.getModelForEdit();
            try {
                IStructuredDocument sse_doc = mEditor.getStructuredDocument();
                if (sse_doc != null) {
                    if (mUiNode.getDescriptor().hasChildren()) {
                        // This UI Node can have children. The new XML is
                        // inserted as the first child.
                        
                        if (mUiNode.getUiChildren().size() > 0) {
                            // There's already at least one child, so insert right before it.
                            Node xml_node = mUiNode.getUiChildren().get(0).getXmlNode();
                            if (xml_node instanceof IndexedRegion) { // implies xml_node != null
                                IndexedRegion region = (IndexedRegion) xml_node;
                                sse_doc.replace(region.getStartOffset(), 0, data);
                                return; // we're done, no need to try the other cases
                            }                                
                        }
                        
                        // If there's no first XML node child. Create one by
                        // inserting at the end of the *start* tag.
                        Node xml_node = mUiNode.getXmlNode();
                        if (xml_node instanceof NodeContainer) {
                            NodeContainer container = (NodeContainer) xml_node;
                            IStructuredDocumentRegion start_tag =
                                container.getStartStructuredDocumentRegion();
                            if (start_tag != null) {
                                sse_doc.replace(start_tag.getEndOffset(), 0, data);
                                return; // we're done, no need to try the other case
                            }
                        }
                    }
                    
                    // This UI Node doesn't accept children. The new XML is inserted as the
                    // next sibling. This also serves as a fallback if all the previous
                    // attempts failed. However, this is not possible if the current node
                    // has for parent a document -- an XML document can only have one root,
                    // with no siblings.
                    if (!(mUiNode.getUiParent() instanceof UiDocumentNode)) {
                        Node xml_node = mUiNode.getXmlNode();
                        if (xml_node instanceof IndexedRegion) {
                            IndexedRegion region = (IndexedRegion) xml_node;
                            sse_doc.replace(region.getEndOffset(), 0, data);
                        }
                    }
                }

            } catch (BadLocationException e) {
                AdtPlugin.log(e, "ParseAction failed for UI Node %2$s, content '%1$s'", //$NON-NLS-1$
                        mUiNode.getBreadcrumbTrailDescription(true), data);
            } finally {
                model.releaseFromEdit();
            }
        }
    }
}

