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

import {Component, EventEmitter, Input, Output} from '@angular/core';
import {proxySetupStyles} from 'app/styles/proxy_setup.styles';
import {ConnectionState} from 'trace_collection/connection_state';

@Component({
  selector: 'wdp-setup',
  template: `
    <ng-container [ngSwitch]="state">
      <ng-container *ngSwitchCase="${ConnectionState.CONNECTING}">
        <p class="connecting-message mat-body-1">
          Connecting...
        </p>
      </ng-container>
      <ng-container *ngSwitchCase="${ConnectionState.NOT_FOUND}">
        <div class="further-adb-info-text">
          <p class="mat-body-1">
            Failed to connect. Web Device Proxy doesn't seem to be running.
          </p>
          <p class="mat-body-1">
            Please check you have Web Device Proxy installed.
          </p>
        </div>

        <div class="further-adb-info-actions">
          <button
            color="primary"
            mat-stroked-button
            class="retry"
            (click)="onRetryButtonClick()">Retry</button>
        </div>
      </ng-container>

      <ng-container *ngSwitchCase="${ConnectionState.UNAUTH}">
        <div class="further-adb-info-text">
          <p class="icon-information mat-body-1">
            <mat-icon class="adb-icon">lock</mat-icon>
            <span class="adb-info">Web Device Proxy not yet authorized. Enable popups and try again.</span>
          </p>
        </div>

        <div class="further-adb-info-actions">
          <button
            color="primary"
            mat-stroked-button
            class="retry"
            (click)="onRetryButtonClick()">Retry</button>
        </div>
      </ng-container>

      <ng-container *ngSwitchDefault></ng-container>
    </ng-container>
  `,
  styles: [proxySetupStyles],
})
export class WdpSetupComponent {
  @Input() state: ConnectionState | undefined;
  @Output() readonly retryConnection = new EventEmitter();

  onRetryButtonClick() {
    this.retryConnection.emit();
  }
}
