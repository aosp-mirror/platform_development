#ifndef GLES_V2_CONTEXT_H
#define GLES_V2_CONTEXT_H

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

#include <GLcommon/GLDispatch.h>
#include <GLcommon/GLEScontext.h>
#include <GLcommon/objectNameManager.h>
#include <utils/threads.h>



class GLESv2Context : public GLEScontext{
public:
    void init();
    GLESv2Context();
    void convertArrs(GLESFloatArrays& fArrs,GLint first,GLsizei count,GLenum type,const GLvoid* indices,bool direct);
private:
    void sendArr(GLvoid* arr,GLenum arrayType,GLint size,GLsizei stride,int pointsIndex = -1);
    void initExtensionString();
};

#endif
