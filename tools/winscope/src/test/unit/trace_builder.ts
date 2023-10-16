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

import {FrameMap} from 'trace/frame_map';
import {FrameMapBuilder} from 'trace/frame_map_builder';
import {AbsoluteEntryIndex, AbsoluteFrameIndex, EntriesRange} from 'trace/index_types';
import {Parser} from 'trace/parser';
import {ParserMock} from 'trace/parser_mock';
import {Timestamp, TimestampType} from 'trace/timestamp';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';

export class TraceBuilder<T> {
  private type = TraceType.SURFACE_FLINGER;
  private parser?: Parser<T>;
  private entries?: T[];
  private timestamps?: Timestamp[];
  private timestampType = TimestampType.REAL;
  private frameMap?: FrameMap;
  private frameMapBuilder?: FrameMapBuilder;
  private descriptors: string[] = [];

  setType(type: TraceType): TraceBuilder<T> {
    this.type = type;
    return this;
  }

  setParser(parser: Parser<T>): TraceBuilder<T> {
    this.parser = parser;
    return this;
  }

  setEntries(entries: T[]): TraceBuilder<T> {
    this.entries = entries;
    return this;
  }

  setTimestamps(timestamps: Timestamp[]): TraceBuilder<T> {
    this.timestamps = timestamps;
    return this;
  }

  setTimestampType(type: TimestampType): TraceBuilder<T> {
    this.timestampType = type;
    return this;
  }

  setFrameMap(frameMap?: FrameMap): TraceBuilder<T> {
    this.frameMap = frameMap;
    return this;
  }

  setFrame(entry: AbsoluteEntryIndex, frame: AbsoluteFrameIndex) {
    if (!this.entries) {
      throw new Error(`Can't set frames before specifying the entries`);
    }
    if (!this.frameMapBuilder) {
      this.frameMapBuilder = new FrameMapBuilder(this.entries.length, 1000);
    }
    this.frameMapBuilder.setFrames(entry, {start: frame, end: frame + 1});
    return this;
  }

  setDescriptors(descriptors: string[]): TraceBuilder<T> {
    this.descriptors = descriptors;
    return this;
  }

  build(): Trace<T> {
    if (!this.parser) {
      this.parser = this.createParser();
    }

    const entriesRange: EntriesRange = {
      start: 0,
      end: this.parser.getLengthEntries(),
    };
    const trace = Trace.newInitializedTrace<T>(
      this.type,
      this.parser,
      this.descriptors,
      this.timestampType,
      entriesRange
    );

    const frameMap = this.getFrameMap();
    if (frameMap) {
      trace.setFrameInfo(frameMap, frameMap.getFullTraceFramesRange());
    }

    return trace;
  }

  private createParser(): Parser<T> {
    if (!this.timestamps && !this.entries) {
      throw new Error(`Either the timestamps or the entries should be specified`);
    }

    if (!this.timestamps) {
      this.timestamps = this.createTimestamps(this.entries as T[]);
    }

    if (!this.entries) {
      this.entries = this.createEntries(this.timestamps);
    }

    if (this.entries.length !== this.timestamps.length) {
      throw new Error('Entries and timestamps arrays must have the same length');
    }

    return new ParserMock(this.timestamps, this.entries);
  }

  private createTimestamps(entries: T[]): Timestamp[] {
    const timestamps = new Array<Timestamp>();
    for (let i = 0; i < entries.length; ++i) {
      timestamps[i] = new Timestamp(TimestampType.REAL, BigInt(i));
    }
    return timestamps;
  }

  private createEntries(timestamps: Timestamp[]): T[] {
    const entries = new Array<T>();
    for (let i = 0; i < timestamps.length; ++i) {
      entries.push(`entry-${i}` as unknown as T);
    }
    return entries;
  }

  private getFrameMap(): FrameMap | undefined {
    if (this.frameMap && this.frameMapBuilder) {
      throw new Error(
        `Cannot set a full frame map as well as individual entry's frames. Pick one of the two options.`
      );
    }
    if (this.frameMap) {
      return this.frameMap;
    }
    if (this.frameMapBuilder) {
      return this.frameMapBuilder.build();
    }
    return undefined;
  }
}
