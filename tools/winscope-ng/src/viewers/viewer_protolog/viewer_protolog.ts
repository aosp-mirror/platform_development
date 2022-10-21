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
import {View, Viewer, ViewType} from "viewers/viewer";
import {Presenter} from "./presenter";
import {Events} from "./events";
import {UiData} from "./ui_data";

class ViewerProtoLog implements Viewer {
  constructor() {
    this.htmlElement = document.createElement("viewer-protolog");

    this.presenter = new Presenter((data: UiData) => {
      (this.htmlElement as any).inputData = data;
    });

    this.htmlElement.addEventListener(Events.LogLevelsFilterChanged, (event) => {
      return this.presenter.onLogLevelsFilterChanged((event as CustomEvent).detail);
    });
    this.htmlElement.addEventListener(Events.TagsFilterChanged, (event) => {
      return this.presenter.onTagsFilterChanged((event as CustomEvent).detail);
    });
    this.htmlElement.addEventListener(Events.SourceFilesFilterChanged, (event) => {
      return this.presenter.onSourceFilesFilterChanged((event as CustomEvent).detail);
    });
    this.htmlElement.addEventListener(Events.SearchStringFilterChanged, (event) => {
      return this.presenter.onSearchStringFilterChanged((event as CustomEvent).detail);
    });
  }

  public notifyCurrentTraceEntries(entries: Map<TraceType, any>): void {
    this.presenter.notifyCurrentTraceEntries(entries);
  }

  public getViews(): View[] {
    return [new View(ViewType.TAB, this.htmlElement, "ProtoLog")];
  }

  public getDependencies(): TraceType[] {
    return ViewerProtoLog.DEPENDENCIES;
  }

  public static readonly DEPENDENCIES: TraceType[] = [TraceType.PROTO_LOG];
  private htmlElement: HTMLElement;
  private presenter: Presenter;
}

export {ViewerProtoLog};
