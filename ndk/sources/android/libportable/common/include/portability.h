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

#ifndef _PORTABILITY_H_
#define _PORTABILITY_H_

#include <stdint.h>
#include "asm-generic/portability.h"
/*
 * Common portability helper routines
 */

/*
 * Check a portable pointer before we access it
 * Well behaved programs should not be passing bad pointers
 * to the kernel but this routine can be used to check a pointer
 * if we need to use it before calling the kernel
 *
 * It does not catch every possible case but it is sufficient for LTP
 */
inline static int invalid_pointer(void *p)
{
    return p == 0
        || p == (void *)-1
#ifdef __mips__
        || (intptr_t)p < 0
#endif
        ;
}

/*
 * Hidden functions are exposed while linking the libportable shared object
 * but are not exposed thereafter.
 */
#define __hidden __attribute__((visibility("hidden")))

#endif /* _PORTABILITY_H_ */
