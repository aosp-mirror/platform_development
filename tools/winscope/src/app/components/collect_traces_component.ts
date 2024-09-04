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
  NgZone,
  Output,
  ViewEncapsulation,
} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {assertDefined} from 'common/assert_utils';
import {FunctionUtils} from 'common/function_utils';
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {Store} from 'common/store';
import {UserNotifier} from 'common/user_notifier';
import {Analytics} from 'logging/analytics';
import {ProgressListener} from 'messaging/progress_listener';
import {ProxyTracingErrors} from 'messaging/user_warnings';
import {
  NoTraceTargetsSelected,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {
  EmitEvent,
  WinscopeEventEmitter,
} from 'messaging/winscope_event_emitter';
import {WinscopeEventListener} from 'messaging/winscope_event_listener';
import {AdbConnection} from 'trace_collection/adb_connection';
import {AdbDevice} from 'trace_collection/adb_device';
import {AdbFiles, RequestedTraceTypes} from 'trace_collection/adb_files';
import {ConnectionState} from 'trace_collection/connection_state';
import {ProxyConnection} from 'trace_collection/proxy_connection';
import {
  EnableConfiguration,
  makeDefaultDumpConfigMap,
  makeDefaultTraceConfigMap,
  makeScreenRecordingConfigs,
  SelectionConfiguration,
  TraceConfigurationMap,
} from 'trace_collection/trace_configuration';
import {TraceRequest, TraceRequestConfig} from 'trace_collection/trace_request';
import {LoadProgressComponent} from './load_progress_component';
import {
  WarningDialogComponent,
  WarningDialogData,
  WarningDialogResult,
} from './warning_dialog_component';

@Component({
  selector: 'collect-traces',
  template: `
    <mat-card class="collect-card">
      <mat-card-title class="title">Collect Traces</mat-card-title>

      <mat-card-content *ngIf="adbConnection" class="collect-card-content">
        <p *ngIf="adbConnection.getState() === ${ConnectionState.CONNECTING}" class="connecting-message mat-body-1">
          Connecting...
        </p>

        <div *ngIf="!adbSuccess()" class="set-up-adb">
          <button
            class="proxy-tab"
            color="primary"
            mat-stroked-button
            [ngClass]="tabClass(true)">
            ADB Proxy
          </button>
          <!-- <button class="web-tab" color="primary" mat-raised-button [ngClass]="tabClass(false)" (click)="displayWebAdbTab()">Web ADB</button> -->
          <adb-proxy
            *ngIf="isAdbProxy()"
            [state]="adbConnection.getState()"
            (retryConnection)="onRetryConnection($event)"></adb-proxy>
          <!-- <web-adb *ngIf="!isAdbProxy()"></web-adb> TODO: fix web adb workflow -->
        </div>

        <div *ngIf="showAllDevices()" class="devices-connecting">
          <div *ngIf="adbConnection.getDevices().length === 0" class="no-device-detected">
            <p class="mat-body-3 icon"><mat-icon inline fontIcon="phonelink_erase"></mat-icon></p>
            <p class="mat-body-1">No devices detected</p>
          </div>
          <div *ngIf="adbConnection.getDevices().length > 0" class="device-selection">
            <p class="mat-body-1 instruction">Select a device:</p>
            <mat-list>
              <mat-list-item
                *ngFor="let device of adbConnection.getDevices()"
                (click)="onDeviceClick(device)"
                class="available-device">
                <mat-icon matListIcon>
                  {{
                    device.authorized ? 'smartphone' : 'screen_lock_portrait'
                  }}
                </mat-icon>
                <p matLine>
                  {{
                    device.authorized
                      ? device.model
                      : 'unauthorized'
                  }}
                  ({{ device.id }})
                </p>
              </mat-list-item>
            </mat-list>
          </div>
        </div>

        <div
          *ngIf="showTraceCollectionConfig()"
          class="trace-collection-config">
          <mat-list>
            <mat-list-item class="selected-device">
              <mat-icon matListIcon>smartphone</mat-icon>
              <p matLine>
                {{ getSelectedDevice()}}
              </p>

              <div class="device-actions">
                <button
                  color="primary"
                  class="change-btn"
                  mat-stroked-button
                  (click)="onChangeDeviceButton()"
                  [disabled]="isTracingOrLoading()">
                  Change device
                </button>
                <button
                  color="primary"
                  class="fetch-btn"
                  mat-stroked-button
                  (click)="fetchExistingTraces()"
                  [disabled]="isTracingOrLoading()">
                  Fetch traces from last session
                </button>
              </div>
            </mat-list-item>
          </mat-list>

          <mat-tab-group [selectedIndex]="selectedTabIndex" class="tracing-tabs">
            <mat-tab
              label="Trace"
              [disabled]="disableTraceSection()">
              <div class="tabbed-section">
                <div class="trace-section" *ngIf="adbConnection.getState() === ${ConnectionState.IDLE}">
                  <trace-config
                    title="Trace targets"
                    [initialTraceConfig]="traceConfig"
                    [storage]="storage"
                    traceConfigStoreKey="TraceSettings"
                    (traceConfigChange)="onTraceConfigChange($event)"></trace-config>
                  <div class="start-btn">
                    <button color="primary" mat-raised-button (click)="startTracing()">
                      Start trace
                    </button>
                  </div>
                </div>

                <div *ngIf="adbConnection.getState() === ${ConnectionState.STARTING_TRACE}" class="starting-trace">
                  <load-progress
                    message="Starting trace...">
                  </load-progress>
                  <div class="end-btn">
                    <button color="primary" mat-raised-button [disabled]="true">
                      End trace
                    </button>
                  </div>
                </div>

                <div *ngIf="adbConnection.getState() === ${ConnectionState.TRACING}" class="tracing">
                  <load-progress
                    icon="cable"
                    message="Tracing...">
                  </load-progress>
                  <div class="end-btn">
                    <button color="primary" mat-raised-button (click)="endTrace()">
                      End trace
                    </button>
                  </div>
                </div>

                <div *ngIf="adbConnection.getState() === ${ConnectionState.ENDING_TRACE}" class="ending-trace">
                  <load-progress
                    icon="cable"
                    message="Ending trace...">
                  </load-progress>
                  <div class="end-btn">
                    <button color="primary" mat-raised-button [disabled]="true">
                      End trace
                    </button>
                  </div>
                </div>

                <div *ngIf="isLoadOperationInProgress()" class="load-data">
                  <load-progress
                    [progressPercentage]="progressPercentage"
                    [message]="progressMessage">
                  </load-progress>
                  <div class="end-btn">
                    <button color="primary" mat-raised-button [disabled]="true">
                      End trace
                    </button>
                  </div>
                </div>
              </div>
            </mat-tab>
            <mat-tab label="Dump" [disabled]="isTracingOrLoading()">
              <div class="tabbed-section">
                <div class="dump-section" *ngIf="adbConnection.getState() === ${ConnectionState.IDLE} && !refreshDumps">
                  <trace-config
                    title="Dump targets"
                    [initialTraceConfig]="dumpConfig"
                    [storage]="storage"
                    [traceConfigStoreKey]="storeKeyDumpConfig"
                    (traceConfigChange)="onDumpConfigChange($event)"></trace-config>
                  <div class="dump-btn" *ngIf="!refreshDumps">
                    <button color="primary" mat-raised-button (click)="dumpState()">
                      Dump state
                    </button>
                  </div>
                </div>

                <load-progress
                  class="dumping-state"
                  *ngIf="isDumpingState()"
                  [progressPercentage]="progressPercentage"
                  [message]="progressMessage">
                </load-progress>
              </div>
            </mat-tab>
          </mat-tab-group>
        </div>

        <div *ngIf="adbConnection.getState() === ${ConnectionState.ERROR}" class="unknown-error">
          <p class="error-wrapper mat-body-1">
            <mat-icon class="error-icon">error</mat-icon>
            Error:
          </p>
          <pre> {{ adbConnection.getErrorText() }} </pre>
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
      .retry-btn,
      .fetch-btn {
        margin-left: 5px;
      }
      .fetch-btn {
        margin-top: 5px;
      }
      .selected-device {
        height: fit-content !important;
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
      .starting-trace,
      .tracing,
      .ending-trace,
      .load-data,
      trace-config {
        display: flex;
        flex-direction: column;
        gap: 10px;
      }
      .trace-section,
      .dump-section,
      .starting-trace,
      .tracing,
      .ending-trace,
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
  implements ProgressListener, WinscopeEventListener, WinscopeEventEmitter
{
  objectKeys = Object.keys;
  isExternalOperationInProgress = false;
  progressMessage = 'Fetching...';
  progressPercentage: number | undefined;
  lastUiProgressUpdateTimeMs?: number;
  refreshDumps = false;
  selectedTabIndex = 0;
  traceConfig: TraceConfigurationMap;
  dumpConfig: TraceConfigurationMap;
  requestedTraceTypes: RequestedTraceTypes[] = [];

  private readonly storeKeyImeWarning = 'doNotShowImeWarningDialog';
  private readonly storeKeyLastDevice = 'adb.lastDevice';
  private readonly storeKeyDumpConfig = 'DumpSettings';

  private selectedDevice: AdbDevice | undefined;
  private emitEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;

  private readonly notConnected = [
    ConnectionState.NOT_FOUND,
    ConnectionState.UNAUTH,
    ConnectionState.INVALID_VERSION,
  ];

  @Input() adbConnection: AdbConnection | undefined;
  @Input() storage: Store | undefined;

  @Output() readonly filesCollected = new EventEmitter<AdbFiles>();

  constructor(
    @Inject(ChangeDetectorRef) private changeDetectorRef: ChangeDetectorRef,
    @Inject(MatDialog) private dialog: MatDialog,
    @Inject(NgZone) private ngZone: NgZone,
  ) {
    this.traceConfig = makeDefaultTraceConfigMap();
    this.dumpConfig = makeDefaultDumpConfigMap();
  }

  ngOnChanges() {
    if (!this.adbConnection) {
      throw new Error('component created without adb connection');
    }
    this.adbConnection.initialize(
      () => this.onConnectionStateChange(),
      this.toggleAvailabilityOfTraces,
      this.handleDevicesChange,
    );
  }

  ngOnDestroy() {
    this.adbConnection?.onDestroy();
  }

  setEmitEvent(callback: EmitEvent) {
    this.emitEvent = callback;
  }

  onDeviceClick(device: AdbDevice) {
    this.selectedDevice = device;
    this.storage?.add(this.storeKeyLastDevice, device.id);
    this.changeDetectorRef.detectChanges();
  }

  async onWinscopeEvent(event: WinscopeEvent) {
    await event.visit(
      WinscopeEventType.APP_REFRESH_DUMPS_REQUEST,
      async (event) => {
        this.selectedTabIndex = 1;
        this.dumpConfig = PersistentStoreProxy.new<TraceConfigurationMap>(
          assertDefined('DumpSettings'),
          assertDefined(
            JSON.parse(JSON.stringify(assertDefined(this.dumpConfig))),
          ),
          assertDefined(this.storage),
        );
        this.refreshDumps = true;
      },
    );
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

  onOperationFinished(success: boolean) {
    this.isExternalOperationInProgress = false;
    this.lastUiProgressUpdateTimeMs = undefined;
    if (!success) {
      this.adbConnection?.restartConnection();
    }
    this.changeDetectorRef.detectChanges();
  }

  isLoadOperationInProgress(): boolean {
    return (
      assertDefined(this.adbConnection).getState() ===
        ConnectionState.LOADING_DATA || this.isExternalOperationInProgress
    );
  }

  async onRetryConnection(token: string) {
    const connection = assertDefined(this.adbConnection);
    connection.setSecurityToken(token);
    await connection.restartConnection();
  }

  showAllDevices(): boolean {
    const connection = assertDefined(this.adbConnection);
    const state = connection.getState();
    if (state !== ConnectionState.IDLE) {
      return false;
    }

    const devices = connection.getDevices();
    const lastId = this.storage?.get(this.storeKeyLastDevice) ?? undefined;
    if (
      this.selectedDevice &&
      !devices.find((d) => d.id === this.selectedDevice?.id)
    ) {
      this.selectedDevice = undefined;
    }

    if (this.selectedDevice === undefined && lastId !== undefined) {
      const device = devices.find((d) => d.id === lastId);
      if (device && device.authorized) {
        this.selectedDevice = device;
        this.storage?.add(this.storeKeyLastDevice, device.id);
        return false;
      }
    }

    return this.selectedDevice === undefined;
  }

  showTraceCollectionConfig(): boolean {
    if (this.selectedDevice === undefined) {
      return false;
    }
    return (
      assertDefined(this.adbConnection).getState() === ConnectionState.IDLE ||
      this.isTracingOrLoading()
    );
  }

  onTraceConfigChange(newConfig: TraceConfigurationMap) {
    this.traceConfig = newConfig;
  }

  onDumpConfigChange(newConfig: TraceConfigurationMap) {
    this.dumpConfig = newConfig;
  }

  async onChangeDeviceButton() {
    this.storage?.add(this.storeKeyLastDevice, '');
    this.selectedDevice = undefined;
    await this.adbConnection?.restartConnection();
  }

  async onRetryButton() {
    await assertDefined(this.adbConnection).restartConnection();
  }

  adbSuccess() {
    const state = this.adbConnection?.getState();
    return !!state && !this.notConnected.includes(state);
  }

  async startTracing() {
    const requestedTraces = this.getRequestedTraces();

    const imeReq = requestedTraces.includes('ime');
    const doNotShowDialog = !!this.storage?.get(this.storeKeyImeWarning);

    if (!imeReq || doNotShowDialog) {
      await this.requestTraces(requestedTraces);
      return;
    }

    const sfReq = requestedTraces.includes('layers_trace');
    const transactionsReq = requestedTraces.includes('transactions');
    const wmReq = requestedTraces.includes('window_trace');
    const imeValidFrameMapping = sfReq && transactionsReq && wmReq;

    if (imeValidFrameMapping) {
      await this.requestTraces(requestedTraces);
      return;
    }

    this.ngZone.run(() => {
      const closeText = 'Collect traces anyway';
      const optionText = 'Do not show again';
      const data: WarningDialogData = {
        message: `Cannot build frame mapping for IME with selected traces - some Winscope features may not work properly.
        Consider the following selection for valid frame mapping:
        Surface Flinger, Transactions, Window Manager, IME`,
        actions: ['Go back'],
        options: [optionText],
        closeText,
      };
      const dialogRef = this.dialog.open(WarningDialogComponent, {
        data,
        disableClose: true,
      });
      dialogRef
        .beforeClosed()
        .subscribe((result: WarningDialogResult | undefined) => {
          if (this.storage && result?.selectedOptions.includes(optionText)) {
            this.storage.add(this.storeKeyImeWarning, 'true');
          }
          if (result?.closeActionText === closeText) {
            this.requestTraces(requestedTraces);
          }
        });
    });
  }

  async dumpState() {
    const requestedDumps = this.getRequestedDumps();
    const requestedTraceTypes = requestedDumps.map((req) => {
      return {
        name: this.dumpConfig[req].name,
        types: this.dumpConfig[req].types,
      };
    });
    Analytics.Tracing.logCollectDumps(requestedDumps);

    if (requestedDumps.length === 0) {
      this.emitEvent(new NoTraceTargetsSelected());
      return;
    }
    requestedDumps.push('perfetto_dump'); // always dump/fetch perfetto dump

    const requestedDumpsWithConfig: TraceRequest[] = requestedDumps.map(
      (dumpName) => {
        const enabledConfig = this.requestedEnabledConfig(
          dumpName,
          this.dumpConfig,
        );
        const selectedConfig = this.requestedSelectedConfig(
          dumpName,
          this.dumpConfig,
        );
        return {
          name: dumpName,
          config: enabledConfig.concat(selectedConfig),
        };
      },
    );

    this.progressMessage = 'Dumping state...';

    const connection = assertDefined(this.adbConnection);
    const device = assertDefined(this.selectedDevice);
    await connection.dumpState(device, requestedDumpsWithConfig);
    this.refreshDumps = false;
    if (connection.getState() === ConnectionState.DUMPING_STATE) {
      this.filesCollected.emit({
        requested: requestedTraceTypes,
        collected: await connection.fetchLastTracingSessionData(device),
      });
    }
  }

  async endTrace() {
    const connection = assertDefined(this.adbConnection);
    const device = assertDefined(this.selectedDevice);
    await connection.endTrace(device);
    if (connection.getState() === ConnectionState.ENDING_TRACE) {
      this.filesCollected.emit({
        requested: this.requestedTraceTypes,
        collected: await connection.fetchLastTracingSessionData(device),
      });
    }
  }

  isAdbProxy(): boolean {
    return this.adbConnection instanceof ProxyConnection;
  }

  tabClass(adbTab: boolean) {
    let isActive: string;
    if (adbTab) {
      isActive = this.isAdbProxy() ? 'active' : 'inactive';
    } else {
      isActive = !this.isAdbProxy() ? 'active' : 'inactive';
    }
    return ['tab', isActive];
  }

  getSelectedDevice(): string {
    const device = assertDefined(this.selectedDevice);
    return device.model + `(${device.id})`;
  }

  isTracingOrLoading(): boolean {
    const state = this.adbConnection?.getState();
    const tracingStates = [
      ConnectionState.STARTING_TRACE,
      ConnectionState.TRACING,
      ConnectionState.ENDING_TRACE,
      ConnectionState.DUMPING_STATE,
    ];
    return (
      (!!state && tracingStates.includes(state)) ||
      this.isLoadOperationInProgress()
    );
  }

  isDumpingState(): boolean {
    return (
      this.refreshDumps ||
      this.adbConnection?.getState() === ConnectionState.DUMPING_STATE ||
      this.isLoadOperationInProgress()
    );
  }

  disableTraceSection(): boolean {
    return this.isTracingOrLoading() || this.refreshDumps;
  }

  async fetchExistingTraces() {
    const connection = assertDefined(this.adbConnection);
    const files = await connection.fetchLastTracingSessionData(
      assertDefined(this.selectedDevice),
    );
    this.filesCollected.emit({
      requested: [],
      collected: files,
    });
    if (files.length === 0) {
      await connection.restartConnection();
    }
  }

  private async requestTraces(requestedTraces: string[]) {
    this.requestedTraceTypes = requestedTraces.map((req) => {
      return {
        name: this.traceConfig[req].name,
        types: this.traceConfig[req].types,
      };
    });
    Analytics.Tracing.logCollectTraces(requestedTraces);

    if (requestedTraces.length === 0) {
      this.emitEvent(new NoTraceTargetsSelected());
      return;
    }
    requestedTraces.push('perfetto_trace'); // always start/stop/fetch perfetto trace

    const requestedTracesWithConfig: TraceRequest[] = requestedTraces.map(
      (traceName) => {
        const enabledConfig = this.requestedEnabledConfig(
          traceName,
          this.traceConfig,
        );
        const selectedConfig = this.requestedSelectedConfig(
          traceName,
          this.traceConfig,
        );
        return {
          name: traceName,
          config: enabledConfig.concat(selectedConfig),
        };
      },
    );
    await assertDefined(this.adbConnection).startTrace(
      assertDefined(this.selectedDevice),
      requestedTracesWithConfig,
    );
  }

  private async onConnectionStateChange() {
    this.changeDetectorRef.detectChanges();

    const connection = assertDefined(this.adbConnection);
    const state = connection.getState();
    if (state === ConnectionState.TRACE_TIMEOUT) {
      UserNotifier.add(new ProxyTracingErrors(['tracing timed out'])).notify();
      this.filesCollected.emit({
        requested: this.requestedTraceTypes,
        collected: await connection.fetchLastTracingSessionData(
          assertDefined(this.selectedDevice),
        ),
      });
      return;
    }

    if (
      !this.refreshDumps ||
      state === ConnectionState.LOADING_DATA ||
      state === ConnectionState.CONNECTING
    ) {
      return;
    }
    if (state === ConnectionState.IDLE && this.selectedDevice) {
      this.dumpState();
    } else {
      // device is not connected or proxy is not started/invalid/in error state
      // so cannot refresh dump automatically
      this.refreshDumps = false;
    }
  }

  private getRequestedTraces(): string[] {
    const tracingConfig = assertDefined(this.traceConfig);
    return Object.keys(tracingConfig).filter((traceKey: string) => {
      return tracingConfig[traceKey].enabled;
    });
  }

  private getRequestedDumps(): string[] {
    let dumpConfig = assertDefined(this.dumpConfig);
    if (this.refreshDumps && this.storage) {
      const storedConfig = this.storage.get(this.storeKeyDumpConfig);
      if (storedConfig) {
        dumpConfig = JSON.parse(storedConfig);
      }
    }
    return Object.keys(dumpConfig).filter((dumpKey: string) => {
      return dumpConfig[dumpKey].enabled;
    });
  }

  private requestedEnabledConfig(
    traceName: string,
    configMap: TraceConfigurationMap,
  ): TraceRequestConfig[] {
    const req: TraceRequestConfig[] = [];
    const trace = configMap[traceName];
    if (trace?.enabled) {
      trace.config?.enableConfigs?.forEach((con: EnableConfiguration) => {
        if (con.enabled) {
          req.push({key: con.key});
        }
      });
    }
    return req;
  }

  private requestedSelectedConfig(
    traceName: string,
    configMap: TraceConfigurationMap,
  ): TraceRequestConfig[] {
    const trace = configMap[traceName];
    if (!trace?.enabled) {
      return [];
    }
    return (
      trace.config?.selectionConfigs.map((con: SelectionConfiguration) => {
        return {key: con.key, value: con.value};
      }) ?? []
    );
  }

  private toggleAvailabilityOfTraces = (traces: string[]) =>
    traces.forEach((trace) => {
      const config = assertDefined(this.traceConfig)[trace];
      config.available = !config.available;
    });

  private handleDevicesChange = (devices: AdbDevice[]) => {
    if (!this.selectedDevice) {
      return;
    }
    const selectedDevice = devices.find(
      (d) => d.id === assertDefined(this.selectedDevice).id,
    );
    if (!selectedDevice) {
      return;
    }
    const screenRecordingConfig = assertDefined(
      this.traceConfig['screen_recording'].config,
    );
    const displays = assertDefined(
      screenRecordingConfig?.selectionConfigs.find((c) => c.key === 'displays'),
    );
    if (
      selectedDevice.multiDisplayScreenRecordingAvailable &&
      !Array.isArray(displays.value)
    ) {
      screenRecordingConfig.selectionConfigs = makeScreenRecordingConfigs(
        selectedDevice.displays,
        [],
      );
    } else if (
      !selectedDevice.multiDisplayScreenRecordingAvailable &&
      Array.isArray(displays.value)
    ) {
      screenRecordingConfig.selectionConfigs = makeScreenRecordingConfigs(
        selectedDevice.displays,
        '',
      );
    } else {
      screenRecordingConfig.selectionConfigs[0].options =
        selectedDevice.displays;
    }

    const screenshotConfig = assertDefined(this.dumpConfig)['screenshot']
      .config;
    assertDefined(
      screenshotConfig?.selectionConfigs.find((c) => c.key === 'displays'),
    ).options = selectedDevice.displays;

    this.changeDetectorRef.detectChanges();
  };
}
