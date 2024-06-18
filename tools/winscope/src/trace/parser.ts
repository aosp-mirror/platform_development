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

import {Timestamp, TimestampType} from 'common/time';
import {
  CustomQueryParamTypeMap,
  CustomQueryParserResultTypeMap,
  CustomQueryType,
} from './custom_query';
import {AbsoluteEntryIndex, EntriesRange} from './index_types';
import {TraceType} from './trace_type';

export interface Parser<T> {
  getTraceType(): TraceType;
  getLengthEntries(): number;
  getTimestamps(type: TimestampType): Timestamp[] | undefined;
  getEntry(index: AbsoluteEntryIndex, timestampType: TimestampType): Promise<T>;
  customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange,
    param?: CustomQueryParamTypeMap[Q],
  ): Promise<CustomQueryParserResultTypeMap[Q]>;
  getDescriptors(): string[];
}
