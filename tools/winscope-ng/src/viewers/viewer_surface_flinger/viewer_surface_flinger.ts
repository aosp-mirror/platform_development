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
import {Viewer} from "viewers/viewer";
import {Presenter} from "./presenter";
import {UiData} from "./ui_data";

class ViewerSurfaceFlinger implements Viewer {
  constructor() {
    this.view = document.createElement("viewer-surface-flinger");
    this.presenter = new Presenter((uiData: UiData) => {
      (this.view as any).inputData = uiData;
    });
    this.view.addEventListener("highlightedChange", (event) => this.presenter.updateHighlightedRect((event as CustomEvent)));
  }

  public notifyCurrentTraceEntries(entries: Map<TraceType, any>): void {
    this.presenter.notifyCurrentTraceEntries(entries);
  }

  public getView(): HTMLElement {
    return this.view;
  }

  public getTitle(): string {
    return "Surface Flinger";
  }

  public getDependencies(): TraceType[] {
    return ViewerSurfaceFlinger.DEPENDENCIES;
  }

  public static readonly DEPENDENCIES: TraceType[] = [TraceType.SURFACE_FLINGER];
  private view: HTMLElement;
  private presenter: Presenter;
}

export {ViewerSurfaceFlinger};
