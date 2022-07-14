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
class StringUtils {
  static nanosecondsToHuman(nanoseconds: number): string {
    const units: [number, string][] = [
      [1000, "ms"],
      [60, "s"],
      [60, "m"],
      [24, "h"],
      [Infinity, "d"],
    ];

    let remainder = Math.floor(nanoseconds / 1000000);
    const parts = [];

    for(const [factor, unit] of units) {
      const part = (remainder % factor).toFixed();
      parts.push(part + unit);

      remainder = Math.floor(remainder / factor);
      if (remainder == 0) {
        break;
      }
    }

    return parts.reverse().join("");
  }
}

export {StringUtils};
