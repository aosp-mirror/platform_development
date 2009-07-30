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
  Standard precompile file
*/
#pragma warning(disable:4200)
#pragma warning(disable:4201)  // nameless struct/union
#pragma warning(disable:4214)  // bit field types other than int
extern "C" {
#include <initguid.h>
#include <ntddk.h>
#include <ntintsafe.h>
#include <ntstrsafe.h>
#include "usbdi.h"
#include "usbdlib.h"
#include <wdf.h>
#include <wdfusb.h>
}  // extern "C"
#pragma warning(default:4200)
#pragma warning(default:4201)
#pragma warning(default:4214)

// Just to make adb_api.h compile. Since we will not reference any
// of the API routines in the driver, only structures and constants,
// we're fine with that.
typedef void* LPOVERLAPPED;

#include "adb_api.h"
#include "adb_api_legacy.h"
#include "android_usb_pool_tags.h"
#include "android_usb_driver_defines.h"
#include "android_usb_new_delete.h"
#include "android_usb_inl.h"
