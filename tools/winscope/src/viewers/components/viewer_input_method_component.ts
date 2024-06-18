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
import {ImeUiData} from 'viewers/common/ime_ui_data';
import {viewerCardStyle} from './styles/viewer_card.styles';

@Component({
  selector: 'viewer-input-method',
  template: `
    <div class="card-grid">
      <collapsed-sections
        [class.empty]="sections.areAllSectionsExpanded()"
        [sections]="sections"
        (sectionChange)="sections.onCollapseStateChange($event, false)">
      </collapsed-sections>

      <div class="left-views" *ngIf="!areLeftViewsCollapsed()">
        <hierarchy-view
          class="hierarchy-view"
          [tree]="inputData?.hierarchyTrees?.at(0)"
          [subtrees]="getSfSubtrees()"
          [dependencies]="inputData ? [inputData.traceType] : []"
          [highlightedItem]="inputData?.highlightedItem"
          [pinnedItems]="inputData?.pinnedItems ?? []"
          [tableProperties]="inputData?.hierarchyTableProperties"
          [store]="store"
          [userOptions]="inputData?.hierarchyUserOptions ?? {}"
          (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.HIERARCHY, true)"
          [class.collapsed]="sections.isSectionCollapsed(CollapsibleSectionType.HIERARCHY)"></hierarchy-view>
        <ime-additional-properties
          class="ime-additional-properties"
          [isImeManagerService]="isImeManagerService()"
          [highlightedItem]="inputData?.highlightedItem ?? ''"
          [additionalProperties]="inputData?.additionalProperties"
          (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.IME_ADDITIONAL_PROPERTIES, true)"
          [class.collapsed]="sections.isSectionCollapsed(CollapsibleSectionType.IME_ADDITIONAL_PROPERTIES)"></ime-additional-properties>
      </div>

      <properties-view
        class="properties-view"
        [store]="store"
        [userOptions]="inputData?.propertiesUserOptions ?? {}"
        [propertiesTree]="inputData?.propertiesTree"
        [traceType]="inputData?.traceType"
        (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.PROPERTIES, true)"
        [class.collapsed]="sections.isSectionCollapsed(CollapsibleSectionType.PROPERTIES)"></properties-view>
    </div>
  `,
  styles: [
    `
      .left-views {
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow: auto;
      }
    `,
    viewerCardStyle,
  ],
})
export class ViewerInputMethodComponent {
  @Input() inputData: ImeUiData | undefined;
  @Input() store: PersistentStore = new PersistentStore();
  @Input() active = false;

  CollapsibleSectionType = CollapsibleSectionType;
  sections = new CollapsibleSections([
    {
      type: CollapsibleSectionType.HIERARCHY,
      label: CollapsibleSectionType.HIERARCHY,
      isCollapsed: false,
    },
    {
      type: CollapsibleSectionType.IME_ADDITIONAL_PROPERTIES,
      label: CollapsibleSectionType.IME_ADDITIONAL_PROPERTIES,
      isCollapsed: false,
    },
    {
      type: CollapsibleSectionType.PROPERTIES,
      label: CollapsibleSectionType.PROPERTIES,
      isCollapsed: false,
    },
  ]);

  isImeManagerService(): boolean {
    return this.inputData?.traceType === TraceType.INPUT_METHOD_MANAGER_SERVICE;
  }

  areLeftViewsCollapsed() {
    return (
      this.sections.isSectionCollapsed(CollapsibleSectionType.HIERARCHY) &&
      this.sections.isSectionCollapsed(
        CollapsibleSectionType.IME_ADDITIONAL_PROPERTIES,
      )
    );
  }

  getSfSubtrees() {
    if (
      !this.inputData?.hierarchyTrees ||
      this.inputData.hierarchyTrees.length <= 1
    ) {
      return [];
    }
    return this.inputData.hierarchyTrees.slice(1);
  }
}
