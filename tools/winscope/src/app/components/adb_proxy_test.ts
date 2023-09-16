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
import {proxyClient, ProxyState} from 'trace_collection/proxy_client';
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
    component.proxy = proxyClient;
    htmlElement = fixture.nativeElement;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('check correct icon and message displays if no proxy', () => {
    component.proxy.setState(ProxyState.NO_PROXY);
    fixture.detectChanges();
    expect(htmlElement.querySelector('.further-adb-info-text')?.innerHTML).toContain(
      'Launch the Winscope ADB Connect proxy'
    );
  });

  it('check correct icon and message displays if invalid proxy', () => {
    component.proxy.setState(ProxyState.INVALID_VERSION);
    fixture.detectChanges();
    expect(htmlElement.querySelector('.adb-info')?.innerHTML).toBe(
      'Your local proxy version is incompatible with Winscope.'
    );
    expect(htmlElement.querySelector('.adb-icon')?.innerHTML).toBe('update');
  });

  it('check correct icon and message displays if unauthorised proxy', () => {
    component.proxy.setState(ProxyState.UNAUTH);
    fixture.detectChanges();
    expect(htmlElement.querySelector('.adb-info')?.innerHTML).toBe('Proxy authorisation required.');
    expect(htmlElement.querySelector('.adb-icon')?.innerHTML).toBe('lock');
  });

  it('check retry button acts as expected', async () => {
    component.proxy.setState(ProxyState.NO_PROXY);
    fixture.detectChanges();
    spyOn(component, 'restart').and.callThrough();
    const button: HTMLButtonElement | null = htmlElement.querySelector('.retry');
    expect(button).toBeInstanceOf(HTMLButtonElement);
    button?.dispatchEvent(new Event('click'));
    await fixture.whenStable();
    expect(component.restart).toHaveBeenCalled();
  });
});
