/*
 * Copyright 2013, The Android Open Source Project
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

#include <sys/cdefs.h>
#include <sys/types.h>
#include <fenv.h>
#include <fenv_portable.h>

static inline int x86_change_except(int flags)
{
    int x86flags = 0;
    int exception = flags & FE_ALL_EXCEPT_PORTABLE;

    // exception flags
    if (exception & FE_INVALID_PORTABLE)
        x86flags |= FE_INVALID;
    if (exception & FE_DIVBYZERO_PORTABLE)
        x86flags |= FE_DIVBYZERO;
    if (exception & FE_OVERFLOW_PORTABLE)
        x86flags |= FE_OVERFLOW;
    if (exception & FE_UNDERFLOW_PORTABLE)
        x86flags |= FE_UNDERFLOW;
    if (exception & FE_INEXACT_PORTABLE)
        x86flags |= FE_INEXACT;

    return x86flags;
}

static inline int x86_change_rounding(int flags)
{
    int x86flags = 0;
    int rounding = flags & 0x03;

    // rounding flags
    switch(rounding)
    {
        case FE_TONEAREST_PORTABLE:
            x86flags = FE_TONEAREST;
            break;
        case FE_DOWNWARD_PORTABLE:
            x86flags = FE_DOWNWARD;
            break;
        case FE_UPWARD_PORTABLE:
            x86flags = FE_UPWARD;
            break;
        case FE_TOWARDZERO_PORTABLE:
            x86flags = FE_TOWARDZERO;
            break;
    }
    return x86flags;
}

static inline int x86_get_except(int x86flags)
{
    int flags = 0;
    int exception = x86flags & FE_ALL_EXCEPT;

    // exception flags
    if (exception & FE_INVALID)
        flags |= FE_INVALID_PORTABLE;
    if (exception & FE_DIVBYZERO)
        flags |= FE_DIVBYZERO_PORTABLE;
    if (exception & FE_OVERFLOW)
        flags |= FE_OVERFLOW_PORTABLE;
    if (exception & FE_UNDERFLOW)
        flags |= FE_UNDERFLOW_PORTABLE;
    if (exception & FE_INEXACT)
        flags |= FE_INEXACT_PORTABLE;

    return flags;
}
static inline int x86_get_rounding(int x86flags)
{
    int flags = 0;
    int rounding = x86flags & _ROUND_MASK;

    // rounding flags
    switch(rounding)
    {
        case FE_TONEAREST:
            flags = FE_TONEAREST_PORTABLE;
            break;
        case FE_DOWNWARD:
            flags = FE_DOWNWARD_PORTABLE;
            break;
        case FE_UPWARD:
            flags = FE_UPWARD_PORTABLE;
            break;
        case FE_TOWARDZERO:
            flags = FE_TOWARDZERO_PORTABLE;
            break;
    }

    return flags;
}

int
WRAP(fesetexceptflag)(const fexcept_t *flagp, int excepts)
{
    const fexcept_t flagp_ = x86_change_except(*flagp);
    int excepts_ = x86_change_except(excepts);
    return REAL(fesetexceptflag)(&flagp_, excepts_);
}

int
WRAP(fegetexceptflag)(fexcept_t *flagp, int excepts)
{
    REAL(fegetexceptflag)(flagp, x86_change_except(excepts));
    *flagp = x86_get_except(*flagp);
    return 0;
}

int
WRAP(feraiseexcept)(int excepts)
{
    return REAL(feraiseexcept)(x86_change_except(excepts));
}

int
WRAP(feclearexcept)(int excepts)
{
    return REAL(feclearexcept)(x86_change_except(excepts));
}

int
WRAP(fetestexcept)(int excepts)
{
    int ret = REAL(fetestexcept)(x86_change_except(excepts));
    return x86_get_except(ret);
}

int
WRAP(fegetround)(void)
{
    int round = REAL(fegetround)();
    return x86_get_rounding(round);
}

int
WRAP(fesetround)(int round)
{
    return REAL(fesetround)(x86_change_rounding(round));
}

int
WRAP(fegetexcept)(void)
{
    int flags = REAL(fegetexcept)();
    return x86_get_except(flags);
}

