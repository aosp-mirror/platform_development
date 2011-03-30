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
import com.android.sdklib.util.SparseArray;
import com.android.sdklib.util.SparseIntArray;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class Context implements Cloneable {
    public final int contextId;
    public ArrayList<Context> shares = new ArrayList<Context>(); // self too
    public GLServerVertex serverVertex = new GLServerVertex();
    public GLServerShader serverShader = new GLServerShader(this);
    public GLServerState serverState = new GLServerState(this);

    byte[] readPixelRef = new byte[0];

    Message processed = null; // return; processed Message

    public Context(int contextId) {
        this.contextId = contextId;
        shares.add(this);
    }

    // returns instance TODO: return new instance if changed
    public Context ProcessMessage(Message msg) {
        GLServerVertex newVertex = serverVertex.Process(msg);
        if (newVertex != null) {
            processed = newVertex.processed;
            assert newVertex == serverVertex;
            return this;
        }

        GLServerShader newShader = serverShader.ProcessMessage(msg);
        if (newShader != null) {
            assert newShader == serverShader;
            return this;
        }

        GLServerState newState = serverState.ProcessMessage(msg);
        if (newState != null) {
            if (newState == serverState)
                return this;
            Context newContext = null;
            try {
                newContext = (Context) clone();
            } catch (CloneNotSupportedException e) {
                assert false;
            }
            newContext.serverState = newState;
            newContext.serverShader.context = newContext;
            return newContext;
        }

        return this;
    }
}

class ContextViewProvider extends LabelProvider implements ITreeContentProvider {
    Context context;

    @Override
    public void dispose() {
    }

    @Override
    public String getText(Object obj) {
        if (obj == null)
            return "null";
        if (obj instanceof Entry) {
            Entry entry = (Entry) obj;
            if (entry != null)
                return entry.name + " = " + entry.obj;
        }
        return obj.toString();
    }

    @Override
    public Image getImage(Object obj) {
        return null;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        context = (Context) newInput;
    }

    class Entry {
        String name;
        Object obj;

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
        } else if (entry.obj == context.serverState.lastSetter) {
            for (int i = 0; i < context.serverState.lastSetter.size(); i++) {
                final int key = context.serverState.lastSetter.keyAt(i);
                final Message msg = context.serverState.lastSetter.valueAt(i);
                if (msg == null)
                    children.add(Function.valueOf(key).name() + " : default");
                else
                    children.add(Function.valueOf(key).name() + " : "
                            + MessageFormatter.Format(msg));
            }
        } else if (entry.obj instanceof SparseArray) {
            SparseArray sa = (SparseArray) entry.obj;
            for (int i = 0; i < sa.size(); i++)
                children.add(new Entry(entry.name + "[" + sa.keyAt(i) + "]", sa.valueAt(i)));
        } else if (entry.obj instanceof Map) {
            Set set = ((Map) entry.obj).entrySet();
            for (Object o : set) {
                Map.Entry e = (Map.Entry) o;
                children.add(new Entry(e.getKey().toString(), e.getValue()));
            }
        } else if (entry.obj instanceof SparseIntArray) {
            SparseIntArray sa = (SparseIntArray) entry.obj;
            for (int i = 0; i < sa.size(); i++)
                children.add(entry.name + "[" + sa.keyAt(i) + "] = " + sa.valueAt(i));
        } else if (entry.obj instanceof Collection) {
            Collection collection = (Collection) entry.obj;
            for (Object o : collection)
                children.add(new Entry(entry.name, o));
        } else if (entry.obj.getClass().isArray()) {
            Object[] list = (Object[]) entry.obj;
            for (Object o : list)
                children.add(new Entry(entry.name, o));
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
        if (element.getClass().isPrimitive())
            return false;
        if (element.getClass().equals(String.class))
            return false;
        if (element instanceof Entry) {
            Entry entry = (Entry) element;
            if (entry.obj != null) {
                if (entry.obj instanceof SparseArray)
                    return ((SparseArray) entry.obj).size() > 0;
                else if (entry.obj instanceof SparseIntArray)
                    return ((SparseIntArray) entry.obj).size() > 0;
                else if (entry.obj instanceof Collection)
                    return ((Collection) entry.obj).size() > 0;
                else if (entry.obj instanceof Map)
                    return ((Map) entry.obj).size() > 0;
                else if (entry.obj.getClass().isArray())
                    return ((Object[]) entry.obj).length > 0;
                return entry.obj.getClass().getFields().length > 0;
            }
        }
        return false;
    }
}
