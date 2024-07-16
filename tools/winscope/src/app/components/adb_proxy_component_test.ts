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
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {FunctionUtils} from 'common/function_utils';
import {ConnectionState} from 'trace_collection/connection_state';
import {ProxyConnection} from 'trace_collection/proxy_connection';
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
      ],
      declarations: [AdbProxyComponent],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
    fixture = TestBed.createComponent(AdbProxyComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    component.connection = new ProxyConnection(
      FunctionUtils.DO_NOTHING_ASYNC,
      FunctionUtils.DO_NOTHING,
      FunctionUtils.DO_NOTHING,
    );
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('correct icon and message displays if no proxy', () => {
    spyOn(assertDefined(component.connection), 'getState').and.returnValue(
      ConnectionState.NOT_FOUND,
    );
    fixture.detectChanges();
    expect(
      htmlElement.querySelector('.further-adb-info-text')?.innerHTML,
    ).toContain('Launch the Winscope ADB Connect proxy');
  });

  it('correct icon and message displays if invalid proxy', () => {
    spyOn(assertDefined(component.connection), 'getState').and.returnValue(
      ConnectionState.INVALID_VERSION,
    );
    fixture.detectChanges();
    expect(htmlElement.querySelector('.adb-info')?.innerHTML).toBe(
      'Your local proxy version is incompatible with Winscope.',
    );
    expect(htmlElement.querySelector('.adb-icon')?.innerHTML).toBe('update');
  });

  it('correct icon and message displays if unauthorized proxy', () => {
    spyOn(assertDefined(component.connection), 'getState').and.returnValue(
      ConnectionState.UNAUTH,
    );
    fixture.detectChanges();
    expect(htmlElement.querySelector('.adb-info')?.innerHTML).toBe(
      'Proxy authorization required.',
    );
    expect(htmlElement.querySelector('.adb-icon')?.innerHTML).toBe('lock');
  });

  it('download proxy button downloads proxy', () => {
    spyOn(assertDefined(component.connection), 'getState').and.returnValue(
      ConnectionState.NOT_FOUND,
    );
    fixture.detectChanges();
    const spy = spyOn(window, 'open');
    const button: HTMLButtonElement | null = htmlElement.querySelector(
      '.download-proxy-btn',
    );
    expect(button).toBeInstanceOf(HTMLButtonElement);
    button?.click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledWith(component.downloadProxyUrl, '_blank');
  });

  it('retry button tries to reconnect proxy', () => {
    spyOn(assertDefined(component.connection), 'getState').and.returnValue(
      ConnectionState.NOT_FOUND,
    );
    fixture.detectChanges();

    const spy = spyOn(assertDefined(component.connection), 'restartConnection');
    const button: HTMLButtonElement | null =
      htmlElement.querySelector('.retry');
    expect(button).toBeInstanceOf(HTMLButtonElement);
    button?.click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  it('input proxy token saved as expected', () => {
    const spy = spyOn(component.addKey, 'emit');

    spyOn(assertDefined(component.connection), 'getState').and.returnValue(
      ConnectionState.UNAUTH,
    );
    fixture.detectChanges();
    let button: HTMLButtonElement | null = htmlElement.querySelector('.retry');
    button?.click();
    fixture.detectChanges();
    expect(spy).not.toHaveBeenCalled();

    component.proxyKeyItem = '12345';
    fixture.detectChanges();
    button = htmlElement.querySelector('.retry');
    button?.click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  it('retries proxy connection on enter key', () => {
    spyOn(assertDefined(component.connection), 'getState').and.returnValue(
      ConnectionState.UNAUTH,
    );
    fixture.detectChanges();
    const proxyKeyInputField = assertDefined(
      htmlElement.querySelector('.proxy-key-input-field'),
    ) as HTMLInputElement;
    const proxyKeyInput = assertDefined(
      proxyKeyInputField.querySelector('input'),
    ) as HTMLInputElement;

    const spy = spyOn(assertDefined(component.connection), 'restartConnection');

    proxyKeyInput.value = '12345';
    proxyKeyInputField.dispatchEvent(
      new KeyboardEvent('keydown', {key: 'Enter'}),
    );
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });
});
