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
#include <stdarg.h>
#include <stdarg_portable.h>

#include <stdio.h>

int WRAP(vfprintf)(FILE *stream, const char *format, va_list_portable *arg) {
  return REAL(vfprintf)(stream, format, arg);
}

int WRAP(vfscanf)(FILE *stream, const char *format, va_list_portable *arg) {
  return REAL(vfscanf)(stream, format, arg);
}

int WRAP(vprintf)(const char *format, va_list_portable *arg) {
  return REAL(vprintf)(format, arg);
}

int WRAP(vscanf)(const char *format, va_list_portable *arg) {
  return REAL(vscanf)(format, arg);
}

int WRAP(vsnprintf)(char *s, size_t n, const char *format, va_list_portable *arg) {
  return REAL(vsnprintf)(s, n, format, arg);
}

int WRAP(vsprintf)(char *s, const char *format, va_list_portable *arg) {
  return REAL(vsprintf)(s, format, arg);
}

int WRAP(vsscanf)(const char *s, const char *format, va_list_portable *arg) {
  return REAL(vsscanf)(s, format, arg);
}

