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

import {assertDefined} from 'common/assert_utils';
import {LogFieldType} from 'viewers/common/ui_data_log';
import {VariableHeightScrollStrategy} from 'viewers/common/variable_height_scroll_strategy';
import {ProtologEntry} from 'viewers/viewer_protolog/ui_data';

export class ProtologScrollStrategy extends VariableHeightScrollStrategy {
  protected readonly defaultRowSize = 16;
  private readonly textCharsPerRow = 150;
  private readonly timestampCharsPerRow = 20;
  private readonly sourceFileCharsPerRow = 50;

  protected override predictScrollItemHeight(entry: ProtologEntry): number {
    const textHeight = this.subItemHeight(
      assertDefined(entry.fields.find((f) => f.type === LogFieldType.TEXT))
        .value as string,
      this.textCharsPerRow,
    );
    const timestampHeight = this.subItemHeight(
      entry.traceEntry.getTimestamp().format(),
      this.timestampCharsPerRow,
    );
    const sourceFileHeight = this.subItemHeight(
      assertDefined(
        entry.fields.find((f) => f.type === LogFieldType.SOURCE_FILE),
      ).value as string,
      this.sourceFileCharsPerRow,
    );
    return Math.max(textHeight, timestampHeight, sourceFileHeight);
  }
}
