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

import {Timestamp} from 'common/time';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
} from 'trace/custom_query';
import {FrameMap} from 'trace/frame_map';
import {FrameMapBuilder} from 'trace/frame_map_builder';
import {
  AbsoluteEntryIndex,
  AbsoluteFrameIndex,
  EntriesRange,
} from 'trace/index_types';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {ParserBuilder} from './parser_builder';

export class TraceBuilder<T> {
  private type = TraceType.SURFACE_FLINGER;
  private parser?: Parser<T>;
  private parserCustomQueryResult = new Map<CustomQueryType, {}>();
  private entries?: T[];
  private timestamps?: Timestamp[];
  private frameMap?: FrameMap;
  private frameMapBuilder?: FrameMapBuilder;
  private descriptors: string[] = [];
  private isCorrupted = false;

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

  setParserCustomQueryResult<Q extends CustomQueryType>(
    type: Q,
    result: CustomQueryParserResultTypeMap[Q],
  ): TraceBuilder<T> {
    this.parserCustomQueryResult.set(type, result);
    return this;
  }

  setDescriptors(descriptors: string[]): TraceBuilder<T> {
    this.descriptors = descriptors;
    return this;
  }

  setIsCorrupted(value: boolean): TraceBuilder<T> {
    this.isCorrupted = value;
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
    const trace = new Trace<T>(
      this.type,
      this.parser,
      this.descriptors,
      undefined,
      entriesRange,
    );

    const frameMap = this.getFrameMap();
    if (frameMap) {
      trace.setFrameInfo(frameMap, frameMap.getFullTraceFramesRange());
    }

    return trace;
  }

  private createParser(): Parser<T> {
    const builder = new ParserBuilder<T>()
      .setType(this.type)
      .setIsCorrupted(this.isCorrupted);

    if (this.timestamps) {
      builder.setTimestamps(this.timestamps);
    }

    if (this.entries) {
      builder.setEntries(this.entries);
    }

    if (this.descriptors.length > 0) {
      builder.setDescriptors(this.descriptors);
    }

    this.parserCustomQueryResult?.forEach((result, queryType) => {
      builder.setCustomQueryResult(queryType, result as any);
    });

    return builder.build();
  }

  private getFrameMap(): FrameMap | undefined {
    if (this.frameMap && this.frameMapBuilder) {
      throw new Error(
        `Cannot set a full frame map as well as individual entry's frames. Pick one of the two options.`,
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
