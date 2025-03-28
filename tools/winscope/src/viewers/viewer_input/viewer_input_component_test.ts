/*
 * Copyright 2024 The Android Open Source Project
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
import {HttpClientModule} from '@angular/common/http';
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
import {MatSliderModule} from '@angular/material/slider';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Trace, TraceEntry} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {LogSelectFilter} from 'viewers/common/log_filters';
import {LogHeader} from 'viewers/common/ui_data_log';
import {CollapsedSectionsComponent} from 'viewers/components/collapsed_sections_component';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
import {LogComponent} from 'viewers/components/log_component';
import {PropertiesComponent} from 'viewers/components/properties_component';
import {PropertyTreeNodeDataViewComponent} from 'viewers/components/property_tree_node_data_view_component';
import {RectsComponent} from 'viewers/components/rects/rects_component';
import {SearchBoxComponent} from 'viewers/components/search_box_component';
import {SelectWithFilterComponent} from 'viewers/components/select_with_filter_component';
import {TreeComponent} from 'viewers/components/tree_component';
import {TreeNodeComponent} from 'viewers/components/tree_node_component';
import {UserOptionsComponent} from 'viewers/components/user_options_component';
import {InputEntry, UiData} from './ui_data';
import {ViewerInputComponent} from './viewer_input_component';

describe('ViewerInputComponent', () => {
  const testSpec = {name: 'Test Column', cssClass: 'test-class'};
  const testField = {spec: testSpec, value: 'VALUE'};
  let fixture: ComponentFixture<ViewerInputComponent>;
  let component: ViewerInputComponent;
  let htmlElement: HTMLElement;

  let tree: PropertyTreeNode;
  let trace: Trace<PropertyTreeNode>;
  let entry: TraceEntry<PropertyTreeNode>;

  beforeAll(async () => {
    tree = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('AndroidMotionEvent')
      .setName('entry')
      .build();
    trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.INPUT_EVENT_MERGED)
      .setEntries([tree])
      .setTimestamps([TimestampConverterUtils.makeElapsedTimestamp(20n)])
      .build();
    entry = trace.getEntry(0);
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      imports: [
        MatSliderModule,
        MatTooltipModule,
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
        HttpClientModule,
      ],
      declarations: [
        ViewerInputComponent,
        TreeComponent,
        TreeNodeComponent,
        PropertyTreeNodeDataViewComponent,
        PropertiesComponent,
        CollapsedSectionsComponent,
        CollapsibleSectionTitleComponent,
        LogComponent,
        RectsComponent,
        SelectWithFilterComponent,
        SearchBoxComponent,
        UserOptionsComponent,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ViewerInputComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;

    component.inputData = makeUiData();
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders filter in title', () => {
    expect(htmlElement.querySelector('.headers .filter')).toBeNull();
    expect(
      htmlElement.querySelector(
        `.title-section .filter.${testSpec.cssClass.split(' ')[0]}`,
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

  it('shows message when no event is selected', () => {
    assertDefined(component.inputData).propertiesTree = undefined;
    assertDefined(component.inputData).dispatchPropertiesTree = undefined;
    fixture.detectChanges();
    expect(
      htmlElement.querySelector('.event-properties .placeholder-text')
        ?.innerHTML,
    ).toContain('No selected entry');
    expect(
      htmlElement.querySelector('.dispatch-properties .placeholder-text')
        ?.innerHTML,
    ).toContain('No selected entry');
  });

  it('creates collapsed sections with no buttons', () => {
    UnitTestUtils.checkNoCollapsedSectionButtons(htmlElement);
  });

  it('handles collapse/expand', () => {
    UnitTestUtils.checkSectionCollapseAndExpand(
      htmlElement,
      fixture,
      '.rects-view',
      'INPUT WINDOWS',
    );
    UnitTestUtils.checkSectionCollapseAndExpand(
      htmlElement,
      fixture,
      '.event-properties',
      'EVENT DETAILS',
    );
    UnitTestUtils.checkSectionCollapseAndExpand(
      htmlElement,
      fixture,
      '.dispatch-properties',
      'DISPATCH DETAILS',
    );
    UnitTestUtils.checkSectionCollapseAndExpand(
      htmlElement,
      fixture,
      '.log-view',
      'EVENT LOG',
    );
  });

  it('shows rects view when rects are defined', () => {
    assertDefined(component.inputData).rectsToDraw = [];
    fixture.detectChanges();
    expect(htmlElement.querySelector('.rects-view')).toBeTruthy();
  });

  it('hides rects view when rects are not defined', () => {
    assertDefined(component.inputData).rectsToDraw = undefined;
    fixture.detectChanges();
    expect(htmlElement.querySelector('.rects-view')).toBeNull();
  });

  function makeUiData(): UiData {
    const entries = [
      createInputEntry(entry, 1),
      createInputEntry(entry, 2),
      createInputEntry(entry, 3),
    ];

    const uiData = UiData.createEmpty();
    uiData.headers = [new LogHeader(testSpec, new LogSelectFilter([]))];
    uiData.entries = entries;
    uiData.selectedIndex = 0;

    uiData.rectsToDraw = [];
    return uiData;
  }

  function createInputEntry(
    entry: TraceEntry<PropertyTreeNode>,
    num: number,
  ): InputEntry {
    return new InputEntry(
      entry,
      [
        {
          spec: testSpec,
          value: 'VALUE',
          propagateEntryTimestamp: true,
        },
        testField,
        testField,
        testField,
        testField,
        testField,
        testField,
      ],
      tree,
      tree,
      undefined,
    );
  }
});
