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
#include <endian.h>

#if defined(__mips_isa_rev) && (__mips_isa_rev >= 2)

uint16_t WRAP(__swap16md)(uint16_t x) {
    register uint16_t _x = x;
    register uint16_t _r;
    __asm volatile ("wsbh %0, %1" : "=r" (_r) : "r" (_x));
    return _r;
}

uint32_t WRAP(__swap32md)(uint32_t x) {
    register uint32_t _x = x;
    register uint32_t _r;
    __asm volatile ("wsbh %0, %1; rotr %0, %0, 16" : "=r" (_r) : "r" (_x));
    return _r;
}

#endif

