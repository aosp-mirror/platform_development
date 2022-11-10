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
import {ScreenRecordingTraceEntry} from "common/trace/screen_recording";

class ViewerScreenRecording implements Viewer {
  constructor() {
    this.htmlElement = document.createElement("viewer-screen-recording");
  }

  public notifyCurrentTraceEntries(entries: Map<TraceType, any>): void {
    const entry: undefined | ScreenRecordingTraceEntry = entries.get(TraceType.SCREEN_RECORDING)
      ? entries.get(TraceType.SCREEN_RECORDING)[0]
      : undefined;

    (<any>this.htmlElement).currentTraceEntry = entry;
  }

  public getViews(): View[] {
    return [new View(ViewType.OVERLAY, this.getDependencies(), this.htmlElement, "ScreenRecording")];
  }

  public getDependencies(): TraceType[] {
    return ViewerScreenRecording.DEPENDENCIES;
  }

  public static readonly DEPENDENCIES: TraceType[] = [TraceType.SCREEN_RECORDING];
  private htmlElement: HTMLElement;
}

export {ViewerScreenRecording};
