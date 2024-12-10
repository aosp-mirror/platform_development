/*
 * Copyright (C) 2024 The Android Open Source Project
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

import {assertDefined} from 'common/assert_utils';
import {INVALID_TIME_NS, Timestamp} from 'common/time';
import {TimestampConverter} from 'common/timestamp_converter';
import {UserNotifier} from 'common/user_notifier';
import {Analytics} from 'logging/analytics';
import {TraceSearchQueryFailed} from 'messaging/user_warnings';
import {CoarseVersion} from 'trace/coarse_version';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
} from 'trace/custom_query';
import {AbsoluteEntryIndex, EntriesRange} from 'trace/index_types';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {QueryResult} from 'trace_processor/query_result';
import {TraceProcessorFactory} from 'trace_processor/trace_processor_factory';

export class ParserSearch implements Parser<QueryResult> {
  private queryResult?: QueryResult;
  private timestamps: Timestamp[] = [];

  constructor(
    private readonly query: string,
    private timestampConverter: TimestampConverter,
  ) {}

  getCoarseVersion(): CoarseVersion {
    return CoarseVersion.LATEST;
  }

  getTraceType(): TraceType {
    return TraceType.SEARCH;
  }

  getLengthEntries(): number {
    const queryResult = this.validateQueryResult();
    const numRows = queryResult.numRows();
    if (numRows === 0 || !this.hasTimestamps()) {
      return 1;
    }
    return numRows;
  }

  getTimestamps(): Timestamp[] {
    return this.timestamps;
  }

  async getEntry(index: AbsoluteEntryIndex): Promise<QueryResult> {
    return this.validateQueryResult();
  }

  customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange,
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    throw new Error('not implemented');
  }

  getDescriptors(): string[] {
    return [this.query];
  }

  getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  getRealToBootTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  createTimestamps(): void {
    throw new Error('not implemented');
  }

  async parse() {
    const tp = await TraceProcessorFactory.getSingleInstance();
    try {
      this.queryResult = await tp.query(this.query).waitAllRows();
      if (this.hasTimestamps() && this.queryResult.numRows() > 0) {
        for (const it = this.queryResult.iter({}); it.valid(); it.next()) {
          const ns = it.get('ts') as bigint;
          if (ns === INVALID_TIME_NS) {
            this.timestamps.push(this.timestampConverter.makeZeroTimestamp());
          } else {
            this.timestamps.push(
              this.timestampConverter.makeTimestampFromBootTimeNs(ns),
            );
          }
        }
      } else {
        this.timestamps.push(this.timestampConverter.makeZeroTimestamp());
      }
    } catch (e) {
      Analytics.TraceSearch.logQueryFailure();
      UserNotifier.add(
        new TraceSearchQueryFailed((e as Error).message),
      ).notify();
      throw e;
    }
  }

  private hasTimestamps(): boolean {
    return this.queryResult?.columns().includes('ts') ?? false;
  }

  private validateQueryResult(): QueryResult {
    return assertDefined(
      this.queryResult,
      () => 'Attempted to retrieve query result before running search query.',
    );
  }
}
