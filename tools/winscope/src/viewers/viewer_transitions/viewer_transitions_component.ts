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

import {Component, ViewChild} from '@angular/core';
import {TraceType} from 'trace/trace_type';
import {CollapsibleSections} from 'viewers/common/collapsible_sections';
import {CollapsibleSectionType} from 'viewers/common/collapsible_section_type';
import {LogComponent} from 'viewers/components/log_component';
import {selectedElementStyle} from 'viewers/components/styles/selected_element.styles';
import {viewerCardStyle} from 'viewers/components/styles/viewer_card.styles';
import {ViewerComponent} from 'viewers/components/viewer_component';
import {UiData} from './ui_data';

@Component({
  selector: 'viewer-transitions',
  template: `
    <div class="card-grid">
      <collapsed-sections
        [class.empty]="sections.areAllSectionsExpanded()"
        [sections]="sections"
        (sectionChange)="sections.onCollapseStateChange($event, false)">
      </collapsed-sections>

      <log-view
        class="log-view"
        [selectedIndex]="inputData?.selectedIndex"
        [scrollToIndex]="inputData?.scrollToIndex"
        [currentIndex]="inputData?.currentIndex"
        [entries]="inputData?.entries"
        [headers]="inputData?.headers"
        [traceType]="${TraceType.TRANSITION}"
        [showTraceEntryTimes]="false"
        [showCurrentTimeButton]="false">
      </log-view>

      <properties-view
        class="properties-view"
        [title]="propertiesTitle"
        [propertiesTree]="inputData?.propertiesTree"
        [traceType]="${TraceType.TRANSITION}"
        [textFilter]="inputData?.propertiesFilter"
        [isProtoDump]="false"
        placeholderText="No current or selected transition."
        (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.PROPERTIES, true)"
        [class.collapsed]="sections.isSectionCollapsed(CollapsibleSectionType.PROPERTIES)"></properties-view>
    </div>
  `,
  styles: [
    `
      .properties-view {
        flex: 1;
      }
    `,
    selectedElementStyle,
    viewerCardStyle,
  ],
})
export class ViewerTransitionsComponent extends ViewerComponent<UiData> {
  @ViewChild(LogComponent) logComponent?: LogComponent;

  propertiesTitle = 'SELECTED TRANSITION';
  CollapsibleSectionType = CollapsibleSectionType;
  sections = new CollapsibleSections([
    {
      type: CollapsibleSectionType.PROPERTIES,
      label: this.propertiesTitle,
      isCollapsed: false,
    },
  ]);
}
