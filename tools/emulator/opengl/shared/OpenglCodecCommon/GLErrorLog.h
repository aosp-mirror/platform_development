/*
* Copyright 2011 The Android Open Source Project
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

#ifndef __GL_ERROR_LOG_H__
#define __GL_ERROR_LOG_H__

#include "ErrorLog.h"

#ifdef CHECK_GL_ERROR
void dbg(){}
#define GET_GL_ERROR(gl)  \
    {   \
        int err = gl.glGetError();    \
        if (err) { dbg(); ERR("Error: 0x%X in %s (%s:%d)\n", err, __FUNCTION__, __FILE__, __LINE__); }  \
    }

#else
#define GET_GL_ERROR(gl)
#endif

#endif //__GL_ERROR_LOG_H__
