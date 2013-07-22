/*
 * Copyright (C) 2013 The Android Open Source Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
#ifndef _SYS__WCHAR_LIMITS_H
#define _SYS__WCHAR_LIMITS_H

#include <android/api-level.h>

/* WCHAR_MIN / WCHAR_MAX can be defined by <stdint.h> or <wchar.h>.
 * Due to historical reasons, their definition is a bit complex.
 *
 * - In NDK r8e and older, all definitions of WCHAR_MIN and WCHAR_MAX
 *   where 32-bit signed values (with one exception described below),
 *   despite the fact that wchar_t is 'unsigned' on ARM.
 *   See http://b.android.com/57749
 *
 *   This is no longer the case, unless you define _WCHAR_IS_ALWAYS_SIGNED
 *   at compile time to restore the old (broken) behaviour. This doesn't
 *   affect other CPU ABIs.
 *
 * - Before API level 9, on ARM, wchar_t was typedef to 'char' when
 *   compiling C (not C++). Also, the definitions of WCHAR_MIN and
 *   WCHAR_MAX differed between <stdint.h> and <wchar.h>:
 *
 *     <stdint.h> conditionally defined them to INT32_MIN / INT32_MAX.
 *     <wchar.h> conditionally defined them to 0 and 255 instead.
 *
 *   <stdint.h> would only define WCHAR_MIN and WCHAR_MAX when:
 *    - Compiling C sources.
 *    - Compiling C++ sources with __STDC_LIMIT_MACROS being defined.
 *
 *   <wchar.h> always ends up including <stdint.h> indirectly. This
 *   means that:
 *
 *     - When compiling C sources, WCHAR_MIN / WCHAR_MAX were always
 *       defined as INT32_MIN / INT32_MAX.
 *
 *     - When compiling C++ sources with __STDC_LIMIT_MACROS defined,
 *       they were always defined to INT32_MIN / INT32_MAX
 *
 *     - When compiling C++ sources without __STDC_LIMIT_MACROS defined,
 *       they were defined by <wchar.h> as 0 and 255, respectively.
 *
 *    Keep in mind that this was ARM-specific, only for API level < 9.
 *
 *    If _WCHAR_IS_8BIT is defined, the same broken behaviour will
 *    be restored. See http://b.android.com/57267
 */ 
#if !defined(WCHAR_MIN)

#  if defined(_WCHAR_IS_8BIT) && defined(__arm__) && __ANDROID_API__ < 9
#    if defined(__cplusplus) && !defined(__STDC_LIMIT_MACROS)
#      define WCHAR_MIN  0
#      define WCHAR_MAX  255
#    else
#      define WCHAR_MIN   (-2147483647 - 1)
#      define WCHAR_MAX   (2147483647)
#    endif
#  elif defined(_WCHAR_IS_ALWAYS_SIGNED)
#    define WCHAR_MIN   (-2147483647 - 1)
#    define WCHAR_MAX   (2147483647)
#  else
  /* Otherwise, the value is derived from the toolchain configuration.
   * to avoid putting explicit CPU checks in this header. */
#    ifndef __WCHAR_MAX__
#      error "__WCHAR_MAX__ is not defined. Check your toolchain!"
#    endif
  /* Clang does define __WCHAR_MAX__, but not __WCHAR_MIN__ */
#    ifndef __WCHAR_MIN__
#      if __WCHAR_MAX__ == 4294967295
#        define __WCHAR_MIN__  (0U)
#      elif __WCHAR_MAX__ == 2147483647
#        define __WCHAR_MIN__  (-2147483647 - 1)
#      else
#        error "Invalid __WCHAR_MAX__ value. Check your toolchain!"
#      endif
#    endif /* !__WCHAR_MIN__ */
#    define WCHAR_MIN    __WCHAR_MIN__
#    define WCHAR_MAX    __WCHAR_MAX__
#  endif /* !_WCHAR_IS_ALWAYS_SIGNED */

#endif /* !WCHAR_MIN */

#endif  /* _SYS__WCHAR_LIMITS_H */
