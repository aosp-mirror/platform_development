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
import { Viewer } from "viewers/viewer";
import { ViewerFactory } from "viewers/viewer_factory";
import {TimelineData, TimestampChangeObserver} from "./timeline_data";
import { Inject, Injectable } from "@angular/core";
import {TraceData} from "./trace_data";

@Injectable() //TODO: remove Injectable
class Mediator implements TimestampChangeObserver {
  private traceData = new TraceData();
  private timelineData: TimelineData;
  private viewers: Viewer[] = [];

  constructor(@Inject(TimelineData) timelineData: TimelineData) {
    this.timelineData = timelineData;
    this.timelineData.registerObserver(this);
  }

  public getTraceData(): TraceData {
    return this.traceData;
  }

  public getViewers(): Viewer[] {
    return this.viewers;
  }

  public onTraceDataLoaded(storage: Storage) {
    this.timelineData.clear();
    this.timelineData.setTimelines(this.traceData.getTimelines());

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
    if (this.timelineData.currentTimestamp) {
      this.onCurrentTimestampChanged(this.timelineData.currentTimestamp);
    }
  }
}

export { Mediator };
