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

#ifdef __ARM_ARCH_7A__

#include <portability.h>
#include <fenv.h>
#include <fenv_portable.h>

int WRAP(fegetenv)(fenv_t* __envp) {
    fenv_t _fpscr;
    __asm__ __volatile__("vmrs %0,fpscr" : "=r" (_fpscr));
    *__envp = _fpscr;
    return 0;
}

int WRAP(fesetenv)(const fenv_t* __envp) {
    fenv_t _fpscr = *__envp;
    __asm__ __volatile__("vmsr fpscr,%0" : :"ri" (_fpscr));
    return 0;
}

int WRAP(feclearexcept)(int __excepts) {
    fexcept_t __fpscr;
    WRAP(fegetenv)(&__fpscr);
    __fpscr &= ~__excepts;
    WRAP(fesetenv)(&__fpscr);
    return 0;
}

int WRAP(fegetexceptflag)(fexcept_t* __flagp, int __excepts) {
    fexcept_t __fpscr;
    WRAP(fegetenv)(&__fpscr);
    *__flagp = __fpscr & __excepts;
    return 0;
}

int WRAP(fesetexceptflag)(const fexcept_t* __flagp, int __excepts) {
    fexcept_t __fpscr;
    WRAP(fegetenv)(&__fpscr);
    __fpscr &= ~__excepts;
    __fpscr |= *__flagp & __excepts;
    WRAP(fesetenv)(&__fpscr);
    return 0;
}

int WRAP(feraiseexcept)(int __excepts) {
    fexcept_t __ex = __excepts;
    WRAP(fesetexceptflag)(&__ex, __excepts);
    return 0;
}

int WRAP(fetestexcept)(int __excepts) {
    fexcept_t __fpscr;
    WRAP(fegetenv)(&__fpscr);
    return (__fpscr & __excepts);
}

int WRAP(fegetround)(void) {
    fenv_t _fpscr;
    WRAP(fegetenv)(&_fpscr);
    return ((_fpscr >> _FPSCR_RMODE_SHIFT) & 0x3);
}

int WRAP(fesetround)(int __round) {
    fenv_t _fpscr;
    WRAP(fegetenv)(&_fpscr);
    _fpscr &= ~(0x3 << _FPSCR_RMODE_SHIFT);
    _fpscr |= (__round << _FPSCR_RMODE_SHIFT);
    WRAP(fesetenv)(&_fpscr);
    return 0;
}

int WRAP(feholdexcept)(fenv_t* __envp) {
    fenv_t __env;
    WRAP(fegetenv)(&__env);
    *__envp = __env;
    __env &= ~(FE_ALL_EXCEPT | _FPSCR_ENABLE_MASK);
    WRAP(fesetenv)(&__env);
    return 0;
}

int WRAP(feupdateenv)(const fenv_t* __envp) {
    fexcept_t __fpscr;
    WRAP(fegetenv)(&__fpscr);
    WRAP(fesetenv)(__envp);
    WRAP(feraiseexcept)(__fpscr & FE_ALL_EXCEPT);
    return 0;
}

#endif /* __ARM_ARCH_7A__ */
