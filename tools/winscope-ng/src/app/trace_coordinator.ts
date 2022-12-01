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

import {ArrayUtils} from "common/utils/array_utils";
import {Timestamp, TimestampType} from "common/trace/timestamp";
import {TraceType} from "common/trace/trace_type";
import {Parser} from "parsers/parser";
import {ParserError, ParserFactory} from "parsers/parser_factory";
import { Viewer } from "viewers/viewer";
import { ViewerFactory } from "viewers/viewer_factory";
import { LoadedTrace } from "app/loaded_trace";
import { FileUtils } from "common/utils/file_utils";
import { TRACE_INFO } from "app/trace_info";
import { TimelineData, TimestampChangeObserver, Timeline} from "./timeline_data";
import { Inject, Injectable } from "@angular/core";
import { ScreenRecordingTraceEntry } from "common/trace/screen_recording";

@Injectable()
class TraceCoordinator implements TimestampChangeObserver {
  private parsers: Parser[] = [];
  private viewers: Viewer[] = [];

  constructor(@Inject(TimelineData) private timelineData: TimelineData) {
    this.timelineData.registerObserver(this);
  }

  public async setTraces(traces: File[]): Promise<ParserError[]> {
    traces = this.parsers.map(parser => parser.getTrace()).concat(traces);
    let parserErrors: ParserError[];
    [this.parsers, parserErrors] = await new ParserFactory().createParsers(traces);
    this.addAllTracesToTimelineData();
    this.addScreenRecodingTimeMappingToTraceCooordinator();
    return parserErrors;
  }

  public removeTrace(type: TraceType) {
    this.parsers = this.parsers.filter(parser => parser.getTraceType() !== type);
    this.timelineData.removeTimeline(type);
    if (type === TraceType.SCREEN_RECORDING) {
      this.timelineData.removeScreenRecordingData();
    }
  }

  private addAllTracesToTimelineData() {
    const timelines: Timeline[] = this.parsers.map(parser => {
      const timestamps = parser.getTimestamps(this.timestampTypeToUse());
      if (timestamps === undefined) {
        throw Error("Couldn't get timestamps from trace parser.");
      }
      return {traceType: parser.getTraceType(), timestamps: timestamps};
    });

    this.timelineData.setTimelines(timelines);
  }

  private addScreenRecodingTimeMappingToTraceCooordinator() {
    const parser = this.getParserFor(TraceType.SCREEN_RECORDING);
    if (parser === undefined) {
      return;
    }

    const timestampMapping = new Map<Timestamp, number>();
    let videoData: Blob|undefined = undefined;
    for (const timestamp of parser.getTimestamps(this.timestampTypeToUse()) ?? []) {
      const entry = parser.getTraceEntry(timestamp) as ScreenRecordingTraceEntry;
      timestampMapping.set(timestamp, entry.videoTimeSeconds);
      if (videoData === undefined) {
        videoData = entry.videoData;
      }
    }

    if (videoData === undefined) {
      throw Error("No video data available!");
    }

    this.timelineData.setScreenRecordingData(videoData, timestampMapping);
  }

  private timestampTypeToUse() {
    const priorityOrder = [TimestampType.REAL, TimestampType.ELAPSED];
    for (const type of priorityOrder) {
      if (this.parsers.every(it => it.getTimestamps(type) !== undefined)) {
        return type;
      }
    }

    throw Error("No common timestamp type across all traces");
  }

  public createViewers(storage: Storage) {
    const activeTraceTypes = this.parsers.map(parser => parser.getTraceType());
    this.viewers = new ViewerFactory().createViewers(new Set<TraceType>(activeTraceTypes), storage);

    // Make sure to update the viewers active entries as soon as they are created.
    if (this.timelineData.currentTimestamp) {
      this.onCurrentTimestampChanged(this.timelineData.currentTimestamp);
    }
  }

  public getLoadedTraces(): LoadedTrace[] {
    return this.parsers.map((parser: Parser) => {
      const name = (<File>parser.getTrace()).name;
      const type = parser.getTraceType();
      return {name: name, type: type};
    });
  }

  public getParsers(): Parser[] {
    return this.parsers;
  }

  public getViewers(): Viewer[] {
    return this.viewers;
  }

  public findParser(traceType: TraceType): Parser | null {
    const parser = this.parsers.find(parser => parser.getTraceType() === traceType);
    return parser ?? null;
  }

  public onCurrentTimestampChanged(timestamp: Timestamp|undefined) {
    const entries = this.getCurrentTraceEntries(timestamp);
    this.viewers.forEach(viewer => {
      viewer.notifyCurrentTraceEntries(entries);
    });
  }

  private getCurrentTraceEntries(timestamp: Timestamp|undefined): Map<TraceType, any> {
    const traceEntries: Map<TraceType, any> = new Map<TraceType, any>();

    if (!timestamp) {
      return traceEntries;
    }

    this.parsers.forEach(parser => {
      const targetTimestamp = timestamp;
      const entry = parser.getTraceEntry(targetTimestamp);
      let prevEntry = null;

      const parserTimestamps = parser.getTimestamps(timestamp.getType());
      if (parserTimestamps === undefined) {
        throw new Error(`Unexpected timestamp type ${timestamp.getType()}.`
          + ` Not supported by parser for trace type: ${parser.getTraceType()}`);
      }

      const index = ArrayUtils.binarySearchLowerOrEqual(parserTimestamps, targetTimestamp);
      if (index !== undefined && index > 0) {
        prevEntry = parser.getTraceEntry(parserTimestamps[index-1]);
      }

      if (entry !== undefined) {
        traceEntries.set(parser.getTraceType(), [entry, prevEntry]);
      }
    });

    return traceEntries;
  }

  public clearData() {
    this.parsers = [];
    this.viewers = [];
    this.timelineData.clearData();
  }

  public async getUnzippedFiles(files: File[]): Promise<File[]> {
    const unzippedFiles: File[] = [];
    for (let i=0; i<files.length; i++) {
      if (FileUtils.isZipFile(files[i])) {
        const unzippedFile = await FileUtils.unzipFile(files[i]);
        unzippedFiles.push(...unzippedFile);
      } else {
        unzippedFiles.push(files[i]);
      }
    }
    return unzippedFiles;
  }

  public async getTraceForDownload(parser: Parser): Promise<File | null> {
    const trace = parser.getTrace();
    if (trace) {
      const traceType = TRACE_INFO[parser.getTraceType()].name;
      const name = traceType + "/" + FileUtils.removeDirFromFileName(trace.name);
      const blob = await trace.arrayBuffer();
      return new File([blob], name);
    }
    return null;
  }

  public async getAllTracesForDownload(): Promise<File[]> {
    const traces: File[] = [];
    for (let i=0; i < this.parsers.length; i++) {
      const trace = await this.getTraceForDownload(this.parsers[i]);
      if (trace) {
        traces.push(trace);
      }
    }
    return traces;
  }

  public getParserFor(traceType: TraceType): undefined|Parser {
    const matchingParsers = this.getParsers()
      .filter((parser) => parser.getTraceType() === traceType);

    if (matchingParsers.length === 0) {
      return undefined;
    }

    if (matchingParsers.length > 1) {
      throw Error(`Too many matching parsers for trace type ${traceType}. `);
    }

    return matchingParsers[0];
  }
}

export { TraceCoordinator };
