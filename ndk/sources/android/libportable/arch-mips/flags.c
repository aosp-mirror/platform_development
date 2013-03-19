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
#include <stdio.h>
#include <fcntl.h>
#include <fcntl_portable.h>

#define PORTABLE_TAG "flags_portable"
#include <log_portable.h>


/* __sflags is an internal bionic routine but the symbol is exported and there are callers... */
extern int __sflags(const char *, int *);

int
WRAP(__sflags)(const char *mode, int *optr)
{
    int rv;
    int nflags, pflags;

    ALOGV(" ");
    ALOGV("%s(mode:%p, optr:%p) {", __func__, mode, optr);

    rv = __sflags(mode, &nflags);

    /* error - no change to *optr */
    if (rv == 0)
        goto done;

    pflags = nflags & O_ACCMODE;
    if (nflags & O_CREAT)
        pflags |= O_CREAT_PORTABLE;
    if (nflags & O_TRUNC)
        pflags |= O_TRUNC_PORTABLE;
    if (nflags & O_APPEND)
        pflags |= O_APPEND_PORTABLE;

    /* Set *optr to portable flags */
    *optr = pflags;

done:
    ALOGV("%s: return(rv:%d); }", __func__, rv);
    return rv;
}
