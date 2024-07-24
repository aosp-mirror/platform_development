/*
 * Copyright 2020, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export class Chip {
  short: string;
  long: string;
  type: string;

  constructor(short: string, long: string, type: string) {
    this.short = short;
    this.long = long;
    this.type = type;
  }
}

export const VISIBLE_CHIP = new Chip('V', 'visible', 'default');

export const RELATIVE_Z_CHIP = new Chip(
  'RelZ',
  'Is relative Z-ordered to another surface',
  'warn',
);

export const RELATIVE_Z_PARENT_CHIP = new Chip(
  'RelZParent',
  'Something is relative Z-ordered to this surface',
  'warn',
);

export const MISSING_LAYER = new Chip(
  'MissingLayer',
  'This layer was referenced from the parent, but not present in the trace',
  'error',
);

export const GPU_CHIP = new Chip(
  'GPU',
  'This layer was composed on the GPU',
  'gpu',
);

export const HWC_CHIP = new Chip(
  'HWC',
  'This layer was composed by Hardware Composer',
  'hwc',
);

export const DUPLICATE_CHIP = new Chip(
  'Duplicate',
  "Multiple layers present with this layer's id",
  'duplicate',
);

export const MISSING_Z_PARENT_CHIP = new Chip(
  'MissingZParent',
  'Is relative Z-ordered to another surface, but RelZParent is missing from hierarchy',
  'zParent',
);
