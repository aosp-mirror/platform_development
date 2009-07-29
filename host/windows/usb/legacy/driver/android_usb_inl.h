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

#ifndef ANDROID_USB_INL_H__
#define ANDROID_USB_INL_H__
/** \file
  This file consists of inline routines for the driver.
*/

/// Gets control code out of the entire IOCTL code packet
__forceinline ULONG GetCtlCode(ULONG ioctl_code) {
  return (ioctl_code >> 2) & 0x0FFF;
}

/** \brief
  Converts string length from number of wide characters into number of bytes.
*/
__forceinline USHORT ByteLen(USHORT wchar_len) {
  return static_cast<USHORT>(wchar_len * sizeof(WCHAR));
}

/** \brief Gets byte length of a zero-terminated string not including
  zero terminator. Must be called at low IRQL.
*/
__forceinline USHORT ByteLen(const WCHAR* str) {
  ASSERT_IRQL_LOW();
  return (NULL != str) ? ByteLen(static_cast<USHORT>(wcslen(str))) : 0;
}

/** \brief
  Converts string length from number of bytes into number of wide characters.
  Can be called at any IRQL.
*/
__forceinline USHORT WcharLen(USHORT byte_len) {
  return byte_len / sizeof(WCHAR);
}

/** \brief Retrieves pointer out of the WDFMEMORY handle
*/
__forceinline void* GetAddress(WDFMEMORY wdf_mem) {
  ASSERT(NULL != wdf_mem);
  return (NULL != wdf_mem) ? WdfMemoryGetBuffer(wdf_mem, NULL) : NULL;
}

/** \brief Retrieves output memory address for WDFREQUEST

  @param request[in] A handle to KMDF request object
  @param status[out] Receives status of the call. Can be NULL.
*/
__forceinline void* OutAddress(WDFREQUEST request, NTSTATUS* status) {
  ASSERT(NULL != request);
  WDFMEMORY wdf_mem = NULL;
  NTSTATUS stat = WdfRequestRetrieveOutputMemory(request, &wdf_mem);
  ASSERT((NULL != wdf_mem) || (!NT_SUCCESS(stat)));
  if (NULL != status)
    *status = stat;
  return NT_SUCCESS(stat) ? GetAddress(wdf_mem) : NULL;
}

/** \brief Retrieves input memory address for WDFREQUEST

  @param request[in] A handle to KMDF request object
  @param status[out] Receives status of the call. Can be NULL.
*/
__forceinline void* InAddress(WDFREQUEST request, NTSTATUS* status) {
  ASSERT(NULL != request);
  WDFMEMORY wdf_mem = NULL;
  NTSTATUS stat = WdfRequestRetrieveInputMemory(request, &wdf_mem);
  ASSERT((NULL != wdf_mem) || (!NT_SUCCESS(stat)));
  if (NULL != status)
    *status = stat;
  return NT_SUCCESS(stat) ? GetAddress(wdf_mem) : NULL;
}

#endif  // ANDROID_USB_INL_H__
