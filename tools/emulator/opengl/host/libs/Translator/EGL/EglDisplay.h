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
#ifndef EGL_DISPLAY_H
#define EGL_DISPLAY_H

#include <list>
#include <map>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <utils/threads.h>
#include <GLcommon/SmartPtr.h>

#include "EglConfig.h"
#include "EglContext.h"
#include "EglSurface.h"
#include "EglWindowSurface.h"



typedef  std::list<EglConfig*>  ConfigsList;
typedef  std::map< unsigned int, ContextPtr>     ContextsHndlMap;
typedef  std::map< unsigned int, SurfacePtr>     SurfacesHndlMap;

class EglDisplay {
public:


    EglDisplay(EGLNativeInternalDisplayType dpy,bool isDefault = true);
    EGLNativeInternalDisplayType nativeType();
    int nConfigs(){ return m_configs.size();}
    int getConfigs(EGLConfig* configs,int config_size);
    int chooseConfigs(const EglConfig& dummy,EGLConfig* configs,int config_size);
    EglConfig* getConfig(EGLConfig conf);
    EglConfig* getConfig(EGLint id );

    EGLSurface addSurface(SurfacePtr s );
    SurfacePtr getSurface(EGLSurface surface);
    bool removeSurface(EGLSurface s);
    bool removeSurface(SurfacePtr s);

    EGLContext addContext(ContextPtr ctx );
    ContextPtr getContext(EGLContext ctx);
    bool removeContext(EGLContext ctx);
    bool removeContext(ContextPtr ctx);
    ObjectNameManager* getManager(GLESVersion ver){ return m_manager[ver];}

    ~EglDisplay();
    void initialize(int renderableType);
    void terminate();
    bool isInitialize();

    ImagePtr getImage(EGLImageKHR img);
    EGLImageKHR addImageKHR(ImagePtr);
    bool destroyImageKHR(EGLImageKHR img);
    EGLNativeContextType getGlobalSharedContext();

private:
   int doChooseConfigs(const EglConfig& dummy,EGLConfig* configs,int config_size);
   void addMissingConfigs(void);
   void initConfigurations(int renderableType);

   EGLNativeInternalDisplayType   m_dpy;
   bool                           m_initialized;
   bool                           m_configInitialized;
   bool                           m_isDefault;
   ConfigsList                    m_configs;
   ContextsHndlMap                m_contexts;
   SurfacesHndlMap                m_surfaces;
   GlobalNameSpace                m_globalNameSpace;
   ObjectNameManager              *m_manager[MAX_GLES_VERSION];
   android::Mutex                 m_lock;
   ImagesHndlMap                  m_eglImages;
   unsigned int                   m_nextEglImageId;
   EGLNativeContextType           m_globalSharedContext;
};

#endif


