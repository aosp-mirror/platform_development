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

import {Event, EventLogParser} from 'trace/flickerlib/common';
import {RealTimestamp, Timestamp, TimestampType} from 'trace/timestamp';
import {TraceType} from 'trace/trace_type';
import {AbstractParser} from './abstract_parser';

class ParserEventLog extends AbstractParser<Event> {
  override getTraceType(): TraceType {
    return TraceType.EVENT_LOG;
  }

  override getMagicNumber(): number[] {
    return ParserEventLog.MAGIC_NUMBER;
  }

  override decodeTrace(buffer: Uint8Array): Event[] {
    const eventLog = EventLogParser.prototype.parse(buffer);

    return eventLog.entries;
  }

  override getTimestamp(type: TimestampType, entry: any): undefined | Timestamp {
    if (type === TimestampType.REAL) {
      return new RealTimestamp(BigInt(entry.timestamp.unixNanos));
    }
    return undefined;
  }

  override processDecodedEntry(index: number, timestampType: TimestampType, entry: Event): Event {
    return entry;
  }

  private static readonly MAGIC_NUMBER: number[] = Array.from(new TextEncoder().encode('EventLog'));
}

export {ParserEventLog};
