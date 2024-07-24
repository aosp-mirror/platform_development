/*
 * Copyright (C) 2023 The Android Open Source Project
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
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {ParserMock} from 'trace/parser_mock';
import {TraceType} from 'trace/trace_type';

export class ParserBuilder<T> {
  private type = TraceType.SURFACE_FLINGER;
  private entries?: T[];
  private timestamps?: Timestamp[];
  private customQueryResult = new Map<CustomQueryType, {}>();
  private descriptors = ['file descriptor'];

  setType(type: TraceType): this {
    this.type = type;
    return this;
  }

  setEntries(entries: T[]): this {
    this.entries = entries;
    return this;
  }

  setTimestamps(timestamps: Timestamp[]): this {
    this.timestamps = timestamps;
    return this;
  }

  setCustomQueryResult<Q extends CustomQueryType>(
    type: Q,
    result: CustomQueryParserResultTypeMap[Q],
  ): this {
    this.customQueryResult.set(type, result);
    return this;
  }

  setDescriptors(descriptors: string[]): this {
    this.descriptors = descriptors;
    return this;
  }

  build(): Parser<T> {
    if (!this.timestamps && !this.entries) {
      throw new Error(
        `Either the timestamps or the entries should be specified`,
      );
    }

    if (!this.timestamps) {
      this.timestamps = this.createTimestamps(this.entries as T[]);
    }

    if (!this.entries) {
      this.entries = this.createEntries(this.timestamps);
    }

    if (this.entries.length !== this.timestamps.length) {
      throw new Error(
        'Entries and timestamps arrays must have the same length',
      );
    }

    return new ParserMock(
      this.type,
      this.timestamps,
      this.entries,
      this.customQueryResult,
      this.descriptors,
    );
  }

  private createTimestamps(entries: T[]): Timestamp[] {
    const timestamps = new Array<Timestamp>();
    for (let i = 0; i < entries.length; ++i) {
      timestamps[i] = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(BigInt(i));
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
}
