/*
 * Copyright (C) 2024 The Android Open Source Project
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

import {AddCompositionType} from './operations/add_composition_type';
import {AddDisplayProperties} from './operations/add_display_properties';
import {AddExcludesCompositionState} from './operations/add_excludes_composition_state';
import {AddVerboseFlags} from './operations/add_verbose_flags';
import {UpdateTransforms} from './operations/update_transforms';

export const COMMON_OPERATIONS = {
  UpdateTransforms: new UpdateTransforms(),
  AddVerboseFlags: new AddVerboseFlags(),
  AddExcludesCompositionStateTrue: new AddExcludesCompositionState(true),
  AddExcludesCompositionStateFalse: new AddExcludesCompositionState(false),
  AddDisplayProperties: new AddDisplayProperties(),
  AddCompositionType: new AddCompositionType(),
};
