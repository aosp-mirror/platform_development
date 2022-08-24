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
import { RectsUtils } from "./rects_utils";

describe("RectsUtils", () => {
  it("transforms rect", () => {
    const transform = {
      matrix: {
        dsdx: 1,
        dsdy: 0,
        dtdx: 0,
        dtdy: 1,
        tx: 1,
        ty: 1
      }
    };
    const rect = {
      topLeft: {x: 0, y: 0},
      bottomRight: {x: 1, y: -1},
      label: "TestRect",
      transform: transform,
      isVisible: true,
      isDisplay: false,
      height: 1,
      width: 1,
      ref: null,
      id: 12345,
      displayId: 0
    };
    const expected = {
      topLeft: {x: 1, y: 1},
      bottomRight: {x: 2, y: 0},
      label: "TestRect",
      transform: transform,
      isVisible: true,
      isDisplay: false,
      height: 1,
      width: 1,
      ref: null,
      id: 12345,
      displayId: 0,
      isVirtual: undefined
    };
    expect(RectsUtils.transformRect(rect.transform.matrix, rect)).toEqual(expected);
  });
});
