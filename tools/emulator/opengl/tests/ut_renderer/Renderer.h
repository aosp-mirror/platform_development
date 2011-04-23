/*
* Copyright (C) 2011 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#ifndef _RENDERER_H_
#define _RENDERER_H_
#include <map>
#include "RendererSurface.h"
#include "RendererContext.h"
#include "NativeWindowing.h"
#include <utils/threads.h>

class RenderingThread;

class Renderer {
public:

    class ClientHandle {
    public:
        unsigned int pid;
        unsigned int handle;
        ClientHandle(unsigned int _pid, unsigned int _handle) : pid(_pid), handle(_handle) {}

        bool operator< (const ClientHandle & p) const {
            bool val = (pid == p.pid) ? handle < p.handle : pid < p.pid;
            return val;
        }
    };

    static Renderer *instance();
    int createSurface(RenderingThread *thread, const ClientHandle & handle);
    int destroySurface(RenderingThread *thread, const ClientHandle &handle);
    int createContext(RenderingThread *thread, const ClientHandle & ctx, const ClientHandle shareCtx, int version);
    int destroyContext(RenderingThread *thread,const ClientHandle & ctx);
    int makeCurrent(RenderingThread *thread,
                    const ClientHandle & drawSurface, const ClientHandle & readSurface, const ClientHandle & ctx);
    int swapBuffers(RenderingThread *thread, const ClientHandle & surface);

private:
    typedef std::map<ClientHandle, RendererSurface *> SurfaceMap;
    typedef std::map<ClientHandle, RendererContext *> ContextMap;
    static Renderer *m_instance;
    Renderer();
    SurfaceMap m_surfaces;
    ContextMap m_ctxs;
    NativeWindowing *m_nw;
    EGLDisplay m_dpy;

    android::Mutex m_mutex; // single global mutex for the renderer class;
};
#endif
