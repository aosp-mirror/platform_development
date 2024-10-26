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
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {Trace, TraceEntry} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {LogSelectFilter} from 'viewers/common/log_filters';
import {LogEntry, LogHeader} from 'viewers/common/ui_data_log';
import {CollapsedSectionsComponent} from 'viewers/components/collapsed_sections_component';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
import {LogComponent} from 'viewers/components/log_component';
import {PropertiesComponent} from 'viewers/components/properties_component';
import {PropertyTreeNodeDataViewComponent} from 'viewers/components/property_tree_node_data_view_component';
import {TreeComponent} from 'viewers/components/tree_component';
import {TreeNodeComponent} from 'viewers/components/tree_node_component';
import {CujEntry, UiData} from './ui_data';
import {ViewerJankCujsComponent} from './viewer_jank_cujs_component';

describe('ViewerJankCujsComponent', () => {
  const testSpec = {name: 'Test Column', cssClass: 'test-class'};
  const testField = {spec: testSpec, value: 'VALUE'};
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

  it('renders entries with field values and no trace timestamp', () => {
    expect(htmlElement.querySelector('.scroll')).toBeTruthy();
    const entry = assertDefined(
      htmlElement.querySelector(
        `.scroll .entry .${testSpec.cssClass.split(' ')[0]}`,
      ),
    );
    expect(entry.textContent).toContain('VALUE');
    expect(htmlElement.querySelector('.scroll .entry .time')).toBeNull();
  });

  it('hides go to current time button', () => {
    expect(htmlElement.querySelector('.go-to-current-time')).toBeNull();
  });

  function makeUiData(): UiData {
    const cujEntries = [
      createMockCujEntry(entry, 1),
      createMockCujEntry(entry, 2),
      createMockCujEntry(entry, 3),
      createMockCujEntry(entry, 4),
    ];

    const uiData = UiData.createEmpty();
    uiData.headers = [new LogHeader(testSpec, new LogSelectFilter([]))];
    uiData.entries = cujEntries;
    uiData.selectedIndex = 0;
    return uiData;
  }

  function createMockCujEntry(
    entry: TraceEntry<PropertyTreeNode>,
    i: number,
  ): LogEntry {
    return new CujEntry(
      entry,
      [testField, testField, testField, testField, testField],
      undefined,
    );
  }
});
