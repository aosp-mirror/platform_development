/*
 * Copyright (C) 2025 The Android Open Source Project
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
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {ConnectionState} from 'trace_collection/connection_state';
import {WdpSetupComponent} from './wdp_setup_component';

describe('WdpSetupComponent', () => {
  let fixture: ComponentFixture<WdpSetupComponent>;
  let component: WdpSetupComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        MatIconModule,
        BrowserAnimationsModule,
        MatButtonModule,
      ],
      declarations: [WdpSetupComponent],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
    fixture = TestBed.createComponent(WdpSetupComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    component.state = ConnectionState.CONNECTING;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('correct connecting message', () => {
    fixture.detectChanges();
    expect(
      htmlElement.querySelector('.connecting-message')?.textContent,
    ).toContain('Connecting...');
    expect(htmlElement.querySelector('.retry')).toBeNull();
    expect(htmlElement.querySelector('.install')).toBeNull();
  });

  it('correct icon and message displays if no proxy', () => {
    component.state = ConnectionState.NOT_FOUND;
    fixture.detectChanges();
    const text = htmlElement.querySelector(
      '.further-adb-info-text',
    )?.textContent;
    expect(text).toContain(
      "Failed to connect. Web Device Proxy doesn't seem to be running.",
    );
    expect(text).toContain('Please check you have Web Device Proxy installed.');
    checkRetryButton();

    const windowSpy = spyOn(window, 'open');
    assertDefined(
      htmlElement.querySelector<HTMLButtonElement>('.install'),
    ).click();
    fixture.detectChanges();
    expect(windowSpy).toHaveBeenCalledOnceWith(
      'https://tools.google.com/dlpage/android_web_device_proxy',
      '_blank',
    );
  });

  it('correct icon and message displays if unauthorized proxy', () => {
    component.state = ConnectionState.UNAUTH;
    fixture.detectChanges();
    expect(htmlElement.querySelector('.adb-info')?.textContent).toEqual(
      'Web Device Proxy not yet authorized. Enable popups and try again.',
    );
    expect(htmlElement.querySelector('.adb-icon')?.textContent).toEqual('lock');
    checkRetryButton();
    expect(htmlElement.querySelector('.install')).toBeNull();
  });

  function checkRetryButton() {
    const spy = spyOn(assertDefined(component.retryConnection), 'emit');
    assertDefined(
      htmlElement.querySelector<HTMLButtonElement>('.retry'),
    ).click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  }
});
