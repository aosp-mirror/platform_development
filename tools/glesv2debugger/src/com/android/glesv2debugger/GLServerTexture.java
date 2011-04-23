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
import com.android.sdklib.util.SparseArray;

import java.nio.ByteBuffer;
import java.util.ArrayList;

class GLTexture implements Cloneable {
    public final int name;
    public final GLEnum target;
    public ArrayList<Message> contentChanges = new ArrayList<Message>();
    public GLEnum wrapS = GLEnum.GL_REPEAT, wrapT = GLEnum.GL_REPEAT;
    public GLEnum min = GLEnum.GL_NEAREST_MIPMAP_LINEAR;
    public GLEnum mag = GLEnum.GL_LINEAR;
    public GLEnum format;
    public int width, height;

    GLTexture(final int name, final GLEnum target) {
        this.name = name;
        this.target = target;
    }

    @Override
    public GLTexture clone() {
        try {
            GLTexture copy = (GLTexture) super.clone();
            copy.contentChanges = (ArrayList<Message>) contentChanges.clone();
            return copy;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            assert false;
            return null;
        }
    }

    boolean processMessage(final Message msg) {
        switch (msg.getFunction()) {
            case glCompressedTexImage2D:
            case glCopyTexImage2D:
            case glTexImage2D:
                if (msg.getArg1() == 0) { // level 0
                    format = GLEnum.valueOf(msg.getArg2());
                    width = msg.getArg3();
                    height = msg.getArg4();
                }
                //$FALL-THROUGH$
            case glCompressedTexSubImage2D:
            case glCopyTexSubImage2D:
            case glTexSubImage2D:
            case glGenerateMipmap:
                contentChanges.add(msg);
                break;
            default:
                assert false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("%s %s %d*%d %d change(s)", target, format, width, height,
                contentChanges.size());
    }
}

public class GLServerTexture implements Cloneable {
    Context context;

    public GLEnum activeTexture = GLEnum.GL_TEXTURE0;
    public int[] tmu2D;
    public int[] tmuCube;
    public SparseArray<GLTexture> textures = new SparseArray<GLTexture>();
    public GLTexture tex2D = null, texCube = null;

    GLServerTexture(final Context context, final int MAX_COMBINED_TEXTURE_IMAGE_UNITS) {
        this.context = context;
        textures.append(0, null);
        tmu2D = new int[MAX_COMBINED_TEXTURE_IMAGE_UNITS];
        tmuCube = new int[MAX_COMBINED_TEXTURE_IMAGE_UNITS];
    }

    public GLServerTexture clone(final Context copyContext) {
        try {
            GLServerTexture copy = (GLServerTexture) super.clone();
            copy.context = copyContext;

            copy.tmu2D = tmu2D.clone();
            copy.tmuCube = tmuCube.clone();

            copy.textures = new SparseArray<GLTexture>(textures.size());
            for (int i = 0; i < textures.size(); i++)
                if (textures.valueAt(i) != null)
                    copy.textures.append(textures.keyAt(i), textures.valueAt(i).clone());
                else
                    copy.textures.append(textures.keyAt(i), null);

            if (tex2D != null)
                copy.tex2D = copy.textures.get(tex2D.name);
            if (texCube != null)
                copy.texCube = copy.textures.get(texCube.name);

            return copy;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            assert false;
            return null;
        }
    }

    public boolean processMessage(final Message msg) {
        switch (msg.getFunction()) {
            case glActiveTexture:
                activeTexture = GLEnum.valueOf(msg.getArg0());
                return true;
            case glBindTexture:
                return bindTexture(msg.getArg0(), msg.getArg1());
            case glCompressedTexImage2D:
            case glCompressedTexSubImage2D:
            case glCopyTexImage2D:
            case glCopyTexSubImage2D:
            case glTexImage2D:
            case glTexSubImage2D:
                switch (GLEnum.valueOf(msg.getArg0())) {
                    case GL_TEXTURE_2D:
                        if (tex2D != null)
                            return tex2D.processMessage(msg);
                        return true;
                    case GL_TEXTURE_CUBE_MAP_POSITIVE_X:
                    case GL_TEXTURE_CUBE_MAP_NEGATIVE_X:
                    case GL_TEXTURE_CUBE_MAP_POSITIVE_Y:
                    case GL_TEXTURE_CUBE_MAP_NEGATIVE_Y:
                    case GL_TEXTURE_CUBE_MAP_POSITIVE_Z:
                    case GL_TEXTURE_CUBE_MAP_NEGATIVE_Z:
                        if (texCube != null)
                            return texCube.processMessage(msg);
                        return true;
                    default:
                        return true;
                }
            case glDeleteTextures: {
                final ByteBuffer names = msg.getData().asReadOnlyByteBuffer();
                names.order(SampleView.targetByteOrder);
                for (int i = 0; i < msg.getArg0(); i++) {
                    final int name = names.getInt();
                    if (tex2D != null && tex2D.name == name)
                        bindTexture(GLEnum.GL_TEXTURE_2D.value, 0);
                    if (texCube != null && texCube.name == name)
                        bindTexture(GLEnum.GL_TEXTURE_CUBE_MAP.value, 0);
                    if (name != 0)
                        textures.remove(name);
                }
                return true;
            }
            case glGenerateMipmap:
                if (GLEnum.valueOf(msg.getArg0()) == GLEnum.GL_TEXTURE_2D && tex2D != null)
                    return tex2D.processMessage(msg);
                else if (GLEnum.valueOf(msg.getArg0()) == GLEnum.GL_TEXTURE_CUBE_MAP
                        && texCube != null)
                    return texCube.processMessage(msg);
                return true;
            case glTexParameteri:
                return texParameter(msg.getArg0(), msg.getArg1(), msg.getArg2());
            case glTexParameterf:
                return texParameter(msg.getArg0(), msg.getArg1(),
                        (int) Float.intBitsToFloat(msg.getArg2()));
            default:
                return false;
        }
    }

    boolean bindTexture(final int target, final int name) {
        final int index = activeTexture.value - GLEnum.GL_TEXTURE0.value;
        if (GLEnum.valueOf(target) == GLEnum.GL_TEXTURE_2D) {
            tex2D = textures.get(name);
            if (name != 0 && tex2D == null)
                textures.put(name, tex2D = new GLTexture(name,
                        GLEnum.GL_TEXTURE_2D));
            if (index >= 0 && index < tmu2D.length)
                tmu2D[index] = name;
        } else if (GLEnum.valueOf(target) == GLEnum.GL_TEXTURE_CUBE_MAP) {
            texCube = textures.get(name);
            if (name != 0 && texCube == null)
                textures.put(name, texCube = new GLTexture(name,
                        GLEnum.GL_TEXTURE_CUBE_MAP));
            if (index >= 0 && index < tmu2D.length)
                tmu2D[index] = name;
        } else
            assert false;
        return true;
    }

    boolean texParameter(final int target, final int pname, final int param) {
        GLTexture tex = null;
        if (GLEnum.valueOf(target) == GLEnum.GL_TEXTURE_2D)
            tex = tex2D;
        else if (GLEnum.valueOf(target) == GLEnum.GL_TEXTURE_CUBE_MAP)
            tex = texCube;
        if (tex == null)
            return true;
        final GLEnum p = GLEnum.valueOf(param);
        switch (GLEnum.valueOf(pname)) {
            case GL_TEXTURE_WRAP_S:
                tex.wrapS = p;
                return true;
            case GL_TEXTURE_WRAP_T:
                tex.wrapT = p;
                return true;
            case GL_TEXTURE_MIN_FILTER:
                tex.min = p;
                return true;
            case GL_TEXTURE_MAG_FILTER:
                tex.mag = p;
                return true;
            default:
                return true;
        }
    }
}
