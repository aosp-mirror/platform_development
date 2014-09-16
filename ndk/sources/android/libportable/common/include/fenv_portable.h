/*
 * Copyright 2014, The Android Open Source Project
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

#ifndef _FENV_PORTABLE_H_
#define _FENV_PORTABLE_H_

#include <fenv.h>
#include <portability.h>
#include <stdint.h>
#include <stdlib.h>

typedef struct {
  unsigned char a[128];
} fenv_t_portable;
typedef uint32_t fexcept_t_portable;

/* Exception flags. */
#define FE_INVALID_PORTABLE    0x01
#define FE_DIVBYZERO_PORTABLE  0x02
#define FE_OVERFLOW_PORTABLE   0x04
#define FE_UNDERFLOW_PORTABLE  0x08
#define FE_INEXACT_PORTABLE    0x10
#define FE_ALL_EXCEPT_PORTABLE (FE_DIVBYZERO_PORTABLE | FE_INEXACT_PORTABLE | FE_INVALID_PORTABLE |\
                                FE_OVERFLOW_PORTABLE | FE_UNDERFLOW_PORTABLE)

/* Rounding modes. */
#define FE_TONEAREST_PORTABLE  0x0
#define FE_UPWARD_PORTABLE     0x1
#define FE_DOWNWARD_PORTABLE   0x2
#define FE_TOWARDZERO_PORTABLE 0x3


static inline int target_change_except(int flags)
{
    int targetflags = 0;

    if (flags & FE_INVALID_PORTABLE)
        targetflags |= FE_INVALID;
    if (flags & FE_DIVBYZERO_PORTABLE)
        targetflags |= FE_DIVBYZERO;
    if (flags & FE_OVERFLOW_PORTABLE)
        targetflags |= FE_OVERFLOW;
    if (flags & FE_UNDERFLOW_PORTABLE)
        targetflags |= FE_UNDERFLOW;
    if (flags & FE_INEXACT_PORTABLE)
        targetflags |= FE_INEXACT;

    return targetflags;
}

static inline int target_change_rounding(int flags)
{
    int targetflags = 0;

    switch(flags)
    {
      case FE_TONEAREST_PORTABLE:
        targetflags = FE_TONEAREST;
        break;
      case FE_DOWNWARD_PORTABLE:
        targetflags = FE_DOWNWARD;
        break;
      case FE_UPWARD_PORTABLE:
        targetflags = FE_UPWARD;
        break;
      case FE_TOWARDZERO_PORTABLE:
        targetflags = FE_TOWARDZERO;
        break;
    }
    return targetflags;
}

static inline int target_get_except(int targetflags)
{
    int flags = 0;

    if (targetflags & FE_INVALID)
        flags |= FE_INVALID_PORTABLE;
    if (targetflags & FE_DIVBYZERO)
        flags |= FE_DIVBYZERO_PORTABLE;
    if (targetflags & FE_OVERFLOW)
        flags |= FE_OVERFLOW_PORTABLE;
    if (targetflags & FE_UNDERFLOW)
        flags |= FE_UNDERFLOW_PORTABLE;
    if (targetflags & FE_INEXACT)
        flags |= FE_INEXACT_PORTABLE;
    return flags;
}

static inline int target_get_rounding(int targetflags)
{
    int flags = 0;

    switch(targetflags)
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


int WRAP(fegetenv)(fenv_t_portable* __envp) {
  return REAL(fegetenv)((fenv_t*) __envp);
}

int WRAP(fesetenv)(const fenv_t_portable* __envp) {
  return REAL(fesetenv)((fenv_t*) __envp);
}

int WRAP(feclearexcept)(int __excepts) {
  __excepts = target_change_except(__excepts);
  return REAL(feclearexcept)(__excepts);
}

int WRAP(fegetexceptflag)(fexcept_t_portable* __flagp, int __excepts) {
  __excepts = target_change_except(__excepts);
  int ret = REAL(fegetexceptflag)((fexcept_t*) __flagp, __excepts);
  *__flagp = target_get_except(*__flagp);
  return ret;
}

int WRAP(fesetexceptflag)(const fexcept_t_portable* __flagp, int __excepts) {
  __excepts = target_change_except(__excepts);
  return REAL(fesetexceptflag)((const fexcept_t*) __flagp, __excepts);
}

int WRAP(feraiseexcept)(int __excepts) {
  __excepts = target_change_except(__excepts);
  return REAL(feraiseexcept)(__excepts);
}

int WRAP(fetestexcept)(int __excepts) {
  __excepts = target_change_except(__excepts);
  return target_get_except(REAL(fetestexcept)(__excepts));
}

int WRAP(fegetround)(void) {
  int rounding = REAL(fegetround)();
  return target_get_rounding(rounding);
}

int WRAP(fesetround)(int __round) {
  __round = target_change_rounding(__round);
  return REAL(fesetround)(__round);
}

int WRAP(feholdexcept)(fenv_t_portable* __envp) {
  memset(__envp, '\0', sizeof(fenv_t_portable));
  fenv_t env;
  int ret = REAL(feholdexcept)(&env);
  memcpy(__envp, &env, sizeof(env));
  return ret;
}

int WRAP(feupdateenv)(const fenv_t_portable* __envp) {
  fenv_t env;
  memcpy(&env, __envp, sizeof(env));
  return REAL(feupdateenv)(&env);
}

int WRAP(feenableexcept)(int __excepts) {
  __excepts = target_change_except(__excepts);
  return REAL(feenableexcept)(__excepts);
}

int WRAP(fedisableexcept)(int __excepts) {
  __excepts = target_change_except(__excepts);
  return REAL(fedisableexcept)(__excepts);
}

#endif /* _FENV_PORTABLE_H_ */
