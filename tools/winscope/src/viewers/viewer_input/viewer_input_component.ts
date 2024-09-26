/*
 * Copyright 2024 The Android Open Source Project
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
import {ViewerEvents} from 'viewers/common/viewer_events';
import {ShadingMode} from 'viewers/components/rects/shading_mode';
import {
  viewerCardInnerStyle,
  viewerCardStyle,
} from 'viewers/components/styles/viewer_card.styles';
import {UiData} from './ui_data';

@Component({
  selector: 'viewer-input',
  template: `
    <div class="card-grid">
      <collapsed-sections
          [class.empty]="sections.areAllSectionsExpanded()"
          [sections]="sections"
          (sectionChange)="sections.onCollapseStateChange($event, false)">
      </collapsed-sections>

      <rects-view *ngIf="inputData?.rectsToDraw"
          class="rects-view"
          [class.collapsed]="sections.isSectionCollapsed(CollapsibleSectionType.RECTS)"
          [title]="rectsTitle"
          [store]="store"
          [isStackBased]="true"
          [dependencies]="inputData?.dependencies"
          [displays]="inputData?.displays"
          [rects]="inputData?.rectsToDraw ?? []"
          [shadingModes]="shadingModes"
          [highlightedItem]="inputData?.highlightedRect ?? ''"
          [userOptions]="inputData?.rectsUserOptions ?? {}"
          [isDarkMode]="inputData?.isDarkMode ?? false"
          (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.RECTS, true)"></rects-view>

      <log-view
          class="log-view"
          [class.collapsed]="sections.isSectionCollapsed(CollapsibleSectionType.LOG)"
          [title]="eventLogTitle"
          [selectedIndex]="inputData?.selectedIndex"
          [scrollToIndex]="inputData?.scrollToIndex"
          [currentIndex]="inputData?.currentIndex"
          [entries]="inputData?.entries"
          [headers]="inputData?.headers"
          [filters]="inputData?.filters"
          [showFiltersInTitle]="true"
          [traceType]="${TraceType.INPUT_EVENT_MERGED}"
          [showTraceEntryTimes]="false"
          [showCurrentTimeButton]="false"
          (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.LOG, true)"></log-view>

      <div class="properties" *ngIf="!arePropertiesCollapsed()">
        <properties-view
          class="properties-view event-properties"
          [class.collapsed]="sections.isSectionCollapsed(CollapsibleSectionType.PROPERTIES)"
          [title]="eventPropertiesTitle"
          [propertiesTree]="inputData?.propertiesTree"
          [highlightedProperty]="inputData?.highlightedProperty"
          [traceType]="${TraceType.INPUT_EVENT_MERGED}"
          [store]="store"
          [isProtoDump]="true"
          [textFilter]="inputData?.propertiesFilter"
          placeholderText="No selected entry."
          (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.PROPERTIES, true)"></properties-view>
        <properties-view
          class="properties-view dispatch-properties"
          [class.collapsed]="sections.isSectionCollapsed(CollapsibleSectionType.INPUT_DISPATCH_PROPERTIES)"
          [title]="dispatchPropertiesTitle"
          [propertiesTree]="inputData?.dispatchPropertiesTree"
          [highlightedProperty]="inputData?.highlightedProperty"
          [traceType]="${TraceType.INPUT_EVENT_MERGED}"
          [store]="store"
          [isProtoDump]="true"
          [textFilter]="inputData?.dispatchPropertiesFilter"
          [filterEventName]="ViewerEvents.DispatchPropertiesFilterChange"
          placeholderText="No selected entry."
          (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.INPUT_DISPATCH_PROPERTIES, true)"></properties-view>
      </div>
    </div>
  `,
  styles: [
    viewerCardStyle,
    viewerCardInnerStyle,
    `
      .properties {
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow: auto;
      }

      .log-view:not(.collapsed) {
        flex: 1;
      }
    `,
  ],
})
export class ViewerInputComponent {
  @Input() inputData: UiData | undefined;
  @Input() store: PersistentStore | undefined;
  @Input() active = false;
  TraceType = TraceType;
  CollapsibleSectionType = CollapsibleSectionType;
  ViewerEvents = ViewerEvents;

  rectsTitle = 'INPUT WINDOWS';
  eventLogTitle = 'EVENT LOG';
  eventPropertiesTitle = 'EVENT DETAILS';
  dispatchPropertiesTitle = 'DISPATCH DETAILS';

  shadingModes = [ShadingMode.OPACITY];

  sections = new CollapsibleSections([
    {
      type: CollapsibleSectionType.RECTS,
      label: this.rectsTitle,
      isCollapsed: false,
    },
    {
      type: CollapsibleSectionType.LOG,
      label: this.eventLogTitle,
      isCollapsed: false,
    },
    {
      type: CollapsibleSectionType.PROPERTIES,
      label: this.eventPropertiesTitle,
      isCollapsed: false,
    },
    {
      type: CollapsibleSectionType.INPUT_DISPATCH_PROPERTIES,
      label: this.dispatchPropertiesTitle,
      isCollapsed: false,
    },
  ]);

  arePropertiesCollapsed(): boolean {
    return (
      this.sections.isSectionCollapsed(CollapsibleSectionType.PROPERTIES) &&
      this.sections.isSectionCollapsed(
        CollapsibleSectionType.INPUT_DISPATCH_PROPERTIES,
      )
    );
  }
}
