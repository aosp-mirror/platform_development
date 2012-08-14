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

#ifndef _MMAN_PORTABLE_H_
#define _MMAN_PORTABLE_H_

/* Derived from development/ndk/platforms/android-3/include/asm-generic/mman.h */
#define PROT_READ_PORTABLE     0x1
#define PROT_WRITE_PORTABLE    0x2
#define PROT_EXEC_PORTABLE     0x4
#define PROT_SEM_PORTABLE      0x8
#define PROT_NONE_PORTABLE     0x0
#define PROT_GROWSDOWN_PORTABLE 0x01000000
#define PROT_GROWSUP_PORTABLE   0x02000000

#define MAP_SHARED_PORTABLE    0x01
#define MAP_PRIVATE_PORTABLE   0x02
#define MAP_TYPE_PORTABLE      0x0f
#define MAP_FIXED_PORTABLE     0x10
#define MAP_ANONYMOUS_PORTABLE 0x20

#define MS_ASYNC_PORTABLE      1
#define MS_INVALIDATE_PORTABLE 2
#define MS_SYNC_PORTABLE       4

#define MADV_NORMAL_PORTABLE   0
#define MADV_RANDOM_PORTABLE   1
#define MADV_SEQUENTIAL_PORTABLE 2
#define MADV_WILLNEED_PORTABLE 3
#define MADV_DONTNEED_PORTABLE 4

#define MADV_REMOVE_PORTABLE   9
#define MADV_DONTFORK_PORTABLE 10
#define MADV_DOFORK_PORTABLE   11

#define MAP_ANON_PORTABLE      MAP_ANONYMOUS_PORTABLE
#define MAP_FILE_PORTABLE      0

/* Derived from development/ndk/platforms/android-3/include/asm-generic/mman.h */
#define MAP_GROWSDOWN_PORTABLE 0x0100
#define MAP_DENYWRITE_PORTABLE 0x0800
#define MAP_EXECUTABLE_PORTABLE        0x1000
#define MAP_LOCKED_PORTABLE    0x2000
#define MAP_NORESERVE_PORTABLE 0x4000
#define MAP_POPULATE_PORTABLE  0x8000
#define MAP_NONBLOCK_PORTABLE  0x10000

#define MCL_CURRENT_PORTABLE   1
#define MCL_FUTURE_PORTABLE    2

#endif /* _MMAN_PORTABLE_H */
