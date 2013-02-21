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

#define _GNU_SOURCE
#include <sched.h>
#include <stdarg.h>
#include <stdlib.h>
#include <signal.h>
#include <signal_portable.h>
#include <portability.h>
#include <stdio.h>
#include <errno.h>

#define PORTABLE_TAG "clone_portable"
#include <log_portable.h>


/*
 * This function maps the clone function call defined in:
 *      $TOP/bionic/libc/bionic/bionic_clone.c
 *
 * which calls the __bionic_clone() system call which is defined in:
 *      $TOP/bionic/libc/unistd/arch-mips/bionic/clone.S
 *
 * We have to map the low byte of the 'flags' parameter which
 * contains the number of the termination signal sent to the
 * parent when the child dies.
 *
 * Note that if this signal is specified as anything other than
 * SIGCHLD, then the parent process must specify the __WALL or
 * __WCLONE options when waiting for the child with wait(2).
 *
 * If no signal is specified, then the parent process is not
 * signaled when the child terminates.
 */
int clone_portable(int (*fn)(void *), void *child_stack, int port_flags, void *arg, ...)
{
    va_list     args;
    int         ret;
    int         mips_flags;
    void        *new_tls = NULL;
    int         *child_tidptr = NULL;
    int         *parent_tidptr = NULL;
    int         mips_term_signum;
    char        *mips_term_signame;
    int         portable_term_signum;
    char        *portable_term_signame;

    ALOGV(" ");
    ALOGV("%s(fn:%p, child_stack:%p, port_flags:0x%x, arg:%p, ...) {", __func__,
              fn,    child_stack,    port_flags,      arg);

    /* Extract optional parameters - they are cumulative */
    va_start(args, arg);
    if (port_flags & (CLONE_PARENT_SETTID|CLONE_SETTLS|CLONE_CHILD_SETTID)) {
        parent_tidptr = va_arg(args, int*);
    }
    if (port_flags & (CLONE_SETTLS|CLONE_CHILD_SETTID)) {
        new_tls = va_arg(args, void*);
    }
    if (port_flags & CLONE_CHILD_SETTID) {
        child_tidptr = va_arg(args, int*);
    }
    va_end(args);

    /*
     * Map the LSB of the flags as explained above.
     */
    portable_term_signum = port_flags & 0xFF;
    if (portable_term_signum == 0) {
        mips_flags = port_flags;
    } else {
        portable_term_signame = map_portable_signum_to_name(portable_term_signum);
        mips_term_signum = signum_pton(portable_term_signum);
        mips_term_signame = map_mips_signum_to_name(mips_term_signum);
        mips_flags = (port_flags & ~0xFF) | (mips_term_signum & 0xFF);
    }

    ret = clone(fn, child_stack, mips_flags, arg, parent_tidptr,
                new_tls, child_tidptr);

    ALOGV("%s: return(ret:%d); }", __func__, ret);
    return ret;
}
