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
import com.android.glesv2debugger.DebuggerMessage.Message.Type;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ShaderEditor extends Composite implements SelectionListener, ExtendedModifyListener,
        ProcessMessage {
    SampleView sampleView;

    ToolBar toolbar;
    ToolItem uploadShader, restoreShader, currentPrograms;
    List list;
    StyledText styledText;

    GLShader current;

    ArrayList<GLShader> shadersToUpload = new ArrayList<GLShader>();

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

        currentPrograms = new ToolItem(toolbar, SWT.PUSH);
        currentPrograms.setText("Current Programs: ");

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
        gridData.verticalSpan = 2;
        styledText.setLayoutData(gridData);
        styledText.addExtendedModifyListener(this);
    }

    public void updateUI() {
        list.removeAll();
        String progs = "Current Programs: ";
        for (int j = 0; j < sampleView.debugContexts.size(); j++) {
            final Context context = sampleView.debugContexts.valueAt(j).currentContext;

            if (context.serverShader.current != null) {
                progs += context.serverShader.current.name + "(0x";
                progs += Integer.toHexString(context.contextId) + ") ";
            }
            for (int i = 0; i < context.serverShader.shaders.size(); i++) {
                GLShader shader = context.serverShader.shaders.valueAt(i);
                StringBuilder builder = new StringBuilder();
                builder.append(String.format("%08X", context.contextId));
                builder.append(' ');
                builder.append(shader.type);
                while (builder.length() < 30)
                    builder.append(" ");
                builder.append(shader.name);
                while (builder.length() < 40)
                    builder.append(" ");
                builder.append(" : ");
                for (Context ctx : context.shares) {
                    builder.append(String.format("%08X", ctx.contextId));
                    builder.append(' ');
                }
                builder.append(": ");
                for (int program : shader.programs) {
                    builder.append(program);
                    builder.append(" ");
                }
                list.add(builder.toString());
            }

        }

        currentPrograms.setText(progs);
        toolbar.redraw();
        toolbar.pack(true);
        toolbar.update();
    }

    void uploadShader() {
        current.source = styledText.getText();

        // optional syntax check by glsl_compiler, built from external/mesa3d
        if (new File("./glsl_compiler").exists())
            try {
                File file = File.createTempFile("shader",
                        current.type == GLEnum.GL_VERTEX_SHADER ? ".vert" : ".frag");
                FileWriter fileWriter = new FileWriter(file, false);
                fileWriter.write(current.source);
                fileWriter.close();

                ProcessBuilder processBuilder = new ProcessBuilder(
                        "./glsl_compiler", "--glsl-es", file.getAbsolutePath());
                final Process process = processBuilder.start();
                InputStream is = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                String infolog = "";

                styledText.setLineBackground(0, styledText.getLineCount(), null);

                while ((line = br.readLine()) != null) {
                    infolog += line;
                    if (!line.startsWith("0:"))
                        continue;
                    String[] details = line.split(":|\\(|\\)");
                    final int ln = Integer.parseInt(details[1]);
                    if (ln > 0) // usually line 0 means errors other than syntax
                        styledText.setLineBackground(ln - 1, 1,
                                new Color(Display.getCurrent(), 255, 230, 230));
                }
                file.delete();
                if (infolog.length() > 0) {
                    if (!MessageDialog.openConfirm(getShell(),
                            "Shader Syntax Error, Continue?", infolog))
                        return;
                }
            } catch (IOException e) {
                sampleView.showError(e);
            }

        // add the initial command, which when read by server will set
        // expectResponse for the message loop and go into message exchange
        synchronized (shadersToUpload) {
            for (GLShader shader : shadersToUpload) {
                if (shader.context.context.contextId != current.context.context.contextId)
                    continue;
                MessageDialog.openWarning(this.getShell(), "Context 0x" +
                        Integer.toHexString(current.context.context.contextId),
                        "Previous shader upload not complete, try again");
                return;
            }
            shadersToUpload.add(current);
            final int contextId = current.context.context.contextId;
            Message.Builder builder = getBuilder(contextId);
            MessageParserEx.instance.parse(builder,
                    String.format("glShaderSource(%d,1,\"%s\",0)", current.name, current.source));
            sampleView.messageQueue.addCommand(builder.build());
        }
    }

    Message.Builder getBuilder(int contextId) {
        Message.Builder builder = Message.newBuilder();
        builder.setContextId(contextId);
        builder.setType(Type.Response);
        builder.setExpectResponse(true);
        return builder;
    }

    Message exchangeMessage(final int contextId, final MessageQueue queue,
            String format, Object... args) throws IOException {
        Message.Builder builder = getBuilder(contextId);
        MessageParserEx.instance.parse(builder, String.format(format, args));
        final Function function = builder.getFunction();
        queue.sendMessage(builder.build());
        final Message msg = queue.receiveMessage(contextId);
        assert msg.getContextId() == contextId;
        assert msg.getType() == Type.AfterGeneratedCall;
        assert msg.getFunction() == function;
        return msg;
    }

    // this is called from network thread
    public boolean processMessage(final MessageQueue queue, final Message msg)
            throws IOException {
        GLShader shader = null;
        final int contextId = msg.getContextId();
        synchronized (shadersToUpload) {
            if (shadersToUpload.size() == 0)
                return false;
            boolean matchingContext = false;
            for (int i = 0; i < shadersToUpload.size(); i++) {
                shader = shadersToUpload.get(i);
                for (Context ctx : shader.context.context.shares)
                    if (ctx.contextId == contextId) {
                        matchingContext = true;
                        break;
                    }
                if (matchingContext) {
                    shadersToUpload.remove(i);
                    break;
                }
            }
            if (!matchingContext)
                return false;
        }

        // glShaderSource was already sent to trigger set expectResponse
        assert msg.getType() == Type.AfterGeneratedCall;
        assert msg.getFunction() == Function.glShaderSource;

        exchangeMessage(contextId, queue, "glCompileShader(%d)", shader.name);

        // the 0, "" and {0} are dummies for the parser
        Message rcv = exchangeMessage(contextId, queue,
                "glGetShaderiv(%d, GL_COMPILE_STATUS, {0})", shader.name);
        assert rcv.hasData();
        if (rcv.getData().asReadOnlyByteBuffer().getInt() == 0) {
            // compile failed
            rcv = exchangeMessage(contextId, queue,
                    "glGetShaderInfoLog(%d, 0, 0, \"\")", shader.name);
            final String title = String.format("Shader %d in 0x%s failed to compile",
                    shader.name, Integer.toHexString(shader.context.context.contextId));
            final String message = rcv.getData().toStringUtf8();
            sampleView.getSite().getShell().getDisplay().syncExec(new Runnable() {
                @Override
                public void run()
                {
                    MessageDialog.openWarning(getShell(), title, message);
                }
            });
        } else
            for (int programName : shader.programs) {
                GLProgram program = shader.context.getProgram(programName);
                exchangeMessage(contextId, queue, "glLinkProgram(%d)", program.name);
                rcv = exchangeMessage(contextId, queue,
                        "glGetProgramiv(%d, GL_LINK_STATUS, {0})", program.name);
                assert rcv.hasData();
                if (rcv.getData().asReadOnlyByteBuffer().getInt() != 0)
                    continue;
                // link failed
                rcv = exchangeMessage(contextId, queue,
                            "glGetProgramInfoLog(%d, 0, 0, \"\")", program.name);
                final String title = String.format("Program %d in 0x%s failed to link",
                        program.name, Integer.toHexString(program.context.context.contextId));
                final String message = rcv.getData().toStringUtf8();
                sampleView.getSite().getShell().getDisplay().syncExec(new Runnable() {
                    @Override
                    public void run()
                    {
                        MessageDialog.openWarning(getShell(), title, message);
                    }
                });
                // break;
            }

        // TODO: add to upload results if failed

        Message.Builder builder = getBuilder(contextId);
        builder.setExpectResponse(false);
        if (queue.getPartialMessage(contextId) != null)
            // the glShaderSource interrupted a BeforeCall, so continue
            builder.setFunction(Function.CONTINUE);
        else
            builder.setFunction(Function.SKIP);
        queue.sendMessage(builder.build());

        return true;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (e.getSource() == uploadShader && null != current) {
            uploadShader();
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
                uploadShader();
            else if (rc == 1)
                current.source = styledText.getText();
            // else if (rc == 2) do nothing; selection is changing
        }
        String[] details = list.getSelection()[0].split("\\s+");
        final int contextId = Integer.parseInt(details[0], 16);
        int name = Integer.parseInt(details[2]);
        current = sampleView.debugContexts.get(contextId).currentContext.serverShader.shaders
                .get(name);
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
