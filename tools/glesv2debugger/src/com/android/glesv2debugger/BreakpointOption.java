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

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
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

import java.io.IOException;

public class BreakpointOption extends ScrolledComposite implements SelectionListener,
        ProcessMessage {

    SampleView sampleView;
    Button[] buttonsBreak = new Button[Function.values().length];
    /** cache of buttonsBreak[Function.getNumber()].getSelection */
    boolean[] breakpoints = new boolean[Function.values().length];

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
            breakpoints[Function.values()[i].getNumber()] = btn.getSelection();
            buttonsBreak[Function.values()[i].getNumber()] = btn;
        }

        Point size = composite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        composite.setSize(size);
        this.setContent(composite);
        this.setExpandHorizontal(true);
        this.setExpandVertical(true);
        this.setMinSize(size);
        this.layout();
    }

    void setBreakpoint(final int contextId, final Function function, final boolean enabled) {
        Message.Builder builder = Message.newBuilder();
        builder.setContextId(contextId);
        builder.setType(Type.Response);
        builder.setExpectResponse(false);
        builder.setFunction(Function.SETPROP);
        builder.setProp(Prop.ExpectResponse);
        builder.setArg0(function.getNumber());
        builder.setArg1(enabled ? 1 : 0);
        sampleView.messageQueue.addCommand(builder.build());
        breakpoints[function.getNumber()] = enabled;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        Button btn = (Button) e.widget;
        Group group = (Group) btn.getParent();
        int contextId = 0;
        if (sampleView.current != null)
            contextId = sampleView.current.contextId;
        setBreakpoint(contextId, Function.valueOf(group.getText()), btn.getSelection());
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    private Function lastFunction = Function.NEG;

    public boolean processMessage(final MessageQueue queue, final Message msg) throws IOException {
        if (!breakpoints[msg.getFunction().getNumber()])
            return false;
        // use DefaultProcessMessage just to register the GL call
        // but do not send response
        final int contextId = msg.getContextId();
        if (msg.getType() == Type.BeforeCall || msg.getType() == Type.AfterCall)
            queue.defaultProcessMessage(msg, true, false);
        final Message.Builder builder = Message.newBuilder();
        builder.setContextId(contextId);
        builder.setType(Type.Response);
        builder.setExpectResponse(true);
        final Shell shell = sampleView.getViewSite().getShell();
        final boolean send[] = new boolean[1];
        shell.getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                String call = MessageFormatter.format(msg, false);
                call = call.substring(0, call.indexOf("(")) + ' ' +
                        msg.getFunction() + call.substring(call.indexOf("("));
                if (msg.hasData() && msg.getFunction() == Function.glShaderSource)
                {
                    int index = call.indexOf("string=") + 7;
                    String ptr = call.substring(index, call.indexOf(',', index));
                    call = call.replace(ptr, '"' + msg.getData().toStringUtf8() + '"');
                }
                if (msg.getType() == Type.AfterCall)
                {
                    call = "skip " + call;
                    builder.setFunction(Function.SKIP);
                }
                else if (msg.getType() == Type.BeforeCall)
                {
                    call = "continue " + call;
                    builder.setFunction(Function.CONTINUE);
                }
                else
                {
                    assert msg.getType() == Type.AfterGeneratedCall;
                    assert msg.getFunction() == lastFunction;
                    call = "skip " + call;
                    builder.setFunction(Function.SKIP);
                }
                InputDialog inputDialog = new InputDialog(shell,
                            msg.getFunction().toString() + " " + msg.getType().toString(),
                        "(s)kip, (c)continue, (r)emove bp or glFunction(...)",
                            call, null);
                if (Window.OK == inputDialog.open())
                {
                    String s = inputDialog.getValue().substring(0, 1).toLowerCase();
                    if (s.startsWith("s"))
                    {
                        builder.setFunction(Function.SKIP);
                        // AfterCall is skipped, so push BeforeCall to complete
                        if (queue.getPartialMessage(contextId) != null)
                            queue.completePartialMessage(contextId);
                    }
                    else if (s.startsWith("c"))
                        builder.setFunction(Function.CONTINUE);
                    else if (s.startsWith("r"))
                    {
                        Button btn = buttonsBreak[msg.getFunction().getNumber()];
                        btn.setSelection(false);
                        setBreakpoint(msg.getContextId(), msg.getFunction(), false);
                        builder.setExpectResponse(false);
                    }
                    else
                    {
                        MessageParserEx.instance.parse(builder, inputDialog.getValue());
                        lastFunction = builder.getFunction();
                        builder.setExpectResponse(true);
                        // AfterCall is skipped, so push BeforeCall to complete
                        if (queue.getPartialMessage(contextId) != null)
                            queue.completePartialMessage(contextId);
                    }
                }
                // else defaults to continue BeforeCall and skip AfterCall
            }
        });
        queue.sendMessage(builder.build());
        return true;
    }
}
