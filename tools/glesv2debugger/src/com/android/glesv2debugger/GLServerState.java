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

class GLStencilState implements Cloneable {
    public int ref, mask;
    public GLEnum func;
    public GLEnum sf, df, dp; // operation

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }
}

public class GLServerState implements Cloneable {
    final Context context;
    public GLStencilState front = new GLStencilState(), back = new GLStencilState();
    public SparseIntArray enableDisables;

    /** integer states set via a GL function and GLEnum; keyed by GLEnum.value */
    public SparseArray<Message> integers;

    /** states set only via a GL function; keyed by Function.getNumber() */
    public SparseArray<Message> lastSetter;

    GLServerState(final Context context) {
        this.context = context;
        enableDisables = new SparseIntArray();
        enableDisables.put(GLEnum.GL_BLEND.value, 0);
        enableDisables.put(GLEnum.GL_DITHER.value, 1);
        enableDisables.put(GLEnum.GL_DEPTH_TEST.value, 0);
        enableDisables.put(GLEnum.GL_STENCIL_TEST.value, 0);
        enableDisables.put(GLEnum.GL_SCISSOR_TEST.value, 0);
        enableDisables.put(GLEnum.GL_SAMPLE_COVERAGE.value, 0);
        enableDisables.put(GLEnum.GL_SAMPLE_ALPHA_TO_COVERAGE.value, 0);
        enableDisables.put(GLEnum.GL_POLYGON_OFFSET_FILL.value, 0);
        enableDisables.put(GLEnum.GL_CULL_FACE.value, 0);
        // enableDisables.put(GLEnum.GL_TEXTURE_2D.value, 1);

        lastSetter = new SparseArray<Message>();
        lastSetter.put(Function.glBlendColor.getNumber(), null);
        // glBlendEquation overwrites glBlendEquationSeparate
        lastSetter.put(Function.glBlendEquationSeparate.getNumber(), null);
        // glBlendFunc overwrites glBlendFuncSeparate
        lastSetter.put(Function.glBlendFuncSeparate.getNumber(), null);
        lastSetter.put(Function.glClearColor.getNumber(), null);
        lastSetter.put(Function.glClearDepthf.getNumber(), null);
        lastSetter.put(Function.glClearStencil.getNumber(), null);
        lastSetter.put(Function.glColorMask.getNumber(), null);
        lastSetter.put(Function.glCullFace.getNumber(), null);
        lastSetter.put(Function.glDepthMask.getNumber(), null);
        lastSetter.put(Function.glDepthFunc.getNumber(), null);
        lastSetter.put(Function.glDepthRangef.getNumber(), null);
        lastSetter.put(Function.glFrontFace.getNumber(), null);
        lastSetter.put(Function.glLineWidth.getNumber(), null);
        lastSetter.put(Function.glPolygonOffset.getNumber(), null);
        lastSetter.put(Function.glSampleCoverage.getNumber(), null);
        lastSetter.put(Function.glScissor.getNumber(), null);
        lastSetter.put(Function.glStencilMaskSeparate.getNumber(), null);
        lastSetter.put(Function.glViewport.getNumber(), null);

        integers = new SparseArray<Message>();
        integers.put(GLEnum.GL_PACK_ALIGNMENT.value, null);
        integers.put(GLEnum.GL_UNPACK_ALIGNMENT.value, null);
    }

    /** returns true if processed */
    public boolean processMessage(final Message msg) {
        switch (msg.getFunction()) {
            case glBlendColor:
            case glBlendEquation:
            case glBlendEquationSeparate:
            case glBlendFunc:
            case glBlendFuncSeparate:
            case glClearColor:
            case glClearDepthf:
            case glClearStencil:
            case glColorMask:
            case glCullFace:
            case glDepthMask:
            case glDepthFunc:
            case glDepthRangef:
                return setter(msg);
            case glDisable:
                return enableDisable(false, msg);
            case glEnable:
                return enableDisable(true, msg);
            case glFrontFace:
            case glLineWidth:
                return setter(msg);
            case glPixelStorei:
                if (GLEnum.valueOf(msg.getArg0()) == GLEnum.GL_PACK_ALIGNMENT)
                    integers.put(msg.getArg0(), msg);
                else if (GLEnum.valueOf(msg.getArg0()) == GLEnum.GL_UNPACK_ALIGNMENT)
                    integers.put(msg.getArg0(), msg);
                else
                    assert false;
                return true;
            case glPolygonOffset:
            case glSampleCoverage:
            case glScissor:
                return setter(msg);
            case glStencilFunc: {
                Message.Builder builder = msg.toBuilder();
                builder.setArg2(msg.getArg1());
                builder.setArg1(msg.getArg0());
                builder.setArg0(GLEnum.GL_FRONT_AND_BACK.value);
                return glStencilFuncSeparate(builder.build());
            }
            case glStencilFuncSeparate:
                return glStencilFuncSeparate(msg);
            case glStencilMask:
            case glStencilMaskSeparate:
                return setter(msg);
            case glStencilOp: {
                Message.Builder builder = msg.toBuilder();
                builder.setArg3(msg.getArg2());
                builder.setArg2(msg.getArg1());
                builder.setArg1(msg.getArg0());
                builder.setArg0(GLEnum.GL_FRONT_AND_BACK.value);
                return glStencilOpSeparate(builder.build());
            }
            case glStencilOpSeparate:
                return glStencilOpSeparate(msg);
            case glViewport:
                return setter(msg);
            default:
                return false;
        }
    }

    boolean setter(final Message msg) {
        switch (msg.getFunction()) {
            case glBlendFunc:
                lastSetter.put(Function.glBlendFuncSeparate.getNumber(), msg);
                break;
            case glBlendEquation:
                lastSetter.put(Function.glBlendEquationSeparate.getNumber(), msg);
                break;
            case glStencilMask:
                lastSetter.put(Function.glStencilMaskSeparate.getNumber(), msg);
                break;
            default:
                lastSetter.put(msg.getFunction().getNumber(), msg);
                break;
        }
        return true;
    }

    boolean enableDisable(boolean enable, final Message msg) {
        int index = enableDisables.indexOfKey(msg.getArg0());
        if (index < 0) {
            System.out.print("invalid glDisable/Enable: ");
            System.out.println(MessageFormatter.format(msg, false));
            return true;
        }
        if ((enableDisables.valueAt(index) != 0) == enable)
            return true; // TODO: redundant
        enableDisables.put(msg.getArg0(), enable ? 1 : 0);
        return true;
    }

    // void StencilFuncSeparate( enum face, enum func, int ref, uint mask )
    boolean glStencilFuncSeparate(final Message msg) {
        GLEnum ff = front.func, bf = back.func;
        int fr = front.ref, br = back.ref;
        int fm = front.mask, bm = back.mask;
        final GLEnum face = GLEnum.valueOf(msg.getArg0());
        if (face == GLEnum.GL_FRONT || face == GLEnum.GL_FRONT_AND_BACK) {
            ff = GLEnum.valueOf(msg.getArg1());
            fr = msg.getArg2();
            fm = msg.getArg3();
        }
        if (face == GLEnum.GL_BACK || face == GLEnum.GL_FRONT_AND_BACK) {
            bf = GLEnum.valueOf(msg.getArg1());
            br = msg.getArg2();
            bm = msg.getArg3();
        }
        if (ff == front.func && fr == front.ref && fm == front.mask)
            if (bf == back.func && br == back.ref && bm == back.mask)
                return true; // TODO: redundant
        front.func = ff;
        front.ref = fr;
        front.mask = fm;
        back.func = bf;
        back.ref = br;
        back.mask = bm;
        return true;
    }

    // void StencilOpSeparate( enum face, enum sfail, enum dpfail, enum dppass )
    boolean glStencilOpSeparate(final Message msg) {
        GLEnum fsf = front.sf, fdf = front.df, fdp = front.dp;
        GLEnum bsf = back.sf, bdf = back.df, bdp = back.dp;
        final GLEnum face = GLEnum.valueOf(msg.getArg0());
        if (face == GLEnum.GL_FRONT || face == GLEnum.GL_FRONT_AND_BACK) {
            fsf = GLEnum.valueOf(msg.getArg1());
            fdf = GLEnum.valueOf(msg.getArg2());
            fdp = GLEnum.valueOf(msg.getArg3());
        }
        if (face == GLEnum.GL_BACK || face == GLEnum.GL_FRONT_AND_BACK) {
            bsf = GLEnum.valueOf(msg.getArg1());
            bdf = GLEnum.valueOf(msg.getArg2());
            bdp = GLEnum.valueOf(msg.getArg3());
        }
        if (fsf == front.sf && fdf == front.df && fdp == front.dp)
            if (bsf == back.sf && bdf == back.df && bdp == back.dp)
                return true; // TODO: redundant
        front.sf = fsf;
        front.df = fdf;
        front.dp = fdp;
        back.sf = bsf;
        back.df = bdf;
        back.dp = bdp;
        return true;
    }

    /** deep copy */
    @Override
    public GLServerState clone() {
        try {
            GLServerState newState = (GLServerState) super.clone();
            newState.front = (GLStencilState) front.clone();
            newState.back = (GLStencilState) back.clone();

            newState.enableDisables = new SparseIntArray(enableDisables.size());
            for (int i = 0; i < enableDisables.size(); i++)
                newState.enableDisables.append(enableDisables.keyAt(i),
                        enableDisables.valueAt(i));

            newState.integers = new SparseArray<Message>(integers.size());
            for (int i = 0; i < integers.size(); i++)
                newState.integers.append(integers.keyAt(i), integers.valueAt(i));

            newState.lastSetter = new SparseArray<Message>(lastSetter.size());
            for (int i = 0; i < lastSetter.size(); i++)
                newState.lastSetter.append(lastSetter.keyAt(i), lastSetter.valueAt(i));

            return newState;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            assert false;
            return null;
        }
    }
}
