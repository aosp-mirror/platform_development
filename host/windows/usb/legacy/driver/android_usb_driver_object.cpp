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

/** \file
  This file consists of implementation of class AndroidUsbDriverObject that
  encapsulates our driver object
*/
#pragma data_seg()
#pragma code_seg()

#include "precomp.h"
#include "android_usb_device_object.h"
#include "android_usb_driver_object.h"

#pragma data_seg()

/** Globally accessible instance of the AndroidUsbDriverObject.
  NT OS design allows us using of a global pointer to our driver object
  instance since it can't be created or destroyed concurently and its value
  is not going to change between creation and destruction.
*/
AndroidUsbDriverObject* global_driver_object = NULL;

#pragma code_seg("INIT")

extern "C" {

/// Main entry point to the driver
NTSTATUS DriverEntry(PDRIVER_OBJECT drv_object, PUNICODE_STRING reg_path) {
  // Just pass it down inside the class
  return AndroidUsbDriverObject::DriverEntry(drv_object, reg_path);
}

}  // extern "C"

NTSTATUS AndroidUsbDriverObject::DriverEntry(PDRIVER_OBJECT drv_object,
                                             PUNICODE_STRING reg_path) {
  ASSERT_IRQL_PASSIVE();
  ASSERT(NULL != drv_object);
  ASSERT((NULL != reg_path) &&
         (NULL != reg_path->Buffer) &&
         (0 != reg_path->Length));

  // Instantiate driver object
  global_driver_object = new(NonPagedPool, GANDR_POOL_TAG_DRIVER_OBJECT)
    AndroidUsbDriverObject(drv_object, reg_path);
  ASSERT(NULL != global_driver_object);
  if (NULL == global_driver_object)
    return STATUS_INSUFFICIENT_RESOURCES;

  // Initialize driver object
  NTSTATUS status = global_driver_object->OnDriverEntry(drv_object, reg_path);

  if (!NT_SUCCESS(status)) {
    // Something went wrong. Delete our driver object and get out of here.
    delete global_driver_object;
  }

  return status;
}

AndroidUsbDriverObject::AndroidUsbDriverObject(PDRIVER_OBJECT drv_object,
                                               PUNICODE_STRING reg_path)
    : driver_object_(drv_object),
      wdf_driver_(NULL) {
  ASSERT_IRQL_PASSIVE();
  ASSERT(NULL != driver_object());
}

NTSTATUS AndroidUsbDriverObject::OnDriverEntry(PDRIVER_OBJECT drv_object,
                                               PUNICODE_STRING reg_path) {
  ASSERT_IRQL_PASSIVE();
  ASSERT(driver_object() == drv_object);

  // Initiialize driver config, specifying our unload callback and default
  // pool tag for memory allocations that KMDF does on our behalf.
  WDF_DRIVER_CONFIG config;
  WDF_DRIVER_CONFIG_INIT(&config, EvtDeviceAddEntry);
  config.EvtDriverUnload = EvtDriverUnloadEntry;
  config.DriverPoolTag = GANDR_POOL_TAG_DEFAULT;

  // Create a framework driver object to represent our driver.
  NTSTATUS status = WdfDriverCreate(drv_object,
                                    reg_path,
                                    WDF_NO_OBJECT_ATTRIBUTES,
                                    &config,
                                    &wdf_driver_);
  ASSERT(NT_SUCCESS(status));
  if (!NT_SUCCESS(status))
    return status;

  GoogleDbgPrint("\n>>>>>>>>>> Android USB driver has started >>>>>>>>>>");

  return STATUS_SUCCESS;
}

#pragma code_seg("PAGE")

AndroidUsbDriverObject::~AndroidUsbDriverObject() {
  ASSERT_IRQL_PASSIVE();
}

NTSTATUS AndroidUsbDriverObject::OnAddDevice(PWDFDEVICE_INIT device_init) {
  ASSERT_IRQL_PASSIVE();
  GoogleDbgPrint("\n++++++++++ AndroidUsbDriverObject::OnAddDevice ++++++++++");
  // Instantiate our device object extension for this device
  AndroidUsbDeviceObject* wdf_device_ext =
    new(NonPagedPool, GANDR_POOL_TAG_KMDF_DEVICE) AndroidUsbDeviceObject();
  ASSERT(NULL != wdf_device_ext);
  if (NULL == wdf_device_ext)
    return STATUS_INSUFFICIENT_RESOURCES;

  // Create and initialize FDO device
  NTSTATUS status = wdf_device_ext->CreateFDODevice(device_init);
  ASSERT(NT_SUCCESS(status));
  if (!NT_SUCCESS(status))
    delete wdf_device_ext;

  return status;
}

void AndroidUsbDriverObject::OnDriverUnload() {
  ASSERT_IRQL_PASSIVE();
  GoogleDbgPrint("\n<<<<<<<<<< Android USB driver is unloaded <<<<<<<<<<");
}

NTSTATUS AndroidUsbDriverObject::EvtDeviceAddEntry(
    WDFDRIVER wdf_drv,
    PWDFDEVICE_INIT device_init) {
  ASSERT_IRQL_PASSIVE();
  ASSERT((NULL != global_driver_object) && (global_driver_object->wdf_driver() == wdf_drv));

  // Pass it down to our driver object
  if ((NULL == global_driver_object) ||
      (global_driver_object->wdf_driver() != wdf_drv)) {
    return STATUS_INTERNAL_ERROR;
  }

  return global_driver_object->OnAddDevice(device_init);
}

VOID AndroidUsbDriverObject::EvtDriverUnloadEntry(WDFDRIVER wdf_drv) {
  ASSERT_IRQL_PASSIVE();
  ASSERT((NULL != global_driver_object) &&
         (global_driver_object->wdf_driver() == wdf_drv));

  // Pass it down to our driver object
  if ((NULL != global_driver_object) &&
      (global_driver_object->wdf_driver() == wdf_drv)) {
    global_driver_object->OnDriverUnload();
    // Now we can (and have to) delete our driver object
    delete global_driver_object;
  }
}

#if DBG

#pragma code_seg()

ULONG __cdecl GoogleDbgPrint(char* format, ...) {
  va_list arg_list;
  va_start(arg_list, format);
  ULONG ret =
    vDbgPrintEx(DPFLTR_IHVDRIVER_ID, DPFLTR_ERROR_LEVEL, format, arg_list);
  va_end(arg_list);

  return ret;
}

#endif  // DBG

#pragma data_seg()
#pragma code_seg()
