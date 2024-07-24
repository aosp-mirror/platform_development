/*
 * Copyright 2023, The Android Open Source Project
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

import {VariableHeightScrollStrategy} from 'viewers/common/variable_height_scroll_strategy';
import {UiDataMessage} from 'viewers/viewer_protolog/ui_data';

export class ProtologScrollStrategy extends VariableHeightScrollStrategy {
  protected readonly defaultRowSize = 16;
  private readonly textCharsPerRow = 150;
  private readonly timestampCharsPerRow = 20;
  private readonly sourceFileCharsPerRow = 50;

  protected override predictScrollItemHeight(message: UiDataMessage): number {
    const textHeight = this.subItemHeight(message.text, this.textCharsPerRow);
    const timestampHeight = this.subItemHeight(
      message.time.formattedValue(),
      this.timestampCharsPerRow,
    );
    const sourceFileHeight = this.subItemHeight(
      message.at,
      this.sourceFileCharsPerRow,
    );
    return Math.max(textHeight, timestampHeight, sourceFileHeight);
  }
}
