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
import {UnitTestUtils} from 'test/unit/utils';
import {TIMESTAMP_NODE_FORMATTER} from 'trace/tree_node/formatters';
import {executeScrollComponentTests} from 'viewers/common/scroll_component_test_utils';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {CollapsedSectionsComponent} from 'viewers/components/collapsed_sections_component';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
import {PropertiesComponent} from 'viewers/components/properties_component';
import {SelectWithFilterComponent} from 'viewers/components/select_with_filter_component';
import {TransactionsScrollDirective} from './scroll_strategy/transactions_scroll_directive';
import {UiData, UiDataEntry} from './ui_data';
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

    it('renders filters', () => {
      expect(htmlElement.querySelector('.entries .filters .pid')).toBeTruthy();
      expect(htmlElement.querySelector('.entries .filters .uid')).toBeTruthy();
      expect(htmlElement.querySelector('.entries .filters .type')).toBeTruthy();
      expect(htmlElement.querySelector('.entries .filters .id')).toBeTruthy();
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

    it('applies transaction id filter correctly', async () => {
      const allEntries = makeUiData(0).entries;
      htmlElement.addEventListener(
        ViewerEvents.TransactionIdFilterChanged,
        (event) => {
          if ((event as CustomEvent).detail.length === 0) {
            component.uiData.entries = allEntries;
            return;
          }
          component.uiData.entries = allEntries.filter((entry) =>
            (event as CustomEvent).detail.includes(entry.transactionId),
          );
        },
      );
      await checkSelectFilter('.transaction-id');
    });

    it('applies vsync id filter correctly', async () => {
      const allEntries = makeUiData(0).entries;
      htmlElement.addEventListener(
        ViewerEvents.VSyncIdFilterChanged,
        (event) => {
          if ((event as CustomEvent).detail.length === 0) {
            component.uiData.entries = allEntries;
            return;
          }
          component.uiData.entries = allEntries.filter((entry) => {
            return (event as CustomEvent).detail.includes(`${entry.vsyncId}`);
          });
        },
      );
      await checkSelectFilter('.vsyncid');
    });

    it('applies pid filter correctly', async () => {
      const allEntries = makeUiData(0).entries;
      htmlElement.addEventListener(ViewerEvents.PidFilterChanged, (event) => {
        if ((event as CustomEvent).detail.length === 0) {
          component.uiData.entries = allEntries;
          return;
        }
        component.uiData.entries = allEntries.filter((entry) => {
          return (event as CustomEvent).detail.includes(entry.pid);
        });
      });
      await checkSelectFilter('.pid');
    });

    it('applies uid filter correctly', async () => {
      const allEntries = makeUiData(0).entries;
      htmlElement.addEventListener(ViewerEvents.UidFilterChanged, (event) => {
        if ((event as CustomEvent).detail.length === 0) {
          component.uiData.entries = allEntries;
          return;
        }
        component.uiData.entries = allEntries.filter((entry) => {
          return (event as CustomEvent).detail.includes(entry.uid);
        });
      });
      await checkSelectFilter('.uid');
    });

    it('applies type filter correctly', async () => {
      const allEntries = makeUiData(0).entries;
      htmlElement.addEventListener(ViewerEvents.TypeFilterChanged, (event) => {
        if ((event as CustomEvent).detail.length === 0) {
          component.uiData.entries = allEntries;
          return;
        }
        component.uiData.entries = allEntries.filter((entry) => {
          return (event as CustomEvent).detail.includes(entry.type);
        });
      });
      await checkSelectFilter('.type');
    });

    it('applies layer/display id filter correctly', async () => {
      const allEntries = makeUiData(0).entries;
      htmlElement.addEventListener(
        ViewerEvents.LayerIdFilterChanged,
        (event) => {
          if ((event as CustomEvent).detail.length === 0) {
            component.uiData.entries = allEntries;
            return;
          }
          component.uiData.entries = allEntries.filter((entry) => {
            return (event as CustomEvent).detail.includes(
              entry.layerOrDisplayId,
            );
          });
        },
      );
      await checkSelectFilter('.layer-or-display-id');
    });

    it('applies what filter correctly', async () => {
      const allEntries = makeUiData(0).entries;
      htmlElement.addEventListener(ViewerEvents.WhatFilterChanged, (event) => {
        if ((event as CustomEvent).detail.length === 0) {
          component.uiData.entries = allEntries;
          return;
        }
        component.uiData.entries = allEntries.filter((entry) => {
          return (event as CustomEvent).detail.some((allowed: string) => {
            return entry.what.includes(allowed);
          });
        });
      });
      await checkSelectFilter('.what');
    });

    it('scrolls to current entry on button click', () => {
      const goToCurrentTimeButton = assertDefined(
        htmlElement.querySelector('.go-to-current-time'),
      ) as HTMLButtonElement;
      const spy = spyOn(
        assertDefined(component.scrollComponent),
        'scrollToIndex',
      );
      goToCurrentTimeButton.click();
      expect(spy).toHaveBeenCalledWith(1);
    });

    it('changes selected entry on arrow key press', () => {
      htmlElement.addEventListener(
        ViewerEvents.LogChangedByKeyPress,
        (event) => {
          component.inputData = makeUiData((event as CustomEvent).detail);
          fixture.detectChanges();
        },
      );

      // does not do anything if no prev entry available
      component.handleKeyboardEvent(
        new KeyboardEvent('keydown', {key: 'ArrowUp'}),
      );
      expect(component.uiData.selectedEntryIndex).toEqual(0);

      component.handleKeyboardEvent(
        new KeyboardEvent('keydown', {key: 'ArrowDown'}),
      );
      expect(component.uiData.selectedEntryIndex).toEqual(1);

      component.handleKeyboardEvent(
        new KeyboardEvent('keydown', {key: 'ArrowUp'}),
      );
      expect(component.uiData.selectedEntryIndex).toEqual(0);
    });

    it('propagates timestamp on click', () => {
      component.inputData = makeUiData(0);
      fixture.detectChanges();
      let index: number | undefined;
      htmlElement.addEventListener(ViewerEvents.TimestampClick, (event) => {
        index = (event as CustomEvent).detail.index;
      });
      const logTimestampButton = assertDefined(
        htmlElement.querySelector('.time button'),
      ) as HTMLButtonElement;
      logTimestampButton.click();

      expect(index).toEqual(0);
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

      const time = new PropertyTreeBuilder()
        .setRootId(propertiesTree.id)
        .setName('timestamp')
        .setValue(TimestampConverterUtils.makeElapsedTimestamp(1n))
        .setFormatter(TIMESTAMP_NODE_FORMATTER)
        .build();

      const entry = new UiDataEntry(
        0,
        time,
        -111,
        'PID_VALUE',
        'UID_VALUE',
        'TYPE_VALUE',
        'LAYER_OR_DISPLAY_ID_VALUE',
        'TRANSACTION_ID_VALUE',
        'flag1 | flag2',
        propertiesTree,
      );

      const entry2 = new UiDataEntry(
        1,
        time,
        -222,
        'PID_VALUE_2',
        'UID_VALUE_2',
        'TYPE_VALUE_2',
        'LAYER_OR_DISPLAY_ID_VALUE_2',
        'TRANSACTION_ID_VALUE_2',
        'flag3 | flag4',
        propertiesTree,
      );

      return new UiData(
        ['-111', '-222'],
        ['PID_VALUE', 'PID_VALUE_2'],
        ['UID_VALUE', 'UID_VALUE_2'],
        ['TYPE_VALUE', 'TYPE_VALUE_2'],
        ['LAYER_OR_DISPLAY_ID_VALUE', 'LAYER_OR_DISPLAY_ID_VALUE_2'],
        ['TRANSACTION_ID_VALUE', 'TRANSACTION_ID_VALUE_2'],
        ['flag1', 'flag2', 'flag3', 'flag4'],
        [entry, entry2],
        1,
        selectedEntryIndex,
        0,
        UiPropertyTreeNode.from(propertiesTree),
        {},
      );
    }

    async function checkSelectFilter(filterSelector: string) {
      component.inputData = makeUiData(0);
      fixture.detectChanges();
      expect(component.uiData.entries.length).toEqual(2);
      const filterTrigger = assertDefined(
        htmlElement.querySelector(
          `.filters ${filterSelector} .mat-select-trigger`,
        ),
      ) as HTMLInputElement;
      filterTrigger.click();
      await fixture.whenStable();

      const firstOption = assertDefined(
        document.querySelector('.mat-select-panel .mat-option'),
      ) as HTMLElement;
      firstOption.click();
      fixture.detectChanges();
      expect(component.uiData.entries.length).toEqual(1);

      firstOption.click();
      fixture.detectChanges();
      expect(component.uiData.entries.length).toEqual(2);
    }
  });

  describe('Scroll component', () => {
    executeScrollComponentTests('entry', setUpTestEnvironment);

    function makeUiDataForScroll(): UiData {
      const propertiesTree = new PropertyTreeBuilder()
        .setRootId('Transactions')
        .setName('tree')
        .setValue(null)
        .build();

      const time = new PropertyTreeBuilder()
        .setRootId(propertiesTree.id)
        .setName('timestamp')
        .setValue(TimestampConverterUtils.makeElapsedTimestamp(1n))
        .setFormatter(TIMESTAMP_NODE_FORMATTER)
        .build();

      const uiData = new UiData(
        [],
        [],
        [],
        [],
        [],
        [],
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
        const entry = new UiDataEntry(
          0,
          time,
          -111,
          'PID_VALUE',
          'UID_VALUE',
          'TYPE_VALUE',
          'LAYER_OR_DISPLAY_ID_VALUE',
          'TRANSACTION_ID_VALUE',
          i % 2 === 0 ? shortMessage : longMessage,
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
          TransactionsScrollDirective,
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
      }).compileComponents();
      const fixture = TestBed.createComponent(ViewerTransactionsComponent);
      const transactionsComponent = fixture.componentInstance;
      const htmlElement = fixture.nativeElement;
      const viewport = assertDefined(transactionsComponent.scrollComponent);
      transactionsComponent.inputData = makeUiDataForScroll();
      return [fixture, htmlElement, viewport];
    }
  });
});
