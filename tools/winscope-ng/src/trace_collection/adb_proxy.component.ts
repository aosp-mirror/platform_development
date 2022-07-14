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
import { ProxyState } from "./proxy_client";

@Component({
  selector: "adb-proxy",
  template: `
    <div *ngIf="status===states.NO_PROXY">
        <div class="title">Unable to connect to Winscope ADB proxy</div>
        <div class="md-body-2" layout="layout-md">
          <p>Launch the Winscope ADB Connect proxy to capture traces directly from your browser.</p>
          <p>Python 3.5+ and ADB are required.</p>
          <p>Run:</p>
          <pre>python3</pre>
          <pre>$ANDROID_BUILD_TOP/development/tools/winscope/adb_proxy/winscope_proxy.py</pre>
          <p>Or get it from the AOSP repository.</p>
        </div>
        <div class="md-layout">
          <button mat-raised-button class="md-accent">
            <a href="{{downloadProxyUrl}}" target='_blank'>Download from AOSP</a>
          </button>
          <button mat-raised-button class="md-accent" (click)="triggerUnauthComponent()">Retry</button>
        </div>
    </div>

    <div *ngIf="status===states.INVALID_VERSION">
        <div class="title">Your local version of the ADB Connect proxy is incompatible with Winscope.</div>
        <div class="md-body-2" layout="layout-md">
          <p>Please update the proxy to version {{ proxyVersion }}.</p>
          <p>Run:</p>
          <pre>python3</pre>
          <pre>$ANDROID_BUILD_TOP/development/tools/winscope/adb_proxy/winscope_proxy.py</pre>
          <p>Or get it from the AOSP repository.</p>
        </div>
        <div class="md-layout">
          <button mat-raised-button class="md-accent">
            <a href="{{downloadProxyUrl}}" target='_blank'>Download from AOSP</a>
          </button>
          <button mat-raised-button class="md-accent" (click)="restart()">Retry</button>
        </div>
    </div>

    <div *ngIf="status===states.UNAUTH">
        <div class="title">Proxy authorisation required</div>
        <div class="md-body-2" layout="layout-md">
          <p>Enter Winscope proxy token:</p>
          <mat-form-field class="proxy-key-field">
            <input matInput [(ngModel)]="proxyKey" name="proxy-key" (keyup.enter)="onEnter()"/>
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
  readonly proxyVersion = "0.8";
  states = ProxyState;

  @Input()
    status = this.states.NO_PROXY;

  @Output()
    statusChange = new EventEmitter<ProxyState>();

  proxyKey = "";
  readonly downloadProxyUrl: string = "https://android.googlesource.com/platform/development/+/master/tools/winscope/adb_proxy/winscope_proxy.py";

  public onEnter() {
    console.log("this is the key,", this.proxyKey);
  }

  public restart() {
    this.status = ProxyState.START_TRACE;
    this.statusChange.emit(this.status);
  }

  public triggerUnauthComponent() {
    this.status = ProxyState.UNAUTH;
    this.statusChange.emit(this.status);
  }
}
