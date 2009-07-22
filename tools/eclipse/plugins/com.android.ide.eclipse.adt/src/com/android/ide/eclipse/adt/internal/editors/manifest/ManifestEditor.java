/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ide.eclipse.adt.internal.editors.manifest;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.editors.AndroidEditor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.manifest.descriptors.AndroidManifestDescriptors;
import com.android.ide.eclipse.adt.internal.editors.manifest.pages.ApplicationPage;
import com.android.ide.eclipse.adt.internal.editors.manifest.pages.InstrumentationPage;
import com.android.ide.eclipse.adt.internal.editors.manifest.pages.OverviewPage;
import com.android.ide.eclipse.adt.internal.editors.manifest.pages.PermissionPage;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiAttributeNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceMonitor;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceMonitor.IFileListener;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData;
import com.android.sdklib.xml.AndroidXPathFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

/**
 * Multi-page form editor for AndroidManifest.xml. 
 */
public final class ManifestEditor extends AndroidEditor {

    public static final String ID = AndroidConstants.EDITORS_NAMESPACE + ".manifest.ManifestEditor"; //$NON-NLS-1$
    
    private final static String EMPTY = ""; //$NON-NLS-1$
    
    /** Root node of the UI element hierarchy */
    private UiElementNode mUiManifestNode;
    /** The Application Page tab */
    private ApplicationPage mAppPage;
    /** The Overview Manifest Page tab */
    private OverviewPage mOverviewPage;
    /** The Permission Page tab */
    private PermissionPage mPermissionPage;
    /** The Instrumentation Page tab */
    private InstrumentationPage mInstrumentationPage;
    
    private IFileListener mMarkerMonitor;
    

    /**
     * Creates the form editor for AndroidManifest.xml.
     */
    public ManifestEditor() {
        super();
    }
    
    @Override
    public void dispose() {
        super.dispose();

        ResourceMonitor.getMonitor().removeFileListener(mMarkerMonitor);
    }

    /**
     * Return the root node of the UI element hierarchy, which here
     * is the "manifest" node.
     */
    @Override
    public UiElementNode getUiRootNode() {
        return mUiManifestNode;
    }
    
    /**
     * Returns the Manifest descriptors for the file being edited.
     */
    public AndroidManifestDescriptors getManifestDescriptors() {
        AndroidTargetData data = getTargetData();
        if (data != null) {
            return data.getManifestDescriptors();
        }
        
        return null;
    }
    
    // ---- Base Class Overrides ----

    /**
     * Returns whether the "save as" operation is supported by this editor.
     * <p/>
     * Save-As is a valid operation for the ManifestEditor since it acts on a
     * single source file. 
     *
     * @see IEditorPart
     */
    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    /**
     * Creates the various form pages.
     */
    @Override
    protected void createFormPages() {
        try {
            addPage(mOverviewPage = new OverviewPage(this));
            addPage(mAppPage = new ApplicationPage(this));
            addPage(mPermissionPage = new PermissionPage(this));
            addPage(mInstrumentationPage = new InstrumentationPage(this));
        } catch (PartInitException e) {
            AdtPlugin.log(e, "Error creating nested page"); //$NON-NLS-1$
        }
    }

    /* (non-java doc)
     * Change the tab/title name to include the project name.
     */
    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
        IFile inputFile = getInputFile();
        if (inputFile != null) {
            startMonitoringMarkers();
            setPartName(String.format("%1$s Manifest", inputFile.getProject().getName()));
        }
    }

    /**
     * Processes the new XML Model, which XML root node is given.
     * 
     * @param xml_doc The XML document, if available, or null if none exists.
     */
    @Override
    protected void xmlModelChanged(Document xml_doc) {
        // create the ui root node on demand.
        initUiRootNode(false /*force*/);

        loadFromXml(xml_doc);

        super.xmlModelChanged(xml_doc);
    }
    
    private void loadFromXml(Document xmlDoc) {
        mUiManifestNode.setXmlDocument(xmlDoc);
        if (xmlDoc != null) {
            ElementDescriptor manifest_desc = mUiManifestNode.getDescriptor();
            try {
                XPath xpath = AndroidXPathFactory.newXPath();
                Node node = (Node) xpath.evaluate("/" + manifest_desc.getXmlName(),  //$NON-NLS-1$
                        xmlDoc,
                        XPathConstants.NODE);
                assert node != null && node.getNodeName().equals(manifest_desc.getXmlName());

                // Refresh the manifest UI node and all its descendants 
                mUiManifestNode.loadFromXmlNode(node);
            } catch (XPathExpressionException e) {
                AdtPlugin.log(e, "XPath error when trying to find '%s' element in XML.", //$NON-NLS-1$
                        manifest_desc.getXmlName());
            }
        }
    }

    private void onDescriptorsChanged(UiElementNode oldManifestNode) {
        mUiManifestNode.reloadFromXmlNode(oldManifestNode.getXmlNode());

        if (mOverviewPage != null) {
            mOverviewPage.refreshUiApplicationNode();
        }

        if (mAppPage != null) {
            mAppPage.refreshUiApplicationNode();
        }
        
        if (mPermissionPage != null) {
            mPermissionPage.refreshUiNode();
        }
        
        if (mInstrumentationPage != null) {
            mInstrumentationPage.refreshUiNode();
        }
    }

    /**
     * Reads and processes the current markers and adds a listener for marker changes. 
     */
    private void startMonitoringMarkers() {
        final IFile inputFile = getInputFile();
        if (inputFile != null) {
            updateFromExistingMarkers(inputFile);
            
            mMarkerMonitor = new IFileListener() {
                public void fileChanged(IFile file, IMarkerDelta[] markerDeltas, int kind) {
                    if (file.equals(inputFile)) {
                        processMarkerChanges(markerDeltas);
                    }
                }
            };
            
            ResourceMonitor.getMonitor().addFileListener(mMarkerMonitor, IResourceDelta.CHANGED);
        }
    }

    /**
     * Processes the markers of the specified {@link IFile} and updates the error status of 
     * {@link UiElementNode}s and {@link UiAttributeNode}s.
     * @param inputFile the file being edited.
     */
    private void updateFromExistingMarkers(IFile inputFile) {
        try {
            // get the markers for the file
            IMarker[] markers = inputFile.findMarkers(AndroidConstants.MARKER_ANDROID, true,
                    IResource.DEPTH_ZERO);
            
            AndroidManifestDescriptors desc = getManifestDescriptors();
            if (desc != null) {
                ElementDescriptor appElement = desc.getApplicationElement();
                
                if (appElement != null) {
                    UiElementNode app_ui_node = mUiManifestNode.findUiChildNode(
                            appElement.getXmlName());
                    List<UiElementNode> children = app_ui_node.getUiChildren();

                    for (IMarker marker : markers) {
                        processMarker(marker, children, IResourceDelta.ADDED);
                    }
                }
            }
            
        } catch (CoreException e) {
            // findMarkers can throw an exception, in which case, we'll do nothing.
        }
    }
    
    /**
     * Processes a {@link IMarker} change.
     * @param markerDeltas the list of {@link IMarkerDelta}
     */
    private void processMarkerChanges(IMarkerDelta[] markerDeltas) {
        AndroidManifestDescriptors descriptors = getManifestDescriptors();
        if (descriptors != null && descriptors.getApplicationElement() != null) {
            UiElementNode app_ui_node = mUiManifestNode.findUiChildNode(
                    descriptors.getApplicationElement().getXmlName());
            List<UiElementNode> children = app_ui_node.getUiChildren();
    
            for (IMarkerDelta markerDelta : markerDeltas) {
                processMarker(markerDelta.getMarker(), children, markerDelta.getKind());
            }
        }
    }

    /**
     * Processes a new/old/updated marker.
     * @param marker The marker being added/removed/changed
     * @param nodeList the list of activity/service/provider/receiver nodes.
     * @param kind the change kind. Can be {@link IResourceDelta#ADDED},
     * {@link IResourceDelta#REMOVED}, or {@link IResourceDelta#CHANGED}
     */
    private void processMarker(IMarker marker, List<UiElementNode> nodeList, int kind) {
        // get the data from the marker
        String nodeType = marker.getAttribute(AndroidConstants.MARKER_ATTR_TYPE, EMPTY);
        if (nodeType == EMPTY) {
            return;
        }
        
        String className = marker.getAttribute(AndroidConstants.MARKER_ATTR_CLASS, EMPTY);
        if (className == EMPTY) {
            return;
        }

        for (UiElementNode ui_node : nodeList) {
            if (ui_node.getDescriptor().getXmlName().equals(nodeType)) {
                for (UiAttributeNode attr : ui_node.getUiAttributes()) {
                    if (attr.getDescriptor().getXmlLocalName().equals(
                            AndroidManifestDescriptors.ANDROID_NAME_ATTR)) {
                        if (attr.getCurrentValue().equals(className)) {
                            if (kind == IResourceDelta.REMOVED) {
                                attr.setHasError(false);
                            } else {
                                attr.setHasError(true);
                            }
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates the initial UI Root Node, including the known mandatory elements.
     * @param force if true, a new UiManifestNode is recreated even if it already exists.
     */
    @Override
    protected void initUiRootNode(boolean force) {
        // The manifest UI node is always created, even if there's no corresponding XML node.
        if (mUiManifestNode != null && force == false) {
            return;
        }
        
        AndroidManifestDescriptors manifestDescriptor = getManifestDescriptors();
        
        if (manifestDescriptor != null) {
            // save the old manifest node if it exists
            UiElementNode oldManifestNode = mUiManifestNode;

            ElementDescriptor manifestElement = manifestDescriptor.getManifestElement();   
            mUiManifestNode = manifestElement.createUiNode();
            mUiManifestNode.setEditor(this);
    
            // Similarly, always create the /manifest/application and /manifest/uses-sdk nodes
            ElementDescriptor appElement = manifestDescriptor.getApplicationElement();
            boolean present = false;
            for (UiElementNode ui_node : mUiManifestNode.getUiChildren()) {
                if (ui_node.getDescriptor() == appElement) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                mUiManifestNode.appendNewUiChild(appElement);
            }

            appElement = manifestDescriptor.getUsesSdkElement();
            present = false;
            for (UiElementNode ui_node : mUiManifestNode.getUiChildren()) {
                if (ui_node.getDescriptor() == appElement) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                mUiManifestNode.appendNewUiChild(appElement);
            }

            if (oldManifestNode != null) {
                onDescriptorsChanged(oldManifestNode);
            }
        } else {
            // create a dummy descriptor/uinode until we have real descriptors
            ElementDescriptor desc = new ElementDescriptor("manifest", //$NON-NLS-1$
                    "temporary descriptors due to missing decriptors", //$NON-NLS-1$
                    null /*tooltip*/, null /*sdk_url*/, null /*attributes*/,
                    null /*children*/, false /*mandatory*/);
            mUiManifestNode = desc.createUiNode();
            mUiManifestNode.setEditor(this);
        }
    }
    
    /**
     * Returns the {@link IFile} being edited, or <code>null</code> if it couldn't be computed.
     */
    private IFile getInputFile() {
        IEditorInput input = getEditorInput();
        if (input instanceof FileEditorInput) {
            FileEditorInput fileInput = (FileEditorInput) input;
            return fileInput.getFile();
        }
        
        return null;
    }
}
