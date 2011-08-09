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
#include "EglDisplay.h"
#include "EglOsApi.h"
#include <GLcommon/GLutils.h>
#include <utils/threads.h>

EglDisplay::EglDisplay(EGLNativeInternalDisplayType dpy,bool isDefault) :
    m_dpy(dpy),
    m_initialized(false),
    m_configInitialized(false),
    m_isDefault(isDefault),
    m_nextEglImageId(0),
    m_globalSharedContext(NULL)
{
    m_manager[GLES_1_1] = new ObjectNameManager(&m_globalNameSpace);
    m_manager[GLES_2_0] = new ObjectNameManager(&m_globalNameSpace);
};

EglDisplay::~EglDisplay() {
    android::Mutex::Autolock mutex(m_lock);

    //
    // Destroy the global context if one was created.
    // (should be true for windows platform only)
    //
    if (m_globalSharedContext != NULL) {
        EglOS::destroyContext( m_dpy, m_globalSharedContext);
    }

    if(m_isDefault) {
        EglOS::releaseDisplay(m_dpy);
    }


    for(ConfigsList::iterator it = m_configs.begin(); it != m_configs.end(); it++) {
        EglConfig* pConfig = *it;
        if(pConfig) delete pConfig;
    }

    delete m_manager[GLES_1_1];
    delete m_manager[GLES_2_0];
    EglOS::deleteDisplay(m_dpy);
}

EGLNativeInternalDisplayType EglDisplay::nativeType(){return m_dpy;}

void EglDisplay::initialize(int renderableType) {
    android::Mutex::Autolock mutex(m_lock);
    m_initialized = true;
    initConfigurations(renderableType);
    m_configInitialized = true;
}

bool EglDisplay::isInitialize() { return m_initialized;}

void EglDisplay::terminate(){
    android::Mutex::Autolock mutex(m_lock);
     m_contexts.clear();
     m_surfaces.clear();
     m_initialized = false;
}

static bool compareEglConfigsPtrs(EglConfig* first,EglConfig* second) {
    return *first < *second ;
}

void EglDisplay::addMissingConfigs(void)
{
    m_configs.sort(compareEglConfigsPtrs);

    EGLConfig match;
    EGLNativePixelFormatType tmpfrmt = PIXEL_FORMAT_INITIALIZER;
    EglConfig dummy(5, 6, 5, 0,  // RGB_565
                    EGL_DONT_CARE,EGL_DONT_CARE,
                    16, // Depth
                    EGL_DONT_CARE,EGL_DONT_CARE,EGL_DONT_CARE,EGL_DONT_CARE,EGL_DONT_CARE,EGL_DONT_CARE,EGL_DONT_CARE,EGL_DONT_CARE,EGL_DONT_CARE,
                    EGL_DONT_CARE, EGL_DONT_CARE,EGL_DONT_CARE,EGL_DONT_CARE,EGL_DONT_CARE,EGL_DONT_CARE,tmpfrmt);

    if(!doChooseConfigs(dummy, &match, 1))
    {
        return;
    }

    const EglConfig* config = (EglConfig*)match;

    int bSize;
    config->getConfAttrib(EGL_BUFFER_SIZE,&bSize);

    if(bSize == 16)
    {
        return;
    }

    int max_config_id = 0;

    for(ConfigsList::iterator it = m_configs.begin(); it != m_configs.end() ;it++) {
        EGLint id;
        (*it)->getConfAttrib(EGL_CONFIG_ID, &id);
        if(id > max_config_id)
            max_config_id = id;
    }

    EglConfig* newConfig = new EglConfig(*config,max_config_id+1,5,6,5,0);

    m_configs.push_back(newConfig);
}

void EglDisplay::initConfigurations(int renderableType) {
    if(m_configInitialized) return;
    EglOS::queryConfigs(m_dpy,renderableType,m_configs);

    addMissingConfigs();
    m_configs.sort(compareEglConfigsPtrs);
}

EglConfig* EglDisplay::getConfig(EGLConfig conf) {
    android::Mutex::Autolock mutex(m_lock);

    for(ConfigsList::iterator it = m_configs.begin(); it != m_configs.end() ;it++) {
        if(static_cast<EGLConfig>(*it) == conf) {
            return (*it);

        }
    }
    return NULL;
}

SurfacePtr EglDisplay::getSurface(EGLSurface surface) {
    android::Mutex::Autolock mutex(m_lock);

    SurfacesHndlMap::iterator it = m_surfaces.find(reinterpret_cast<unsigned int>(surface));
    return it != m_surfaces.end() ?
                                  (*it).second :
                                   SurfacePtr(NULL);
}

ContextPtr EglDisplay::getContext(EGLContext ctx) {
    android::Mutex::Autolock mutex(m_lock);

    ContextsHndlMap::iterator it = m_contexts.find(reinterpret_cast<unsigned int>(ctx));
    return it != m_contexts.end() ?
                                  (*it).second :
                                   ContextPtr(NULL);
}

bool EglDisplay::removeSurface(EGLSurface s) {
    android::Mutex::Autolock mutex(m_lock);

    SurfacesHndlMap::iterator it = m_surfaces.find(reinterpret_cast<unsigned int>(s));
    if(it != m_surfaces.end()) {
        m_surfaces.erase(it);
        return true;
    }
    return false;
}

bool EglDisplay::removeSurface(SurfacePtr s) {
    android::Mutex::Autolock mutex(m_lock);

    SurfacesHndlMap::iterator it;
    for(it = m_surfaces.begin(); it!= m_surfaces.end();it++)
    {
        if((*it).second.Ptr() == s.Ptr()) {
            break;
        }
    }
    if(it != m_surfaces.end()) {
        m_surfaces.erase(it);
        return true;
    }
    return false;
}

bool EglDisplay::removeContext(EGLContext ctx) {
    android::Mutex::Autolock mutex(m_lock);

    ContextsHndlMap::iterator it = m_contexts.find(reinterpret_cast<unsigned int>(ctx));
    if(it != m_contexts.end()) {
        m_contexts.erase(it);
        return true;
    }
    return false;
}

bool EglDisplay::removeContext(ContextPtr ctx) {
    android::Mutex::Autolock mutex(m_lock);

    ContextsHndlMap::iterator it;
    for(it = m_contexts.begin(); it != m_contexts.end();it++) {
        if((*it).second.Ptr() == ctx.Ptr()){
            break;
        }
    }
    if(it != m_contexts.end()) {
        m_contexts.erase(it);
        return true;
    }
    return false;
}

EglConfig* EglDisplay::getConfig(EGLint id) {
    android::Mutex::Autolock mutex(m_lock);

    for(ConfigsList::iterator it = m_configs.begin(); it != m_configs.end() ;it++) {
        if((*it)->id() == id) {
            return (*it);

        }
    }
    return NULL;
}

int EglDisplay::getConfigs(EGLConfig* configs,int config_size) {
    android::Mutex::Autolock mutex(m_lock);
    int i = 0;
    for(ConfigsList::iterator it = m_configs.begin(); it != m_configs.end() && i < config_size ;i++,it++) {
        configs[i] = static_cast<EGLConfig>(*it);
    }
    return i;
}

int EglDisplay::chooseConfigs(const EglConfig& dummy,EGLConfig* configs,int config_size) {
    android::Mutex::Autolock mutex(m_lock);
    return doChooseConfigs(dummy, configs, config_size);
}

int EglDisplay::doChooseConfigs(const EglConfig& dummy,EGLConfig* configs,int config_size) {
    int added = 0;
    for(ConfigsList::iterator it = m_configs.begin(); it != m_configs.end() && (added < config_size || !configs);it++) {

       if( (*it)->choosen(dummy)){
            if(configs) {
                configs[added] = static_cast<EGLConfig>(*it);
            }
            added++;
       }
    }
    //no need to sort since the configurations are saved already in sorted maner
    return added;
}

EGLSurface EglDisplay::addSurface(SurfacePtr s ) {
    android::Mutex::Autolock mutex(m_lock);
   unsigned int hndl = s.Ptr()->getHndl();
   EGLSurface ret =reinterpret_cast<EGLSurface> (hndl);

   if(m_surfaces.find(hndl) != m_surfaces.end()) {
       return ret;
   }

   m_surfaces[hndl] = s;
   return ret;
}

EGLContext EglDisplay::addContext(ContextPtr ctx ) {
    android::Mutex::Autolock mutex(m_lock);

   unsigned int hndl = ctx.Ptr()->getHndl();
   EGLContext ret    = reinterpret_cast<EGLContext> (hndl);

   if(m_contexts.find(hndl) != m_contexts.end()) {
       return ret;
   }
   m_contexts[hndl] = ctx;
   return ret;
}


EGLImageKHR EglDisplay::addImageKHR(ImagePtr img) {
    android::Mutex::Autolock mutex(m_lock);
    do { ++m_nextEglImageId; } while(m_nextEglImageId == 0);
    img->imageId = m_nextEglImageId;
    m_eglImages[m_nextEglImageId] = img;
    return reinterpret_cast<EGLImageKHR>(m_nextEglImageId);
}

ImagePtr EglDisplay::getImage(EGLImageKHR img) {
    android::Mutex::Autolock mutex(m_lock);
    ImagesHndlMap::iterator i( m_eglImages.find((unsigned int)img) );
    return (i != m_eglImages.end()) ? (*i).second :ImagePtr(NULL);
}

bool EglDisplay:: destroyImageKHR(EGLImageKHR img) {
    android::Mutex::Autolock mutex(m_lock);
    ImagesHndlMap::iterator i( m_eglImages.find((unsigned int)img) );
    if (i != m_eglImages.end())
    {
        m_eglImages.erase(i);
        return true;
    }
    return false;
}

EGLNativeContextType EglDisplay::getGlobalSharedContext(){
    android::Mutex::Autolock mutex(m_lock);
#ifndef _WIN32
    // find an existing OpenGL context to share with, if exist
    EGLNativeContextType ret = 
        (EGLNativeContextType)m_manager[GLES_1_1]->getGlobalContext();
    if (!ret)
        ret = (EGLNativeContextType)m_manager[GLES_2_0]->getGlobalContext();
    return ret;
#else
    if (!m_globalSharedContext) {
        //
        // On windows we create a dummy context to serve as the
        // "global context" which all contexts share with.
        // This is because on windows it is not possible to share
        // with a context which is already current. This dummy context
        // will never be current to any thread so it is safe to share with.
        // Create that context using the first config
        if (m_configs.size() < 1) {
            // Should not happen! config list should be initialized at this point
            return NULL;
        }
        EglConfig *cfg = (*m_configs.begin());
        m_globalSharedContext = EglOS::createContext(m_dpy,cfg,NULL);
    }

    return m_globalSharedContext;
#endif
}
