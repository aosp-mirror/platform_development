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

export function isPointInRect(
  point: {x: number; y: number},
  rect: {
    x: number;
    y: number;
    w: number;
    h: number;
  }
): boolean {
  return (
    rect.x <= point.x &&
    point.x <= rect.x + rect.w &&
    rect.y <= point.y &&
    point.y <= rect.y + rect.h
  );
}
