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
import {MatSelectChange} from '@angular/material/select';
import {
  assertDefined,
  assertTrue,
  assertUnreachable,
} from 'common/assert_utils';
import {FunctionUtils} from 'common/function_utils';
import {Store} from 'common/store/store';
import {UserNotifier} from 'common/user_notifier';
import {Analytics} from 'logging/analytics';
import {ProgressListener} from 'messaging/progress_listener';
import {ProxyTraceTimeout} from 'messaging/user_warnings';
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
import {
  AdbDeviceConnection,
  AdbDeviceState,
} from 'trace_collection/adb/adb_device_connection';
import {AdbConnectionType} from 'trace_collection/adb_connection_type';
import {AdbFiles, RequestedTraceTypes} from 'trace_collection/adb_files';
import {ConnectionState} from 'trace_collection/connection_state';
import {ConnectionStateListener} from 'trace_collection/connection_state_listener';
import {TraceCollectionController} from 'trace_collection/controller/trace_collection_controller';
import {
  CheckboxConfiguration,
  makeDefaultDumpConfigMap,
  makeDefaultTraceConfigMap,
  makeScreenRecordingSelectionConfigs,
  SelectionConfiguration,
  TraceConfigurationMap,
  updateConfigsFromStore,
} from 'trace_collection/ui/ui_trace_configuration';
import {UiTraceTarget} from 'trace_collection/ui/ui_trace_target';
import {UserRequest, UserRequestConfig} from 'trace_collection/user_request';
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

      <mat-card-content *ngIf="controller" class="collect-card-content">
        <mat-form-field class="connection-type">
          <mat-label>Select connection type</mat-label>
          <mat-select
            [value]="getConnectionType()"
            (selectionChange)="onConnectionChange($event)"
            [disabled]="disableTraceSection()">
            <mat-option [value]="AdbConnectionType.WINSCOPE_PROXY">
                <span>{{AdbConnectionType.WINSCOPE_PROXY}}</span>
              </mat-option>
            <mat-option [value]="AdbConnectionType.WDP">
                <span>{{AdbConnectionType.WDP}}</span>
              </mat-option>
          </mat-select>
        </mat-form-field>

        <button
          mat-icon-button
          class="refresh-connection"
          (click)="onRetryConnection()"
          matTooltip="Refresh connection"><mat-icon>refresh</mat-icon></button>

        <ng-container *ngIf="!adbSuccess()">
          <winscope-proxy-setup
            *ngIf="getConnectionType() === AdbConnectionType.WINSCOPE_PROXY"
            [state]="state"
            (retryConnection)="onRetryConnection($event)"></winscope-proxy-setup>
          <wdp-setup
            *ngIf="getConnectionType() === AdbConnectionType.WDP"
            [state]="state"
            (retryConnection)="onRetryConnection()"></wdp-setup>
        </ng-container>

        <div *ngIf="showAllDevices()" class="devices-connecting">
          <div
            *ngIf="controller.getDevices().length === 0"
            class="no-device-detected">
            <p class="mat-body-3 icon">
              <mat-icon inline fontIcon="phonelink_erase"></mat-icon>
            </p>
            <p class="mat-body-1">No devices detected</p>
          </div>
          <div
            *ngIf="controller.getDevices().length > 0"
            class="device-selection">
            <p class="mat-body-1 instruction">Select a device:</p>
            <mat-list>
              <mat-list-item
                *ngFor="let device of controller.getDevices()"
                [disabled]="device.state === ${AdbDeviceState.OFFLINE}"
                (click)="onDeviceClick(device)"
                class="available-device">
                <mat-icon matListIcon>
                  {{ getDeviceStateIcon(device.state) }}
                </mat-icon>
                <p matLine>
                  {{ getDeviceName(device) }}
                </p>
                <mat-icon
                  *ngIf="showTryAuthorizeButton(device)"
                  class="material-symbols-outlined authorize-btn"
                  matTooltip="Authorize device"
                  (click)="device.tryAuthorize()">lock_open</mat-icon>
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

          <mat-tab-group [selectedIndex]="targetTabIndex" class="target-tabs">
            <mat-tab
              label="Trace"
              [disabled]="disableTraceSection()">
              <div class="tabbed-section">
                <div
                  class="trace-section"
                  *ngIf="state === ${ConnectionState.IDLE}">
                  <trace-config
                    title="Trace targets"
                    [traceConfig]="traceConfig"
                    [storage]="storage"
                    [traceConfigStoreKey]="storeKeyPrefixTraceConfig"
                    (traceConfigChange)="onTraceConfigChange($event)"></trace-config>
                  <div class="start-btn">
                    <button
                      color="primary"
                      mat-raised-button
                      (click)="startTracing()">Start trace</button>
                  </div>
                </div>

                <div *ngIf="isTracingOrLoading()" class="tracing-progress">
                  <load-progress
                    [icon]="progressIcon"
                    [message]="progressMessage"
                    [progressPercentage]="progressPercentage">
                  </load-progress>
                  <div class="end-btn" *ngIf="isTracing()">
                    <button
                      color="primary"
                      mat-raised-button
                      [disabled]="state !== ${ConnectionState.TRACING}"
                      (click)="endTrace()">
                      End trace
                    </button>
                  </div>
                </div>
              </div>
            </mat-tab>
            <mat-tab
              label="Dump"
              [disabled]="isTracingOrLoading()">
              <div class="tabbed-section">
                <div
                  class="dump-section"
                  *ngIf="state === ${ConnectionState.IDLE} && !refreshDumps">
                  <trace-config
                    title="Dump targets"
                    [traceConfig]="dumpConfig"
                    [storage]="storage"
                    [traceConfigStoreKey]="storeKeyPrefixDumpConfig"
                    (traceConfigChange)="onDumpConfigChange($event)"></trace-config>
                  <div class="dump-btn" *ngIf="!refreshDumps">
                    <button
                      color="primary"
                      mat-raised-button
                      (click)="dumpState()">Dump state</button>
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

        <div *ngIf="state === ${ConnectionState.ERROR}" class="unknown-error">
          <p class="error-wrapper mat-body-1">
            <mat-icon class="error-icon">error</mat-icon>
            Error:
          </p>
          <pre> {{ errorText }} </pre>
          <button
            color="primary"
            class="retry-btn"
            mat-raised-button
            (click)="onRetryButton()">Retry</button>
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
      .trace-collection-config,
      .trace-section,
      .dump-section,
      .tracing-progress,
      trace-config {
        display: flex;
        flex-direction: column;
        gap: 10px;
      }
      .trace-section,
      .dump-section,
      .tracing-progress {
        height: 100%;
      }
      .winscope-proxy-setup-tab,
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

      .target-tabs {
        flex-grow: 1;
      }

      .target-tabs .mat-tab-body-wrapper {
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
  implements
    ProgressListener,
    WinscopeEventListener,
    WinscopeEventEmitter,
    ConnectionStateListener
{
  objectKeys = Object.keys;
  AdbConnectionType = AdbConnectionType;
  isExternalOperationInProgress = false;
  progressMessage = 'Fetching...';
  progressIcon = 'sync';
  progressPercentage: number | undefined;
  lastUiProgressUpdateTimeMs?: number;
  refreshDumps = false;
  targetTabIndex = 0;
  traceConfig: TraceConfigurationMap;
  dumpConfig: TraceConfigurationMap;
  requestedTraceTypes: RequestedTraceTypes[] = [];
  controller: TraceCollectionController | undefined;
  state = ConnectionState.CONNECTING;
  errorText = '';

  readonly storeKeyPrefixTraceConfig = 'TraceSettings.';
  readonly storeKeyPrefixDumpConfig = 'DumpSettings.';
  private readonly storeKeyImeWarning = 'doNotShowImeWarningDialog';
  private readonly storeKeyLastDevice = 'adb.lastDevice';
  private readonly storeKeyAdbConnectionType = 'adbConnectionType';

  private selectedDevice: AdbDeviceConnection | undefined;
  private emitEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;

  private readonly notConnected = [
    ConnectionState.CONNECTING,
    ConnectionState.NOT_FOUND,
    ConnectionState.UNAUTH,
    ConnectionState.INVALID_VERSION,
  ];
  private readonly tracingSessionStates = [
    ConnectionState.STARTING_TRACE,
    ConnectionState.TRACING,
    ConnectionState.ENDING_TRACE,
    ConnectionState.DUMPING_STATE,
  ];

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

  async ngOnInit() {
    const adbConnectionType = this.storage?.get(this.storeKeyAdbConnectionType);
    if (adbConnectionType !== undefined) {
      await this.changeHostConnection(adbConnectionType);
    } else {
      await this.changeHostConnection(AdbConnectionType.WINSCOPE_PROXY);
    }
  }

  getConnectionType() {
    return this.controller?.getConnectionType();
  }

  ngOnDestroy() {
    if (this.selectedDevice) {
      this.controller?.onDestroy(this.selectedDevice);
    }
  }

  setEmitEvent(callback: EmitEvent) {
    this.emitEvent = callback;
  }

  async onConnectionChange(event: MatSelectChange) {
    this.changeHostConnection(event.value);
  }

  onDeviceClick(device: AdbDeviceConnection) {
    this.selectedDevice = device;
    this.onDevicesChange(assertDefined(this.controller).getDevices());
    this.storage?.add(this.storeKeyLastDevice, device.id);
    this.changeDetectorRef.detectChanges();
  }

  async onWinscopeEvent(event: WinscopeEvent) {
    await event.visit(
      WinscopeEventType.APP_REFRESH_DUMPS_REQUEST,
      async (event) => {
        this.targetTabIndex = 1;
        this.dumpConfig = updateConfigsFromStore(
          JSON.parse(JSON.stringify(assertDefined(this.dumpConfig))),
          assertDefined(this.storage),
          this.storeKeyPrefixDumpConfig,
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
      this.controller?.restartConnection();
    }
    this.changeDetectorRef.detectChanges();
  }

  isLoadOperationInProgress(): boolean {
    return (
      this.state === ConnectionState.LOADING_DATA ||
      this.isExternalOperationInProgress
    );
  }

  async onRetryConnection(token?: string) {
    const controller = assertDefined(this.controller);
    if (token !== undefined) {
      controller.setSecurityToken(token);
    }
    await controller.restartConnection();
  }

  showAllDevices(): boolean {
    const controller = assertDefined(this.controller);
    if (this.state !== ConnectionState.IDLE) {
      return false;
    }

    const devices = controller.getDevices();
    const lastId = this.storage?.get(this.storeKeyLastDevice) ?? undefined;

    if (this.selectedDevice) {
      const newDevice = devices.find((d) => d.id === this.selectedDevice?.id);
      if (newDevice && newDevice.getState() === AdbDeviceState.AVAILABLE) {
        this.selectedDevice = newDevice;
      } else {
        this.selectedDevice = undefined;
      }
    }

    if (this.selectedDevice === undefined && lastId !== undefined) {
      const device = devices.find((d) => d.id === lastId);
      if (device && device.getState() === AdbDeviceState.AVAILABLE) {
        this.selectedDevice = device;
        this.onDevicesChange(devices);
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
    return this.state === ConnectionState.IDLE || this.isTracingOrLoading();
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
    await this.controller?.restartConnection();
  }

  async onRetryButton() {
    await assertDefined(this.controller).restartConnection();
  }

  adbSuccess() {
    return !this.notConnected.includes(this.state);
  }

  async startTracing() {
    const requestedTraces = this.getRequests(assertDefined(this.traceConfig));
    const imeReq = requestedTraces.includes(UiTraceTarget.IME);
    const doNotShowDialog = !!this.storage?.get(this.storeKeyImeWarning);

    if (!imeReq || doNotShowDialog) {
      await this.requestTraces(requestedTraces);
      return;
    }

    const sfReq = requestedTraces.includes(UiTraceTarget.SURFACE_FLINGER_TRACE);
    const transactionsReq = requestedTraces.includes(
      UiTraceTarget.TRANSACTIONS,
    );
    const wmReq = requestedTraces.includes(UiTraceTarget.WINDOW_MANAGER_TRACE);
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
    const requestedDumps = this.getRequests(assertDefined(this.dumpConfig));
    if (requestedDumps.length === 0) {
      this.emitEvent(new NoTraceTargetsSelected());
      return;
    }

    const requestedTraceTypes = requestedDumps.map((req) => {
      return {
        name: this.dumpConfig[req].name,
        types: this.dumpConfig[req].types,
      };
    });
    Analytics.Tracing.logCollectDumps(requestedTraceTypes.map((t) => t.name));

    const requestedDumpsWithConfig: UserRequest[] = requestedDumps.map(
      (target) => {
        const enabledConfig = this.requestedEnabledConfig(
          target,
          this.dumpConfig,
        );
        const selectedConfig = this.requestedSelectedConfig(
          target,
          this.dumpConfig,
        );
        return {
          target,
          config: enabledConfig.concat(selectedConfig),
        };
      },
    );

    const controller = assertDefined(this.controller);
    const device = assertDefined(this.selectedDevice);
    await this.setState(ConnectionState.DUMPING_STATE);
    await controller.dumpState(device, requestedDumpsWithConfig);
    this.refreshDumps = false;
    if (this.state === ConnectionState.DUMPING_STATE) {
      this.filesCollected.emit({
        requested: requestedTraceTypes,
        collected: await this.fetchLastSessionData(),
      });
    }
  }

  async endTrace() {
    if (!this.selectedDevice) {
      return;
    }
    const controller = assertDefined(this.controller);
    await this.setState(ConnectionState.ENDING_TRACE);
    await controller.endTrace(this.selectedDevice);
    if (this.state === ConnectionState.ENDING_TRACE) {
      this.filesCollected.emit({
        requested: this.requestedTraceTypes,
        collected: await this.fetchLastSessionData(),
      });
    }
  }

  getDeviceName(device: AdbDeviceConnection): string {
    return device.getFormattedName();
  }

  showTryAuthorizeButton(device: AdbDeviceConnection): boolean {
    return (
      device.getState() === AdbDeviceState.UNAUTHORIZED &&
      this.getConnectionType() === AdbConnectionType.WDP
    );
  }

  getSelectedDevice(): string {
    return this.getDeviceName(assertDefined(this.selectedDevice));
  }

  getDeviceStateIcon(state: AdbDeviceState): string {
    switch (state) {
      case AdbDeviceState.AVAILABLE:
        return 'smartphone';
      case AdbDeviceState.UNAUTHORIZED:
        return 'screen_lock_portrait';
      case AdbDeviceState.OFFLINE:
        return 'mobile_off';
      default:
        assertUnreachable(state);
    }
  }

  isTracing(): boolean {
    return this.tracingSessionStates.includes(this.state);
  }

  isTracingOrLoading(): boolean {
    return this.isTracing() || this.isLoadOperationInProgress();
  }

  isDumpingState(): boolean {
    return (
      this.refreshDumps ||
      this.state === ConnectionState.DUMPING_STATE ||
      this.isLoadOperationInProgress()
    );
  }

  disableTraceSection(): boolean {
    return this.isTracingOrLoading() || this.refreshDumps;
  }

  async fetchExistingTraces() {
    const controller = assertDefined(this.controller);
    const files = await this.fetchLastSessionData();
    this.filesCollected.emit({
      requested: [],
      collected: files,
    });
    if (files.length === 0) {
      await controller.restartConnection();
    }
  }

  onAvailableTracesChange(
    newTraces: UiTraceTarget[],
    removedTraces: UiTraceTarget[],
  ) {
    newTraces.forEach((trace) => {
      const config = assertDefined(this.traceConfig)[trace];
      config.available = true;
    });
    removedTraces.forEach((trace) => {
      const config = assertDefined(this.traceConfig)[trace];
      config.available = false;
    });
  }

  onDevicesChange(devices: AdbDeviceConnection[]) {
    if (!this.selectedDevice) {
      return;
    }
    const device = devices.find(
      (d) => d.id === assertDefined(this.selectedDevice).id,
    );
    if (!device) {
      return;
    }
    const screenRecordingConfig = assertDefined(this.traceConfig)[
      UiTraceTarget.SCREEN_RECORDING
    ].config;
    const displaysConfig = assertDefined(
      screenRecordingConfig.selectionConfigs.find((c) => c.key === 'displays'),
    );
    const multiDisplay = device.hasMultiDisplayScreenRecording();
    const displays = device.getDisplays();

    if (multiDisplay && !Array.isArray(displaysConfig.value)) {
      screenRecordingConfig.selectionConfigs =
        makeScreenRecordingSelectionConfigs(displays, []);
    } else if (!multiDisplay && Array.isArray(displaysConfig.value)) {
      screenRecordingConfig.selectionConfigs =
        makeScreenRecordingSelectionConfigs(displays, '');
    } else {
      screenRecordingConfig.selectionConfigs[0].options = displays;
    }

    const screenshotConfig = assertDefined(this.dumpConfig)[
      UiTraceTarget.SCREENSHOT
    ].config;
    assertDefined(
      screenshotConfig.selectionConfigs.find((c) => c.key === 'displays'),
    ).options = displays;
    this.changeDetectorRef.detectChanges();
  }

  async onError(errorText: string) {
    await this.setState(ConnectionState.ERROR, errorText);
  }

  async onConnectionStateChange(newState: ConnectionState): Promise<void> {
    switch (newState) {
      case ConnectionState.IDLE:
        if (this.state === ConnectionState.CONNECTING) {
          await this.setState(newState);
        }
        return;
      case ConnectionState.CONNECTING:
        await this.setState(newState);
        return;
      default:
        if (newState !== this.state) {
          await this.setState(newState);
        }
    }
  }

  private async changeHostConnection(adbConnectionType: string) {
    if (this.selectedDevice) {
      await this.controller?.onDestroy(this.selectedDevice);
    }
    this.controller = new TraceCollectionController(adbConnectionType, this);
    this.storage?.add(this.storeKeyAdbConnectionType, adbConnectionType);
    await this.controller.restartConnection();
  }

  private async requestTraces(requestedTraces: UiTraceTarget[]) {
    this.requestedTraceTypes = requestedTraces.map((req) => {
      return {
        name: this.traceConfig[req].name,
        types: this.traceConfig[req].types,
      };
    });
    Analytics.Tracing.logCollectTraces(
      this.requestedTraceTypes.map((t) => t.name),
    );

    if (requestedTraces.length === 0) {
      this.emitEvent(new NoTraceTargetsSelected());
      return;
    }

    const requestedTracesWithConfig: UserRequest[] = requestedTraces.map(
      (target) => {
        const enabledConfig = this.requestedEnabledConfig(
          target,
          this.traceConfig,
        );
        const selectedConfig = this.requestedSelectedConfig(
          target,
          this.traceConfig,
        );
        return {
          target,
          config: enabledConfig.concat(selectedConfig),
        };
      },
    );
    const startTimeMs = Date.now();
    await this.setState(ConnectionState.STARTING_TRACE);
    await assertDefined(this.controller).startTrace(
      assertDefined(this.selectedDevice),
      requestedTracesWithConfig,
    );
    if (this.state === ConnectionState.STARTING_TRACE) {
      Analytics.Tracing.logStartTime(Date.now() - startTimeMs);
      await this.setState(ConnectionState.TRACING);
    }
  }

  private async fetchLastSessionData() {
    await this.setState(ConnectionState.LOADING_DATA);
    const startTimeMs = Date.now();
    const files = await assertDefined(this.controller).fetchLastSessionData(
      assertDefined(this.selectedDevice),
    );
    if (files.length === 0) {
      Analytics.Proxy.logNoFilesFound();
    }
    const size = files.reduce((total, file) => (total += file.size), 0);
    Analytics.Loading.logFileExtractionTime(
      'device',
      Date.now() - startTimeMs,
      size,
    );
    return files;
  }

  private getRequests(configMap: TraceConfigurationMap): UiTraceTarget[] {
    return Object.keys(configMap)
      .filter((dumpKey: string) => {
        return configMap[dumpKey].config.enabled && dumpKey in UiTraceTarget;
      })
      .map((key) => Number(key)) as UiTraceTarget[];
  }

  private requestedEnabledConfig(
    target: UiTraceTarget,
    configMap: TraceConfigurationMap,
  ): UserRequestConfig[] {
    const req: UserRequestConfig[] = [];
    const trace = configMap[target];
    assertTrue(trace?.config.enabled ?? false);
    trace.config.checkboxConfigs.forEach((con: CheckboxConfiguration) => {
      if (con.enabled) {
        req.push({key: con.key});
      }
    });
    return req;
  }

  private requestedSelectedConfig(
    target: UiTraceTarget,
    configMap: TraceConfigurationMap,
  ): UserRequestConfig[] {
    const trace = configMap[target];
    assertTrue(trace?.config.enabled ?? false);
    return trace.config.selectionConfigs.map((con: SelectionConfiguration) => {
      return {key: con.key, value: con.value};
    });
  }

  private async setState(newState: ConnectionState, errorText = '') {
    this.updateProgressMessage(newState);

    const controller = assertDefined(this.controller);

    this.state = newState;
    this.errorText = errorText;
    this.changeDetectorRef.detectChanges();

    const maybeRefreshDumps =
      this.refreshDumps &&
      newState !== ConnectionState.LOADING_DATA &&
      newState !== ConnectionState.CONNECTING;
    if (
      maybeRefreshDumps &&
      newState === ConnectionState.IDLE &&
      this.selectedDevice
    ) {
      await this.dumpState();
    } else if (maybeRefreshDumps) {
      // device is not connected or proxy is not started/invalid/in error state
      // so cannot refresh dump automatically
      this.refreshDumps = false;
    }

    const deviceRequestStates = [
      ConnectionState.IDLE,
      ConnectionState.CONNECTING,
    ];
    if (!deviceRequestStates.includes(newState)) {
      controller.cancelDeviceRequests();
    }

    switch (newState) {
      case ConnectionState.TRACE_TIMEOUT:
        UserNotifier.add(new ProxyTraceTimeout());
        await this.endTrace();
        return;
      case ConnectionState.NOT_FOUND:
        Analytics.Proxy.logServerNotFound(controller.getConnectionType());
        return;

      case ConnectionState.ERROR:
        Analytics.Error.logProxyError(this.errorText);
        return;

      case ConnectionState.CONNECTING:
        await controller.requestDevices();
        return;

      case ConnectionState.IDLE: {
        await this.selectedDevice?.updateAvailableTraces();
        return;
      }
      default:
      // do nothing
    }
  }

  private updateProgressMessage(newState: ConnectionState) {
    switch (newState) {
      case ConnectionState.STARTING_TRACE:
        this.progressMessage = 'Starting trace...';
        this.progressIcon = 'cable';
        this.progressPercentage = undefined;
        break;
      case ConnectionState.TRACING:
        this.progressMessage = 'Tracing...';
        this.progressIcon = 'cable';
        this.progressPercentage = undefined;
        break;
      case ConnectionState.ENDING_TRACE:
        this.progressMessage = 'Ending trace...';
        this.progressIcon = 'cable';
        break;
      case ConnectionState.DUMPING_STATE:
        this.progressMessage = 'Dumping state...';
        this.progressIcon = 'cable';
        break;
      default:
        this.progressIcon = 'sync';
    }
  }
}
