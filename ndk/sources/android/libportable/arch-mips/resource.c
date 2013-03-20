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
#include <sys/resource.h>
#include <resource_portable.h>

#if RLIMIT_NOFILE_PORTABLE==RLIMIT_NOFILE
#error Bad build environment
#endif

static inline int mips_change_resource(int resource)
{
    switch(resource) {
    case RLIMIT_NOFILE_PORTABLE:
        return RLIMIT_NOFILE;
    case RLIMIT_AS_PORTABLE:
        return RLIMIT_AS;
    case RLIMIT_RSS_PORTABLE:
        return RLIMIT_RSS;
    case RLIMIT_NPROC_PORTABLE:
        return RLIMIT_NPROC;
    case RLIMIT_MEMLOCK_PORTABLE:
        return RLIMIT_MEMLOCK;
    }
    return resource;
}

extern int REAL(getrlimit)(int resource, struct rlimit *rlp);
int WRAP(getrlimit)(int resource, struct rlimit *rlp)
{
    return REAL(getrlimit)(mips_change_resource(resource), rlp);
}

extern int REAL(setrlimit)(int resource, const struct rlimit *rlp);
int WRAP(setrlimit)(int resource, const struct rlimit *rlp)
{
    return REAL(setrlimit)(mips_change_resource(resource), rlp);
}
