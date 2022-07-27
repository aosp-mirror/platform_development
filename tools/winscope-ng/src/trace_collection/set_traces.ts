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

export class SetTraces {
  DYNAMIC_TRACES = TRACES["default"];
  reqTraces: string[] = [];
  reqDumps: string[] = [];
  dataReady = false;
  dumpError = false;

  DUMPS: TraceConfigurationMap = {
    "window_dump": {
      name: "Window Manager",
      run: true,
    },
    "layers_dump": {
      name: "Surface Flinger",
      run: true,
    }
  };

  setAvailableTraces() {
    proxyRequest.call("GET", ProxyEndpoint.CHECK_WAYLAND, this, proxyRequest.onSuccessSetAvailableTraces);
  }
  appendOptionalTraces(view:any, device_key:string) {
    for(const key in TRACES[device_key]) {
      view.DYNAMIC_TRACES[key] = TRACES[device_key][key];
    }
  }
}
export const setTraces = new SetTraces();
