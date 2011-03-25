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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

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

public class SampleView extends ViewPart implements Runnable {

    boolean running = false;
    Thread thread;
    MessageQueue messageQueue;
    ViewContentProvider viewContentProvider;
    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "glesv2debuggerclient.views.SampleView";

    TableViewer viewer;
    org.eclipse.swt.widgets.Canvas canvas;
    Text text;
    Action actionConnect; // connect / disconnect
    Action doubleClickAction;
    Action actionAutoScroll;
    Action actionFilter;
    Action actionCapture;

    Point origin = new Point(0, 0); // for smooth scrolling canvas
    String[] filters = null;

    /*
     * The content provider class is responsible for providing objects to the
     * view. It can wrap existing objects in adapters or simply return objects
     * as-is. These objects may be sensitive to the current input of the view,
     * or ignore it and always show the same content (like Task List, for
     * example).
     */

    class ViewContentProvider implements IStructuredContentProvider {
        ArrayList<MessageData> entries = new ArrayList<MessageData>();

        public void add(final ArrayList<MessageData> msgs) {
            entries.addAll(msgs);
            viewer.getTable().getDisplay().syncExec(new Runnable() {
                @Override
                public void run() {
                    viewer.add(msgs.toArray());
                    org.eclipse.swt.widgets.ScrollBar bar = viewer
                            .getTable().getVerticalBar();
                    if (null != bar && actionAutoScroll.isChecked()) {
                        bar.setSelection(bar.getMaximum());
                        viewer.getTable().setSelection(
                                entries.size() - 1);
                    }
                }
            });
        }

        @Override
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public Object[] getElements(Object parent) {
            return entries.toArray();
        }
    }

    class ViewLabelProvider extends LabelProvider implements
            ITableLabelProvider {
        @Override
        public String getColumnText(Object obj, int index) {
            MessageData msgData = (MessageData) obj;
            if (null == msgData)
                return getText(obj);
            if (index >= msgData.columns.length)
                return null;
            return msgData.columns[index];
        }

        @Override
        public Image getColumnImage(Object obj, int index) {
            if (index > 0)
                return null;
            MessageData msgData = (MessageData) obj;
            if (null == msgData)
                return getImage(obj);
            if (null == msgData.image)
                return getImage(obj);
            return msgData.image;
        }

        @Override
        public Image getImage(Object obj) {
            return PlatformUI.getWorkbench().getSharedImages()
                    .getImage(ISharedImages.IMG_OBJ_ELEMENT);
        }
    }

    class NameSorter extends ViewerSorter {
    }

    class Filter extends ViewerFilter {
        @Override
        public boolean select(Viewer viewer, Object parentElement,
                Object element) {
            MessageData msgData = (MessageData) element;
            if (null == filters)
                return true;
            for (int i = 0; i < filters.length; i++)
                if (msgData.columns[0].contains(filters[i]))
                    return true;
            return false;
        }
    }

    /**
     * The constructor.
     */
    public SampleView() {
        messageQueue = new MessageQueue(this);
    }

    public void CreateTable(Composite parent) {
        Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI
                | SWT.FULL_SELECTION);
        TableLayout layout = new TableLayout();
        table.setLayout(layout);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        String[] headings = {
                "Name", "Elapsed (ms)", "Context", "Detail"
        };
        int[] weights = {
                35, 16, 16, 60
        };
        int[] widths = {
                120, 90, 90, 100
        };
        for (int i = 0; i < headings.length; i++) {
            layout.addColumnData(new ColumnWeightData(weights[i], widths[i],
                    true));
            TableColumn nameCol = new TableColumn(table, SWT.NONE, i);
            nameCol.setText(headings[i]);
        }

        viewer = new TableViewer(table);
        viewContentProvider = new ViewContentProvider();
        viewer.setContentProvider(viewContentProvider);
        viewer.setLabelProvider(new ViewLabelProvider());
        // viewer.setSorter(new NameSorter());
        viewer.setInput(getViewSite());
        viewer.setFilters(new ViewerFilter[] {
                new Filter()
        });
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    @Override
    public void createPartControl(Composite parent) {
        CreateTable(parent);

        // Create the help context id for the viewer's control
        PlatformUI.getWorkbench().getHelpSystem()
                .setHelp(viewer.getControl(), "GLESv2DebuggerClient.viewer");
        makeActions();
        hookContextMenu();
        hookDoubleClickAction();
        hookSelectionChanged();
        contributeToActionBars();

        class LayoutComposite extends Composite {
            public LayoutComposite(Composite parent, int style) {
                super(parent, style);
            }

            @Override
            public Control[] getChildren() {
                Control[] children = super.getChildren();
                ArrayList<Control> controls = new ArrayList<Control>();
                for (int i = 0; i < children.length; i++)
                    if (children[i].isVisible())
                        controls.add(children[i]);
                children = new Control[controls.size()];
                return controls.toArray(children);
            }

        }

        LayoutComposite layoutComposite = new LayoutComposite(parent, 0);
        layoutComposite.setLayout(new FillLayout());

        text = new Text(layoutComposite, SWT.NO_BACKGROUND | SWT.READ_ONLY
                | SWT.V_SCROLL | SWT.H_SCROLL);
        text.setVisible(false);

        canvas = new Canvas(layoutComposite, SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE
                | SWT.V_SCROLL | SWT.H_SCROLL);
        canvas.setVisible(false);

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
        manager.add(actionConnect);
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

        actionCapture = new Action("Capture", Action.AS_CHECK_BOX) {
            @Override
            public void run() {
                Message.Builder builder = Message.newBuilder();
                builder.setContextId(0); // FIXME: proper context id
                builder.setType(Type.Response);
                builder.setExpectResponse(false);
                builder.setFunction(Function.SETPROP);
                builder.setProp(Prop.Capture);
                builder.setArg0(isChecked() ? 1 : 0);
                messageQueue.AddCommand(builder.build());
                manager.update(true);
            }
        };
        actionCapture.setChecked(false);
        manager.add(actionCapture);

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
                messageQueue.AddCommand(builder.build());
                this.setText(timeModes[i]);
                manager.update(true);
            }
        });
    }

    private void ConnectDisconnect() {
        if (!running) {
            running = true;
            messageQueue.Start();
            thread = new Thread(this);
            thread.start();
            actionConnect.setText("Disconnect");
            actionConnect.setToolTipText("Disconnect from debuggee");
        } else {
            running = false;
            messageQueue.Stop();
            actionConnect.setText("Connect");
            actionConnect.setToolTipText("Connect to debuggee");
        }
        this.getSite().getShell().getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                getViewSite().getActionBars().getToolBarManager().update(true);
            }
        });
    }

    private void makeActions() {
        actionConnect = new Action() {
            @Override
            public void run() {
                ConnectDisconnect();
            }
        };
        actionConnect.setText("Connect");
        actionConnect.setToolTipText("Connect to debuggee");
        // action1.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
        // .getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

        doubleClickAction = new Action() {
            @Override
            public void run() {
                IStructuredSelection selection = (IStructuredSelection) viewer
                        .getSelection();
                MessageData msgData = (MessageData) selection.getFirstElement();
                if (null != msgData.shader)
                    showMessage(msgData.shader);
                else if (null != msgData.data)
                {
                    String str = "";
                    for (int i = 0; i < msgData.data.length; i++)
                    {
                        str += String.format("%f", msgData.data[i]);
                        if (i % (4 * msgData.maxAttrib) == (4 * msgData.maxAttrib - 1))
                            str += '\n';
                        else if (i % 4 == 3)
                            str += " -";
                        if (i < msgData.data.length - 1)
                            str += ' ';
                    }
                    showMessage(str);
                }
                else
                    showMessage(msgData.columns[3].toString());
            }
        };
    }

    private void hookDoubleClickAction() {
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                doubleClickAction.run();
            }
        });
    }

    private void hookSelectionChanged() {
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                StructuredSelection selection = (StructuredSelection) event
                        .getSelection();
                if (null == selection)
                    return;
                if (1 != selection.size())
                {
                    Object[] objects = selection.toArray();
                    float totalTime = 0;
                    for (int i = 0; i < objects.length; i++)
                    {
                        MessageData msgData = (MessageData) objects[i];
                        if (null == msgData)
                            continue;
                        totalTime += Float.parseFloat(msgData.columns[1]);
                    }
                    viewer.getTable().getColumn(1).setText(Float.toString(totalTime));
                    return;
                }
                else
                    viewer.getTable().getColumn(1).setText("Elapsed (ms)");
                MessageData msgData = (MessageData) selection.getFirstElement();
                if (null == msgData)
                    return;
                if (null != msgData.image)
                {
                    text.setVisible(false);
                    canvas.setVisible(true);
                    canvas.setBackgroundImage(msgData.image);
                    canvas.getParent().layout();
                }
                else if (null != msgData.shader)
                {
                    text.setText(msgData.shader);
                    text.setVisible(true);
                    canvas.setVisible(false);
                    text.getParent().layout();
                }
                else if (null != msgData.data)
                {
                    String str = "";
                    for (int i = 0; i < msgData.data.length; i++)
                    {
                        str += String.format("%.3g", msgData.data[i]);
                        if (i % (4 * msgData.maxAttrib) == (4 * msgData.maxAttrib - 1))
                            str += '\n';
                        else if (i % 4 == 3)
                            str += " -";
                        if (i < msgData.data.length - 1)
                            str += ' ';
                    }

                    text.setText(str);
                    text.setVisible(true);
                    canvas.setVisible(false);
                    text.getParent().layout();
                }
            }

        });
    }

    private void showMessage(final String message) {
        viewer.getControl().getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openInformation(viewer.getControl().getShell(),
                        "GL ES 2.0 Debugger Client", message);
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
        FileWriter file = null;
        PrintWriter writer = null;
        try {
            file = new FileWriter("GLES2Debugger.log", true);
            writer = new PrintWriter(file);
            writer.write("\n\n");
            writer.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance()
                    .getTime()));
        } catch (IOException e1) {
            showError(e1);
        }
        ArrayList<MessageData> msgs = new ArrayList<MessageData>();
        while (running) {
            if (!messageQueue.IsRunning())
                break;

            Message msg = messageQueue.RemoveMessage(0);
            if (msgs.size() > 40) {
                viewContentProvider.add(msgs);
                msgs.clear();
            }
            if (null == msg) {
                try {
                    Thread.sleep(1);
                    continue;
                } catch (InterruptedException e) {
                    showError(e);
                }
            }
            final MessageData msgData = new MessageData(this.getViewSite()
                    .getShell().getDisplay(), msg);
            if (null != writer) {
                writer.write(msgData.columns[0]);
                for (int i = 0; i < 30 - msgData.columns[0].length(); i++)
                    writer.write(" ");
                writer.write("\t");
                writer.write(msgData.columns[1] + " \t ");
                writer.write(msgData.columns[2] + " \t ");
                writer.write(msgData.columns[3] + " \n");
                if (msgData.columns[0] == "eglSwapBuffers") {
                    writer.write("\n-------\n");
                    writer.flush();
                }
            }
            msgs.add(msgData);
        }
        if (running)
            ConnectDisconnect(); // error occurred, disconnect
        if (null != writer) {
            writer.flush();
            writer.close();
        }
    }
}
