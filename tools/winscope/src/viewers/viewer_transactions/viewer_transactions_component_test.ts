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
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {LogSelectFilter} from 'viewers/common/log_filters';
import {executeScrollComponentTests} from 'viewers/common/scroll_component_tests';
import {LogHeader} from 'viewers/common/ui_data_log';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {CollapsedSectionsComponent} from 'viewers/components/collapsed_sections_component';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
import {LogComponent} from 'viewers/components/log_component';
import {PropertiesComponent} from 'viewers/components/properties_component';
import {PropertyTreeNodeDataViewComponent} from 'viewers/components/property_tree_node_data_view_component';
import {SearchBoxComponent} from 'viewers/components/search_box_component';
import {SelectWithFilterComponent} from 'viewers/components/select_with_filter_component';
import {TreeComponent} from 'viewers/components/tree_component';
import {TreeNodeComponent} from 'viewers/components/tree_node_component';
import {TransactionsScrollDirective} from './scroll_strategy/transactions_scroll_directive';
import {TransactionsEntry, UiData} from './ui_data';
import {ViewerTransactionsComponent} from './viewer_transactions_component';

describe('ViewerTransactionsComponent', () => {
  const testSpec = {name: 'Test Column', cssClass: 'test-class'};
  const testField = {spec: testSpec, value: 'VALUE'};
  let fixture: ComponentFixture<ViewerTransactionsComponent>;
  let component: ViewerTransactionsComponent;
  let htmlElement: HTMLElement;

  describe('Main component', () => {
    beforeEach(async () => {
      await setUpTestEnvironment(() => makeUiData(0));
    });

    it('can be created', () => {
      expect(component).toBeTruthy();
    });

    it('renders log component', () => {
      expect(htmlElement.querySelector('.log-view')).toBeTruthy();
    });

    it('render headers as filters', () => {
      expect(
        htmlElement.querySelector(
          `.headers .filter.${testSpec.cssClass.split(' ')[0]}`,
        ),
      ).toBeTruthy();
    });

    it('renders entries with field values and trace timestamp', () => {
      expect(htmlElement.querySelector('.scroll')).toBeTruthy();
      const entry = assertDefined(
        htmlElement.querySelector(
          `.scroll .entry .${testSpec.cssClass.split(' ')[0]}`,
        ),
      );
      expect(entry.textContent).toContain('VALUE');

      const entryTimestamp = assertDefined(
        htmlElement.querySelector('.scroll .entry .time'),
      );
      expect(entryTimestamp.textContent?.trim()).toEqual('1ns');
    });

    it('shows go to current time button', () => {
      expect(htmlElement.querySelector('.go-to-current-time')).toBeTruthy();
    });

    it('renders properties', () => {
      expect(htmlElement.querySelector('.properties-view')).toBeTruthy();
    });

    it('shows message when no transaction is selected', () => {
      assertDefined(component.inputData).propertiesTree = undefined;
      fixture.detectChanges();
      expect(
        htmlElement.querySelector('.properties-view .placeholder-text')
          ?.textContent,
      ).toContain('No current or selected transaction');
    });

    it('creates collapsed sections with no buttons', () => {
      UnitTestUtils.checkNoCollapsedSectionButtons(htmlElement);
    });

    it('handles properties section collapse/expand', () => {
      UnitTestUtils.checkSectionCollapseAndExpand(
        htmlElement,
        fixture,
        '.properties-view',
        'PROPERTIES - PROTO DUMP',
      );
    });

    function makeUiData(selectedEntryIndex: number): UiData {
      const propertiesTree = new PropertyTreeBuilder()
        .setRootId('Transactions')
        .setName('tree')
        .setValue(null)
        .build();

      const ts = TimestampConverterUtils.makeElapsedTimestamp(1n);

      const trace = new TraceBuilder<PropertyTreeNode>()
        .setEntries([propertiesTree, propertiesTree])
        .setTimestamps([ts, ts])
        .build();

      const entry1 = new TransactionsEntry(
        trace.getEntry(0),
        Array.from({length: 7}, () => testField),
        propertiesTree,
      );

      const uiData = new UiData(
        [new LogHeader(testSpec, new LogSelectFilter([]))],
        [entry1],
        1,
        selectedEntryIndex,
        0,
        UiPropertyTreeNode.from(propertiesTree),
        {},
      );

      return uiData;
    }
  });

  describe('Scroll component', () => {
    executeScrollComponentTests(() =>
      setUpTestEnvironment(makeUiDataForScroll),
    );

    function makeUiDataForScroll(): UiData {
      const propertiesTree = new PropertyTreeBuilder()
        .setRootId('Transactions')
        .setName('tree')
        .setValue(null)
        .build();

      const ts = TimestampConverterUtils.makeElapsedTimestamp(1n);

      const trace = new TraceBuilder<PropertyTreeNode>()
        .setType(TraceType.TRANSACTIONS)
        .setEntries([propertiesTree, propertiesTree])
        .setTimestamps([ts, ts])
        .build();

      const uiData = new UiData(
        [],
        [],
        0,
        0,
        0,
        UiPropertyTreeNode.from(propertiesTree),
        {},
      );
      const shortMessage = 'flag1 | flag2';
      const longMessage = shortMessage.repeat(20);
      const traceEntry = trace.getEntry(0);

      for (let i = 0; i < 200; i++) {
        const entry = new TransactionsEntry(
          traceEntry,
          Array.from({length: 6}, () => testField).concat([
            {
              spec: {name: 'Test Column Flags', cssClass: 'test-class-flags'},
              value: i % 2 === 0 ? shortMessage : longMessage,
            },
          ]),
          propertiesTree,
        );
        uiData.entries.push(entry);
      }
      return uiData;
    }
  });

  async function setUpTestEnvironment(
    makeUiData: () => UiData,
  ): Promise<
    [
      ComponentFixture<ViewerTransactionsComponent>,
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
        ViewerTransactionsComponent,
        TransactionsScrollDirective,
        SelectWithFilterComponent,
        CollapsedSectionsComponent,
        CollapsibleSectionTitleComponent,
        PropertiesComponent,
        TreeComponent,
        TreeNodeComponent,
        PropertyTreeNodeDataViewComponent,
        SearchBoxComponent,
        LogComponent,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ViewerTransactionsComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;

    component.inputData = makeUiData();
    fixture.detectChanges();
    const viewport = assertDefined(component.logComponent?.scrollComponent);
    return [fixture, htmlElement, viewport];
  }
});
