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

import {ScrollingModule} from '@angular/cdk/scrolling';
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {MatDividerModule} from '@angular/material/divider';
import {MatIconModule} from '@angular/material/icon';
import {assertDefined} from 'common/assert_utils';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Trace, TraceEntry} from 'trace/trace';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {LogComponent} from 'viewers/common/log_component';
import {LogField, LogFieldType} from 'viewers/common/ui_data_log';
import {CollapsedSectionsComponent} from 'viewers/components/collapsed_sections_component';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
import {PropertiesComponent} from 'viewers/components/properties_component';
import {PropertyTreeNodeDataViewComponent} from 'viewers/components/property_tree_node_data_view_component';
import {TreeComponent} from 'viewers/components/tree_component';
import {TreeNodeComponent} from 'viewers/components/tree_node_component';
import {Presenter} from './presenter';
import {TransitionsEntry, TransitionStatus, UiData} from './ui_data';
import {ViewerTransitionsComponent} from './viewer_transitions_component';

describe('ViewerTransitionsComponent', () => {
  let fixture: ComponentFixture<ViewerTransitionsComponent>;
  let component: ViewerTransitionsComponent;
  let htmlElement: HTMLElement;

  let transitionTree: PropertyTreeNode;
  let trace: Trace<PropertyTreeNode>;
  let entry: TraceEntry<PropertyTreeNode>;

  beforeAll(() => {
    transitionTree = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionTraceEntry')
      .setName('transition')
      .build();
    trace = new TraceBuilder<PropertyTreeNode>()
      .setEntries([transitionTree])
      .setTimestamps([TimestampConverterUtils.makeElapsedTimestamp(20n)])
      .build();
    entry = trace.getEntry(0);
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      imports: [MatDividerModule, ScrollingModule, MatIconModule],
      declarations: [
        ViewerTransitionsComponent,
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

    fixture = TestBed.createComponent(ViewerTransitionsComponent);
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
    expect(entry.innerHTML).toContain('TO_FRONT');
    expect(entry.innerHTML).toContain('10ns');
  });

  it('shows message when no transition is selected', () => {
    assertDefined(component.inputData).propertiesTree = undefined;
    fixture.detectChanges();
    expect(
      htmlElement.querySelector('.properties-view .placeholder-text')
        ?.innerHTML,
    ).toContain('No selected transition');
  });

  it('creates collapsed sections with no buttons', () => {
    UnitTestUtils.checkNoCollapsedSectionButtons(htmlElement);
  });

  it('handles properties section collapse/expand', () => {
    UnitTestUtils.checkSectionCollapseAndExpand(
      htmlElement,
      fixture,
      '.properties-view',
      'SELECTED TRANSITION',
    );
  });

  function makeUiData(): UiData {
    let mockTransitionIdCounter = 0;

    const transitions = [
      createMockTransition(entry, 20, 30, mockTransitionIdCounter++),
      createMockTransition(
        entry,
        42,
        50,
        mockTransitionIdCounter++,
        TransitionStatus.MERGED,
      ),
      createMockTransition(entry, 46, 49, mockTransitionIdCounter++),
      createMockTransition(
        entry,
        58,
        70,
        mockTransitionIdCounter++,
        TransitionStatus.ABORTED,
      ),
    ];

    const uiData = UiData.createEmpty();
    uiData.entries = transitions;
    uiData.selectedIndex = 0;
    uiData.headers = Presenter.FIELD_TYPES;
    return uiData;
  }

  function createMockTransition(
    entry: TraceEntry<PropertyTreeNode>,
    sendTimeNanos: number,
    finishTimeNanos: number,
    id: number,
    status = TransitionStatus.PLAYED,
  ): TransitionsEntry {
    const fields: LogField[] = [
      {
        type: LogFieldType.TRANSITION_ID,
        value: id,
      },
      {
        type: LogFieldType.TRANSITION_TYPE,
        value: 'TO_FRONT',
      },
      {
        type: LogFieldType.SEND_TIME,
        value: TimestampConverterUtils.makeElapsedTimestamp(
          BigInt(sendTimeNanos),
        ),
      },
      {
        type: LogFieldType.DISPATCH_TIME,
        value: TimestampConverterUtils.makeElapsedTimestamp(
          BigInt(sendTimeNanos) + 5n,
        ),
      },
      {
        type: LogFieldType.DURATION,
        value: (finishTimeNanos - sendTimeNanos).toString() + 'ns',
      },
      {
        type: LogFieldType.STATUS,
        value: status,
        icon: 'check',
        iconColor: 'green',
      },
    ];

    return new TransitionsEntry(entry, fields, transitionTree);
  }
});
