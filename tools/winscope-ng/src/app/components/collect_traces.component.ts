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
import { Component, Inject, Input, Output, EventEmitter, OnInit, OnDestroy } from "@angular/core";
import { ProxyConnection } from "trace_collection/proxy_connection";
import { Connection } from "trace_collection/connection";
import { setTraces } from "trace_collection/set_traces";
import { ProxyState } from "trace_collection/proxy_client";
import { traceConfigurations, configMap, SelectionConfiguration, EnableConfiguration } from "trace_collection/trace_collection_utils";
import { TraceCoordinator } from "app/trace_coordinator";
import { PersistentStore } from "common/persistent_store";


@Component({
  selector: "collect-traces",
  template: `
      <mat-card-title id="title">Collect Traces</mat-card-title>
      <mat-card-content>

      <div class="connecting-message" *ngIf="connect.isConnectingState()">Connecting...</div>

      <div class="set-up-adb" *ngIf="!connect.adbSuccess()">
        <button id="proxy-tab" mat-raised-button [ngClass]="tabClass(true)" (click)="displayAdbProxyTab()">ADB Proxy</button>
        <!-- <button id="web-tab" mat-raised-button [ngClass]="tabClass(false)" (click)="displayWebAdbTab()">Web ADB</button> -->
        <adb-proxy *ngIf="isAdbProxy" [(proxy)]="connect.proxy!" (addKey)="onAddKey($event)"></adb-proxy>
        <!-- <web-adb *ngIf="!isAdbProxy"></web-adb> TODO: fix web adb workflow -->
      </div>

      <div id="devices-connecting" *ngIf="connect.isDevicesState()">
        <div> {{ objectKeys(connect.devices()).length > 0 ? "Connected devices:" : "No devices detected" }}</div>
          <mat-list class="device-choice">
            <mat-list-item *ngFor="let deviceId of objectKeys(connect.devices())" (click)="connect.selectDevice(deviceId)">
              <mat-icon class="icon-message">
                {{ connect.devices()[deviceId].authorised ? "smartphone" : "screen_lock_portrait" }}
              </mat-icon>
              <span class="icon-message">
                {{ connect.devices()[deviceId].authorised ? connect.devices()[deviceId].model : "unauthorised" }} ({{ deviceId }})
              </span>
            </mat-list-item>
          </mat-list>
      </div>

      <div id="trace-collection-config" *ngIf="connect.isStartTraceState()">
        <div class="device-choice">
            <mat-list class="device-choice">
            <mat-list-item>
                <mat-icon class="icon-message">smartphone</mat-icon>
                <span class="icon-message">
                  {{ connect.selectedDevice().model }} ({{ connect.selectedDeviceId() }})
                </span>
            </mat-list-item>
            </mat-list>
        </div>

        <div class="trace-section">
          <div>
            <button class="start-btn" mat-raised-button (click)="startTracing()">Start Trace</button>
            <button class="dump-btn" mat-raised-button (click)="dumpState()">Dump State</button>
            <button class="change-btn" mat-raised-button (click)="connect.resetLastDevice()">Change Device</button>
          </div>
          <h3>Trace targets:</h3>
          <trace-config
            *ngFor="let traceKey of objectKeys(setTraces.DYNAMIC_TRACES)"
            [trace]="setTraces.DYNAMIC_TRACES[traceKey]"
          ></trace-config>
        </div>

        <div class="dump-section">
          <h3>Dump targets:</h3>
          <div class="selection">
            <mat-checkbox
              *ngFor="let dumpKey of objectKeys(setTraces.DUMPS)"
              [(ngModel)]="setTraces.DUMPS[dumpKey].run"
            >{{setTraces.DUMPS[dumpKey].name}}</mat-checkbox>
          </div>
        </div>
      </div>

      <div class="unknown-error" *ngIf="connect.isErrorState()">
        <mat-icon class="icon-message">error</mat-icon>
        <span class="icon-message">Error:</span>
        <pre>
            {{ connect.proxy?.errorText }}
        </pre>
        <button class="retry-btn" mat-raised-button (click)="connect.restart()">Retry</button>
      </div>

      <div class="end-tracing" *ngIf="connect.isEndTraceState()">
        <span>Tracing...</span>
        <mat-progress-bar md-indeterminate value="{{connect.loadProgress}}"></mat-progress-bar>
        <button class="end-btn" mat-raised-button (click)="endTrace()">End trace</button>
      </div>

      <div class="load-data" *ngIf="connect.isLoadDataState()">
        <span>Loading data...</span>
        <mat-progress-bar md-indeterminate></mat-progress-bar>
      </div>

      </mat-card-content>
  `,
  styles: [
    ".device-choice {cursor: pointer}",
    ".mat-checkbox .mat-checkbox-frame {transform: scale(0.7); font-size: 10;}",
    ".mat-checkbox-checked .mat-checkbox-background {transform: scale(0.7); font-size: 10;}"
  ]
})
export class CollectTracesComponent implements OnInit, OnDestroy {
  objectKeys = Object.keys;
  isAdbProxy = true;
  traceConfigurations = traceConfigurations;
  connect: Connection = new ProxyConnection();
  setTraces = setTraces;
  dataLoaded = false;

  @Input() store!: PersistentStore;
  @Input() traceCoordinator!: TraceCoordinator;

  @Output() dataLoadedChange = new EventEmitter<boolean>();

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
    this.store.addToStore("adb.proxyKey", key);
    if (this.connect.setProxyKey) {
      this.connect.setProxyKey(key);
    }
    this.connect.restart();
  }

  public onProxyChange(newState: ProxyState) {
    this.connect.onConnectChange(newState);
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

  public requestedTraces() {
    const tracesFromCollection: Array<string> = [];
    const req = Object.keys(setTraces.DYNAMIC_TRACES)
      .filter((traceKey:string) => {
        const traceConfig = setTraces.DYNAMIC_TRACES[traceKey];
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

  public requestedDumps() {
    return Object.keys(setTraces.DUMPS)
      .filter((dumpKey:string) => {
        return setTraces.DUMPS[dumpKey].run;
      });
  }

  public requestedEnableConfig(): Array<string> | undefined {
    const req: Array<string> = [];
    Object.keys(setTraces.DYNAMIC_TRACES)
      .forEach((traceKey:string) => {
        const trace = setTraces.DYNAMIC_TRACES[traceKey];
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

  public requestedSelection(traceType: string): configMap | undefined {
    if (!setTraces.DYNAMIC_TRACES[traceType].run) {
      return undefined;
    }
    const selected: configMap = {};
    setTraces.DYNAMIC_TRACES[traceType].config?.selectionConfigs.forEach(
      (con: SelectionConfiguration) => {
        selected[con.key] = con.value;
      }
    );
    return selected;
  }

  public startTracing() {
    console.log("begin tracing");
    setTraces.reqTraces = this.requestedTraces();
    const reqEnableConfig = this.requestedEnableConfig();
    const reqSelectedSfConfig = this.requestedSelection("layers_trace");
    const reqSelectedWmConfig = this.requestedSelection("window_trace");
    if (setTraces.reqTraces.length < 1) {
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
    setTraces.reqDumps = this.requestedDumps();
    await this.connect.dumpState();
    while (!setTraces.dataReady && !setTraces.dumpError) {
      await this.waitForData(1000);
    }
    if (!setTraces.dumpError) {
      await this.loadFiles();
    } else {
      this.traceCoordinator.clearData();
    }
  }

  public async endTrace() {
    console.log("end tracing");
    await this.connect.endTrace();
    while (!setTraces.dataReady) {
      await this.waitForData(1000);
    }
    await this.loadFiles();
  }

  public async loadFiles() {
    console.log("loading files", this.connect.adbData());
    this.traceCoordinator.clearData();

    await this.traceCoordinator.addTraces(this.connect.adbData());
    this.dataLoaded = true;
    this.dataLoadedChange.emit(this.dataLoaded);
    console.log("finished loading data!");
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

  private waitForData(ms: number) {
    return new Promise( resolve => setTimeout(resolve, ms) );
  }
}
