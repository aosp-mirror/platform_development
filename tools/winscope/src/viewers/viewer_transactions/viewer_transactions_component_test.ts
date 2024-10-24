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
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {LogComponent} from 'viewers/common/log_component';
import {executeScrollComponentTests} from 'viewers/common/scroll_component_tests';
import {LogFieldType, LogFilter} from 'viewers/common/ui_data_log';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {CollapsedSectionsComponent} from 'viewers/components/collapsed_sections_component';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
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

    it('renders headers as filters', () => {
      expect(htmlElement.querySelector('.headers .filter.pid')).toBeTruthy();
      expect(htmlElement.querySelector('.headers .filter.uid')).toBeTruthy();
      expect(
        htmlElement.querySelector('.headers .filter.transaction-type'),
      ).toBeTruthy();
      expect(
        htmlElement.querySelector('.headers .filter.transaction-id'),
      ).toBeTruthy();
    });

    it('renders entries', () => {
      expect(htmlElement.querySelector('.scroll')).toBeTruthy();

      const entry = assertDefined(htmlElement.querySelector('.scroll .entry'));
      expect(entry.textContent).toContain('1ns');
      expect(entry.textContent).toContain('-111');
      expect(entry.textContent).toContain('PID_VALUE');
      expect(entry.textContent).toContain('UID_VALUE');
      expect(entry.textContent).toContain('TYPE_VALUE');
      expect(entry.textContent).toContain('ID_VALUE');
      expect(entry.textContent).toContain('flag1 | flag2');
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
        [
          {type: LogFieldType.VSYNC_ID, value: -111},
          {type: LogFieldType.PID, value: 'PID_VALUE'},
          {type: LogFieldType.UID, value: 'UID_VALUE'},
          {type: LogFieldType.TRANSACTION_TYPE, value: 'TYPE_VALUE'},
          {
            type: LogFieldType.LAYER_OR_DISPLAY_ID,
            value: 'LAYER_OR_DISPLAY_ID_VALUE',
          },
          {type: LogFieldType.TRANSACTION_ID, value: 'TRANSACTION_ID_VALUE'},
          {type: LogFieldType.FLAGS, value: 'flag1 | flag2'},
        ],
        propertiesTree,
      );

      const entry2 = new TransactionsEntry(
        trace.getEntry(1),
        [
          {type: LogFieldType.VSYNC_ID, value: -222},
          {type: LogFieldType.PID, value: 'PID_VALUE_2'},
          {type: LogFieldType.UID, value: 'UID_VALUE_2'},
          {type: LogFieldType.TRANSACTION_TYPE, value: 'TYPE_VALUE_2'},
          {
            type: LogFieldType.LAYER_OR_DISPLAY_ID,
            value: 'LAYER_OR_DISPLAY_ID_VALUE_2',
          },
          {type: LogFieldType.TRANSACTION_ID, value: 'TRANSACTION_ID_VALUE_2'},
          {type: LogFieldType.FLAGS, value: 'flag3 | flag4'},
        ],
        propertiesTree,
      );

      return new UiData(
        [
          new LogFilter(LogFieldType.VSYNC_ID, ['-111', '-222']),
          new LogFilter(LogFieldType.PID, ['PID_VALUE', 'PID_VALUE_2']),
          new LogFilter(LogFieldType.UID, ['UID_VALUE', 'UID_VALUE_2']),
          new LogFilter(LogFieldType.TRANSACTION_TYPE, [
            'TYPE_VALUE',
            'TYPE_VALUE_2',
          ]),
          new LogFilter(LogFieldType.LAYER_OR_DISPLAY_ID, [
            'LAYER_OR_DISPLAY_ID_VALUE',
            'LAYER_OR_DISPLAY_ID_VALUE_2',
          ]),
          new LogFilter(LogFieldType.TRANSACTION_ID, [
            'TRANSACTION_ID_VALUE',
            'TRANSACTION_ID_VALUE_2',
          ]),
          new LogFilter(LogFieldType.FLAGS, [
            'flag1',
            'flag2',
            'flag3',
            'flag4',
          ]),
        ],
        [entry1, entry2],
        1,
        selectedEntryIndex,
        0,
        UiPropertyTreeNode.from(propertiesTree),
        {},
      );
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
      for (let i = 0; i < 200; i++) {
        const entry = new TransactionsEntry(
          trace.getEntry(0),
          [
            {type: LogFieldType.VSYNC_ID, value: -111},
            {type: LogFieldType.PID, value: 'PID_VALUE'},
            {type: LogFieldType.UID, value: 'UID_VALUE'},
            {type: LogFieldType.TRANSACTION_TYPE, value: 'TYPE_VALUE'},
            {
              type: LogFieldType.LAYER_OR_DISPLAY_ID,
              value: 'LAYER_OR_DISPLAY_ID_VALUE',
            },
            {
              type: LogFieldType.TRANSACTION_ID,
              value: 'TRANSACTION_ID_VALUE',
            },
            {
              type: LogFieldType.FLAGS,
              value: i % 2 === 0 ? shortMessage : longMessage,
            },
          ],
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
