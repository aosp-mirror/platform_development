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
#include <signal.h>
#include <signal_portable.h>
#include <time.h>

int WRAP(timer_create)(clockid_t clockid, struct sigevent *portable_evp,
                          timer_t *timerid)
{
    struct sigevent native_sigevent, *evp = portable_evp;

    if (!invalid_pointer(portable_evp) &&
        (evp->sigev_notify == SIGEV_SIGNAL ||
         evp->sigev_notify == SIGEV_THREAD_ID)) {

        native_sigevent = *portable_evp;
        evp = &native_sigevent;
        evp->sigev_signo = signum_pton(evp->sigev_signo);
    }
    return REAL(timer_create)(clockid, evp, timerid);
}
