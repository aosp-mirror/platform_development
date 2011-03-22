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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import java.util.HashMap;

public class BreakpointOption extends ScrolledComposite implements SelectionListener {

    SampleView sampleView;
    HashMap<Function, Button> buttons = new HashMap<Function, Button>();

    BreakpointOption(SampleView sampleView, Composite parent) {
        super(parent, SWT.NO_BACKGROUND | SWT.V_SCROLL | SWT.H_SCROLL);
        this.sampleView = sampleView;

        Composite composite = new Composite(this, 0);
        GridLayout layout = new GridLayout();
        layout.numColumns = 4;
        composite.setLayout(layout);
        this.setLayout(new FillLayout());

        for (int i = 0; i < Function.values().length; i++) {
            Group group = new Group(composite, 0);
            group.setLayout(new RowLayout());
            group.setText(Function.values()[i].toString());
            Button btn = new Button(group, SWT.CHECK);
            btn.addSelectionListener(this);
            btn.setText("Break");
            btn.setSelection(false);
            buttons.put(Function.values()[i], btn);
        }

        Point size = composite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        composite.setSize(size);
        this.setContent(composite);
        this.setExpandHorizontal(true);
        this.setExpandVertical(true);
        this.setMinSize(size);
        this.layout();
        // this.pack(true);
    }

    void SetBreakpoint(Function function, boolean enabled) {
        Message.Builder builder = Message.newBuilder();
        builder.setContextId(0); // FIXME: proper context id
        builder.setType(Type.Response);
        builder.setExpectResponse(false);
        builder.setFunction(Function.SETPROP);
        builder.setProp(Prop.ExpectResponse);
        builder.setArg0(function.getNumber());
        builder.setArg1(enabled ? 1 : 0);
        sampleView.messageQueue.AddCommand(builder.build());
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        Button btn = (Button) e.widget;
        Group group = (Group) btn.getParent();
        SetBreakpoint(Function.valueOf(group.getText()), btn.getSelection());
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    public void BreakpointReached(final Message.Builder builder, final Message msg) {
        final Shell shell = sampleView.getViewSite().getShell();
        shell.getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                String[] btns = {
                        "&Continue", "&Skip", "&Remove"
                };
                int defaultBtn = 0;
                if (msg.getType() == Type.AfterCall)
                    defaultBtn = 1;
                String message = msg.getFunction().toString();
                if (msg.hasTime())
                    message += String.format("\n%.3fms", msg.getTime());
                message += "\n" + MessageFormatter.Format(msg);
                MessageDialog dialog = new MessageDialog(shell, "Breakpoint " + msg.getType(),
                        null, message, MessageDialog.QUESTION, btns, defaultBtn);
                int rc = dialog.open();
                if (rc == SWT.DEFAULT || rc == 0)
                    builder.setFunction(Function.CONTINUE);
                else if (rc == 1)
                    builder.setFunction(Function.SKIP);
                else if (rc == 2)
                {
                    Button btn = buttons.get(msg.getFunction());
                    btn.setSelection(false);
                    SetBreakpoint(msg.getFunction(), false);
                }
                else
                    assert false;

            }
        });

    }
}
