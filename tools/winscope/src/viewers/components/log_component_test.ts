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

import {ScrollingModule} from '@angular/cdk/scrolling';
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {Timestamp} from 'common/time';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {TraceEntry} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {LogSelectFilter, LogTextFilter} from 'viewers/common/log_filters';
import {TextFilter} from 'viewers/common/text_filter';
import {
  ColumnSpec,
  LogEntry,
  LogField,
  LogHeader,
} from 'viewers/common/ui_data_log';
import {
  LogFilterChangeDetail,
  LogTextFilterChangeDetail,
  TimestampClickDetail,
  ViewerEvents,
} from 'viewers/common/viewer_events';
import {CollapsedSectionsComponent} from 'viewers/components/collapsed_sections_component';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
import {PropertiesComponent} from 'viewers/components/properties_component';
import {SearchBoxComponent} from 'viewers/components/search_box_component';
import {SelectWithFilterComponent} from 'viewers/components/select_with_filter_component';
import {LogComponent} from './log_component';

describe('LogComponent', () => {
  const testColumn1: ColumnSpec = {name: 'test1', cssClass: 'test-1'};
  const testColumn2: ColumnSpec = {name: 'test2', cssClass: 'test-2'};
  const testColumn3: ColumnSpec = {name: 'test3', cssClass: 'test-3'};

  let fixture: ComponentFixture<LogComponent>;
  let component: LogComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      imports: [
        ScrollingModule,
        MatFormFieldModule,
        FormsModule,
        MatInputModule,
        BrowserAnimationsModule,
        MatSelectModule,
        MatDividerModule,
        MatButtonModule,
        MatIconModule,
      ],
      declarations: [
        LogComponent,
        SelectWithFilterComponent,
        CollapsedSectionsComponent,
        CollapsibleSectionTitleComponent,
        PropertiesComponent,
        SearchBoxComponent,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LogComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    setComponentInputData();
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders filters', () => {
    const filtersInTable = htmlElement.querySelectorAll('.entries .filter');
    expect(filtersInTable.length).toEqual(2);
    const filtersInTitle = htmlElement.querySelectorAll(
      '.title-section .filter',
    );
    expect(filtersInTitle.length).toEqual(0);
  });

  it('renders filters in title', () => {
    component.title = 'Test';
    component.showFiltersInTitle = true;
    fixture.detectChanges();
    const filtersInTable = htmlElement.querySelectorAll('.entries .filter');
    expect(filtersInTable.length).toEqual(0);
    const filtersInTitle = htmlElement.querySelectorAll(
      '.title-section .filter',
    );
    expect(filtersInTitle.length).toEqual(2);
  });

  it('renders entries', () => {
    expect(htmlElement.querySelector('.scroll')).toBeTruthy();

    const entryText = assertDefined(
      htmlElement.querySelector('.scroll .entry'),
    ).textContent;
    expect(entryText).toContain('Test tag');
    expect(entryText).toContain('123');
    expect(entryText).toContain('2ns');
  });

  it('scrolls to current entry on button click', () => {
    component.currentIndex = 1;
    fixture.detectChanges();
    const goToCurrentTimeButton = assertDefined(
      htmlElement.querySelector<HTMLElement>('.go-to-current-time'),
    );
    const spy = spyOn(
      assertDefined(component.scrollComponent),
      'scrollToIndex',
    );
    goToCurrentTimeButton.click();
    expect(spy).toHaveBeenCalledWith(1);
  });

  it('applies select filter correctly', async () => {
    const allEntries = component.entries.slice();
    htmlElement.addEventListener(ViewerEvents.LogFilterChange, (event) => {
      const detail: LogFilterChangeDetail = (event as CustomEvent).detail;
      if (detail.value.length === 0) {
        component.entries = allEntries;
        return;
      }
      component.entries = allEntries.filter((entry) => {
        const entryValue = assertDefined(
          entry.fields.find((f) => f.spec === detail.header.spec),
        ).value.toString();
        if (Array.isArray(detail.value)) {
          return detail.value.includes(entryValue);
        }
        return entryValue.includes(detail.value);
      });
    });
    expect(htmlElement.querySelectorAll('.entry').length).toEqual(2);
    const filterTrigger = assertDefined(
      htmlElement.querySelector<HTMLElement>('.headers .mat-select-trigger'),
    );
    filterTrigger.click();
    await fixture.whenStable();

    const firstOption = assertDefined(
      document.querySelector<HTMLElement>('.mat-select-panel .mat-option'),
    );
    firstOption.click();
    fixture.detectChanges();
    expect(htmlElement.querySelectorAll('.entry').length).toEqual(1);

    firstOption.click();
    fixture.detectChanges();
    expect(htmlElement.querySelectorAll('.entry').length).toEqual(2);
  });

  it('applies text filter correctly', async () => {
    const allEntries = component.entries.slice();
    htmlElement.addEventListener(ViewerEvents.LogTextFilterChange, (event) => {
      const detail: LogTextFilterChangeDetail = (event as CustomEvent).detail;
      if (detail.filter.filterString.length === 0) {
        component.entries = allEntries;
        return;
      }
      component.entries = allEntries.filter((entry) => {
        const entryValue = assertDefined(
          entry.fields.find((f) => f.spec === detail.header.spec),
        ).value.toString();
        return entryValue.includes(detail.filter.filterString);
      });
    });
    expect(htmlElement.querySelectorAll('.entry').length).toEqual(2);

    const inputEl = assertDefined(
      htmlElement.querySelector<HTMLInputElement>('.headers input'),
    );

    inputEl.value = '123';
    inputEl.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(htmlElement.querySelectorAll('.entry').length).toEqual(2);

    inputEl.value = '1234';
    inputEl.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(htmlElement.querySelectorAll('.entry').length).toEqual(1);

    inputEl.value = '12345';
    inputEl.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(htmlElement.querySelectorAll('.entry').length).toEqual(0);

    inputEl.value = '';
    inputEl.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(htmlElement.querySelectorAll('.entry').length).toEqual(2);
  });

  it('emits event on arrow key press', () => {
    let downArrowPressedTimes = 0;
    htmlElement.addEventListener(ViewerEvents.ArrowDownPress, (event) => {
      downArrowPressedTimes++;
    });
    let upArrowPressedTimes = 0;
    htmlElement.addEventListener(ViewerEvents.ArrowUpPress, (event) => {
      upArrowPressedTimes++;
    });

    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowUp'}));
    expect(upArrowPressedTimes).toEqual(1);

    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowDown'}));
    expect(downArrowPressedTimes).toEqual(1);

    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowUp'}));
    expect(upArrowPressedTimes).toEqual(2);

    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowDown'}));
    expect(downArrowPressedTimes).toEqual(2);
  });

  it('propagates entry on trace entry timestamp click', () => {
    const logTimestampButton = assertDefined(
      htmlElement.querySelectorAll<HTMLElement>('.time-button').item(1),
    );
    checkEntryPropagatedOnTimestampClick(logTimestampButton);
  });

  it('propagates entry on timestamp click with propagateEntryTimestamp set', () => {
    const logTimestampButton = assertDefined(
      htmlElement
        .querySelectorAll<HTMLElement>(`.${testColumn3.cssClass} button`)
        .item(1),
    );
    checkEntryPropagatedOnTimestampClick(logTimestampButton);
  });

  it('propagates timestamp on raw timestamp click', () => {
    let timestamp: Timestamp | undefined;
    htmlElement.addEventListener(ViewerEvents.TimestampClick, (event) => {
      const detail: TimestampClickDetail = (event as CustomEvent).detail;
      timestamp = detail.timestamp;
    });
    const logTimestampButton = assertDefined(
      htmlElement.querySelector<HTMLElement>(`.${testColumn3.cssClass} button`),
    );
    logTimestampButton.click();

    expect(timestamp).toBeDefined();
  });

  it('changes css class on entry click and does not scroll', () => {
    htmlElement.addEventListener(ViewerEvents.LogEntryClick, (event) => {
      const index = (event as CustomEvent).detail;
      component.selectedIndex = index;
      fixture.detectChanges();
    });

    const entry = assertDefined(
      htmlElement.querySelector<HTMLElement>('.entry[item-id="1"]'),
    );
    expect(entry.className).not.toContain('selected');
    const spy = spyOn(
      assertDefined(component.scrollComponent),
      'scrollToIndex',
    );
    entry.click();
    expect(spy).not.toHaveBeenCalled();
    expect(entry.className).toContain('selected');
  });

  it('shows placeholder text', () => {
    expect(htmlElement.querySelector('.placeholder-text')).toBeNull();
    component.entries = [];
    fixture.detectChanges();
    expect(htmlElement.querySelector('.placeholder-text')).toBeTruthy();
  });

  it('formats timestamp without date unless multiple dates present', () => {
    const entry = assertDefined(htmlElement.querySelector('.scroll .entry'));
    expect(entry.textContent?.trim()).toEqual('1ns Test tag 1123 2ns');

    const spy = spyOn(component, 'areMultipleDatesPresent').and.returnValue(
      true,
    );
    fixture.detectChanges();
    expect(entry.textContent?.trim()).toEqual('1ns Test tag 1123 2ns');

    setComponentInputData(false);
    fixture.detectChanges();
    expect(entry.textContent?.trim()).toEqual(
      '1970-01-01, 00:00:00.000 Test tag 21234 1970-01-01, 00:00:00.000',
    );

    spy.and.returnValue(false);
    fixture.detectChanges();
    expect(entry.textContent?.trim()).toEqual(
      '00:00:00.000 Test tag 21234 00:00:00.000',
    );
  });

  function setComponentInputData(elapsed = true) {
    let entryTime: Timestamp;
    let fieldTime: Timestamp;
    if (elapsed) {
      entryTime = TimestampConverterUtils.makeElapsedTimestamp(1n);
      fieldTime = TimestampConverterUtils.makeElapsedTimestamp(2n);
    } else {
      entryTime = TimestampConverterUtils.makeRealTimestamp(1n);
      fieldTime = TimestampConverterUtils.makeRealTimestamp(2n);
    }

    const fields1: LogField[] = [
      {spec: testColumn1, value: 'Test tag 1'},
      {spec: testColumn2, value: 123},
      {spec: testColumn3, value: fieldTime},
    ];
    const fields2 = [
      {spec: testColumn1, value: 'Test tag 2'},
      {spec: testColumn2, value: 1234},
      {spec: testColumn3, value: fieldTime, propagateEntryTimestamp: true},
    ];

    const trace = new TraceBuilder<PropertyTreeNode>()
      .setTimestamps([entryTime, entryTime])
      .build();

    const entry1: LogEntry = {
      traceEntry: trace.getEntry(0),
      fields: fields1,
    };
    const entry2: LogEntry = {
      traceEntry: trace.getEntry(1),
      fields: fields2,
    };

    const entries = [entry1, entry2];

    const headers = [
      new LogHeader(
        testColumn1,
        new LogSelectFilter(['Test tag 1', 'Test tag 2']),
      ),
      new LogHeader(testColumn2, new LogTextFilter(new TextFilter())),
    ];

    component.entries = entries;
    component.headers = headers;
    component.selectedIndex = 0;
    component.traceType = TraceType.CUJS;
  }

  function checkEntryPropagatedOnTimestampClick(button: HTMLElement) {
    let entry: TraceEntry<PropertyTreeNode> | undefined;
    htmlElement.addEventListener(ViewerEvents.TimestampClick, (event) => {
      const detail: TimestampClickDetail = (event as CustomEvent).detail;
      entry = detail.entry;
    });
    button.click();
    expect(entry).toBeDefined();
  }
});
