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

#ifndef _PORTABILITY_H_
#define _PORTABILITY_H_

/*
 * Hidden functions are exposed while linking the libportable shared object
 * but are not exposed thereafter.
 */
#define __hidden __attribute__((visibility("hidden")))

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


#endif /* _PORTABILITY_H_ */
