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
import { Component, Input, Inject, Output, EventEmitter, OnInit, OnDestroy } from "@angular/core";
import { ProxyConnection } from "trace_collection/proxy_connection";
import { Connection } from "trace_collection/connection";
import { ProxyState } from "trace_collection/proxy_client";
import { traceConfigurations, configMap, SelectionConfiguration, EnableConfiguration } from "trace_collection/trace_collection_utils";
import { TraceCoordinator } from "app/trace_coordinator";
import { PersistentStore } from "common/utils/persistent_store";
import { MatSnackBar } from "@angular/material/snack-bar";
import { ParserError } from "parsers/parser_factory";
import { ParserErrorSnackBarComponent } from "./parser_error_snack_bar_component";
import { TracingConfig } from "trace_collection/tracing_config";

@Component({
  selector: "collect-traces",
  template: `
    <mat-card class="collect-card">
      <mat-card-title class="title">Collect Traces</mat-card-title>

      <mat-card-content class="collect-card-content">
        <p *ngIf="connect.isConnectingState()" class="connecting-message mat-body-1">Connecting...</p>

        <div *ngIf="!connect.adbSuccess()" class="set-up-adb">
          <button class="proxy-tab" color="primary" mat-stroked-button [ngClass]="tabClass(true)" (click)="displayAdbProxyTab()">ADB Proxy</button>
          <!-- <button class="web-tab" color="primary" mat-raised-button [ngClass]="tabClass(false)" (click)="displayWebAdbTab()">Web ADB</button> -->
          <adb-proxy *ngIf="isAdbProxy" [(proxy)]="connect.proxy!" (addKey)="onAddKey($event)"></adb-proxy>
          <!-- <web-adb *ngIf="!isAdbProxy"></web-adb> TODO: fix web adb workflow -->
        </div>

        <div *ngIf="connect.isDevicesState()" class="devices-connecting">
          <p class="mat-body-1">{{ objectKeys(connect.devices()).length > 0 ? "Connected devices:" : "No devices detected" }}</p>
          <mat-list *ngIf="objectKeys(connect.devices()).length > 0">
            <mat-list-item
              *ngFor="let deviceId of objectKeys(connect.devices())"
              (click)="connect.selectDevice(deviceId)"
              class="available-device"
            >
              <mat-icon matListIcon>
                {{ connect.devices()[deviceId].authorised ? "smartphone" : "screen_lock_portrait" }}
              </mat-icon>
              <p matLine>
                {{ connect.devices()[deviceId].authorised ? connect.devices()[deviceId].model : "unauthorised" }} ({{ deviceId }})
              </p>
            </mat-list-item>
          </mat-list>
        </div>

        <div *ngIf="connect.isStartTraceState()" class="trace-collection-config">
          <mat-list>
            <mat-list-item>
              <mat-icon matListIcon>smartphone</mat-icon>
              <p matLine>
                {{ connect.selectedDevice().model }} ({{ connect.selectedDeviceId() }})

                <button color="primary" class="change-btn" mat-button (click)="connect.resetLastDevice()">Change device</button>
              </p>
            </mat-list-item>
          </mat-list>

          <div class="trace-section">
            <trace-config [traces]="tracingConfig.getTracingConfig()"></trace-config>
            <button color="primary" class="start-btn" mat-stroked-button (click)="startTracing()">Start trace</button>
          </div>

          <mat-divider></mat-divider>

          <div class="dump-section">
            <h3 class="mat-subheading-2">Dump targets</h3>
            <div class="selection">
              <mat-checkbox
                *ngFor="let dumpKey of objectKeys(tracingConfig.getDumpConfig())"
                color="primary"
                class="dump-checkbox"
                [(ngModel)]="tracingConfig.getDumpConfig()[dumpKey].run"
              >{{tracingConfig.getDumpConfig()[dumpKey].name}}</mat-checkbox>
            </div>
            <button color="primary" class="dump-btn" mat-stroked-button (click)="dumpState()">Dump state</button>
          </div>
        </div>

        <div *ngIf="connect.isErrorState()" class="unknown-error">
          <p class="error-wrapper mat-body-1">
            <mat-icon class="error-icon">error</mat-icon>
            Error:
          </p>
          <pre> {{ connect.proxy?.errorText }} </pre>
          <button color="primary" class="retry-btn" mat-raised-button (click)="connect.restart()">Retry</button>
        </div>

        <div *ngIf="connect.isEndTraceState()" class="end-tracing">
          <p class="mat-body-1">Tracing...</p>
          <mat-progress-bar md-indeterminate value="{{connect.loadProgress}}"></mat-progress-bar>
          <button color="primary" class="end-btn" mat-raised-button (click)="endTrace()">End trace</button>
        </div>

        <div *ngIf="connect.isLoadDataState()" class="load-data">
          <p class="mat-body-1">Loading data...</p>
          <mat-progress-bar md-indeterminate></mat-progress-bar>
        </div>

      </mat-card-content>
    </mat-card>
  `,
  styles: [
    `
      .change-btn, .retry-btn, .edn-btn {
        margin-left: 5px;
      }
      .collect-card {
        height: 100%;
        display: flex;
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
      .proxy-tab, .web-tab, .start-btn, .dump-btn, .end-btn {
        align-self: flex-start;
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
    `
  ]
})
export class CollectTracesComponent implements OnInit, OnDestroy {
  objectKeys = Object.keys;
  isAdbProxy = true;
  traceConfigurations = traceConfigurations;
  connect: Connection = new ProxyConnection();
  tracingConfig = TracingConfig.getInstance();
  dataLoaded = false;

  @Input() store!: PersistentStore;
  @Input() traceCoordinator!: TraceCoordinator;

  @Output() dataLoadedChange = new EventEmitter<boolean>();

  constructor(
    @Inject(MatSnackBar) private snackBar: MatSnackBar
  ) {}

  ngOnInit() {
    if (this.isAdbProxy) {
      this.connect = new ProxyConnection();
    } else {
      //TODO: change to WebAdbConnection
      this.connect = new ProxyConnection();
    }
  }

  ngOnDestroy(): void {
    this.connect.proxy?.removeOnProxyChange(this.onProxyChange);
  }

  public onAddKey(key: string) {
    if (this.connect.setProxyKey) {
      this.connect.setProxyKey(key);
    }
    this.connect.restart();
  }

  public displayAdbProxyTab() {
    this.isAdbProxy = true;
    this.connect = new ProxyConnection();
  }

  public displayWebAdbTab() {
    this.isAdbProxy = false;
    //TODO: change to WebAdbConnection
    this.connect = new ProxyConnection();
  }

  public startTracing() {
    console.log("begin tracing");
    this.tracingConfig.requestedTraces = this.requestedTraces();
    const reqEnableConfig = this.requestedEnableConfig();
    const reqSelectedSfConfig = this.requestedSelection("layers_trace");
    const reqSelectedWmConfig = this.requestedSelection("window_trace");
    if (this.tracingConfig.requestedTraces.length < 1) {
      this.connect.throwNoTargetsError();
      return;
    }
    this.connect.startTrace(
      reqEnableConfig,
      reqSelectedSfConfig,
      reqSelectedWmConfig
    );
  }

  public async dumpState() {
    console.log("begin dump");
    this.tracingConfig.requestedDumps = this.requestedDumps();
    const dumpError = await this.connect.dumpState();
    if (!dumpError) {
      await this.loadFiles();
    } else {
      this.traceCoordinator.clearData();
    }
  }

  public async endTrace() {
    console.log("end tracing");
    await this.connect.endTrace();
    await this.loadFiles();
  }

  public tabClass(adbTab: boolean) {
    let isActive: string;
    if (adbTab) {
      isActive = this.isAdbProxy ? "active" : "inactive";
    } else {
      isActive = !this.isAdbProxy ? "active" : "inactive";
    }
    return ["tab", isActive];
  }

  private onProxyChange(newState: ProxyState) {
    this.connect.onConnectChange(newState);
  }

  private requestedTraces() {
    const tracesFromCollection: Array<string> = [];
    const tracingConfig = this.tracingConfig.getTracingConfig();
    const req = Object.keys(tracingConfig)
      .filter((traceKey:string) => {
        const traceConfig = tracingConfig[traceKey];
        if (traceConfig.isTraceCollection) {
          traceConfig.config?.enableConfigs.forEach((innerTrace:EnableConfiguration) => {
            if (innerTrace.enabled) {
              tracesFromCollection.push(innerTrace.key);
            }
          });
          return false;
        }
        return traceConfig.run;
      });
    return req.concat(tracesFromCollection);
  }

  private requestedDumps() {
    const dumpConfig = this.tracingConfig.getDumpConfig();
    return Object.keys(dumpConfig)
      .filter((dumpKey:string) => {
        return dumpConfig[dumpKey].run;
      });
  }

  private requestedEnableConfig(): Array<string> | undefined {
    const req: Array<string> = [];
    const tracingConfig = this.tracingConfig.getTracingConfig();
    Object.keys(tracingConfig)
      .forEach((traceKey:string) => {
        const trace = tracingConfig[traceKey];
        if(!trace.isTraceCollection
              && trace.run
              && trace.config
              && trace.config.enableConfigs) {
          trace.config.enableConfigs.forEach((con:EnableConfiguration) => {
            if (con.enabled) {
              req.push(con.key);
            }
          });
        }
      });
    if (req.length === 0) {
      return undefined;
    }
    return req;
  }

  private requestedSelection(traceType: string): configMap | undefined {
    const tracingConfig = this.tracingConfig.getTracingConfig();
    if (!tracingConfig[traceType].run) {
      return undefined;
    }
    const selected: configMap = {};
    tracingConfig[traceType].config?.selectionConfigs.forEach(
      (con: SelectionConfiguration) => {
        selected[con.key] = con.value;
      }
    );
    return selected;
  }

  private async loadFiles() {
    console.log("loading files", this.connect.adbData());
    this.traceCoordinator.clearData();

    const parserErrors = await this.traceCoordinator.setTraces(this.connect.adbData());
    if (parserErrors.length > 0) {
      this.openTempSnackBar(parserErrors);
    }
    this.dataLoaded = true;
    this.dataLoadedChange.emit(this.dataLoaded);
    console.log("finished loading data!");
  }

  private openTempSnackBar(parserErrors: ParserError[]) {
    this.snackBar.openFromComponent(ParserErrorSnackBarComponent, {
      data: parserErrors,
      duration: 7500,
    });
  }
}
