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
import {Component} from "@angular/core";
import { ProxyState } from "./proxy_client";

interface TraceConfiguration {
  name: string,
  defaultCheck?: boolean,
  config?: ConfigurationOptions
}

export interface ConfigurationOptions {
  enableConfigs: Array<string>,
  selectionConfigs: Array<SelectionConfiguration>
}

export interface SelectionConfiguration {
  name: string,
  options: Array<string>,
  value: string
}

export type configMap = {
[key: string]: Array<string> | string;
}

interface Device {
  authorised: boolean;
  id: string;
}


@Component({
  selector: "collect-traces",
  template: `
    <mat-card-title>Collect Traces</mat-card-title>
    <mat-card-content>
    <div id="set-up-adb" *ngIf="!proxySuccess()">
      <button mat-raised-button [ngClass]="tabClass(true)" (click)="displayAdbProxyTab()">ADB Proxy</button>
      <button mat-raised-button [ngClass]="tabClass(false)" (click)="displayWebAdbTab()">Web ADB</button>
      <adb-proxy *ngIf="isAdbProxy" [(status)]="status"></adb-proxy>
      <web-adb *ngIf="!isAdbProxy"></web-adb>
    </div>

    <div id="trace-collection-config" *ngIf="proxySuccess()">
      <div class="device-list">
        <mat-list>
          <mat-list-item *ngFor="let device of devices()" (click)="selectDevice(device.id)">
            <mat-icon>{{ device.authorised ? "smartphone" : "screen_lock_portrait" }}</mat-icon>
            <span class="md-list-item-text">selected device name</span>
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
          [(status)]="status"
        ></trace-config>
      </div>

      <div class="dump-section">
        <h3>Dump targets:</h3>
        <div class="selection">
          <mat-checkbox class="md-primary" *ngFor="let dumpKey of objectKeys(DUMPS)" >{{DUMPS[dumpKey]}}</mat-checkbox>
        </div>
      </div>
    </div>
    </mat-card-content>
  `,
})
export class CollectTracesComponent {
  objectKeys = Object.keys;
  isAdbProxy = true;
  startTrace = false;
  startDump = false;
  downloadProxyUrl = "https://android.googlesource.com/platform/development/+/master/tools/winscope/adb_proxy/winscope_proxy.py";

  DUMPS: configMap = {
    "window_dump": "Window Manager",
    "layers_dump": "Surface Flinger"
  };

  wmTraceSelectionConfigs = [
    {
      name: "wmbuffersize (KB)",
      options: [
        "4000",
        "8000",
        "16000",
        "32000",
      ],
      value: "4000"
    },
    {
      name: "tracingtype",
      options: [
        "frame",
        "transaction",
      ],
      value: "frame"
    },
    {
      name: "tracinglevel",
      options: [
        "verbose",
        "debug",
        "critical",
      ],
      value: "verbose"
    },
  ];

  traceConfigurations: Array<TraceConfiguration> = [
    {
      name: "Surface Flinger",
      defaultCheck: true,
      config: {
        enableConfigs: ["composition","metadata","hwc","tracebuffers"],
        selectionConfigs: [
          {
            name: "sfbuffersize (KB)",
            options: ["4000","8000","16000","32000",],
            value: "4000"
          }
        ]
      }
    },
    {
      name: "Window Manager",
      defaultCheck: true,
      config: {
        enableConfigs: [],
        selectionConfigs: this.wmTraceSelectionConfigs,
      }
    },
    {
      name: "Screen Recording",
    },
    {
      name: "Accessibility",
    },
    {
      name: "Transaction",
    },
    {
      name: "Input Method Clients",
      defaultCheck: true,
    },
    {
      name: "Input Method Service",
      defaultCheck: true,
    },
    {
      name: "Input Method Manager Service",
      defaultCheck: true,
    },
  ];

  states = ProxyState;
  status = this.states.NO_PROXY;

  public devices(): Array<Device> {
    return [
      {authorised: true, id: "1"},
    ];
  }

  public restart() {
    this.status === this.states.START_TRACE;
  }

  public proxySuccess() {
    return this.status === this.states.START_TRACE;
  }

  public resetLastDevice() {
    this.restart();
  }

  public selectDevice(id: string) {
    console.log("selected", id);
  }

  public displayAdbProxyTab() {
    this.isAdbProxy = true;
    console.log("Adb Proxy options?", this.isAdbProxy);
  }

  public displayWebAdbTab() {
    this.isAdbProxy = false;
    console.log("Web ADB options?", !this.isAdbProxy);
  }

  public startTracing() {
    this.startTrace = true;
    console.log("begin tracing");
  }

  public dumpState() {
    this.startDump = true;
    console.log("begin dump");
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
}
