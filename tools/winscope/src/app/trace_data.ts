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

import {ScreenRecordingTraceEntry} from 'common/trace/screen_recording';
import {Timestamp, TimestampType} from 'common/trace/timestamp';
import {Trace, TraceFile} from 'common/trace/trace';
import {TraceType} from 'common/trace/trace_type';
import {ArrayUtils} from 'common/utils/array_utils';
import {FunctionUtils, OnProgressUpdateType} from 'common/utils/function_utils';
import {Parser} from 'parsers/parser';
import {ParserError, ParserFactory} from 'parsers/parser_factory';

interface Timeline {
  traceType: TraceType;
  timestamps: Timestamp[];
}

class TraceData {
  private parserFactory = new ParserFactory();
  private parsers: Parser[] = [];
  private commonTimestampType?: TimestampType;

  public async loadTraces(
    traceFiles: TraceFile[],
    onLoadProgressUpdate: OnProgressUpdateType = FunctionUtils.DO_NOTHING
  ): Promise<ParserError[]> {
    const [parsers, parserErrors] = await this.parserFactory.createParsers(
      traceFiles,
      onLoadProgressUpdate
    );
    this.parsers = parsers;
    return parserErrors;
  }

  public removeTrace(type: TraceType) {
    this.parsers = this.parsers.filter((parser) => parser.getTraceType() !== type);
  }

  public getLoadedTraces(): Trace[] {
    return this.parsers.map((parser: Parser) => parser.getTrace());
  }

  public getTraceEntries(timestamp: Timestamp | undefined): Map<TraceType, any> {
    const traceEntries: Map<TraceType, any> = new Map<TraceType, any>();

    if (!timestamp) {
      return traceEntries;
    }

    this.parsers.forEach((parser) => {
      const targetTimestamp = timestamp;
      const entry = parser.getTraceEntry(targetTimestamp);
      let prevEntry = null;

      const parserTimestamps = parser.getTimestamps(timestamp.getType());
      if (parserTimestamps === undefined) {
        throw new Error(
          `Unexpected timestamp type ${timestamp.getType()}.` +
            ` Not supported by parser for trace type: ${parser.getTraceType()}`
        );
      }

      const index = ArrayUtils.binarySearchLowerOrEqual(parserTimestamps, targetTimestamp);
      if (index !== undefined && index > 0) {
        prevEntry = parser.getTraceEntry(parserTimestamps[index - 1]);
      }

      if (entry !== undefined) {
        traceEntries.set(parser.getTraceType(), [entry, prevEntry]);
      }
    });

    return traceEntries;
  }

  public getTimelines(): Timeline[] {
    const timelines = this.parsers.map((parser): Timeline => {
      const timestamps = parser.getTimestamps(this.getCommonTimestampType());
      if (timestamps === undefined) {
        throw Error('Failed to get timestamps from parser');
      }
      return {traceType: parser.getTraceType(), timestamps: timestamps};
    });

    return timelines;
  }

  public getScreenRecordingVideo(): undefined | Blob {
    const parser = this.parsers.find(
      (parser) => parser.getTraceType() === TraceType.SCREEN_RECORDING
    );
    if (!parser) {
      return undefined;
    }

    const timestamps = parser.getTimestamps(this.getCommonTimestampType());
    if (!timestamps || timestamps.length === 0) {
      return undefined;
    }

    return (parser.getTraceEntry(timestamps[0]) as ScreenRecordingTraceEntry)?.videoData;
  }

  public clear() {
    this.parserFactory = new ParserFactory();
    this.parsers = [];
    this.commonTimestampType = undefined;
  }

  private getCommonTimestampType(): TimestampType {
    if (this.commonTimestampType !== undefined) {
      return this.commonTimestampType;
    }

    const priorityOrder = [TimestampType.REAL, TimestampType.ELAPSED];
    for (const type of priorityOrder) {
      if (this.parsers.every((it) => it.getTimestamps(type) !== undefined)) {
        this.commonTimestampType = type;
        return this.commonTimestampType;
      }
    }

    throw Error('Failed to find common timestamp type across all traces');
  }
}

export {Timeline, TraceData};
