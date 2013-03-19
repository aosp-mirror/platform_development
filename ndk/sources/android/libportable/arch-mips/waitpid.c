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
#include <stdarg.h>
#include <stdlib.h>
#include <signal.h>
#include <signal_portable.h>
#include <stdio.h>
#include <sys/wait.h>

#define PORTABLE_TAG            "waitpid_portable"
#include <log_portable.h>

pid_t WRAP(waitpid)(pid_t pid, int *status, int options)
{
    pid_t ret;

    ret = REAL(waitpid)(pid, status, options);
    if (status && ret > 0) {
        /*
         * Status layout is identical, so just the signal
         * number needs to be changed.
         */
        if (WIFSIGNALED(*status))
            *status = (*status & ~0x7f) | signum_ntop(WTERMSIG(*status));
        else if (WIFSTOPPED(*status))
            *status = (*status & ~0xff00) | (signum_ntop(WSTOPSIG(*status)) << 8);
    }

    return ret;
}
