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
  This file consists of implementation of a class AndroidUsbWdfObject that
  encapsulates a basic extension to all KMDF objects. Currently, device and
  file object extensions ared derived from it.
*/
#pragma data_seg()
#pragma code_seg()

#include "precomp.h"
#include "android_usb_wdf_object.h"

#pragma data_seg()
#pragma code_seg("PAGE")

AndroidUsbWdfObject::AndroidUsbWdfObject(AndroidUsbWdfObjectType obj_type)
    : wdf_object_(NULL),
      object_type_(obj_type) {
  ASSERT_IRQL_LOW();
  ASSERT(obj_type < AndroidUsbWdfObjectTypeMax);
}

#pragma code_seg()

AndroidUsbWdfObject::~AndroidUsbWdfObject() {
  ASSERT_IRQL_LOW_OR_DISPATCH();
}

#pragma code_seg("PAGE")

NTSTATUS AndroidUsbWdfObject::InitObjectAttributes(
    PWDF_OBJECT_ATTRIBUTES wdf_obj_attr,
    WDFOBJECT parent) {
  ASSERT_IRQL_LOW();

  // Enforce file object extension exception.
  ASSERT(!Is(AndroidUsbWdfObjectTypeFile));
  if (Is(AndroidUsbWdfObjectTypeFile))
    return STATUS_INTERNAL_ERROR;

  // Initialize attributes and set cleanup and destroy callbacks
  WDF_OBJECT_ATTRIBUTES_INIT(wdf_obj_attr);
  WDF_OBJECT_ATTRIBUTES_SET_CONTEXT_TYPE(wdf_obj_attr,
                                         AndroidUsbWdfObjectContext);
  wdf_obj_attr->EvtCleanupCallback = EvtCleanupCallbackEntry;
  wdf_obj_attr->EvtDestroyCallback = EvtDestroyCallbackEntry;
  wdf_obj_attr->ParentObject = parent;
  wdf_obj_attr->SynchronizationScope = GetWdfSynchronizationScope();

  return STATUS_SUCCESS;
}

NTSTATUS AndroidUsbWdfObject::InitializeContext() {
  ASSERT_IRQL_LOW();
  ASSERT(IsAttached());
  if (!IsAttached())
    return STATUS_INTERNAL_ERROR;

  // Initialize our extension to that object
  AndroidUsbWdfObjectContext* context =
    GetAndroidUsbWdfObjectContext(wdf_object());
  ASSERT(NULL != context);
  if (NULL == context)
    return STATUS_INTERNAL_ERROR;

  // Make sure that extension has not been initialized
  ASSERT((0 == context->object_type) && (NULL == context->wdf_object_ext));
  if ((0 != context->object_type) || (NULL != context->wdf_object_ext))
    return STATUS_INTERNAL_ERROR;

  context->object_type = object_type();
  context->wdf_object_ext = this;
  ASSERT(this == GetAndroidUsbWdfObjectFromHandle(wdf_object()));

  return STATUS_SUCCESS;
}

#pragma code_seg()

WDF_SYNCHRONIZATION_SCOPE AndroidUsbWdfObject::GetWdfSynchronizationScope() {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  // By default we don't want KMDF to synchronize access to our objects
  return WdfSynchronizationScopeNone;
}

void AndroidUsbWdfObject::OnEvtCleanupCallback() {
  ASSERT_IRQL_LOW_OR_DISPATCH();
  GoogleDbgPrint("\n----- Object %p of type %u is cleaned up",
           this, object_type());
}

void AndroidUsbWdfObject::OnEvtDestroyCallback() {
  ASSERT_IRQL_LOW_OR_DISPATCH();
  GoogleDbgPrint("\n----- Object %p of type %u is destroyed",
           this, object_type());
}

void AndroidUsbWdfObject::EvtCleanupCallbackEntry(WDFOBJECT wdf_obj) {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  AndroidUsbWdfObjectContext* context = GetAndroidUsbWdfObjectContext(wdf_obj);
  ASSERT(NULL != context);
  if (NULL != context) {
    // For file objects we will be always called here even though we didn't
    // create any extension for them. In this case the context must not be
    // initialized.
    ASSERT(((0 == context->object_type) && (NULL == context->wdf_object_ext)) ||
           ((0 != context->object_type) && (NULL != context->wdf_object_ext)));
    if (NULL != context->wdf_object_ext) {
      ASSERT(context->wdf_object_ext->Is(context->object_type));
      context->wdf_object_ext->OnEvtCleanupCallback();
    }
  }
}

void AndroidUsbWdfObject::EvtDestroyCallbackEntry(WDFOBJECT wdf_obj) {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  AndroidUsbWdfObjectContext* context =
    GetAndroidUsbWdfObjectContext(wdf_obj);
  ASSERT(NULL != context);
  if (NULL != context) {
    // For file objects we will be always called here even though we didn't
    // create any extension for them. In this case the context must not be
    // initialized.
    ASSERT(((0 == context->object_type) && (NULL == context->wdf_object_ext)) ||
          ((0 != context->object_type) && (NULL != context->wdf_object_ext)));
    if (NULL != context->wdf_object_ext) {
      ASSERT(context->wdf_object_ext->Is(context->object_type));
      context->wdf_object_ext->OnEvtDestroyCallback();
      delete context->wdf_object_ext;
    }
  }
}

#pragma data_seg()
#pragma code_seg()
