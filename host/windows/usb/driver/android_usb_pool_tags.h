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

#ifndef ANDROID_USB_POOL_TAGS_H__
#define ANDROID_USB_POOL_TAGS_H__
/** \file 
  This file consists definitions for pool tags used in memory allocations for
  the driver.
*/

/// Default pool tag for memory allocations (GAND)
#define GANDR_POOL_TAG_DEFAULT                  'DNAG'

/// Pool tag for the driver object (GADR)
#define GANDR_POOL_TAG_DRIVER_OBJECT            'RDAG'

/// Pool tag for KMDF device object extension (GADx)
#define GANDR_POOL_TAG_KMDF_DEVICE              'xDAG'

/// Pool tag for target device configuration descriptor (GACD)
#define GANDR_POOL_TAG_DEV_CFG_DESC             'DCAG'

/// Pool tag for device file object extension (GADf)
#define GANDR_POOL_TAG_DEVICE_FO                'fDAG'

/// Pool tag for a bulk file object extension (GABx)
#define GANDR_POOL_TAG_BULK_FILE                'xBAG'

/// Pool tag for an interrupt file object extension (GAIx)
#define GANDR_POOL_TAG_INTERRUPT_FILE           'xIAG'

/// Pool tag for URB allocated in bulk read / write (GAbu)
#define GANDR_POOL_TAG_BULKRW_URB               'ubAG'

/// Pool tag for interface pairs (GAip)
#define GANDR_POOL_TAG_INTERF_PAIRS             'piAG'

#endif  // ANDROID_USB_POOL_TAGS_H__
