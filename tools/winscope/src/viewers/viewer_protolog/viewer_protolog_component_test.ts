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
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {TimestampConverterUtils} from 'common/time/test_utils';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {LogSelectFilter} from 'viewers/common/log_filters';
import {executeScrollComponentTests} from 'viewers/common/scroll_component_tests';
import {LogHeader} from 'viewers/common/ui_data_log';
import {LogComponent} from 'viewers/components/log_component';
import {SearchBoxComponent} from 'viewers/components/search_box_component';
import {SelectWithFilterComponent} from 'viewers/components/select_with_filter_component';
import {ProtologScrollDirective} from './scroll_strategy/protolog_scroll_directive';
import {ProtologEntry, UiData} from './ui_data';
import {ViewerProtologComponent} from './viewer_protolog_component';

describe('ViewerProtologComponent', () => {
  const testSpec = {name: 'Test Column', cssClass: 'test-class'};
  const testField = {spec: testSpec, value: 'VALUE'};
  let fixture: ComponentFixture<ViewerProtologComponent>;
  let component: ViewerProtologComponent;
  let htmlElement: HTMLElement;

  describe('Main component', () => {
    beforeEach(async () => {
      await setUpTestEnvironment();
    });

    it('can be created', () => {
      expect(component).toBeTruthy();
    });

    it('render headers as filters', () => {
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

      const entryTimestamp = assertDefined(
        htmlElement.querySelector('.scroll .entry .time'),
      );
      expect(entryTimestamp.textContent?.trim()).toEqual('10ns');
    });

    it('shows go to current time button', () => {
      expect(htmlElement.querySelector('.go-to-current-time')).toBeTruthy();
    });
  });

  describe('Scroll component', () => {
    executeScrollComponentTests(setUpTestEnvironment);
  });

  async function setUpTestEnvironment(): Promise<
    [
      ComponentFixture<ViewerProtologComponent>,
      HTMLElement,
      CdkVirtualScrollViewport,
    ]
  > {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      imports: [
        ScrollingModule,
        MatFormFieldModule,
        FormsModule,
        MatInputModule,
        BrowserAnimationsModule,
        MatSelectModule,
        MatButtonModule,
        MatIconModule,
      ],
      declarations: [
        ViewerProtologComponent,
        SelectWithFilterComponent,
        LogComponent,
        ProtologScrollDirective,
        SearchBoxComponent,
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ViewerProtologComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;

    component.inputData = makeUiData();
    fixture.detectChanges();
    const viewport = assertDefined(component.logComponent?.scrollComponent);
    return [fixture, htmlElement, viewport];
  }

  function makeUiData(): UiData {
    const propertiesTree = new PropertyTreeBuilder()
      .setRootId('Protolog')
      .setName('tree')
      .setValue(null)
      .build();
    const ts = TimestampConverterUtils.makeElapsedTimestamp(10n);
    const trace = new TraceBuilder<PropertyTreeNode>()
      .setEntries([propertiesTree, propertiesTree])
      .setTimestamps([ts, ts])
      .build();

    const messages: ProtologEntry[] = [];
    const shortMessage = 'test information about message';
    const longMessage = shortMessage.repeat(10) + 'keep';
    const traceEntry = trace.getEntry(0);
    for (let i = 0; i < 200; i++) {
      messages.push(
        new ProtologEntry(traceEntry, [
          testField,
          testField,
          testField,
          {
            spec: {name: 'Test Column Text', cssClass: 'test-class-text'},
            value: i % 2 === 0 ? shortMessage : longMessage,
          },
        ]),
      );
    }
    return new UiData(
      [new LogHeader(testSpec, new LogSelectFilter([]))],
      messages,
      150,
      undefined,
      undefined,
    );
  }
});
