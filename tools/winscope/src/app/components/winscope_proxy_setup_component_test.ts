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
import {CommonModule} from '@angular/common';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {Download} from 'common/download';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {ConnectionState} from 'trace_collection/connection_state';
import {WinscopeProxySetupComponent} from './winscope_proxy_setup_component';

describe('WinscopeProxySetupComponent', () => {
  let component: WinscopeProxySetupComponent;
  let dom: DOMTestHelper<WinscopeProxySetupComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        MatIconModule,
        MatFormFieldModule,
        MatInputModule,
        BrowserAnimationsModule,
        MatButtonModule,
        FormsModule,
      ],
      declarations: [WinscopeProxySetupComponent],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
    const fixture = TestBed.createComponent(WinscopeProxySetupComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    component.state = ConnectionState.CONNECTING;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('correct connecting message', () => {
    dom.detectChanges();
    dom.get('.connecting-message').checkText('Connecting...');
  });

  it('correct icon and message displays if no proxy', () => {
    component.state = ConnectionState.NOT_FOUND;
    dom.detectChanges();
    dom
      .get('.further-adb-info-text')
      .checkText('Launch the Winscope ADB Connect proxy');
  });

  it('correct icon and message displays if invalid proxy', () => {
    component.state = ConnectionState.INVALID_VERSION;
    dom.detectChanges();
    dom
      .get('.further-adb-info-text')
      .checkText(
        `Your local proxy version is incompatible with Winscope. Please update the proxy to version ${component.proxyVersion}.`,
      );
    dom.get('.adb-icon').checkTextExact('update');
  });

  it('correct icon and message displays if unauthorized proxy', () => {
    component.state = ConnectionState.UNAUTH;
    dom.detectChanges();
    dom.get('.adb-info').checkText('Proxy authorization required.');
    dom.get('.adb-icon').checkTextExact('lock');
  });

  it('download proxy button downloads proxy', () => {
    component.state = ConnectionState.NOT_FOUND;
    dom.detectChanges();
    const spy = spyOn(Download, 'fromUrl');
    dom.findAndClick('.download-proxy-btn');
    expect(spy).toHaveBeenCalledWith(
      component.downloadProxyUrl,
      'winscope_proxy.py',
    );
  });

  it('retry button emits event', () => {
    component.state = ConnectionState.NOT_FOUND;
    dom.detectChanges();

    const spy = spyOn(assertDefined(component.retryConnection), 'emit');
    dom.findAndClick('.retry');
    expect(spy).toHaveBeenCalledWith('');
  });

  it('input proxy token saved as expected', () => {
    const spy = spyOn(assertDefined(component.retryConnection), 'emit');
    component.state = ConnectionState.UNAUTH;
    dom.detectChanges();

    dom.findAndClick('.retry');
    expect(spy).not.toHaveBeenCalled();

    dom.findAndDispatchInput('.proxy-token-input-field', '12345');
    expect(spy).not.toHaveBeenCalled();

    dom.findAndClick('.retry');
    expect(spy).toHaveBeenCalledWith('12345');
  });

  it('emits event on enter key', () => {
    const spy = spyOn(assertDefined(component.retryConnection), 'emit');
    component.state = ConnectionState.UNAUTH;
    dom.detectChanges();

    dom.findAndDispatchInput('.proxy-token-input-field', '12345');
    expect(spy).not.toHaveBeenCalled();

    dom.get('.proxy-token-input-field').keydownEnter();
    expect(spy).toHaveBeenCalledWith('12345');
  });
});
