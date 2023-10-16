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
import {UiData} from './ui_data';

@Component({
  selector: 'viewer-window-manager',
  template: `
    <div class="card-grid">
      <rects-view
        class="rects-view"
        title="Windows"
        [rects]="inputData?.rects ?? []"
        [displayIds]="inputData?.displayIds ?? []"
        [highlightedItems]="inputData?.highlightedItems ?? []"></rects-view>
      <mat-divider [vertical]="true"></mat-divider>
      <hierarchy-view
        class="hierarchy-view"
        [tree]="inputData?.tree ?? null"
        [dependencies]="inputData?.dependencies ?? []"
        [highlightedItems]="inputData?.highlightedItems ?? []"
        [pinnedItems]="inputData?.pinnedItems ?? []"
        [store]="store"
        [userOptions]="inputData?.hierarchyUserOptions ?? {}"></hierarchy-view>
      <mat-divider [vertical]="true"></mat-divider>
      <properties-view
        class="properties-view"
        [userOptions]="inputData?.propertiesUserOptions ?? {}"
        [propertiesTree]="inputData?.propertiesTree ?? {}"
        [isProtoDump]="true"></properties-view>
    </div>
  `,
  styles: [
    `
      .rects-view,
      .hierarchy-view,
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
export class ViewerWindowManagerComponent {
  @Input() inputData?: UiData;
  @Input() store: PersistentStore = new PersistentStore();
  @Input() active = false;
  TRACE_INFO = TRACE_INFO;
  TraceType = TraceType;
}
