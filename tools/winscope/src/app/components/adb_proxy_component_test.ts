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
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {Download} from 'common/download';
import {ConnectionState} from 'trace_collection/connection_state';
import {AdbProxyComponent} from './adb_proxy_component';

describe('AdbProxyComponent', () => {
  let fixture: ComponentFixture<AdbProxyComponent>;
  let component: AdbProxyComponent;
  let htmlElement: HTMLElement;

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
      declarations: [AdbProxyComponent],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
    fixture = TestBed.createComponent(AdbProxyComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    component.state = ConnectionState.CONNECTING;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('correct icon and message displays if no proxy', () => {
    component.state = ConnectionState.NOT_FOUND;
    fixture.detectChanges();
    expect(
      htmlElement.querySelector('.further-adb-info-text')?.textContent,
    ).toContain('Launch the Winscope ADB Connect proxy');
  });

  it('correct icon and message displays if invalid proxy', () => {
    component.state = ConnectionState.INVALID_VERSION;
    fixture.detectChanges();
    expect(
      htmlElement.querySelector('.further-adb-info-text')?.textContent,
    ).toContain(
      `Your local proxy version is incompatible with Winscope. Please update the proxy to version ${component.proxyVersion}.`,
    );
    expect(htmlElement.querySelector('.adb-icon')?.textContent).toEqual(
      'update',
    );
  });

  it('correct icon and message displays if unauthorized proxy', () => {
    component.state = ConnectionState.UNAUTH;
    fixture.detectChanges();
    expect(htmlElement.querySelector('.adb-info')?.textContent).toEqual(
      'Proxy authorization required.',
    );
    expect(htmlElement.querySelector('.adb-icon')?.textContent).toEqual('lock');
  });

  it('download proxy button downloads proxy', () => {
    component.state = ConnectionState.NOT_FOUND;
    fixture.detectChanges();
    const spy = spyOn(Download, 'fromUrl');
    const button = assertDefined(
      htmlElement.querySelector<HTMLButtonElement>('.download-proxy-btn'),
    );
    button.click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledWith(
      component.downloadProxyUrl,
      'winscope_proxy.py',
    );
  });

  it('retry button emits event', () => {
    component.state = ConnectionState.NOT_FOUND;
    fixture.detectChanges();

    const spy = spyOn(assertDefined(component.retryConnection), 'emit');
    const button = assertDefined(
      htmlElement.querySelector<HTMLButtonElement>('.retry'),
    );
    button.click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledWith('');
  });

  it('input proxy token saved as expected', () => {
    const spy = spyOn(assertDefined(component.retryConnection), 'emit');
    component.state = ConnectionState.UNAUTH;
    fixture.detectChanges();

    const button = assertDefined(
      htmlElement.querySelector<HTMLButtonElement>('.retry'),
    );
    button.click();
    fixture.detectChanges();
    expect(spy).not.toHaveBeenCalled();

    const proxyTokenInput = assertDefined(
      htmlElement.querySelector<HTMLInputElement>(
        '.proxy-token-input-field input',
      ),
    );
    proxyTokenInput.value = '12345';
    proxyTokenInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    assertDefined(
      htmlElement.querySelector<HTMLButtonElement>('.retry'),
    ).click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledWith('12345');
  });

  it('emits event on enter key', () => {
    const spy = spyOn(assertDefined(component.retryConnection), 'emit');
    component.state = ConnectionState.UNAUTH;
    fixture.detectChanges();

    const proxyTokenInputField = assertDefined(
      htmlElement.querySelector('.proxy-token-input-field'),
    );
    const proxyTokenInput = assertDefined(
      proxyTokenInputField.querySelector<HTMLInputElement>('input'),
    );

    proxyTokenInput.value = '12345';
    proxyTokenInput.dispatchEvent(new Event('input'));
    proxyTokenInputField.dispatchEvent(
      new KeyboardEvent('keydown', {key: 'Enter'}),
    );
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledWith('12345');
  });
});
