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
    public SparseArray<Message> lastSetter; // keyed by Function.getNumber()

    GLServerState(final Context context) {
        this.context = context;
        enableDisables = new SparseIntArray(9);
        enableDisables.put(GLEnum.GL_BLEND.value, 0);
        enableDisables.put(GLEnum.GL_DITHER.value, 1);
        enableDisables.put(GLEnum.GL_DEPTH_TEST.value, 0);
        enableDisables.put(GLEnum.GL_STENCIL_TEST.value, 0);
        enableDisables.put(GLEnum.GL_SCISSOR_TEST.value, 0);
        enableDisables.put(GLEnum.GL_SAMPLE_COVERAGE.value, 0);
        enableDisables.put(GLEnum.GL_SAMPLE_ALPHA_TO_COVERAGE.value, 0);
        enableDisables.put(GLEnum.GL_POLYGON_OFFSET_FILL.value, 0);
        enableDisables.put(GLEnum.GL_CULL_FACE.value, 0);

        lastSetter = new SparseArray<Message>();
        lastSetter.put(Function.glBlendColor.getNumber(), null);
        // glBlendEquation overwrites glBlendEquationSeparate
        lastSetter.put(Function.glBlendEquationSeparate.getNumber(), null);
        // glBlendFunc overwrites glBlendFuncSeparate
        lastSetter.put(Function.glBlendFuncSeparate.getNumber(), null);
        lastSetter.put(Function.glColorMask.getNumber(), null);
        lastSetter.put(Function.glDepthMask.getNumber(), null);
        lastSetter.put(Function.glDepthFunc.getNumber(), null);
        lastSetter.put(Function.glScissor.getNumber(), null);
        lastSetter.put(Function.glStencilMaskSeparate.getNumber(), null);
    }

    // returns instance if processed (returns new instance if changed)
    public GLServerState ProcessMessage(final Message msg) {
        switch (msg.getFunction()) {
            case glBlendColor:
                return Setter(msg);
            case glBlendEquation:
                return Setter(msg);
            case glBlendEquationSeparate:
                return Setter(msg);
            case glBlendFunc:
                return Setter(msg);
            case glBlendFuncSeparate:
                return Setter(msg);
            case glColorMask:
                return Setter(msg);
            case glDepthMask:
                return Setter(msg);
            case glDepthFunc:
                return Setter(msg);
            case glDisable:
                return EnableDisable(false, msg);
            case glEnable:
                return EnableDisable(true, msg);
            case glScissor:
                return Setter(msg);
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
                return Setter(msg);
            case glStencilMaskSeparate:
                return Setter(msg);
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
            default:
                return null;
        }
    }

    GLServerState Setter(final Message msg) {
        GLServerState newState = (GLServerState) this.clone();
        // TODO: compare for change
        switch (msg.getFunction()) {
            case glBlendFunc:
                newState.lastSetter.put(Function.glBlendFuncSeparate.getNumber(), msg);
                break;
            case glBlendEquation:
                newState.lastSetter.put(Function.glBlendEquationSeparate.getNumber(), msg);
                break;
            case glStencilMask:
                newState.lastSetter.put(Function.glStencilMaskSeparate.getNumber(), msg);
                break;
            default:
                newState.lastSetter.put(msg.getFunction().getNumber(), msg);
                break;
        }
        return newState;
    }

    GLServerState EnableDisable(boolean enable, final Message msg) {
        int index = enableDisables.indexOfKey(msg.getArg0());
        assert index >= 0;
        if ((enableDisables.valueAt(index) != 0) == enable)
            return this;
        GLServerState newState0 = (GLServerState) this.clone();
        newState0.enableDisables.put(msg.getArg0(), enable ? 1 : 0);
        return newState0;
    }

    // void StencilFuncSeparate( enum face, enum func, int ref, uint mask )
    GLServerState glStencilFuncSeparate(final Message msg) {
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
                return this;
        GLServerState newState = (GLServerState) this.clone();
        newState.front.func = ff;
        newState.front.ref = fr;
        newState.front.mask = fm;
        newState.back.func = bf;
        newState.back.ref = br;
        newState.back.mask = bm;
        return newState;
    }

    // void StencilOpSeparate( enum face, enum sfail, enum dpfail, enum dppass )
    GLServerState glStencilOpSeparate(final Message msg) {
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
                return this;
        GLServerState newState = (GLServerState) this.clone();
        newState.front.sf = fsf;
        newState.front.df = fdf;
        newState.front.dp = fdp;
        newState.back.sf = bsf;
        newState.back.df = bdf;
        newState.back.dp = bdp;
        return newState;
    }

    @Override
    public Object clone() {
        try {
            GLServerState newState = (GLServerState) super.clone();
            newState.front = (GLStencilState) front.clone();
            newState.back = (GLStencilState) back.clone();

            newState.enableDisables = new SparseIntArray(enableDisables.size());
            for (int i = 0; i < enableDisables.size(); i++) {
                final int key = enableDisables.keyAt(i);
                newState.enableDisables.append(key, enableDisables.valueAt(i));
            }

            newState.lastSetter = new SparseArray<Message>(lastSetter.size());
            for (int i = 0; i < lastSetter.size(); i++) {
                final int key = lastSetter.keyAt(i);
                newState.lastSetter.append(key, lastSetter.valueAt(i));
            }

            return newState;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
