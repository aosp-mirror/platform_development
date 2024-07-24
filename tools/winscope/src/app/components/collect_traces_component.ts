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

import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Inject,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewEncapsulation,
} from '@angular/core';
import {assertDefined} from 'common/assert_utils';
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {ProgressListener} from 'messaging/progress_listener';
import {Connection} from 'trace_collection/connection';
import {ProxyState} from 'trace_collection/proxy_client';
import {ProxyConnection} from 'trace_collection/proxy_connection';
import {
  ConfigMap,
  EnableConfiguration,
  SelectionConfiguration,
  TraceConfigurationMap,
} from 'trace_collection/trace_collection_utils';
import {LoadProgressComponent} from './load_progress_component';

@Component({
  selector: 'collect-traces',
  template: `
    <mat-card class="collect-card">
      <mat-card-title class="title">Collect Traces</mat-card-title>

      <mat-card-content class="collect-card-content">
        <p *ngIf="connect.isConnectingState()" class="connecting-message mat-body-1">
          Connecting...
        </p>

        <div *ngIf="!connect.adbSuccess()" class="set-up-adb">
          <button
            class="proxy-tab"
            color="primary"
            mat-stroked-button
            [ngClass]="tabClass(true)"
            (click)="displayAdbProxyTab()">
            ADB Proxy
          </button>
          <!-- <button class="web-tab" color="primary" mat-raised-button [ngClass]="tabClass(false)" (click)="displayWebAdbTab()">Web ADB</button> -->
          <adb-proxy
            *ngIf="isAdbProxy"
            [(proxy)]="connect.proxy!"
            (addKey)="onAddKey($event)"></adb-proxy>
          <!-- <web-adb *ngIf="!isAdbProxy"></web-adb> TODO: fix web adb workflow -->
        </div>

        <div *ngIf="connect.isDevicesState()" class="devices-connecting">
          <div *ngIf="objectKeys(connect.devices()).length === 0" class="no-device-detected">
            <p class="mat-body-3 icon"><mat-icon inline fontIcon="phonelink_erase"></mat-icon></p>
            <p class="mat-body-1">No devices detected</p>
          </div>
          <div *ngIf="objectKeys(connect.devices()).length > 0" class="device-selection">
            <p class="mat-body-1 instruction">Select a device:</p>
            <mat-list *ngIf="objectKeys(connect.devices()).length > 0">
              <mat-list-item
                *ngFor="let deviceId of objectKeys(connect.devices())"
                (click)="onDeviceClick(deviceId)"
                class="available-device">
                <mat-icon matListIcon>
                  {{
                    connect.devices()[deviceId].authorised ? 'smartphone' : 'screen_lock_portrait'
                  }}
                </mat-icon>
                <p matLine>
                  {{
                    connect.devices()[deviceId].authorised
                      ? connect.devices()[deviceId]?.model
                      : 'unauthorised'
                  }}
                  ({{ deviceId }})
                </p>
              </mat-list-item>
            </mat-list>
          </div>
        </div>

        <div
          *ngIf="
            connect.isStartTraceState() || connect.isEndTraceState() || isOperationInProgress()
          "
          class="trace-collection-config">
          <mat-list>
            <mat-list-item>
              <mat-icon matListIcon>smartphone</mat-icon>
              <p matLine>
                {{ connect.selectedDevice()?.model }} ({{ connect.selectedDeviceId() }})

                <button
                  color="primary"
                  class="change-btn"
                  mat-button
                  (click)="onChangeDeviceButton()"
                  [disabled]="connect.isEndTraceState() || isOperationInProgress()">
                  Change device
                </button>
              </p>
            </mat-list-item>
          </mat-list>

          <mat-tab-group class="tracing-tabs">
            <mat-tab
              label="Trace"
              [disabled]="connect.isEndTraceState() || isOperationInProgress()">
              <div class="tabbed-section">
                <div class="trace-section" *ngIf="connect.isStartTraceState()">
                  <trace-config [(traceConfig)]="traceConfig"></trace-config>
                  <div class="start-btn">
                    <button color="primary" mat-stroked-button (click)="startTracing()">
                      Start trace
                    </button>
                  </div>
                </div>

                <div *ngIf="connect.isEndTraceState()" class="end-tracing">
                  <div class="progress-desc">
                    <p class="mat-body-3"><mat-icon fontIcon="cable"></mat-icon></p>
                    <mat-progress-bar mode="indeterminate"></mat-progress-bar>
                    <p class="mat-body-1">Tracing...</p>
                  </div>
                  <div class="end-btn">
                    <button color="primary" mat-raised-button (click)="endTrace()">
                      End trace
                    </button>
                  </div>
                </div>

                <div *ngIf="isOperationInProgress()" class="load-data">
                  <load-progress
                    [progressPercentage]="progressPercentage"
                    [message]="progressMessage">
                  </load-progress>
                  <div class="end-btn">
                    <button color="primary" mat-raised-button (click)="endTrace()" disabled="true">
                      End trace
                    </button>
                  </div>
                </div>
              </div>
            </mat-tab>
            <mat-tab label="Dump" [disabled]="connect.isEndTraceState() || isOperationInProgress()">
              <div class="tabbed-section">
                <div class="dump-section" *ngIf="connect.isStartTraceState()">
                  <h3 class="mat-subheading-2">Dump targets</h3>
                  <div class="selection">
                    <mat-checkbox
                      *ngFor="let dumpKey of objectKeys(dumpConfig)"
                      color="primary"
                      class="dump-checkbox"
                      [(ngModel)]="dumpConfig[dumpKey].run"
                      >{{ dumpConfig[dumpKey].name }}</mat-checkbox
                    >
                  </div>
                  <div class="dump-btn">
                    <button color="primary" mat-stroked-button (click)="dumpState()">
                      Dump state
                    </button>
                  </div>
                </div>

                <load-progress
                  *ngIf="isOperationInProgress()"
                  [progressPercentage]="progressPercentage"
                  [message]="progressMessage">
                </load-progress>
              </div>
            </mat-tab>
          </mat-tab-group>
        </div>

        <div *ngIf="connect.isErrorState()" class="unknown-error">
          <p class="error-wrapper mat-body-1">
            <mat-icon class="error-icon">error</mat-icon>
            Error:
          </p>
          <pre> {{ connect.proxy?.errorText }} </pre>
          <button color="primary" class="retry-btn" mat-raised-button (click)="onRetryButton()">
            Retry
          </button>
        </div>
      </mat-card-content>
    </mat-card>
  `,
  styles: [
    `
      .change-btn,
      .retry-btn {
        margin-left: 5px;
      }
      .mat-card.collect-card {
        display: flex;
      }
      .collect-card {
        height: 100%;
        flex-direction: column;
        overflow: auto;
        margin: 10px;
      }
      .collect-card-content {
        overflow: auto;
      }
      .selection {
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
        gap: 10px;
      }
      .set-up-adb,
      .trace-collection-config,
      .trace-section,
      .dump-section,
      .end-tracing,
      .load-data,
      trace-config {
        display: flex;
        flex-direction: column;
        gap: 10px;
      }
      .trace-section,
      .dump-section,
      .end-tracing,
      .load-data {
        height: 100%;
      }
      .trace-collection-config {
        height: 100%;
      }
      .proxy-tab,
      .web-tab,
      .start-btn,
      .dump-btn,
      .end-btn {
        align-self: flex-start;
      }
      .start-btn,
      .dump-btn,
      .end-btn {
        margin: auto 0 0 0;
        padding: 1rem 0 0 0;
      }
      .error-wrapper {
        display: flex;
        flex-direction: row;
        align-items: center;
      }
      .error-icon {
        margin-right: 5px;
      }
      .available-device {
        cursor: pointer;
      }

      .no-device-detected {
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-content: center;
        align-items: center;
        height: 100%;
      }

      .no-device-detected p,
      .device-selection p.instruction {
        padding-top: 1rem;
        opacity: 0.6;
        font-size: 1.2rem;
      }

      .no-device-detected .icon {
        font-size: 3rem;
        margin: 0 0 0.2rem 0;
      }

      .devices-connecting {
        height: 100%;
      }

      mat-card-content {
        flex-grow: 1;
      }

      mat-tab-body {
        padding: 1rem;
      }

      .loading-info {
        opacity: 0.8;
        padding: 1rem 0;
      }

      .tracing-tabs {
        flex-grow: 1;
      }

      .tracing-tabs .mat-tab-body-wrapper {
        flex-grow: 1;
      }

      .tabbed-section {
        height: 100%;
      }

      .load-data p,
      .end-tracing p {
        opacity: 0.7;
      }

      .progress-desc {
        display: flex;
        height: 100%;
        flex-direction: column;
        justify-content: center;
        align-content: center;
        align-items: center;
      }

      .progress-desc > * {
        max-width: 250px;
      }

      load-progress {
        height: 100%;
      }
    `,
  ],
  encapsulation: ViewEncapsulation.None,
})
export class CollectTracesComponent
  implements OnInit, OnDestroy, ProgressListener
{
  objectKeys = Object.keys;
  isAdbProxy = true;
  connect: Connection | undefined;
  isExternalOperationInProgress = false;
  progressMessage = 'Fetching...';
  progressPercentage: number | undefined;
  lastUiProgressUpdateTimeMs?: number;

  @Input() traceConfig: TraceConfigurationMap | undefined;
  @Input() dumpConfig: TraceConfigurationMap | undefined;
  @Input() storage: Storage | undefined;

  @Output() readonly filesCollected = new EventEmitter<File[]>();

  constructor(
    @Inject(ChangeDetectorRef) private changeDetectorRef: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    if (this.isAdbProxy) {
      this.connect = new ProxyConnection(
        (newState) => this.changeDetectorRef.detectChanges(),
        (progress) => this.onLoadProgressUpdate(progress),
        this.setTraceConfigForAvailableTraces,
      );
    } else {
      // TODO: change to WebAdbConnection
      this.connect = new ProxyConnection(
        (newState) => this.changeDetectorRef.detectChanges(),
        (progress) => this.onLoadProgressUpdate(progress),
        this.setTraceConfigForAvailableTraces,
      );
    }
  }

  ngOnDestroy(): void {
    assertDefined(this.connect).proxy?.removeOnProxyChange(this.onProxyChange);
  }

  async onDeviceClick(deviceId: string) {
    await assertDefined(this.connect).selectDevice(deviceId);
  }

  onProgressUpdate(message: string, progressPercentage: number | undefined) {
    if (
      !LoadProgressComponent.canUpdateComponent(this.lastUiProgressUpdateTimeMs)
    ) {
      return;
    }
    this.isExternalOperationInProgress = true;
    this.progressMessage = message;
    this.progressPercentage = progressPercentage;
    this.lastUiProgressUpdateTimeMs = Date.now();
    this.changeDetectorRef.detectChanges();
  }

  onOperationFinished() {
    this.isExternalOperationInProgress = false;
    this.lastUiProgressUpdateTimeMs = undefined;
    this.changeDetectorRef.detectChanges();
  }

  isOperationInProgress(): boolean {
    return (
      assertDefined(this.connect).isLoadDataState() ||
      this.isExternalOperationInProgress
    );
  }

  async onAddKey(key: string) {
    if (this.connect?.setProxyKey) {
      this.connect.setProxyKey(key);
    }
    await assertDefined(this.connect).restart();
  }

  displayAdbProxyTab() {
    this.isAdbProxy = true;
    this.connect = new ProxyConnection(
      (newState) => this.changeDetectorRef.detectChanges(),
      (progress) => this.onLoadProgressUpdate(progress),
      this.setTraceConfigForAvailableTraces,
    );
  }

  displayWebAdbTab() {
    this.isAdbProxy = false;
    //TODO: change to WebAdbConnection
    this.connect = new ProxyConnection(
      (newState) => this.changeDetectorRef.detectChanges(),
      (progress) => this.onLoadProgressUpdate(progress),
      this.setTraceConfigForAvailableTraces,
    );
  }

  async onChangeDeviceButton() {
    await assertDefined(this.connect).resetLastDevice();
  }

  async onRetryButton() {
    await assertDefined(this.connect).restart();
  }

  async startTracing() {
    console.log('begin tracing');
    const requestedTraces = this.getRequestedTraces();
    const reqEnableConfig = this.requestedEnableConfig();
    const reqSelectedSfConfig = this.requestedSelection('layers_trace');
    const reqSelectedWmConfig = this.requestedSelection('window_trace');
    if (requestedTraces.length < 1) {
      await assertDefined(this.connect).throwNoTargetsError();
      return;
    }

    await assertDefined(this.connect).startTrace(
      requestedTraces,
      reqEnableConfig,
      reqSelectedSfConfig,
      reqSelectedWmConfig,
    );
  }

  async dumpState() {
    console.log('begin dump');
    const requestedDumps = this.getRequestedDumps();
    const dumpSuccessful = await assertDefined(this.connect).dumpState(
      requestedDumps,
    );
    if (dumpSuccessful) {
      this.filesCollected.emit(assertDefined(this.connect).adbData());
    }
  }

  async endTrace() {
    console.log('end tracing');
    await assertDefined(this.connect).endTrace();
    this.filesCollected.emit(assertDefined(this.connect).adbData());
  }

  tabClass(adbTab: boolean) {
    let isActive: string;
    if (adbTab) {
      isActive = this.isAdbProxy ? 'active' : 'inactive';
    } else {
      isActive = !this.isAdbProxy ? 'active' : 'inactive';
    }
    return ['tab', isActive];
  }

  private async onProxyChange(newState: ProxyState) {
    await assertDefined(this.connect).onConnectChange.bind(this.connect)(
      newState,
    );
  }

  private getRequestedTraces() {
    const tracesFromCollection: string[] = [];
    const tracingConfig = assertDefined(this.traceConfig);
    const requested = Object.keys(tracingConfig).filter((traceKey: string) => {
      const traceConfig = tracingConfig[traceKey];
      if (traceConfig.isTraceCollection) {
        traceConfig.config?.enableConfigs.forEach(
          (innerTrace: EnableConfiguration) => {
            if (innerTrace.enabled) {
              tracesFromCollection.push(innerTrace.key);
            }
          },
        );
        return false;
      }
      return traceConfig.run;
    });
    requested.push(...tracesFromCollection);
    requested.push('perfetto_trace'); // always start/stop/fetch perfetto trace
    return requested;
  }

  private getRequestedDumps() {
    const dumpConfig = assertDefined(this.dumpConfig);
    const requested = Object.keys(dumpConfig).filter((dumpKey: string) => {
      return dumpConfig[dumpKey].run;
    });
    requested.push('perfetto_dump'); // always dump/fetch perfetto dump
    return requested;
  }

  private requestedEnableConfig(): string[] {
    const req: string[] = [];
    const tracingConfig = assertDefined(this.traceConfig);
    Object.keys(tracingConfig).forEach((traceKey: string) => {
      const trace = tracingConfig[traceKey];
      if (
        !trace.isTraceCollection &&
        trace.run &&
        trace.config &&
        trace.config.enableConfigs
      ) {
        trace.config.enableConfigs.forEach((con: EnableConfiguration) => {
          if (con.enabled) {
            req.push(con.key);
          }
        });
      }
    });
    return req;
  }

  private requestedSelection(traceType: string): ConfigMap | undefined {
    const tracingConfig = assertDefined(this.traceConfig);
    if (!tracingConfig[traceType].run) {
      return undefined;
    }
    const selected: ConfigMap = {};
    tracingConfig[traceType].config?.selectionConfigs.forEach(
      (con: SelectionConfiguration) => {
        selected[con.key] = con.value;
      },
    );
    return selected;
  }

  private onLoadProgressUpdate(progressPercentage: number) {
    this.progressPercentage = progressPercentage;
    this.changeDetectorRef.detectChanges();
  }

  private setTraceConfigForAvailableTraces = (
    availableTracesConfig: TraceConfigurationMap,
  ) =>
    (this.traceConfig = PersistentStoreProxy.new<TraceConfigurationMap>(
      'TraceConfiguration',
      availableTracesConfig,
      assertDefined(this.storage),
    ));
}
