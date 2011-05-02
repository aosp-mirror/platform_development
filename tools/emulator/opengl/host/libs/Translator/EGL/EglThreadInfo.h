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
#ifndef EGL_THREAD_INFO_H
#define EGL_THREAD_INFO_H

#include <EGL/egl.h>
#include "EglDisplay.h"
#include "EglContext.h"
#include "EglSurface.h"
#include "EglPbufferSurface.h"

class EglThreadInfo {
public:

    EglThreadInfo();
    void       setError(EGLint err) { m_err = err;}
    EGLint     getError(){ return m_err;}
    void       destroyContextIfNotCurrent(ContextPtr context );
    void       setApi(EGLenum api){m_api = api;}
    EGLenum    getApi(){return m_api;}

    static EglThreadInfo*  get(void) __attribute__((const));

private:
    EglDisplay*     m_currentDisplay;
    EGLint          m_err;
    EGLenum         m_api;
};

#endif
