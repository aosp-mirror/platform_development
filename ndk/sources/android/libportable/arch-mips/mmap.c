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

#include <unistd.h>
#include <stdio.h>
#include <errno.h>
#include <sys/mman.h>
#include <mman_portable.h>

#if MAP_ANONYMOUS_PORTABLE==MAP_ANONYMOUS
#error Bad build environment
#endif

static inline int mips_change_prot(int prot)
{
    /* Only PROT_SEM is different */
    if (prot & PROT_SEM_PORTABLE) {
        prot &= ~PROT_SEM_PORTABLE;
        prot |= PROT_SEM;
    }

    return prot;
}

static inline int mips_change_flags(int flags)
{
    int mipsflags = 0;
    /* These are the documented flags for mmap */
    if (flags & MAP_SHARED_PORTABLE)
       mipsflags |= MAP_SHARED;
    if (flags & MAP_PRIVATE_PORTABLE)
       mipsflags |= MAP_PRIVATE;
#if defined(MAP_32BIT_PORTABLE) && defined(MAP_32BIT)
    if (flags & MAP_32BIT_PORTABLE)
       mipsflags |= MAP_32BIT;
#endif
    if (flags & MAP_ANONYMOUS_PORTABLE)
       mipsflags |= MAP_ANONYMOUS;
    if (flags & MAP_FIXED_PORTABLE)
       mipsflags |= MAP_FIXED;
    if (flags & MAP_GROWSDOWN_PORTABLE)
       mipsflags |= MAP_GROWSDOWN;
#if defined(MAP_HUGETLB_PORTABLE) && defined(MAP_HUGETLB)
    if (flags & MAP_HUGETLB_PORTABLE)
       mipsflags |= MAP_HUGETLB;
#endif
    if (flags & MAP_LOCKED_PORTABLE)
       mipsflags |= MAP_LOCKED;
    if (flags & MAP_NONBLOCK_PORTABLE)
       mipsflags |= MAP_NONBLOCK;
    if (flags & MAP_NORESERVE_PORTABLE)
       mipsflags |= MAP_NORESERVE;
    if (flags & MAP_POPULATE_PORTABLE)
       mipsflags |= MAP_POPULATE;
#if defined(MAP_STACK_PORTABLE) && defined(MAP_STACK)
    if (flags & MAP_STACK_PORTABLE)
       mipsflags |= MAP_STACK;
#endif

    return mipsflags;
}

#define  MMAP2_SHIFT  12
extern void *__mmap2(void *, size_t, int, int, int, size_t);
void *mmap_portable(void *addr, size_t size, int prot, int flags, int fd, long offset)
{
    if ( offset & ((1UL << MMAP2_SHIFT)-1) ) {
        errno = EINVAL;
        return MAP_FAILED;
    }

    return __mmap2(addr, size, mips_change_prot(prot), mips_change_flags(flags),
                   fd, (size_t)offset >> MMAP2_SHIFT);
}
