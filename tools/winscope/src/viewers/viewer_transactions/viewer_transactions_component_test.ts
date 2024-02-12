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
import {CdkVirtualScrollViewport, ScrollingModule} from '@angular/cdk/scrolling';
import {CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA} from '@angular/core';
import {ComponentFixture, ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {MatDividerModule} from '@angular/material/divider';
import {assertDefined} from 'common/assert_utils';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {executeScrollComponentTests} from 'viewers/common/scroll_component_test_utils';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
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
        imports: [MatDividerModule, ScrollingModule],
        declarations: [ViewerTransactionsComponent, TransactionsScrollDirective],
        schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA],
      }).compileComponents();

      fixture = TestBed.createComponent(ViewerTransactionsComponent);
      component = fixture.componentInstance;
      htmlElement = fixture.nativeElement;

      component.inputData = makeUiData();
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
      expect(entry.innerHTML).toContain('TIME_VALUE');
      expect(entry.innerHTML).toContain('-111');
      expect(entry.innerHTML).toContain('PID_VALUE');
      expect(entry.innerHTML).toContain('UID_VALUE');
      expect(entry.innerHTML).toContain('TYPE_VALUE');
      expect(entry.innerHTML).toContain('ID_VALUE');
      expect(entry.innerHTML).toContain('flag1 | flag2');
    });

    it('renders properties', () => {
      expect(htmlElement.querySelector('.properties-tree')).toBeTruthy();
    });

    function makeUiData(): UiData {
      const propertiesTree = new PropertyTreeBuilder()
        .setRootId('Transactions')
        .setName('tree')
        .setValue(null)
        .build();

      const entry = new UiDataEntry(
        0,
        'TIME_VALUE',
        -111,
        'PID_VALUE',
        'UID_VALUE',
        'TYPE_VALUE',
        'LAYER_OR_DISPLAY_ID_VALUE',
        'TRANSACTION_ID_VALUE',
        'flag1 | flag2',
        propertiesTree
      );

      const entry2 = new UiDataEntry(
        1,
        'TIME_VALUE',
        -222,
        'PID_VALUE_2',
        'UID_VALUE_2',
        'TYPE_VALUE_2',
        'LAYER_OR_DISPLAY_ID_VALUE_2',
        'TRANSACTION_ID_VALUE_2',
        'flag3 | flag4',
        propertiesTree
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
        0,
        0,
        0,
        UiPropertyTreeNode.from(propertiesTree),
        {}
      );
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
        {}
      );
      const shortMessage = 'flag1 | flag2';
      const longMessage = shortMessage.repeat(20);
      for (let i = 0; i < 200; i++) {
        const entry = new UiDataEntry(
          0,
          'TIME_VALUE',
          -111,
          'PID_VALUE',
          'UID_VALUE',
          'TYPE_VALUE',
          'LAYER_OR_DISPLAY_ID_VALUE',
          'TRANSACTION_ID_VALUE',
          i % 2 === 0 ? shortMessage : longMessage,
          propertiesTree
        );
        uiData.entries.push(entry);
      }
      return uiData;
    }

    async function setUpTestEnvironment(): Promise<
      [ComponentFixture<ViewerTransactionsComponent>, HTMLElement, CdkVirtualScrollViewport]
    > {
      await TestBed.configureTestingModule({
        providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
        imports: [ScrollingModule],
        declarations: [ViewerTransactionsComponent, TransactionsScrollDirective],
        schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA],
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
