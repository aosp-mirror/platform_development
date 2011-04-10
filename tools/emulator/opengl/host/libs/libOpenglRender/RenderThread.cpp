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
#include "RenderThread.h"
#include "RenderControl.h"
#include "ReadBuffer.h"
#include "TimeUtils.h"
#include "GLDispatch.h"

#define STREAM_BUFFER_SIZE 4*1024*1024

RenderThread::RenderThread() :
    osUtils::Thread(),
    m_stream(NULL)
{
}

RenderThread *RenderThread::create(IOStream *p_stream)
{
    RenderThread *rt = new RenderThread();
    if (!rt) {
        return NULL;
    }

    rt->m_stream = p_stream;

    return rt;
}

int RenderThread::Main()
{
    //
    // initialize decoders
    //
    m_glDec.initGL( gl_dispatch_get_proc_func, NULL );
    initRenderControlContext( &m_rcDec );

    ReadBuffer readBuf(m_stream, STREAM_BUFFER_SIZE);

    int stats_totalBytes = 0;
    long long stats_t0 = GetCurrentTimeMS();

    while (1) {

        int stat = readBuf.getData();
        if (stat <= 0) {
            fprintf(stderr, "client shutdown\n");
            break;
        }

        //
        // log received bandwidth statistics
        //
        stats_totalBytes += readBuf.validData();
        long long dt = GetCurrentTimeMS() - stats_t0;
        if (dt > 1000) {
            float dts = (float)dt / 1000.0f;
            printf("Used Bandwidth %5.3f MB/s\n", ((float)stats_totalBytes / dts) / (1024.0f*1024.0f));
            stats_totalBytes = 0;
            stats_t0 = GetCurrentTimeMS();
        }

        bool progress;
        do {
            progress = false;

            //
            // try to process some of the command buffer using the GLES decoder
            //
            size_t last = m_glDec.decode(readBuf.buf(), readBuf.validData(), m_stream);
            if (last > 0) {
                progress = true;
                readBuf.consume(last);
            }

            //
            // try to process some of the command buffer using the
            // renderControl decoder
            //
            last = m_rcDec.decode(readBuf.buf(), readBuf.validData(), m_stream);
            if (last > 0) {
                readBuf.consume(last);
                progress = true;
            }

        } while( progress );

    }

    return 0;
}
