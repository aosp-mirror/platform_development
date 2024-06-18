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
import {CdkVirtualScrollViewport} from '@angular/cdk/scrolling';
import {
  Component,
  ElementRef,
  HostListener,
  Inject,
  Input,
  ViewChild,
} from '@angular/core';
import {MatSelectChange} from '@angular/material/select';
import {TraceType} from 'trace/trace_type';
import {CollapsibleSections} from 'viewers/common/collapsible_sections';
import {CollapsibleSectionType} from 'viewers/common/collapsible_section_type';
import {TimestampClickDetail, ViewerEvents} from 'viewers/common/viewer_events';
import {timeButtonStyle} from 'viewers/components/styles/clickable_property.styles';
import {currentElementStyle} from 'viewers/components/styles/current_element.styles';
import {selectedElementStyle} from 'viewers/components/styles/selected_element.styles';
import {viewerCardStyle} from 'viewers/components/styles/viewer_card.styles';
import {UiData, UiDataEntry} from './ui_data';

@Component({
  selector: 'viewer-transactions',
  template: `
    <div class="card-grid">
      <collapsed-sections
        [class.empty]="sections.areAllSectionsExpanded()"
        [sections]="sections"
        (sectionChange)="sections.onCollapseStateChange($event, false)">
      </collapsed-sections>
      <div class="log-view entries">
        <div class="filters">
          <div class="time"></div>
          <div class="id transaction-id">
            <select-with-filter
              label="TX ID"
              [options]="uiData.allTransactionIds"
              outerFilterWidth="125"
              innerFilterWidth="125"
              (selectChange)="onTransactionIdFilterChanged($event)">
            </select-with-filter>
          </div>
          <div class="vsyncid">
            <select-with-filter
              label="VSYNC ID"
              [options]="uiData.allVSyncIds"
              outerFilterWidth="110"
              innerFilterWidth="90"
              (selectChange)="onVSyncIdFilterChanged($event)">
            </select-with-filter>
          </div>
          <div class="pid">
            <select-with-filter
              label="PID"
              [options]="uiData.allPids"
              (selectChange)="onPidFilterChanged($event)">
            </select-with-filter>
          </div>
          <div class="uid">
            <select-with-filter
              label="UID"
              [options]="uiData.allUids"
              (selectChange)="onUidFilterChanged($event)">
            </select-with-filter>
          </div>
          <div class="type">
            <select-with-filter
              label="Type"
              innerFilterWidth="175"
              [options]="uiData.allTypes"
              (selectChange)="onTypeFilterChanged($event)">
            </select-with-filter>
          </div>
          <div class="id layer-or-display-id">
            <select-with-filter
              label="LAYER/DISP ID"
              outerFilterWidth="125"
              innerFilterWidth="100"
              [options]="uiData.allLayerAndDisplayIds"
              (selectChange)="onLayerIdFilterChanged($event)">
            </select-with-filter>
          </div>
          <div class="what">
            <select-with-filter
              label="Search text"
              outerFilterWidth="250"
              innerFilterWidth="250"
              [options]="uiData.allFlags"
              flex="2 0 250px"
              (selectChange)="onWhatFilterChanged($event)">
            </select-with-filter>
          </div>

          <button
            color="primary"
            mat-stroked-button
            class="go-to-current-time"
            (click)="onGoToCurrentTimeClick()">
            Go to Current Time
          </button>
        </div>

        <cdk-virtual-scroll-viewport
          transactionsVirtualScroll
          class="scroll"
          [scrollItems]="uiData.entries">
          <div
            *cdkVirtualFor="let entry of uiData.entries; let i = index"
            class="entry"
            [attr.item-id]="i"
            [class.current]="isCurrentEntry(i)"
            [class.selected]="isSelectedEntry(i)"
            (click)="onEntryClicked(i)">
            <div class="time">
              <button
                mat-button
                color="primary"
                (click)="onTimestampClicked(entry)">
                {{ entry.time.formattedValue() }}
              </button>
            </div>
            <div class="id transaction-id">
              <span class="mat-body-1">{{ entry.transactionId }}</span>
            </div>
            <div class="vsyncid">
              <span class="mat-body-1">{{ entry.vsyncId }}</span>
            </div>
            <div class="pid">
              <span class="mat-body-1">{{ entry.pid }}</span>
            </div>
            <div class="uid">
              <span class="mat-body-1">{{ entry.uid }}</span>
            </div>
            <div class="type">
              <span class="mat-body-1">{{ entry.type }}</span>
            </div>
            <div class="id layer-or-display-id">
              <span class="mat-body-1">{{ entry.layerOrDisplayId }}</span>
            </div>
            <div class="what">
              <span class="mat-body-1">{{ entry.what }}</span>
            </div>
          </div>
        </cdk-virtual-scroll-viewport>
      </div>

      <properties-view
        *ngIf="uiData.currentPropertiesTree"
        class="properties-view"
        [title]="propertiesTitle"
        [showFilter]="false"
        [userOptions]="uiData.propertiesUserOptions"
        [propertiesTree]="uiData.currentPropertiesTree"
        [traceType]="${TraceType.TRANSACTIONS}"
        [isProtoDump]="false"
        (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.PROPERTIES, true)"
        [class.collapsed]="sections.isSectionCollapsed(CollapsibleSectionType.PROPERTIES)"></properties-view>
    </div>
  `,
  styles: [
    `
      .properties-view {
        flex: 1;
      }

      .entries .filters {
        display: flex;
        flex-direction: row;
      }

      .entries .scroll {
        flex: 1;
        height: 100%;
      }

      .scroll .entry {
        display: flex;
        flex-direction: row;
      }

      .filters div,
      .entries div {
        padding: 4px;
      }

      .time {
        flex: 0 1 250px;
      }

      .id {
        flex: none;
        width: 125px;
      }

      .vsyncid {
        flex: none;
        width: 110px;
      }

      .pid {
        flex: none;
        width: 75px;
      }

      .uid {
        flex: none;
        width: 75px;
      }

      .type {
        width: 200px;
      }

      .what {
        flex: 2 0 250px;
      }

      .filters .what {
        margin-right: 16px;
      }

      .go-to-current-time {
        flex: none;
        margin-top: 4px;
        font-size: 12px;
        height: 65%;
        width: fit-content;
      }
    `,
    selectedElementStyle,
    currentElementStyle,
    timeButtonStyle,
    viewerCardStyle,
  ],
})
class ViewerTransactionsComponent {
  objectKeys = Object.keys;
  uiData: UiData = UiData.EMPTY;
  private lastClicked = '';
  propertiesTitle = 'PROPERTIES - PROTO DUMP';
  CollapsibleSectionType = CollapsibleSectionType;
  sections = new CollapsibleSections([
    {
      type: CollapsibleSectionType.PROPERTIES,
      label: this.propertiesTitle,
      isCollapsed: false,
    },
  ]);

  @ViewChild(CdkVirtualScrollViewport)
  scrollComponent?: CdkVirtualScrollViewport;

  constructor(@Inject(ElementRef) private elementRef: ElementRef) {}

  @Input()
  set inputData(data: UiData) {
    this.uiData = data;
    if (
      this.uiData.scrollToIndex !== undefined &&
      this.scrollComponent &&
      this.lastClicked !==
        this.uiData.entries[this.uiData.scrollToIndex].time.formattedValue()
    ) {
      this.scrollComponent.scrollToIndex(this.uiData.scrollToIndex);
    }
  }

  onVSyncIdFilterChanged(event: MatSelectChange) {
    this.emitEvent(ViewerEvents.VSyncIdFilterChanged, event.value);
  }

  onPidFilterChanged(event: MatSelectChange) {
    this.emitEvent(ViewerEvents.PidFilterChanged, event.value);
  }

  onUidFilterChanged(event: MatSelectChange) {
    this.emitEvent(ViewerEvents.UidFilterChanged, event.value);
  }

  onTypeFilterChanged(event: MatSelectChange) {
    this.emitEvent(ViewerEvents.TypeFilterChanged, event.value);
  }

  onLayerIdFilterChanged(event: MatSelectChange) {
    this.emitEvent(ViewerEvents.LayerIdFilterChanged, event.value);
  }

  onWhatFilterChanged(event: MatSelectChange) {
    this.emitEvent(ViewerEvents.WhatFilterChanged, event.value);
  }

  onTransactionIdFilterChanged(event: MatSelectChange) {
    this.emitEvent(ViewerEvents.TransactionIdFilterChanged, event.value);
  }

  onEntryClicked(index: number) {
    this.emitEvent(ViewerEvents.LogClicked, index);
  }

  onGoToCurrentTimeClick() {
    if (this.uiData.currentEntryIndex !== undefined && this.scrollComponent) {
      this.scrollComponent.scrollToIndex(this.uiData.currentEntryIndex);
    }
  }

  onTimestampClicked(entry: UiDataEntry) {
    this.emitEvent(
      ViewerEvents.TimestampClick,
      new TimestampClickDetail(entry.time.getValue(), entry.traceIndex),
    );
  }

  @HostListener('document:keydown', ['$event'])
  async handleKeyboardEvent(event: KeyboardEvent) {
    const index =
      this.uiData.selectedEntryIndex ?? this.uiData.currentEntryIndex;
    if (index === undefined) {
      return;
    }
    if (event.key === 'ArrowDown' && index < this.uiData.entries.length - 1) {
      event.preventDefault();
      this.emitEvent(ViewerEvents.LogChangedByKeyPress, index + 1);
    }

    if (event.key === 'ArrowUp' && index > 0) {
      event.preventDefault();
      this.emitEvent(ViewerEvents.LogChangedByKeyPress, index - 1);
    }
  }

  isCurrentEntry(index: number): boolean {
    return index === this.uiData.currentEntryIndex;
  }

  isSelectedEntry(index: number): boolean {
    return index === this.uiData.selectedEntryIndex;
  }

  private emitEvent(event: string, data: any) {
    const customEvent = new CustomEvent(event, {
      bubbles: true,
      detail: data,
    });
    this.elementRef.nativeElement.dispatchEvent(customEvent);
  }
}

export {ViewerTransactionsComponent};
