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
import {TIMESTAMP_NODE_FORMATTER} from 'trace/tree_node/formatters';
import {executeScrollComponentTests} from 'viewers/common/scroll_component_test_utils';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {SelectWithFilterComponent} from 'viewers/components/select_with_filter_component';
import {ProtologScrollDirective} from './scroll_strategy/protolog_scroll_directive';
import {UiData, UiDataMessage} from './ui_data';
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
          ProtologScrollDirective,
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
      }).compileComponents();
      fixture = TestBed.createComponent(ViewerProtologComponent);
      component = fixture.componentInstance;
      htmlElement = fixture.nativeElement;
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
      expect(htmlElement.querySelector('.scroll-messages')).toBeTruthy();
    });

    it('applies log level filter correctly', async () => {
      const allMessages = makeUiData().messages;
      htmlElement.addEventListener(
        ViewerEvents.LogLevelsFilterChanged,
        (event) => {
          if ((event as CustomEvent).detail.length === 0) {
            component.uiData.messages = allMessages;
            return;
          }
          component.uiData.messages = allMessages.filter((message) =>
            (event as CustomEvent).detail.includes(message.level),
          );
        },
      );
      await checkSelectFilter('.log-level');
    });

    it('applies tag filter correctly', async () => {
      const allMessages = makeUiData().messages;
      htmlElement.addEventListener(ViewerEvents.TagsFilterChanged, (event) => {
        if ((event as CustomEvent).detail.length === 0) {
          component.uiData.messages = allMessages;
          return;
        }
        component.uiData.messages = allMessages.filter((message) =>
          (event as CustomEvent).detail.includes(message.tag),
        );
      });
      await checkSelectFilter('.tag');
    });

    it('applies source file filter correctly', async () => {
      const allMessages = makeUiData().messages;
      htmlElement.addEventListener(
        ViewerEvents.SourceFilesFilterChanged,
        (event) => {
          if ((event as CustomEvent).detail.length === 0) {
            component.uiData.messages = allMessages;
            return;
          }
          component.uiData.messages = allMessages.filter((message) =>
            (event as CustomEvent).detail.includes(message.at),
          );
        },
      );
      await checkSelectFilter('.source-file');
    });

    it('applies text filter correctly', () => {
      const allMessages = makeUiData().messages;
      htmlElement.addEventListener(
        ViewerEvents.SearchStringFilterChanged,
        (event) => {
          component.uiData.messages = allMessages.filter((message) =>
            message.text.includes((event as CustomEvent).detail),
          );
        },
      );
      component.inputData = makeUiData();
      fixture.detectChanges();
      expect(component.uiData.messages.length).toEqual(200);

      const textFilterDiv = assertDefined(
        htmlElement.querySelector('.filters .text'),
      );
      const inputEl = assertDefined(
        textFilterDiv.querySelector('input'),
      ) as HTMLInputElement;
      inputEl.value = 'keep';
      inputEl.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      expect(component.uiData.messages.length).toEqual(100);
      inputEl.value = '';
      inputEl.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      expect(component.uiData.messages.length).toEqual(200);
    });

    it('scrolls to current entry on button click', () => {
      component.inputData = makeUiData();
      fixture.detectChanges();
      const goToCurrentTimeButton = assertDefined(
        htmlElement.querySelector('.go-to-current-time'),
      ) as HTMLButtonElement;
      const spy = spyOn(
        assertDefined(component.scrollComponent),
        'scrollToIndex',
      );
      goToCurrentTimeButton.click();
      expect(spy).toHaveBeenCalledWith(150);
    });

    it('changes css class on message click and does not scroll', () => {
      const uiData = makeUiData();
      component.inputData = uiData;
      fixture.detectChanges();

      htmlElement.addEventListener(ViewerEvents.LogClicked, (event) => {
        const index = (event as CustomEvent).detail;
        uiData.selectedMessageIndex = index;
        component.inputData = uiData;
        fixture.detectChanges();
      });

      const message = assertDefined(
        htmlElement.querySelector('.message[item-id="3"]'),
      ) as HTMLButtonElement;
      expect(message.className).not.toContain('selected');
      const spy = spyOn(
        assertDefined(component.scrollComponent),
        'scrollToIndex',
      );
      message.click();
      expect(spy).not.toHaveBeenCalled();
      expect(message.className).toContain('selected');
    });

    it('propagates timestamp on click', () => {
      component.inputData = makeUiData();
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

    async function checkSelectFilter(filterSelector: string) {
      component.inputData = makeUiData();
      fixture.detectChanges();
      expect(component.uiData.messages.length).toEqual(200);

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
      expect(component.uiData.messages.length).toEqual(100);

      firstOption.click();
      fixture.detectChanges();
      expect(component.uiData.messages.length).toEqual(200);
    }
  });

  describe('Scroll component', () => {
    executeScrollComponentTests('message', setUpTestEnvironment);
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
        declarations: [ViewerProtologComponent, ProtologScrollDirective],
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
      }).compileComponents();
      const fixture = TestBed.createComponent(ViewerProtologComponent);
      const protologComponent = fixture.componentInstance;
      const htmlElement = fixture.nativeElement;
      const viewport = assertDefined(protologComponent.scrollComponent);
      protologComponent.inputData = makeUiData();
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

    const time = new PropertyTreeBuilder()
      .setRootId('ProtologMessage')
      .setName('timestamp')
      .setValue(TimestampConverterUtils.makeElapsedTimestamp(10n))
      .setFormatter(TIMESTAMP_NODE_FORMATTER)
      .build();

    const messages = [];
    const shortMessage = 'test information about message';
    const longMessage = shortMessage.repeat(10) + 'keep';
    for (let i = 0; i < 200; i++) {
      const uiDataMessage: UiDataMessage = {
        traceIndex: i,
        text: i % 2 === 0 ? shortMessage : longMessage,
        time,
        tag: i % 2 === 0 ? allTags[0] : allTags[1],
        level: i % 2 === 0 ? allLogLevels[0] : allLogLevels[1],
        at: i % 2 === 0 ? allSourceFiles[0] : allSourceFiles[1],
      };
      messages.push(uiDataMessage);
    }
    return new UiData(
      allLogLevels,
      allTags,
      allSourceFiles,
      messages,
      150,
      undefined,
    );
  }
});
