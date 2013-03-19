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
#include <stdint.h>

struct _Unwind_Context;

typedef enum {
  _UVRSC_CORE = 0,  // integer register
  _UVRSC_VFP = 1, // vfp
  _UVRSC_WMMXD = 3, // Intel WMMX data register
  _UVRSC_WMMXC = 4  // Intel WMMX control register
} _Unwind_VRS_RegClass;

typedef enum {
  _UVRSD_UINT32 = 0,
  _UVRSD_VFPX = 1,
  _UVRSD_UINT64 = 3,
  _UVRSD_FLOAT = 4,
  _UVRSD_DOUBLE = 5
} _Unwind_VRS_DataRepresentation;

typedef enum {
  _UVRSR_OK = 0,
  _UVRSR_NOT_IMPLEMENTED = 1,
  _UVRSR_FAILED = 2
} _Unwind_VRS_Result;

_Unwind_VRS_Result _Unwind_VRS_Get(struct _Unwind_Context *context,
                                   _Unwind_VRS_RegClass regclass,
                                   uint32_t regno,
                                   _Unwind_VRS_DataRepresentation representation,
                                   void* valuep);

_Unwind_VRS_Result _Unwind_VRS_Set(struct _Unwind_Context *context,
                                   _Unwind_VRS_RegClass regclass,
                                   uint32_t regno,
                                   _Unwind_VRS_DataRepresentation representation,
                                   void* valuep);

#define UNWIND_POINTER_REG  12
#define UNWIND_STACK_REG    13
#define UNWIND_IP_REG       15

uint64_t WRAP(_Unwind_GetGR)(struct _Unwind_Context* ctx, int index) {
  uint32_t val;
  _Unwind_VRS_Get(ctx, _UVRSC_CORE, index, _UVRSD_UINT32, &val);
  return (uint64_t)val;
}

void WRAP(_Unwind_SetGR)(struct _Unwind_Context* ctx, int index, uint64_t new_value) {
  uint32_t val = (uint32_t)new_value;
  _Unwind_VRS_Set(ctx, _UVRSC_CORE, index, _UVRSD_UINT32, &val);
}

uint64_t WRAP(_Unwind_GetIP)(struct _Unwind_Context* ctx) {
  return WRAP(_Unwind_GetGR)(ctx, UNWIND_IP_REG) & ~1; // thumb bit
}

void WRAP(_Unwind_SetIP)(struct _Unwind_Context* ctx, uintptr_t new_value) {
  uint32_t val = (uint32_t)new_value;
  // Propagate thumb bit to instruction pointer
  uint32_t thumbState = WRAP(_Unwind_GetGR)(ctx, UNWIND_IP_REG) & 1;
  uint64_t new_val = (uint64_t)(val | thumbState);
  WRAP(_Unwind_SetGR)(ctx, UNWIND_IP_REG, new_val);
}
