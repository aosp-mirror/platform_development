/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <sys/types.h>
#include <sys/socket.h>

extern int setsockopt(int, int, int, const void *, socklen_t);
int setsockopt_portable(int s, int level, int optname, const void *optval, socklen_t optlen)
{
    return setsockopt(s, level, optname, optval, optlen);
}

extern int getsockopt (int, int, int, void *, socklen_t *);
int getsockopt_portable(int s, int level, int optname, void *optval, socklen_t *optlen)
{
    return getsockopt(s, level, optname, optval, optlen);
}
