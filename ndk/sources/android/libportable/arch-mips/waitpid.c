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

/*
 * Converts native status information at *status to portable.
 */
static void status_ntop(int *status)
{
    int portable_status;

    ALOGV("%s(status:%p) {", __func__,
              status);

    ASSERT(status != NULL);

    /*
     * The interpretation of status is documented in the wait(2) manual page
     * and the implementation is in bionic/libc/include/sys/wait.h
     */
    if (WIFSIGNALED(*status))
        portable_status = (*status & ~0x7f) | signum_ntop(WTERMSIG(*status));
    else if (WIFSTOPPED(*status))
        portable_status = (*status & ~0xff00) | (signum_ntop(WSTOPSIG(*status)) << 8);
    else
        portable_status = *status;

    ALOGV("%s: (*status):0x%08x = portable_status:0x%08x", __func__,
               *status,           portable_status);

    *status = portable_status;

    ALOGV("%s: return; }", __func__);
}


pid_t WRAP(waitpid)(pid_t pid, int *status, int options)
{
    pid_t rv;

    ALOGV("%s(pid:%d, status:%p, options:0x%x) {", __func__,
              pid,    status,    options);

    rv = REAL(waitpid)(pid, status, options);
    if (rv > 0 && status)
        status_ntop(status);

    ALOGV("%s: return rv:%d; }", __func__, rv);
    return rv;
}


pid_t WRAP(wait)(int *status)
{
    pid_t rv;

    ALOGV("%s(status:%p) {", __func__,
              status);

    rv = REAL(wait)(status);
    if (rv > 0 && status)
        status_ntop(status);

    ALOGV("%s: return rv:%d; }", __func__, rv);
    return rv;
}


pid_t WRAP(wait3)(int *status, int options, struct rusage *rusage)
{
    pid_t rv;

    ALOGV("%s(status:%p, options:0x%x, rusage:%p) {", __func__,
              status,    options,      rusage);

    rv = REAL(wait3)(status, options, rusage);
    if (rv > 0 && status)
        status_ntop(status);

    ALOGV("%s: return rv:%d; }", __func__, rv);
    return rv;
}

// FIXME: WORKAROUND after Android wait4 has been implemented
pid_t REAL(wait4)(pid_t p, int *s, int o, struct rusage *r) {
  extern pid_t  __wait4(pid_t, int *, int, struct rusage *);
  return __wait4(p,s,o,r);
}

pid_t WRAP(wait4)(pid_t pid, int *status, int options, struct rusage *rusage)
{
    pid_t rv;

    ALOGV("%s(pid:%d, status:%p, options:0x%x, rusage:%p) {", __func__,
              pid,    status,    options,      rusage);

    rv = REAL(wait4)(pid, status, options, rusage);
    if (rv > 0 && status)
        status_ntop(status);

    ALOGV("%s: return rv:%d; }", __func__, rv);
    return rv;
}
