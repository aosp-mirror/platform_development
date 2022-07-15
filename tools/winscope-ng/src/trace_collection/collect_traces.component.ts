import {Component, Input, OnInit } from "@angular/core";
import { ProxyState, proxyClient, ProxyEndpoint } from "./proxy_client";
import { PersistentStore } from "../common/persistent_store";

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
  model: string;
}

@Component({
  selector: "collect-traces",
  template: `
    <mat-card-title>Collect Traces</mat-card-title>
    <mat-card-content>
    <div *ngIf="proxy.state===states.CONNECTING">Connecting...</div>

    <div id="set-up-adb" *ngIf="!adbSuccess()">
      <button mat-raised-button [ngClass]="tabClass(true)" (click)="displayAdbProxyTab()">ADB Proxy</button>
      <button mat-raised-button [ngClass]="tabClass(false)" (click)="displayWebAdbTab()">Web ADB</button>
      <adb-proxy *ngIf="isAdbProxy" [(proxy)]="proxy" (addKey)="onAddKey($event)"></adb-proxy>
      <web-adb *ngIf="!isAdbProxy"></web-adb>
    </div>

    <div id="devices-connecting" *ngIf="proxy.state===states.DEVICES">
      <div> {{ devices().length > 0 ? "Connected devices:" : "No devices detected" }}</div>
        <mat-list class="device-choice">
          <mat-list-item *ngFor="let deviceId of devices()" (click)="selectDevice(deviceId)">
            <mat-icon>
              {{ proxy.devices[deviceId].authorised ? "smartphone" : "screen_lock_portrait" }}
            </mat-icon>
            <span class="md-list-item-text">
              {{ proxy.devices[deviceId].authorised ? proxy.devices[deviceId].model : "unauthorised" }} ({{ deviceId }})
            </span>
          </mat-list-item>
        </mat-list>
    </div>

    <div id="trace-collection-config" *ngIf="proxy.state===states.START_TRACE">
      <div class="device-choice">
          <mat-list class="device-choice">
          <mat-list-item>
              <mat-icon>smartphone</mat-icon>
              <span class="md-list-item-text">{{ selectedDevice().model }} ({{ proxy.selectedDevice }})</span>
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
          [(proxy)]="proxy"
        ></trace-config>
      </div>

      <div class="dump-section">
        <h3>Dump targets:</h3>
        <div class="selection">
          <mat-checkbox
            class="md-primary"
            *ngFor="let dumpKey of objectKeys(DUMPS)"
            [checked]="true">{{DUMPS[dumpKey]}}
          </mat-checkbox>
        </div>
      </div>
    </div>

    <div id="unknown-error" *ngIf="proxy.state===states.ERROR">
      <mat-icon>error</mat-icon>
      <span class="md-subheading">Error:</span>
      <pre>
          {{ errorText }}
      </pre>
      <button mat-raised-button (click)="restart()">Retry</button>
    </div>

    <div id="end-tracing" *ngIf="proxy.state===states.END_TRACE">
      <span class="md-subheading">Tracing...</span>
      <mat-progress-bar md-indeterminate></mat-progress-bar>
      <pre>
          {{ errorText }}
      </pre>
      <button mat-raised-button (click)="endTrace()">End trace</button>
    </div>

    <div id="load-data" *ngIf="proxy.state===states.LOAD_DATA">
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
  proxy: any = null;
  downloadProxyUrl = "https://android.googlesource.com/platform/development/+/master/tools/winscope/adb_proxy/winscope_proxy.py";
  states = ProxyState;
  notConnected = [
    this.states.NO_PROXY,
    this.states.UNAUTH,
    this.states.INVALID_VERSION,
  ];

  constructor() {
    this.proxy = proxyClient;
  }

  @Input()
  store: PersistentStore = new PersistentStore();

  ngOnInit(): void {
    this.proxy.setState(ProxyState.CONNECTING);
    this.proxy.onProxyChange(this.onProxyChange);
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has("token")) {
      this.proxy.proxyKey = urlParams.get("token")!;
    }
    this.proxy.getDevices();
  }

  ngOnDestroy(): void {
    this.proxy.removeOnProxyChange(this.onProxyChange);
  }

  public onAddKey(key: string) {
    this.store.addToStore("adb.proxyKey", key);
    proxyClient.proxyKey = key;
    this.restart();
  }

  public adbSuccess() {
    return !this.notConnected.includes(this.proxy.state);
  }

  public onProxyChange(newState: ProxyState, errorText: string) {
    if (newState === ProxyState.CONNECTING) {
      proxyClient.getDevices();
    }
    if (newState == ProxyState.START_TRACE) {
      proxyClient.call("GET", ProxyEndpoint.CHECK_WAYLAND, this, function(request: any,view:any) {
        try {
          if(request.responseText == "true") {
          //TODO: add this function
          //view.appendOptionalTraces('arc', view.TRACES);
          }
        } catch(err) {
          console.error(err);
          proxyClient.setState(ProxyState.ERROR, request.responseText);
        }
      });
    }
  }

  public devices(): Array<string> {
    return Object.keys(this.proxy.devices);
  }

  public selectedDevice(): Device {
    return this.proxy.devices[this.proxy.selectedDevice];
  }

  public restart() {
    this.proxy.setState(this.states.CONNECTING);
  }

  public resetLastDevice() {
    this.proxy.resetLastDevice();
    this.restart();
  }

  public selectDevice(id: string) {
    this.proxy.selectDevice(id);
  }

  public displayAdbProxyTab() {
    this.isAdbProxy = true;
  }

  public displayWebAdbTab() {
    this.isAdbProxy = false;
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
    this.DYNAMIC_TRACES = this.TRACES["default"];
    proxyClient.call("GET", ProxyEndpoint.CHECK_WAYLAND, this, function(request:any, view:any) {
      try {
        if(request.responseText == "true") {
          //TODO: add this function
          //view.appendOptionalTraces('arc');
        }
      } catch(err) {
        console.error(err);
        proxyClient.setState(ProxyState.ERROR, request.responseText);
      }
    });
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

  TRACES = {
    "default": {
      "window_trace": {
        name: "Window Manager",
      },
      "accessibility_trace": {
        name: "Accessibility",
      },
      "layers_trace": {
        name: "Surface Flinger",
      },
      "transactions": {
        name: "Transaction",
      },
      "proto_log": {
        name: "ProtoLog",
      },
      "screen_recording": {
        name: "Screen Recording",
      },
      "ime_trace_clients": {
        name: "Input Method Clients",
      },
      "ime_trace_service": {
        name: "Input Method Service",
      },
      "ime_trace_managerservice": {
        name: "Input Method Manager Service",
      },
    },
    "arc": {
      "wayland_trace": {
        name: "Wayland",
      },
    },
  };

  DYNAMIC_TRACES: any = null;

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
}