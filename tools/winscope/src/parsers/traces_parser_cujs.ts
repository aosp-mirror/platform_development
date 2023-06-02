/*
 * Copyright 2023, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Cuj, CujTrace, EventLog, Transition} from 'trace/flickerlib/common';
import {Parser} from 'trace/parser';
import {Timestamp, TimestampType} from 'trace/timestamp';
import {TraceType} from 'trace/trace_type';
import {AbstractTracesParser} from './abstract_traces_parser';
import {ParserEventLog} from './parser_eventlog';

export class TracesParserCujs extends AbstractTracesParser<Transition> {
  private readonly eventLogTrace: ParserEventLog | undefined;
  private readonly descriptors: string[];

  constructor(parsers: Array<Parser<object>>) {
    super(parsers);

    const eventlogTraces = this.parsers.filter((it) => it.getTraceType() === TraceType.EVENT_LOG);
    if (eventlogTraces.length > 0) {
      this.eventLogTrace = eventlogTraces[0] as ParserEventLog;
    }

    if (this.eventLogTrace !== undefined) {
      this.descriptors = this.eventLogTrace.getDescriptors();
    } else {
      this.descriptors = [];
    }
  }

  override canProvideEntries(): boolean {
    return this.eventLogTrace !== undefined;
  }

  getLengthEntries(): number {
    return this.getDecodedEntries().length;
  }

  getEntry(index: number, timestampType: TimestampType): Transition {
    return this.getDecodedEntries()[index];
  }

  private cujTrace: CujTrace | undefined;
  getDecodedEntries(): Cuj[] {
    if (this.eventLogTrace === undefined) {
      throw new Error('eventLogTrace not defined');
    }

    if (this.cujTrace === undefined) {
      const events: Event[] = [];

      for (let i = 0; i < this.eventLogTrace.getLengthEntries(); i++) {
        events.push(this.eventLogTrace.getEntry(i, TimestampType.REAL));
      }

      this.cujTrace = new EventLog(events).cujTrace;
    }

    return this.cujTrace.entries;
  }

  override getDescriptors(): string[] {
    return this.descriptors;
  }

  getTraceType(): TraceType {
    return TraceType.CUJS;
  }

  override getTimestamp(type: TimestampType, transition: Transition): undefined | Timestamp {
    if (type === TimestampType.ELAPSED) {
      return new Timestamp(type, BigInt(transition.timestamp.elapsedNanos.toString()));
    } else if (type === TimestampType.REAL) {
      return new Timestamp(type, BigInt(transition.timestamp.unixNanos.toString()));
    }
    return undefined;
  }
}
