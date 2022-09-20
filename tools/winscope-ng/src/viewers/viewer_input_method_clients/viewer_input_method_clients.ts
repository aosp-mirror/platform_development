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
import {TraceType} from "common/trace/trace_type";
import { PresenterInputMethodClients } from "./presenter_input_method_clients";
import { ViewerInputMethod } from "viewers/common/viewer_input_method";

class ViewerInputMethodClients extends ViewerInputMethod {
  public getTitle(): string {
    return "Input Method Clients";
  }

  public getDependencies(): TraceType[] {
    return ViewerInputMethodClients.DEPENDENCIES;
  }

  protected initialisePresenter() {
    return new PresenterInputMethodClients(this.imeUiCallback, this.getDependencies());
  }

  public static readonly DEPENDENCIES: TraceType[] = [TraceType.INPUT_METHOD_CLIENTS];
}

export {ViewerInputMethodClients};
