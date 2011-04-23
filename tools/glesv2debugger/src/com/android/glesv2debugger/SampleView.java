/*
 ** Copyright 2011, The Android Open Source Project
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 **     http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package com.android.glesv2debugger;

import com.android.glesv2debugger.DebuggerMessage.Message;
import com.android.glesv2debugger.DebuggerMessage.Message.Function;
import com.android.glesv2debugger.DebuggerMessage.Message.Prop;
import com.android.glesv2debugger.DebuggerMessage.Message.Type;
import com.android.sdklib.util.SparseArray;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.ByteOrder;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class SampleView extends ViewPart implements Runnable, SelectionListener {
    public static final ByteOrder targetByteOrder = ByteOrder.LITTLE_ENDIAN;

    boolean running = false;
    Thread thread;
    MessageQueue messageQueue;
    SparseArray<DebugContext> debugContexts = new SparseArray<DebugContext>();

    /** The ID of the view as specified by the extension. */
    public static final String ID = "glesv2debuggerclient.views.SampleView";

    TabFolder tabFolder;
    TabItem tabItemText, tabItemImage, tabItemBreakpointOption;
    TabItem tabItemShaderEditor, tabContextViewer;
    ListViewer viewer; // ListViewer / TableViewer
    Slider frameNum; // scale max cannot overlap min, so max is array size
    TreeViewer contextViewer;
    BreakpointOption breakpointOption;
    ShaderEditor shaderEditor;
    Canvas canvas;
    Text text;
    Action actionConnect; // connect / disconnect

    Action actionAutoScroll;
    Action actionFilter;
    Action actionPort;

    Action actContext; // for toggling contexts
    DebugContext current = null;

    Point origin = new Point(0, 0); // for smooth scrolling canvas
    String[] filters = null;

    class ViewContentProvider extends LabelProvider implements IStructuredContentProvider,
            ITableLabelProvider {
        Frame frame = null;

        @Override
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
            frame = (Frame) newInput;
        }

        @Override
        public void dispose() {
        }

        @Override
        public Object[] getElements(Object parent) {
            return frame.get().toArray();
        }

        @Override
        public String getText(Object obj) {
            MessageData msgData = (MessageData) obj;
            return msgData.text;
        }

        @Override
        public Image getImage(Object obj) {
            MessageData msgData = (MessageData) obj;
            return msgData.getImage();
        }

        @Override
        public String getColumnText(Object obj, int index) {
            MessageData msgData = (MessageData) obj;
            if (index >= msgData.columns.length)
                return null;
            return msgData.columns[index];
        }

        @Override
        public Image getColumnImage(Object obj, int index) {
            if (index > -1)
                return null;
            MessageData msgData = (MessageData) obj;
            return msgData.getImage();
        }
    }

    class NameSorter extends ViewerSorter {
        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            MessageData m1 = (MessageData) e1;
            MessageData m2 = (MessageData) e2;
            return (int) ((m1.msg.getTime() - m2.msg.getTime()) * 100);
        }
    }

    class Filter extends ViewerFilter {
        @Override
        public boolean select(Viewer viewer, Object parentElement,
                Object element) {
            MessageData msgData = (MessageData) element;
            if (null == filters)
                return true;
            for (int i = 0; i < filters.length; i++)
                if (msgData.text.contains(filters[i]))
                    return true;
            return false;
        }
    }

    public SampleView() {

    }

    public void createLeftPane(Composite parent) {
        Composite composite = new Composite(parent, 0);

        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        composite.setLayout(gridLayout);

        frameNum = new Slider(composite, SWT.BORDER | SWT.HORIZONTAL);
        frameNum.setMinimum(0);
        frameNum.setMaximum(1);
        frameNum.setSelection(0);
        frameNum.addSelectionListener(this);

        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.verticalAlignment = SWT.FILL;
        frameNum.setLayoutData(gridData);

        // Table table = new Table(composite, SWT.H_SCROLL | SWT.V_SCROLL |
        // SWT.MULTI
        // | SWT.FULL_SELECTION);
        // TableLayout layout = new TableLayout();
        // table.setLayout(layout);
        // table.setLinesVisible(true);
        // table.setHeaderVisible(true);
        // String[] headings = {
        // "Name", "Elapsed (ms)", "Detail"
        // };
        // int[] weights = {
        // 50, 16, 60
        // };
        // int[] widths = {
        // 180, 90, 200
        // };
        // for (int i = 0; i < headings.length; i++) {
        // layout.addColumnData(new ColumnWeightData(weights[i], widths[i],
        // true));
        // TableColumn nameCol = new TableColumn(table, SWT.NONE, i);
        // nameCol.setText(headings[i]);
        // }

        // viewer = new TableViewer(table);
        viewer = new ListViewer(composite, SWT.DEFAULT);
        viewer.getList().setFont(new Font(viewer.getList().getDisplay(),
                "Courier", 10, SWT.BOLD));
        ViewContentProvider contentProvider = new ViewContentProvider();
        viewer.setContentProvider(contentProvider);
        viewer.setLabelProvider(contentProvider);
        // viewer.setSorter(new NameSorter());
        viewer.setFilters(new ViewerFilter[] {
                new Filter()
        });

        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.verticalAlignment = SWT.FILL;
        gridData.grabExcessVerticalSpace = true;
        viewer.getControl().setLayoutData(gridData);
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    @Override
    public void createPartControl(Composite parent) {
        createLeftPane(parent);

        // Create the help context id for the viewer's control
        PlatformUI.getWorkbench().getHelpSystem()
                .setHelp(viewer.getControl(), "GLESv2DebuggerClient.viewer");

        tabFolder = new TabFolder(parent, SWT.BORDER);

        text = new Text(tabFolder, SWT.NO_BACKGROUND | SWT.READ_ONLY
                | SWT.V_SCROLL | SWT.H_SCROLL);

        tabItemText = new TabItem(tabFolder, SWT.NONE);
        tabItemText.setText("Text");
        tabItemText.setControl(text);

        canvas = new Canvas(tabFolder, SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE
                | SWT.V_SCROLL | SWT.H_SCROLL);
        tabItemImage = new TabItem(tabFolder, SWT.NONE);
        tabItemImage.setText("Image");
        tabItemImage.setControl(canvas);

        breakpointOption = new BreakpointOption(this, tabFolder);
        tabItemBreakpointOption = new TabItem(tabFolder, SWT.NONE);
        tabItemBreakpointOption.setText("Breakpoint Option");
        tabItemBreakpointOption.setControl(breakpointOption);

        shaderEditor = new ShaderEditor(this, tabFolder);
        tabItemShaderEditor = new TabItem(tabFolder, SWT.NONE);
        tabItemShaderEditor.setText("Shader Editor");
        tabItemShaderEditor.setControl(shaderEditor);

        contextViewer = new TreeViewer(tabFolder);
        ContextViewProvider contextViewProvider = new ContextViewProvider(this);
        contextViewer.addSelectionChangedListener(contextViewProvider);
        contextViewer.setContentProvider(contextViewProvider);
        contextViewer.setLabelProvider(contextViewProvider);
        tabContextViewer = new TabItem(tabFolder, SWT.NONE);
        tabContextViewer.setText("Context Viewer");
        tabContextViewer.setControl(contextViewer.getTree());

        final ScrollBar hBar = canvas.getHorizontalBar();
        hBar.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if (null == canvas.getBackgroundImage())
                    return;
                Image image = canvas.getBackgroundImage();
                int hSelection = hBar.getSelection();
                int destX = -hSelection - origin.x;
                Rectangle rect = image.getBounds();
                canvas.scroll(destX, 0, 0, 0, rect.width, rect.height, false);
                origin.x = -hSelection;
            }
        });
        final ScrollBar vBar = canvas.getVerticalBar();
        vBar.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if (null == canvas.getBackgroundImage())
                    return;
                Image image = canvas.getBackgroundImage();
                int vSelection = vBar.getSelection();
                int destY = -vSelection - origin.y;
                Rectangle rect = image.getBounds();
                canvas.scroll(0, destY, 0, 0, rect.width, rect.height, false);
                origin.y = -vSelection;
            }
        });
        canvas.addListener(SWT.Resize, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if (null == canvas.getBackgroundImage())
                    return;
                Image image = canvas.getBackgroundImage();
                Rectangle rect = image.getBounds();
                Rectangle client = canvas.getClientArea();
                hBar.setMaximum(rect.width);
                vBar.setMaximum(rect.height);
                hBar.setThumb(Math.min(rect.width, client.width));
                vBar.setThumb(Math.min(rect.height, client.height));
                int hPage = rect.width - client.width;
                int vPage = rect.height - client.height;
                int hSelection = hBar.getSelection();
                int vSelection = vBar.getSelection();
                if (hSelection >= hPage) {
                    if (hPage <= 0)
                        hSelection = 0;
                    origin.x = -hSelection;
                }
                if (vSelection >= vPage) {
                    if (vPage <= 0)
                        vSelection = 0;
                    origin.y = -vSelection;
                }
                canvas.redraw();
            }
        });
        canvas.addListener(SWT.Paint, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if (null == canvas.getBackgroundImage())
                    return;
                Image image = canvas.getBackgroundImage();
                GC gc = e.gc;
                gc.drawImage(image, origin.x, origin.y);
                Rectangle rect = image.getBounds();
                Rectangle client = canvas.getClientArea();
                int marginWidth = client.width - rect.width;
                if (marginWidth > 0) {
                    gc.fillRectangle(rect.width, 0, marginWidth, client.height);
                }
                int marginHeight = client.height - rect.height;
                if (marginHeight > 0) {
                    gc.fillRectangle(0, rect.height, client.width, marginHeight);
                }
            }
        });

        hookContextMenu();
        hookSelectionChanged();
        contributeToActionBars();

        messageQueue = new MessageQueue(this, new ProcessMessage[] {
                breakpointOption, shaderEditor
        });
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                SampleView.this.fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(IMenuManager manager) {
        // manager.add(actionConnect);
        // manager.add(new Separator());
        // manager.add(actionDisconnect);
    }

    private void fillContextMenu(IMenuManager manager) {
        // manager.add(actionConnect);
        // manager.add(actionDisconnect);
        // Other plug-ins can contribute there actions here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private void fillLocalToolBar(final IToolBarManager manager) {
        actionConnect = new Action("Connect", Action.AS_PUSH_BUTTON) {
            @Override
            public void run() {
                if (!running)
                    changeContext(null); // viewer will switch to newest context
                connectDisconnect();
            }
        };
        manager.add(actionConnect);

        manager.add(new Action("Open File", Action.AS_PUSH_BUTTON)
        {
            @Override
            public void run()
            {
                if (!running)
                {
                    changeContext(null); // viewer will switch to newest context
                    openFile();
                }
            }
        });

        final Shell shell = this.getViewSite().getShell();
        actionAutoScroll = new Action("Auto Scroll", Action.AS_CHECK_BOX) {
            @Override
            public void run() {
            }
        };
        actionAutoScroll.setChecked(true);
        manager.add(actionAutoScroll);

        actionFilter = new Action("*", Action.AS_DROP_DOWN_MENU) {
            @Override
            public void run() {
                org.eclipse.jface.dialogs.InputDialog dialog = new org.eclipse.jface.dialogs.InputDialog(
                        shell, "Contains Filter",
                        "case sensitive substring or *",
                        actionFilter.getText(), null);
                if (Window.OK == dialog.open()) {
                    actionFilter.setText(dialog.getValue());
                    manager.update(true);
                    filters = dialog.getValue().split("\\|");
                    if (filters.length == 1 && filters[0].equals("*"))
                        filters = null;
                    viewer.refresh();
                }

            }
        };
        manager.add(actionFilter);

        manager.add(new Action("CaptureDraw", Action.AS_DROP_DOWN_MENU)
        {
            @Override
            public void run()
            {
                int contextId = 0;
                if (current != null)
                    contextId = current.contextId;
                InputDialog inputDialog = new InputDialog(shell,
                        "Capture glDrawArrays/Elements",
                        "Enter number of glDrawArrays/Elements to glReadPixels for "
                                + "context 0x" + Integer.toHexString(contextId) +
                                "\n(0x0 is any context)", "9001", null);
                if (inputDialog.open() != Window.OK)
                    return;
                Message.Builder builder = Message.newBuilder();
                builder.setContextId(contextId);
                builder.setType(Type.Response);
                builder.setExpectResponse(false);
                builder.setFunction(Function.SETPROP);
                builder.setProp(Prop.CaptureDraw);
                builder.setArg0(Integer.parseInt(inputDialog.getValue()));
                messageQueue.addCommand(builder.build());
            }
        });

        manager.add(new Action("CaptureSwap", Action.AS_DROP_DOWN_MENU)
        {
            @Override
            public void run()
            {
                int contextId = 0;
                if (current != null)
                    contextId = current.contextId;
                InputDialog inputDialog = new InputDialog(shell,
                        "Capture eglSwapBuffers",
                        "Enter number of eglSwapBuffers to glReadPixels for "
                                + "context 0x" + Integer.toHexString(contextId) +
                                "\n(0x0 is any context)", "9001", null);
                if (inputDialog.open() != Window.OK)
                    return;
                Message.Builder builder = Message.newBuilder();
                builder.setContextId(contextId);
                builder.setType(Type.Response);
                builder.setExpectResponse(false);
                builder.setFunction(Function.SETPROP);
                builder.setProp(Prop.CaptureSwap);
                builder.setArg0(Integer.parseInt(inputDialog.getValue()));
                messageQueue.addCommand(builder.build());
            }
        });

        manager.add(new Action("SYSTEM_TIME_THREAD", Action.AS_DROP_DOWN_MENU)
        {
            @Override
            public void run()
            {
                final String[] timeModes = {
                        "SYSTEM_TIME_REALTIME", "SYSTEM_TIME_MONOTONIC", "SYSTEM_TIME_PROCESS",
                        "SYSTEM_TIME_THREAD"
                };
                int i = java.util.Arrays.asList(timeModes).indexOf(this.getText());
                i = (i + 1) % timeModes.length;
                Message.Builder builder = Message.newBuilder();
                builder.setContextId(0); // FIXME: proper context id
                builder.setType(Type.Response);
                builder.setExpectResponse(false);
                builder.setFunction(Message.Function.SETPROP);
                builder.setProp(Prop.TimeMode);
                builder.setArg0(i);
                messageQueue.addCommand(builder.build());
                this.setText(timeModes[i]);
                manager.update(true);
            }
        });

        actContext = new Action("Context: 0x", Action.AS_DROP_DOWN_MENU) {
            @Override
            public void run() {
                if (debugContexts.size() < 2)
                    return;
                final String idStr = this.getText().substring(
                                          "Context: 0x".length());
                if (idStr.length() == 0)
                    return;
                final int contextId = Integer.parseInt(idStr, 16);
                int index = debugContexts.indexOfKey(contextId);
                index = (index + 1) % debugContexts.size();
                changeContext(debugContexts.valueAt(index));
            }
        };
        manager.add(actContext);

        actionPort = new Action("5039", Action.AS_DROP_DOWN_MENU)
        {
            @Override
            public void run() {
                InputDialog dialog = new InputDialog(shell, "Port", "Debugger port",
                        actionPort.getText(), null);
                if (Window.OK == dialog.open()) {
                    actionPort.setText(dialog.getValue());
                    manager.update(true);
                }
            }
        };
        manager.add(actionPort);

        manager.add(new Action("CodeGen Frame", Action.AS_PUSH_BUTTON)
        {
            @Override
            public void run()
            {
                if (current != null)
                {
                    new CodeGen().codeGenFrame((Frame) viewer.getInput());
                    // need to reload current frame
                    viewer.setInput(current.getFrame(frameNum.getSelection()));
                }
            }
        });

        manager.add(new Action("CodeGen Frames", Action.AS_PUSH_BUTTON)
        {
            @Override
            public void run()
            {
                if (current != null)
                {
                    new CodeGen().codeGenFrames(current, frameNum.getSelection() + 1,
                            getSite().getShell());
                    // need to reload current frame
                    viewer.setInput(current.getFrame(frameNum.getSelection()));
                }
            }
        });
    }

    private void openFile() {
        FileDialog dialog = new FileDialog(getSite().getShell(), SWT.OPEN);
        dialog.setText("Open");
        dialog.setFilterExtensions(new String[] {
                "*.gles2dbg"
        });
        String filePath = dialog.open();
        if (filePath == null)
            return;
        FileInputStream file = null;
        try {
            file = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        running = true;
        messageQueue.start(targetByteOrder, file);
        thread = new Thread(this);
        thread.start();
        actionConnect.setText("Disconnect");
        getViewSite().getActionBars().getToolBarManager().update(true);
    }

    private void connectDisconnect() {
        if (!running) {
            running = true;
            messageQueue.start(targetByteOrder, null);
            thread = new Thread(this);
            thread.start();
            actionConnect.setText("Disconnect");
        } else {
            running = false;
            messageQueue.stop();
            actionConnect.setText("Connect");
        }
        this.getSite().getShell().getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                getViewSite().getActionBars().getToolBarManager().update(true);
            }
        });
    }

    void messageDataSelected(final MessageData msgData) {
        if (null == msgData)
            return;
        if (frameNum.getSelection() == frameNum.getMaximum())
            return; // scale max cannot overlap min, so max is array size
        final Frame frame = current.getFrame(frameNum.getSelection());
        final Context context = frame.computeContext(msgData);
        contextViewer.setInput(context);
        if (msgData.getImage() != null) {
            canvas.setBackgroundImage(msgData.getImage());
            tabFolder.setSelection(tabItemImage);
            canvas.redraw();
        } else if (null != msgData.shader) {
            text.setText(msgData.shader);
            tabFolder.setSelection(tabItemText);
        } else if (null != msgData.attribs) {
            StringBuilder builder = new StringBuilder();
            final int maxAttrib = msgData.msg.getArg7();
            for (int i = 0; i < msgData.attribs[0].length / 4; i++) {
                if (msgData.indices != null) {
                    builder.append(msgData.indices[i] & 0xffff);
                    builder.append(": ");
                }
                for (int j = 0; j < maxAttrib; j++) {
                    for (int k = 0; k < 4; k++)
                        builder.append(String.format("%.3g ", msgData.attribs[j][i * 4 + k]));
                    if (j < maxAttrib - 1)
                        builder.append("|| ");
                }
                builder.append('\n');
            }
            text.setText(builder.toString());
            tabFolder.setSelection(tabItemText);
        }
    }

    private void hookSelectionChanged() {
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                StructuredSelection selection = (StructuredSelection) event
                        .getSelection();
                if (null == selection)
                    return;
                MessageData msgData = (MessageData) selection.getFirstElement();
                messageDataSelected(msgData);
            }
        });
    }

    public void showError(final Exception e) {
        viewer.getControl().getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openError(viewer.getControl().getShell(),
                        "GL ES 2.0 Debugger Client", e.getMessage());
            }
        });
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    @Override
    public void run() {
        int newMessages = 0;

        boolean shaderEditorUpdate = false;
        while (running) {
            final Message oriMsg = messageQueue.removeCompleteMessage(0);
            if (oriMsg == null && !messageQueue.isRunning())
                break;
            if (newMessages > 60 || (newMessages > 0 && null == oriMsg)) {
                newMessages = 0;
                if (current != null && current.uiUpdate)
                    getSite().getShell().getDisplay().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (frameNum.getSelection() == current.frameCount() - 1 ||
                                    frameNum.getSelection() == current.frameCount() - 2)
                            {
                                viewer.refresh(false);
                                if (actionAutoScroll.isChecked())
                                    viewer.getList().setSelection(
                                            viewer.getList().getItemCount() - 1);
                            }
                            frameNum.setMaximum(current.frameCount());
                        }
                    });
                current.uiUpdate = false;

                if (shaderEditorUpdate)
                    this.getSite().getShell().getDisplay().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            shaderEditor.updateUI();
                        }
                    });
                shaderEditorUpdate = false;
            }
            if (null == oriMsg) {
                try {
                    Thread.sleep(1);
                    continue;
                } catch (InterruptedException e) {
                    showError(e);
                }
            }
            DebugContext debugContext = debugContexts.get(oriMsg.getContextId());
            if (debugContext == null) {
                debugContext = new DebugContext(oriMsg.getContextId());
                debugContexts.put(oriMsg.getContextId(), debugContext);
            }
            debugContext.processMessage(oriMsg);
            shaderEditorUpdate |= debugContext.currentContext.serverShader.uiUpdate;
            debugContext.currentContext.serverShader.uiUpdate = false;
            if (current == null && debugContext.frameCount() > 0)
                changeContext(debugContext);
            newMessages++;
        }
        if (running)
            connectDisconnect(); // error occurred, disconnect
    }

    /** can be called from non-UI thread */
    void changeContext(final DebugContext newContext) {
        getSite().getShell().getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                current = newContext;
                if (current != null)
                {
                    frameNum.setMaximum(current.frameCount());
                    if (frameNum.getSelection() >= current.frameCount())
                        if (current.frameCount() > 0)
                            frameNum.setSelection(current.frameCount() - 1);
                        else
                            frameNum.setSelection(0);
                    viewer.setInput(current.getFrame(frameNum.getSelection()));
                    actContext.setText("Context: 0x" + Integer.toHexString(current.contextId));
                }
                else
                {
                    frameNum.setMaximum(1); // cannot overlap min
                    frameNum.setSelection(0);
                    viewer.setInput(null);
                    actContext.setText("Context: 0x");
                }
                shaderEditor.updateUI();
                getViewSite().getActionBars().getToolBarManager().update(true);
            }
        });
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (e.widget != frameNum)
            assert false;
        if (current == null)
            return;
        if (frameNum.getSelection() == current.frameCount())
            return; // scale maximum cannot overlap minimum
        Frame frame = current.getFrame(frameNum.getSelection());
        viewer.setInput(frame);
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
    }
}
