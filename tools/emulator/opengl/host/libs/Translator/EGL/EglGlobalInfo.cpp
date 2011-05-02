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
#include "EglGlobalInfo.h"
#include "EglOsApi.h"
#include <string.h>

int EglGlobalInfo::m_refCount = 0;
EglGlobalInfo* EglGlobalInfo::m_singleton = NULL;


EglGlobalInfo::EglGlobalInfo(){
    m_default = EglOS::getDefaultDisplay();
    memset(m_gles_ifaces,0,sizeof(m_gles_ifaces));
}

EglGlobalInfo* EglGlobalInfo::getInstance() {
    if(!m_singleton) {
        m_singleton = new EglGlobalInfo();
        m_refCount = 0;
    }
    m_refCount++;
    return m_singleton;
}

void EglGlobalInfo::delInstance() {
    m_refCount--;
    if(m_refCount <= 0 && m_singleton) {
        delete m_singleton;
        m_singleton = NULL;
    }

}

EglDisplay* EglGlobalInfo::addDisplay(EGLNativeDisplayType dpy) {
    //search if it is not already exists
    android::Mutex::Autolock mutex(m_lock);
    for(DisplaysList::iterator it = m_displays.begin(); it != m_displays.end() ;it++) {
        if((*it)->nativeType() == dpy) return (*it);
    }

    EglDisplay* p_dpy = new EglDisplay(dpy);
    if(p_dpy) {
        m_displays.push_front(p_dpy);
        return p_dpy;
    }
    return NULL;
}

bool  EglGlobalInfo::removeDisplay(EGLDisplay dpy) {

    android::Mutex::Autolock mutex(m_lock);
    for(DisplaysList::iterator it = m_displays.begin(); it != m_displays.end() ;it++) {
        if(static_cast<EGLDisplay>(*it) == dpy) {
            delete (*it);
            m_displays.remove(*it);
            return true;
        }
    }
    return false;
}

EglDisplay* EglGlobalInfo::getDisplay(EGLNativeDisplayType dpy) {
    android::Mutex::Autolock mutex(m_lock);
    for(DisplaysList::iterator it = m_displays.begin(); it != m_displays.end() ;it++) {
        if((*it)->nativeType() == dpy) return (*it);
    }
    return NULL;
}

EglDisplay* EglGlobalInfo::getDisplay(EGLDisplay dpy) {
    android::Mutex::Autolock mutex(m_lock);
    for(DisplaysList::iterator it = m_displays.begin(); it != m_displays.end() ;it++) {
        if(static_cast<EGLDisplay>(*it) == dpy) return (*it);
    }
    return NULL;
}
