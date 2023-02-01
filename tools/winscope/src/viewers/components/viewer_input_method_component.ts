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
import {TRACE_INFO} from 'app/trace_info';
import {PersistentStore} from 'common/persistent_store';
import {TraceType} from 'trace/trace_type';
import {ImeUiData} from 'viewers/common/ime_ui_data';

@Component({
  selector: 'viewer-input-method',
  template: `
    <div class="card-grid">
      <div class="left-views">
        <hierarchy-view
          class="hierarchy-view"
          [tree]="inputData?.tree ?? null"
          [dependencies]="inputData?.dependencies ?? []"
          [highlightedItems]="inputData?.highlightedItems ?? []"
          [pinnedItems]="inputData?.pinnedItems ?? []"
          [tableProperties]="inputData?.hierarchyTableProperties"
          [store]="store"
          [userOptions]="inputData?.hierarchyUserOptions ?? {}"></hierarchy-view>

        <ng-container *ngIf="inputData?.additionalProperties">
          <mat-divider></mat-divider>

          <ime-additional-properties
            class="ime-additional-properties"
            [additionalProperties]="inputData?.additionalProperties!"></ime-additional-properties>
        </ng-container>
      </div>

      <mat-divider [vertical]="true"></mat-divider>

      <properties-view
        class="properties-view"
        [userOptions]="inputData?.propertiesUserOptions ?? {}"
        [propertiesTree]="inputData?.propertiesTree ?? {}"></properties-view>
    </div>
  `,
  styles: [
    `
      .left-views {
        flex: 1;
        display: flex;
        flex-direction: column;
      }

      .hierarchy-view,
      .ime-additional-properties,
      .properties-view {
        flex: 1;
        padding: 16px;
        display: flex;
        flex-direction: column;
        overflow: auto;
      }
    `,
  ],
})
export class ViewerInputMethodComponent {
  @Input() inputData: ImeUiData | null = null;
  @Input() store: PersistentStore = new PersistentStore();
  @Input() active = false;
  TRACE_INFO = TRACE_INFO;
  TraceType = TraceType;
}
