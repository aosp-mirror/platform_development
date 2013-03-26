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

static inline int mips_change_except(int flags)
{
    int mipsflags = 0;
    int exception = flags & FE_ALL_EXCEPT_PORTABLE;

    // exception flags
    if (exception & FE_INVALID_PORTABLE)
        mipsflags |= FE_INVALID;
    if (exception & FE_DIVBYZERO_PORTABLE)
        mipsflags |= FE_DIVBYZERO;
    if (exception & FE_OVERFLOW_PORTABLE)
        mipsflags |= FE_OVERFLOW;
    if (exception & FE_UNDERFLOW_PORTABLE)
        mipsflags |= FE_UNDERFLOW;
    if (exception & FE_INEXACT_PORTABLE)
        mipsflags |= FE_INEXACT;

    return mipsflags;
}

static inline int mips_change_rounding(int flags)
{
    int mipsflags = 0;
    int rounding = flags & 0x03;

    // rounding flags
    switch(rounding)
    {
      case FE_TONEAREST_PORTABLE:
        mipsflags = FE_TONEAREST;
        break;
      case FE_DOWNWARD_PORTABLE:
        mipsflags = FE_DOWNWARD;
        break;
      case FE_UPWARD_PORTABLE:
        mipsflags = FE_UPWARD;
        break;
      case FE_TOWARDZERO_PORTABLE:
        mipsflags = FE_TOWARDZERO;
        break;
    }
    return mipsflags;
}

static inline int mips_get_except(int mipsflags)
{
    int flags = 0;
    int exception = mipsflags & FE_ALL_EXCEPT;

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

static inline int mips_get_rounding(int mipsflags)
{
    int flags = 0;
    int rounding = mipsflags & _FCSR_RMASK;

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

int WRAP(fegetenv)(fenv_t* __envp) {
   fenv_t _fcsr = 0;
#ifdef  __mips_hard_float
   __asm__ __volatile__("cfc1 %0,$31" : "=r" (_fcsr));
#endif
   *__envp = _fcsr;
   return 0;
}

int WRAP(fesetenv)(const fenv_t* __envp) {
  fenv_t _fcsr = *__envp;
#ifdef  __mips_hard_float
  __asm__ __volatile__("ctc1 %0,$31" : : "r" (_fcsr));
#endif
  return 0;
}

int WRAP(feclearexcept)(int __excepts) {
  __excepts = mips_change_except(__excepts);
  fexcept_t __fcsr;
  WRAP(fegetenv)(&__fcsr);
  __excepts &= FE_ALL_EXCEPT;
  __fcsr &= ~(__excepts | (__excepts << _FCSR_CAUSE_SHIFT));
  WRAP(fesetenv)(&__fcsr);
  return 0;
}

int WRAP(fegetexceptflag)(fexcept_t* __flagp, int __excepts) {
  __excepts = mips_change_except(__excepts);
  fexcept_t __fcsr;
  WRAP(fegetenv)(&__fcsr);
  *__flagp = mips_get_except(__fcsr & __excepts & FE_ALL_EXCEPT);
  return 0;
}

int WRAP(fesetexceptflag)(const fexcept_t* __flagp, int __excepts) {
  int __flagp_ = mips_change_except(*__flagp);
  __excepts = mips_change_except(__excepts);
  fexcept_t __fcsr;
  WRAP(fegetenv)(&__fcsr);
  /* Ensure that flags are all legal */
  __excepts &= FE_ALL_EXCEPT;
  __fcsr &= ~__excepts;
  __fcsr |= __flagp_ & __excepts;
  WRAP(fesetenv)(&__fcsr);
  return 0;
}

int WRAP(feraiseexcept)(int __excepts) {
  __excepts = mips_change_except(__excepts);
  fexcept_t __fcsr;
  WRAP(fegetenv)(&__fcsr);
  /* Ensure that flags are all legal */
  __excepts &= FE_ALL_EXCEPT;
  /* Cause bit needs to be set as well for generating the exception*/
  __fcsr |= __excepts | (__excepts << _FCSR_CAUSE_SHIFT);
  WRAP(fesetenv)(&__fcsr);
  return 0;
}

int WRAP(fetestexcept)(int __excepts) {
   __excepts = mips_change_except(__excepts);
  fexcept_t __FCSR;
  WRAP(fegetenv)(&__FCSR);
  return mips_get_except(__FCSR & __excepts & FE_ALL_EXCEPT);
}

int WRAP(fegetround)(void) {
  fenv_t _fcsr;
  WRAP(fegetenv)(&_fcsr);
  return mips_get_rounding(_fcsr & _FCSR_RMASK);
}

int WRAP(fesetround)(int __round) {
  __round = mips_change_rounding(__round);
  fenv_t _fcsr;
  WRAP(fegetenv)(&_fcsr);
  _fcsr &= ~_FCSR_RMASK;
  _fcsr |= (__round & _FCSR_RMASK );
  WRAP(fesetenv)(&_fcsr);
  return 0;
}

int WRAP(feholdexcept)(fenv_t* __envp) {
  fenv_t __env;
  WRAP(fegetenv)(&__env);
  *__envp = __env;
  __env &= ~(FE_ALL_EXCEPT | _FCSR_ENABLE_MASK);
  WRAP(fesetenv)(&__env);
  return 0;
}

int WRAP(feupdateenv)(const fenv_t* __envp) {
  fexcept_t __fcsr;
  WRAP(fegetenv)(&__fcsr);
  WRAP(fesetenv)(__envp);
  WRAP(feraiseexcept)(__fcsr & FE_ALL_EXCEPT);
  return 0;
}

