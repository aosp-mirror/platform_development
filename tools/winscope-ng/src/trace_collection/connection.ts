import { proxyRequest, proxyClient, ProxyState, ProxyEndpoint } from "trace_collection/proxy_client";
import { TRACES } from "./trace_collection_utils";

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
    DYNAMIC_TRACES(): any;
    state(): ProxyState;
    onConnectChange(newState: any): any;
    resetLastDevice(): any;
    isDevicesState(): boolean;
    isStartTraceState(): boolean;
    isErrorState(): boolean;
    isEndTraceState(): boolean;
    isLoadDataState(): boolean;
    isConnectingState(): boolean;
    isNoProxy(): boolean;
    isInvalidProxy(): boolean;
    isUnauthProxy(): boolean;
    throwNoTargetsError(): any;
    startTrace(
      reqEnableConfig?: Array<string>,
      reqSelectedSfConfig?: any,
      reqSelectedWmConfig?: any
    ): any;
    DUMPS: any;
    endTrace(): any;
    dumpState(req:Array<string>): any;
    adbData(): any;
}

export class ProxyConnection implements Connection {
  proxy = proxyClient;
  DUMPS: any = {
    "window_dump": {
      name: "Window Manager",
      enabled: true,
    },
    "layers_dump": {
      name: "Surface Flinger",
      enabled: true,
    }
  };
  keep_alive_worker: any = null;
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

  public devices() {
    return Object.keys(this.proxy.devices);
  }

  public adbData() {
    return this.proxy.adbData;
  }

  DYNAMIC_TRACES() {
    return configureTraces.DYNAMIC_TRACES;
  }

  public state() {
    return this.proxy.state;
  }

  public isDevicesState() {
    return this.state() === ProxyState.DEVICES;
  }

  public isStartTraceState() {
    return this.state() === ProxyState.START_TRACE;
  }

  public isErrorState() {
    return this.state() === ProxyState.ERROR;
  }

  public isEndTraceState() {
    return this.state() === ProxyState.END_TRACE;
  }

  public isLoadDataState() {
    return this.state() === ProxyState.LOAD_DATA;
  }

  public isConnectingState() {
    return this.state() === ProxyState.CONNECTING;
  }

  public isNoProxy() {
    return this.state() === ProxyState.NO_PROXY;
  }

  public isInvalidProxy() {
    return this.state() === ProxyState.INVALID_VERSION;
  }

  public isUnauthProxy() {
    return this.state() === ProxyState.UNAUTH;
  }

  public throwNoTargetsError() {
    this.proxy.setState(ProxyState.ERROR, "No targets selected");
  }

  public dataReady() {
    return this.proxy.dataReady;
  }

  public setProxyKey(key: string) {
    this.proxy.proxyKey = key;
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
    this.proxy.store.addToStore("adb.lastDevice", "");
    this.restart();
  }

  public selectDevice(id: string) {
    this.proxy.selectDevice(id);
  }

  public keepAliveTrace(view:any) {
    if (!view.isEndTraceState()) {
      clearInterval(view.keep_alive_worker);
      view.keep_alive_worker = null;
      return;
    }
    proxyRequest.call("GET", `${ProxyEndpoint.STATUS}${view.proxy.selectedDevice}/`, view, function(request:any, newView:any) {
      if (request.responseText !== "True") {
        newView.endTrace();
      } else if (newView.keep_alive_worker === null) {
        newView.keep_alive_worker = setInterval(newView.keepAliveTrace, 1000, newView);
      }
    });
  }

  public startTrace(
    reqEnableConfig: any,
    reqSelectedSfConfig: any,
    reqSelectedWmConfig: any
  ) {
    if (reqEnableConfig) {
      proxyRequest.call("POST", `${ProxyEndpoint.CONFIG_TRACE}${this.proxy.selectedDevice}/`, this, null, null, reqEnableConfig);
    }
    if (reqSelectedSfConfig) {
      proxyRequest.call("POST", `${ProxyEndpoint.SELECTED_SF_CONFIG_TRACE}${this.proxy.selectedDevice}/`, this, null, null, reqSelectedSfConfig);
    }
    if (reqSelectedWmConfig) {
      proxyRequest.call("POST",  `${ProxyEndpoint.SELECTED_WM_CONFIG_TRACE}${this.proxy.selectedDevice}/`, this, null, null, reqSelectedWmConfig);
    }
    proxyClient.setState(ProxyState.END_TRACE);
    proxyRequest.call("POST", `${ProxyEndpoint.START_TRACE}${this.proxy.selectedDevice}/`, this, function(request:any, view:any) {
      view.keepAliveTrace(view);
    }, null, configureTraces.reqTraces);
  }

  public async endTrace() {
    this.proxy.setState(ProxyState.LOAD_DATA);
    await proxyRequest.call("POST", `${ProxyEndpoint.END_TRACE}${this.proxy.selectedDevice}/`, this,
      async function (request:any, view:any) {
        await proxyClient.updateAdbData(configureTraces.reqTraces, 0, "trace", view);
      });
  }

  public dumpState() {
    if (configureTraces.reqDumps.length < 1) {
      this.proxy.setState(ProxyState.ERROR, "No targets selected");
      return;
    }
    this.proxy.setState(ProxyState.LOAD_DATA);
    proxyRequest.call("POST", `${ProxyEndpoint.DUMP}${this.proxy.selectedDevice}/`, this, function(request:any, view:any) {
      view.proxy.updateAdbData(configureTraces.reqDumps, 0, "dump", view);
    }, null, configureTraces.reqDumps);
  }

  public onConnectChange(newState: ProxyState) {
    if (newState === ProxyState.CONNECTING) {
      proxyClient.getDevices();
    }
    if (newState == ProxyState.START_TRACE) {
      configureTraces.setAvailableTraces();
    }
  }
}

class ConfigureTraces {
  DYNAMIC_TRACES = TRACES["default"];
  reqTraces: string[] = [];
  reqDumps: string[] = [];

  setAvailableTraces() {
    proxyRequest.call("GET", ProxyEndpoint.CHECK_WAYLAND, this, proxyRequest.setAvailableTraces);
  }
  appendOptionalTraces(view:any, device_key:string) {
    for(const key in TRACES[device_key]) {
      view.DYNAMIC_TRACES[key] = TRACES[device_key][key];
    }
  }
}
export const configureTraces = new ConfigureTraces();