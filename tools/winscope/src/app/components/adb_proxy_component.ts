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
import {Component, EventEmitter, Input, Output} from '@angular/core';
import {proxyClient, ProxyClient, ProxyState} from 'trace_collection/proxy_client';

@Component({
  selector: 'adb-proxy',
  template: `
    <ng-container [ngSwitch]="proxy.state">
      <ng-container *ngSwitchCase="states.NO_PROXY">
        <div class="further-adb-info-text">
          <p class="mat-body-1">
            Launch the Winscope ADB Connect proxy to capture traces directly from your browser.
          </p>
          <p class="mat-body-1">Python 3.5+ and ADB are required.</p>
          <p class="mat-body-1">
            Run:
            <code>
              python3 $ANDROID_BUILD_TOP/development/tools/winscope/src/adb/winscope_proxy.py
            </code>
          </p>
          <p class="mat-body-1">Or get it from the AOSP repository.</p>
        </div>

        <div class="further-adb-info-actions">
          <button color="primary" mat-stroked-button (click)="downloadFromAosp()">
            Download from AOSP
          </button>
          <button color="primary" mat-stroked-button class="retry" (click)="restart()">
            Retry
          </button>
        </div>
      </ng-container>

      <ng-container *ngSwitchCase="states.INVALID_VERSION">
        <div class="further-adb-info-text">
          <p class="icon-information mat-body-1">
            <mat-icon class="adb-icon">update</mat-icon>
            <span class="adb-info">Your local proxy version is incompatible with Winscope.</span>
          </p>
          <p class="mat-body-1">Please update the proxy to version {{ proxyVersion }}.</p>
          <p class="mat-body-1">
            Run:
            <code>
              python3 $ANDROID_BUILD_TOP/development/tools/winscope/src/adb/winscope_proxy.py
            </code>
          </p>
          <p class="mat-body-1">Or get it from the AOSP repository.</p>
        </div>

        <div class="further-adb-info-actions">
          <button color="primary" mat-stroked-button (click)="downloadFromAosp()">
            Download from AOSP
          </button>
          <button color="primary" mat-stroked-button class="retry" (click)="restart()">
            Retry
          </button>
        </div>
      </ng-container>

      <ng-container *ngSwitchCase="states.UNAUTH">
        <div class="further-adb-info-text">
          <p class="icon-information mat-body-1">
            <mat-icon class="adb-icon">lock</mat-icon>
            <span class="adb-info">Proxy authorisation required.</span>
          </p>
          <p class="mat-body-1">Enter Winscope proxy token:</p>
          <mat-form-field>
            <input matInput [(ngModel)]="proxyKeyItem" name="proxy-key" />
          </mat-form-field>
          <p class="mat-body-1">
            The proxy token is printed to console on proxy launch, copy and paste it above.
          </p>
        </div>

        <div class="further-adb-info-actions">
          <button color="primary" mat-stroked-button class="retry" (click)="restart()">
            Connect
          </button>
        </div>
      </ng-container>

      <ng-container *ngSwitchDefault></ng-container>
    </ng-container>
  `,
  styles: [
    `
      .icon-information {
        display: flex;
        flex-direction: row;
        align-items: center;
      }
      .further-adb-info-text {
        display: flex;
        flex-direction: column;
        overflow-wrap: break-word;
        gap: 10px;
        margin-bottom: 10px;
      }
      .further-adb-info-actions {
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
        gap: 10px;
      }
      .adb-info {
        margin-left: 5px;
      }
    `,
  ],
})
export class AdbProxyComponent {
  @Input()
  proxy: ProxyClient = proxyClient;

  @Output()
  proxyChange = new EventEmitter<ProxyClient>();

  @Output()
  addKey = new EventEmitter<string>();

  states = ProxyState;
  proxyKeyItem = '';
  readonly proxyVersion = this.proxy.VERSION;
  readonly downloadProxyUrl: string =
    'https://android.googlesource.com/platform/development/+/master/tools/winscope/adb_proxy/winscope_proxy.py';

  restart() {
    this.addKey.emit(this.proxyKeyItem);
    this.proxy.setState(this.states.CONNECTING);
    this.proxyChange.emit(this.proxy);
  }

  downloadFromAosp() {
    window.open(this.downloadProxyUrl, '_blank')?.focus();
  }
}
