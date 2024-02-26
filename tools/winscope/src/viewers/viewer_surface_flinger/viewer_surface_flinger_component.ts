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
import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {PersistentStore} from 'common/persistent_store';
import {TraceType} from 'trace/trace_type';
import {viewerCardStyle} from 'viewers/components/styles/viewer_card.styles';
import {UiData} from './ui_data';

@Component({
  selector: 'viewer-surface-flinger',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="card-grid">
      <rects-view
        class="rects-view"
        title="Layers"
        [store]="store"
        [isStackBased]="true"
        [rects]="inputData?.rects ?? []"
        [highlightedItem]="inputData?.highlightedItem ?? ''"
        [displays]="inputData?.displays ?? []"></rects-view>
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
        [highlightedProperty]="inputData?.highlightedProperty ?? ''"
        [curatedProperties]="inputData?.curatedProperties"
        [traceType]="${TraceType.SURFACE_FLINGER}"
        [store]="store"
        [displayPropertyGroups]="inputData?.displayPropertyGroups"
        [isProtoDump]="true"
        placeholderText="No selected entry or layer."></properties-view>
    </div>
  `,
  styles: [viewerCardStyle],
})
export class ViewerSurfaceFlingerComponent {
  @Input() inputData: UiData | undefined;
  @Input() store: PersistentStore | undefined;
  @Input() active = false;
  TraceType = TraceType;
}
