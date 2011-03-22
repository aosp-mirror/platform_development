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
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
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
import java.util.HashMap;

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

    ListViewer viewer;
    org.eclipse.swt.widgets.Canvas canvas;
    Text text;
    Action actionConnect; // connect / disconnect
    Action doubleClickAction;
    Action actionAutoScroll;
    Action actionFilter;
    Action actionCapture;
    Action actionPort;

    Point origin = new Point(0, 0); // for smooth scrolling canvas
    String[] filters = null;
    public HashMap<Integer, Context> contexts = new HashMap<Integer, Context>();

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
            viewer.getList().getDisplay().syncExec(new Runnable() {
                @Override
                public void run() {
                    viewer.add(msgs.toArray());
                    org.eclipse.swt.widgets.ScrollBar bar = viewer
                            .getList().getVerticalBar();
                    if (null != bar && actionAutoScroll.isChecked()) {
                        bar.setSelection(bar.getMaximum());
                        viewer.getList().setSelection(
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
            ILabelProvider {
        @Override
        public String getText(Object obj) {
            MessageData msgData = (MessageData) obj;
            if (null == msgData)
                return obj.toString();
            return msgData.text;
        }

        @Override
        public Image getImage(Object obj) {
            MessageData msgData = (MessageData) obj;
            if (null == msgData.image)
                return PlatformUI.getWorkbench().getSharedImages()
                        .getImage(ISharedImages.IMG_OBJ_ELEMENT);
            return msgData.image;
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

    /**
     * The constructor.
     */
    public SampleView() {
        messageQueue = new MessageQueue(this);
    }

    public void CreateView(Composite parent) {
        viewer = new ListViewer(parent);
        viewer.getList().setFont(new Font(viewer.getList().getDisplay(), "Courier", 10, SWT.BOLD));
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
        CreateView(parent);

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

        actionPort = new Action("5039", Action.AS_DROP_DOWN_MENU)
        {
            @Override
            public void run() {
                org.eclipse.jface.dialogs.InputDialog dialog = new org.eclipse.jface.dialogs.InputDialog(
                        shell, "Port",
                        "Debugger port",
                        actionPort.getText(), null);
                if (Window.OK == dialog.open()) {
                    actionPort.setText(dialog.getValue());
                    manager.update(true);
                }
            }
        };
        manager.add(actionPort);
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

        doubleClickAction = new Action() {
            @Override
            public void run() {
                IStructuredSelection selection = (IStructuredSelection) viewer
                        .getSelection();
                MessageData msgData = (MessageData) selection.getFirstElement();
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
                    StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < msgData.data.length; i++)
                    {
                        builder.append(String.format("%.3g", msgData.data[i]));
                        if (i % (4 * msgData.maxAttrib) == (4 * msgData.maxAttrib - 1))
                            builder.append('\n');
                        else if (i % 4 == 3)
                            builder.append(" -");
                        if (i < msgData.data.length - 1)
                            builder.append(' ');
                    }

                    text.setText(builder.toString());
                    text.setVisible(true);
                    canvas.setVisible(false);
                    text.getParent().layout();
                }
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
            writer.write("\n\n");
        } catch (IOException e1) {
            showError(e1);
        }
        ArrayList<MessageData> msgs = new ArrayList<MessageData>();
        while (running) {
            if (!messageQueue.IsRunning())
                break;

            Message msg = messageQueue.RemoveMessage(0);
            if (msgs.size() > 60 || (msgs.size() > 0 && null == msg)) {
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

            Context context = contexts.get(msg.getContextId());
            if (null == context) {
                context = new Context();
                contexts.put(msg.getContextId(), context);
            }
            msg = context.ProcessMessage(msg);

            final MessageData msgData = new MessageData(this.getViewSite()
                    .getShell().getDisplay(), msg, context);
            if (null != writer) {
                writer.write(msgData.text + "\n");
                if (msgData.msg.getFunction() == Function.eglSwapBuffers) {
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
