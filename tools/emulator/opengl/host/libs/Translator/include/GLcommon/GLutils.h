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
#ifndef GL_UTILS_H
#define GL_UTILS_H

typedef enum{
             GLES_1_1 = 1,
             GLES_2_0 = 2,
             MAX_GLES_VERSION //Must be last
            }GLESVersion;

template <class T>
void swap(T& x,T& y) {
     T temp;
     temp=x;
     x=y;
     y=temp;
}

bool isPowerOf2(int num);

#endif
