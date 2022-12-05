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

import {Timeline, ScreenRecordingData} from "./trace_data";
import {Timestamp, TimestampType} from "common/trace/timestamp";
import {TraceType} from "common/trace/trace_type";
import { ArrayUtils } from "common/utils/array_utils";
import { FunctionUtils} from "common/utils/function_utils";

export type TimestampCallbackType = (timestamp: Timestamp|undefined) => void;
export type TimeRange = { from: Timestamp, to: Timestamp }
type TimestampWithIndex = {index: number, timestamp: Timestamp};

export class TimelineData {
  private timelines = new Map<TraceType, Timestamp[]>();
  private timestampType?: TimestampType = undefined;
  private explicitlySetTimestamp?: Timestamp = undefined;
  private explicitlySetSelection?: TimeRange = undefined;
  private screenRecordingData?: ScreenRecordingData = undefined;
  private activeViewTraceTypes: TraceType[] = []; // dependencies of current active view
  private onCurrentTimestampChanged: TimestampCallbackType = FunctionUtils.DO_NOTHING;

  public initialize(timelines: Timeline[]) {
    this.clear();

    const allTimestamps = timelines.flatMap(timeline => timeline.timestamps);
    if (allTimestamps.some(timestamp => timestamp.getType() != allTimestamps[0].getType())) {
      throw Error("Added timeline has inconsistent timestamps.");
    }

    if (allTimestamps.length > 0) {
      this.timestampType = allTimestamps[0].getType();
    }

    timelines.forEach(timeline => {
      this.timelines.set(timeline.traceType, timeline.timestamps);
    });

    this.onCurrentTimestampChanged(this.getCurrentTimestamp());
  }

  setOnCurrentTimestampChangedCallback(callback: TimestampCallbackType) {
    this.onCurrentTimestampChanged = callback;
  }

  getCurrentTimestamp(): Timestamp|undefined {
    if (this.explicitlySetTimestamp !== undefined) {
      return this.explicitlySetTimestamp;
    }
    if (this.getFirstTimestampOfActiveViewTraces() !== undefined) {
      return this.getFirstTimestampOfActiveViewTraces();
    }
    return this.getFirstTimestamp();
  }

  public setCurrentTimestamp(timestamp: Timestamp|undefined) {
    if (!this.hasTimestamps()) {
      console.warn("Attempted to set timestamp on traces with no timestamps/entries...");
      return;
    }

    if (timestamp !== undefined) {
      if (this.timestampType === undefined) {
        throw Error("Timestamp type wasn't set before calling updateCurrentTimestamp");
      }
      if (timestamp.getType() !== this.timestampType) {
        throw Error("Timeline based on different timestamp type");
      }
    }

    this.applyOperationAndNotifyIfCurrentTimestampChanged(() => {
      this.explicitlySetTimestamp = timestamp;
    });
  }


  public setActiveViewTraceTypes(types: TraceType[]) {
    this.applyOperationAndNotifyIfCurrentTimestampChanged(() => {
      this.activeViewTraceTypes = types;
    });
  }

  public getTimestampType(): TimestampType|undefined {
    return this.timestampType;
  }

  public getFullRange(): TimeRange {
    if (!this.hasTimestamps()) {
      throw Error("Trying to get full range when there are no timestamps");
    }
    return {
      from: this.getFirstTimestamp()!,
      to: this.getLastTimestamp()!
    };
  }

  public getSelectionRange(): TimeRange {
    if (this.explicitlySetSelection === undefined) {
      return this.getFullRange();
    } else {
      return this.explicitlySetSelection;
    }
  }

  public setSelectionRange(selection: TimeRange) {
    this.explicitlySetSelection = selection;
  }

  public getTimelines(): Map<TraceType, Timestamp[]>  {
    return this.timelines;
  }

  public setScreenRecordingData(data: ScreenRecordingData) {
    this.screenRecordingData = data;
  }

  public getVideoData(): Blob|undefined {
    return this.screenRecordingData?.video;
  }

  public searchCorrespondingScreenRecordingTimeInSeconds(timestamp: Timestamp): number|undefined {
    const screenRecordingTimestamp = this.searchCorrespondingTimestampFor(TraceType.SCREEN_RECORDING, timestamp)?.timestamp;
    if (screenRecordingTimestamp === undefined) {
      return undefined;
    }

    return this.screenRecordingData?.timestampsMapping.get(screenRecordingTimestamp);
  }

  public hasTimestamps(): boolean {
    return Array.from(this.timelines.values()).some(timestamps => timestamps.length > 0);
  }

  public hasMoreThanOneDistinctTimestamp(): boolean {
    return this.hasTimestamps() && this.getFirstTimestamp() !== this.getLastTimestamp();
  }

  public getCurrentTimestampFor(type: TraceType): Timestamp|undefined {
    return this.searchCorrespondingTimestampFor(type, this.getCurrentTimestamp())?.timestamp;
  }

  public getPreviousTimestampFor(type: TraceType): Timestamp|undefined {
    const currentIndex =
      this.searchCorrespondingTimestampFor(type, this.getCurrentTimestamp())?.index;

    if (currentIndex === undefined) {
      // Only acceptable reason for this to be undefined is if we are before the first entry for this type
      if (this.timelines.get(type)!.length === 0 ||
          this.getCurrentTimestamp()!.getValueNs() < this.timelines.get(type)![0].getValueNs()) {
        return undefined;
      }
      throw Error(`Missing active timestamp for trace type ${type}`);
    }

    const previousIndex = currentIndex - 1;
    if (previousIndex < 0) {
      return undefined;
    }

    return this.timelines.get(type)?.[previousIndex];
  }

  public getNextTimestampFor(type: TraceType): Timestamp|undefined {
    const currentIndex =
      this.searchCorrespondingTimestampFor(type, this.getCurrentTimestamp())?.index ?? -1;

    if (this.timelines.get(type)?.length == 0 ?? true) {
      throw Error(`Missing active timestamp for trace type ${type}`);
    }

    const timestamps = this.timelines.get(type);
    if (timestamps === undefined) {
      throw Error("Timestamps for tracetype not found");
    }
    const nextIndex = currentIndex + 1;
    if (nextIndex >= timestamps.length) {
      return undefined;
    }

    return timestamps[nextIndex];
  }

  public moveToPreviousTimestampFor(type: TraceType) {
    const prevTimestamp = this.getPreviousTimestampFor(type);
    if (prevTimestamp !== undefined) {
      this.setCurrentTimestamp(prevTimestamp);
    }
  }

  public moveToNextTimestampFor(type: TraceType) {
    const nextTimestamp = this.getNextTimestampFor(type);
    if (nextTimestamp !== undefined) {
      this.setCurrentTimestamp(nextTimestamp);
    }
  }

  public clear() {
    this.applyOperationAndNotifyIfCurrentTimestampChanged(() => {
      this.timelines.clear();
      this.explicitlySetTimestamp = undefined;
      this.timestampType = undefined;
      this.explicitlySetSelection = undefined;
      this.screenRecordingData = undefined;
      this.activeViewTraceTypes = [];
    });
  }

  private getFirstTimestamp(): Timestamp|undefined {
    if (!this.hasTimestamps()) {
      return undefined;
    }

    return Array.from(this.timelines.values())
      .map(timestamps => timestamps[0])
      .filter(timestamp => timestamp !== undefined)
      .reduce((prev, current) => prev < current ? prev : current);
  }

  private getLastTimestamp(): Timestamp|undefined {
    if (!this.hasTimestamps()) {
      return undefined;
    }

    return Array.from(this.timelines.values())
      .map(timestamps => timestamps[timestamps.length-1])
      .filter(timestamp => timestamp !== undefined)
      .reduce((prev, current) => prev > current ? prev : current);
  }

  private searchCorrespondingTimestampFor(type: TraceType, timestamp: Timestamp|undefined):
    TimestampWithIndex|undefined {
    if (timestamp === undefined) {
      return undefined;
    }

    if (timestamp.getType() !== this.timestampType) {
      throw Error("Invalid timestamp type");
    }

    const timeline = this.timelines.get(type);
    if (timeline === undefined) {
      throw Error(`No timeline for requested trace type ${type}`);
    }
    const index = ArrayUtils.binarySearchLowerOrEqual(timeline, timestamp);
    if (index === undefined) {
      return undefined;
    }
    return { index, timestamp: timeline[index] };
  }

  private getFirstTimestampOfActiveViewTraces(): Timestamp|undefined {
    if (this.activeViewTraceTypes.length === 0) {
      return undefined;
    }
    const activeTimestamps = this.activeViewTraceTypes
      .map(traceType => this.timelines.get(traceType)!)
      .map(timestamps => timestamps[0])
      .filter(timestamp => timestamp !== undefined)
      .sort();
    if (activeTimestamps.length === 0) {
      return undefined;
    }
    return activeTimestamps[0];
  }

  private applyOperationAndNotifyIfCurrentTimestampChanged(op: () => void) {
    const prevTimestamp = this.getCurrentTimestamp();
    op();
    if (prevTimestamp !== this.getCurrentTimestamp()) {
      this.onCurrentTimestampChanged(this.getCurrentTimestamp());
    }
  }
}
