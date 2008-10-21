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

package com.android.ide.eclipse.editors.ui.tree;

import com.android.ide.eclipse.editors.AndroidEditor;
import com.android.ide.eclipse.editors.EditorsPlugin;
import com.android.ide.eclipse.editors.uimodel.UiElementNode;

import org.apache.xml.serialize.Method;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.document.NodeContainer;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.StringWriter;


/**
 * Provides Cut and Copy actions for the tree nodes.
 */
public class CopyCutAction extends Action {
    private UiElementNode mUiNode;
    private boolean mPerformCut;
    private final AndroidEditor mEditor;
    private final Clipboard mClipboard;
    private final ICommitXml mXmlCommit;

    /**
     * Creates a new Copy or Cut action.
     * 
     * @param ui_node The UI node to cut or copy. It *must* have a non-null XML node.
     * @param perform_cut True if the operation is cut, false if it is copy.
     */
    public CopyCutAction(AndroidEditor editor, Clipboard clipboard, ICommitXml xmlCommit,
            UiElementNode ui_node, boolean perform_cut) {
        super(perform_cut ? "Cut" : "Copy");
        mEditor = editor;
        mClipboard = clipboard;
        mXmlCommit = xmlCommit;
        
        ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
        if (perform_cut) {
            setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_CUT));
            setHoverImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_CUT));
            setDisabledImageDescriptor(
                    images.getImageDescriptor(ISharedImages.IMG_TOOL_CUT_DISABLED));
        } else {
            setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
            setHoverImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
            setDisabledImageDescriptor(
                    images.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
        }

        mUiNode = ui_node;
        mPerformCut = perform_cut;
    }

    /**
     * Performs the cut or copy action.
     * First an XML serializer is used to turn the existing XML node into a valid
     * XML fragment, which is added as text to the clipboard.
     */
    @Override
    public void run() {
        super.run();
        try {
            String data = null;
            
            // Get the data directly from the editor.
            
            // Commit the current pages first, to make sure the XML is in sync.
            if (mXmlCommit != null) {
                mXmlCommit.commitPendingXmlChanges();
            }

            // Committing may change the XML structure.
            Node xml_node = mUiNode.getXmlNode();
            if (xml_node == null) {
                return;
            }

            IStructuredModel model = mEditor.getModelForRead();
            try {
                IStructuredDocument sse_doc = mEditor.getStructuredDocument();
                if (xml_node instanceof NodeContainer) {
                    // The easy way to get the source of an SSE XML node.
                    data = ((NodeContainer) xml_node).getSource();
                } else  if (xml_node instanceof IndexedRegion && sse_doc != null) {
                    // Try harder.
                    IndexedRegion region = (IndexedRegion) xml_node;
                    int start = region.getStartOffset();
                    int end = region.getEndOffset();

                    if (end > start) {
                        data = sse_doc.get(start, end - start);
                    }
                }
            } catch (BadLocationException e) {
                // the region offset was invalid. ignore.
                } finally {
                    model.releaseFromRead();
                }
 
                // In the unlikely event that IStructuredDocument failed to extract the text
            // directly from the editor, try to fall back on a direct XML serialization
            // of the XML node. This uses the generic Node interface with no SSE tricks.
            if (data == null) {
                StringWriter sw = new StringWriter();
                XMLSerializer serializer = new XMLSerializer(sw,
                        new OutputFormat(Method.XML,
                                OutputFormat.Defaults.Encoding /* utf-8 */,
                                true /* indent */));
                // Serialize will throw an IOException if it fails.
                serializer.serialize((Element) xml_node);
                data = sw.toString();
            }

            if (data != null && data.length() > 0) {
                mClipboard.setContents(
                        new Object[] { data },
                        new Transfer[] { TextTransfer.getInstance() });
                if (mPerformCut) {
                    mUiNode.deleteXmlNode();
                }
            }
        } catch (Exception e) {
            EditorsPlugin.log(e, "CopyCutAction failed for UI node %1$s", //$NON-NLS-1$
                    mUiNode.getBreadcrumbTrailDescription(true));
        }
    }
}

