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
import {Component, Input, OnInit} from "@angular/core";
import { Connection, ProxyConnection, Device } from "./connection";
import { ProxyState } from "./proxy_client";
import { traceConfigurations, configMap } from "./trace_collection_utils";
import { PersistentStore } from "../common/persistent_store";

@Component({
  selector: "collect-traces",
  template: `
      <mat-card-title>Collect Traces</mat-card-title>
      <mat-card-content>

      <div *ngIf="connect.state()===states.CONNECTING">Connecting...</div>

      <div id="set-up-adb" *ngIf="!adbSuccess()">
        <button mat-raised-button [ngClass]="tabClass(true)" (click)="displayAdbProxyTab()">ADB Proxy</button>
        <button mat-raised-button [ngClass]="tabClass(false)" (click)="displayWebAdbTab()">Web ADB</button>
        <adb-proxy *ngIf="isAdbProxy" [(proxy)]="connect.proxy" (addKey)="onAddKey($event)"></adb-proxy>
        <web-adb *ngIf="!isAdbProxy"></web-adb>
      </div>

      <div id="devices-connecting" *ngIf="connect.state()===states.DEVICES">
        <div> {{ devices().length > 0 ? "Connected devices:" : "No devices detected" }}</div>
          <mat-list class="device-choice">
            <mat-list-item *ngFor="let deviceId of devices()" (click)="selectDevice(deviceId)">
              <mat-icon>{{ connect.proxy.devices[deviceId].authorised ? "smartphone" : "screen_lock_portrait" }}</mat-icon>
              <span class="md-list-item-text">{{ connect.proxy.devices[deviceId].authorised ? connect.proxy.devices[deviceId].model : "unauthorised" }} ({{ deviceId }})</span>
            </mat-list-item>
          </mat-list>
      </div>

      <div id="trace-collection-config" *ngIf="connect.state()===states.START_TRACE">
        <div class="device-choice">
            <mat-list class="device-choice">
            <mat-list-item>
                <mat-icon>smartphone</mat-icon>
                <span class="md-list-item-text">{{ selectedDevice().model }} ({{ connect.proxy.selectedDevice }})</span>
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
            *ngFor="let trace of traceConfigurations"
            [name]="trace.name"
            [defaultCheck]="trace.defaultCheck"
            [configs]="trace.config ? trace.config : null"
          ></trace-config>
        </div>

        <div class="dump-section">
          <h3>Dump targets:</h3>
          <div class="selection">
            <mat-checkbox class="md-primary" *ngFor="let dumpKey of objectKeys(DUMPS)" [checked]="true">{{DUMPS[dumpKey]}}</mat-checkbox>
          </div>
        </div>
      </div>

      <div id="unknown-error" *ngIf="connect.state()===states.ERROR">
        <mat-icon>error</mat-icon>
        <span class="md-subheading">Error:</span>
        <pre>
            {{ errorText }}
        </pre>
        <button mat-raised-button (click)="restart()">Retry</button>
      </div>

      <div id="end-tracing" *ngIf="connect.state()===states.END_TRACE">
        <span class="md-subheading">Tracing...</span>
        <mat-progress-bar md-indeterminate></mat-progress-bar>
        <pre>
            {{ errorText }}
        </pre>
        <button mat-raised-button (click)="endTrace()">End trace</button>
      </div>

      <div id="load-data" *ngIf="connect.state()===states.LOAD_DATA">
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
  errorText = "";
  traceConfigurations = traceConfigurations;

  connect: any = new ProxyConnection();

  downloadProxyUrl = "https://android.googlesource.com/platform/development/+/master/tools/winscope-ng/adb/winscope_proxy.py";

  states = ProxyState;

  @Input()
  store: PersistentStore = new PersistentStore();

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

  public onConnectChange(newState: Connection) {
    this.connect.onConnectChange(newState);
  }

  public onProxyChange(newState: ProxyState, errorText: string) {
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

  public startTracing() {
    this.startTrace = true;
    console.log("begin tracing");
  }

  public dumpState() {
    this.startDump = true;
    console.log("begin dump");
  }

  public endTrace() {
    console.log("end trace");
  }

  public setAvailableTraces() {
    this.connect.setAvailableTraces();
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

  DYNAMIC_TRACES: any = null;

  DUMPS: configMap = {
    "window_dump": "Window Manager",
    "layers_dump": "Surface Flinger"
  };

}