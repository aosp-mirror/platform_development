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

import Chip from "./Chip"
import ChipType from "./ChipType"

export const VISIBLE_CHIP = new Chip("V", "visible", ChipType.DEFAULT)

export const RELATIVE_Z_CHIP = {
    short: 'RelZ',
    long: 'Is relative Z-ordered to another surface',
    class: 'warn',
};

export const RELATIVE_Z_PARENT_CHIP = {
    short: 'RelZParent',
    long: 'Something is relative Z-ordered to this surface',
    class: 'warn',
};

export const MISSING_LAYER = {
    short: 'MissingLayer',
    long: 'This layer was referenced from the parent, but not present in the trace',
    class: 'error',
};

export const GPU_CHIP = {
    short: 'GPU',
    long: 'This layer was composed on the GPU',
    class: 'gpu',
};

export const HWC_CHIP = {
    short: 'HWC',
    long: 'This layer was composed by Hardware Composer',
    class: 'hwc',
};