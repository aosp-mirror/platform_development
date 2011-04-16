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
#ifndef __COMMON_HOST_CONNECTION_H
#define __COMMON_HOST_CONNECTION_H

#include "IOStream.h"
#include "GLEncoder.h"
#include "renderControl_enc.h"

class HostConnection
{
public:
    static HostConnection *get();
    ~HostConnection();

    GLEncoder *glEncoder();
    renderControl_encoder_context_t *rcEncoder();

    void flush() {
        if (m_stream) {
            m_stream->flush();
        }
    }

private:
    HostConnection();
    static gl_client_context_t *s_getGLContext();

private:
    IOStream *m_stream;
    GLEncoder *m_glEnc;
    renderControl_encoder_context_t *m_rcEnc;
};

#endif
