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
import {Component, Input} from '@angular/core';
import {PersistentStore} from 'common/persistent_store';
import {TraceType} from 'trace/trace_type';
import {ImeUiData} from 'viewers/common/ime_ui_data';
import {viewerCardStyle} from './styles/viewer_card.styles';

@Component({
  selector: 'viewer-input-method',
  template: `
    <div class="card-grid">
      <div class="left-views">
        <hierarchy-view
          class="hierarchy-view"
          [tree]="inputData?.tree"
          [subtrees]="inputData?.sfSubtrees ?? []"
          [dependencies]="inputData?.dependencies ?? []"
          [highlightedItem]="inputData?.highlightedItem"
          [pinnedItems]="inputData?.pinnedItems ?? []"
          [tableProperties]="inputData?.hierarchyTableProperties"
          [store]="store"
          [userOptions]="inputData?.hierarchyUserOptions ?? {}"></hierarchy-view>

        <mat-divider></mat-divider>

        <ime-additional-properties
          class="ime-additional-properties"
          [isImeManagerService]="isImeManagerService()"
          [highlightedItem]="inputData?.highlightedItem ?? ''"
          [additionalProperties]="inputData?.additionalProperties"></ime-additional-properties>
      </div>

      <mat-divider [vertical]="true"></mat-divider>

      <properties-view
        class="properties-view"
        [store]="store"
        [userOptions]="inputData?.propertiesUserOptions ?? {}"
        [propertiesTree]="inputData?.propertiesTree"></properties-view>
    </div>
  `,
  styles: [
    `
      .left-views {
        flex: 1;
        display: flex;
        flex-direction: column;
      }
    `,
    viewerCardStyle,
  ],
})
export class ViewerInputMethodComponent {
  @Input() inputData: ImeUiData | undefined;
  @Input() store: PersistentStore = new PersistentStore();
  @Input() active = false;

  isImeManagerService(): boolean {
    return (
      this.inputData?.dependencies.includes(
        TraceType.INPUT_METHOD_MANAGER_SERVICE,
      ) ?? false
    );
  }
}
