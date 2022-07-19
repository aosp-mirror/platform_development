import { proxyClient, ProxyState, ProxyEndpoint, ProxyClient } from "trace_collection/proxy_client";
import {TRACES, traceConfigurations} from "./trace_collection_utils";

export interface Device {
  authorised: boolean;
  model: string;
}

export interface Connection {
    adbSuccess: () => boolean;
    setProxyKey(key:string): any;
    devices(): Array<string>;
    selectedDevice(): Device;
    restart(): any;
    selectDevice(id:string): any;
    DYNAMIC_TRACES: any;
    state(): ProxyState;
    onConnectChange(newState: any): any;
    setAvailableTraces(): any;
    resetLastDevice(): any;
}

export class ProxyConnection implements Connection {
  DYNAMIC_TRACES: any;
  proxy = proxyClient;

  public state() {
    return this.proxy.state;
  }

  public devices() {
    return Object.keys(this.proxy.devices);
  }
  notConnected = [
    ProxyState.NO_PROXY,
    ProxyState.UNAUTH,
    ProxyState.INVALID_VERSION,
  ];

  constructor() {
    this.proxy.setState(ProxyState.CONNECTING);
    this.proxy.onProxyChange(this.onConnectChange);
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has("token")) {
      this.proxy.proxyKey = urlParams.get("token")!;
    }
    this.proxy.getDevices();
  }

  public onConnectChange(newState: ProxyState) {
    if (newState === ProxyState.CONNECTING) {
      proxyClient.getDevices();
    }
    if (newState == ProxyState.START_TRACE) {
      proxyClient.call("GET", ProxyEndpoint.CHECK_WAYLAND, this, function(request: any,view:any) {
        try {
          if(request.responseText == "true") {
          //view.appendOptionalTraces('arc', view.TRACES);
          }
        } catch(err) {
          console.error(err);
          proxyClient.setState(ProxyState.ERROR, request.responseText);
        }
      });
    }
  }

  public setProxyKey(key: string) {
    proxyClient.proxyKey = key;
    this.restart();
  }

  public adbSuccess() {
    return !this.notConnected.includes(this.proxy.state);
  }

  public selectedDevice(): Device {
    return this.proxy.devices[this.proxy.selectedDevice];
  }

  public restart() {
    this.proxy.setState(ProxyState.CONNECTING);
  }

  public resetLastDevice() {
    this.proxy.resetLastDevice();
    this.restart();
  }

  public selectDevice(id: string) {
    this.proxy.selectDevice(id);
  }

  public setAvailableTraces() {
    this.DYNAMIC_TRACES = TRACES["default"];
    proxyClient.call("GET", ProxyEndpoint.CHECK_WAYLAND, this, function(request:any, view:any) {
      try {
        if(request.responseText == "true") {
          //view.appendOptionalTraces('arc');
        }
      } catch(err) {
        console.error(err);
        proxyClient.setState(ProxyState.ERROR, request.responseText);
      }
    });
  }
}
