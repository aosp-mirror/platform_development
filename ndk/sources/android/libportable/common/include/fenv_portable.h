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

#ifndef _FENV_PORTABLE_H_
#define _FENV_PORTABLE_H_

#include <sys/types.h>

#ifdef __LP64__
typedef struct {
  unsigned char a[28];  // x86_64 largest size
} fenv_t_portable;
typedef __uint32_t fexcept_t_portable;
#endif

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

#endif /* _FENV_PORTABLE_H_ */
