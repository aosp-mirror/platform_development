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

import {ClipboardModule} from '@angular/cdk/clipboard';
import {ScrollingModule} from '@angular/cdk/scrolling';
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {MatDividerModule} from '@angular/material/divider';
import {MatIconModule} from '@angular/material/icon';
import {assertDefined} from 'common/assert_utils';
import {TimeDuration} from 'common/time_duration';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {CujType} from 'trace/cuj_type';
import {Parser} from 'trace/parser';
import {Trace, TraceEntry} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {LogComponent} from 'viewers/common/log_component';
import {LogEntry, LogField, LogFieldType} from 'viewers/common/ui_data_log';
import {CollapsedSectionsComponent} from 'viewers/components/collapsed_sections_component';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
import {PropertiesComponent} from 'viewers/components/properties_component';
import {PropertyTreeNodeDataViewComponent} from 'viewers/components/property_tree_node_data_view_component';
import {TreeComponent} from 'viewers/components/tree_component';
import {TreeNodeComponent} from 'viewers/components/tree_node_component';
import {Presenter} from './presenter';
import {CujStatus, UiData} from './ui_data';
import {ViewerJankCujsComponent} from './viewer_jank_cujs_component';

describe('ViewerJankCujsComponent', () => {
  let fixture: ComponentFixture<ViewerJankCujsComponent>;
  let component: ViewerJankCujsComponent;
  let htmlElement: HTMLElement;

  let trace: Trace<PropertyTreeNode>;
  let entry: TraceEntry<PropertyTreeNode>;

  beforeAll(async () => {
    const parser = (await UnitTestUtils.getTracesParser([
      'traces/eventlog.winscope',
    ])) as Parser<PropertyTreeNode>;

    trace = new TraceBuilder<PropertyTreeNode>()
      .setParser(parser)
      .setType(TraceType.CUJS)
      .build();

    entry = trace.getEntry(0);
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      imports: [
        MatDividerModule,
        ScrollingModule,
        MatIconModule,
        ClipboardModule,
      ],
      declarations: [
        ViewerJankCujsComponent,
        TreeComponent,
        TreeNodeComponent,
        PropertyTreeNodeDataViewComponent,
        PropertiesComponent,
        CollapsedSectionsComponent,
        CollapsibleSectionTitleComponent,
        LogComponent,
      ],
      schemas: [],
    }).compileComponents();

    fixture = TestBed.createComponent(ViewerJankCujsComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;

    component.inputData = makeUiData();
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders entries', () => {
    expect(htmlElement.querySelector('.scroll')).toBeTruthy();

    const entry = assertDefined(htmlElement.querySelector('.scroll .entry'));
    expect(entry.innerHTML).toContain('LOCKSCREEN_PASSWORD_DISAPPEAR');
    expect(entry.innerHTML).toContain('30ns');
  });

  function makeUiData(): UiData {
    let mockTransitionIdCounter = 0;

    const cujEntries = [
      createMockCujEntry(entry, 20, CujType[20], 30, mockTransitionIdCounter++),
      createMockCujEntry(entry, 66, CujType[66], 42, 50, CujStatus.CANCELLED),
      createMockCujEntry(entry, 46, CujType[46], 49, mockTransitionIdCounter++),
      createMockCujEntry(entry, 59, CujType[59], 58, 70, CujStatus.EXECUTED),
    ];

    const uiData = UiData.createEmpty();
    uiData.entries = cujEntries;
    uiData.selectedIndex = 0;
    uiData.headers = Presenter.FIELD_NAMES;
    return uiData;
  }

  function createMockCujEntry(
    entry: TraceEntry<PropertyTreeNode>,
    cujTypeId: number,
    cujTypeStr: string,
    startTsNanos: number,
    endTsNanos: number,
    status = CujStatus.EXECUTED,
  ): LogEntry {
    const fields: LogField[] = [
      {
        type: LogFieldType.CUJ_TYPE,
        value: `${cujTypeStr} (${cujTypeId})`,
      },
      {
        type: LogFieldType.START_TIME,
        value: TimestampConverterUtils.makeElapsedTimestamp(
          BigInt(startTsNanos),
        ),
      },
      {
        type: LogFieldType.END_TIME,
        value: TimestampConverterUtils.makeElapsedTimestamp(BigInt(endTsNanos)),
      },
      {
        type: LogFieldType.DURATION,
        value: new TimeDuration(BigInt(endTsNanos - startTsNanos)).format(),
      },
      {
        type: LogFieldType.STATUS,
        value: status,
        icon: 'check',
        iconColor: 'green',
      },
    ];

    return {
      traceEntry: entry,
      fields,
      propertiesTree: undefined,
    };
  }
});
