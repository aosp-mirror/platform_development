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


#include <portability.h>

#include <sys/cdefs.h>
#include <sys/types.h>
#include <fenv.h>
#include <fenv_portable.h>

#ifndef _ROUND_MASK
#define FE_TONEAREST  0x0000
#define FE_DOWNWARD   0x0400
#define FE_UPWARD     0x0800
#define FE_TOWARDZERO 0x0c00
#define _ROUND_MASK   (FE_TONEAREST | FE_DOWNWARD | \
                       FE_UPWARD | FE_TOWARDZERO)
#endif

static inline int x86_64_change_except(int flag) {
  int x86_64flag = 0;
  int exception = flag & FE_ALL_EXCEPT_PORTABLE;

  if (exception & FE_INVALID_PORTABLE)
      x86_64flag |= FE_INVALID;
  if (exception & FE_DIVBYZERO_PORTABLE)
      x86_64flag |= FE_DIVBYZERO;
  if (exception & FE_OVERFLOW_PORTABLE)
      x86_64flag |= FE_OVERFLOW;
  if (exception & FE_UNDERFLOW_PORTABLE)
      x86_64flag |= FE_UNDERFLOW;
  if (exception & FE_INEXACT_PORTABLE)
      x86_64flag |= FE_INEXACT;

  return x86_64flag;
}

static inline int x86_64_change_rounding(int flag) {
  int x86_64flag = 0;
  int rounding = flag & 0x03;

  switch(rounding) {
    case FE_TONEAREST_PORTABLE: {
      x86_64flag = FE_TONEAREST;
      break;
    }
    case FE_DOWNWARD_PORTABLE: {
      x86_64flag = FE_DOWNWARD;
      break;
    }
    case FE_UPWARD_PORTABLE: {
      x86_64flag = FE_UPWARD;
      break;
    }
    case FE_TOWARDZERO_PORTABLE: {
      x86_64flag = FE_TOWARDZERO;
      break;
    }
  }

  return x86_64flag;
}

static inline int x86_64_get_except(int x86_64flag) {
  int flag = 0;
  int exception = x86_64flag & FE_ALL_EXCEPT;

  if (exception & FE_INVALID)
      flag |= FE_INVALID_PORTABLE;
  if (exception & FE_DIVBYZERO)
      flag |= FE_DIVBYZERO_PORTABLE;
  if (exception & FE_OVERFLOW)
      flag |= FE_OVERFLOW_PORTABLE;
  if (exception & FE_UNDERFLOW)
      flag |= FE_UNDERFLOW_PORTABLE;
  if (exception & FE_INEXACT)
      flag |= FE_INEXACT_PORTABLE;

  return flag;
}

static inline int x86_64_get_rounding(int x86_64flag) {
  int flag = 0;
  int rounding = x86_64flag & _ROUND_MASK;

  switch(rounding) {
    case FE_TONEAREST: {
      flag = FE_TONEAREST_PORTABLE;
      break;
    }
    case FE_DOWNWARD: {
      flag = FE_DOWNWARD_PORTABLE;
      break;
    }
    case FE_UPWARD: {
      flag = FE_UPWARD_PORTABLE;
      break;
    }
    case FE_TOWARDZERO: {
      flag = FE_TOWARDZERO_PORTABLE;
      break;
    }
  }

  return flag;
}


int WRAP(feclearexcept)(int flag) {
  return REAL(feclearexcept)(x86_64_change_except(flag));
}

#ifdef __LP64__
int WRAP(fegetexceptflag)(fexcept_t_portable *obj, int flag) {
  int ret = REAL(fegetexceptflag)((fexcept_t*)obj, x86_64_change_except(flag));
  *obj = (fexcept_t_portable) x86_64_get_except(*obj);
  return ret;
}

int WRAP(fesetexceptflag)(const fexcept_t_portable *obj, int flag) {
  const fexcept_t x86_64obj = x86_64_change_except(*obj);
  int x86_64flag = x86_64_change_except(flag);
  return REAL(fesetexceptflag)(&x86_64obj, x86_64flag);
}
#endif

int WRAP(feraiseexcept)(int flag) {
  return REAL(feraiseexcept)(x86_64_change_except(flag));
}


int WRAP(fetestexcept)(int flag) {
  int ret = REAL(fetestexcept)(x86_64_change_except(flag));
  return x86_64_get_except(ret);
}

int WRAP(fegetround)(void) {
  int round = REAL(fegetround)();
  return x86_64_get_rounding(round);
}

int WRAP(fesetround)(int round) {
  return REAL(fesetround)(x86_64_change_rounding(round));
}

#ifdef __LP64__
int WRAP(fegetenv)(fenv_t_portable *obj) {
  return REAL(fegetenv)((fenv_t*)obj);
}

int WRAP(feholdexcept)(fenv_t_portable *obj) {
  return REAL(feholdexcept)((fenv_t*)obj);
}

int WRAP(fesetenv)(const fenv_t_portable *obj) {
  return REAL(fesetenv)((const fenv_t*)obj);
}

int WRAP(feupdateenv)(const fenv_t_portable *obj) {
  return REAL(feupdateenv)((const fenv_t*)obj);
}
#endif

int WRAP(fegetexcept)(void) {
  int flag = REAL(fegetexcept)();
  return x86_64_get_except(flag);
}

