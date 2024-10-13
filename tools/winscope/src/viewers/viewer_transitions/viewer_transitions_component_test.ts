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
import {
  CdkVirtualScrollViewport,
  ScrollingModule,
} from '@angular/cdk/scrolling';
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
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Trace, TraceEntry} from 'trace/trace';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {LogComponent} from 'viewers/common/log_component';
import {executeScrollComponentTests} from 'viewers/common/scroll_component_tests';
import {LogField, LogFieldType, LogFilter} from 'viewers/common/ui_data_log';
import {CollapsedSectionsComponent} from 'viewers/components/collapsed_sections_component';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
import {PropertiesComponent} from 'viewers/components/properties_component';
import {PropertyTreeNodeDataViewComponent} from 'viewers/components/property_tree_node_data_view_component';
import {SearchBoxComponent} from 'viewers/components/search_box_component';
import {SelectWithFilterComponent} from 'viewers/components/select_with_filter_component';
import {TreeComponent} from 'viewers/components/tree_component';
import {TreeNodeComponent} from 'viewers/components/tree_node_component';
import {Presenter} from './presenter';
import {TransitionsScrollDirective} from './scroll_strategy/transitions_scroll_directive';
import {TransitionsEntry, TransitionStatus, UiData} from './ui_data';
import {ViewerTransitionsComponent} from './viewer_transitions_component';

describe('ViewerTransitionsComponent', () => {
  let transitionTree: PropertyTreeNode;
  let trace: Trace<PropertyTreeNode>;
  let entry: TraceEntry<PropertyTreeNode>;
  let fixture: ComponentFixture<ViewerTransitionsComponent>;
  let component: ViewerTransitionsComponent;
  let htmlElement: HTMLElement;

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

  describe('Main component', () => {
    beforeEach(async () => {
      await setUpTestEnvironment();
    });

    it('can be created', () => {
      expect(component).toBeTruthy();
    });

    it('renders headers with plain titles and filters', () => {
      expect(
        htmlElement.querySelector('.headers .header.transition-id'),
      ).toBeTruthy();
      expect(
        htmlElement.querySelector('.headers .filter.transition-type'),
      ).toBeTruthy();
      expect(
        htmlElement.querySelector('.headers .header.send-time'),
      ).toBeTruthy();
      expect(
        htmlElement.querySelector('.headers .header.dispatch-time'),
      ).toBeTruthy();
      expect(
        htmlElement.querySelector('.headers .header.duration'),
      ).toBeTruthy();
      expect(
        htmlElement.querySelector('.headers .filter.handler'),
      ).toBeTruthy();
      expect(
        htmlElement.querySelector('.headers .filter.participants'),
      ).toBeTruthy();
      expect(htmlElement.querySelector('.headers .filter.flags')).toBeTruthy();
      expect(htmlElement.querySelector('.headers .filter.status')).toBeTruthy();
    });

    it('renders entries', () => {
      expect(htmlElement.querySelector('.scroll')).toBeTruthy();

      const entry = assertDefined(htmlElement.querySelector('.scroll .entry'));
      expect(entry.textContent).toContain('TO_FRONT');
      expect(entry.textContent).toContain('1ns');
      expect(entry.textContent).toContain('testHandler');
      expect(entry.textContent).toContain('FLAG | OTHER_FLAG');
      expect(entry.textContent).toContain('Layers: 1\nWindows: 0x423jf43');
      expect(entry.textContent).toContain('PLAYED');
    });

    it('shows message when no transition is selected', () => {
      assertDefined(component.inputData).propertiesTree = undefined;
      fixture.detectChanges();
      expect(
        htmlElement.querySelector('.properties-view .placeholder-text')
          ?.innerHTML,
      ).toContain('No current or selected transition');
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
  });

  describe('Scroll component', () => {
    executeScrollComponentTests(setUpTestEnvironment);
  });

  async function setUpTestEnvironment(): Promise<
    [
      ComponentFixture<ViewerTransitionsComponent>,
      HTMLElement,
      CdkVirtualScrollViewport,
    ]
  > {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      imports: [
        MatDividerModule,
        ScrollingModule,
        MatIconModule,
        ClipboardModule,
        MatFormFieldModule,
        MatButtonModule,
        MatInputModule,
        BrowserAnimationsModule,
        FormsModule,
        MatSelectModule,
      ],
      declarations: [
        ViewerTransitionsComponent,
        TreeComponent,
        TreeNodeComponent,
        PropertyTreeNodeDataViewComponent,
        PropertiesComponent,
        CollapsedSectionsComponent,
        CollapsibleSectionTitleComponent,
        LogComponent,
        SearchBoxComponent,
        TransitionsScrollDirective,
        SelectWithFilterComponent,
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ViewerTransitionsComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    component.inputData = makeUiData();
    fixture.detectChanges();
    const viewport = assertDefined(component.logComponent?.scrollComponent);
    return [fixture, htmlElement, viewport];
  }

  function makeUiData(): UiData {
    const transitions = [];
    for (let i = 0; i < 200; i++) {
      transitions.push(createMockTransition(entry, i + 1, i + 2, i));
    }
    const uiData = UiData.createEmpty();
    uiData.entries = transitions;
    uiData.selectedIndex = 0;
    uiData.headers = Presenter.FIELD_TYPES.map((type) => {
      if (Presenter.FILTER_TYPES.includes(type)) {
        return new LogFilter(type, ['test']);
      }
      return type;
    });
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
        type: LogFieldType.HANDLER,
        value: 'testHandler',
      },
      {
        type: LogFieldType.PARTICIPANTS,
        value: 'Layers: 1\nWindows: 0x423jf43',
      },
      {
        type: LogFieldType.FLAGS,
        value: 'FLAG | OTHER_FLAG',
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
