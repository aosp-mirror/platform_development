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

package com.android.ide.eclipse.adt.internal.editors;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.ide.eclipse.adt.internal.sdk.Sdk.ITargetChangeListener;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.internal.browser.WorkbenchBrowserSupport;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.WorkbenchPart;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelStateListener;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Document;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Multi-page form editor for Android XML files.
 * <p/>
 * It is designed to work with a {@link StructuredTextEditor} that will display an XML file.
 * <br/>
 * Derived classes must implement createFormPages to create the forms before the
 * source editor. This can be a no-op if desired.
 */
public abstract class AndroidEditor extends FormEditor implements IResourceChangeListener {

    /** Preference name for the current page of this file */
    private static final String PREF_CURRENT_PAGE = "_current_page";

    /** Id string used to create the Android SDK browser */
    private static String BROWSER_ID = "android"; // $NON-NLS-1$

    /** Page id of the XML source editor, used for switching tabs programmatically */
    public final static String TEXT_EDITOR_ID = "editor_part"; //$NON-NLS-1$

    /** Width hint for text fields. Helps the grid layout resize properly on smaller screens */
    public static final int TEXT_WIDTH_HINT = 50;

    /** Page index of the text editor (always the last page) */
    private int mTextPageIndex;
    /** The text editor */
    private StructuredTextEditor mTextEditor;
    /** Listener for the XML model from the StructuredEditor */
    private XmlModelStateListener mXmlModelStateListener;
    /** Listener to update the root node if the target of the file is changed because of a
     * SDK location change or a project target change */
    private ITargetChangeListener mTargetListener;

    /**
     * Creates a form editor.
     */
    public AndroidEditor() {
        super();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);

        mTargetListener = new ITargetChangeListener() {
            public void onProjectTargetChange(IProject changedProject) {
                if (changedProject == getProject()) {
                    onTargetsLoaded();
                }
            }

            public void onTargetsLoaded() {
                commitPages(false /* onSave */);

                // recreate the ui root node always
                initUiRootNode(true /*force*/);
            }
        };
        AdtPlugin.getDefault().addTargetListener(mTargetListener);
    }

    // ---- Abstract Methods ----

    /**
     * Returns the root node of the UI element hierarchy manipulated by the current
     * UI node editor.
     */
    abstract public UiElementNode getUiRootNode();

    /**
     * Creates the various form pages.
     * <p/>
     * Derived classes must implement this to add their own specific tabs.
     */
    abstract protected void createFormPages();

    /**
     * Creates the initial UI Root Node, including the known mandatory elements.
     * @param force if true, a new UiManifestNode is recreated even if it already exists.
     */
    abstract protected void initUiRootNode(boolean force);

    /**
     * Subclasses should override this method to process the new XML Model, which XML
     * root node is given.
     *
     * The base implementation is empty.
     *
     * @param xml_doc The XML document, if available, or null if none exists.
     */
    protected void xmlModelChanged(Document xml_doc) {
        // pass
    }

    // ---- Base Class Overrides, Interfaces Implemented ----

    /**
     * Creates the pages of the multi-page editor.
     */
    @Override
    protected void addPages() {
        createAndroidPages();
        selectDefaultPage(null /* defaultPageId */);
    }

    /**
     * Creates the page for the Android Editors
     */
    protected void createAndroidPages() {
        createFormPages();
        createTextEditor();

        createUndoRedoActions();
    }

    /**
     * Creates undo redo actions for the editor site (so that it works for any page of this
     * multi-page editor) by re-using the actions defined by the {@link StructuredTextEditor}
     * (aka the XML text editor.)
     */
    private void createUndoRedoActions() {
        IActionBars bars = getEditorSite().getActionBars();
        if (bars != null) {
            IAction action = mTextEditor.getAction(ActionFactory.UNDO.getId());
            bars.setGlobalActionHandler(ActionFactory.UNDO.getId(), action);

            action = mTextEditor.getAction(ActionFactory.REDO.getId());
            bars.setGlobalActionHandler(ActionFactory.REDO.getId(), action);

            bars.updateActionBars();
        }
    }

    /**
     * Selects the default active page.
     * @param defaultPageId the id of the page to show. If <code>null</code> the editor attempts to
     * find the default page in the properties of the {@link IResource} object being edited.
     */
    protected void selectDefaultPage(String defaultPageId) {
        if (defaultPageId == null) {
            if (getEditorInput() instanceof IFileEditorInput) {
                IFile file = ((IFileEditorInput) getEditorInput()).getFile();

                QualifiedName qname = new QualifiedName(AdtPlugin.PLUGIN_ID,
                        getClass().getSimpleName() + PREF_CURRENT_PAGE);
                String pageId;
                try {
                    pageId = file.getPersistentProperty(qname);
                    if (pageId != null) {
                        defaultPageId = pageId;
                    }
                } catch (CoreException e) {
                    // ignored
                }
            }
        }

        if (defaultPageId != null) {
            try {
                setActivePage(Integer.parseInt(defaultPageId));
            } catch (Exception e) {
                // We can get NumberFormatException from parseInt but also
                // AssertionError from setActivePage when the index is out of bounds.
                // Generally speaking we just want to ignore any exception and fall back on the
                // first page rather than crash the editor load. Logging the error is enough.
                AdtPlugin.log(e, "Selecting page '%s' in AndroidEditor failed", defaultPageId);
            }
        }
    }

    /**
     * Removes all the pages from the editor.
     */
    protected void removePages() {
        int count = getPageCount();
        for (int i = count - 1 ; i >= 0 ; i--) {
            removePage(i);
        }
    }

    /**
     * Overrides the parent's setActivePage to be able to switch to the xml editor.
     *
     * If the special pageId TEXT_EDITOR_ID is given, switches to the mTextPageIndex page.
     * This is needed because the editor doesn't actually derive from IFormPage and thus
     * doesn't have the get-by-page-id method. In this case, the method returns null since
     * IEditorPart does not implement IFormPage.
     */
    @Override
    public IFormPage setActivePage(String pageId) {
        if (pageId.equals(TEXT_EDITOR_ID)) {
            super.setActivePage(mTextPageIndex);
            return null;
        } else {
            return super.setActivePage(pageId);
        }
    }


    /**
     * Notifies this multi-page editor that the page with the given id has been
     * activated. This method is called when the user selects a different tab.
     *
     * @see MultiPageEditorPart#pageChange(int)
     */
    @Override
    protected void pageChange(int newPageIndex) {
        super.pageChange(newPageIndex);

        if (getEditorInput() instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput) getEditorInput()).getFile();

            QualifiedName qname = new QualifiedName(AdtPlugin.PLUGIN_ID,
                    getClass().getSimpleName() + PREF_CURRENT_PAGE);
            try {
                file.setPersistentProperty(qname, Integer.toString(newPageIndex));
            } catch (CoreException e) {
                // ignore
            }
        }
    }

    /**
     * Notifies this listener that some resource changes
     * are happening, or have already happened.
     *
     * Closes all project files on project close.
     * @see IResourceChangeListener
     */
    public void resourceChanged(final IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    IWorkbenchPage[] pages = getSite().getWorkbenchWindow()
                            .getPages();
                    for (int i = 0; i < pages.length; i++) {
                        if (((FileEditorInput)mTextEditor.getEditorInput())
                                .getFile().getProject().equals(
                                        event.getResource())) {
                            IEditorPart editorPart = pages[i].findEditor(mTextEditor
                                    .getEditorInput());
                            pages[i].closeEditor(editorPart, true);
                        }
                    }
                }
            });
        }
    }

    /**
     * Initializes the editor part with a site and input.
     * <p/>
     * Checks that the input is an instance of {@link IFileEditorInput}.
     *
     * @see FormEditor
     */
    @Override
    public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
        if (!(editorInput instanceof IFileEditorInput))
            throw new PartInitException("Invalid Input: Must be IFileEditorInput");
        super.init(site, editorInput);
    }

    /**
     * Removes attached listeners.
     *
     * @see WorkbenchPart
     */
    @Override
    public void dispose() {
        IStructuredModel xml_model = getModelForRead();
        if (xml_model != null) {
            try {
                if (mXmlModelStateListener != null) {
                    xml_model.removeModelStateListener(mXmlModelStateListener);
                }

            } finally {
                xml_model.releaseFromRead();
            }
        }
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);

        if (mTargetListener != null) {
            AdtPlugin.getDefault().removeTargetListener(mTargetListener);
            mTargetListener = null;
        }

        super.dispose();
    }

    /**
     * Commit all dirty pages then saves the contents of the text editor.
     * <p/>
     * This works by committing all data to the XML model and then
     * asking the Structured XML Editor to save the XML.
     *
     * @see IEditorPart
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
        commitPages(true /* onSave */);

        // The actual "save" operation is done by the Structured XML Editor
        getEditor(mTextPageIndex).doSave(monitor);
    }

    /* (non-Javadoc)
     * Saves the contents of this editor to another object.
     * <p>
     * Subclasses must override this method to implement the open-save-close lifecycle
     * for an editor.  For greater details, see <code>IEditorPart</code>
     * </p>
     *
     * @see IEditorPart
     */
    @Override
    public void doSaveAs() {
        commitPages(true /* onSave */);

        IEditorPart editor = getEditor(mTextPageIndex);
        editor.doSaveAs();
        setPageText(mTextPageIndex, editor.getTitle());
        setInput(editor.getEditorInput());
    }

    /**
     * Commits all dirty pages in the editor. This method should
     * be called as a first step of a 'save' operation.
     * <p/>
     * This is the same implementation as in {@link FormEditor}
     * except it fixes two bugs: a cast to IFormPage is done
     * from page.get(i) <em>before</em> being tested with instanceof.
     * Another bug is that the last page might be a null pointer.
     * <p/>
     * The incorrect casting makes the original implementation crash due
     * to our {@link StructuredTextEditor} not being an {@link IFormPage}
     * so we have to override and duplicate to fix it.
     *
     * @param onSave <code>true</code> if commit is performed as part
     * of the 'save' operation, <code>false</code> otherwise.
     * @since 3.3
     */
    @Override
    public void commitPages(boolean onSave) {
        if (pages != null) {
            for (int i = 0; i < pages.size(); i++) {
                Object page = pages.get(i);
                if (page != null && page instanceof IFormPage) {
                    IFormPage form_page = (IFormPage) page;
                    IManagedForm managed_form = form_page.getManagedForm();
                    if (managed_form != null && managed_form.isDirty()) {
                        managed_form.commit(onSave);
                    }
                }
            }
        }
    }

    /* (non-Javadoc)
     * Returns whether the "save as" operation is supported by this editor.
     * <p>
     * Subclasses must override this method to implement the open-save-close lifecycle
     * for an editor.  For greater details, see <code>IEditorPart</code>
     * </p>
     *
     * @see IEditorPart
     */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    // ---- Local methods ----


    /**
     * Helper method that creates a new hyper-link Listener.
     * Used by derived classes which need active links in {@link FormText}.
     * <p/>
     * This link listener handles two kinds of URLs:
     * <ul>
     * <li> Links starting with "http" are simply sent to a local browser.
     * <li> Links starting with "file:/" are simply sent to a local browser.
     * <li> Links starting with "page:" are expected to be an editor page id to switch to.
     * <li> Other links are ignored.
     * </ul>
     *
     * @return A new hyper-link listener for FormText to use.
     */
    public final IHyperlinkListener createHyperlinkListener() {
        return new HyperlinkAdapter() {
            /**
             * Switch to the page corresponding to the link that has just been clicked.
             * For this purpose, the HREF of the &lt;a&gt; tags above is the page ID to switch to.
             */
            @Override
            public void linkActivated(HyperlinkEvent e) {
                super.linkActivated(e);
                String link = e.data.toString();
                if (link.startsWith("http") ||          //$NON-NLS-1$
                        link.startsWith("file:/")) {    //$NON-NLS-1$
                    openLinkInBrowser(link);
                } else if (link.startsWith("page:")) {  //$NON-NLS-1$
                    // Switch to an internal page
                    setActivePage(link.substring(5 /* strlen("page:") */));
                }
            }
        };
    }

    /**
     * Open the http link into a browser
     *
     * @param link The URL to open in a browser
     */
    private void openLinkInBrowser(String link) {
        try {
            IWorkbenchBrowserSupport wbs = WorkbenchBrowserSupport.getInstance();
            wbs.createBrowser(BROWSER_ID).openURL(new URL(link));
        } catch (PartInitException e1) {
            // pass
        } catch (MalformedURLException e1) {
            // pass
        }
    }

    /**
     * Creates the XML source editor.
     * <p/>
     * Memorizes the index page of the source editor (it's always the last page, but the number
     * of pages before can change.)
     * <br/>
     * Retrieves the underlying XML model from the StructuredEditor and attaches a listener to it.
     * Finally triggers modelChanged() on the model listener -- derived classes can use this
     * to initialize the model the first time.
     * <p/>
     * Called only once <em>after</em> createFormPages.
     */
    private void createTextEditor() {
        try {
            mTextEditor = new StructuredTextEditor();
            int index = addPage(mTextEditor, getEditorInput());
            mTextPageIndex = index;
            setPageText(index, mTextEditor.getTitle());

            if (!(mTextEditor.getTextViewer().getDocument() instanceof IStructuredDocument)) {
                Status status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                        "Error opening the Android XML editor. Is the document an XML file?");
                throw new RuntimeException("Android XML Editor Error", new CoreException(status));
            }

            IStructuredModel xml_model = getModelForRead();
            if (xml_model != null) {
                try {
                    mXmlModelStateListener = new XmlModelStateListener();
                    xml_model.addModelStateListener(mXmlModelStateListener);
                    mXmlModelStateListener.modelChanged(xml_model);
                } catch (Exception e) {
                    AdtPlugin.log(e, "Error while loading editor"); //$NON-NLS-1$
                } finally {
                    xml_model.releaseFromRead();
                }
            }
        } catch (PartInitException e) {
            ErrorDialog.openError(getSite().getShell(),
                    "Android XML Editor Error", null, e.getStatus());
        }
    }

    /**
     * Returns the ISourceViewer associated with the Structured Text editor.
     */
    public final ISourceViewer getStructuredSourceViewer() {
        if (mTextEditor != null) {
            // We can't access mEditor.getSourceViewer() because it is protected,
            // however getTextViewer simply returns the SourceViewer casted, so we
            // can use it instead.
            return mTextEditor.getTextViewer();
        }
        return null;
    }

    /**
     * Returns the {@link IStructuredDocument} used by the StructuredTextEditor (aka Source
     * Editor) or null if not available.
     */
    public final IStructuredDocument getStructuredDocument() {
        if (mTextEditor != null && mTextEditor.getTextViewer() != null) {
            return (IStructuredDocument) mTextEditor.getTextViewer().getDocument();
        }
        return null;
    }

    /**
     * Returns a version of the model that has been shared for read.
     * <p/>
     * Callers <em>must</em> call model.releaseFromRead() when done, typically
     * in a try..finally clause.
     *
     * Portability note: this uses getModelManager which is part of wst.sse.core; however
     * the interface returned is part of wst.sse.core.internal.provisional so we can
     * expect it to change in a distant future if they start cleaning their codebase,
     * however unlikely that is.
     *
     * @return The model for the XML document or null if cannot be obtained from the editor
     */
    public final IStructuredModel getModelForRead() {
        IStructuredDocument document = getStructuredDocument();
        if (document != null) {
            IModelManager mm = StructuredModelManager.getModelManager();
            if (mm != null) {
                return mm.getModelForRead(document);
            }
        }
        return null;
    }

    /**
     * Returns a version of the model that has been shared for edit.
     * <p/>
     * Callers <em>must</em> call model.releaseFromEdit() when done, typically
     * in a try..finally clause.
     *
     * @return The model for the XML document or null if cannot be obtained from the editor
     */
    public final IStructuredModel getModelForEdit() {
        IStructuredDocument document = getStructuredDocument();
        if (document != null) {
            IModelManager mm = StructuredModelManager.getModelManager();
            if (mm != null) {
                return mm.getModelForEdit(document);
            }
        }
        return null;
    }

    /**
     * Helper class to perform edits on the XML model whilst making sure the
     * model has been prepared to be changed.
     * <p/>
     * It first gets a model for edition using {@link #getModelForEdit()},
     * then calls {@link IStructuredModel#aboutToChangeModel()},
     * then performs the requested action
     * and finally calls {@link IStructuredModel#changedModel()}
     * and {@link IStructuredModel#releaseFromEdit()}.
     * <p/>
     * The method is synchronous. As soon as the {@link IStructuredModel#changedModel()} method
     * is called, XML model listeners will be triggered.
     *
     * @param edit_action Something that will change the XML.
     */
    public final void editXmlModel(Runnable edit_action) {
        IStructuredModel model = getModelForEdit();
        try {
            model.aboutToChangeModel();
            edit_action.run();
        } finally {
            // Notify the model we're done modifying it. This must *always* be executed.
            model.changedModel();
            model.releaseFromEdit();
        }
    }

    /**
     * Starts an "undo recording" session. This is managed by the underlying undo manager
     * associated to the structured XML model.
     * <p/>
     * There <em>must</em> be a corresponding call to {@link #endUndoRecording()}.
     * <p/>
     * beginUndoRecording/endUndoRecording calls can be nested (inner calls are ignored, only one
     * undo operation is recorded.)
     *
     * @param label The label for the undo operation. Can be null but we should really try to put
     *              something meaningful if possible.
     * @return True if the undo recording actually started, false if any kind of error occured.
     *         {@link #endUndoRecording()} should only be called if True is returned.
     */
    private final boolean beginUndoRecording(String label) {
        IStructuredDocument document = getStructuredDocument();
        if (document != null) {
            IModelManager mm = StructuredModelManager.getModelManager();
            if (mm != null) {
                IStructuredModel model = mm.getModelForEdit(document);
                if (model != null) {
                    model.beginRecording(this, label);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Ends an "undo recording" session.
     * <p/>
     * This is the counterpart call to {@link #beginUndoRecording(String)} and should only be
     * used if the initial call returned true.
     */
    private final void endUndoRecording() {
        IStructuredDocument document = getStructuredDocument();
        if (document != null) {
            IModelManager mm = StructuredModelManager.getModelManager();
            if (mm != null) {
                IStructuredModel model = mm.getModelForEdit(document);
                if (model != null) {
                    model.endRecording(this);
                }
            }
        }
    }

    /**
     * Creates an "undo recording" session by calling the undoableAction runnable
     * using {@link #beginUndoRecording(String)} and {@link #endUndoRecording()}.
     * <p>
     * You can nest several calls to {@link #wrapUndoRecording(String, Runnable)}, only one
     * recording session will be created.
     *
     * @param label The label for the undo operation. Can be null. Ideally we should really try
     *              to put something meaningful if possible.
     */
    public void wrapUndoRecording(String label, Runnable undoableAction) {
        boolean recording = false;
        try {
            recording = beginUndoRecording(label);
            undoableAction.run();
        } finally {
            if (recording) {
                endUndoRecording();
            }
        }
    }

    /**
     * Returns the XML {@link Document} or null if we can't get it
     */
    protected final Document getXmlDocument(IStructuredModel model) {
        if (model == null) {
            AdtPlugin.log(IStatus.WARNING, "Android Editor: No XML model for root node."); //$NON-NLS-1$
            return null;
        }

        if (model instanceof IDOMModel) {
            IDOMModel dom_model = (IDOMModel) model;
            return dom_model.getDocument();
        }
        return null;
    }

    /**
     * Returns the {@link IProject} for the edited file.
     */
    public IProject getProject() {
        if (mTextEditor != null) {
            IEditorInput input = mTextEditor.getEditorInput();
            if (input instanceof FileEditorInput) {
                FileEditorInput fileInput = (FileEditorInput)input;
                IFile inputFile = fileInput.getFile();

                if (inputFile != null) {
                    return inputFile.getProject();
                }
            }
        }

        return null;
    }

    /**
     * Returns the {@link AndroidTargetData} for the edited file.
     */
    public AndroidTargetData getTargetData() {
        IProject project = getProject();
        if (project != null) {
            Sdk currentSdk = Sdk.getCurrent();
            if (currentSdk != null) {
                IAndroidTarget target = currentSdk.getTarget(project);

                if (target != null) {
                    return currentSdk.getTargetData(target);
                }
            }
        }

        return null;
    }


    /**
     * Listen to changes in the underlying XML model in the structured editor.
     */
    private class XmlModelStateListener implements IModelStateListener {

        /**
         * A model is about to be changed. This typically is initiated by one
         * client of the model, to signal a large change and/or a change to the
         * model's ID or base Location. A typical use might be if a client might
         * want to suspend processing until all changes have been made.
         * <p/>
         * This AndroidEditor implementation of IModelChangedListener is empty.
         */
        public void modelAboutToBeChanged(IStructuredModel model) {
            // pass
        }

        /**
         * Signals that the changes foretold by modelAboutToBeChanged have been
         * made. A typical use might be to refresh, or to resume processing that
         * was suspended as a result of modelAboutToBeChanged.
         * <p/>
         * This AndroidEditor implementation calls the xmlModelChanged callback.
         */
        public void modelChanged(IStructuredModel model) {
            xmlModelChanged(getXmlDocument(model));
        }

        /**
         * Notifies that a model's dirty state has changed, and passes that state
         * in isDirty. A model becomes dirty when any change is made, and becomes
         * not-dirty when the model is saved.
         * <p/>
         * This AndroidEditor implementation of IModelChangedListener is empty.
         */
        public void modelDirtyStateChanged(IStructuredModel model, boolean isDirty) {
            // pass
        }

        /**
         * A modelDeleted means the underlying resource has been deleted. The
         * model itself is not removed from model management until all have
         * released it. Note: baseLocation is not (necessarily) changed in this
         * event, but may not be accurate.
         * <p/>
         * This AndroidEditor implementation of IModelChangedListener is empty.
         */
        public void modelResourceDeleted(IStructuredModel model) {
            // pass
        }

        /**
         * A model has been renamed or copied (as in saveAs..). In the renamed
         * case, the two paramenters are the same instance, and only contain the
         * new info for id and base location.
         * <p/>
         * This AndroidEditor implementation of IModelChangedListener is empty.
         */
        public void modelResourceMoved(IStructuredModel oldModel, IStructuredModel newModel) {
            // pass
        }

        /**
         * This AndroidEditor implementation of IModelChangedListener is empty.
         */
        public void modelAboutToBeReinitialized(IStructuredModel structuredModel) {
            // pass
        }

        /**
         * This AndroidEditor implementation of IModelChangedListener is empty.
         */
        public void modelReinitialized(IStructuredModel structuredModel) {
            // pass
        }
    }
}
