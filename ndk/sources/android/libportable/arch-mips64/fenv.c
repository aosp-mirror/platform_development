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
#include <sys/types.h>
#include <fenv.h>
#include <fenv_portable.h>

static inline int mips64_change_except(int flags) {
  int mips64flags = 0;
  int exception = flags & FE_ALL_EXCEPT_PORTABLE;

  // exception flags
  if (exception & FE_INVALID_PORTABLE)
      mips64flags |= FE_INVALID;
  if (exception & FE_DIVBYZERO_PORTABLE)
      mips64flags |= FE_DIVBYZERO;
  if (exception & FE_OVERFLOW_PORTABLE)
      mips64flags |= FE_OVERFLOW;
  if (exception & FE_UNDERFLOW_PORTABLE)
      mips64flags |= FE_UNDERFLOW;
  if (exception & FE_INEXACT_PORTABLE)
      mips64flags |= FE_INEXACT;

  return mips64flags;
}

static inline int mips64_change_rounding(int flags) {
  int mips64flags = 0;
  int rounding = flags & 0x03;

  // rounding flags
  switch(rounding)
  {
    case FE_TONEAREST_PORTABLE:
      mips64flags = FE_TONEAREST;
      break;
    case FE_DOWNWARD_PORTABLE:
      mips64flags = FE_DOWNWARD;
      break;
    case FE_UPWARD_PORTABLE:
      mips64flags = FE_UPWARD;
      break;
    case FE_TOWARDZERO_PORTABLE:
      mips64flags = FE_TOWARDZERO;
      break;
  }
  return mips64flags;
}

static inline int mips64_get_except(int mips64flags) {
  int flags = 0;
  int exception = mips64flags & FE_ALL_EXCEPT;

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

static inline int mips64_get_rounding(int mips64flags) {
  int flags = 0;
  int rounding = mips64flags & _FCSR_RMASK;

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


int WRAP(feclearexcept)(int flag) {
  return REAL(feclearexcept)(mips64_change_except(flag));
}

int WRAP(fegetexceptflag)(fexcept_t_portable *obj, int flag) {
  int ret = REAL(fegetexceptflag)((fexcept_t*)obj, mips64_change_except(flag));
  *obj = (fexcept_t_portable) mips64_get_except(*obj);
  return ret;
}

int WRAP(feraiseexcept)(int flag) {
  return REAL(feraiseexcept)(mips64_change_except(flag));
}

int WRAP(fesetexceptflag)(const fexcept_t_portable *obj, int flag) {
  const fexcept_t mips64obj = mips64_change_except(*obj);
  int mips64flag = mips64_change_except(flag);
  return REAL(fesetexceptflag)(&mips64obj, mips64flag);
}

int WRAP(fetestexcept)(int flag) {
  int ret = REAL(fetestexcept)(mips64_change_except(flag));
  return mips64_get_except(ret);
}

int WRAP(fegetround)(void) {
  int round = REAL(fegetround)();
  return mips64_get_rounding(round);
}

int WRAP(fesetround)(int round) {
  return REAL(fesetround)(mips64_change_rounding(round));
}

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

int WRAP(fegetexcept)(void) {
  int flag = REAL(fegetexcept)();
  return mips64_get_except(flag);
}

