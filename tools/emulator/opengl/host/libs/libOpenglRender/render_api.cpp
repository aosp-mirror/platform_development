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
#include "FrameBuffer.h"
#include "RenderServer.h"
#include "osProcess.h"
#include "TimeUtils.h"

static osUtils::childProcess *s_renderProc = NULL;
static RenderServer *s_renderThread = NULL;
static int s_renderPort = 0;

bool initOpenGLRenderer(FBNativeWindowType window,
                        int x, int y, int width, int height,
                        int portNum)
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
    bool inited = FrameBuffer::initialize(window, x, y, width, height);
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
        TimeSleepMS(300);
        dummy = createRenderThread(8);

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

    if (s_renderProc) {
        //
        // kill the render process
        //
        ret = osUtils::KillProcess(s_renderProc->getPID(), true) != 0;
        if (ret) {
            delete s_renderProc;
            s_renderProc = NULL;
        }
    }
    else if (s_renderThread) {
        // flag the thread it should exit
        s_renderThread->flagNeedExit();

        // open a dummy connection to the renderer to make it 
        // realize the exit request
        IOStream *dummy = createRenderThread(8);
        if (dummy) {
            // wait for the thread to exit
            int status;
            ret = s_renderThread->wait(&status);
    
            delete dummy;
        }

        delete s_renderThread;
        s_renderThread = NULL;
    }

    return ret;
}

IOStream *createRenderThread(int p_stream_buffer_size)
{
    TcpStream *stream = new TcpStream(p_stream_buffer_size);
    if (!stream) {
        return NULL;
    }

    if (stream->connect("localhost", s_renderPort) < 0) {
        delete stream;
        return NULL;
    }

    return stream;
}
