/*
 * Copyright (C) 2022 The Android Open Source Project
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
import { ArrayUtils } from "common/utils/array_utils";
import { Timestamp } from "common/trace/timestamp";

export class TimestampUtils {
  static getClosestIndex(targetTimestamp: Timestamp, timestamps: Timestamp[]) {
    if (timestamps === undefined) {
      throw TypeError(`Timestamps with type "${targetTimestamp.getType()}" not available`);
    }
    return ArrayUtils.binarySearchLowerOrEqual(timestamps, targetTimestamp);
  }
}
