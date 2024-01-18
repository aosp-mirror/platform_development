/*
 * Copyright (C) 2023 The Android Open Source Project
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

export enum LayerFlag {
  HIDDEN = 0x01,
  OPAQUE = 0x02,
  SKIP_SCREENSHOT = 0x40,
  SECURE = 0x80,
  ENABLE_BACKPRESSURE = 0x100,
  DISPLAY_DECORATION = 0x200,
  IGNORE_DESTINATION_FRAME = 0x400,
}
