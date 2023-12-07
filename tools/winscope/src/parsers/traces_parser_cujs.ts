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

import {assertDefined} from 'common/assert_utils';
import {Timestamp, TimestampType} from 'common/time';
import {Cuj, EventLog, Transition} from 'flickerlib/common';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {AbstractTracesParser} from './abstract_traces_parser';

export class TracesParserCujs extends AbstractTracesParser<Transition> {
  private readonly eventLogTrace: Trace<object> | undefined;
  private readonly descriptors: string[];
  private decodedEntries: Cuj[] | undefined;

  constructor(traces: Traces) {
    super();

    const eventlogTrace = traces.getTrace(TraceType.EVENT_LOG);
    if (eventlogTrace !== undefined) {
      this.eventLogTrace = eventlogTrace;
      this.descriptors = this.eventLogTrace.getDescriptors();
    } else {
      this.descriptors = [];
    }
  }

  override async parse() {
    if (this.eventLogTrace === undefined) {
      throw new Error('EventLog trace not defined');
    }

    const eventsPromises = this.eventLogTrace.mapEntry((entry) => entry.getValue());
    const events = await Promise.all(eventsPromises);

    this.decodedEntries = new EventLog(events).cujTrace.entries;

    await this.parseTimestamps();
  }

  getLengthEntries(): number {
    return assertDefined(this.decodedEntries).length;
  }

  getEntry(index: number, timestampType: TimestampType): Promise<Transition> {
    const entry = assertDefined(this.decodedEntries)[index];
    return Promise.resolve(entry);
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
