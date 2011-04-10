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
#ifndef _LIB_OPENGL_RENDER_RENDER_SERVER_H
#define _LIB_OPENGL_RENDER_RENDER_SERVER_H

#include "TcpStream.h"
#include "osThread.h"

class RenderServer : public osUtils::Thread
{
public:
    static RenderServer *create(int port);
    virtual int Main();

    void flagNeedExit() { m_exit = true; }

private:
    RenderServer();

private:
    TcpStream *m_listenSock;
    bool m_exit;
};

#endif
