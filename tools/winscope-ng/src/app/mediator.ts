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

import {Timestamp} from "common/trace/timestamp";
import {TraceType} from "common/trace/trace_type";
import {FunctionUtils} from "common/utils/function_utils";
import { Viewer } from "viewers/viewer";
import { ViewerFactory } from "viewers/viewer_factory";
import {TimelineData} from "./timeline_data";
import {TraceData} from "./trace_data";

type CurrentTimestampChangedCallback = (timestamp: Timestamp|undefined) => void;

class Mediator {
  private traceData: TraceData;
  private timelineData: TimelineData;
  private viewers: Viewer[] = [];
  private notifyCurrentTimestampChangedToTimelineComponent: CurrentTimestampChangedCallback =
    FunctionUtils.DO_NOTHING;

  constructor(traceData: TraceData, timelineData: TimelineData) {
    this.traceData = traceData;
    this.timelineData = timelineData;
    this.timelineData.setOnCurrentTimestampChangedCallback(timestamp => {
      this.onCurrentTimestampChanged(timestamp);
    });
  }

  public setNotifyCurrentTimestampChangedToTimelineComponentCallback(callback: CurrentTimestampChangedCallback) {
    this.notifyCurrentTimestampChangedToTimelineComponent = callback;
  }

  public getViewers(): Viewer[] {
    return this.viewers;
  }

  public onTraceDataLoaded(storage: Storage) {
    this.timelineData.initialize(this.traceData.getTimelines());

    const screenRecordingData = this.traceData.getScreenRecordingData();
    if (screenRecordingData) {
      this.timelineData.setScreenRecordingData(screenRecordingData);
    }

    this.createViewers(storage);
  }

  public onCurrentTimestampChanged(timestamp: Timestamp|undefined) {
    const entries = this.traceData.getTraceEntries(timestamp);
    this.viewers.forEach(viewer => {
      viewer.notifyCurrentTraceEntries(entries);
    });

    this.notifyCurrentTimestampChangedToTimelineComponent(timestamp);
  }

  public clearData() {
    this.traceData.clear();
    this.timelineData.clear();
    this.viewers = [];
  }

  private createViewers(storage: Storage) {
    const traceTypes = this.traceData.getLoadedTraces().map(trace => trace.type);
    this.viewers = new ViewerFactory().createViewers(new Set<TraceType>(traceTypes), storage);

    // Make sure to update the viewers active entries as soon as they are created.
    if (this.timelineData.getCurrentTimestamp()) {
      this.onCurrentTimestampChanged(this.timelineData.getCurrentTimestamp());
    }
  }
}

export { Mediator };
