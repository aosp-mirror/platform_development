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

#ifndef ANDROID_USB_DRIVER_DEFINES_H__
#define ANDROID_USB_DRIVER_DEFINES_H__
/** \file
  This file consists of constants, types and macros used (and useful) in driver
  development.
*/

/** \name IRQL assertions
  These assertions help to verify that code is running at expected IRQL
*/
///@{

/// Asserts that current IRQL is less than provided level
#define ASSERT_IRQL_LESS(irql_level) ASSERT(KeGetCurrentIrql() < irql_level)
/// Asserts that current IRQL is less or equal than provided level
#define ASSERT_IRQL_LESS_OR_EQUAL(irql_level) ASSERT(KeGetCurrentIrql() <= irql_level)
/// Asserts that current IRQL is the same as provided level
#define ASSERT_IRQL_IS(irql_level) ASSERT(irql_level == KeGetCurrentIrql())
/// Asserts that current IRQL is less than DISPATCH_LEVEL
#define ASSERT_IRQL_LOW() ASSERT_IRQL_LESS(DISPATCH_LEVEL)
/// Asserts that current IRQL is above APC_LEVEL
#define ASSERT_IRQL_HIGH() ASSERT(KeGetCurrentIrql() >= DISPATCH_LEVEL)
/// Asserts that current IRQL is at PASSIVE_LEVEL
#define ASSERT_IRQL_PASSIVE() ASSERT_IRQL_IS(PASSIVE_LEVEL)
/// Asserts that current IRQL is at APC_LEVEL
#define ASSERT_IRQL_APC() ASSERT_IRQL_IS(APC_LEVEL)
/// Asserts that current IRQL is at DISPATCH_LEVEL
#define ASSERT_IRQL_DISPATCH() ASSERT_IRQL_IS(DISPATCH_LEVEL)
/// Asserts that current IRQL is at APC or DISPATCH_LEVEL
#define ASSERT_IRQL_APC_OR_DISPATCH() \
  ASSERT((KeGetCurrentIrql() == APC_LEVEL) || (KeGetCurrentIrql() == DISPATCH_LEVEL))
/// Asserts that current IRQL is less or equal DISPATCH_LEVEL
#define ASSERT_IRQL_LOW_OR_DISPATCH() \
  ASSERT_IRQL_LESS_OR_EQUAL(DISPATCH_LEVEL)

///@}

#if DBG
/** \brief Overrides DbgPrint to make sure that nothing gets printed
  to debug output in release build.
*/
ULONG __cdecl GoogleDbgPrint(char* format, ...);
#else 
#define GoogleDbgPrint(Arg) NOTHING
#endif

/// Invalid UCHAR value
#define INVALID_UCHAR   (static_cast<UCHAR>(0xFF))

/// Invalid ULONG value
#define INVALID_ULONG   (static_cast<ULONG>(-1))

/** Enum AndroidUsbWdfObjectType enumerates types of KMDF objects that
  we extend in our driver.
*/
enum AndroidUsbWdfObjectType {
  // We start enum with 1 insetead of 0 to protect orselves from a dangling
  // or uninitialized context structures because KMDF will zero our extension
  // when it gets created.

  /// Device object context
  AndroidUsbWdfObjectTypeDevice = 1,

  /// File object context
  AndroidUsbWdfObjectTypeFile,

  /// Request object context
  AndroidUsbWdfObjectTypeRequest,

  /// Workitem object context
  AndroidUsbWdfObjectTypeWorkitem,

  /// Illegal (maximum) context id
  AndroidUsbWdfObjectTypeMax
};

/** Structure AndroidUsbWdfObjectContext represents our context that extends
  every KMDF object (device, file, pipe, etc).
*/
typedef struct TagAndroidUsbWdfObjectContext {
  /// KMDF object type that is extended with this context
  AndroidUsbWdfObjectType     object_type;

  /// Instance of the class that extends KMDF object with this context
  class AndroidUsbWdfObject*  wdf_object_ext;
} AndroidUsbWdfObjectContext;

// KMDF woodoo to register our extension and implement accessor method
WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(AndroidUsbWdfObjectContext,
                                   GetAndroidUsbWdfObjectContext)

/** Structure AndroidUsbWdfRequestContext represents our context that is
  associated with every request recevied by the driver.
*/
typedef struct TagAndroidUsbWdfRequestContext {
  /// KMDF object type that is extended with this context
  /// (must be AndroidUsbWdfObjectTypeRequest)
  AndroidUsbWdfObjectType object_type;

  /// System time request has been first scheduled
  // (time of the first WdfRequestSend is called for it)
  LARGE_INTEGER           sent_at;

  /// KMDF descriptor for the memory allocated for URB
  WDFMEMORY               urb_mem;

  /// MDL describing the transfer buffer
  PMDL                    transfer_mdl;

  /// Private MDL that we build in order to perform the transfer
  PMDL                    mdl;

  // Virtual address for the current segment of transfer.
  void*                   virtual_address;

  /// Number of bytes remaining to transfer
  ULONG                   length;

  /// Number of bytes requested to transfer
  ULONG                   transfer_size;

  /// Accummulated number of bytes transferred
  ULONG                   num_xfer;

  /// Initial timeout (in millisec) set for this request
  ULONG                   initial_time_out;

  // Read / Write selector
  bool                    is_read;

  // IOCTL selector
  bool                    is_ioctl;
} AndroidUsbWdfRequestContext;

// KMDF woodoo to register our extension and implement accessor method
WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(AndroidUsbWdfRequestContext,
                                   GetAndroidUsbWdfRequestContext)

/** Structure AndroidUsbWorkitemContext represents our context that is
  associated with workitems created by our driver.
*/
typedef struct TagAndroidUsbWorkitemContext {
  /// KMDF object type that is extended with this context
  /// (must be AndroidUsbWdfObjectTypeWorkitem)
  AndroidUsbWdfObjectType         object_type;

  /// Pipe file object extension that enqueued this work item
  class AndroidUsbPipeFileObject* pipe_file_ext;
} AndroidUsbWorkitemContext;

// KMDF woodoo to register our extension and implement accessor method
WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(AndroidUsbWorkitemContext,
                                   GetAndroidUsbWorkitemContext)

#endif  // ANDROID_USB_DRIVER_DEFINES_H__
