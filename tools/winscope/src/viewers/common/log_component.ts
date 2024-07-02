/*
 * Copyright (C) 2024 The Android Open Source Project
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
import {Timestamp} from 'common/time';
import {TraceType} from 'trace/trace_type';
import {
  LogFilterChangeDetail,
  TimestampClickDetail,
  ViewerEvents,
} from 'viewers/common/viewer_events';
import {timeButtonStyle} from 'viewers/components/styles/clickable_property.styles';
import {currentElementStyle} from 'viewers/components/styles/current_element.styles';
import {logComponentStyles} from 'viewers/components/styles/log_component.styles';
import {selectedElementStyle} from 'viewers/components/styles/selected_element.styles';
import {viewerCardStyle} from 'viewers/components/styles/viewer_card.styles';
import {
  LogEntry,
  LogField,
  LogFieldClassNames,
  LogFieldNames,
  LogFieldType,
  LogFieldValue,
  LogFilter,
} from './ui_data_log';

@Component({
  selector: 'log-view',
  template: `
    <div class="entries">
      <div class="headers" *ngIf="headers.length > 0">
        <div *ngFor="let header of headers" class="mat-body-2" [class]="getLogFieldClass(header)">{{getLogFieldName(header)}}</div>
      </div>

      <div class="filters" *ngIf="filters.length > 0">
        <div *ngIf="showTraceEntryTimes" class="time"></div>

        <div class="filter" *ngFor="let filter of filters" [class]="getLogFieldClass(filter.type)">
          <select-with-filter
              *ngIf="filter.options?.length > 0"
              [label]="getLogFieldName(filter.type)"
              [options]="filter.options"
              [outerFilterWidth]="getOuterFilterWidth(filter.type)"
              [innerFilterWidth]="getInnerFilterWidth(filter.type)"
              (selectChange)="onFilterChange($event, filter.type)">
          </select-with-filter>

          <mat-form-field *ngIf="filter.options === undefined" appearance="fill" (keydown.enter)="$event.target.blur()">
            <mat-label>{{filter.type}}</mat-label>
            <input
                matInput
                [name]="getLogFieldName(filter.type)"
                [ngModel]="emptyFilterValue"
                (ngModelChange)="onFilterChange($event, filter.type)" />
          </mat-form-field>
        </div>

        <button
            color="primary"
            mat-stroked-button
            class="go-to-current-time"
            *ngIf="showCurrentTimeButton"
            (click)="onGoToCurrentTimeClick()">
          Go to Current Time
        </button>
      </div>

      <div class="placeholder-text mat-body-1" *ngIf="entries.length === 0"> No entries found. </div>

      <cdk-virtual-scroll-viewport
          *ngIf="isTransactions()"
          transactionsVirtualScroll
          class="scroll"
          [scrollItems]="entries">
        <ng-container
            *cdkVirtualFor="let entry of entries; let i = index"
            [ngTemplateOutlet]="content"
            [ngTemplateOutletContext]="{entry: entry, i: i}"> </ng-container>
      </cdk-virtual-scroll-viewport>

      <cdk-virtual-scroll-viewport
          *ngIf="isProtolog()"
          protologVirtualScroll
          class="scroll"
          [scrollItems]="entries">
        <ng-container
            *cdkVirtualFor="let entry of entries; let i = index"
            [ngTemplateOutlet]="content"
            [ngTemplateOutletContext]="{entry: entry, i: i}"> </ng-container>
      </cdk-virtual-scroll-viewport>

      <cdk-virtual-scroll-viewport
          *ngIf="isFixedSizeScrollViewport()"
          itemSize="36"
          class="scroll">
        <ng-container
            *cdkVirtualFor="let entry of entries; let i = index"
            [ngTemplateOutlet]="content"
            [ngTemplateOutletContext]="{entry: entry, i: i}"> </ng-container>
      </cdk-virtual-scroll-viewport>

      <ng-template #content let-entry="entry" let-i="i">
        <div
            class="entry"
            [attr.item-id]="i"
            [class.current]="isCurrentEntry(i)"
            [class.selected]="isSelectedEntry(i)"
            (click)="onEntryClicked(i)">
          <div *ngIf="showTraceEntryTimes" class="time">
            <button
                mat-button
                color="primary"
                (click)="onTraceEntryTimestampClick($event, entry)"
                [disabled]="!entry.traceEntry.hasValidTimestamp()">
              {{ entry.traceEntry.getTimestamp().format() }}
            </button>
          </div>

          <div [class]="getLogFieldClass(field.type)" *ngFor="let field of entry.fields; index as i">
            <span class="mat-body-1" *ngIf="!showTimestampButton(field.value)">{{ field.value }}</span>
            <button
                *ngIf="showTimestampButton(field.value)"
                mat-button
                color="primary"
                (click)="onTimestampClick($event, entry, field)">
              {{ field.value.format() }}
            </button>
            <mat-icon
                *ngIf="field.icon"
                aria-hidden="false"
                [style]="{color: field.iconColor}"> {{field.icon}} </mat-icon>
          </div>
        </div>
      </ng-template>
    </div>
  `,
  styles: [
    selectedElementStyle,
    currentElementStyle,
    timeButtonStyle,
    viewerCardStyle,
    logComponentStyles,
  ],
})
export class LogComponent {
  emptyFilterValue = '';
  private lastClickedTimestamp: Timestamp | undefined;

  @Input() selectedIndex: number | undefined;
  @Input() scrollToIndex: number | undefined;
  @Input() currentIndex: number | undefined;
  @Input() headers: LogFieldType[] = [];
  @Input() filters: LogFilter[] = [];
  @Input() entries: LogEntry[] = [];
  @Input() showCurrentTimeButton = true;
  @Input() traceType: TraceType | undefined;
  @Input() showTraceEntryTimes = true;

  @ViewChild(CdkVirtualScrollViewport)
  scrollComponent?: CdkVirtualScrollViewport;

  constructor(@Inject(ElementRef) private elementRef: ElementRef) {}

  showTimestampButton(value: LogFieldValue) {
    return value instanceof Timestamp;
  }

  getLogFieldClass(fieldType: LogFieldType) {
    return LogFieldClassNames.get(fieldType);
  }

  getLogFieldName(fieldType: LogFieldType) {
    return LogFieldNames.get(fieldType);
  }

  ngOnChanges() {
    if (
      this.scrollToIndex !== undefined &&
      this.lastClickedTimestamp !==
        this.entries[this.scrollToIndex].traceEntry.getTimestamp()
    ) {
      this.scrollComponent?.scrollToIndex(Math.max(0, this.scrollToIndex - 1));
    }
  }

  onFilterChange(event: MatSelectChange | string, filterType: LogFieldType) {
    const value = event instanceof MatSelectChange ? event.value : event;
    this.emitEvent(
      ViewerEvents.LogFilterChange,
      new LogFilterChangeDetail(filterType, value),
    );
  }

  onEntryClicked(index: number) {
    this.emitEvent(ViewerEvents.LogEntryClick, index);
  }

  onGoToCurrentTimeClick() {
    if (this.currentIndex !== undefined && this.scrollComponent) {
      this.scrollComponent.scrollToIndex(this.currentIndex);
    }
  }

  onTraceEntryTimestampClick(event: MouseEvent, entry: LogEntry) {
    event.stopPropagation();
    this.lastClickedTimestamp = entry.traceEntry.getTimestamp();
    this.emitEvent(
      ViewerEvents.TimestampClick,
      new TimestampClickDetail(entry.traceEntry),
    );
  }

  onTimestampClick(event: MouseEvent, entry: LogEntry, field: LogField) {
    event.stopPropagation();
    if (field.type === LogFieldType.DISPATCH_TIME) {
      this.onTraceEntryTimestampClick(event, entry);
    } else {
      this.onRawTimestampClick(field.value as Timestamp);
    }
  }

  @HostListener('document:keydown', ['$event'])
  async handleKeyboardEvent(event: KeyboardEvent) {
    const logComponentRect = (
      this.elementRef.nativeElement as HTMLElement
    ).getBoundingClientRect();
    const logComponentVisible =
      logComponentRect.height > 0 && logComponentRect.width > 0;
    if (event.key === 'ArrowDown' && logComponentVisible) {
      event.stopPropagation();
      event.preventDefault();
      this.emitEvent(ViewerEvents.ArrowDownPress);
    }
    if (event.key === 'ArrowUp' && logComponentVisible) {
      event.stopPropagation();
      event.preventDefault();
      this.emitEvent(ViewerEvents.ArrowUpPress);
    }
  }

  isCurrentEntry(index: number): boolean {
    return index === this.currentIndex;
  }

  isSelectedEntry(index: number): boolean {
    return index === this.selectedIndex;
  }

  getOuterFilterWidth(type: LogFieldType): string | undefined {
    switch (type) {
      case LogFieldType.TRANSACTION_ID:
        return '125';
      case LogFieldType.VSYNC_ID:
        return '110';
      case LogFieldType.LAYER_OR_DISPLAY_ID:
        return '125';
      case LogFieldType.FLAGS:
        return '250';
      case LogFieldType.LOG_LEVEL:
        return '100';
      case LogFieldType.TAG:
        return '100';
      case LogFieldType.SOURCE_FILE:
        return '300';
      default:
        return '75';
    }
  }

  getInnerFilterWidth(type: LogFieldType): string | undefined {
    switch (type) {
      case LogFieldType.TRANSACTION_ID:
        return '125';
      case LogFieldType.VSYNC_ID:
        return '90';
      case LogFieldType.TRANSACTION_TYPE:
        return '175';
      case LogFieldType.LAYER_OR_DISPLAY_ID:
        return '100';
      case LogFieldType.FLAGS:
        return '250';
      case LogFieldType.TAG:
        return '150';
      case LogFieldType.SOURCE_FILE:
        return '300';
      default:
        return '100';
    }
  }

  isTransactions() {
    return this.traceType === TraceType.TRANSACTIONS;
  }

  isProtolog() {
    return this.traceType === TraceType.PROTO_LOG;
  }

  isFixedSizeScrollViewport() {
    return !(this.isTransactions() || this.isProtolog());
  }

  private onRawTimestampClick(value: Timestamp) {
    this.emitEvent(
      ViewerEvents.TimestampClick,
      new TimestampClickDetail(undefined, value),
    );
  }

  private emitEvent(event: ViewerEvents, data?: any) {
    const customEvent = new CustomEvent(event, {
      bubbles: true,
      detail: data,
    });
    this.elementRef.nativeElement.dispatchEvent(customEvent);
  }
}
