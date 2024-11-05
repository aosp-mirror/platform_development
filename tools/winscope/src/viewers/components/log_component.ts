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
  EventEmitter,
  HostListener,
  Inject,
  Input,
  Output,
  ViewChild,
} from '@angular/core';
import {MatSelectChange} from '@angular/material/select';

import {DOMUtils} from 'common/dom_utils';
import {Timestamp, TimestampFormatType} from 'common/time';
import {TimeUtils} from 'common/time_utils';
import {TraceType} from 'trace/trace_type';
import {TextFilter} from 'viewers/common/text_filter';
import {LogEntry, LogField, LogHeader} from 'viewers/common/ui_data_log';
import {
  LogFilterChangeDetail,
  LogTextFilterChangeDetail,
  TimestampClickDetail,
  ViewerEvents,
} from 'viewers/common/viewer_events';
import {
  inlineButtonStyle,
  timeButtonStyle,
} from 'viewers/components/styles/clickable_property.styles';
import {currentElementStyle} from 'viewers/components/styles/current_element.styles';
import {logComponentStyles} from 'viewers/components/styles/log_component.styles';
import {selectedElementStyle} from 'viewers/components/styles/selected_element.styles';
import {
  viewerCardInnerStyle,
  viewerCardStyle,
} from 'viewers/components/styles/viewer_card.styles';

@Component({
  selector: 'log-view',
  template: `
    <div class="view-header" *ngIf="title">
      <div class="title-section">
        <collapsible-section-title
            class="log-title"
            [title]="title"
            (collapseButtonClicked)="collapseButtonClicked.emit()"></collapsible-section-title>

        <div class="filters" *ngIf="showFiltersInTitle && getHeadersWithFilters().length > 0">
          <div class="filter" *ngFor="let header of getHeadersWithFilters()"
               [class]="header.spec.cssClass">
            <select-with-filter
                *ngIf="(header.filter.options?.length ?? 0) > 0"
                [label]="header.spec.name"
                [options]="header.filter.options"
                [outerFilterWidth]="header.filter.outerFilterWidthCss"
                [innerFilterWidth]="header.filter.innerFilterWidthCss"
                formFieldClass="no-border-top-field"
                (selectChange)="onFilterChange($event, header)">
            </select-with-filter>
          </div>
        </div>
      </div>
    </div>

    <div class="entries">
      <div class="headers table-header" *ngIf="headers.length > 0">
        <div *ngIf="showTraceEntryTimes" class="time">
          <button
              color="primary"
              mat-button
              class="time-button go-to-current-time"
              *ngIf="showCurrentTimeButton"
              (click)="onGoToCurrentTimeClick()">
            Go to Current Time
          </button>
        </div>

        <ng-container *ngFor="let header of headers">
          <div
            *ngIf="!isHeaderWithFilter(header)"
            class="mat-body-2 header"
            [class]="header.spec.cssClass">
          {{header.spec.name}}</div>

          <div
            *ngIf="isHeaderWithFilter(header) && !showFiltersInTitle"
            class="filter mat-body-2"
            [class]="header.spec.cssClass">
            <select-with-filter
                *ngIf="(header.filter.options?.length ?? 0) > 0"
                [label]="header.spec.name"
                [options]="header.filter.options"
                [outerFilterWidth]="header.filter.outerFilterWidthCss"
                [innerFilterWidth]="header.filter.innerFilterWidthCss"
                appearance="none"
                formFieldClass="no-padding-field"
                (selectChange)="onFilterChange($event, header)">
            </select-with-filter>

            <search-box
              *ngIf="header.filter.textFilter"
              [textFilter]="header.filter.textFilter"
              [label]="header.spec.name"
              [filterName]="header.spec.name"
              appearance="none"
              [formFieldClass]="
                'wide-field no-padding-field center-field '
                 + header.spec.cssClass
                 + (header.filter.textFilter.values.filterString?.length === 0 ? ' mat-body-2' : '')
              "
              height="fit-content"
              (filterChange)="onSearchBoxChange($event, header)"></search-box>
          </div>
        </ng-container>
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
          *ngIf="isTransitions()"
          transitionsVirtualScroll
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
                class="time-button"
                color="primary"
                (click)="onTraceEntryTimestampClick($event, entry)"
                [disabled]="!entry.traceEntry.hasValidTimestamp()">
              {{ formatTimestamp(entry.traceEntry.getTimestamp()) }}
            </button>
          </div>

          <div [class]="field.spec.cssClass" *ngFor="let field of entry.fields; index as i">
            <span class="mat-body-1" *ngIf="!showFieldButton(field)">{{ field.value }}</span>
            <button
                *ngIf="showFieldButton(field)"
                mat-button
                class="time-button"
                color="primary"
                (click)="onFieldButtonClick($event, entry, field)">
              {{ formatFieldButton(field) }}
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
    `
      .view-header {
        display: flex;
        flex-direction: column;
        flex: 0 0 auto
      }
    `,
    selectedElementStyle,
    currentElementStyle,
    timeButtonStyle,
    inlineButtonStyle,
    viewerCardStyle,
    viewerCardInnerStyle,
    logComponentStyles,
  ],
})
export class LogComponent {
  emptyFilterValue = '';
  private lastClickedTimestamp: Timestamp | undefined;

  @Input() title: string | undefined;
  @Input() selectedIndex: number | undefined;
  @Input() scrollToIndex: number | undefined;
  @Input() currentIndex: number | undefined;
  @Input() headers: LogHeader[] = [];
  @Input() entries: LogEntry[] = [];
  @Input() showCurrentTimeButton = true;
  @Input() traceType: TraceType | undefined;
  @Input() showTraceEntryTimes = true;
  @Input() showFiltersInTitle = false;

  @Output() collapseButtonClicked = new EventEmitter();

  @ViewChild(CdkVirtualScrollViewport)
  scrollComponent?: CdkVirtualScrollViewport;

  constructor(
    @Inject(ElementRef) private elementRef: ElementRef<HTMLElement>,
  ) {}

  getHeadersWithFilters() {
    return this.headers.filter((header) => this.isHeaderWithFilter(header));
  }

  isHeaderWithFilter(header: LogHeader): boolean {
    return header.filter !== undefined;
  }

  showFieldButton(field: LogField) {
    return field.value instanceof Timestamp || field.propagateEntryTimestamp;
  }

  formatFieldButton(field: LogField): string | number {
    return field.value instanceof Timestamp
      ? this.formatTimestamp(field.value)
      : field.value;
  }

  areMultipleDatesPresent(): boolean {
    return (
      this.entries.at(0)?.traceEntry.getFullTrace().spansMultipleDates() ??
      false
    );
  }

  formatTimestamp(timestamp: Timestamp) {
    if (!this.areMultipleDatesPresent()) {
      return timestamp.format(TimestampFormatType.DROP_DATE);
    }
    return timestamp.format();
  }

  ngOnChanges() {
    if (
      this.scrollToIndex !== undefined &&
      this.lastClickedTimestamp !==
        this.entries.at(this.scrollToIndex)?.traceEntry.getTimestamp()
    ) {
      this.scrollComponent?.scrollToIndex(Math.max(0, this.scrollToIndex - 1));
    }
  }

  async ngAfterContentInit() {
    await TimeUtils.sleepMs(10);
    this.updateTableMarginEnd();
  }

  @HostListener('window:resize', ['$event'])
  onResize(event: Event) {
    this.updateTableMarginEnd();
  }

  onFilterChange(event: MatSelectChange, header: LogHeader) {
    this.emitEvent(
      ViewerEvents.LogFilterChange,
      new LogFilterChangeDetail(header, event.value),
    );
  }

  onSearchBoxChange(detail: TextFilter, header: LogHeader) {
    this.emitEvent(
      ViewerEvents.LogTextFilterChange,
      new LogTextFilterChangeDetail(header, detail),
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

  onFieldButtonClick(event: MouseEvent, entry: LogEntry, field: LogField) {
    event.stopPropagation();
    if (field.propagateEntryTimestamp) {
      this.onTraceEntryTimestampClick(event, entry);
    } else if (field.value instanceof Timestamp) {
      this.onRawTimestampClick(field.value as Timestamp);
    }
  }

  @HostListener('document:keydown', ['$event'])
  async handleKeyboardEvent(event: KeyboardEvent) {
    const logComponentVisible = DOMUtils.isElementVisible(
      this.elementRef.nativeElement,
    );
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

  isTransactions() {
    return this.traceType === TraceType.TRANSACTIONS;
  }

  isProtolog() {
    return this.traceType === TraceType.PROTO_LOG;
  }

  isTransitions() {
    return this.traceType === TraceType.TRANSITION;
  }

  isFixedSizeScrollViewport() {
    return !(
      this.isTransactions() ||
      this.isProtolog() ||
      this.isTransitions()
    );
  }

  updateTableMarginEnd() {
    const tableHeader =
      this.elementRef.nativeElement.querySelector<HTMLElement>('.table-header');
    if (!tableHeader) {
      return;
    }
    const el = this.scrollComponent?.elementRef.nativeElement;
    if (el && el.scrollHeight > el.offsetHeight) {
      tableHeader.style.marginInlineEnd =
        el.offsetWidth - el.scrollWidth + 'px';
    } else {
      tableHeader.style.marginInlineEnd = '';
    }
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
