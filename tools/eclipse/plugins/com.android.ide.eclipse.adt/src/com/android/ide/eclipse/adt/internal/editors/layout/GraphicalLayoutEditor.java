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
import com.android.ide.eclipse.adt.internal.editors.layout.descriptors.ViewElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.layout.parts.ElementCreateCommand;
import com.android.ide.eclipse.adt.internal.editors.layout.parts.UiElementEditPart;
import com.android.ide.eclipse.adt.internal.editors.layout.parts.UiElementsEditPartFactory;
import com.android.ide.eclipse.adt.internal.editors.ui.tree.CopyCutAction;
import com.android.ide.eclipse.adt.internal.editors.ui.tree.PasteAction;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiDocumentNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.resources.ResourceType;
import com.android.ide.eclipse.adt.internal.resources.configurations.CountryCodeQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.ide.eclipse.adt.internal.resources.configurations.KeyboardStateQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.LanguageQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.NavigationMethodQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.NetworkCodeQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.PixelDensityQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.RegionQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenDimensionQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenOrientationQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.TextInputMethodQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.TouchScreenQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.KeyboardStateQualifier.KeyboardState;
import com.android.ide.eclipse.adt.internal.resources.configurations.NavigationMethodQualifier.NavigationMethod;
import com.android.ide.eclipse.adt.internal.resources.configurations.PixelDensityQualifier.Density;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenOrientationQualifier.ScreenOrientation;
import com.android.ide.eclipse.adt.internal.resources.configurations.TextInputMethodQualifier.TextInputMethod;
import com.android.ide.eclipse.adt.internal.resources.configurations.TouchScreenQualifier.TouchScreenType;
import com.android.ide.eclipse.adt.internal.resources.manager.ProjectResources;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFile;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFolderType;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceManager;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData;
import com.android.ide.eclipse.adt.internal.sdk.LoadStatus;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData.LayoutBridge;
import com.android.ide.eclipse.adt.internal.sdk.Sdk.ITargetChangeListener;
import com.android.ide.eclipse.adt.internal.ui.ConfigurationSelector.DimensionVerifier;
import com.android.ide.eclipse.adt.internal.ui.ConfigurationSelector.LanguageRegionVerifier;
import com.android.ide.eclipse.adt.internal.ui.ConfigurationSelector.MobileCodeVerifier;
import com.android.layoutlib.api.ILayoutLog;
import com.android.layoutlib.api.ILayoutResult;
import com.android.layoutlib.api.IProjectCallback;
import com.android.layoutlib.api.IResourceValue;
import com.android.layoutlib.api.IStyleResourceValue;
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
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Graphical layout editor, based on GEF.
 * <p/>
 * To understand GEF: http://www.ibm.com/developerworks/opensource/library/os-gef/
 * <p/>
 * To understand Drag'n'drop: http://www.eclipse.org/articles/Article-Workbench-DND/drag_drop.html
 */
public class GraphicalLayoutEditor extends AbstractGraphicalLayoutEditor
        implements ILayoutReloadListener {

    private final static String THEME_SEPARATOR = "----------"; //$NON-NLS-1$

    /** Reference to the layout editor */
    private final LayoutEditor mLayoutEditor;

    /** reference to the file being edited. */
    private IFile mEditedFile;

    private Clipboard mClipboard;
    private Composite mParent;
    private PaletteRoot mPaletteRoot;

    private Text mCountry;
    private Text mNetwork;
    private Combo mLanguage;
    private Combo mRegion;
    private Combo mOrientation;
    private Combo mDensity;
    private Combo mTouch;
    private Combo mKeyboard;
    private Combo mTextInput;
    private Combo mNavigation;
    private Text mSize1;
    private Text mSize2;
    private Combo mThemeCombo;
    private Button mCreateButton;

    private Label mCountryIcon;
    private Label mNetworkIcon;
    private Label mLanguageIcon;
    private Label mRegionIcon;
    private Label mOrientationIcon;
    private Label mDensityIcon;
    private Label mTouchIcon;
    private Label mKeyboardIcon;
    private Label mTextInputIcon;
    private Label mNavigationIcon;
    private Label mSizeIcon;

    private Label mCurrentLayoutLabel;

    private Image mWarningImage;
    private Image mMatchImage;
    private Image mErrorImage;

    /** The {@link FolderConfiguration} representing the state of the UI controls */
    private FolderConfiguration mCurrentConfig = new FolderConfiguration();
    /** The {@link FolderConfiguration} being edited. */
    private FolderConfiguration mEditedConfig;

    private Map<String, Map<String, IResourceValue>> mConfiguredFrameworkRes;
    private Map<String, Map<String, IResourceValue>> mConfiguredProjectRes;
    private ProjectCallback mProjectCallback;
    private ILayoutLog mLogger;

    private boolean mNeedsXmlReload = false;
    private boolean mNeedsRecompute = false;
    private int mPlatformThemeCount = 0;
    private boolean mDisableUpdates = false;

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

            updateUIFromResources();

            mThemeCombo.getParent().layout();

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
            updateUIFromResources();
            mThemeCombo.getParent().layout();
        }
    };

    public GraphicalLayoutEditor(LayoutEditor layoutEditor) {
        mLayoutEditor = layoutEditor;
        setEditDomain(new DefaultEditDomain(this));
        setPartName("Layout");

        IconFactory factory = IconFactory.getInstance();
        mWarningImage = factory.getIcon("warning"); //$NON-NLS-1$
        mMatchImage = factory.getIcon("match"); //$NON-NLS-1$
        mErrorImage = factory.getIcon("error"); //$NON-NLS-1$

        AdtPlugin.getDefault().addTargetListener(mTargetListener);
    }

    // ------------------------------------
    // Methods overridden from base classes
    //------------------------------------

    @Override
    public void createPartControl(Composite parent) {
        mParent = parent;
        GridLayout gl;
        GridData gd;

        mClipboard = new Clipboard(parent.getDisplay());

        parent.setLayout(gl = new GridLayout(1, false));
        gl.marginHeight = gl.marginWidth = 0;

        // create the top part for the configuration control
        int cols = 10;

        Composite topParent = new Composite(parent, SWT.NONE);
        topParent.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        topParent.setLayout(gl = new GridLayout(cols, false));

        new Label(topParent, SWT.NONE).setText("MCC");
        mCountryIcon = createControlComposite(topParent, true /* grab_horizontal */);
        mCountry = new Text(mCountryIcon.getParent(), SWT.BORDER);
        mCountry.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mCountry.addVerifyListener(new MobileCodeVerifier());
        mCountry.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                onCountryCodeChange();
            }
        });
        mCountry.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onCountryCodeChange();
            }
        });

        new Label(topParent, SWT.NONE).setText("MNC");
        mNetworkIcon = createControlComposite(topParent, true /* grab_horizontal */);
        mNetwork = new Text(mNetworkIcon.getParent(), SWT.BORDER);
        mNetwork.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mNetwork.addVerifyListener(new MobileCodeVerifier());
        mNetwork.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                onNetworkCodeChange();
            }
        });
        mNetwork.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onNetworkCodeChange();
            }
        });

        new Label(topParent, SWT.NONE).setText("Lang");
        mLanguageIcon = createControlComposite(topParent, true /* grab_horizontal */);
        mLanguage = new Combo(mLanguageIcon.getParent(), SWT.DROP_DOWN);
        mLanguage.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mLanguage.addVerifyListener(new LanguageRegionVerifier());
        mLanguage.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                onLanguageChange();
            }
            public void widgetSelected(SelectionEvent e) {
                onLanguageChange();
            }
        });
        mLanguage.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onLanguageChange();
            }
        });

        new Label(topParent, SWT.NONE).setText("Region");
        mRegionIcon = createControlComposite(topParent, true /* grab_horizontal */);
        mRegion = new Combo(mRegionIcon.getParent(), SWT.DROP_DOWN);
        mRegion.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mRegion.addVerifyListener(new LanguageRegionVerifier());
        mRegion.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                onRegionChange();
            }
            public void widgetSelected(SelectionEvent e) {
                onRegionChange();
            }
        });
        mRegion.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onRegionChange();
            }
        });

        new Label(topParent, SWT.NONE).setText("Orient");
        mOrientationIcon = createControlComposite(topParent, true /* grab_horizontal */);
        mOrientation = new Combo(mOrientationIcon.getParent(), SWT.DROP_DOWN | SWT.READ_ONLY);
        ScreenOrientation[] soValues = ScreenOrientation.values();
        mOrientation.add("(Default)");
        for (ScreenOrientation value : soValues) {
            mOrientation.add(value.getDisplayValue());
        }
        mOrientation.select(0);
        mOrientation.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mOrientation.addSelectionListener(new SelectionAdapter() {
           @Override
            public void widgetSelected(SelectionEvent e) {
               onOrientationChange();
            }
        });

        new Label(topParent, SWT.NONE).setText("Density");
        mDensityIcon = createControlComposite(topParent, true /* grab_horizontal */);
        mDensity = new Combo(mDensityIcon.getParent(), SWT.DROP_DOWN | SWT.READ_ONLY);
        Density[] dValues = Density.values();
        mDensity.add("(Default)");
        for (Density value : dValues) {
            mDensity.add(value.getDisplayValue());
        }
        mDensity.select(0);
        mDensity.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mDensity.addSelectionListener(new SelectionAdapter() {
           @Override
            public void widgetSelected(SelectionEvent e) {
               onDensityChange();
            }
        });

        new Label(topParent, SWT.NONE).setText("Touch");
        mTouchIcon = createControlComposite(topParent, true /* grab_horizontal */);
        mTouch = new Combo(mTouchIcon.getParent(), SWT.DROP_DOWN | SWT.READ_ONLY);
        TouchScreenType[] tstValues = TouchScreenType.values();
        mTouch.add("(Default)");
        for (TouchScreenType value : tstValues) {
            mTouch.add(value.getDisplayValue());
        }
        mTouch.select(0);
        mTouch.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mTouch.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onTouchChange();
            }
        });

        new Label(topParent, SWT.NONE).setText("Keybrd");
        mKeyboardIcon = createControlComposite(topParent, true /* grab_horizontal */);
        mKeyboard = new Combo(mKeyboardIcon.getParent(), SWT.DROP_DOWN | SWT.READ_ONLY);
        KeyboardState[] ksValues = KeyboardState.values();
        mKeyboard.add("(Default)");
        for (KeyboardState value : ksValues) {
            mKeyboard.add(value.getDisplayValue());
        }
        mKeyboard.select(0);
        mKeyboard.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mKeyboard.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onKeyboardChange();
            }
        });

        new Label(topParent, SWT.NONE).setText("Input");
        mTextInputIcon = createControlComposite(topParent, true /* grab_horizontal */);
        mTextInput = new Combo(mTextInputIcon.getParent(), SWT.DROP_DOWN | SWT.READ_ONLY);
        TextInputMethod[] timValues = TextInputMethod.values();
        mTextInput.add("(Default)");
        for (TextInputMethod value : timValues) {
            mTextInput.add(value.getDisplayValue());
        }
        mTextInput.select(0);
        mTextInput.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mTextInput.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onTextInputChange();
            }
        });

        new Label(topParent, SWT.NONE).setText("Nav");
        mNavigationIcon = createControlComposite(topParent, true /* grab_horizontal */);
        mNavigation = new Combo(mNavigationIcon.getParent(), SWT.DROP_DOWN | SWT.READ_ONLY);
        NavigationMethod[] nValues = NavigationMethod.values();
        mNavigation.add("(Default)");
        for (NavigationMethod value : nValues) {
            mNavigation.add(value.getDisplayValue());
        }
        mNavigation.select(0);
        mNavigation.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mNavigation.addSelectionListener(new SelectionAdapter() {
            @Override
             public void widgetSelected(SelectionEvent e) {
                onNavigationChange();
            }
        });

        Composite labelParent = new Composite(topParent, SWT.NONE);
        labelParent.setLayout(gl = new GridLayout(8, false));
        gl.marginWidth = gl.marginHeight = 0;
        labelParent.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.horizontalSpan = cols;

        new Label(labelParent, SWT.NONE).setText("Editing config:");
        mCurrentLayoutLabel = new Label(labelParent, SWT.NONE);
        mCurrentLayoutLabel.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.widthHint = 50;

        new Label(labelParent, SWT.NONE).setText("Size");
        mSizeIcon = createControlComposite(labelParent, false);
        Composite sizeParent = new Composite(mSizeIcon.getParent(), SWT.NONE);
        sizeParent.setLayout(gl = new GridLayout(3, false));
        gl.marginWidth = gl.marginHeight = 0;
        gl.horizontalSpacing = 0;

        mSize1 = new Text(sizeParent, SWT.BORDER);
        mSize1.setLayoutData(gd = new GridData());
        gd.widthHint = 30;
        new Label(sizeParent, SWT.NONE).setText("x");
        mSize2 = new Text(sizeParent, SWT.BORDER);
        mSize2.setLayoutData(gd = new GridData());
        gd.widthHint = 30;

        DimensionVerifier verifier = new DimensionVerifier();
        mSize1.addVerifyListener(verifier);
        mSize2.addVerifyListener(verifier);

        SelectionListener sl = new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                onSizeChange();
            }
            public void widgetSelected(SelectionEvent e) {
                onSizeChange();
            }
        };

        mSize1.addSelectionListener(sl);
        mSize2.addSelectionListener(sl);

        ModifyListener sizeModifyListener = new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onSizeChange();
            }
        };

        mSize1.addModifyListener(sizeModifyListener);
        mSize2.addModifyListener(sizeModifyListener);

        // first separator
        Label separator = new Label(labelParent, SWT.SEPARATOR | SWT.VERTICAL);
        separator.setLayoutData(gd = new GridData(
                GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL));
        gd.heightHint = 0;

        mThemeCombo = new Combo(labelParent, SWT.READ_ONLY | SWT.DROP_DOWN);
        mThemeCombo.setEnabled(false);
        updateUIFromResources();
        mThemeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onThemeChange();
            }
        });

        // second separator
        separator = new Label(labelParent, SWT.SEPARATOR | SWT.VERTICAL);
        separator.setLayoutData(gd = new GridData(
                GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL));
        gd.heightHint = 0;

        mCreateButton = new Button(labelParent, SWT.PUSH | SWT.FLAT);
        mCreateButton.setText("Create...");
        mCreateButton.setEnabled(false);
        mCreateButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                LayoutCreatorDialog dialog = new LayoutCreatorDialog(mCreateButton.getShell(),
                        mEditedFile.getName(),
                        Sdk.getCurrent().getTarget(mEditedFile.getProject()), mCurrentConfig);
                if (dialog.open() == Dialog.OK) {
                    final FolderConfiguration config = new FolderConfiguration();
                    dialog.getConfiguration(config);

                    createAlternateLayout(config);
                }
            }
        });

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

    /* (non-Javadoc)
     * Creates the palette root.
     */
    @Override
    protected PaletteRoot getPaletteRoot() {
        mPaletteRoot = PaletteFactory.createPaletteRoot(mPaletteRoot,
                mLayoutEditor.getTargetData());
        return mPaletteRoot;
    }

    @Override
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

            updateUIFromResources();

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
     * created by  {@link ElementCreateCommand#execute()}.
     *
     * @param uiNodeModel The {@link UiElementNode} to select.
     */
    @Override
    void selectModel(UiElementNode uiNodeModel) {
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

    @Override
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
    @Override
    void editNewFile(FolderConfiguration configuration) {
        // update the configuration UI
        setConfiguration(configuration, true /*force*/);

        // enable the create button if the current and edited config are not equals
        mCreateButton.setEnabled(mEditedConfig.equals(mCurrentConfig) == false);
    }

    public Rectangle getBounds() {
        ScreenOrientation orientation = null;
        if (mOrientation.getSelectionIndex() == 0) {
            orientation = ScreenOrientation.PORTRAIT;
        } else {
            orientation = ScreenOrientation.getByIndex(
                    mOrientation.getSelectionIndex() - 1);
        }

        int s1, s2;

        // get the size from the UI controls. If it fails, revert to default values.
        try {
            s1 = Integer.parseInt(mSize1.getText().trim());
        } catch (NumberFormatException e) {
            s1 = 480;
        }

        try {
            s2 = Integer.parseInt(mSize2.getText().trim());
        } catch (NumberFormatException e) {
            s2 = 320;
        }

        // make sure s1 is bigger than s2
        if (s1 < s2) {
            int tmp = s1;
            s1 = s2;
            s2 = tmp;
        }

        switch (orientation) {
            default:
            case PORTRAIT:
                return new Rectangle(0, 0, s2, s1);
            case LANDSCAPE:
                return new Rectangle(0, 0, s1, s2);
            case SQUARE:
                return new Rectangle(0, 0, s1, s1);
        }
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
                    mConfiguredProjectRes = projectRes.getConfiguredResources(mCurrentConfig);
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
                int themeIndex = mThemeCombo.getSelectionIndex();
                if (themeIndex != -1) {
                    String theme = mThemeCombo.getItem(themeIndex);

                    // Render a single object as described by the ViewElementDescriptor.
                    WidgetPullParser parser = new WidgetPullParser(descriptor);
                    ILayoutResult result = computeLayout(bridge, parser,
                            null /* projectKey */,
                            300 /* width */, 300 /* height */, 160 /*density*/,
                            160.f /*xdpi*/, 160.f /*ydpi*/, theme,
                            themeIndex >= mPlatformThemeCount /*isProjectTheme*/,
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
    @Override
    void reloadEditor() {
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
    @Override
    void onXmlModelChanged() {
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
        mDisableUpdates = true; // we do not want to trigger onXXXChange when setting new values in the widgets.

        mEditedConfig = config;
        mConfiguredFrameworkRes = mConfiguredProjectRes = null;

        mCountryIcon.setImage(mMatchImage);
        CountryCodeQualifier countryQualifier = config.getCountryCodeQualifier();
        if (countryQualifier != null) {
            mCountry.setText(String.format("%1$d", countryQualifier.getCode()));
            mCurrentConfig.setCountryCodeQualifier(countryQualifier);
        } else if (force) {
            mCountry.setText(""); //$NON-NLS-1$
            mCurrentConfig.setCountryCodeQualifier(null);
        } else if (mCountry.getText().length() > 0) {
            mCountryIcon.setImage(mWarningImage);
        }

        mNetworkIcon.setImage(mMatchImage);
        NetworkCodeQualifier networkQualifier = config.getNetworkCodeQualifier();
        if (networkQualifier != null) {
            mNetwork.setText(String.format("%1$d", networkQualifier.getCode()));
            mCurrentConfig.setNetworkCodeQualifier(networkQualifier);
        } else if (force) {
            mNetwork.setText(""); //$NON-NLS-1$
            mCurrentConfig.setNetworkCodeQualifier(null);
        } else if (mNetwork.getText().length() > 0) {
            mNetworkIcon.setImage(mWarningImage);
        }

        mLanguageIcon.setImage(mMatchImage);
        LanguageQualifier languageQualifier = config.getLanguageQualifier();
        if (languageQualifier != null) {
            mLanguage.setText(languageQualifier.getValue());
            mCurrentConfig.setLanguageQualifier(languageQualifier);
        } else if (force) {
            mLanguage.setText(""); //$NON-NLS-1$
            mCurrentConfig.setLanguageQualifier(null);
        } else if (mLanguage.getText().length() > 0) {
            mLanguageIcon.setImage(mWarningImage);
        }

        mRegionIcon.setImage(mMatchImage);
        RegionQualifier regionQualifier = config.getRegionQualifier();
        if (regionQualifier != null) {
            mRegion.setText(regionQualifier.getValue());
            mCurrentConfig.setRegionQualifier(regionQualifier);
        } else if (force) {
            mRegion.setText(""); //$NON-NLS-1$
            mCurrentConfig.setRegionQualifier(null);
        } else if (mRegion.getText().length() > 0) {
            mRegionIcon.setImage(mWarningImage);
        }

        mOrientationIcon.setImage(mMatchImage);
        ScreenOrientationQualifier orientationQualifier = config.getScreenOrientationQualifier();
        if (orientationQualifier != null) {
            mOrientation.select(
                    ScreenOrientation.getIndex(orientationQualifier.getValue()) + 1);
            mCurrentConfig.setScreenOrientationQualifier(orientationQualifier);
        } else if (force) {
            mOrientation.select(0);
            mCurrentConfig.setScreenOrientationQualifier(null);
        } else if (mOrientation.getSelectionIndex() != 0) {
            mOrientationIcon.setImage(mWarningImage);
        }

        mDensityIcon.setImage(mMatchImage);
        PixelDensityQualifier densityQualifier = config.getPixelDensityQualifier();
        if (densityQualifier != null) {
            mDensity.select(
                    Density.getIndex(densityQualifier.getValue()) + 1);
            mCurrentConfig.setPixelDensityQualifier(densityQualifier);
        } else if (force) {
            mOrientation.select(0);
            mCurrentConfig.setPixelDensityQualifier(null);
        } else if (mDensity.getSelectionIndex() != 0) {
            mDensityIcon.setImage(mWarningImage);
        }

        mTouchIcon.setImage(mMatchImage);
        TouchScreenQualifier touchQualifier = config.getTouchTypeQualifier();
        if (touchQualifier != null) {
            mTouch.select(TouchScreenType.getIndex(touchQualifier.getValue()) + 1);
            mCurrentConfig.setTouchTypeQualifier(touchQualifier);
        } else if (force) {
            mTouch.select(0);
            mCurrentConfig.setTouchTypeQualifier(null);
        } else if (mTouch.getSelectionIndex() != 0) {
            mTouchIcon.setImage(mWarningImage);
        }

        mKeyboardIcon.setImage(mMatchImage);
        KeyboardStateQualifier keyboardQualifier = config.getKeyboardStateQualifier();
        if (keyboardQualifier != null) {
            mKeyboard.select(KeyboardState.getIndex(keyboardQualifier.getValue()) + 1);
            mCurrentConfig.setKeyboardStateQualifier(keyboardQualifier);
        } else if (force) {
            mKeyboard.select(0);
            mCurrentConfig.setKeyboardStateQualifier(null);
        } else if (mKeyboard.getSelectionIndex() != 0) {
            mKeyboardIcon.setImage(mWarningImage);
        }

        mTextInputIcon.setImage(mMatchImage);
        TextInputMethodQualifier inputQualifier = config.getTextInputMethodQualifier();
        if (inputQualifier != null) {
            mTextInput.select(TextInputMethod.getIndex(inputQualifier.getValue()) + 1);
            mCurrentConfig.setTextInputMethodQualifier(inputQualifier);
        } else if (force) {
            mTextInput.select(0);
            mCurrentConfig.setTextInputMethodQualifier(null);
        } else if (mTextInput.getSelectionIndex() != 0) {
            mTextInputIcon.setImage(mWarningImage);
        }

        mNavigationIcon.setImage(mMatchImage);
        NavigationMethodQualifier navigationQualifiter = config.getNavigationMethodQualifier();
        if (navigationQualifiter != null) {
            mNavigation.select(
                    NavigationMethod.getIndex(navigationQualifiter.getValue()) + 1);
            mCurrentConfig.setNavigationMethodQualifier(navigationQualifiter);
        } else if (force) {
            mNavigation.select(0);
            mCurrentConfig.setNavigationMethodQualifier(null);
        } else if (mNavigation.getSelectionIndex() != 0) {
            mNavigationIcon.setImage(mWarningImage);
        }

        mSizeIcon.setImage(mMatchImage);
        ScreenDimensionQualifier sizeQualifier = config.getScreenDimensionQualifier();
        if (sizeQualifier != null) {
            mSize1.setText(String.format("%1$d", sizeQualifier.getValue1()));
            mSize2.setText(String.format("%1$d", sizeQualifier.getValue2()));
            mCurrentConfig.setScreenDimensionQualifier(sizeQualifier);
        } else if (force) {
            mSize1.setText(""); //$NON-NLS-1$
            mSize2.setText(""); //$NON-NLS-1$
            mCurrentConfig.setScreenDimensionQualifier(null);
        } else if (mSize1.getText().length() > 0 && mSize2.getText().length() > 0) {
            mSizeIcon.setImage(mWarningImage);
        }

        // update the string showing the folder name
        String current = config.toDisplayString();
        mCurrentLayoutLabel.setText(current != null ? current : "(Default)");

        mDisableUpdates = false;
    }

    /**
     * Displays an error icon in front of all the non-null qualifiers.
     */
    void displayConfigError() {
        mCountryIcon.setImage(mMatchImage);
        CountryCodeQualifier countryQualifier = mCurrentConfig.getCountryCodeQualifier();
        if (countryQualifier != null) {
            mCountryIcon.setImage(mErrorImage);
        }

        mNetworkIcon.setImage(mMatchImage);
        NetworkCodeQualifier networkQualifier = mCurrentConfig.getNetworkCodeQualifier();
        if (networkQualifier != null) {
            mNetworkIcon.setImage(mErrorImage);
        }

        mLanguageIcon.setImage(mMatchImage);
        LanguageQualifier languageQualifier = mCurrentConfig.getLanguageQualifier();
        if (languageQualifier != null) {
            mLanguageIcon.setImage(mErrorImage);
        }

        mRegionIcon.setImage(mMatchImage);
        RegionQualifier regionQualifier = mCurrentConfig.getRegionQualifier();
        if (regionQualifier != null) {
            mRegionIcon.setImage(mErrorImage);
        }

        mOrientationIcon.setImage(mMatchImage);
        ScreenOrientationQualifier orientationQualifier =
            mCurrentConfig.getScreenOrientationQualifier();
        if (orientationQualifier != null) {
            mOrientationIcon.setImage(mErrorImage);
        }

        mDensityIcon.setImage(mMatchImage);
        PixelDensityQualifier densityQualifier = mCurrentConfig.getPixelDensityQualifier();
        if (densityQualifier != null) {
            mDensityIcon.setImage(mErrorImage);
        }

        mTouchIcon.setImage(mMatchImage);
        TouchScreenQualifier touchQualifier = mCurrentConfig.getTouchTypeQualifier();
        if (touchQualifier != null) {
            mTouchIcon.setImage(mErrorImage);
        }

        mKeyboardIcon.setImage(mMatchImage);
        KeyboardStateQualifier keyboardQualifier = mCurrentConfig.getKeyboardStateQualifier();
        if (keyboardQualifier != null) {
            mKeyboardIcon.setImage(mErrorImage);
        }

        mTextInputIcon.setImage(mMatchImage);
        TextInputMethodQualifier inputQualifier = mCurrentConfig.getTextInputMethodQualifier();
        if (inputQualifier != null) {
            mTextInputIcon.setImage(mErrorImage);
        }

        mNavigationIcon.setImage(mMatchImage);
        NavigationMethodQualifier navigationQualifiter =
            mCurrentConfig.getNavigationMethodQualifier();
        if (navigationQualifiter != null) {
            mNavigationIcon.setImage(mErrorImage);
        }

        mSizeIcon.setImage(mMatchImage);
        ScreenDimensionQualifier sizeQualifier = mCurrentConfig.getScreenDimensionQualifier();
        if (sizeQualifier != null) {
            mSizeIcon.setImage(mErrorImage);
        }

        // update the string showing the folder name
        String current = mCurrentConfig.toDisplayString();
        mCurrentLayoutLabel.setText(current != null ? current : "(Default)");
    }

    @Override
    UiDocumentNode getModel() {
        return mLayoutEditor.getUiRootNode();
    }

    @Override
    void reloadPalette() {
        PaletteFactory.createPaletteRoot(mPaletteRoot, mLayoutEditor.getTargetData());
    }

    private void onCountryCodeChange() {
        // because mCountry triggers onCountryCodeChange at each modification, calling setText()
        // will trigger notifications, and we don't want that.
        if (mDisableUpdates == true) {
            return;
        }

        // update the current config
        String value = mCountry.getText();

        // empty string, means no qualifier.
        if (value.length() == 0) {
            mCurrentConfig.setCountryCodeQualifier(null);
        } else {
            try {
                CountryCodeQualifier qualifier = CountryCodeQualifier.getQualifier(
                        CountryCodeQualifier.getFolderSegment(Integer.parseInt(value)));
                if (qualifier != null) {
                    mCurrentConfig.setCountryCodeQualifier(qualifier);
                } else {
                    // Failure! Looks like the value is wrong (for instance a one letter string).
                    // We do nothing in this case.
                    mCountryIcon.setImage(mErrorImage);
                    return;
                }
            } catch (NumberFormatException e) {
                // Looks like the code is not a number. This should not happen since the text
                // field has a VerifyListener that prevents it.
                mCurrentConfig.setCountryCodeQualifier(null);
                mCountryIcon.setImage(mErrorImage);
            }
        }

        // look for a file to open/create
        onConfigurationChange();
    }

    private void onNetworkCodeChange() {
        // because mNetwork triggers onNetworkCodeChange at each modification, calling setText()
        // will trigger notifications, and we don't want that.
        if (mDisableUpdates == true) {
            return;
        }

        // update the current config
        String value = mNetwork.getText();

        // empty string, means no qualifier.
        if (value.length() == 0) {
            mCurrentConfig.setNetworkCodeQualifier(null);
        } else {
            try {
                NetworkCodeQualifier qualifier = NetworkCodeQualifier.getQualifier(
                        NetworkCodeQualifier.getFolderSegment(Integer.parseInt(value)));
                if (qualifier != null) {
                    mCurrentConfig.setNetworkCodeQualifier(qualifier);
                } else {
                    // Failure! Looks like the value is wrong (for instance a one letter string).
                    // We do nothing in this case.
                    mNetworkIcon.setImage(mErrorImage);
                    return;
                }
            } catch (NumberFormatException e) {
                // Looks like the code is not a number. This should not happen since the text
                // field has a VerifyListener that prevents it.
                mCurrentConfig.setNetworkCodeQualifier(null);
                mNetworkIcon.setImage(mErrorImage);
            }
        }

        // look for a file to open/create
        onConfigurationChange();
    }

    /**
     * Call back for language combo selection
     */
    private void onLanguageChange() {
        // because mLanguage triggers onLanguageChange at each modification, the filling
        // of the combo with data will trigger notifications, and we don't want that.
        if (mDisableUpdates == true) {
            return;
        }

        // update the current config
        String value = mLanguage.getText();

        updateRegionUi(null /* projectResources */, null /* frameworkResources */);

        // empty string, means no qualifier.
        if (value.length() == 0) {
            mCurrentConfig.setLanguageQualifier(null);
        } else {
            LanguageQualifier qualifier = null;
            String segment = LanguageQualifier.getFolderSegment(value);
            if (segment != null) {
                qualifier = LanguageQualifier.getQualifier(segment);
            }

            if (qualifier != null) {
                mCurrentConfig.setLanguageQualifier(qualifier);
            } else {
                // Failure! Looks like the value is wrong (for instance a one letter string).
                mCurrentConfig.setLanguageQualifier(null);
                mLanguageIcon.setImage(mErrorImage);
            }
        }

        // look for a file to open/create
        onConfigurationChange();
    }

    private void onRegionChange() {
        // because mRegion triggers onRegionChange at each modification, the filling
        // of the combo with data will trigger notifications, and we don't want that.
        if (mDisableUpdates == true) {
            return;
        }

        // update the current config
        String value = mRegion.getText();

        // empty string, means no qualifier.
        if (value.length() == 0) {
            mCurrentConfig.setRegionQualifier(null);
        } else {
            RegionQualifier qualifier = null;
            String segment = RegionQualifier.getFolderSegment(value);
            if (segment != null) {
                qualifier = RegionQualifier.getQualifier(segment);
            }

            if (qualifier != null) {
                mCurrentConfig.setRegionQualifier(qualifier);
            } else {
                // Failure! Looks like the value is wrong (for instance a one letter string).
                mCurrentConfig.setRegionQualifier(null);
                mRegionIcon.setImage(mErrorImage);
            }
        }

        // look for a file to open/create
        onConfigurationChange();
    }

    private void onOrientationChange() {
        // update the current config
        int index = mOrientation.getSelectionIndex();
        if (index != 0) {
            mCurrentConfig.setScreenOrientationQualifier(new ScreenOrientationQualifier(
                ScreenOrientation.getByIndex(index-1)));
        } else {
            mCurrentConfig.setScreenOrientationQualifier(null);
        }

        // look for a file to open/create
        onConfigurationChange();
    }

    private void onDensityChange() {
        int index = mDensity.getSelectionIndex();
        if (index != 0) {
            mCurrentConfig.setPixelDensityQualifier((new PixelDensityQualifier(
                Density.getByIndex(index-1))));
        } else {
            mCurrentConfig.setPixelDensityQualifier(null);
        }

        // look for a file to open/create
        onConfigurationChange();
    }

    private void onTouchChange() {
        // update the current config
        int index = mTouch.getSelectionIndex();
        if (index != 0) {
            mCurrentConfig.setTouchTypeQualifier(new TouchScreenQualifier(
                TouchScreenType.getByIndex(index-1)));
        } else {
            mCurrentConfig.setTouchTypeQualifier(null);
        }

        // look for a file to open/create
        onConfigurationChange();
    }

    private void onKeyboardChange() {
        // update the current config
        int index = mKeyboard.getSelectionIndex();
        if (index != 0) {
            mCurrentConfig.setKeyboardStateQualifier(new KeyboardStateQualifier(
                KeyboardState.getByIndex(index-1)));
        } else {
            mCurrentConfig.setKeyboardStateQualifier(null);
        }

        // look for a file to open/create
        onConfigurationChange();
    }

    private void onTextInputChange() {
        // update the current config
        int index = mTextInput.getSelectionIndex();
        if (index != 0) {
            mCurrentConfig.setTextInputMethodQualifier(new TextInputMethodQualifier(
                TextInputMethod.getByIndex(index-1)));
        } else {
            mCurrentConfig.setTextInputMethodQualifier(null);
        }

        // look for a file to open/create
        onConfigurationChange();
    }

    private void onNavigationChange() {
        // update the current config
        int index = mNavigation.getSelectionIndex();
        if (index != 0) {
            mCurrentConfig.setNavigationMethodQualifier(new NavigationMethodQualifier(
                NavigationMethod.getByIndex(index-1)));
        } else {
            mCurrentConfig.setNavigationMethodQualifier(null);
        }

        // look for a file to open/create
        onConfigurationChange();
    }

    private void onSizeChange() {
        // because mSize1 and mSize2 trigger onSizeChange at each modification, calling setText()
        // will trigger notifications, and we don't want that.
        if (mDisableUpdates == true) {
            return;
        }

        // update the current config
        String size1 = mSize1.getText();
        String size2 = mSize2.getText();

        // if only one of the strings is empty, do nothing
        if ((size1.length() == 0) ^ (size2.length() == 0)) {
            mSizeIcon.setImage(mErrorImage);
            return;
        } else if (size1.length() == 0 && size2.length() == 0) {
            // both sizes are empty: remove the qualifier.
            mCurrentConfig.setScreenDimensionQualifier(null);
        } else {
            ScreenDimensionQualifier qualifier = ScreenDimensionQualifier.getQualifier(size1,
                    size2);

            if (qualifier != null) {
                mCurrentConfig.setScreenDimensionQualifier(qualifier);
            } else {
                // Failure! Looks like the value is wrong.
                // we do nothing in this case.
                return;
            }
        }

        // look for a file to open/create
        onConfigurationChange();
    }


    /**
     * Looks for a file matching the new {@link FolderConfiguration} and attempts to open it.
     * <p/>If there is no match, notify the user.
     */
    private void onConfigurationChange() {
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
                                              mCurrentConfig);
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
            mCreateButton.setEnabled(mEditedConfig.equals(mCurrentConfig) == false);

            // Even though the layout doesn't change, the config changed, and referenced
            // resources need to be updated.
            recomputeLayout();
        } else {
            // update the configuration icons with the new edited config.
            displayConfigError();

            // enable the Create button
            mCreateButton.setEnabled(true);

            // display the error.
            String message = String.format(
                    "No resources match the configuration\n \n\t%1$s\n \nChange the configuration or create:\n \n\tres/%2$s/%3$s\n \nYou can also click the 'Create' button above.",
                    mCurrentConfig.toDisplayString(),
                    mCurrentConfig.getFolderName(ResourceFolderType.LAYOUT,
                            Sdk.getCurrent().getTarget(mEditedFile.getProject())),
                    mEditedFile.getName());
            showErrorInEditor(message);
        }
    }

    private void onThemeChange() {
        int themeIndex = mThemeCombo.getSelectionIndex();
        if (themeIndex != -1) {
            String theme = mThemeCombo.getItem(themeIndex);

            if (theme.equals(THEME_SEPARATOR)) {
                mThemeCombo.select(0);
            }

            recomputeLayout();
        }
    }

    /**
     * Creates a composite with no margin/spacing, and puts a {@link Label} in it with the matching
     * icon.
     * @param parent the parent to receive the composite
     * @return the created {@link Label} object.
     */
    private Label createControlComposite(Composite parent, boolean grab) {
        GridLayout gl;

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(gl = new GridLayout(2, false));
        gl.marginHeight = gl.marginWidth = 0;
        gl.horizontalSpacing = 0;
        if (grab) {
            composite.setLayoutData(
                    new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        }

        // create the label
        Label icon = new Label(composite, SWT.NONE);
        icon.setImage(mMatchImage);

        return icon;
    }

    /**
     * Recomputes the layout with the help of layoutlib.
     */
    @Override
    @SuppressWarnings("deprecation")
    void recomputeLayout() {
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
                    if (mConfiguredProjectRes == null) {
                        // make sure they are loaded
                        projectRes.loadAll();

                        // get the project resource values based on the current config
                        mConfiguredProjectRes = projectRes.getConfiguredResources(mCurrentConfig);
                    }

                    // get the framework resources
                    Map<String, Map<String, IResourceValue>> frameworkResources =
                        getConfiguredFrameworkResources();

                    if (mConfiguredProjectRes != null && frameworkResources != null) {
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
                        int themeIndex = mThemeCombo.getSelectionIndex();
                        if (themeIndex != -1) {
                            String theme = mThemeCombo.getItem(themeIndex);

                            // Compute the layout
                            UiElementPullParser parser = new UiElementPullParser(getModel());
                            Rectangle rect = getBounds();
                            boolean isProjectTheme = themeIndex >= mPlatformThemeCount;

                            // FIXME pass the density/dpi from somewhere (resource config or skin).
                            // For now, get it from the config
                            int density = Density.MEDIUM.getDpiValue();
                            PixelDensityQualifier qual = mCurrentConfig.getPixelDensityQualifier();
                            if (qual != null) {
                                int d = qual.getValue().getDpiValue();
                                if (d > 0) {
                                    density = d;
                                }
                            }

                            ILayoutResult result = computeLayout(bridge, parser,
                                    iProject /* projectKey */,
                                    rect.width, rect.height, density, density, density,
                                    theme, isProjectTheme,
                                    mConfiguredProjectRes, frameworkResources, mProjectCallback,
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
    @Override
    void activated() {
        if (mNeedsRecompute || mNeedsXmlReload) {
            recomputeLayout();
        }
    }

    /**
     * Responds to a page change that made the Graphical editor page the deactivated page
     */
    @Override
    void deactivated() {
        // nothing to be done here for now.
    }

    /**
     * Updates the UI from values in the resources, such as languages, regions, themes, etc...
     * This must be called from the UI thread.
     */
    private void updateUIFromResources() {

        ResourceManager manager = ResourceManager.getInstance();

        ProjectResources frameworkProject = getFrameworkResources();

        mDisableUpdates = true;

        // Reset stuff
        int selection = mThemeCombo.getSelectionIndex();
        mThemeCombo.removeAll();
        mPlatformThemeCount = 0;
        mLanguage.removeAll();

        Set<String> languages = new HashSet<String>();
        ArrayList<String> themes = new ArrayList<String>();

        // get the themes, and languages from the Framework.
        if (frameworkProject != null) {
            // get the configured resources for the framework
            Map<String, Map<String, IResourceValue>> frameworResources =
                getConfiguredFrameworkResources();

            if (frameworResources != null) {
                // get the styles.
                Map<String, IResourceValue> styles = frameworResources.get(
                        ResourceType.STYLE.getName());


                // collect the themes out of all the styles.
                for (IResourceValue value : styles.values()) {
                    String name = value.getName();
                    if (name.startsWith("Theme.") || name.equals("Theme")) {
                        themes.add(value.getName());
                        mPlatformThemeCount++;
                    }
                }

                // sort them and add them to the combo
                Collections.sort(themes);

                for (String theme : themes) {
                    mThemeCombo.add(theme);
                }

                mPlatformThemeCount = themes.size();
                themes.clear();
            }
            // now get the languages from the framework.
            Set<String> frameworkLanguages = frameworkProject.getLanguages();
            if (frameworkLanguages != null) {
                languages.addAll(frameworkLanguages);
            }
        }

        // now get the themes and languages from the project.
        ProjectResources project = null;
        if (mEditedFile != null) {
            project = manager.getProjectResources(mEditedFile.getProject());

            // in cases where the opened file is not linked to a project, this could be null.
            if (project != null) {
                // get the configured resources for the project
                if (mConfiguredProjectRes == null) {
                    // make sure they are loaded
                    project.loadAll();

                    // get the project resource values based on the current config
                    mConfiguredProjectRes = project.getConfiguredResources(mCurrentConfig);
                }

                if (mConfiguredProjectRes != null) {
                    // get the styles.
                    Map<String, IResourceValue> styleMap = mConfiguredProjectRes.get(
                            ResourceType.STYLE.getName());

                    if (styleMap != null) {
                        // collect the themes out of all the styles, ie styles that extend,
                        // directly or indirectly a platform theme.
                        for (IResourceValue value : styleMap.values()) {
                            if (isTheme(value, styleMap)) {
                                themes.add(value.getName());
                            }
                        }

                        // sort them and add them the to the combo.
                        if (mPlatformThemeCount > 0 && themes.size() > 0) {
                            mThemeCombo.add(THEME_SEPARATOR);
                        }

                        Collections.sort(themes);

                        for (String theme : themes) {
                            mThemeCombo.add(theme);
                        }
                    }
                }

                // now get the languages from the project.
                Set<String> projectLanguages = project.getLanguages();
                if (projectLanguages != null) {
                    languages.addAll(projectLanguages);
                }
            }
        }

        // add the languages to the Combo
        for (String language : languages) {
            mLanguage.add(language);
        }

        mDisableUpdates = false;

        // and update the Region UI based on the current language
        updateRegionUi(project, frameworkProject);

        // handle default selection of themes
        if (mThemeCombo.getItemCount() > 0) {
            mThemeCombo.setEnabled(true);
            if (selection == -1) {
                selection = 0;
            }

            if (mThemeCombo.getItemCount() <= selection) {
                mThemeCombo.select(0);
            } else {
                mThemeCombo.select(selection);
            }
        } else {
            mThemeCombo.setEnabled(false);
        }
    }

    /**
     * Returns whether the given <var>style</var> is a theme.
     * This is done by making sure the parent is a theme.
     * @param value the style to check
     * @param styleMap the map of styles for the current project. Key is the style name.
     * @return True if the given <var>style</var> is a theme.
     */
    private boolean isTheme(IResourceValue value, Map<String, IResourceValue> styleMap) {
        if (value instanceof IStyleResourceValue) {
            IStyleResourceValue style = (IStyleResourceValue)value;

            boolean frameworkStyle = false;
            String parentStyle = style.getParentStyle();
            if (parentStyle == null) {
                // if there is no specified parent style we look an implied one.
                // For instance 'Theme.light' is implied child style of 'Theme',
                // and 'Theme.light.fullscreen' is implied child style of 'Theme.light'
                String name = style.getName();
                int index = name.lastIndexOf('.');
                if (index != -1) {
                    parentStyle = name.substring(0, index);
                }
            } else {
                // remove the useless @ if it's there
                if (parentStyle.startsWith("@")) {
                    parentStyle = parentStyle.substring(1);
                }

                // check for framework identifier.
                if (parentStyle.startsWith("android:")) {
                    frameworkStyle = true;
                    parentStyle = parentStyle.substring("android:".length());
                }

                // at this point we could have the format style/<name>. we want only the name
                if (parentStyle.startsWith("style/")) {
                    parentStyle = parentStyle.substring("style/".length());
                }
            }

            if (frameworkStyle) {
                // if the parent is a framework style, it has to be 'Theme' or 'Theme.*'
                return parentStyle.equals("Theme") || parentStyle.startsWith("Theme.");
            } else {
                // if it's a project style, we check this is a theme.
                value = styleMap.get(parentStyle);
                if (value != null) {
                    return isTheme(value, styleMap);
                }
            }
        }

        return false;
    }

    /**
     * Update the Region UI widget based on the current language selection
     * @param projectResources the project resources or {@code null}.
     * @param frameworkResources the framework resource or {@code null}
     */
    private void updateRegionUi(ProjectResources projectResources,
            ProjectResources frameworkResources) {
        if (projectResources == null && mEditedFile != null) {
            projectResources = ResourceManager.getInstance().getProjectResources(
                    mEditedFile.getProject());
        }

        if (frameworkResources == null) {
            frameworkResources = getFrameworkResources();
        }

        String currentLanguage = mLanguage.getText();

        Set<String> set = null;

        if (projectResources != null) {
            set = projectResources.getRegions(currentLanguage);
        }

        if (frameworkResources != null) {
            if (set != null) {
                Set<String> set2 = frameworkResources.getRegions(currentLanguage);
                set.addAll(set2);
            } else {
                set = frameworkResources.getRegions(currentLanguage);
            }
        }

        if (set != null) {
            mDisableUpdates = true;

            mRegion.removeAll();
            for (String region : set) {
                mRegion.add(region);
            }

            mDisableUpdates = false;
        }
    }

    private Map<String, Map<String, IResourceValue>> getConfiguredFrameworkResources() {
        if (mConfiguredFrameworkRes == null) {
            ProjectResources frameworkRes = getFrameworkResources();

            if (frameworkRes == null) {
                AdtPlugin.log(IStatus.ERROR, "Failed to get ProjectResource for the framework");
            }

            // get the framework resource values based on the current config
            mConfiguredFrameworkRes = frameworkRes.getConfiguredResources(mCurrentConfig);
        }

        return mConfiguredFrameworkRes;
    }

    /**
     * Creates a new layout file from the specificed {@link FolderConfiguration}.
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
                            mCurrentConfig.set(config);
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
     * Returns a {@link ProjectResources} for the framework resources.
     * @return the framework resources or null if not found.
     */
    private ProjectResources getFrameworkResources() {
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

    /**
     * Computes a layout by calling the correct computeLayout method of ILayoutBridge based on
     * the implementation API level.
     */
    @SuppressWarnings("deprecation")
    private ILayoutResult computeLayout(LayoutBridge bridge,
            IXmlPullParser layoutDescription, Object projectKey,
            int screenWidth, int screenHeight, int density, float xdpi, float ydpi,
            String themeName, boolean isProjectTheme,
            Map<String, Map<String, IResourceValue>> projectResources,
            Map<String, Map<String, IResourceValue>> frameworkResources,
            IProjectCallback projectCallback, ILayoutLog logger) {

        if (bridge.apiLevel >= 3) {
            // newer api with boolean for separation of project/framework theme,
            // and density support.
            return bridge.bridge.computeLayout(layoutDescription,
                    projectKey, screenWidth, screenHeight, density, xdpi, ydpi,
                    themeName, isProjectTheme,
                    projectResources, frameworkResources, projectCallback,
                    logger);
        } else if (bridge.apiLevel == 2) {
            // api with boolean for separation of project/framework theme
            return bridge.bridge.computeLayout(layoutDescription,
                    projectKey, screenWidth, screenHeight, themeName, isProjectTheme,
                    mConfiguredProjectRes, frameworkResources, mProjectCallback,
                    mLogger);
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
                    mConfiguredProjectRes, frameworkResources, mProjectCallback,
                    mLogger);
        }
    }
}
