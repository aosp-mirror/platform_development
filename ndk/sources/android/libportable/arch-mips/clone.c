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
#include <portability.h>
#include <sched.h>
#include <stdarg.h>
#include <stdlib.h>
#include <signal.h>
#include <signal_portable.h>
#include <portability.h>
#include <stdio.h>
#include <errno.h>
#include <filefd_portable.h>

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
int WRAP(clone)(int (*fn)(void *), void *child_stack, int port_flags, void *arg, ...)
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
    int         cloning_vm = ((port_flags & CLONE_VM) == CLONE_VM);
    int         cloning_files = ((port_flags & CLONE_FILES) == CLONE_FILES);
    int         cloning_sighand = ((port_flags & CLONE_SIGHAND) == CLONE_SIGHAND);

    ALOGV(" ");
    ALOGV("%s(fn:%p, child_stack:%p, port_flags:0x%x, arg:%p, ...) {", __func__,
              fn,    child_stack,    port_flags,      arg);

    /* Shared file descriptor table requires shared memory. */
    if (cloning_files != cloning_vm) {
        ALOGE("%s: cloning_files:%d != cloning_vm:%d) ...", __func__,
                   cloning_files,      cloning_vm);

        ALOGE("%s: ... port_flags:0x%x Not Supported by Lib-Portable!", __func__,
                       port_flags);
    }

    /* Shared signal handler table requires shared memory. */
    if (cloning_sighand != cloning_vm) {
        ALOGE("%s: cloning_sighand:%d != cloning_vm:%d) ...",  __func__,
                   cloning_sighand,      cloning_vm);

        ALOGE("%s: ... port_flags:0x%x Not Supported by Lib-Portable!", __func__,
                       port_flags);
    }

    /* Extract optional parameters - they are cumulative. */
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
        ALOGV("%s: portable_term_signum:0x%x:'%s'", __func__,
                   portable_term_signum, portable_term_signame);
        mips_term_signum = signum_pton(portable_term_signum);
        mips_term_signame = map_mips_signum_to_name(mips_term_signum);
        ALOGV("%s: mips_term_signum:0x%x:'%s'", __func__,
                   mips_term_signum, mips_term_signame);
        mips_flags = (port_flags & ~0xFF) | (mips_term_signum & 0xFF);
    }
    ALOGV("%s: clone(%p, %p, 0x%x, %p, %p, %p, %p);", __func__,
           fn, child_stack, mips_flags, arg, parent_tidptr, new_tls, child_tidptr);

    ret = REAL(clone)(fn, child_stack, mips_flags, arg, parent_tidptr,
                new_tls, child_tidptr);

    if (ret > 0) {
        /*
         * Disable mapping in the parent if the child could interfere
         * and make things even worse than skipping the signal and
         * file read mapping.
         */
        if (cloning_files != cloning_vm) {
            filefd_disable_mapping();
        }
        if (cloning_sighand != cloning_vm) {
            signal_disable_mapping();
        }
    }

    ALOGV("%s: return(ret:%d); }", __func__, ret);
    return ret;
}
