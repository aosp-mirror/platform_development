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
import {CollapsibleSections} from 'viewers/common/collapsible_sections';
import {CollapsibleSectionType} from 'viewers/common/collapsible_section_type';
import {ShadingMode} from 'viewers/components/rects/shading_mode';

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
      <collapsed-sections
        [class.empty]="sections.areAllSectionsExpanded()"
        [sections]="sections"
        (sectionChange)="sections.onCollapseStateChange($event, false)">
      </collapsed-sections>
      <rects-view
        class="rects-view"
        [class.collapsed]="sections.isSectionCollapsed(CollapsibleSectionType.RECTS)"
        [title]="rectsTitle"
        [store]="store"
        [rects]="inputData?.rectsToDraw ?? []"
        [zoomFactor]="4"
        [miniRects]="inputData?.sfRects ?? []"
        [highlightedItem]="inputData?.highlightedItem ?? ''"
        [displays]="inputData?.displays ?? []"
        groupLabel="Windows"
        [shadingModes]="shadingModes"
        [dependencies]="inputData?.dependencies ?? []"
        [userOptions]="inputData?.rectsUserOptions ?? {}"
        [pinnedItems]="inputData?.pinnedItems ?? []"
        [isDarkMode]="inputData?.isDarkMode ?? false"
        (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.RECTS, true)"></rects-view>
      <hierarchy-view
        class="hierarchy-view"
        [class.collapsed]="sections.isSectionCollapsed(CollapsibleSectionType.HIERARCHY)"
        [subtrees]="inputData?.hierarchyTrees"
        [dependencies]="inputData?.dependencies ?? []"
        [highlightedItem]="inputData?.highlightedItem ?? ''"
        [pinnedItems]="inputData?.pinnedItems ?? []"
        [textFilter]="inputData?.hierarchyFilter"
        [store]="store"
        [userOptions]="inputData?.hierarchyUserOptions ?? {}"
        [rectIdToShowState]="inputData?.rectIdToShowState"
        (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.HIERARCHY, true)"></hierarchy-view>
      <properties-view
        class="properties-view"
        [class.collapsed]="sections.isSectionCollapsed(CollapsibleSectionType.PROPERTIES)"
        [userOptions]="inputData?.propertiesUserOptions ?? {}"
        [propertiesTree]="inputData?.propertiesTree"
        [curatedProperties]="inputData?.curatedProperties"
        [traceType]="${TraceType.VIEW_CAPTURE}"
        [store]="store"
        [isProtoDump]="false"
        placeholderText="No selected item."
        [textFilter]="inputData?.propertiesFilter"
        (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.PROPERTIES, true)"></properties-view>
    </div>
  `,
  styles: [viewerCardStyle],
})
export class ViewerViewCaptureComponent {
  @Input() inputData: UiData | undefined;
  @Input() store: PersistentStore | undefined;
  CollapsibleSectionType = CollapsibleSectionType;

  rectsTitle = 'SKETCH';
  sections = new CollapsibleSections([
    {
      type: CollapsibleSectionType.RECTS,
      label: this.rectsTitle,
      isCollapsed: false,
    },
    {
      type: CollapsibleSectionType.HIERARCHY,
      label: CollapsibleSectionType.HIERARCHY,
      isCollapsed: false,
    },
    {
      type: CollapsibleSectionType.PROPERTIES,
      label: CollapsibleSectionType.PROPERTIES,
      isCollapsed: false,
    },
  ]);
  shadingModes = [
    ShadingMode.GRADIENT,
    ShadingMode.OPACITY,
    ShadingMode.WIRE_FRAME,
  ];
}
