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
import {TimestampConverterUtils} from 'common/time/test_utils';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Trace, TraceEntry} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {LogSelectFilter} from 'viewers/common/log_filters';
import {executeScrollComponentTests} from 'viewers/common/scroll_component_tests';
import {LogHeader} from 'viewers/common/ui_data_log';
import {CollapsedSectionsComponent} from 'viewers/components/collapsed_sections_component';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
import {LogComponent} from 'viewers/components/log_component';
import {PropertiesComponent} from 'viewers/components/properties_component';
import {PropertyTreeNodeDataViewComponent} from 'viewers/components/property_tree_node_data_view_component';
import {SearchBoxComponent} from 'viewers/components/search_box_component';
import {SelectWithFilterComponent} from 'viewers/components/select_with_filter_component';
import {TreeComponent} from 'viewers/components/tree_component';
import {TreeNodeComponent} from 'viewers/components/tree_node_component';
import {TransitionsScrollDirective} from './scroll_strategy/transitions_scroll_directive';
import {TransitionsEntry, UiData} from './ui_data';
import {ViewerTransitionsComponent} from './viewer_transitions_component';

describe('ViewerTransitionsComponent', () => {
  const testSpec = {name: 'Test Column', cssClass: 'test-class'};
  const testField = {spec: testSpec, value: 'VALUE'};
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
      .setType(TraceType.TRANSITION)
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

    it('renders headers with filters', () => {
      expect(
        htmlElement.querySelector(
          `.headers .filter.${testSpec.cssClass.split(' ')[0]}`,
        ),
      ).toBeTruthy();
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

    it('renders properties', () => {
      expect(htmlElement.querySelector('.properties-view')).toBeTruthy();
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
      transitions.push(createMockTransition(entry, i));
    }
    const uiData = UiData.createEmpty();
    uiData.headers = [new LogHeader(testSpec, new LogSelectFilter([]))];
    uiData.entries = transitions;
    uiData.selectedIndex = 0;
    return uiData;
  }

  function createMockTransition(
    entry: TraceEntry<PropertyTreeNode>,
    i: number,
  ): TransitionsEntry {
    return new TransitionsEntry(
      entry,
      [
        testField,
        testField,
        testField,
        testField,
        testField,
        testField,
        {spec: testSpec, value: i % 2 === 0 ? 'VALUE' : 'VALUE'.repeat(40)},
      ],
      transitionTree,
    );
  }
});
