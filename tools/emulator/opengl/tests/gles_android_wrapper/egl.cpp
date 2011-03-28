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

//
// WARNING -------------------------- WARNING
// This code meant to be used for testing purposes only. It is not production
// level quality.
// Use on your own risk !!
//

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dlfcn.h>
#include "egl_dispatch.h"
#include "egl_ftable.h"
#include <cutils/process_name.h>
#include <cutils/log.h>
#include "ServerConnection.h"
#include "ThreadInfo.h"
#include <pthread.h>


#define GLES_EMUL_TARGETS_FILE "/system/etc/gles_emul.cfg"

static struct egl_dispatch *s_dispatch = NULL;
pthread_once_t dispatchTablesInitialized = PTHREAD_ONCE_INIT;

static bool s_needEncode = false;

extern void init_gles(void *gles_android);
extern __eglMustCastToProperFunctionPointerType gles_getProcAddress(const char *procname);


const char *getProcName()
{
    static const char *procname = NULL;

    if (procname == NULL) {
        const char *str = get_process_name();
        if (strcmp(str, "unknown") != 0) {
            procname = str;
        } else {
            // we need to obtain our process name from the command line;
            FILE *fp = fopen("/proc/self/cmdline", "rt");
            if (fp == NULL) {
                LOGE("couldn't open /proc/self/cmdline\n");
                return NULL;
            }

            char line[1000];
            if (fgets(line, sizeof(line), fp) == NULL) {
                LOGE("couldn't read the self cmdline from \n");
                fclose(fp);
                return NULL;
            }
            fclose(fp);

            if (line[0] == '\0') {
                LOGE("cmdline is empty\n");
                return NULL;
            }

            //obtain the basename;
            line[sizeof(line) - 1] = '\0';
            char *p = line;
            while (*p != '\0' &&
                   *p != '\t' &&
                   *p != ' ' &&
                   *p != '\n') {
                p++;
            }

            *p = '\0'; p--;
            while (p > line && *p != '/') p--;
            if (*p == '/') p++;
            procname = strdup(p);
        }
    }
    LOGD("getProcessName: %s\n", procname == NULL ? "NULL": procname);

    return procname;
}



bool isNeedEncode()
{
    const char *procname = getProcName();
    if (procname == NULL) return false;
    LOGD("isNeedEncode? for %s\n", procname);
    // check on our whitelist
    FILE *fp = fopen(GLES_EMUL_TARGETS_FILE, "rt");
    if (fp == NULL) {
        LOGE("couldn't open %s\n", GLES_EMUL_TARGETS_FILE);
        return false;
    }

    char line[100];
    bool found = false;
    size_t  procnameLen = strlen(procname);

    while (fgets(line, sizeof(line), fp) != NULL) {
        if (strlen(line) >= procnameLen &&
            !strncmp(procname, line, procnameLen)) {
            char c = line[procnameLen];
            if (c == '\0' || c == ' ' || c == '\t' || c == '\n') {
                found = true;
                LOGD("should use encoder for %s\n", procname);
                break;
            }
        }
    }
    fclose(fp);
    return found;
}

void initDispatchTables()
{
    //
    // Load our back-end implementation of EGL/GLES
    //
    LOGD("Loading egl dispatch for %s\n", getProcName());

    void *gles_android = dlopen("/system/lib/egl/libGLES_android.so", RTLD_NOW | RTLD_LOCAL);
    if (!gles_android) {
        fprintf(stderr,"FATAL ERROR: Could not load libGLES_android lib\n");
        exit(-1);
    }

    //
    // Load back-end EGL implementation library
    //
    s_dispatch = create_egl_dispatch( gles_android );
    if (!s_dispatch) {
        fprintf(stderr,"FATAL ERROR: Could not create egl dispatch\n");
        exit(-1);
    }

    //
    // initialize gles
    //
    s_needEncode = isNeedEncode();
    void *gles_encoder = NULL;
    if (s_needEncode) {
        ServerConnection * connection = ServerConnection::s_getServerConnection();
        if (connection == NULL) {
            LOGE("couldn't create server connection\n");
            s_needEncode = false;
        } else {
            LOGD("Created server connection for %s\n", getProcName());
            gles_encoder = dlopen("/system/lib/libGLESv1_enc.so", RTLD_NOW);
            if (gles_encoder == NULL) {
                LOGE("couldn't open libGLESv1_enc.so... aborting connection");
                delete connection;
                s_needEncode = false;
            }
        }
    }

    if (s_needEncode && gles_encoder) {
        init_gles(gles_encoder);
    } else {
        LOGD("Initializing native opengl for %s\n", getProcName());
        init_gles(gles_android);
    }
}

static struct egl_dispatch *getDispatch()
{
    pthread_once(&dispatchTablesInitialized, initDispatchTables);
    return s_dispatch;
}

__eglMustCastToProperFunctionPointerType eglGetProcAddress(const char *procname)
{

    // search in EGL function table
    for (int i=0; i<egl_num_funcs; i++) {
        if (!strcmp(egl_funcs_by_name[i].name, procname)) {
            return (__eglMustCastToProperFunctionPointerType)egl_funcs_by_name[i].proc;
        }
    }

    // search in GLES function table
    __eglMustCastToProperFunctionPointerType f = gles_getProcAddress(procname);
    if (f != NULL) {
        return f;
    }

    // should probably fail - search in back-end anyway.
    return getDispatch()->eglGetProcAddress(procname);
}

////////////////  Path through functions //////////

EGLint eglGetError()
{
    return getDispatch()->eglGetError();
}

EGLDisplay eglGetDisplay(EGLNativeDisplayType display_id)
{
    return getDispatch()->eglGetDisplay(display_id);
}

EGLBoolean eglInitialize(EGLDisplay dpy, EGLint *major, EGLint *minor)
{
    return getDispatch()->eglInitialize(dpy, major, minor);
}

EGLBoolean eglTerminate(EGLDisplay dpy)
{
    return getDispatch()->eglTerminate(dpy);
}

const char* eglQueryString(EGLDisplay dpy, EGLint name)
{
    return getDispatch()->eglQueryString(dpy, name);
}

EGLBoolean eglGetConfigs(EGLDisplay dpy, EGLConfig *configs, EGLint config_size, EGLint *num_config)
{
    return getDispatch()->eglGetConfigs(dpy, configs, config_size, num_config);
}

EGLBoolean eglChooseConfig(EGLDisplay dpy, const EGLint *attrib_list, EGLConfig *configs, EGLint config_size, EGLint *num_config)
{
    return getDispatch()->eglChooseConfig(dpy, attrib_list, configs, config_size, num_config);
}

EGLBoolean eglGetConfigAttrib(EGLDisplay dpy, EGLConfig config, EGLint attribute, EGLint *value)
{
    return getDispatch()->eglGetConfigAttrib(dpy, config, attribute, value);
}

EGLSurface eglCreateWindowSurface(EGLDisplay dpy, EGLConfig config, EGLNativeWindowType win, const EGLint *attrib_list)
{
    EGLSurface surface =  getDispatch()->eglCreateWindowSurface(dpy, config, win, attrib_list);
    if (surface != EGL_NO_SURFACE) {
        ServerConnection *server;
        if (s_needEncode && (server = ServerConnection::s_getServerConnection()) != NULL) {
            server->utEnc()->createSurface(server->utEnc(), getpid(), (uint32_t)surface);
        }
    }
    return surface;
}

EGLSurface eglCreatePbufferSurface(EGLDisplay dpy, EGLConfig config, const EGLint *attrib_list)
{
    EGLSurface surface =  getDispatch()->eglCreatePbufferSurface(dpy, config, attrib_list);
    if (surface != EGL_NO_SURFACE) {
        ServerConnection *server;
        if (s_needEncode && (server = ServerConnection::s_getServerConnection()) != NULL) {
            server->utEnc()->createSurface(server->utEnc(), getpid(), (uint32_t)surface);
        }
    }
    return surface;
}

EGLSurface eglCreatePixmapSurface(EGLDisplay dpy, EGLConfig config, EGLNativePixmapType pixmap, const EGLint *attrib_list)
{
    EGLSurface surface =  getDispatch()->eglCreatePixmapSurface(dpy, config, pixmap, attrib_list);
    if (surface != EGL_NO_SURFACE) {
        ServerConnection *server;
        if (s_needEncode && (server = ServerConnection::s_getServerConnection()) != NULL) {
            server->utEnc()->createSurface(server->utEnc(), getpid(), (uint32_t)surface);
        }
    }
    return surface;
}

EGLBoolean eglDestroySurface(EGLDisplay dpy, EGLSurface surface)
{
    EGLBoolean res =  getDispatch()->eglDestroySurface(dpy, surface);
    if (res && surface != EGL_NO_SURFACE) {
        ServerConnection *server;
        if (s_needEncode && (server = ServerConnection::s_getServerConnection()) != NULL) {
            server->utEnc()->destroySurface(server->utEnc(), getpid(), (uint32_t)surface);
        }
    }
    return res;
}

EGLBoolean eglQuerySurface(EGLDisplay dpy, EGLSurface surface, EGLint attribute, EGLint *value)
{
    return getDispatch()->eglQuerySurface(dpy, surface, attribute, value);
}

EGLBoolean eglBindAPI(EGLenum api)
{
    return getDispatch()->eglBindAPI(api);
}

EGLenum eglQueryAPI()
{
    return getDispatch()->eglQueryAPI();
}

EGLBoolean eglWaitClient()
{
    return getDispatch()->eglWaitClient();
}

EGLBoolean eglReleaseThread()
{
    return getDispatch()->eglReleaseThread();
}

EGLSurface eglCreatePbufferFromClientBuffer(EGLDisplay dpy, EGLenum buftype, EGLClientBuffer buffer, EGLConfig config, const EGLint *attrib_list)
{
    return getDispatch()->eglCreatePbufferFromClientBuffer(dpy, buftype, buffer, config, attrib_list);
}

EGLBoolean eglSurfaceAttrib(EGLDisplay dpy, EGLSurface surface, EGLint attribute, EGLint value)
{
    return getDispatch()->eglSurfaceAttrib(dpy, surface, attribute, value);
}

EGLBoolean eglBindTexImage(EGLDisplay dpy, EGLSurface surface, EGLint buffer)
{
    return getDispatch()->eglBindTexImage(dpy, surface, buffer);
}

EGLBoolean eglReleaseTexImage(EGLDisplay dpy, EGLSurface surface, EGLint buffer)
{
    return getDispatch()->eglReleaseTexImage(dpy, surface, buffer);
}

EGLBoolean eglSwapInterval(EGLDisplay dpy, EGLint interval)
{
    return getDispatch()->eglSwapInterval(dpy, interval);
}

EGLContext eglCreateContext(EGLDisplay dpy, EGLConfig config, EGLContext share_context, const EGLint *attrib_list)
{
    EGLContext share = share_context;
    if (share) share = ((EGLWrapperContext *)share_context)->aglContext;

    EGLContext ctx =  getDispatch()->eglCreateContext(dpy, config, share, attrib_list);
    EGLWrapperContext *wctx = new EGLWrapperContext(ctx);
    if (ctx != EGL_NO_CONTEXT) {
        ServerConnection *server;
        if (s_needEncode && (server = ServerConnection::s_getServerConnection()) != NULL) {
            wctx->clientState = new GLClientState();
            server->utEnc()->createContext(server->utEnc(), getpid(),
                                           (uint32_t)wctx,
                                           (uint32_t)(share_context == EGL_NO_CONTEXT ? 0 : share_context));
        }
    }
    return (EGLContext)wctx;
}

EGLBoolean eglDestroyContext(EGLDisplay dpy, EGLContext ctx)
{
    EGLWrapperContext *wctx = (EGLWrapperContext *)ctx;
    EGLBoolean res = EGL_FALSE;

    if (ctx && ctx != EGL_NO_CONTEXT) {
        res = getDispatch()->eglDestroyContext(dpy, wctx->aglContext);
        if (res) {
            EGLThreadInfo *ti = getEGLThreadInfo();
            if (s_needEncode && ti->serverConn) {
                ti->serverConn->utEnc()->destroyContext(ti->serverConn->utEnc(), getpid(), (uint32_t)ctx);
            }
            if (ti->currentContext == wctx) ti->currentContext = NULL;
            delete wctx;
        }
    }

    return res;
}

EGLBoolean eglMakeCurrent(EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx)
{
    EGLWrapperContext *wctx = (EGLWrapperContext *)ctx;
    EGLContext aglContext = (ctx == EGL_NO_CONTEXT ? EGL_NO_CONTEXT : wctx->aglContext);
    EGLThreadInfo *ti = getEGLThreadInfo();
    EGLBoolean res = getDispatch()->eglMakeCurrent(dpy, draw, read, aglContext);
    if (res ) {
        ServerConnection *server;
        if (s_needEncode && ti->serverConn) {
            ti->serverConn->utEnc()->makeCurrentContext(ti->serverConn->utEnc(), getpid(),
                                                (uint32_t) (draw == EGL_NO_SURFACE ? 0 : draw),
                                                (uint32_t) (read == EGL_NO_SURFACE ? 0 : read),
                                                (uint32_t) (ctx == EGL_NO_CONTEXT ? 0 : ctx));
            ti->serverConn->glEncoder()->setClientState( wctx ? wctx->clientState : NULL );
        }

        // set current context in our thread info
        ti->currentContext = wctx;
    }
    return res;

}

EGLContext eglGetCurrentContext()
{
    EGLThreadInfo *ti = getEGLThreadInfo();
    return (ti->currentContext ? ti->currentContext : EGL_NO_CONTEXT);
}

EGLSurface eglGetCurrentSurface(EGLint readdraw)
{
    return getDispatch()->eglGetCurrentSurface(readdraw);
}

EGLDisplay eglGetCurrentDisplay()
{
    return getDispatch()->eglGetCurrentDisplay();
}

EGLBoolean eglQueryContext(EGLDisplay dpy, EGLContext ctx, EGLint attribute, EGLint *value)
{
    EGLWrapperContext *wctx = (EGLWrapperContext *)ctx;
    if (wctx) {
        return getDispatch()->eglQueryContext(dpy, wctx->aglContext, attribute, value);
    }
    else {
        return EGL_BAD_CONTEXT;
    }
}

EGLBoolean eglWaitGL()
{
    return getDispatch()->eglWaitGL();
}

EGLBoolean eglWaitNative(EGLint engine)
{
    return getDispatch()->eglWaitNative(engine);
}

EGLBoolean eglSwapBuffers(EGLDisplay dpy, EGLSurface surface)
{
    ServerConnection *server;
    if (s_needEncode && (server = ServerConnection::s_getServerConnection()) != NULL) {
        server->utEnc()->swapBuffers(server->utEnc(), getpid(), (uint32_t)surface);
        server->glEncoder()->flush();
        return 1;
    }
    return getDispatch()->eglSwapBuffers(dpy, surface);
}

EGLBoolean eglCopyBuffers(EGLDisplay dpy, EGLSurface surface, EGLNativePixmapType target)
{
    return getDispatch()->eglCopyBuffers(dpy, surface, target);
}

EGLBoolean eglLockSurfaceKHR(EGLDisplay display, EGLSurface surface, const EGLint *attrib_list)
{
    return getDispatch()->eglLockSurfaceKHR(display, surface, attrib_list);
}

EGLBoolean eglUnlockSurfaceKHR(EGLDisplay display, EGLSurface surface)
{
    return getDispatch()->eglUnlockSurfaceKHR(display, surface);
}

EGLImageKHR eglCreateImageKHR(EGLDisplay dpy, EGLContext ctx, EGLenum target, EGLClientBuffer buffer, const EGLint *attrib_list)
{
    EGLWrapperContext *wctx = (EGLWrapperContext *)ctx;
    EGLContext aglContext = (wctx ? wctx->aglContext : EGL_NO_CONTEXT);
    return getDispatch()->eglCreateImageKHR(dpy, aglContext, target, buffer, attrib_list);
}

EGLBoolean eglDestroyImageKHR(EGLDisplay dpy, EGLImageKHR image)
{
    return getDispatch()->eglDestroyImageKHR(dpy, image);
}

EGLSyncKHR eglCreateSyncKHR(EGLDisplay dpy, EGLenum type, const EGLint *attrib_list)
{
    return getDispatch()->eglCreateSyncKHR(dpy, type, attrib_list);
}

EGLBoolean eglDestroySyncKHR(EGLDisplay dpy, EGLSyncKHR sync)
{
    return getDispatch()->eglDestroySyncKHR(dpy, sync);
}

EGLint eglClientWaitSyncKHR(EGLDisplay dpy, EGLSyncKHR sync, EGLint flags, EGLTimeKHR timeout)
{
    return getDispatch()->eglClientWaitSyncKHR(dpy, sync, flags, timeout);
}

EGLBoolean eglSignalSyncKHR(EGLDisplay dpy, EGLSyncKHR sync, EGLenum mode)
{
    return getDispatch()->eglSignalSyncKHR(dpy, sync, mode);
}

EGLBoolean eglGetSyncAttribKHR(EGLDisplay dpy, EGLSyncKHR sync, EGLint attribute, EGLint *value)
{
    return getDispatch()->eglGetSyncAttribKHR(dpy, sync, attribute, value);
}

EGLBoolean eglSetSwapRectangleANDROID(EGLDisplay dpy, EGLSurface draw, EGLint left, EGLint top, EGLint width, EGLint height)
{
    return getDispatch()->eglSetSwapRectangleANDROID(dpy, draw, left, top, width, height);
}
