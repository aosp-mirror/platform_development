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

#ifndef _STDARG_PORTABLE_H_
#define _STDARG_PORTABLE_H_

// The elements are not important. This struct should be interpreted
// differently by all targets.
typedef struct va_list_portable {
  void *ptr1;
  void *ptr2;
  void *ptr3;
  int offset1;
  int offset2;
} va_list_portable;

#endif /* _STDARG_PORTABLE_H_ */
