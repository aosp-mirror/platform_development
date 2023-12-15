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
import {ComponentFixture, ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {ViewerProtologComponent} from './viewer_protolog_component';

import {CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {LogMessage} from 'trace/protolog';
import {executeScrollComponentTests} from 'viewers/common/scroll_component_test_utils';
import {ProtologScrollDirective} from './scroll_strategy/protolog_scroll_directive';
import {UiData, UiDataMessage} from './ui_data';

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
        declarations: [ViewerProtologComponent, ProtologScrollDirective],
        schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA],
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
  });

  describe('Scroll component', () => {
    executeScrollComponentTests('message', setUpTestEnvironment);

    function makeUiDataForScroll(): UiData {
      const messages = [];
      const shortMessage = 'test information about message';
      const longMessage = shortMessage.repeat(10);
      for (let i = 0; i < 200; i++) {
        const uiDataMessage = new LogMessage(
          i % 2 === 0 ? shortMessage : longMessage,
          '2022-11-21T18:05:09.777144978',
          'WindowManager',
          'INFO',
          'test_source_file.java',
          BigInt(123)
        );
        (uiDataMessage as UiDataMessage).originalIndex = i;
        messages.push(uiDataMessage as UiDataMessage);
      }
      return new UiData([], [], [], messages, undefined);
    }

    async function setUpTestEnvironment(): Promise<
      [ComponentFixture<any>, HTMLElement, CdkVirtualScrollViewport]
    > {
      await TestBed.configureTestingModule({
        providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
        imports: [ScrollingModule],
        declarations: [ViewerProtologComponent, ProtologScrollDirective],
        schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA],
      }).compileComponents();
      const fixture = TestBed.createComponent(ViewerProtologComponent);
      const protologComponent = fixture.componentInstance;
      const htmlElement = fixture.nativeElement;
      const viewport = protologComponent.scrollComponent;
      protologComponent.uiData = makeUiDataForScroll();
      return [fixture, htmlElement, viewport];
    }
  });
});
