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

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.editors.AndroidEditor;
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

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;


/**
 * Provides Cut and Copy actions for the tree nodes.
 */
public class CopyCutAction extends Action {
    private List<UiElementNode> mUiNodes;
    private boolean mPerformCut;
    private final AndroidEditor mEditor;
    private final Clipboard mClipboard;
    private final ICommitXml mXmlCommit;

    /**
     * Creates a new Copy or Cut action.
     * 
     * @param selected The UI node to cut or copy. It *must* have a non-null XML node.
     * @param perform_cut True if the operation is cut, false if it is copy.
     */
    public CopyCutAction(AndroidEditor editor, Clipboard clipboard, ICommitXml xmlCommit,
            UiElementNode selected, boolean perform_cut) {
        this(editor, clipboard, xmlCommit, toList(selected), perform_cut);
    }

    /**
     * Creates a new Copy or Cut action.
     * 
     * @param selected The UI nodes to cut or copy. They *must* have a non-null XML node.
     *                 The list becomes owned by the {@link CopyCutAction}.
     * @param perform_cut True if the operation is cut, false if it is copy.
     */
    public CopyCutAction(AndroidEditor editor, Clipboard clipboard, ICommitXml xmlCommit,
            List<UiElementNode> selected, boolean perform_cut) {
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

        mUiNodes = selected;
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
        if (mUiNodes == null || mUiNodes.size() < 1) {
            return;
        }

        // Commit the current pages first, to make sure the XML is in sync.
        // Committing may change the XML structure.
        if (mXmlCommit != null) {
            mXmlCommit.commitPendingXmlChanges();
        }

        StringBuilder allText = new StringBuilder();
        ArrayList<UiElementNode> nodesToCut = mPerformCut ? new ArrayList<UiElementNode>() : null;

        for (UiElementNode uiNode : mUiNodes) {
            try {            
                Node xml_node = uiNode.getXmlNode();
                if (xml_node == null) {
                    return;
                }
                
                String data = getXmlTextFromEditor(xml_node);
 
                // In the unlikely event that IStructuredDocument failed to extract the text
                // directly from the editor, try to fall back on a direct XML serialization
                // of the XML node. This uses the generic Node interface with no SSE tricks.
                if (data == null) {
                    data = getXmlTextFromSerialization(xml_node);
                }
                
                if (data != null) {
                    allText.append(data);
                    if (mPerformCut) {
                        // only remove notes to cut if we actually got some XML text from them
                        nodesToCut.add(uiNode);
                    }
                }
    
            } catch (Exception e) {
                AdtPlugin.log(e, "CopyCutAction failed for UI node %1$s", //$NON-NLS-1$
                        uiNode.getBreadcrumbTrailDescription(true));
            }
        } // for uiNode

        if (allText != null && allText.length() > 0) {
            mClipboard.setContents(
                    new Object[] { allText.toString() },
                    new Transfer[] { TextTransfer.getInstance() });
            if (mPerformCut) {
                for (UiElementNode uiNode : nodesToCut) {
                    uiNode.deleteXmlNode();
                }
            }
        }
    }

    /** Get the data directly from the editor. */
    private String getXmlTextFromEditor(Node xml_node) {
        String data = null;
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
        return data;
    }
    
    /**
     * Direct XML serialization of the XML node.
     * <p/>
     * This uses the generic Node interface with no SSE tricks. It's however slower
     * and doesn't respect formatting (since serialization is involved instead of reading
     * the actual text buffer.)
     */
    private String getXmlTextFromSerialization(Node xml_node) throws IOException {
        String data;
        StringWriter sw = new StringWriter();
        XMLSerializer serializer = new XMLSerializer(sw,
                new OutputFormat(Method.XML,
                        OutputFormat.Defaults.Encoding /* utf-8 */,
                        true /* indent */));
        // Serialize will throw an IOException if it fails.
        serializer.serialize((Element) xml_node);
        data = sw.toString();
        return data;
    }

    /**
     * Static helper class to wrap on node into a list for the constructors.
     */
    private static ArrayList<UiElementNode> toList(UiElementNode selected) {
        ArrayList<UiElementNode> list = null;
        if (selected != null) {
            list = new ArrayList<UiElementNode>(1);
            list.add(selected);
        }
        return list;
    }
}

