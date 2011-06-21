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
#include "RenderServer.h"
#include "TcpStream.h"
#include "RenderThread.h"

RenderServer::RenderServer() :
    m_listenSock(NULL),
    m_exit(false)
{
}

RenderServer *RenderServer::create(int port)
{
    RenderServer *server = new RenderServer();
    if (!server) {
        return NULL;
    }

    server->m_listenSock = new TcpStream();
    if (server->m_listenSock->listen(port) < 0) {
        ERR("RenderServer::create failed to listen on port %d\n", port);
        delete server;
        return NULL;
    }

    return server;
}

int RenderServer::Main()
{
    while(!m_exit) {
        TcpStream *stream = m_listenSock->accept();
        if (!stream) {
            fprintf(stderr,"Error accepting connection, aborting\n");
            break;
        }

        DBG("\n\n\n\n Got new stream!!!! \n\n\n\n\n");
        // check if we have been requested to exit while waiting on accept
        if (m_exit) {
            break;
        }

        RenderThread *rt = RenderThread::create(stream);
        if (!rt) {
            fprintf(stderr,"Failed to create RenderThread\n");
            delete stream;
        }

        if (!rt->start()) {
            fprintf(stderr,"Failed to start RenderThread\n");
            delete stream;
        }

        printf("Started new RenderThread\n");
    }

    return 0;
}
