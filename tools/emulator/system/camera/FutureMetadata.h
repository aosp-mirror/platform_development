/*
 * Copyright (C) 2012 The Android Open Source Project
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

/*
 * Rename select master metadata 2.0 symbols to metadata symbols in jb-mr1-dev
 * - this avoids having to bring in cross-repo changes
 */

#ifndef HW_EMULATOR_CAMERA_FUTURE_METADATA_H
#define HW_EMULATOR_CAMERA_FUTURE_METADATA_H

#define ANDROID_SENSOR_INFO_EXPOSURE_TIME_RANGE ANDROID_SENSOR_EXPOSURE_TIME_RANGE
#define ANDROID_CONTROL_MODE_OFF ANDROID_CONTROL_OFF
#define ANDROID_CONTROL_EFFECT_MODE_OFF ANDROID_CONTROL_EFFECT_OFF
#define ANDROID_CONTROL_AF_MODE_OFF ANDROID_CONTROL_AF_OFF
#define ANDROID_CONTROL_AE_MODE_OFF ANDROID_CONTROL_AE_OFF
#define ANDROID_CONTROL_AWB_MODE_OFF ANDROID_CONTROL_AWB_OFF

#endif
