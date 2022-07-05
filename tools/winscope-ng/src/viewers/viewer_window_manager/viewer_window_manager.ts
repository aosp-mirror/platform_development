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
import {TraceTypeId} from "common/trace/type_id";
import {Viewer} from 'viewers/viewer';

class ViewerWindowManager implements Viewer {
  public static readonly DEPENDENCIES: TraceTypeId[] = [TraceTypeId.WINDOW_MANAGER];
  private view: HTMLElement;

  constructor() {
    this.view = document.createElement("viewer-window-manager");

    //this.view.setAttribute("input-value", new Date() as unknown as string)
    (this.view as any).inputValue = new Date();

    this.view.addEventListener("outputEvent", () => console.log("Viewer component generated event!"));
  }

  public notifyCurrentTraceEntries(entries: Map<TraceTypeId, any>): void {
  }

  public getView(): HTMLElement {
    return this.view;
  }
}

export {ViewerWindowManager};
