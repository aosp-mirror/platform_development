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

#ifndef _EPOLL_PORTABLE_H_
#define _EPOLL_PORTABLE_H_

/*
 * GDK's compiler generates paddings to guarantee 8-byte alignment on
 * struct and 64bit POD types. If compilers on your platform have no such
 * alignment rule, please use the following struct and convert it into your
 * native struct form.
 */
struct epoll_event_portable
{
  unsigned int events;
  unsigned char __padding[4];
  epoll_data_t data;
};

#endif /* _EPOLL_PORTABLE_H */
