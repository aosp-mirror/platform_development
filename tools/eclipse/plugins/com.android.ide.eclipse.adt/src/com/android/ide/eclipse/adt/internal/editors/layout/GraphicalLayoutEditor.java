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

package com.android.ide.eclipse.adt.internal.editors.layout;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.IconFactory;
import com.android.ide.eclipse.adt.internal.editors.layout.LayoutEditor.UiEditorActions;
import com.android.ide.eclipse.adt.internal.editors.layout.LayoutReloadMonitor.ILayoutReloadListener;
import com.android.ide.eclipse.adt.internal.editors.layout.configuration.ConfigurationComposite;
import com.android.ide.eclipse.adt.internal.editors.layout.configuration.LayoutCreatorDialog;
import com.android.ide.eclipse.adt.internal.editors.layout.configuration.ConfigurationComposite.IConfigListener;
import com.android.ide.eclipse.adt.internal.editors.layout.descriptors.ViewElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.layout.parts.ElementCreateCommand;
import com.android.ide.eclipse.adt.internal.editors.layout.parts.UiElementEditPart;
import com.android.ide.eclipse.adt.internal.editors.layout.parts.UiElementsEditPartFactory;
import com.android.ide.eclipse.adt.internal.editors.ui.tree.CopyCutAction;
import com.android.ide.eclipse.adt.internal.editors.ui.tree.PasteAction;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiDocumentNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.ide.eclipse.adt.internal.resources.manager.ProjectResources;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFile;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFolderType;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceManager;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData;
import com.android.ide.eclipse.adt.internal.sdk.LoadStatus;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData.LayoutBridge;
import com.android.ide.eclipse.adt.internal.sdk.Sdk.ITargetChangeListener;
import com.android.layoutlib.api.ILayoutBridge;
import com.android.layoutlib.api.ILayoutLog;
import com.android.layoutlib.api.ILayoutResult;
import com.android.layoutlib.api.IProjectCallback;
import com.android.layoutlib.api.IResourceValue;
import com.android.layoutlib.api.IXmlPullParser;
import com.android.layoutlib.api.ILayoutResult.ILayoutViewInfo;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.SelectionManager;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.ui.parts.GraphicalEditorWithPalette;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Graphical layout editor, based on GEF.
 * <p/>
 * To understand GEF: http://www.ibm.com/developerworks/opensource/library/os-gef/
 * <p/>
 * To understand Drag'n'drop: http://www.eclipse.org/articles/Article-Workbench-DND/drag_drop.html
 *
 * @since GLE1
 */
public class GraphicalLayoutEditor extends GraphicalEditorWithPalette
        implements IGraphicalLayoutEditor, IConfigListener, ILayoutReloadListener {


    /** Reference to the layout editor */
    private final LayoutEditor mLayoutEditor;

    /** reference to the file being edited. */
    private IFile mEditedFile;

    private Clipboard mClipboard;
    private Composite mParent;
    private ConfigurationComposite mConfigComposite;

    private PaletteRoot mPaletteRoot;

    /** The {@link FolderConfiguration} being edited. */
    private FolderConfiguration mEditedConfig;

    private Map<String, Map<String, IResourceValue>> mConfiguredFrameworkRes;
    private Map<String, Map<String, IResourceValue>> mConfiguredProjectRes;
    private ProjectCallback mProjectCallback;
    private ILayoutLog mLogger;

    private boolean mNeedsXmlReload = false;
    private boolean mNeedsRecompute = false;

    /** Listener to update the root node if the target of the file is changed because of a
     * SDK location change or a project target change */
    private ITargetChangeListener mTargetListener = new ITargetChangeListener() {
        public void onProjectTargetChange(IProject changedProject) {
            if (changedProject == getLayoutEditor().getProject()) {
                onTargetsLoaded();
            }
        }

        public void onTargetsLoaded() {
            // because the SDK changed we must reset the configured framework resource.
            mConfiguredFrameworkRes = null;

            mConfigComposite.updateUIFromResources();

            // updateUiFromFramework will reset language/region combo, so we must call
            // setConfiguration after, or the settext on language/region will be lost.
            if (mEditedConfig != null) {
                setConfiguration(mEditedConfig, false /*force*/);
            }

            // make sure we remove the custom view loader, since its parent class loader is the
            // bridge class loader.
            mProjectCallback = null;

            recomputeLayout();
        }
    };

    private final Runnable mConditionalRecomputeRunnable = new Runnable() {
        public void run() {
            if (mLayoutEditor.isGraphicalEditorActive()) {
                recomputeLayout();
            } else {
                mNeedsRecompute = true;
            }
        }
    };

    private final Runnable mUiUpdateFromResourcesRunnable = new Runnable() {
        public void run() {
            mConfigComposite.updateUIFromResources();
        }
    };


    public GraphicalLayoutEditor(LayoutEditor layoutEditor) {
        mLayoutEditor = layoutEditor;
        setEditDomain(new DefaultEditDomain(this));
        setPartName("Layout");

        AdtPlugin.getDefault().addTargetListener(mTargetListener);
    }

    // ------------------------------------
    // Methods overridden from base classes
    //------------------------------------

    @Override
    public void createPartControl(Composite parent) {
        mParent = parent;
        GridLayout gl;

        mClipboard = new Clipboard(parent.getDisplay());

        parent.setLayout(gl = new GridLayout(1, false));
        gl.marginHeight = gl.marginWidth = 0;

        // create the top part for the configuration control
        mConfigComposite = new ConfigurationComposite(this, parent, SWT.NONE);

        // create a new composite that will contain the standard editor controls.
        Composite editorParent = new Composite(parent, SWT.NONE);
        editorParent.setLayoutData(new GridData(GridData.FILL_BOTH));
        editorParent.setLayout(new FillLayout());
        super.createPartControl(editorParent);
    }

    @Override
    public void dispose() {
        if (mTargetListener != null) {
            AdtPlugin.getDefault().removeTargetListener(mTargetListener);
            mTargetListener = null;
        }

        LayoutReloadMonitor.getMonitor().removeListener(mEditedFile.getProject(), this);

        if (mClipboard != null) {
            mClipboard.dispose();
            mClipboard = null;
        }

        super.dispose();
    }

    /**
     * Returns the selection synchronizer object.
     * The synchronizer can be used to sync the selection of 2 or more EditPartViewers.
     * <p/>
     * This is changed from protected to public so that the outline can use it.
     *
     * @return the synchronizer
     */
    @Override
    public SelectionSynchronizer getSelectionSynchronizer() {
        return super.getSelectionSynchronizer();
    }

    /**
     * Returns the edit domain.
     * <p/>
     * This is changed from protected to public so that the outline can use it.
     *
     * @return the edit domain
     */
    @Override
    public DefaultEditDomain getEditDomain() {
        return super.getEditDomain();
    }

    /* (non-Javadoc)
     * Creates the palette root.
     */
    @Override
    protected PaletteRoot getPaletteRoot() {
        mPaletteRoot = PaletteFactory.createPaletteRoot(mPaletteRoot,
                mLayoutEditor.getTargetData());
        return mPaletteRoot;
    }

    public Clipboard getClipboard() {
        return mClipboard;
    }

    /**
     * Save operation in the Graphical Layout Editor.
     * <p/>
     * In our workflow, the model is owned by the Structured XML Editor.
     * The graphical layout editor just displays it -- thus we don't really
     * save anything here.
     * <p/>
     * This must NOT call the parent editor part. At the contrary, the parent editor
     * part will call this *after* having done the actual save operation.
     * <p/>
     * The only action this editor must do is mark the undo command stack as
     * being no longer dirty.
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
        getCommandStack().markSaveLocation();
        firePropertyChange(PROP_DIRTY);
    }

    @Override
    protected void configurePaletteViewer() {
        super.configurePaletteViewer();

        // Create a drag source listener on an edit part that is a viewer.
        // What this does is use DND with a TemplateTransfer type which is actually
        // the PaletteTemplateEntry held in the PaletteRoot.
        TemplateTransferDragSourceListener dragSource =
            new TemplateTransferDragSourceListener(getPaletteViewer());

        // Create a drag source on the palette viewer.
        // See the drag target associated with the GraphicalViewer in configureGraphicalViewer.
        getPaletteViewer().addDragSourceListener(dragSource);
    }

    /* (non-javadoc)
     * Configure the graphical viewer before it receives its contents.
     */
    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();

        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new UiElementsEditPartFactory(mParent.getDisplay()));
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());

        // Disable the following -- we don't drag *from* the GraphicalViewer yet:
        // viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));

        viewer.addDropTargetListener(new DropListener(viewer));
    }

    class DropListener extends TemplateTransferDropTargetListener {
        public DropListener(EditPartViewer viewer) {
            super(viewer);
        }

        // TODO explain
        @Override
        protected CreationFactory getFactory(final Object template) {
            return new CreationFactory() {
                public Object getNewObject() {
                    // We don't know the newly created EditPart since "creating" new
                    // elements is done by ElementCreateCommand.execute() directly by
                    // manipulating the XML elements..
                    return null;
                }

                public Object getObjectType() {
                    return template;
                }

            };
        }
    }

    /* (non-javadoc)
     * Set the contents of the GraphicalViewer after it has been created.
     */
    @Override
    protected void initializeGraphicalViewer() {
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(getModel());

        IEditorInput input = getEditorInput();
        if (input instanceof FileEditorInput) {
            FileEditorInput fileInput = (FileEditorInput)input;
            mEditedFile = fileInput.getFile();

            mConfigComposite.updateUIFromResources();

            LayoutReloadMonitor.getMonitor().addListener(mEditedFile.getProject(), this);
        } else {
            // really this shouldn't happen! Log it in case it happens
            mEditedFile = null;
            AdtPlugin.log(IStatus.ERROR, "Input is not of type FileEditorInput: %1$s",
                    input.toString());
        }
    }

    /* (non-javadoc)
     * Sets the graphicalViewer for this EditorPart.
     * @param viewer the graphical viewer
     */
    @Override
    protected void setGraphicalViewer(GraphicalViewer viewer) {
        super.setGraphicalViewer(viewer);

        // TODO: viewer.setKeyHandler()
        viewer.setContextMenu(createContextMenu(viewer));
    }

    /**
     * Used by LayoutEditor.UiEditorActions.selectUiNode to select a new UI Node
     * created by {@link ElementCreateCommand#execute()}.
     *
     * @param uiNodeModel The {@link UiElementNode} to select.
     */
    public void selectModel(UiElementNode uiNodeModel) {
        GraphicalViewer viewer = getGraphicalViewer();

        // Give focus to the graphical viewer (in case the outline has it)
        viewer.getControl().forceFocus();

        Object editPart = viewer.getEditPartRegistry().get(uiNodeModel);

        if (editPart instanceof EditPart) {
            viewer.select((EditPart)editPart);
        }
    }


    //--------------
    // Local methods
    //--------------

    public LayoutEditor getLayoutEditor() {
        return mLayoutEditor;
    }

    private MenuManager createContextMenu(GraphicalViewer viewer) {
        MenuManager menuManager = new MenuManager();
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new ActionMenuListener(viewer));

        return menuManager;
    }

    private class ActionMenuListener implements IMenuListener {
        private final GraphicalViewer mViewer;

        public ActionMenuListener(GraphicalViewer viewer) {
            mViewer = viewer;
        }

        /**
         * The menu is about to be shown. The menu manager has already been
         * requested to remove any existing menu item. This method gets the
         * tree selection and if it is of the appropriate type it re-creates
         * the necessary actions.
         */
       public void menuAboutToShow(IMenuManager manager) {
           ArrayList<UiElementNode> selected = new ArrayList<UiElementNode>();

           // filter selected items and only keep those we can handle
           for (Object obj : mViewer.getSelectedEditParts()) {
               if (obj instanceof UiElementEditPart) {
                   UiElementEditPart part = (UiElementEditPart) obj;
                   UiElementNode uiNode = part.getUiNode();
                   if (uiNode != null) {
                       selected.add(uiNode);
                   }
               }
           }

           if (selected.size() > 0) {
               doCreateMenuAction(manager, mViewer, selected);
           }
        }
    }

    private void doCreateMenuAction(IMenuManager manager,
            final GraphicalViewer viewer,
            final ArrayList<UiElementNode> selected) {
        if (selected != null) {
            boolean hasXml = false;
            for (UiElementNode uiNode : selected) {
                if (uiNode.getXmlNode() != null) {
                    hasXml = true;
                    break;
                }
            }

            if (hasXml) {
                manager.add(new CopyCutAction(mLayoutEditor, getClipboard(),
                        null, selected, true /* cut */));
                manager.add(new CopyCutAction(mLayoutEditor, getClipboard(),
                        null, selected, false /* cut */));

                // Can't paste with more than one element selected (the selection is the target)
                if (selected.size() <= 1) {
                    // Paste is not valid if it would add a second element on a terminal element
                    // which parent is a document -- an XML document can only have one child. This
                    // means paste is valid if the current UI node can have children or if the
                    // parent is not a document.
                    UiElementNode ui_root = selected.get(0).getUiRoot();
                    if (ui_root.getDescriptor().hasChildren() ||
                            !(ui_root.getUiParent() instanceof UiDocumentNode)) {
                        manager.add(new PasteAction(mLayoutEditor, getClipboard(),
                                                    selected.get(0)));
                    }
                }
                manager.add(new Separator());
            }
        }

        // Append "add" and "remove" actions. They do the same thing as the add/remove
        // buttons on the side.
        IconFactory factory = IconFactory.getInstance();

        final UiEditorActions uiActions = mLayoutEditor.getUiEditorActions();

        // "Add" makes sense only if there's 0 or 1 item selected since the
        // one selected item becomes the target.
        if (selected == null || selected.size() <= 1) {
            manager.add(new Action("Add...", factory.getImageDescriptor("add")) { //$NON-NLS-2$
                @Override
                public void run() {
                    UiElementNode node = selected != null && selected.size() > 0 ? selected.get(0)
                                                                                 : null;
                    uiActions.doAdd(node, viewer.getControl().getShell());
                }
            });
        }

        if (selected != null) {
            manager.add(new Action("Remove", factory.getImageDescriptor("delete")) { //$NON-NLS-2$
                @Override
                public void run() {
                    uiActions.doRemove(selected, viewer.getControl().getShell());
                }
            });

            manager.add(new Separator());

            manager.add(new Action("Up", factory.getImageDescriptor("up")) { //$NON-NLS-2$
                @Override
                public void run() {
                    uiActions.doUp(selected);
                }
            });
            manager.add(new Action("Down", factory.getImageDescriptor("down")) { //$NON-NLS-2$
                @Override
                public void run() {
                    uiActions.doDown(selected);
                }
            });
        }

    }

    /**
     * Sets the UI for the edition of a new file.
     * @param configuration the configuration of the new file.
     */
    public void editNewFile(FolderConfiguration configuration) {
        // update the configuration UI
        setConfiguration(configuration, true /*force*/);

        // enable the create button if the current and edited config are not equals
        mConfigComposite.setEnabledCreate(
                mEditedConfig.equals(mConfigComposite.getCurrentConfig()) == false);

        reloadConfigurationUi(false /*notifyListener*/);
    }

    public Rectangle getBounds() {
        return mConfigComposite.getScreenBounds();
    }

    /**
     * Renders an Android View described by a {@link ViewElementDescriptor}.
     * <p/>This uses the <code>wrap_content</code> mode for both <code>layout_width</code> and
     * <code>layout_height</code>, and use the class name for the <code>text</code> attribute.
     * @param descriptor the descriptor for the class to render.
     * @return an ImageData containing the rendering or <code>null</code> if rendering failed.
     */
    public ImageData renderWidget(ViewElementDescriptor descriptor) {
        if (mEditedFile == null) {
            return null;
        }

        IAndroidTarget target = Sdk.getCurrent().getTarget(mEditedFile.getProject());
        if (target == null) {
            return null;
        }

        AndroidTargetData data = Sdk.getCurrent().getTargetData(target);
        if (data == null) {
            return null;
        }

        LayoutBridge bridge = data.getLayoutBridge();

        if (bridge.bridge != null) { // bridge can never be null.
            ResourceManager resManager = ResourceManager.getInstance();

            ProjectCallback projectCallback = null;
            Map<String, Map<String, IResourceValue>> configuredProjectResources = null;
            if (mEditedFile != null) {
                ProjectResources projectRes = resManager.getProjectResources(
                        mEditedFile.getProject());
                projectCallback = new ProjectCallback(bridge.classLoader,
                        projectRes, mEditedFile.getProject());

                // get the configured resources for the project
                // get the resources of the file's project.
                if (mConfiguredProjectRes == null && projectRes != null) {
                    // make sure they are loaded
                    projectRes.loadAll();

                    // get the project resource values based on the current config
                    mConfiguredProjectRes = projectRes.getConfiguredResources(
                            mConfigComposite.getCurrentConfig());
                }

                configuredProjectResources = mConfiguredProjectRes;
            } else {
                // we absolutely need a Map of configured project resources.
                configuredProjectResources = new HashMap<String, Map<String, IResourceValue>>();
            }

            // get the framework resources
            Map<String, Map<String, IResourceValue>> frameworkResources =
                    getConfiguredFrameworkResources();

            if (configuredProjectResources != null && frameworkResources != null) {
                // get the selected theme
                String theme = mConfigComposite.getTheme();
                if (theme != null) {
                    // Render a single object as described by the ViewElementDescriptor.
                    WidgetPullParser parser = new WidgetPullParser(descriptor);
                    ILayoutResult result = computeLayout(bridge, parser,
                            null /* projectKey */,
                            1 /* width */, 1 /* height */, true /* renderFullSize */,
                            160 /*density*/, 160.f /*xdpi*/, 160.f /*ydpi*/, theme,
                            mConfigComposite.isProjectTheme(),
                            configuredProjectResources, frameworkResources, projectCallback,
                            null /* logger */);

                    // update the UiElementNode with the layout info.
                    if (result.getSuccess() == ILayoutResult.SUCCESS) {
                        BufferedImage largeImage = result.getImage();

                        // we need to resize it to the actual widget size, and convert it into
                        // an SWT image object.
                        int width = result.getRootView().getRight();
                        int height = result.getRootView().getBottom();
                        Raster raster = largeImage.getData(new java.awt.Rectangle(width, height));
                        int[] imageDataBuffer = ((DataBufferInt)raster.getDataBuffer()).getData();

                        ImageData imageData = new ImageData(width, height, 32,
                                new PaletteData(0x00FF0000, 0x0000FF00, 0x000000FF));

                        imageData.setPixels(0, 0, imageDataBuffer.length, imageDataBuffer, 0);

                        return imageData;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Reloads this editor, by getting the new model from the {@link LayoutEditor}.
     */
    public void reloadEditor() {
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(getModel());

        IEditorInput input = mLayoutEditor.getEditorInput();
        setInput(input);

        if (input instanceof FileEditorInput) {
            FileEditorInput fileInput = (FileEditorInput)input;
            mEditedFile = fileInput.getFile();
        } else {
            // really this shouldn't happen! Log it in case it happens
            mEditedFile = null;
            AdtPlugin.log(IStatus.ERROR, "Input is not of type FileEditorInput: %1$s",
                    input.toString());
        }
    }

    /**
     * Callback for XML model changed. Only update/recompute the layout if the editor is visible
     */
    public void onXmlModelChanged() {
        if (mLayoutEditor.isGraphicalEditorActive()) {
            doXmlReload(true /* force */);
            recomputeLayout();
        } else {
            mNeedsXmlReload = true;
        }
    }

    /**
     * Actually performs the XML reload
     * @see #onXmlModelChanged()
     */
    private void doXmlReload(boolean force) {
        if (force || mNeedsXmlReload) {
            GraphicalViewer viewer = getGraphicalViewer();

            // try to preserve the selection before changing the content
            SelectionManager selMan = viewer.getSelectionManager();
            ISelection selection = selMan.getSelection();

            try {
                viewer.setContents(getModel());
            } finally {
                selMan.setSelection(selection);
            }

            mNeedsXmlReload = false;
        }
    }

    /**
     * Update the UI controls state with a given {@link FolderConfiguration}.
     * <p/>If <var>force</var> is set to <code>true</code> the UI will be changed to exactly reflect
     * <var>config</var>, otherwise, if a qualifier is not present in <var>config</var>,
     * the UI control is not modified. However if the value in the control is not the default value,
     * a warning icon is shown.
     * @param config The {@link FolderConfiguration} to set.
     * @param force Whether the UI should be changed to exactly match the received configuration.
     */
    void setConfiguration(FolderConfiguration config, boolean force) {
        mEditedConfig = config;
        mConfiguredFrameworkRes = mConfiguredProjectRes = null;

        mConfigComposite.setConfiguration(config, force);

    }


    public UiDocumentNode getModel() {
        return mLayoutEditor.getUiRootNode();
    }

    public void reloadPalette() {
        PaletteFactory.createPaletteRoot(mPaletteRoot, mLayoutEditor.getTargetData());
    }

    public void reloadConfigurationUi(boolean notifyListener) {
        // enable the clipping button if it's supported.
        Sdk currentSdk = Sdk.getCurrent();
        if (currentSdk != null) {
            IAndroidTarget target = currentSdk.getTarget(mEditedFile.getProject());
            AndroidTargetData data = currentSdk.getTargetData(target);
            if (data != null) {
                LayoutBridge bridge = data.getLayoutBridge();
                mConfigComposite.reloadDevices(notifyListener);
                mConfigComposite.setClippingSupport(bridge.apiLevel >= 4);
            }
        }
    }

    /**
     * Looks for a file matching the new {@link FolderConfiguration} and attempts to open it.
     * <p/>If there is no match, notify the user.
     */
    public void onConfigurationChange() {
        mConfiguredFrameworkRes = mConfiguredProjectRes = null;

        if (mEditedFile == null || mEditedConfig == null) {
            return;
        }

        // get the resources of the file's project.
        ProjectResources resources = ResourceManager.getInstance().getProjectResources(
                mEditedFile.getProject());

        // from the resources, look for a matching file
        ResourceFile match = null;
        if (resources != null) {
            match = resources.getMatchingFile(mEditedFile.getName(),
                                              ResourceFolderType.LAYOUT,
                                              mConfigComposite.getCurrentConfig());
        }

        if (match != null) {
            if (match.getFile().equals(mEditedFile) == false) {
                try {
                    IDE.openEditor(
                            getSite().getWorkbenchWindow().getActivePage(),
                            match.getFile().getIFile());

                    // we're done!
                    return;
                } catch (PartInitException e) {
                    // FIXME: do something!
                }
            }

            // at this point, we have not opened a new file.

            // update the configuration icons with the new edited config.
            setConfiguration(mEditedConfig, false /*force*/);

            // enable the create button if the current and edited config are not equals
            mConfigComposite.setEnabledCreate(
                    mEditedConfig.equals(mConfigComposite.getCurrentConfig()) == false);

            // Even though the layout doesn't change, the config changed, and referenced
            // resources need to be updated.
            recomputeLayout();
        } else {
            // enable the Create button
            mConfigComposite.setEnabledCreate(true);

            // display the error.
            FolderConfiguration currentConfig = mConfigComposite.getCurrentConfig();
            String message = String.format(
                    "No resources match the configuration\n \n\t%1$s\n \nChange the configuration or create:\n \n\tres/%2$s/%3$s\n \nYou can also click the 'Create' button above.",
                    currentConfig.toDisplayString(),
                    currentConfig.getFolderName(ResourceFolderType.LAYOUT,
                            Sdk.getCurrent().getTarget(mEditedFile.getProject())),
                    mEditedFile.getName());
            showErrorInEditor(message);
        }
    }

    public void onThemeChange() {
        recomputeLayout();
    }

    public void OnClippingChange() {
        recomputeLayout();
    }


    public void onCreate() {
        LayoutCreatorDialog dialog = new LayoutCreatorDialog(mParent.getShell(),
                mEditedFile.getName(),
                Sdk.getCurrent().getTarget(mEditedFile.getProject()),
                mConfigComposite.getCurrentConfig());
        if (dialog.open() == Dialog.OK) {
            final FolderConfiguration config = new FolderConfiguration();
            dialog.getConfiguration(config);

            createAlternateLayout(config);
        }
    }

    /**
     * Recomputes the layout with the help of layoutlib.
     */
    public void recomputeLayout() {
        doXmlReload(false /* force */);
        try {
            // check that the resource exists. If the file is opened but the project is closed
            // or deleted for some reason (changed from outside of eclipse), then this will
            // return false;
            if (mEditedFile.exists() == false) {
                String message = String.format("Resource '%1$s' does not exist.",
                        mEditedFile.getFullPath().toString());

                showErrorInEditor(message);

                return;
            }

            IProject iProject = mEditedFile.getProject();

            if (mEditedFile.isSynchronized(IResource.DEPTH_ZERO) == false) {
                String message = String.format("%1$s is out of sync. Please refresh.",
                        mEditedFile.getName());

                showErrorInEditor(message);

                // also print it in the error console.
                AdtPlugin.printErrorToConsole(iProject.getName(), message);
                return;
            }

            Sdk currentSdk = Sdk.getCurrent();
            if (currentSdk != null) {
                IAndroidTarget target = currentSdk.getTarget(mEditedFile.getProject());
                if (target == null) {
                    showErrorInEditor("The project target is not set.");
                    return;
                }

                AndroidTargetData data = currentSdk.getTargetData(target);
                if (data == null) {
                    // It can happen that the workspace refreshes while the SDK is loading its
                    // data, which could trigger a redraw of the opened layout if some resources
                    // changed while Eclipse is closed.
                    // In this case data could be null, but this is not an error.
                    // We can just silently return, as all the opened editors are automatically
                    // refreshed once the SDK finishes loading.
                    if (AdtPlugin.getDefault().getSdkLoadStatus() != LoadStatus.LOADING) {
                        showErrorInEditor(String.format(
                                "The project target (%s) was not properly loaded.",
                                target.getName()));
                    }
                    return;
                }

                // check there is actually a model (maybe the file is empty).
                UiDocumentNode model = getModel();

                if (model.getUiChildren().size() == 0) {
                    showErrorInEditor("No Xml content. Go to the Outline view and add nodes.");
                    return;
                }

                LayoutBridge bridge = data.getLayoutBridge();

                if (bridge.bridge != null) { // bridge can never be null.
                    ResourceManager resManager = ResourceManager.getInstance();

                    ProjectResources projectRes = resManager.getProjectResources(iProject);
                    if (projectRes == null) {
                        return;
                    }

                    // get the resources of the file's project.
                    Map<String, Map<String, IResourceValue>> configuredProjectRes =
                        getConfiguredProjectResources();

                    // get the framework resources
                    Map<String, Map<String, IResourceValue>> frameworkResources =
                        getConfiguredFrameworkResources();

                    if (configuredProjectRes != null && frameworkResources != null) {
                        if (mProjectCallback == null) {
                            mProjectCallback = new ProjectCallback(
                                    bridge.classLoader, projectRes, iProject);
                        }

                        if (mLogger == null) {
                            mLogger = new ILayoutLog() {
                                public void error(String message) {
                                    AdtPlugin.printErrorToConsole(mEditedFile.getName(), message);
                                }

                                public void error(Throwable error) {
                                    String message = error.getMessage();
                                    if (message == null) {
                                        message = error.getClass().getName();
                                    }

                                    PrintStream ps = new PrintStream(AdtPlugin.getErrorStream());
                                    error.printStackTrace(ps);
                                }

                                public void warning(String message) {
                                    AdtPlugin.printToConsole(mEditedFile.getName(), message);
                                }
                            };
                        }

                        // get the selected theme
                        String theme = mConfigComposite.getTheme();
                        if (theme != null) {

                            // Compute the layout
                            UiElementPullParser parser = new UiElementPullParser(getModel());
                            Rectangle rect = getBounds();
                            boolean isProjectTheme = mConfigComposite.isProjectTheme();

                            int density = mConfigComposite.getDensity().getDpiValue();
                            float xdpi = mConfigComposite.getXDpi();
                            float ydpi = mConfigComposite.getYDpi();

                            ILayoutResult result = computeLayout(bridge, parser,
                                    iProject /* projectKey */,
                                    rect.width, rect.height, !mConfigComposite.getClipping(),
                                    density, xdpi, ydpi,
                                    theme, isProjectTheme,
                                    configuredProjectRes, frameworkResources, mProjectCallback,
                                    mLogger);

                            // update the UiElementNode with the layout info.
                            if (result.getSuccess() == ILayoutResult.SUCCESS) {
                                model.setEditData(result.getImage());

                                updateNodeWithBounds(result.getRootView());
                            } else {
                                String message = result.getErrorMessage();

                                // Reset the edit data for all the nodes.
                                resetNodeBounds(model);

                                if (message != null) {
                                    // set the error in the top element.
                                    model.setEditData(message);
                                }
                            }

                            model.refreshUi();
                        }
                    }
                } else {
                    // SDK is loaded but not the layout library!
                    String message = null;
                    // check whether the bridge managed to load, or not
                    if (bridge.status == LoadStatus.LOADING) {
                        message = String.format(
                                "Eclipse is loading framework information and the Layout library from the SDK folder.\n%1$s will refresh automatically once the process is finished.",
                                mEditedFile.getName());
                    } else {
                        message = String.format("Eclipse failed to load the framework information and the Layout library!");
                    }
                    showErrorInEditor(message);
                }
            } else {
                String message = String.format(
                        "Eclipse is loading the SDK.\n%1$s will refresh automatically once the process is finished.",
                        mEditedFile.getName());

                showErrorInEditor(message);
            }
        } finally {
            // no matter the result, we are done doing the recompute based on the latest
            // resource/code change.
            mNeedsRecompute = false;
        }
    }

    private void showErrorInEditor(String message) {
        // get the model to display the error directly in the editor
        UiDocumentNode model = getModel();

        // Reset the edit data for all the nodes.
        resetNodeBounds(model);

        if (message != null) {
            // set the error in the top element.
            model.setEditData(message);
        }

        model.refreshUi();
    }

    private void resetNodeBounds(UiElementNode node) {
        node.setEditData(null);

        List<UiElementNode> children = node.getUiChildren();
        for (UiElementNode child : children) {
            resetNodeBounds(child);
        }
    }

    private void updateNodeWithBounds(ILayoutViewInfo r) {
        if (r != null) {
            // update the node itself, as the viewKey is the XML node in this implementation.
            Object viewKey = r.getViewKey();
            if (viewKey instanceof UiElementNode) {
                Rectangle bounds = new Rectangle(r.getLeft(), r.getTop(),
                        r.getRight()-r.getLeft(), r.getBottom() - r.getTop());

                ((UiElementNode)viewKey).setEditData(bounds);
            }

            // and then its children.
            ILayoutViewInfo[] children = r.getChildren();
            if (children != null) {
                for (ILayoutViewInfo child : children) {
                    updateNodeWithBounds(child);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see com.android.ide.eclipse.editors.layout.LayoutReloadMonitor.ILayoutReloadListener#reloadLayout(boolean, boolean, boolean)
     *
     * Called when the file changes triggered a redraw of the layout
     */
    public void reloadLayout(boolean codeChange, boolean rChange, boolean resChange) {
        boolean recompute = rChange;

        if (resChange) {
            recompute = true;

            // TODO: differentiate between single and multi resource file changed, and whether the resource change affects the cache.

            // force a reparse in case a value XML file changed.
            mConfiguredProjectRes = null;

            // clear the cache in the bridge in case a bitmap/9-patch changed.
            IAndroidTarget target = Sdk.getCurrent().getTarget(mEditedFile.getProject());
            if (target != null) {

                AndroidTargetData data = Sdk.getCurrent().getTargetData(target);
                if (data != null) {
                    LayoutBridge bridge = data.getLayoutBridge();

                    if (bridge.bridge != null) {
                        bridge.bridge.clearCaches(mEditedFile.getProject());
                    }
                }
            }

            mParent.getDisplay().asyncExec(mUiUpdateFromResourcesRunnable);
        }

        if (codeChange) {
            // only recompute if the custom view loader was used to load some code.
            if (mProjectCallback != null && mProjectCallback.isUsed()) {
                mProjectCallback = null;
                recompute = true;
            }
        }

        if (recompute) {
            mParent.getDisplay().asyncExec(mConditionalRecomputeRunnable);
        }
    }

    /**
     * Responds to a page change that made the Graphical editor page the activated page.
     */
    public void activated() {
        if (mNeedsRecompute || mNeedsXmlReload) {
            recomputeLayout();
        }
    }

    /**
     * Responds to a page change that made the Graphical editor page the deactivated page
     */
    public void deactivated() {
        // nothing to be done here for now.
    }

    public Map<String, Map<String, IResourceValue>> getConfiguredFrameworkResources() {
        if (mConfiguredFrameworkRes == null) {
            ProjectResources frameworkRes = getFrameworkResources();

            if (frameworkRes == null) {
                AdtPlugin.log(IStatus.ERROR, "Failed to get ProjectResource for the framework");
            } else {
                // get the framework resource values based on the current config
                mConfiguredFrameworkRes = frameworkRes.getConfiguredResources(
                        mConfigComposite.getCurrentConfig());
            }
        }

        return mConfiguredFrameworkRes;
    }

    public Map<String, Map<String, IResourceValue>> getConfiguredProjectResources() {
        if (mConfiguredProjectRes == null) {
            ProjectResources project = getProjectResources();

            // make sure they are loaded
            project.loadAll();

            // get the project resource values based on the current config
            mConfiguredProjectRes = project.getConfiguredResources(
                    mConfigComposite.getCurrentConfig());
        }

        return mConfiguredProjectRes;
    }

    /**
     * Returns a {@link ProjectResources} for the framework resources.
     * @return the framework resources or null if not found.
     */
    public ProjectResources getFrameworkResources() {
        if (mEditedFile != null) {
            Sdk currentSdk = Sdk.getCurrent();
            if (currentSdk != null) {
                IAndroidTarget target = currentSdk.getTarget(mEditedFile.getProject());

                if (target != null) {
                    AndroidTargetData data = currentSdk.getTargetData(target);

                    if (data != null) {
                        return data.getFrameworkResources();
                    }
                }
            }
        }

        return null;
    }

    public ProjectResources getProjectResources() {
        if (mEditedFile != null) {
            ResourceManager manager = ResourceManager.getInstance();
            return manager.getProjectResources(mEditedFile.getProject());
        }

        return null;
    }

    /**
     * Creates a new layout file from the specified {@link FolderConfiguration}.
     */
    private void createAlternateLayout(final FolderConfiguration config) {
        new Job("Create Alternate Resource") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                // get the folder name
                String folderName = config.getFolderName(ResourceFolderType.LAYOUT,
                        Sdk.getCurrent().getTarget(mEditedFile.getProject()));
                try {

                    // look to see if it exists.
                    // get the res folder
                    IFolder res = (IFolder)mEditedFile.getParent().getParent();
                    String path = res.getLocation().toOSString();

                    File newLayoutFolder = new File(path + File.separator + folderName);
                    if (newLayoutFolder.isFile()) {
                        // this should not happen since aapt would have complained
                        // before, but if one disable the automatic build, this could
                        // happen.
                        String message = String.format("File 'res/%1$s' is in the way!",
                                folderName);

                        AdtPlugin.displayError("Layout Creation", message);

                        return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, message);
                    } else if (newLayoutFolder.exists() == false) {
                        // create it.
                        newLayoutFolder.mkdir();
                    }

                    // now create the file
                    File newLayoutFile = new File(newLayoutFolder.getAbsolutePath() +
                                File.separator + mEditedFile.getName());

                    newLayoutFile.createNewFile();

                    InputStream input = mEditedFile.getContents();

                    FileOutputStream fos = new FileOutputStream(newLayoutFile);

                    byte[] data = new byte[512];
                    int count;
                    while ((count = input.read(data)) != -1) {
                        fos.write(data, 0, count);
                    }

                    input.close();
                    fos.close();

                    // refreshes the res folder to show up the new
                    // layout folder (if needed) and the file.
                    // We use a progress monitor to catch the end of the refresh
                    // to trigger the edit of the new file.
                    res.refreshLocal(IResource.DEPTH_INFINITE, new IProgressMonitor() {
                        public void done() {
                            mParent.getDisplay().asyncExec(new Runnable() {
                                public void run() {
                                    onConfigurationChange();
                                }
                            });
                        }

                        public void beginTask(String name, int totalWork) {
                            // pass
                        }

                        public void internalWorked(double work) {
                            // pass
                        }

                        public boolean isCanceled() {
                            // pass
                            return false;
                        }

                        public void setCanceled(boolean value) {
                            // pass
                        }

                        public void setTaskName(String name) {
                            // pass
                        }

                        public void subTask(String name) {
                            // pass
                        }

                        public void worked(int work) {
                            // pass
                        }
                    });
                } catch (IOException e2) {
                    String message = String.format(
                            "Failed to create File 'res/%1$s/%2$s' : %3$s",
                            folderName, mEditedFile.getName(), e2.getMessage());

                    AdtPlugin.displayError("Layout Creation", message);

                    return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                            message, e2);
                } catch (CoreException e2) {
                    String message = String.format(
                            "Failed to create File 'res/%1$s/%2$s' : %3$s",
                            folderName, mEditedFile.getName(), e2.getMessage());

                    AdtPlugin.displayError("Layout Creation", message);

                    return e2.getStatus();
                }

                return Status.OK_STATUS;

            }
        }.schedule();
    }

    /**
     * Computes a layout by calling the correct computeLayout method of ILayoutBridge based on
     * the implementation API level.
     */
    @SuppressWarnings("deprecation")
    private static ILayoutResult computeLayout(LayoutBridge bridge,
            IXmlPullParser layoutDescription, Object projectKey,
            int screenWidth, int screenHeight, boolean renderFullSize,
            int density, float xdpi, float ydpi,
            String themeName, boolean isProjectTheme,
            Map<String, Map<String, IResourceValue>> projectResources,
            Map<String, Map<String, IResourceValue>> frameworkResources,
            IProjectCallback projectCallback, ILayoutLog logger) {

        if (bridge.apiLevel >= ILayoutBridge.API_CURRENT) {
            // newest API with support for "render full height"
            // TODO: link boolean to UI.
            return bridge.bridge.computeLayout(layoutDescription,
                    projectKey, screenWidth, screenHeight, renderFullSize,
                    density, xdpi, ydpi,
                    themeName, isProjectTheme,
                    projectResources, frameworkResources, projectCallback,
                    logger);
        } else if (bridge.apiLevel == 3) {
            // newer api with density support.
            return bridge.bridge.computeLayout(layoutDescription,
                    projectKey, screenWidth, screenHeight, density, xdpi, ydpi,
                    themeName, isProjectTheme,
                    projectResources, frameworkResources, projectCallback,
                    logger);
        } else if (bridge.apiLevel == 2) {
            // api with boolean for separation of project/framework theme
            return bridge.bridge.computeLayout(layoutDescription,
                    projectKey, screenWidth, screenHeight, themeName, isProjectTheme,
                    projectResources, frameworkResources, projectCallback,
                    logger);
        } else {
            // oldest api with no density/dpi, and project theme boolean mixed
            // into the theme name.

            // change the string if it's a custom theme to make sure we can
            // differentiate them
            if (isProjectTheme) {
                themeName = "*" + themeName; //$NON-NLS-1$
            }

            return bridge.bridge.computeLayout(layoutDescription,
                    projectKey, screenWidth, screenHeight, themeName,
                    projectResources, frameworkResources, projectCallback,
                    logger);
        }
    }
}
