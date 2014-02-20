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

#ifndef _ASM_PORTABILITY_H_
#define _ASM_PORTABILITY_H_

#if !defined(__HOST__)
#define WRAP(f)     f ## _portable
#define REAL(f)     f
#else
/* On host app link with libpportable.a with -Wl,--wrap=symbol, which resolves undefined symbol to __wrap_symbol,
 * and undefined __real_symbol to the original symbol
 */
#define WRAP(f)     __wrap_ ## f
#define REAL(f)     __real_ ## f
#endif

#if defined(__mips__) && !defined(END)
#define END(f) .cfi_endproc; .end f
#endif

#endif /* _ASM_PORTABILITY_H_ */
