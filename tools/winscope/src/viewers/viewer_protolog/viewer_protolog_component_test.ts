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
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {LogComponent} from 'viewers/common/log_component';
import {executeScrollComponentTests} from 'viewers/common/scroll_component_test_utils';
import {LogFieldType} from 'viewers/common/ui_data_log';
import {SelectWithFilterComponent} from 'viewers/components/select_with_filter_component';
import {ProtologScrollDirective} from './scroll_strategy/protolog_scroll_directive';
import {ProtologEntry, UiData} from './ui_data';
import {ViewerProtologComponent} from './viewer_protolog_component';

describe('ViewerProtologComponent', () => {
  describe('Main component', () => {
    let fixture: ComponentFixture<ViewerProtologComponent>;
    let component: ViewerProtologComponent;
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
        ],
        declarations: [
          ViewerProtologComponent,
          SelectWithFilterComponent,
          LogComponent,
          ProtologScrollDirective,
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
      }).compileComponents();
      fixture = TestBed.createComponent(ViewerProtologComponent);
      component = fixture.componentInstance;
      htmlElement = fixture.nativeElement;

      component.inputData = makeUiData();
      fixture.detectChanges();
    });

    it('can be created', () => {
      expect(component).toBeTruthy();
    });

    it('creates message filters', () => {
      expect(htmlElement.querySelector('.filters .log-level')).toBeTruthy();
      expect(htmlElement.querySelector('.filters .tag')).toBeTruthy();
      expect(htmlElement.querySelector('.filters .source-file')).toBeTruthy();
      expect(htmlElement.querySelector('.filters .text')).toBeTruthy();
    });

    it('renders log messages', () => {
      expect(htmlElement.querySelector('.scroll')).toBeTruthy();
      const entry = assertDefined(htmlElement.querySelector('.scroll .entry'));
      expect(entry.innerHTML).toContain('INFO');
      expect(entry.innerHTML).toContain('WindowManager');
      expect(entry.innerHTML).toContain('test_source_file.java');
      expect(entry.innerHTML).toContain('test information about message');
    });
  });

  describe('Scroll component', () => {
    executeScrollComponentTests(setUpTestEnvironment);
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
        ],
        declarations: [
          ViewerProtologComponent,
          LogComponent,
          ProtologScrollDirective,
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
      }).compileComponents();
      const fixture = TestBed.createComponent(ViewerProtologComponent);
      const protologComponent = fixture.componentInstance;
      const htmlElement = fixture.nativeElement;
      protologComponent.inputData = makeUiData();
      fixture.detectChanges();
      const viewport = assertDefined(
        protologComponent.logComponent?.scrollComponent,
      );
      return [fixture, htmlElement, viewport];
    }
  });

  function makeUiData(): UiData {
    const allLogLevels = ['INFO', 'ERROR'];
    const allTags = ['WindowManager', 'INVALID'];
    const allSourceFiles = [
      'test_source_file.java',
      'other_test_source_file.java',
    ];
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
    for (let i = 0; i < 200; i++) {
      const message = new ProtologEntry(trace.getEntry(0), [
        {
          type: LogFieldType.LOG_LEVEL,
          value: i % 2 === 0 ? allLogLevels[0] : allLogLevels[1],
        },
        {type: LogFieldType.TAG, value: i % 2 === 0 ? allTags[0] : allTags[1]},
        {
          type: LogFieldType.SOURCE_FILE,
          value: i % 2 === 0 ? allSourceFiles[0] : allSourceFiles[1],
        },
        {
          type: LogFieldType.TEXT,
          value: i % 2 === 0 ? shortMessage : longMessage,
        },
      ]);
      messages.push(message);
    }
    return new UiData(
      [
        {type: LogFieldType.LOG_LEVEL, options: allLogLevels},
        {type: LogFieldType.TAG, options: allTags},
        {type: LogFieldType.SOURCE_FILE, options: allSourceFiles},
        {type: LogFieldType.TEXT},
      ],
      messages,
      150,
      undefined,
      undefined,
    );
  }
});
