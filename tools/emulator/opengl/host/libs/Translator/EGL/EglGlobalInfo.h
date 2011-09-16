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
#ifndef EGL_GLOBAL_INFO
#define EGL_GLOBAL_INFO

#include <list>
#include <EGL/egl.h>
#include <utils/threads.h>
#include <GLcommon/TranslatorIfaces.h>
#include "EglDisplay.h"
#include "EglConfig.h"
#include "EglContext.h"

typedef std::map<EglDisplay*,EGLNativeDisplayType>DisplaysMap;


class EglGlobalInfo {

public:
    EglDisplay* addDisplay(EGLNativeDisplayType dpy,EGLNativeInternalDisplayType idpy);
    EglDisplay* getDisplay(EGLNativeDisplayType dpy);
    EglDisplay* getDisplay(EGLDisplay dpy);
    bool removeDisplay(EGLDisplay dpy);
    EGLNativeInternalDisplayType getDefaultNativeDisplay(){ return m_default;};
    EGLNativeInternalDisplayType generateInternalDisplay(EGLNativeDisplayType dpy);

    void setIface(GLESiface* iface,GLESVersion ver) { m_gles_ifaces[ver] = iface;};
    GLESiface* getIface(GLESVersion ver){ return m_gles_ifaces[ver];}

    int  nDisplays() const { return m_displays.size();};

    void initClientExtFuncTable(GLESVersion ver);

    static EglGlobalInfo* getInstance();
    static void delInstance();

private:
    EglGlobalInfo();
    ~EglGlobalInfo(){};

    static EglGlobalInfo*          m_singleton;
    static int                     m_refCount;

    DisplaysMap                    m_displays;
    EGLNativeInternalDisplayType   m_default;
    GLESiface*                     m_gles_ifaces[MAX_GLES_VERSION];
    bool                           m_gles_extFuncs_inited[MAX_GLES_VERSION];
    android::Mutex                 m_lock;
};

#endif
