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
import {Component, ElementRef, Inject, Input, ViewChild} from '@angular/core';
import {MatSelectChange} from '@angular/material/select';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {currentElementStyle} from 'viewers/components/styles/current_element.styles';
import {selectedElementStyle} from 'viewers/components/styles/selected_element.styles';
import {timeButtonStyle} from 'viewers/components/styles/timestamp_button.styles';
import {viewerCardStyle} from 'viewers/components/styles/viewer_card.styles';
import {Events} from './events';
import {UiData} from './ui_data';

@Component({
  selector: 'viewer-transactions',
  template: `
    <div class="card-grid">
      <div class="entries">
        <div class="filters">
          <div class="time"></div>
          <div class="id">
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
          <div class="id">
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
                [color]="isCurrentEntry(i) ? 'secondary' : 'primary'"
                (click)="onTimestampClicked(entry.time)">
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

      <mat-divider [vertical]="true"></mat-divider>

      <properties-view
        *ngIf="uiData.currentPropertiesTree"
        class="properties-view"
        title="PROPERTIES - PROTO DUMP"
        [showFilter]="false"
        [userOptions]="uiData.propertiesUserOptions"
        [propertiesTree]="uiData.currentPropertiesTree"
        [traceType]="${TraceType.TRANSACTIONS}"
        [isProtoDump]="false"></properties-view>
    </div>
  `,
  styles: [
    `
      .entries {
        flex: 3;
        display: flex;
        flex-direction: column;
        padding: 16px;
      }

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

      ::ng-deep .mat-select-panel-wrap {
        overflow: scroll;
        overflow-x: hidden;
        max-height: 75vh;
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
    this.emitEvent(Events.VSyncIdFilterChanged, event.value);
  }

  onPidFilterChanged(event: MatSelectChange) {
    this.emitEvent(Events.PidFilterChanged, event.value);
  }

  onUidFilterChanged(event: MatSelectChange) {
    this.emitEvent(Events.UidFilterChanged, event.value);
  }

  onTypeFilterChanged(event: MatSelectChange) {
    this.emitEvent(Events.TypeFilterChanged, event.value);
  }

  onLayerIdFilterChanged(event: MatSelectChange) {
    this.emitEvent(Events.LayerIdFilterChanged, event.value);
  }

  onWhatFilterChanged(event: MatSelectChange) {
    this.emitEvent(Events.WhatFilterChanged, event.value);
  }

  onTransactionIdFilterChanged(event: MatSelectChange) {
    this.emitEvent(Events.TransactionIdFilterChanged, event.value);
  }

  onEntryClicked(index: number) {
    this.emitEvent(Events.EntryClicked, index);
  }

  onUserOptionChange() {
    const event: CustomEvent = new CustomEvent(
      ViewerEvents.PropertiesUserOptionsChange,
      {
        bubbles: true,
        detail: {userOptions: this.uiData.propertiesUserOptions},
      },
    );
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  onGoToCurrentTimeClick() {
    if (this.uiData.currentEntryIndex !== undefined && this.scrollComponent) {
      this.scrollComponent.scrollToIndex(this.uiData.currentEntryIndex);
    }
  }

  onTimestampClicked(timestamp: PropertyTreeNode) {
    this.lastClicked = timestamp.formattedValue();
    this.emitEvent(ViewerEvents.TimestampClick, timestamp);
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
