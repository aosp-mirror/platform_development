/*
 * Copyright 2022, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { TraceConfigurationMap, TRACES } from "./trace_collection_utils";
import {
  proxyRequest,
  ProxyEndpoint
} from "trace_collection/proxy_client";
import { PersistentStoreObject } from "common/utils/persistent_store_object";

export class SetTraces {
  private tracingConfig = PersistentStoreObject.new<TraceConfigurationMap>("TracingSettings", TRACES["default"]);
  reqTraces: string[] = [];
  reqDumps: string[] = [];
  dataReady = false;
  dumpError = false;

  private dumpConfig: TraceConfigurationMap = PersistentStoreObject.new<TraceConfigurationMap>("DumpSettings", {
    "window_dump": {
      name: "Window Manager",
      isTraceCollection: undefined,
      run: true,
      config: undefined
    },
    "layers_dump": {
      name: "Surface Flinger",
      isTraceCollection: undefined,
      run: true,
      config: undefined
    }
  });

  // TODO: Might make sense to split this into two function calls
  public fetchAndSetTracingConfigForAvailableTraces() {
    proxyRequest.call("GET", ProxyEndpoint.CHECK_WAYLAND, (request:XMLHttpRequest) => {
      const availableTracesConfig = TRACES["default"];
      if(request.responseText == "true") {
        Object.assign(availableTracesConfig, TRACES["arc"]);
      }
      this.setTracingConfig(availableTracesConfig);
    });
  }

  public getTracingConfig(): TraceConfigurationMap {
    return this.tracingConfig;
  }

  private setTracingConfig(traceConfig: TraceConfigurationMap) {
    this.tracingConfig = PersistentStoreObject.new<TraceConfigurationMap>("TraceConfiguration", traceConfig);
  }

  public getDumpConfig(): TraceConfigurationMap {
    return this.dumpConfig;
  }
}
export const setTraces = new SetTraces();
