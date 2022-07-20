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
import { Component, Input, OnInit, Output, EventEmitter } from "@angular/core";
import { ProxyConnection, Device, configureTraces } from "../trace_collection/connection";
import { ProxyState } from "../trace_collection/proxy_client";
import { traceConfigurations, configMap, SelectionConfiguration } from "../trace_collection/trace_collection_utils";
import { Core } from "app/core";
import { PersistentStore } from "../common/persistent_store";


@Component({
  selector: "collect-traces",
  template: `
      <mat-card-title>Collect Traces</mat-card-title>
      <mat-card-content>

      <div *ngIf="connect.isConnectingState()">Connecting...</div>

      <div id="set-up-adb" *ngIf="!adbSuccess()">
        <button mat-raised-button [ngClass]="tabClass(true)" (click)="displayAdbProxyTab()">ADB Proxy</button>
        <button mat-raised-button [ngClass]="tabClass(false)" (click)="displayWebAdbTab()">Web ADB</button>
        <adb-proxy *ngIf="isAdbProxy" [(proxy)]="connect.proxy" (addKey)="onAddKey($event)"></adb-proxy>
        <web-adb *ngIf="!isAdbProxy"></web-adb>
      </div>

      <div id="devices-connecting" *ngIf="connect.isDevicesState()">
        <div> {{ devices().length > 0 ? "Connected devices:" : "No devices detected" }}</div>
          <mat-list class="device-choice">
            <mat-list-item *ngFor="let deviceId of devices()" (click)="selectDevice(deviceId)">
              <mat-icon class="icon-message">
                {{ connect.proxy.devices[deviceId].authorised ? "smartphone" : "screen_lock_portrait" }}
              </mat-icon>
              <span class="icon-message">
                {{ connect.proxy.devices[deviceId].authorised ? connect.proxy.devices[deviceId].model : "unauthorised" }} ({{ deviceId }})
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
                  {{ selectedDevice().model }} ({{ connect.proxy.selectedDevice }})
                </span>
            </mat-list-item>
            </mat-list>
        </div>

        <div class="trace-section">
          <div class="md-layout">
            <button mat-raised-button class="md-accent" (click)="startTracing()">Start Trace</button>
            <button mat-raised-button (click)="dumpState()">Dump State</button>
            <button mat-raised-button class="md-primary" (click)="resetLastDevice()">Change Device</button>
          </div>
          <h3>Trace targets:</h3>
          <trace-config
            *ngFor="let traceKey of objectKeys(connect.DYNAMIC_TRACES())"
            [trace]="connect.DYNAMIC_TRACES()[traceKey]"
          ></trace-config>
        </div>

        <div class="dump-section">
          <h3>Dump targets:</h3>
          <div class="selection">
            <mat-checkbox
              class="md-primary"
              *ngFor="let dumpKey of objectKeys(connect.DUMPS)"
              [(ngModel)]="connect.DUMPS[dumpKey].enabled"
            >{{connect.DUMPS[dumpKey].name}}</mat-checkbox>
          </div>
        </div>
      </div>

      <div id="unknown-error" *ngIf="connect.isErrorState()">
        <mat-icon class="icon-message">error</mat-icon>
        <span class="icon-message">Error:</span>
        <pre>
            {{ connect.proxy.errorText }}
        </pre>
        <button mat-raised-button (click)="restart()">Retry</button>
      </div>

      <div id="end-tracing" *ngIf="connect.isEndTraceState()">
        <span class="md-subheading">Tracing...</span>
        <mat-progress-bar md-indeterminate value="{{connect.loadProgress}}"></mat-progress-bar>
        <button mat-raised-button (click)="endTrace()">End trace</button>
      </div>

      <div id="load-data" *ngIf="connect.isLoadDataState()">
        <span class="md-subheading">Loading data...</span>
        <mat-progress-bar md-indeterminate></mat-progress-bar>
      </div>

      </mat-card-content>
  `,
  styles: [".device-choice {cursor: pointer}"]
})
export class CollectTracesComponent implements OnInit {
  objectKeys = Object.keys;
  isAdbProxy = true;
  startTrace = false;
  startDump = false;
  traceConfigurations = traceConfigurations;
  connect: any = new ProxyConnection();
  downloadProxyUrl = "https://android.googlesource.com/platform/development/+/master/tools/winscope-ng/adb/winscope_proxy.py";

  @Input()
  store: PersistentStore = new PersistentStore();

  @Input()
  core: Core = new Core();

  @Output()
  coreChange = new EventEmitter<Core>();

  @Input()
  dataLoaded: boolean = false;

  @Output()
  dataLoadedChange = new EventEmitter<boolean>();

  ngOnInit(): void {
    if (this.isAdbProxy) {
      this.connect = new ProxyConnection();
    } else {
      //TODO: change to WebAdbConnection
      this.connect = new ProxyConnection();
    }
  }

  ngOnDestroy(): void {
    this.connect.proxy.removeOnProxyChange(this.onProxyChange);
  }

  public onAddKey(key: string) {
    this.store.addToStore("adb.proxyKey", key);
    this.connect.setProxyKey(key);
    this.restart();
  }

  public onProxyChange(newState: ProxyState) {
    this.connect.onConnectChange(newState);
  }

  public adbSuccess() {
    return this.connect.adbSuccess();
  }

  public devices(): Array<string> {
    return this.connect.devices();
  }

  public selectedDevice(): Device {
    return this.connect.selectedDevice();
  }

  public restart() {
    this.connect.restart();
  }

  public resetLastDevice() {
    this.connect.resetLastDevice();
  }

  public selectDevice(id: string) {
    this.connect.selectDevice(id);
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
    const tracesFromCollection: Array<any> = [];
    const req = Object.keys(this.connect.DYNAMIC_TRACES())
      .filter((traceKey:string) => {
        const traceConfig = this.connect.DYNAMIC_TRACES()[traceKey];
        if (traceConfig.isTraceCollection) {
          traceConfig.config.enableConfigs.forEach((innerTrace:any) => {
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
    return Object.keys(this.connect.DUMPS)
      .filter((dumpKey:any) => {
        return this.connect.DUMPS[dumpKey].enabled;
      });
  }

  public requestedEnableConfig(): Array<string> | null{
    const req: Array<string> = [];
    Object.keys(this.connect.DYNAMIC_TRACES())
      .forEach((traceKey:any) => {
        const trace = this.connect.DYNAMIC_TRACES()[traceKey];
        if(!trace.isTraceCollection
              && trace.run
              && trace.config
              && trace.config.enableConfigs) {
          trace.config.enableConfigs.forEach((con:any) => {
            if (con.enabled) {
              req.push(con.key);
            }
          });
        }
      });
    if (req.length === 0) {
      return null;
    }
    return req;
  }

  public requestedSelection(traceType: string) {
    if (!this.connect.DYNAMIC_TRACES()[traceType].run) {
      return null;
    }
    const selected: configMap = {};
    this.connect.DYNAMIC_TRACES()[traceType].config.selectionConfigs.forEach(
      (con: SelectionConfiguration) => {
        selected[con.key] = con.value;
      }
    );
    return selected;
  }

  public startTracing() {
    this.startTrace = true;
    console.log("begin tracing");
    configureTraces.reqTraces = this.requestedTraces();
    const reqEnableConfig = this.requestedEnableConfig();
    const reqSelectedSfConfig = this.requestedSelection("layers_trace");
    const reqSelectedWmConfig = this.requestedSelection("window_trace");
    if (configureTraces.reqTraces.length < 1) {
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
    this.startDump = true;
    console.log("begin dump");
    configureTraces.reqDumps = this.requestedDumps();
    await this.connect.dumpState();
    while (!this.connect.proxy.dataReady) {
      await this.waitForData(1000);
    }
    await this.loadFiles();
  }

  public async endTrace() {
    console.log("end tracing");
    await this.connect.endTrace();
    while (!this.connect.proxy.dataReady) {
      await this.waitForData(1000);
    }
    await this.loadFiles();
  }

  public async loadFiles() {
    console.log("loading files", this.connect.adbData());
    await this.core.bootstrap(this.connect.adbData());
    this.dataLoaded = true;
    this.dataLoadedChange.emit(this.dataLoaded);
    this.coreChange.emit(this.core);
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
