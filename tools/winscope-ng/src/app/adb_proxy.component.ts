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
import { Component, Input, Output, EventEmitter } from "@angular/core";
import { ProxyClient, ProxyState } from "../trace_collection/proxy_client";

@Component({
  selector: "adb-proxy",
  template: `
    <div *ngIf="proxy.state===states.NO_PROXY">
        <div>
            <mat-icon class="icon-message">error</mat-icon>
            <span class="icon-message">Unable to connect to Winscope ADB proxy</span>
        </div>
        <div class="md-body-2" layout="layout-md">
          <p>Launch the Winscope ADB Connect proxy to capture traces directly from your browser.</p>
          <p>Python 3.5+ and ADB are required.</p>
          <p>Run:</p>
          <pre>python3</pre>
          <pre>$ANDROID_BUILD_TOP/development/tools/winscope-ng/src/adb/winscope_proxy.py</pre>
          <p>Or get it from the AOSP repository.</p>
        </div>
        <div class="md-layout">
          <button mat-raised-button class="md-accent">
            <a href="{{downloadProxyUrl}}" target='_blank'>Download from AOSP</a>
          </button>
          <button mat-raised-button class="md-accent" (click)="restart()">Retry</button>
        </div>
    </div>

    <div *ngIf="proxy.state===states.INVALID_VERSION">
        <div>
            <mat-icon class="icon-message">update</mat-icon>
            <span class="icon-message">Your local proxy version is incompatible with Winscope.</span>
        </div>
        <div class="md-body-2" layout="layout-md">
          <p>Please update the proxy to version {{ proxyVersion }}.</p>
          <p>Run:</p>
          <pre>python3</pre>
          <pre>$ANDROID_BUILD_TOP/development/tools/winscope-ng/src/adb/winscope_proxy.py</pre>
          <p>Or get it from the AOSP repository.</p>
        </div>
        <div class="md-layout">
          <button mat-raised-button class="md-accent">
            <a href="{{downloadProxyUrl}}" target='_blank'>Download from AOSP</a>
          </button>
          <button mat-raised-button class="md-accent" (click)="restart()">Retry</button>
        </div>
    </div>

    <div *ngIf="proxy.state===states.UNAUTH">
        <div>
            <mat-icon class="icon-message">lock</mat-icon>
            <span class="icon-message">Proxy authorisation required</span>
        </div>
        <div class="md-body-2" layout="layout-md">
          <p>Enter Winscope proxy token:</p>
          <mat-form-field class="proxy-key-field">
            <input matInput [(ngModel)]="proxyKeyItem" name="proxy-key"/>
          </mat-form-field>
          <p>The proxy token is printed to console on proxy launch, copy and paste it above.</p>
        </div>
        <div class="md-layout">
          <button mat-raised-button class="md-accent" (click)="restart()">Connect</button>
        </div>
    </div>

  `,
  styles: [".proxy-key-field {width: 30rem}"]
})
export class AdbProxyComponent {
  @Input()
  proxy: any = {};

  @Output()
  proxyChange = new EventEmitter<ProxyClient>();

  @Output() addKey = new EventEmitter<string>();

  states = ProxyState;
  proxyKeyItem = "";
  readonly proxyVersion = this.proxy.VERSION;
  readonly downloadProxyUrl: string = "https://android.googlesource.com/platform/development/+/master/tools/winscope-ng/adb/winscope_proxy.py";

  public restart() {
    this.addKey.emit(this.proxyKeyItem);
    this.proxy.setState(this.states.CONNECTING);
    this.proxyChange.emit(this.proxy);
  }
}
