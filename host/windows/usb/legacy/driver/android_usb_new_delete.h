/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ANDROID_USB_NEW_DELETE_H__
#define ANDROID_USB_NEW_DELETE_H__
/** \file
  This file consists implementations of our 'new' and 'delete' operators
*/

#include "android_usb_pool_tags.h"

/** \brief Checks if given pool type is one of NonPaged pool kinds.

  All numeric values for all NonPaged pool types are even numbers while all
  numeric values for all PagedPool types are odd numbers (see definition of
  POOL_TYPE enum). So this routine utilizes this to see whether given pool
  type is one of NonPaged pool kinds. This routine can be called at any IRQL.
  @param pool_type[in] Pool type
  @return True if pool type is one of NonPaged pool types, false otherwise
*/
__forceinline bool IsPoolNonPaged(POOL_TYPE pool_type) {
  return (0 == (pool_type & 0x1));
}

/** @name Operators new and delete
 
  In Kernel Mode development each memory allocation must specify type of the
  pool from which memory should be allocated, usualy PagedPool or NonPagedPool.
  Because of that "traditional" operator 'new' that takes only one parameter
  (memory size) is not good so we modify that operator by adding two more
  parameters: pool type and memory tag (last one is optional but highly
  encouraged). To prevent from mistakes, traditional operator 'new' is also
  defined. It will allocate requested number of bytes from NonPagedPool with
  default memory tag but it will always assert on checked (debug) builds.
  Since there is no infrastructure for C++ exceptions in Kernel Mode we are
  not using them to report memory allocation error. So, on failure operators
  'new' are returning NULL instead of throwing an exception.
*/
///@{

/** \brief Main operator new

  This is the main operator new that allocates specified number of bytes from
  the specified pool and assigns a custom tag to the allocated memory.
  Inherits IRQL restrictions for ExAllocatePoolWithTag (see the DDK doc).
  @param size[in] Number of bytes to allocate.
  @param pool_type[in] Type of the pool to allocate from.
  @param pool_tag[in] A tag to attach to the allocated memory. Since utilities
         that display tags use their ASCII representations it's advisable to
         use tag values that are ASCII symbols, f.i. 'ATag'. Note that due to
         inversion of bytes in stored ULONG value, to read 'ATag' in the tag
         displaying utility, the actual value passed to operator 'new' must be
         'gaTA'
  @return Pointer to allocated memory on success, NULL on error.
*/
__forceinline void* __cdecl operator new(size_t size,
                                         POOL_TYPE pool_type,
                                         ULONG pool_tag) {
  ASSERT((pool_type < MaxPoolType) && (0 != size));
  // Enforce IRQL restriction check.
  ASSERT(IsPoolNonPaged(pool_type) || (KeGetCurrentIrql() < DISPATCH_LEVEL));
  return size ? ExAllocatePoolWithTag(pool_type,
                                      static_cast<ULONG>(size),
                                      pool_tag) :
                NULL;
}

/** \brief
  Short operator new that attaches a default tag to the allocated memory.

  This version of operator new allocates specified number of bytes from the
  specified pool and assigns a default tag (GANDR_POOL_TAG_DEFAULT) to the
  allocated memory. Inherits IRQL restrictions for ExAllocatePoolWithTag.
  @param size[in] Number of bytes to allocate.
  @param pool_type[in] Type of the pool to allocate from.
  @return Pointer to allocated memory on success, NULL on error.
*/
__forceinline void* __cdecl operator new(size_t size, POOL_TYPE pool_type) {
  ASSERT((pool_type < MaxPoolType) && (0 != size));
  // Enforce IRQL restriction check.
  ASSERT(IsPoolNonPaged(pool_type) || (KeGetCurrentIrql() < DISPATCH_LEVEL));
  return size ? ExAllocatePoolWithTag(pool_type,
                                      static_cast<ULONG>(size),
                                      GANDR_POOL_TAG_DEFAULT) :
                NULL;
}

/** \brief Traditional operator new that should never be used.

  Using of this version of operator 'new' is prohibited in Kernel Mode
  development. For the sake of safety it is implemented though to allocate
  requested number of bytes from the NonPagedPool and attach default tag
  to the allocated memory. It will assert on checked (debug) builds.
  Inherits IRQL restrictions for ExAllocatePoolWithTag.
  @param size[in] Number of bytes to allocate.
  @return Pointer to memory allocated from NonPagedPool on success or NULL on
          error.
*/
__forceinline void* __cdecl operator new(size_t size) {
  ASSERTMSG("\n!!! Using of operator new(size_t size) is detected!\n"
    "This is illegal in our driver C++ development environment to use "
    "this version of operator 'new'. Please switch to\n"
    "new(size_t size, POOL_TYPE pool_type) or "
    "new(size_t size, POOL_TYPE pool_type, ULONG pool_tag) ASAP!!!\n",
    false);
  ASSERT(0 != size);
  return size ? ExAllocatePoolWithTag(NonPagedPool,
                                      static_cast<ULONG>(size),
                                      GANDR_POOL_TAG_DEFAULT) :
                NULL;
}

/** \brief Operator delete.

  Frees memory allocated by 'new' operator.
  @param pointer[in] Memory to free. If this parameter is NULL operator does
         nothing but asserts on checked build. Inherits IRQL restrictions
         for ExFreePool.
*/
__forceinline void __cdecl operator delete(void* pointer) {
  ASSERT(NULL != pointer);
  if (NULL != pointer)
    ExFreePool(pointer);
}

/** \brief Operator delete for arrays.

  Frees memory allocated by 'new' operator.
  @param pointer[in] Memory to free. If this parameter is NULL operator does
         nothing but asserts on checked build. Inherits IRQL restrictions
         for ExFreePool.
*/
__forceinline void __cdecl operator delete[](void* pointer) {
  ASSERT(NULL != pointer);
  if (NULL != pointer)
    ExFreePool(pointer);
}

///@}

#endif  // ANDROID_USB_NEW_DELETE_H__
