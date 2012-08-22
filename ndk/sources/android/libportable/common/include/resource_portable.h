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

#ifndef _RESOURCE_PORTABLE_H_
#define _RESOURCE_PORTABLE_H_

/* Derived from development/ndk/platforms/android-3/include/asm-generic/resource.h */

#define RLIMIT_CPU_PORTABLE 0
#define RLIMIT_FSIZE_PORTABLE 1
#define RLIMIT_DATA_PORTABLE 2
#define RLIMIT_STACK_PORTABLE 3
#define RLIMIT_CORE_PORTABLE 4

#ifndef RLIMIT_RSS_PORTABLE
#define RLIMIT_RSS_PORTABLE 5
#endif

#ifndef RLIMIT_NPROC_PORTABLE
#define RLIMIT_NPROC_PORTABLE 6
#endif

#ifndef RLIMIT_NOFILE_PORTABLE
#define RLIMIT_NOFILE_PORTABLE 7
#endif

#ifndef RLIMIT_MEMLOCK_PORTABLE
#define RLIMIT_MEMLOCK_PORTABLE 8
#endif

#ifndef RLIMIT_AS_PORTABLE
#define RLIMIT_AS_PORTABLE 9
#endif

#define RLIMIT_LOCKS_PORTABLE 10
#define RLIMIT_SIGPENDING_PORTABLE 11
#define RLIMIT_MSGQUEUE_PORTABLE 12
#define RLIMIT_NICE_PORTABLE 13
#define RLIMIT_RTPRIO_PORTABLE 14
#define RLIMIT_RTTIME_PORTABLE 15

#define RLIM_NLIMITS_PORTABLE 16

#ifndef RLIM_INFINITY_PORTABLE
#define RLIM_INFINITY_PORTABLE (~0UL)
#endif

#ifndef _STK_LIM_MAX_PORTABLE
#define _STK_LIM_MAX_PORTABLE RLIM_INFINITY_PORTABLE
#endif

#endif /* _RESOURCE_PORTABLE_H_ */
