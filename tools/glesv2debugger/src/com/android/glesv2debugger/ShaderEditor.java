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
import com.android.glesv2debugger.DebuggerMessage.Message.Type;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import java.util.ArrayList;

public class ShaderEditor extends Composite implements SelectionListener, ExtendedModifyListener {
    SampleView sampleView;

    ToolBar toolbar;
    ToolItem uploadShader, restoreShader;
    List list;
    StyledText styledText;

    GLShader current;

    ArrayList<Message> cmds = new ArrayList<Message>();

    ShaderEditor(SampleView sampleView, Composite parent) {
        super(parent, 0);
        this.sampleView = sampleView;

        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        this.setLayout(gridLayout);

        toolbar = new ToolBar(this, SWT.BORDER);

        uploadShader = new ToolItem(toolbar, SWT.PUSH);
        uploadShader.setText("Upload Shader");
        uploadShader.addSelectionListener(this);

        restoreShader = new ToolItem(toolbar, SWT.PUSH);
        restoreShader.setText("Original Shader");
        restoreShader.addSelectionListener(this);

        list = new List(this, SWT.V_SCROLL);
        list.setFont(new Font(parent.getDisplay(), "Courier", 10, 0));
        list.addSelectionListener(this);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.verticalAlignment = SWT.FILL;
        gridData.grabExcessVerticalSpace = true;
        list.setLayoutData(gridData);

        styledText = new StyledText(this, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.verticalAlignment = SWT.FILL;
        gridData.grabExcessVerticalSpace = true;
        styledText.setLayoutData(gridData);
        styledText.addExtendedModifyListener(this);
    }

    public void Update() {
        list.removeAll();
        for (Context context : sampleView.contexts.values()) {
            for (GLShader shader : context.serverShader.privateShaders.values()) {
                StringBuilder builder = new StringBuilder();
                builder.append(String.format("%08X", context.contextId));
                builder.append(' ');
                builder.append(shader.type);
                while (builder.length() < 30)
                    builder.append(" ");
                builder.append(shader.name);
                while (builder.length() < 40)
                    builder.append(" ");
                builder.append(':');
                for (Context ctx : context.shares) {
                    builder.append(String.format("%08X", ctx.contextId));
                    builder.append(' ');
                }
                builder.append(':');
                for (GLProgram program : shader.programs) {
                    builder.append(program.name);
                    builder.append(" ");
                }
                list.add(builder.toString());
            }
        }
    }

    void UploadShader() {
        current.source = styledText.getText();

        ArrayList<Message> cmds = new ArrayList<Message>();
        final int contextId = current.context.context.contextId;

        Message.Builder builder = GetBuilder(contextId);
        MessageParserEx.instance.Parse(builder,
                String.format("glShaderSource(%d,1,\"%s\",0)", current.name, current.source));
        cmds.add(builder.build());

        builder = GetBuilder(contextId);
        MessageParserEx.instance.Parse(builder,
                String.format("glCompileShader(%d)", current.name));
        cmds.add(builder.build());

        for (GLProgram program : current.programs) {
            builder = GetBuilder(contextId);
            MessageParserEx.instance.Parse(builder,
                    String.format("glLinkProgram(%d)", program.name));
            cmds.add(builder.build());
        }

        sampleView.messageQueue.AddCommands(cmds);
    }

    Message.Builder GetBuilder(int contextId) {
        Message.Builder builder = Message.newBuilder();
        builder.setContextId(contextId);
        builder.setType(Type.Response);
        builder.setExpectResponse(false);
        return builder;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (e.getSource() == uploadShader && null != current) {
            UploadShader();
            return;
        } else if (e.getSource() == restoreShader && null != current) {
            current.source = styledText.getText();
            styledText.setText(current.originalSource);
            return;
        }

        if (list.getSelectionCount() < 1)
            return;
        if (null != current && !current.source.equals(styledText.getText())) {
            String[] btns = {
                    "&Upload", "&Save", "&Discard"
            };
            MessageDialog dialog = new MessageDialog(this.getShell(), "Shader Edited",
                    null, "Shader source has been edited", MessageDialog.QUESTION, btns, 0);
            int rc = dialog.open();
            if (rc == SWT.DEFAULT || rc == 0)
                UploadShader();
            else if (rc == 1)
                current.source = styledText.getText();
            // else if (rc == 2) do nothing; selection is changing
        }
        String[] details = list.getSelection()[0].split("\\s+");
        final int contextId = Integer.parseInt(details[0], 16);
        int name = Integer.parseInt(details[2]);
        current = sampleView.contexts.get(contextId).serverShader.privateShaders.get(name);
        styledText.setText(current.source);
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
    }

    @Override
    public void modifyText(ExtendedModifyEvent event) {
        final String[] keywords = {
                "gl_Position", "gl_FragColor"
        };
        // FIXME: proper scanner for syntax highlighting
        String text = styledText.getText();
        int start = event.start;
        int end = event.start + event.length;
        start -= 20; // deleting chars from keyword causes rescan
        end += 20;
        if (start < 0)
            start = 0;
        if (end > text.length())
            end = text.length();
        if (null != styledText.getStyleRangeAtOffset(event.start)) {
            StyleRange clearStyleRange = new StyleRange();
            clearStyleRange.start = start;
            clearStyleRange.length = end - start;
            clearStyleRange.foreground = event.display.getSystemColor(SWT.COLOR_BLACK);
            styledText.setStyleRange(clearStyleRange);
        }

        while (start < end) {
            for (final String keyword : keywords) {
                if (!text.substring(start).startsWith(keyword))
                    continue;
                if (start > 0) {
                    final char before = text.charAt(start - 1);
                    if (Character.isLetterOrDigit(before))
                        continue;
                    else if (before == '_')
                        continue;
                }
                if (start + keyword.length() < text.length()) {
                    final char after = text.charAt(start + keyword.length());
                    if (Character.isLetterOrDigit(after))
                        continue;
                    else if (after == '_')
                        continue;
                }
                StyleRange style1 = new StyleRange();
                style1.start = start;
                style1.length = keyword.length();
                style1.foreground = event.display.getSystemColor(SWT.COLOR_BLUE);
                styledText.setStyleRange(style1);
            }
            start++;
        }
    }
}
