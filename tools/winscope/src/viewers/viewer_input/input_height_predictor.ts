/*
 * Copyright (C) 2025 The Android Open Source Project
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

import {assertString} from 'common/assert_utils';
import {ItemHeightPredictor} from 'viewers/common/item_height_predictor';
import {InputEntry} from 'viewers/viewer_input/ui_data';

export class InputHeightPredictor extends ItemHeightPredictor {
  protected override readonly defaultRowSize = 24;
  private readonly actionCharsPerRow = 11;

  override predictHeight(entry: InputEntry): number {
    const action = assertString(entry.fields[2].value);
    return this.subItemHeight(action, this.actionCharsPerRow);
  }
}
