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
#ifdef _WIN32
#undef EGLAPI
#define EGLAPI __declspec(dllexport)
#endif

#include <EGL/egl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <stdio.h>
#include <GLcommon/ThreadInfo.h>
#include <GLcommon/TranslatorIfaces.h>
#include <OpenglOsUtils/osDynLibrary.h>

#include "EglWindowSurface.h"
#include "EglPbufferSurface.h"
#include "EglPixmapSurface.h"
#include "EglGlobalInfo.h"
#include "EglThreadInfo.h"
#include "EglValidate.h"
#include "EglDisplay.h"
#include "EglContext.h"
#include "EglConfig.h"
#include "EglOsApi.h"

#define MAJOR          1
#define MINOR          4

//declarations

EglImage *attachEGLImage(unsigned int imageId);
void detachEGLImage(unsigned int imageId);

#define tls_thread  EglThreadInfo::get()

EglGlobalInfo* g_eglInfo = EglGlobalInfo::getInstance();

static EGLiface            s_eglIface = {
    getThreadInfo    : getThreadInfo,      // implemented in ThreadInfo.cpp
    eglAttachEGLImage:attachEGLImage,
    eglDetachEGLImage:detachEGLImage
};

/*****************************************  supported extentions  ***********************************************************************/

//extentions
#define EGL_EXTENTIONS 2

//decleration
EGLImageKHR eglCreateImageKHR(EGLDisplay display, EGLContext context, EGLenum target, EGLClientBuffer buffer, const EGLint *attrib_list);
EGLBoolean eglDestroyImageKHR(EGLDisplay display, EGLImageKHR image);

// extentions descriptors
static ExtentionDescriptor s_eglExtentions[] = {
                                                   {"eglCreateImageKHR" ,(__eglMustCastToProperFunctionPointerType)eglCreateImageKHR},
                                                   {"eglDestroyImageKHR",(__eglMustCastToProperFunctionPointerType)eglDestroyImageKHR}
                                               };

/****************************************************************************************************************************************/
//macros for accessing global egl info & tls objects

#define CURRENT_THREAD() do {} while (0);

#define RETURN_ERROR(ret,err)                                \
        CURRENT_THREAD()                                     \
        if(tls_thread->getError() == EGL_SUCCESS) {          \
          tls_thread->setError(err);                         \
        }                                                    \
        return ret;

#define VALIDATE_DISPLAY_RETURN(EGLDisplay,ret)              \
        EglDisplay* dpy = g_eglInfo->getDisplay(EGLDisplay); \
        if(!dpy){                                            \
            RETURN_ERROR(ret,EGL_BAD_DISPLAY);               \
        }                                                    \
        if(!dpy->isInitialize()) {                           \
            RETURN_ERROR(ret,EGL_NOT_INITIALIZED);           \
        }

#define VALIDATE_CONFIG_RETURN(EGLConfig,ret)                \
        EglConfig* cfg = dpy->getConfig(EGLConfig);          \
        if(!cfg) {                                           \
            RETURN_ERROR(ret,EGL_BAD_CONFIG);                \
        }

#define VALIDATE_SURFACE_RETURN(EGLSurface,ret,varName)      \
        SurfacePtr varName = dpy->getSurface(EGLSurface);    \
        if(!varName.Ptr()) {                                 \
            RETURN_ERROR(ret,EGL_BAD_SURFACE);               \
        }

#define VALIDATE_CONTEXT_RETURN(EGLContext,ret)              \
        ContextPtr ctx = dpy->getContext(EGLContext);        \
        if(!ctx.Ptr()) {                                     \
            RETURN_ERROR(ret,EGL_BAD_CONTEXT);               \
        }


#define VALIDATE_DISPLAY(EGLDisplay) \
        VALIDATE_DISPLAY_RETURN(EGLDisplay,EGL_FALSE)

#define VALIDATE_CONFIG(EGLConfig)   \
        VALIDATE_CONFIG_RETURN(EGLConfig,EGL_FALSE)

#define VALIDATE_SURFACE(EGLSurface,varName) \
        VALIDATE_SURFACE_RETURN(EGLSurface,EGL_FALSE,varName)

#define VALIDATE_CONTEXT(EGLContext) \
        VALIDATE_CONTEXT_RETURN(EGLContext,EGL_FALSE)


EGLAPI EGLint EGLAPIENTRY eglGetError(void) {
    CURRENT_THREAD();
    EGLint err = tls_thread->getError();
    tls_thread->setError(EGL_SUCCESS);
    return err;
}

EGLAPI EGLDisplay EGLAPIENTRY eglGetDisplay(EGLNativeDisplayType display_id) {
    EglDisplay* dpy = NULL;

    if( display_id == EGL_DEFAULT_DISPLAY) {
        display_id = g_eglInfo->getDefaultNativeDisplay();
    }

    if ((dpy = g_eglInfo->getDisplay(display_id))) {
        return dpy;
    } else {
        dpy = g_eglInfo->addDisplay(display_id);
        if(dpy) return dpy;
        return EGL_NO_DISPLAY;
    }
}


#define TRANSLATOR_GETIFACE_NAME "__translator_getIfaces"

static __translator_getGLESIfaceFunc loadIfaces(const char* libName){
    osUtils::dynLibrary* libGLES = osUtils::dynLibrary::open(libName);

    if(!libGLES) return NULL;
    __translator_getGLESIfaceFunc func =  (__translator_getGLESIfaceFunc)libGLES->findSymbol(TRANSLATOR_GETIFACE_NAME);
    if(!func) return NULL;
    return func;
}

#ifdef _WIN32
#define LIB_GLES_CM_NAME "libGLES_CM_translator"
#define LIB_GLES_V2_NAME "libGLES_V2_translator"
#elif __linux__
#define LIB_GLES_CM_NAME "libGLES_CM_translator.so"
#define LIB_GLES_V2_NAME "libGLES_V2_translator.so"
#else
#define LIB_GLES_CM_NAME "libGLES_CM_translator"
#define LIB_GLES_V2_NAME "libGLES_V2_translator"
#endif

EGLAPI EGLBoolean EGLAPIENTRY eglInitialize(EGLDisplay display, EGLint *major, EGLint *minor) {
    EglDisplay* dpy = g_eglInfo->getDisplay(display);
    if(!dpy) {
         RETURN_ERROR(EGL_FALSE,EGL_BAD_DISPLAY);
    }

    if(major) *major = MAJOR;
    if(minor) *minor = MINOR;

    __translator_getGLESIfaceFunc func  = NULL;
    int renderableType = EGL_OPENGL_ES_BIT;

    if(!g_eglInfo->getIface(GLES_1_1)) {
        func  = loadIfaces(LIB_GLES_CM_NAME);
        if(func){
            g_eglInfo->setIface(func(&s_eglIface),GLES_1_1);
        } else {
           fprintf(stderr,"could not find ifaces for GLES CM 1.1\n");
           return EGL_FALSE;
        }
    }
    if(!g_eglInfo->getIface(GLES_2_0)) {
        func  = loadIfaces(LIB_GLES_V2_NAME);
        if(func){
            renderableType |= EGL_OPENGL_ES2_BIT;
            g_eglInfo->setIface(func(&s_eglIface),GLES_2_0);
        } else {
           fprintf(stderr,"could not find ifaces for GLES 2.0\n");
        }
    }
    dpy->initialize(renderableType);
    return EGL_TRUE;
}

EGLAPI EGLBoolean EGLAPIENTRY eglTerminate(EGLDisplay display) {
    VALIDATE_DISPLAY(display);
    dpy->terminate();
    return EGL_TRUE;
}

EGLAPI const char * EGLAPIENTRY eglQueryString(EGLDisplay display, EGLint name) {
    VALIDATE_DISPLAY(display);
    static const char* vendor     = "Google";
    static const char* version    = "1.4";
    static const char* extensions = "EGL_KHR_image_base EGL_KHR_gl_texture_2d_image";
    if(!EglValidate::stringName(name)) {
        RETURN_ERROR(NULL,EGL_BAD_PARAMETER);
    }
    switch(name) {
    case EGL_VENDOR:
        return vendor;
    case EGL_VERSION:
        return version;
    case EGL_EXTENSIONS:
        return extensions;
    }
    return NULL;
}

EGLAPI EGLBoolean EGLAPIENTRY eglGetConfigs(EGLDisplay display, EGLConfig *configs,
             EGLint config_size, EGLint *num_config) {
    VALIDATE_DISPLAY(display);
    if(!num_config) {
        RETURN_ERROR(EGL_FALSE,EGL_BAD_PARAMETER);
    }

    if(configs == NULL) {
        *num_config = dpy->nConfigs();
    } else {
        *num_config = dpy->getConfigs(configs,config_size);
    }

    return EGL_TRUE;
}

EGLAPI EGLBoolean EGLAPIENTRY eglChooseConfig(EGLDisplay display, const EGLint *attrib_list,
               EGLConfig *configs, EGLint config_size,
               EGLint *num_config) {
    VALIDATE_DISPLAY(display);
    if(!num_config) {
         RETURN_ERROR(EGL_FALSE,EGL_BAD_PARAMETER);
    }

        //selection defaults
        EGLint      surface_type       = EGL_WINDOW_BIT;
        EGLint      renderable_type    = EGL_OPENGL_ES_BIT;
        EGLBoolean  bind_to_tex_rgb    = EGL_DONT_CARE;
        EGLBoolean  bind_to_tex_rgba   = EGL_DONT_CARE;
        EGLenum     caveat             = EGL_DONT_CARE;
        EGLint      config_id          = EGL_DONT_CARE;
        EGLBoolean  native_renderable  = EGL_DONT_CARE;
        EGLint      native_visual_type = EGL_DONT_CARE;
        EGLint      max_swap_interval  = EGL_DONT_CARE;
        EGLint      min_swap_interval  = EGL_DONT_CARE;
        EGLint      trans_red_val      = EGL_DONT_CARE;
        EGLint      trans_green_val    = EGL_DONT_CARE;
        EGLint      trans_blue_val     = EGL_DONT_CARE;
        EGLenum     transparent_type   = EGL_NONE;
        EGLint      buffer_size        = 0;
        EGLint      red_size           = 0;
        EGLint      green_size         = 0;
        EGLint      blue_size          = 0;
        EGLint      alpha_size         = 0;
        EGLint      depth_size         = 0;
        EGLint      frame_buffer_level = 0;
        EGLint      sample_buffers_num = 0;
        EGLint      samples_per_pixel  = 0;
        EGLint      stencil_size       = 0;

    if(!EglValidate::noAttribs(attrib_list)) { //there are attribs
        int i = 0 ;
        bool hasConfigId = false;
        while(attrib_list[i] != EGL_NONE && !hasConfigId) {
            switch(attrib_list[i]) {
            case EGL_MAX_PBUFFER_WIDTH:
            case EGL_MAX_PBUFFER_HEIGHT:
            case EGL_MAX_PBUFFER_PIXELS:
            case EGL_NATIVE_VISUAL_ID:
                continue; //we dont care from those selection crateria
            case EGL_LEVEL:
                if(attrib_list[i+1] == EGL_DONT_CARE) {
                    RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
                }
                frame_buffer_level = attrib_list[i+1];
                break;
            case EGL_BUFFER_SIZE:
                if(attrib_list[i+1] < 0) {
                    RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
                }
                buffer_size = attrib_list[i+1];
                break;
            case EGL_RED_SIZE:
                if(attrib_list[i+1] < 0) {
                     RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
                }
                red_size = attrib_list[i+1];
                break;
            case EGL_GREEN_SIZE:
                if(attrib_list[i+1] < 0) {
                    RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
                }
                green_size = attrib_list[i+1];
                break;
            case EGL_BLUE_SIZE:
                if(attrib_list[i+1] < 0) {
                    RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
                }
                blue_size = attrib_list[i+1];
                break;
            case EGL_ALPHA_SIZE:
                if(attrib_list[i+1] < 0) {
                    RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
                }
                alpha_size = attrib_list[i+1];
                break;
            case EGL_BIND_TO_TEXTURE_RGB:
                bind_to_tex_rgb = attrib_list[i+1];
                break;
            case EGL_BIND_TO_TEXTURE_RGBA:
                bind_to_tex_rgba = attrib_list[i+1];
                break;
            case EGL_CONFIG_CAVEAT:
                if(attrib_list[i+1] != EGL_NONE && attrib_list[i+1] != EGL_SLOW_CONFIG && attrib_list[i+1] != EGL_NON_CONFORMANT_CONFIG) {
                    RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
                }
                caveat = attrib_list[i+1];
                break;
            case EGL_CONFIG_ID:
                if(attrib_list[i+1] < 0) {
                    RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
                }
                config_id = attrib_list[i+1];
                hasConfigId = true;
                break;
            case EGL_DEPTH_SIZE:
                if(attrib_list[i+1] < 0) {
                    RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
                }
                depth_size = attrib_list[i+1];
                break;
            case EGL_MAX_SWAP_INTERVAL:
                if(attrib_list[i+1] < 0) {
                    RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
                }
                max_swap_interval = attrib_list[i+1];
                break;
            case EGL_MIN_SWAP_INTERVAL:
                if(attrib_list[i+1] < 0) {
                    RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
                }
                min_swap_interval = attrib_list[i+1];
                break;
            case EGL_NATIVE_RENDERABLE:
                native_renderable = attrib_list[i+1];
                break;
            case EGL_RENDERABLE_TYPE:
                renderable_type = attrib_list[i+1];
                break;
            case EGL_NATIVE_VISUAL_TYPE:
                native_visual_type = attrib_list[i+1];
                break;
                if(attrib_list[i+1] < 0 || attrib_list[i+1] > 1 ) {
                    RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
                }
            case EGL_SAMPLE_BUFFERS:
                sample_buffers_num = attrib_list[i+1];
                break;
                if(attrib_list[i+1] < 0) {
                    RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
                }
            case EGL_SAMPLES:
                if(attrib_list[i+1] < 0) {
                    RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
                }
                samples_per_pixel = attrib_list[i+1];
                break;
            case EGL_STENCIL_SIZE:
                if(attrib_list[i+1] < 0) {
                    RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
                }
                stencil_size = attrib_list[i+1];
                break;
            case EGL_SURFACE_TYPE:
                surface_type = attrib_list[i+1];
                break;
            case EGL_TRANSPARENT_TYPE:
                if(attrib_list[i+1] != EGL_NONE && attrib_list[i+1] != EGL_TRANSPARENT_RGB ) {
                    RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
                }
                transparent_type = attrib_list[i+1];
                break;
            case EGL_TRANSPARENT_RED_VALUE:
                trans_red_val = attrib_list[i+1];
                break;
            case EGL_TRANSPARENT_GREEN_VALUE:
                trans_green_val = attrib_list[i+1];
                break;
            case EGL_TRANSPARENT_BLUE_VALUE:
                trans_blue_val = attrib_list[i+1];
                break;
            default:
                RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
            }
            i+=2;
        }
        if(hasConfigId) {
            EglConfig* pConfig = dpy->getConfig(config_id);
            if(pConfig) {
                if(configs) {
                    configs[0]  = static_cast<EGLConfig>(pConfig);
                }
                *num_config = 1;
                return EGL_TRUE;
            } else {
                RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
            }
        }
    }
    EGLNativePixelFormatType tmpfrmt = PIXEL_FORMAT_INITIALIZER;
    EglConfig dummy(red_size,green_size,blue_size,alpha_size,caveat,config_id,depth_size,
                    frame_buffer_level,0,0,0,native_renderable,renderable_type,0,native_visual_type,
                    samples_per_pixel,stencil_size,surface_type,transparent_type,
                    trans_red_val,trans_green_val,trans_blue_val,tmpfrmt);

    *num_config = dpy->chooseConfigs(dummy,configs,config_size);


    return EGL_TRUE;
}

EGLAPI EGLBoolean EGLAPIENTRY eglGetConfigAttrib(EGLDisplay display, EGLConfig config,
                  EGLint attribute, EGLint *value) {
    VALIDATE_DISPLAY(display);
    VALIDATE_CONFIG(config);
    if(!EglValidate::confAttrib(attribute)){
         RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
    }
    return cfg->getConfAttrib(attribute,value)? EGL_TRUE:EGL_FALSE;
}

EGLAPI EGLSurface EGLAPIENTRY eglCreateWindowSurface(EGLDisplay display, EGLConfig config,
                  EGLNativeWindowType win,
                  const EGLint *attrib_list) {
    VALIDATE_DISPLAY_RETURN(display,EGL_NO_SURFACE);
    VALIDATE_CONFIG_RETURN(config,EGL_NO_SURFACE);

    if(!(cfg->surfaceType() & EGL_WINDOW_BIT)) {
        RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_MATCH);
    }
    if(!EglOS::validNativeWin(dpy->nativeType(),win)) {
        RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_NATIVE_WINDOW);
    }
    if(!EglValidate::noAttribs(attrib_list)) {
        RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_ATTRIBUTE);
    }
    if(EglWindowSurface::alreadyAssociatedWithConfig(win)) {
        RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_ALLOC);
    }

    unsigned int width,height;
    if(!EglOS::checkWindowPixelFormatMatch(dpy->nativeType(),win,cfg,&width,&height)) {
        RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_ALLOC);
    }
    SurfacePtr wSurface(new EglWindowSurface(win,cfg,width,height));
    if(!wSurface.Ptr()) {
        RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_ALLOC);
    }
    return dpy->addSurface(wSurface);
}

EGLAPI EGLSurface EGLAPIENTRY eglCreatePbufferSurface(EGLDisplay display, EGLConfig config,
                   const EGLint *attrib_list) {
    VALIDATE_DISPLAY_RETURN(display,EGL_NO_SURFACE);
    VALIDATE_CONFIG_RETURN(config,EGL_NO_SURFACE);
    if(!(cfg->surfaceType() & EGL_PBUFFER_BIT)) {
        RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_MATCH);
    }


    SurfacePtr pbSurface(new EglPbufferSurface(cfg));
    if(!pbSurface.Ptr()) {
        RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_ALLOC);
    }

    if(!EglValidate::noAttribs(attrib_list)) { //there are attribs
        int i = 0 ;
        while(attrib_list[i] != EGL_NONE) {
            if(!pbSurface->setAttrib(attrib_list[i],attrib_list[i+1])) {
                RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_ATTRIBUTE);
            }
            i+=2;
        }
    }

    EGLint width,height,largest,texTarget,texFormat;
    EglPbufferSurface* tmpPbSurfacePtr = static_cast<EglPbufferSurface*>(pbSurface.Ptr());
    tmpPbSurfacePtr->getDim(&width,&height,&largest);
    tmpPbSurfacePtr->getTexInfo(&texTarget,&texFormat);

    if(!EglValidate::pbufferAttribs(width,height,texFormat == EGL_NO_TEXTURE,texTarget == EGL_NO_TEXTURE)) {
        //TODO: RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_VALUE); dont have bad_value
        RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_ATTRIBUTE);
    }

    EGLNativePbufferType pb = EglOS::createPbuffer(dpy->nativeType(),cfg,tmpPbSurfacePtr);
    if(!pb) {
        //TODO: RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_VALUE); dont have bad value
        RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_ATTRIBUTE);
    }

    tmpPbSurfacePtr->setNativePbuffer(pb);
    return dpy->addSurface(pbSurface);
}

EGLAPI EGLSurface EGLAPIENTRY eglCreatePixmapSurface(EGLDisplay display, EGLConfig config,
                  EGLNativePixmapType pixmap,
                  const EGLint *attrib_list) {
    VALIDATE_DISPLAY_RETURN(display,EGL_NO_SURFACE);
    VALIDATE_CONFIG_RETURN(config,EGL_NO_SURFACE);
    if(!(cfg->surfaceType() & EGL_PIXMAP_BIT)) {
        RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_MATCH);
    }
    if(!EglValidate::noAttribs(attrib_list)) {
        RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_ATTRIBUTE);
    }
    if(EglPixmapSurface::alreadyAssociatedWithConfig(pixmap)) {
        RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_ALLOC);
    }

    unsigned int width,height;
    if(!EglOS::checkPixmapPixelFormatMatch(dpy->nativeType(),pixmap,cfg,&width,&height)) {
        RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_ALLOC);
    }
    SurfacePtr pixSurface(new EglPixmapSurface(pixmap,cfg));
    if(!pixSurface.Ptr()) {
        RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_ALLOC);
    }

    return dpy->addSurface(pixSurface);
}

static bool destroySurfaceIfNotCurrent(EglDisplay* dpy,SurfacePtr surface) {

    ThreadInfo* thread  = getThreadInfo();
    EglContext* currCtx = static_cast<EglContext*>(thread->eglContext);
    if(currCtx && !currCtx->usingSurface(surface)){
        if(surface->type() == EglSurface::PBUFFER) {
            EglOS::releasePbuffer(dpy->nativeType(),reinterpret_cast<EGLNativePbufferType>(surface->native()));
            return true;
        }
    }
    return false;
}
EGLAPI EGLBoolean EGLAPIENTRY eglDestroySurface(EGLDisplay display, EGLSurface surface) {
    VALIDATE_DISPLAY(display);
    SurfacePtr srfc = dpy->getSurface(surface);
    if(!srfc.Ptr()) {
        RETURN_ERROR(EGL_FALSE,EGL_BAD_SURFACE);
    }

    srfc->markForDestruction(); //mark surface for destruction
    if(destroySurfaceIfNotCurrent(dpy,srfc)) { //removes surface from the list if not current
        dpy->removeSurface(surface);
    }
    return EGL_TRUE;
}

EGLAPI EGLBoolean EGLAPIENTRY eglQuerySurface(EGLDisplay display, EGLSurface surface,
               EGLint attribute, EGLint *value) {
   VALIDATE_DISPLAY(display);
   VALIDATE_SURFACE(surface,srfc);

   if(!srfc->getAttrib(attribute,value)) {
       RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
   }
   return EGL_TRUE;
}

EGLAPI EGLBoolean EGLAPIENTRY eglSurfaceAttrib(EGLDisplay display, EGLSurface surface,
                EGLint attribute, EGLint value) {
   VALIDATE_DISPLAY(display);
   VALIDATE_SURFACE(surface,srfc);
   if(!srfc->setAttrib(attribute,value)) {
       RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
   }
   return EGL_TRUE;
}

EGLAPI EGLContext EGLAPIENTRY eglCreateContext(EGLDisplay display, EGLConfig config,
                EGLContext share_context,
                const EGLint *attrib_list) {
    VALIDATE_DISPLAY_RETURN(display,EGL_NO_CONTEXT);
    VALIDATE_CONFIG_RETURN(config,EGL_NO_CONTEXT);

    GLESVersion version = GLES_1_1;
    if(!EglValidate::noAttribs(attrib_list)) {
        int i = 0;
        while(attrib_list[i] != EGL_NONE) {
            switch(attrib_list[i]) {
            case EGL_CONTEXT_CLIENT_VERSION:
                if(attrib_list[i+1] == 2) {
                    version = GLES_2_0;
                } else {
                    version = GLES_1_1;
                }
                break;
            default:
                RETURN_ERROR(EGL_NO_CONTEXT,EGL_BAD_ATTRIBUTE);
            }
            i+=2;
        }
    }
    GLESiface* iface = g_eglInfo->getIface(version);
    GLEScontext* glesCtx = NULL;
    if(iface) {
        glesCtx = iface->createGLESContext();
    } else { // there is no interface for this gles version
                RETURN_ERROR(EGL_NO_CONTEXT,EGL_BAD_ATTRIBUTE);
    }

    ContextPtr sharedCtxPtr;
    EGLNativeContextType nativeShared = NULL;
    if(share_context != EGL_NO_CONTEXT) {
        sharedCtxPtr = dpy->getContext(share_context);
        if(!sharedCtxPtr.Ptr()) {
            RETURN_ERROR(EGL_NO_CONTEXT,EGL_BAD_CONTEXT);
        }
        nativeShared = sharedCtxPtr->nativeType();
    }

    EGLNativeContextType nativeContext = EglOS::createContext(dpy->nativeType(),cfg,static_cast<EGLNativeContextType>(dpy->getManager(version)->getGlobalContext()));
    if(nativeContext) {
        ContextPtr ctx(new EglContext(nativeContext,sharedCtxPtr,cfg,glesCtx,version,dpy->getManager(version)));
        return dpy->addContext(ctx);
    } else {
        iface->deleteGLESContext(glesCtx);
    }

return EGL_NO_CONTEXT;
}

static bool destroyContextIfNotCurrent(EglDisplay* dpy,ContextPtr ctx ) {
    ThreadInfo* thread  = getThreadInfo();
    EglContext* currCtx = static_cast<EglContext*>(thread->eglContext);
  if(ctx.Ptr() != currCtx ){
      EglOS::destroyContext(dpy->nativeType(),ctx->nativeType());
      return true;
  }
  return false;
}

EGLAPI EGLBoolean EGLAPIENTRY eglDestroyContext(EGLDisplay display, EGLContext context) {
    VALIDATE_DISPLAY(display);
    VALIDATE_CONTEXT(context);

    ctx->markForDestruction(); //mark for destruction
    if(destroyContextIfNotCurrent(dpy,ctx)){ //removes the context from the list if it is not current
        g_eglInfo->getIface(ctx->version())->deleteGLESContext(ctx->getGlesContext());
        dpy->removeContext(context);
    }
    return EGL_TRUE;
}

EGLAPI EGLBoolean EGLAPIENTRY eglMakeCurrent(EGLDisplay display, EGLSurface draw,
              EGLSurface read, EGLContext context) {
    VALIDATE_DISPLAY(display);

    bool releaseContext = EglValidate::releaseContext(context,read,draw);
    if(!releaseContext && EglValidate::badContextMatch(context,read,draw)) {
        RETURN_ERROR(EGL_FALSE,EGL_BAD_MATCH);
    }

    ThreadInfo* thread     = getThreadInfo();
    EglContext* prevCtx    = static_cast<EglContext*>(thread->eglContext);

    if(releaseContext) { //releasing current context
       if(prevCtx) {
           g_eglInfo->getIface(prevCtx->version())->flush();
           if(!EglOS::makeCurrent(dpy->nativeType(),NULL,NULL,NULL)) {
               RETURN_ERROR(EGL_FALSE,EGL_BAD_ACCESS);
           }
           thread->updateInfo(NULL,dpy,NULL,ShareGroupPtr(NULL),dpy->getManager(prevCtx->version()));
       }
    } else { //assining new context
        VALIDATE_CONTEXT(context);
        VALIDATE_SURFACE(draw,newDrawSrfc);
        VALIDATE_SURFACE(read,newReadSrfc);

        EglSurface* newDrawPtr = newDrawSrfc.Ptr();
        EglSurface* newReadPtr = newReadSrfc.Ptr();
        EglContext* newCtx     = ctx.Ptr();

        //surfaces compitability check
        if(!((*ctx->getConfig()).compitableWith((*newDrawPtr->getConfig()))) ||
           !((*ctx->getConfig()).compitableWith((*newReadPtr->getConfig())))) {
            RETURN_ERROR(EGL_FALSE,EGL_BAD_MATCH);
        }

         EGLNativeDisplayType nativeDisplay = dpy->nativeType();
         void* nativeRead = newReadPtr->native();
         void* nativeDraw = newDrawPtr->native();
        //checking native window validity
        if(newReadPtr->type() == EglSurface::WINDOW && !EglOS::validNativeWin(nativeDisplay,reinterpret_cast<EGLNativeWindowType>(nativeRead))) {
            RETURN_ERROR(EGL_FALSE,EGL_BAD_NATIVE_WINDOW);
        }
        if(newDrawPtr->type() == EglSurface::WINDOW && !EglOS::validNativeWin(nativeDisplay,reinterpret_cast<EGLNativeWindowType>(nativeDraw))) {
            RETURN_ERROR(EGL_FALSE,EGL_BAD_NATIVE_WINDOW);
        }

        //checking native pixmap validity
        if(newReadPtr->type() == EglSurface::PIXMAP && !EglOS::validNativePixmap(nativeDisplay,reinterpret_cast<EGLNativePixmapType>(nativeRead))) {
            RETURN_ERROR(EGL_FALSE,EGL_BAD_NATIVE_PIXMAP);
        }
        if(newDrawPtr->type() == EglSurface::PIXMAP && !EglOS::validNativePixmap(nativeDisplay,reinterpret_cast<EGLNativePixmapType>(nativeDraw))) {
            RETURN_ERROR(EGL_FALSE,EGL_BAD_NATIVE_PIXMAP);
        }
        if(prevCtx) {
            g_eglInfo->getIface(prevCtx->version())->flush();
        }
        if(!EglOS::makeCurrent(dpy->nativeType(),newReadPtr,newDrawPtr,newCtx->nativeType())) {
               RETURN_ERROR(EGL_FALSE,EGL_BAD_ACCESS);
        }
        //TODO: handle the following errors
        // EGL_BAD_CURRENT_SURFACE , EGL_CONTEXT_LOST  , EGL_BAD_ACCESS

        thread->updateInfo(newCtx,dpy,newCtx->getGlesContext(),newCtx->getShareGroup(),dpy->getManager(newCtx->version()));
        newCtx->setSurfaces(newReadSrfc,newDrawSrfc);
        g_eglInfo->getIface(newCtx->version())->initContext(newCtx->getGlesContext());
        g_eglInfo->getIface(newCtx->version())->setShareGroup(newCtx->getGlesContext(),newCtx->getShareGroup());
    }

    SurfacePtr  prevRead;
    SurfacePtr  prevDraw;
    //removing terminated surfaces & context
    if(prevCtx) {
        prevRead = prevCtx->read();
        if(prevRead->destroy()){
            if(destroySurfaceIfNotCurrent(dpy,prevRead)) { //removes surface from the list if not current
                dpy->removeSurface(prevRead);
            }
        }
        prevDraw = prevCtx->draw();
        if(prevDraw->destroy()){
            if(destroySurfaceIfNotCurrent(dpy,prevDraw)) { //removes surface from the list if not current
                dpy->removeSurface(prevDraw);
            }
        }

        if(prevCtx->destroy()) {
           ContextPtr prevCtxPtr = ContextPtr(prevCtx);
           if(destroyContextIfNotCurrent(dpy,prevCtxPtr)){ //removes the context from the list if it is not current
                g_eglInfo->getIface(prevCtx->version())->deleteGLESContext(prevCtx->getGlesContext());
                dpy->removeContext(prevCtxPtr);
           }
        }

        prevCtx->setSurfaces(SurfacePtr(NULL),SurfacePtr(NULL));
    }
    return EGL_TRUE;
}

EGLAPI EGLBoolean EGLAPIENTRY eglQueryContext(EGLDisplay display, EGLContext context,
               EGLint attribute, EGLint *value) {
    VALIDATE_DISPLAY(display);
    VALIDATE_CONTEXT(context);

    if(!ctx->getAttrib(attribute,value)){
        RETURN_ERROR(EGL_FALSE,EGL_BAD_ATTRIBUTE);
    }
    return EGL_TRUE;
}

EGLAPI EGLBoolean EGLAPIENTRY eglSwapBuffers(EGLDisplay display, EGLSurface surface) {
    VALIDATE_DISPLAY(display);
    VALIDATE_SURFACE(surface,Srfc);
    ThreadInfo* thread        = getThreadInfo();
    EglContext* currentCtx    = static_cast<EglContext*>(thread->eglContext);


    //if surface not window return
    if(Srfc->type() != EglSurface::WINDOW){
        RETURN_ERROR(EGL_TRUE,EGL_SUCCESS);
    }

    if(!currentCtx || !currentCtx->usingSurface(Srfc) || !EglOS::validNativeWin(dpy->nativeType(),reinterpret_cast<EGLNativeWindowType>(Srfc.Ptr()->native()))) {
        RETURN_ERROR(EGL_FALSE,EGL_BAD_SURFACE);
    }

    EglOS::swapBuffers(dpy->nativeType(),reinterpret_cast<EGLNativeWindowType>(Srfc->native()));
    return EGL_TRUE;
}

EGLAPI EGLBoolean EGLAPIENTRY eglSwapInterval(EGLDisplay display, EGLint interval) {
    VALIDATE_DISPLAY(display);
    ThreadInfo* thread  = getThreadInfo();
    EglContext* currCtx = static_cast<EglContext*>(thread->eglContext);
    if(currCtx) {
        if(!currCtx->read().Ptr() || !currCtx->draw().Ptr() || currCtx->draw()->type()!=EglSurface::WINDOW) {
            RETURN_ERROR(EGL_FALSE,EGL_BAD_CURRENT_SURFACE);
        }
        EglOS::swapInterval(dpy->nativeType(),reinterpret_cast<EGLNativeWindowType>(currCtx->draw()->native()),interval);
    } else {
            RETURN_ERROR(EGL_FALSE,EGL_BAD_SURFACE);
    }
    return EGL_TRUE;
}


EGLAPI EGLContext EGLAPIENTRY eglGetCurrentContext(void) {
    ThreadInfo* thread = getThreadInfo();
    EglDisplay* dpy    = static_cast<EglDisplay*>(thread->eglDisplay);
    EglContext* ctx    = static_cast<EglContext*>(thread->eglContext);
    if(dpy && ctx){
        // This double check is required because a context might still be current after it is destroyed - in which case
        // its handle should be invalid, that is EGL_NO_CONTEXT should be returned even though the context is current
        EGLContext c = (EGLContext)ctx->getHndl();
        if(dpy->getContext(c).Ptr())
        {
            return c;
        }
    }
    return EGL_NO_CONTEXT;
}

EGLAPI EGLSurface EGLAPIENTRY eglGetCurrentSurface(EGLint readdraw) {
    if(!EglValidate::surfaceTarget(readdraw)) return EGL_NO_SURFACE;

    ThreadInfo* thread = getThreadInfo();
    EglDisplay* dpy    = static_cast<EglDisplay*>(thread->eglDisplay);
    EglContext* ctx    = static_cast<EglContext*>(thread->eglContext);

    if(dpy && ctx) {
        SurfacePtr surface = readdraw == EGL_READ ? ctx->read() : ctx->draw();
        if(surface.Ptr())
        {
            // This double check is required because a surface might still be
            // current after it is destroyed - in which case its handle should
            // be invalid, that is EGL_NO_SURFACE should be returned even
            // though the surface is current.
            EGLSurface s = (EGLSurface)surface->getHndl();
            surface = dpy->getSurface(s);
            if(surface.Ptr())
            {
                return s;
            }
        }
    }
    return EGL_NO_SURFACE;
}

EGLAPI EGLDisplay EGLAPIENTRY eglGetCurrentDisplay(void) {
    ThreadInfo* thread     = getThreadInfo();
    return (thread->eglContext) ? thread->eglDisplay : EGL_NO_DISPLAY;
}

EGLAPI EGLBoolean EGLAPIENTRY eglWaitGL(void) {
    EGLenum api = eglQueryAPI();
    eglBindAPI(EGL_OPENGL_ES_API);
    return eglWaitClient();
}

EGLAPI EGLBoolean EGLAPIENTRY eglWaitNative(EGLint engine) {
    if(!EglValidate::engine(engine)) {
        RETURN_ERROR(EGL_FALSE,EGL_BAD_PARAMETER);
    }
    ThreadInfo* thread  = getThreadInfo();
    EglContext* currCtx = static_cast<EglContext*>(thread->eglContext);
    EglDisplay* dpy     = static_cast<EglDisplay*>(thread->eglDisplay);
    if(currCtx) {
        SurfacePtr read = currCtx->read();
        SurfacePtr draw = currCtx->draw();

        EGLNativeDisplayType nativeDisplay = dpy->nativeType();
        if(read.Ptr()) {
            if(read->type() == EglSurface::WINDOW &&
               !EglOS::validNativeWin(nativeDisplay,reinterpret_cast<EGLNativeWindowType>(read->native()))) {
                RETURN_ERROR(EGL_FALSE,EGL_BAD_SURFACE);
            }
            if(read->type() == EglSurface::PIXMAP &&
               !EglOS::validNativePixmap(nativeDisplay,reinterpret_cast<EGLNativePixmapType>(read->native()))) {
                RETURN_ERROR(EGL_FALSE,EGL_BAD_SURFACE);
            }
        }
        if(draw.Ptr()) {
            if(draw->type() == EglSurface::WINDOW &&
               !EglOS::validNativeWin(nativeDisplay,reinterpret_cast<EGLNativeWindowType>(draw->native()))) {
                RETURN_ERROR(EGL_FALSE,EGL_BAD_SURFACE);
            }
            if(draw->type() == EglSurface::PIXMAP &&
               !EglOS::validNativePixmap(nativeDisplay,reinterpret_cast<EGLNativePixmapType>(draw->native()))) {
                RETURN_ERROR(EGL_FALSE,EGL_BAD_SURFACE);
            }
        }
    }
    EglOS::waitNative();
    return EGL_TRUE;
}

EGLAPI EGLBoolean EGLAPIENTRY eglBindAPI(EGLenum api) {
    if(!EglValidate::supportedApi(api)) {
        RETURN_ERROR(EGL_FALSE,EGL_BAD_PARAMETER);
    }
    CURRENT_THREAD();
    tls_thread->setApi(api);
    return EGL_TRUE;
}

EGLAPI EGLenum EGLAPIENTRY eglQueryAPI(void) {
    CURRENT_THREAD();
    return tls_thread->getApi();
}

EGLAPI EGLBoolean EGLAPIENTRY eglWaitClient(void) {
    ThreadInfo* thread  = getThreadInfo();
    EglContext* currCtx = static_cast<EglContext*>(thread->eglContext);
    if(currCtx) {
        if(!currCtx->read().Ptr() || !currCtx->draw().Ptr()) {
            RETURN_ERROR(EGL_FALSE,EGL_BAD_CURRENT_SURFACE);
        }
        g_eglInfo->getIface(currCtx->version())->finish();
    }
    return EGL_TRUE;
}

EGLAPI EGLBoolean EGLAPIENTRY eglReleaseThread(void) {
    ThreadInfo* thread  = getThreadInfo();
    EglDisplay* dpy     = static_cast<EglDisplay*>(thread->eglDisplay);
    return eglMakeCurrent(dpy,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);
}

EGLAPI __eglMustCastToProperFunctionPointerType EGLAPIENTRY
       eglGetProcAddress(const char *procname){
    __eglMustCastToProperFunctionPointerType retVal = NULL;
    if(!strncmp(procname,"egl",3)) { //EGL proc
        for(int i=0;i < EGL_EXTENSIONS;i++){
            if(strcmp(procname,s_eglExtentions[i].name) == 0){
                retVal = s_eglExtentions[i].address;
                break;
            }
        }
    } else if (!strncmp(procname,"gl",2)){ //GL proc
        retVal = g_eglInfo->getIface(GLES_1_1)->getProcAddress(procname); //try to get it from GLES 1.0
        if(!retVal){ //try to get it from GLES 2.0
            retVal = g_eglInfo->getIface(GLES_2_0)->getProcAddress(procname);
        }
    }
    return retVal;
}
//not supported for now
/************************* NOT SUPPORTED FOR NOW ***********************/
EGLAPI EGLSurface EGLAPIENTRY eglCreatePbufferFromClientBuffer(
          EGLDisplay display, EGLenum buftype, EGLClientBuffer buffer,
          EGLConfig config, const EGLint *attrib_list) {
    VALIDATE_DISPLAY(display);
    VALIDATE_CONFIG(config);
    //we do not support for now openVG, and the only client API resources which may be bound in this fashion are OpenVG
    RETURN_ERROR(EGL_NO_SURFACE,EGL_BAD_PARAMETER);
}

EGLAPI EGLBoolean EGLAPIENTRY eglCopyBuffers(EGLDisplay display, EGLSurface surface,
              EGLNativePixmapType target) {
    VALIDATE_DISPLAY(display);
    VALIDATE_SURFACE(surface,srfc);
    if(!EglOS::validNativePixmap(dpy->nativeType(),target)) {
        RETURN_ERROR(EGL_FALSE,EGL_BAD_NATIVE_PIXMAP);
    }

    //we do not need to support this for android , since we are not gonna use pixmaps
    RETURN_ERROR(EGL_FALSE,EGL_BAD_NATIVE_PIXMAP);
}

/***********************************************************************/



//do last ( only if needed)
/*********************************************************************************************************/
EGLAPI EGLBoolean EGLAPIENTRY eglBindTexImage(EGLDisplay dpy, EGLSurface surface, EGLint buffer) {
//TODO:
return 0;
}

EGLAPI EGLBoolean EGLAPIENTRY eglReleaseTexImage(EGLDisplay dpy, EGLSurface surface, EGLint buffer) {
//TODO:
return 0;
}
/*********************************************************************************************************/


/************************** KHR IMAGE *************************************************************/
EglImage *attachEGLImage(unsigned int imageId)
{
    ThreadInfo* thread  = getThreadInfo();
    EglDisplay* dpy     = static_cast<EglDisplay*>(thread->eglDisplay);
    EglContext* ctx     = static_cast<EglContext*>(thread->eglContext);
    if (ctx) {
        ImagePtr img = dpy->getImage(reinterpret_cast<EGLImageKHR>(imageId));
        if(img.Ptr()) {
             ctx->attachImage(imageId,img);
             return img.Ptr();
        }
    }
    return NULL;
}

void detachEGLImage(unsigned int imageId)
{
    ThreadInfo* thread  = getThreadInfo();
    EglDisplay* dpy     = static_cast<EglDisplay*>(thread->eglDisplay);
    EglContext* ctx     = static_cast<EglContext*>(thread->eglContext);
    if (ctx) {
        ctx->detachImage(imageId);
    }
}


EGLImageKHR eglCreateImageKHR(EGLDisplay display, EGLContext context, EGLenum target, EGLClientBuffer buffer, const EGLint *attrib_list)
{
    VALIDATE_DISPLAY(display);
    VALIDATE_CONTEXT(context);

    // We only support EGL_GL_TEXTURE_2D images
    if (target != EGL_GL_TEXTURE_2D_KHR) {
        RETURN_ERROR(EGL_NO_IMAGE_KHR,EGL_BAD_PARAMETER);
    }

    ThreadInfo* thread  = getThreadInfo();
    ShareGroupPtr sg = thread->shareGroup;
    if (sg.Ptr() != NULL) {
        unsigned int globalTexName = sg->getGlobalName(TEXTURE, (unsigned int)buffer);
        if (!globalTexName) return EGL_NO_IMAGE_KHR;

        ImagePtr img( new EglImage() );
        if (img.Ptr() != NULL) {

            ObjectDataPtr objData = sg->getObjectData(TEXTURE, (unsigned int)buffer);
            if (!objData.Ptr()) return EGL_NO_IMAGE_KHR;

            TextureData *texData = (TextureData *)objData.Ptr();
            if(!texData->width || !texData->height) return EGL_NO_IMAGE_KHR;
            img->width = texData->width;
            img->height = texData->height;
            img->border = texData->border;
            img->internalFormat = texData->internalFormat;
            img->globalTexName = globalTexName;
            return dpy->addImageKHR(img);
        }
    }

    return EGL_NO_IMAGE_KHR;
}


EGLBoolean eglDestroyImageKHR(EGLDisplay display, EGLImageKHR image)
{
    VALIDATE_DISPLAY(display);
    return dpy->destroyImageKHR(image) ? EGL_TRUE:EGL_FALSE;
}

/*********************************************************************************/
