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
import com.android.glesv2debugger.DebuggerMessage.Message.DataType;
import com.android.glesv2debugger.DebuggerMessage.Message.Function;
import com.android.glesv2debugger.DebuggerMessage.Message.Prop;
import com.android.sdklib.util.SparseArray;
import com.android.sdklib.util.SparseIntArray;
import com.google.protobuf.ByteString;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

class Frame {
    public final long filePosition;
    private int callsCount;

    final Context startContext;
    private ArrayList<MessageData> calls = new ArrayList<MessageData>();

    Frame(final Context context, final long filePosition) {
        this.startContext = context.clone();
        this.filePosition = filePosition;
    }

    void add(final MessageData msgData) {
        calls.add(msgData);
    }

    void increaseCallsCount() {
        callsCount++;
    }

    Context computeContext(final MessageData call) {
        Context ctx = startContext.clone();
        for (int i = 0; i < calls.size(); i++)
            if (call == calls.get(i))
                return ctx;
            else
                ctx.processMessage(calls.get(i).msg);
        assert false;
        return ctx;
    }

    int size() {
        return callsCount;
    }

    MessageData get(final int i) {
        return calls.get(i);
    }

    ArrayList<MessageData> get() {
        return calls;
    }

    void unload() {
        if (calls == null)
            return;
        calls.clear();
        calls = null;
    }

    void load(final RandomAccessFile file) {
        if (calls != null && calls.size() == callsCount)
            return;
        try {
            Context ctx = startContext.clone();
            calls = new ArrayList<MessageData>(callsCount);
            final long oriPosition = file.getFilePointer();
            file.seek(filePosition);
            for (int i = 0; i < callsCount; i++) {
                int len = file.readInt();
                if (SampleView.targetByteOrder == ByteOrder.LITTLE_ENDIAN)
                    len = Integer.reverseBytes(len);
                final byte[] data = new byte[len];
                file.read(data);
                Message msg = Message.parseFrom(data);
                ctx.processMessage(msg);
                final MessageData msgData = new MessageData(Display.getCurrent(), msg, ctx);
                calls.add(msgData);
            }
            file.seek(oriPosition);
        } catch (IOException e) {
            e.printStackTrace();
            assert false;
        }
    }
}

class DebugContext {
    boolean uiUpdate = false;
    final int contextId;
    Context currentContext;
    private ArrayList<Frame> frames = new ArrayList<Frame>(128);
    private Frame lastFrame;
    private Frame loadedFrame;
    private RandomAccessFile file;

    DebugContext(final int contextId) {
        this.contextId = contextId;
        currentContext = new Context(contextId);
        try {
            file = new RandomAccessFile("0x" + Integer.toHexString(contextId) +
                    ".gles2dbg", "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            assert false;
        }
    }

    /** write message to file; if frame not null, then increase its call count */
    void saveMessage(final Message msg, final RandomAccessFile file, Frame frame) {
        synchronized (file) {
            if (frame != null)
                frame.increaseCallsCount();
            final byte[] data = msg.toByteArray();
            final ByteBuffer len = ByteBuffer.allocate(4);
            len.order(SampleView.targetByteOrder);
            len.putInt(data.length);
            try {
                if (SampleView.targetByteOrder == ByteOrder.BIG_ENDIAN)
                    file.writeInt(data.length);
                else
                    file.writeInt(Integer.reverseBytes(data.length));
                file.write(data);
            } catch (IOException e) {
                e.printStackTrace();
                assert false;
            }
        }
    }

    /**
     * Caches new Message, and formats into MessageData for current frame; this
     * function is called exactly once for each new Message
     */
    void processMessage(final Message newMsg) {
        Message msg = newMsg;
        if (msg.getFunction() == Function.SETPROP) {
            // GL impl. consts should have been sent before any GL call messages
            assert frames.size() == 0;
            assert lastFrame == null;
            assert msg.getProp() == Prop.GLConstant;
            switch (GLEnum.valueOf(msg.getArg0())) {
                case GL_MAX_VERTEX_ATTRIBS:
                    currentContext.serverVertex = new GLServerVertex(msg.getArg1());
                    break;
                case GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS:
                    currentContext.serverTexture = new GLServerTexture(currentContext,
                            msg.getArg1());
                    break;
                default:
                    assert false;
                    return;
            }
            saveMessage(msg, file, null);
            return;
        }

        if (lastFrame == null) {
            // first real message after the GL impl. consts
            synchronized (file) {
                try {
                    lastFrame = new Frame(currentContext, file.getFilePointer());
                } catch (IOException e) {
                    e.printStackTrace();
                    assert false;
                }
            }
            synchronized (frames) {
                frames.add(lastFrame);
            }
            assert loadedFrame == null;
            loadedFrame = lastFrame;
        }
        currentContext.processMessage(msg);
        if (msg.hasDataType() && msg.getDataType() == DataType.ReferencedImage) {
            // decode referenced image so it doesn't rely on context later on
            final byte[] referenced = MessageProcessor.lzfDecompressChunks(msg.getData());
            currentContext.readPixelRef = MessageProcessor.decodeReferencedImage(
                        currentContext.readPixelRef, referenced);
            final byte[] decoded = MessageProcessor.lzfCompressChunks(
                        currentContext.readPixelRef, referenced.length);
            msg = msg.toBuilder().setDataType(DataType.NonreferencedImage)
                        .setData(ByteString.copyFrom(decoded)).build();
        }
        saveMessage(msg, file, lastFrame);
        if (loadedFrame == lastFrame) {
            // frame selected for view, so format MessageData
            final MessageData msgData = new MessageData(Display.getCurrent(), msg, currentContext);
            lastFrame.add(msgData);
            uiUpdate = true;
        }
        if (msg.getFunction() != Function.eglSwapBuffers)
            return;
        synchronized (frames) {
            if (loadedFrame != lastFrame)
                lastFrame.unload();
            try {
                frames.add(lastFrame = new Frame(currentContext, file.getFilePointer()));
                // file.getChannel().force(false);
                uiUpdate = true;
            } catch (IOException e) {
                e.printStackTrace();
                assert false;
            }
        }
        return;
    }

    Frame getFrame(int index) {
        synchronized (frames) {
            Frame newFrame = frames.get(index);
            if (loadedFrame != null && loadedFrame != lastFrame && newFrame != loadedFrame) {
                loadedFrame.unload();
                uiUpdate = true;
            }
            loadedFrame = newFrame;
            synchronized (file) {
                loadedFrame.load(file);
            }
            return loadedFrame;
        }
    }

    int frameCount() {
        synchronized (frames) {
            return frames.size();
        }
    }
}

/** aggregate of GL states */
public class Context implements Cloneable {
    public final int contextId;
    public ArrayList<Context> shares = new ArrayList<Context>(); // self too
    public GLServerVertex serverVertex;
    public GLServerShader serverShader = new GLServerShader(this);
    public GLServerState serverState = new GLServerState(this);
    public GLServerTexture serverTexture;

    byte[] readPixelRef = new byte[0];

    public Context(int contextId) {
        this.contextId = contextId;
        shares.add(this);
    }

    @Override
    public Context clone() {
        try {
            Context copy = (Context) super.clone();
            // FIXME: context sharing list clone
            copy.shares = new ArrayList<Context>(1);
            copy.shares.add(copy);
            if (serverVertex != null)
                copy.serverVertex = serverVertex.clone();
            copy.serverShader = serverShader.clone(copy);
            copy.serverState = serverState.clone();
            if (serverTexture != null)
                copy.serverTexture = serverTexture.clone(copy);
            // don't need to clone readPixelsRef, since referenced images
            // are decoded when they are encountered
            return copy;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            assert false;
            return null;
        }
    }

    /** mainly updating states */
    public void processMessage(Message msg) {
        if (serverVertex.process(msg))
            return;
        if (serverShader.processMessage(msg))
            return;
        if (serverState.processMessage(msg))
            return;
        if (serverTexture.processMessage(msg))
            return;
    }
}

class ContextViewProvider extends LabelProvider implements ITreeContentProvider,
        ISelectionChangedListener {
    Context context;
    final SampleView sampleView;

    ContextViewProvider(final SampleView sampleView) {
        this.sampleView = sampleView;
    }

    @Override
    public void dispose() {
    }

    @Override
    public String getText(Object obj) {
        if (obj == null)
            return "null";
        if (obj instanceof Entry) {
            Entry entry = (Entry) obj;
            String objStr = "null (or default)";
            if (entry.obj != null) {
                objStr = entry.obj.toString();
                if (entry.obj instanceof Message)
                    objStr = MessageFormatter.format((Message) entry.obj, false);
            }
            return entry.name + " = " + objStr;
        }
        return obj.toString();
    }

    @Override
    public Image getImage(Object obj) {
        if (!(obj instanceof Entry))
            return null;
        final Entry entry = (Entry) obj;
        if (!(entry.obj instanceof Message))
            return null;
        final Message msg = (Message) entry.obj;
        switch (msg.getFunction()) {
            case glTexImage2D:
            case glTexSubImage2D:
            case glCopyTexImage2D:
            case glCopyTexSubImage2D: {
                entry.image = new MessageData(Display.getCurrent(), msg, null).getImage();
                if (entry.image == null)
                    return null;
                return new Image(Display.getCurrent(), entry.image.getImageData().scaledTo(96, 96));
            }
            default:
                return null;
        }
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        StructuredSelection selection = (StructuredSelection) event
                .getSelection();
        if (null == selection)
            return;
        final Object obj = selection.getFirstElement();
        if (!(obj instanceof Entry))
            return;
        final Entry entry = (Entry) obj;
        if (entry.image == null)
            return;
        sampleView.tabFolder.setSelection(sampleView.tabItemImage);
        sampleView.canvas.setBackgroundImage(entry.image);
        sampleView.canvas.redraw();
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        context = (Context) newInput;
    }

    class Entry {
        String name;
        Object obj;
        Image image;

        Entry(String name, Object obj) {
            this.name = name;
            this.obj = obj;
        }
    }

    @Override
    public Object[] getElements(Object inputElement) {
        if (inputElement != context)
            return null;
        return getChildren(new Entry("Context", inputElement));
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if (!(parentElement instanceof Entry))
            return null;
        Entry entry = (Entry) parentElement;
        ArrayList<Object> children = new ArrayList<Object>();
        if (entry.obj == context.serverState.enableDisables) {
            for (int i = 0; i < context.serverState.enableDisables.size(); i++) {
                final int key = context.serverState.enableDisables.keyAt(i);
                final int value = context.serverState.enableDisables.valueAt(i);
                children.add(GLEnum.valueOf(key).name() + " = " + value);
            }
        } else if (entry.obj == context.serverState.integers) {
            for (int i = 0; i < context.serverState.integers.size(); i++) {
                final int key = context.serverState.integers.keyAt(i);
                final Message val = context.serverState.integers.valueAt(i);
                if (val != null)
                    children.add(GLEnum.valueOf(key).name() + " : " +
                            MessageFormatter.format(val, false));
                else
                    children.add(GLEnum.valueOf(key).name() + " : default");
            }
        } else if (entry.obj == context.serverState.lastSetter) {
            for (int i = 0; i < context.serverState.lastSetter.size(); i++) {
                final int key = context.serverState.lastSetter.keyAt(i);
                final Message msg = context.serverState.lastSetter.valueAt(i);
                if (msg == null)
                    children.add(Function.valueOf(key).name() + " : default");
                else
                    children.add(Function.valueOf(key).name() + " : "
                            + MessageFormatter.format(msg, false));
            }
        } else if (entry.obj instanceof SparseArray) {
            SparseArray<?> sa = (SparseArray<?>) entry.obj;
            for (int i = 0; i < sa.size(); i++)
                children.add(new Entry("[" + sa.keyAt(i) + "]", sa.valueAt(i)));
        } else if (entry.obj instanceof Map) {
            Set<?> set = ((Map<?, ?>) entry.obj).entrySet();
            for (Object o : set) {
                Map.Entry e = (Map.Entry) o;
                children.add(new Entry(e.getKey().toString(), e.getValue()));
            }
        } else if (entry.obj instanceof SparseIntArray) {
            SparseIntArray sa = (SparseIntArray) entry.obj;
            for (int i = 0; i < sa.size(); i++)
                children.add("[" + sa.keyAt(i) + "] = " + sa.valueAt(i));
        } else if (entry.obj instanceof Collection) {
            Collection<?> collection = (Collection<?>) entry.obj;
            for (Object o : collection)
                children.add(new Entry("[?]", o));
        } else if (entry.obj.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(entry.obj); i++)
                children.add(new Entry("[" + i + "]", Array.get(entry.obj, i)));
        } else {
            Field[] fields = entry.obj.getClass().getFields();
            for (Field f : fields) {
                try {
                    children.add(new Entry(f.getName(), f.get(entry.obj)));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return children.toArray();
    }

    @Override
    public Object getParent(Object element) {
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        if (element == null)
            return false;
        if (!(element instanceof Entry))
            return false;
        Object obj = ((Entry) element).obj;
        if (obj == null)
            return false;
        if (obj instanceof SparseArray)
            return ((SparseArray<?>) obj).size() > 0;
        else if (obj instanceof SparseIntArray)
            return ((SparseIntArray) obj).size() > 0;
        else if (obj instanceof Collection)
            return ((Collection<?>) obj).size() > 0;
        else if (obj instanceof Map)
            return ((Map<?, ?>) obj).size() > 0;
        else if (obj.getClass().isArray())
            return Array.getLength(obj) > 0;
        else if (obj instanceof Message)
            return false;
        else if (isPrimitive(obj))
            return false;
        else if (obj.getClass().equals(String.class))
            return false;
        else if (obj.getClass().equals(Message.class))
            return false;
        else if (obj instanceof GLEnum)
            return false;
        return obj.getClass().getFields().length > 0;
    }

    static boolean isPrimitive(final Object obj) {
        final Class<? extends Object> c = obj.getClass();
        if (c.isPrimitive())
            return true;
        if (c == Integer.class)
            return true;
        if (c == Boolean.class)
            return true;
        if (c == Float.class)
            return true;
        if (c == Short.class)
            return true;
        return false;
    }
}
