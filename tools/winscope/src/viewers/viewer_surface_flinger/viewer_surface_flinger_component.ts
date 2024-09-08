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
import {CollapsibleSections} from 'viewers/common/collapsible_sections';
import {CollapsibleSectionType} from 'viewers/common/collapsible_section_type';
import {ShadingMode} from 'viewers/components/rects/shading_mode';
import {viewerCardStyle} from 'viewers/components/styles/viewer_card.styles';
import {UiData} from './ui_data';

@Component({
  selector: 'viewer-surface-flinger',
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
        [isStackBased]="true"
        [rects]="inputData?.rectsToDraw ?? []"
        [highlightedItem]="inputData?.highlightedItem ?? ''"
        [displays]="inputData?.displays ?? []"
        [shadingModes]="shadingModes"
        [dependencies]="inputData?.dependencies ?? []"
        [userOptions]="inputData?.rectsUserOptions ?? {}"
        [pinnedItems]="inputData?.pinnedItems ?? []"
        (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.RECTS, true)"></rects-view>

      <hierarchy-view
        class="hierarchy-view"
        [class.collapsed]="sections.isSectionCollapsed(CollapsibleSectionType.HIERARCHY)"
        [tree]="inputData?.hierarchyTrees?.at(0)"
        [dependencies]="inputData?.dependencies ?? []"
        [highlightedItem]="inputData?.highlightedItem ?? ''"
        [pinnedItems]="inputData?.pinnedItems ?? []"
        [textFilter]="inputData?.hierarchyFilter"
        [store]="store"
        [userOptions]="inputData?.hierarchyUserOptions ?? {}"
        [rectIdToShowState]="inputData?.rectIdToShowState"
        (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.HIERARCHY, true)"></hierarchy-view>

      <div class="properties" *ngIf="!arePropertiesCollapsed()">
        <surface-flinger-property-groups
          class="property-groups"
          [class.empty]="!inputData?.curatedProperties && !sections.isSectionCollapsed(CollapsibleSectionType.PROPERTIES)"
          [class.collapsed]="sections.isSectionCollapsed(CollapsibleSectionType.CURATED_PROPERTIES)"
          [properties]="inputData?.curatedProperties"
          (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.CURATED_PROPERTIES, true)"></surface-flinger-property-groups>

        <properties-view
          class="properties-view"
          [class.collapsed]="sections.isSectionCollapsed(CollapsibleSectionType.PROPERTIES)"
          [title]="propertiesTitle"
          [userOptions]="inputData?.propertiesUserOptions ?? {}"
          [propertiesTree]="inputData?.propertiesTree"
          [highlightedProperty]="inputData?.highlightedProperty ?? ''"
          [traceType]="${TraceType.SURFACE_FLINGER}"
          [store]="store"
          [displayPropertyGroups]="inputData?.displayPropertyGroups"
          [isProtoDump]="true"
          placeholderText="No selected entry or layer."
          [textFilter]="inputData?.propertiesFilter"
          (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.PROPERTIES, true)"></properties-view>
      </div>
    </div>
  `,
  styles: [
    `
      .properties {
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow: auto;
      }
    `,
    viewerCardStyle,
  ],
})
export class ViewerSurfaceFlingerComponent {
  @Input() inputData: UiData | undefined;
  @Input() store: PersistentStore | undefined;
  @Input() active = false;
  TraceType = TraceType;
  CollapsibleSectionType = CollapsibleSectionType;

  rectsTitle = 'LAYERS';
  propertiesTitle = 'PROTO DUMP';
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
      type: CollapsibleSectionType.CURATED_PROPERTIES,
      label: 'PROPERTIES',
      isCollapsed: false,
    },
    {
      type: CollapsibleSectionType.PROPERTIES,
      label: this.propertiesTitle,
      isCollapsed: false,
    },
  ]);
  shadingModes = [
    ShadingMode.GRADIENT,
    ShadingMode.OPACITY,
    ShadingMode.WIRE_FRAME,
  ];

  arePropertiesCollapsed(): boolean {
    return (
      this.sections.isSectionCollapsed(CollapsibleSectionType.PROPERTIES) &&
      this.sections.isSectionCollapsed(
        CollapsibleSectionType.CURATED_PROPERTIES,
      )
    );
  }
}
