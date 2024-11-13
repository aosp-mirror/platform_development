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

import {
  CdkVirtualScrollViewport,
  ScrollingModule,
} from '@angular/cdk/scrolling';
import {CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
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
import {LogFieldType} from 'viewers/common/ui_data_log';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {CollapsedSectionsComponent} from 'viewers/components/collapsed_sections_component';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
import {PropertiesComponent} from 'viewers/components/properties_component';
import {SelectWithFilterComponent} from 'viewers/components/select_with_filter_component';
import {TransactionsScrollDirective} from './scroll_strategy/transactions_scroll_directive';
import {TransactionsEntry, UiData} from './ui_data';
import {ViewerTransactionsComponent} from './viewer_transactions_component';

describe('ViewerTransactionsComponent', () => {
  describe('Main component', () => {
    let fixture: ComponentFixture<ViewerTransactionsComponent>;
    let component: ViewerTransactionsComponent;
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
        ],
        declarations: [
          ViewerTransactionsComponent,
          TransactionsScrollDirective,
          SelectWithFilterComponent,
          CollapsedSectionsComponent,
          CollapsibleSectionTitleComponent,
          PropertiesComponent,
          LogComponent,
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
      }).compileComponents();

      fixture = TestBed.createComponent(ViewerTransactionsComponent);
      component = fixture.componentInstance;
      htmlElement = fixture.nativeElement;

      component.inputData = makeUiData(0);
      fixture.detectChanges();
    });

    it('can be created', () => {
      expect(component).toBeTruthy();
    });

    it('renders log component', () => {
      expect(htmlElement.querySelector('.log-view')).toBeTruthy();
    });

    it('renders filters', () => {
      expect(htmlElement.querySelector('.filters .pid')).toBeTruthy();
      expect(htmlElement.querySelector('.filters .uid')).toBeTruthy();
      expect(
        htmlElement.querySelector('.filters .transaction-type'),
      ).toBeTruthy();
      expect(
        htmlElement.querySelector('.filters .transaction-id'),
      ).toBeTruthy();
    });

    it('renders entries', () => {
      expect(htmlElement.querySelector('.scroll')).toBeTruthy();

      const entry = assertDefined(htmlElement.querySelector('.scroll .entry'));
      expect(entry.innerHTML).toContain('1ns');
      expect(entry.innerHTML).toContain('-111');
      expect(entry.innerHTML).toContain('PID_VALUE');
      expect(entry.innerHTML).toContain('UID_VALUE');
      expect(entry.innerHTML).toContain('TYPE_VALUE');
      expect(entry.innerHTML).toContain('ID_VALUE');
      expect(entry.innerHTML).toContain('flag1 | flag2');
    });

    it('renders properties', () => {
      expect(htmlElement.querySelector('.properties-view')).toBeTruthy();
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
          {type: LogFieldType.VSYNC_ID, options: ['-111', '-222']},
          {type: LogFieldType.PID, options: ['PID_VALUE', 'PID_VALUE_2']},
          {type: LogFieldType.UID, options: ['UID_VALUE', 'UID_VALUE_2']},
          {
            type: LogFieldType.TRANSACTION_TYPE,
            options: ['TYPE_VALUE', 'TYPE_VALUE_2'],
          },
          {
            type: LogFieldType.LAYER_OR_DISPLAY_ID,
            options: [
              'LAYER_OR_DISPLAY_ID_VALUE',
              'LAYER_OR_DISPLAY_ID_VALUE_2',
            ],
          },
          {
            type: LogFieldType.TRANSACTION_ID,
            options: ['TRANSACTION_ID_VALUE', 'TRANSACTION_ID_VALUE_2'],
          },
          {
            type: LogFieldType.FLAGS,
            options: ['flag1', 'flag2', 'flag3', 'flag4'],
          },
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
    executeScrollComponentTests(setUpTestEnvironment);

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

    async function setUpTestEnvironment(): Promise<
      [
        ComponentFixture<ViewerTransactionsComponent>,
        HTMLElement,
        CdkVirtualScrollViewport,
      ]
    > {
      await TestBed.configureTestingModule({
        providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
        imports: [ScrollingModule],
        declarations: [
          ViewerTransactionsComponent,
          LogComponent,
          TransactionsScrollDirective,
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
      }).compileComponents();
      const fixture = TestBed.createComponent(ViewerTransactionsComponent);
      const transactionsComponent = fixture.componentInstance;
      const htmlElement = fixture.nativeElement;
      transactionsComponent.inputData = makeUiDataForScroll();
      fixture.detectChanges();
      const viewport = assertDefined(
        transactionsComponent.logComponent?.scrollComponent,
      );
      return [fixture, htmlElement, viewport];
    }
  });
});
