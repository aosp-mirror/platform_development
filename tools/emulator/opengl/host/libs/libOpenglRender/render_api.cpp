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
#include "libOpenglRender/render_api.h"
#include "IOStream.h"
#include "FrameBuffer.h"
#include "RenderServer.h"
#include "osProcess.h"
#include "TimeUtils.h"

#include "TcpStream.h"
#ifdef _WIN32
#include "Win32PipeStream.h"
#else
#include "UnixStream.h"
#endif

#include "EGLDispatch.h"
#include "GLDispatch.h"
#include "GL2Dispatch.h"

static osUtils::childProcess *s_renderProc = NULL;
static RenderServer *s_renderThread = NULL;
static int s_renderPort = 0;

static IOStream *createRenderThread(int p_stream_buffer_size,
                                    unsigned int clientFlags);

//
// For now run the renderer as a thread inside the calling
// process instead as running it in a separate process for all
// platforms.
// at the future we want it to run as a seperate process except for
// Mac OS X since it is imposibble on this platform to make one process
// render to a window created by another process.
//
//#ifdef __APPLE__
#define  RENDER_API_USE_THREAD
//#endif

bool initLibrary(void)
{
    //
    // Load EGL Plugin
    //
    if (!init_egl_dispatch()) {
        // Failed to load EGL
        printf("Failed to init_egl_dispatch\n");
        return false;
    }

    //
    // Load GLES Plugin
    //
    if (!init_gl_dispatch()) {
        // Failed to load GLES
        ERR("Failed to init_gl_dispatch\n");
        return false;
    }

    /* failure to init the GLES2 dispatch table is not fatal */
    init_gl2_dispatch();

    return true;
}

bool initOpenGLRenderer(int width, int height, int portNum)
{

    //
    // Fail if renderer is already initialized
    //
    if (s_renderProc || s_renderThread) {
        return false;
    }

    s_renderPort = portNum;

#ifdef RENDER_API_USE_THREAD  // should be defined for mac
    //
    // initialize the renderer and listen to connections
    // on a thread in the current process.
    //
    bool inited = FrameBuffer::initialize(width, height);
    if (!inited) {
        return false;
    }

    s_renderThread = RenderServer::create(portNum);
    if (!s_renderThread) {
        return false;
    }

    s_renderThread->start();

#else
    //
    // Launch emulator_renderer
    //
    char cmdLine[128];
    snprintf(cmdLine, 128, "emulator_renderer -windowid %d -port %d -x %d -y %d -width %d -height %d",
             (int)window, portNum, x, y, width, height);

    s_renderProc = osUtils::childProcess::create(cmdLine, NULL);
    if (!s_renderProc) {
        return false;
    }

    //
    // try to connect to the renderer in order to check it
    // was successfully initialized.
    //
    int nTrys = 0;
    IOStream *dummy = NULL;
    do {
        ++nTrys;

        //
        // Wait a bit to make the renderer process a chance to be
        // initialized.
        // On Windows we need during this time to handle windows
        // events since the renderer generates a subwindow of this
        // process's window, we need to be responsive for windows
        // during this time to let the renderer generates this subwindow.
        //
#ifndef _WIN32
        TimeSleepMS(300);
#else
        long long t0 = GetCurrentTimeMS();
        while( (GetCurrentTimeMS() - t0) < 300 ) {
            MSG msg;
            int n = 0;
            while( PeekMessage(&msg, NULL, 0, 0, PM_REMOVE) )
            {
                n++;
                TranslateMessage( &msg );
                DispatchMessage( &msg );
            }
            if (n == 0) TimeSleepMS(10);
        }
#endif

        dummy = createRenderThread(8, 0);

        if (!dummy) {
            // stop if the process is no longer running
            if (!osUtils::isProcessRunning(s_renderProc->getPID())) {
                break;
            }
        }
    } while(!dummy && nTrys < 10); // give up after 3 seconds, XXX: ???

    if (!dummy) {
        //
        // Failed - make sure the process is killed
        //
        osUtils::KillProcess(s_renderProc->getPID(), true);
        delete s_renderProc;
        s_renderProc = NULL;
        return false;
    }

    // destroy the dummy connection
    delete dummy;
#endif

    return true;
}

bool stopOpenGLRenderer()
{
    bool ret = false;

    // open a dummy connection to the renderer to make it
    // realize the exit request.
    // (send the exit request in clientFlags)
    IOStream *dummy = createRenderThread(8, IOSTREAM_CLIENT_EXIT_SERVER);
    if (!dummy) return false;

    if (s_renderProc) {
        //
        // wait for the process to exit
        //
        int exitStatus;
        ret = s_renderProc->wait(&exitStatus);

        delete s_renderProc;
        s_renderProc = NULL;
    }
    else if (s_renderThread) {

        // wait for the thread to exit
        int status;
        ret = s_renderThread->wait(&status);

        delete s_renderThread;
        s_renderThread = NULL;
    }

    return ret;
}

bool createOpenGLSubwindow(FBNativeWindowType window,
                           int x, int y, int width, int height, float zRot)
{
    if (s_renderThread) {
        return FrameBuffer::setupSubWindow(window,x,y,width,height, zRot);
    }
    else {
        //
        // XXX: should be implemented by sending the renderer process
        //      a request
        ERR("%s not implemented for separate renderer process !!!\n",
            __FUNCTION__);
    }
    return false;
}

bool destroyOpenGLSubwindow()
{
    if (s_renderThread) {
        return FrameBuffer::removeSubWindow();
    }
    else {
        //
        // XXX: should be implemented by sending the renderer process
        //      a request
        ERR("%s not implemented for separate renderer process !!!\n",
                __FUNCTION__);
        return false;
    }
}

void setOpenGLDisplayRotation(float zRot)
{
    if (s_renderThread) {
        FrameBuffer *fb = FrameBuffer::getFB();
        if (fb) {
            fb->setDisplayRotation(zRot);
        }
    }
    else {
        //
        // XXX: should be implemented by sending the renderer process
        //      a request
        ERR("%s not implemented for separate renderer process !!!\n",
                __FUNCTION__);
    }
}

void repaintOpenGLDisplay()
{
    if (s_renderThread) {
        FrameBuffer *fb = FrameBuffer::getFB();
        if (fb) {
            fb->repost();
        }
    }
    else {
        //
        // XXX: should be implemented by sending the renderer process
        //      a request
        ERR("%s not implemented for separate renderer process !!!\n",
                __FUNCTION__);
    }
}


/* NOTE: For now, always use TCP mode by default, until the emulator
 *        has been updated to support Unix and Win32 pipes
 */
#define  DEFAULT_STREAM_MODE  STREAM_MODE_TCP

int gRendererStreamMode = DEFAULT_STREAM_MODE;

IOStream *createRenderThread(int p_stream_buffer_size, unsigned int clientFlags)
{
    SocketStream*  stream = NULL;

    if (gRendererStreamMode == STREAM_MODE_TCP) {
        stream = new TcpStream(p_stream_buffer_size);
    } else {
#ifdef _WIN32
        stream = new Win32PipeStream(p_stream_buffer_size);
#else /* !_WIN32 */
        stream = new UnixStream(p_stream_buffer_size);
#endif
    }

    if (!stream) {
        ERR("createRenderThread failed to create stream\n");
        return NULL;
    }
    if (stream->connect(s_renderPort) < 0) {
        ERR("createRenderThread failed to connect\n");
        delete stream;
        return NULL;
    }

    //
    // send clientFlags to the renderer
    //
    unsigned int *pClientFlags =
                (unsigned int *)stream->allocBuffer(sizeof(unsigned int));
    *pClientFlags = clientFlags;
    stream->commitBuffer(sizeof(unsigned int));

    return stream;
}

int
setStreamMode(int mode)
{
    switch (mode) {
        case STREAM_MODE_DEFAULT:
            mode = DEFAULT_STREAM_MODE;
            break;

        case STREAM_MODE_TCP:
            break;

#ifndef _WIN32
        case STREAM_MODE_UNIX:
            break;
#else /* _WIN32 */
        case STREAM_MODE_PIPE:
            break;
#endif /* _WIN32 */
        default:
            // Invalid stream mode
            return -1;
    }
    gRendererStreamMode = mode;
    return 0;
}
