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

import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {PersistentStore} from 'common/persistent_store';
import {TraceType} from 'trace/trace_type';
import {viewerCardStyle} from 'viewers/components/styles/viewer_card.styles';
import {UiData} from './ui_data';

/**
 * TODO: Upgrade the View Capture's Properties View after getting UX's opinion.
 */
@Component({
  selector: 'viewer-view-capture',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="card-grid">
      <rects-view
        class="rects-view"
        title="View Hierarchy Sketch"
        [store]="store"
        [rects]="inputData?.rects ?? []"
        [zoomFactor]="4"
        [miniRects]="inputData?.sfRects ?? []"
        [highlightedItem]="inputData?.highlightedItem ?? ''"
        [displays]="[{displayId: 0, stackId: 0}]"></rects-view>
      <mat-divider [vertical]="true"></mat-divider>
      <hierarchy-view
        class="hierarchy-view"
        [tree]="inputData?.tree"
        [dependencies]="inputData?.dependencies ?? []"
        [highlightedItem]="inputData?.highlightedItem ?? ''"
        [pinnedItems]="inputData?.pinnedItems ?? []"
        [store]="store"
        [userOptions]="inputData?.hierarchyUserOptions ?? {}"></hierarchy-view>
      <mat-divider [vertical]="true"></mat-divider>
      <properties-view
        class="properties-view"
        [userOptions]="inputData?.propertiesUserOptions ?? {}"
        [propertiesTree]="inputData?.propertiesTree"
        [curatedProperties]="inputData?.curatedProperties"
        [traceType]="${TraceType.VIEW_CAPTURE}"
        [store]="store"
        [isProtoDump]="false">
      </properties-view>
    </div>
  `,
  styles: [viewerCardStyle],
})
export class ViewerViewCaptureComponent {
  @Input() inputData: UiData | undefined;
  @Input() store: PersistentStore | undefined;
}
