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

#include <portability.h>
#include <unistd.h>
#include <errno.h>
#include <sys/mman.h>
#include <mman_portable.h>

#if MAP_ANONYMOUS_PORTABLE==MAP_ANONYMOUS
#error Bad build environment
#endif

#define PORTABLE_TAG "mmap_portable"
#include <log_portable.h>

static inline int mmap_prot_pton(int portable_prot)
{
    int native_prot = portable_prot;

    ALOGV("%s(portable_prot:0x%x) {", __func__, portable_prot);

    /* Only PROT_SEM is different */
    if (portable_prot & PROT_SEM_PORTABLE) {
        native_prot &= ~PROT_SEM_PORTABLE;
        native_prot |= PROT_SEM;
    }

    ALOGV("%s: return(native_prot:0x%x); }", __func__, native_prot);
    return native_prot;
}


static inline int mmap_flags_pton(int portable_flags)
{
    int native_flags = 0;

    ALOGV("%s(portable_flags:0x%x) {", __func__, portable_flags);

    if (portable_flags & MAP_SHARED_PORTABLE) {
       native_flags |= MAP_SHARED;
    }
    if (portable_flags & MAP_PRIVATE_PORTABLE) {
       native_flags |= MAP_PRIVATE;
    }
    if (portable_flags & MAP_FIXED_PORTABLE) {
       native_flags |= MAP_FIXED;
    }
    if (portable_flags & MAP_ANONYMOUS_PORTABLE) {
       native_flags |= MAP_ANONYMOUS;
    }
    if (portable_flags & MAP_GROWSDOWN_PORTABLE) {
       native_flags |= MAP_GROWSDOWN;
    }
    if (portable_flags & MAP_DENYWRITE_PORTABLE) {
       native_flags |= MAP_DENYWRITE;
    }
    if (portable_flags & MAP_EXECUTABLE_PORTABLE) {
       native_flags |= MAP_EXECUTABLE;
    }
    if (portable_flags & MAP_LOCKED_PORTABLE) {
       native_flags |= MAP_LOCKED;
    }
    if (portable_flags & MAP_NORESERVE_PORTABLE) {
       native_flags |= MAP_NORESERVE;
    }
    if (portable_flags & MAP_POPULATE_PORTABLE) {
       native_flags |= MAP_POPULATE;
    }
    if (portable_flags & MAP_NONBLOCK_PORTABLE) {
       native_flags |= MAP_NONBLOCK;
    }

    ALOGV("%s: return(native_flags:0x%x); }", __func__, native_flags);
    return native_flags;
}

extern void* REAL(mmap)(void *, size_t, int, int, int, off_t);
void *WRAP(mmap)(void *addr, size_t size, int prot, int flags, int fd, long byte_offset)
{
    int native_prot, native_flags;
    int saved_errno;
    void *ret_addr;

    ALOGV(" ");
    ALOGV("%s(addr:%p, size:%d, prot:0x%x, flags:0x%x, fd:%d, byte_offset:0x%lx) {", __func__,
              addr,    size,    prot,      flags,      fd,    byte_offset);

    native_prot = mmap_prot_pton(prot);
    native_flags = mmap_flags_pton(flags);

    ret_addr = REAL(mmap)(addr, size, native_prot, native_flags, fd, byte_offset);

    ALOGV("%s: return(ret_addr:%p); }", __func__, ret_addr);
    return ret_addr;
}


extern int mprotect(const void *, size_t, int);

int WRAP(mprotect)(const void *addr, size_t size, int portable_prot)
{
    int rv;
    int native_prot;

    ALOGV(" ");
    ALOGV("%s(addr:%p, size:%d, portable_prot:0x%x); {", __func__,
              addr,    size,    portable_prot);

    native_prot = mmap_prot_pton(portable_prot);

    rv = REAL(mprotect)(addr, size, native_prot);

    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}
