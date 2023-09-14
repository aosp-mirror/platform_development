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

import {Layer} from 'flickerlib/common';
import {SurfaceFlingerUtils as Utils} from './surface_flinger_utils';

describe('SurfaceFlingerUtils', () => {
  describe("Layer's z order comparison", () => {
    it('handles z-order paths with equal lengths', () => {
      const a: Layer = {
        zOrderPath: [1],
      };
      const b: Layer = {
        zOrderPath: [0],
      };
      expect(Utils.compareLayerZ(a, b)).toEqual(-1);
      expect(Utils.compareLayerZ(b, a)).toEqual(1);
    });

    it('handles z-order paths with different lengths', () => {
      const a: Layer = {
        zOrderPath: [0, 1],
      };
      const b: Layer = {
        zOrderPath: [0, 0, 0],
      };
      expect(Utils.compareLayerZ(a, b)).toEqual(-1);
      expect(Utils.compareLayerZ(b, a)).toEqual(1);
    });

    it('handles z-order paths with equal values (fall back to Layer ID comparison)', () => {
      const a: Layer = {
        id: 1,
        zOrderPath: [0, 1],
      };
      const b: Layer = {
        id: 0,
        zOrderPath: [0, 1, 0],
      };
      expect(Utils.compareLayerZ(a, b)).toEqual(-1);
      expect(Utils.compareLayerZ(b, a)).toEqual(1);
    });
  });
});
