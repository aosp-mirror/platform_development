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
import {Transition, TransitionsTrace} from 'flickerlib/common';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {AbstractTracesParser} from './abstract_traces_parser';

export class TracesParserTransitions extends AbstractTracesParser<Transition> {
  private readonly wmTransitionTrace: Trace<object> | undefined;
  private readonly shellTransitionTrace: Trace<object> | undefined;
  private readonly descriptors: string[];
  private decodedEntries: Transition[] | undefined;

  constructor(traces: Traces) {
    super();
    const wmTransitionTrace = traces.getTrace(TraceType.WM_TRANSITION);
    const shellTransitionTrace = traces.getTrace(TraceType.SHELL_TRANSITION);
    if (wmTransitionTrace && shellTransitionTrace) {
      this.wmTransitionTrace = wmTransitionTrace;
      this.shellTransitionTrace = shellTransitionTrace;
      this.descriptors = this.wmTransitionTrace
        .getDescriptors()
        .concat(this.shellTransitionTrace.getDescriptors());
    } else {
      this.descriptors = [];
    }
  }

  override async parse() {
    if (this.wmTransitionTrace === undefined) {
      throw new Error('Missing WM Transition trace');
    }

    if (this.shellTransitionTrace === undefined) {
      throw new Error('Missing Shell Transition trace');
    }

    const wmTransitionEntries: Transition[] = await Promise.all(
      this.wmTransitionTrace.mapEntry((entry) => entry.getValue())
    );

    const shellTransitionEntries: Transition[] = await Promise.all(
      this.shellTransitionTrace.mapEntry((entry) => entry.getValue())
    );

    const transitionsTrace = new TransitionsTrace(
      wmTransitionEntries.concat(shellTransitionEntries)
    );

    this.decodedEntries = transitionsTrace.asCompressed().entries as Transition[];

    await this.parseTimestamps();
  }

  override getLengthEntries(): number {
    return assertDefined(this.decodedEntries).length;
  }

  override getEntry(index: number, timestampType: TimestampType): Promise<Transition> {
    const entry = assertDefined(this.decodedEntries)[index];
    return Promise.resolve(entry);
  }

  override getDescriptors(): string[] {
    return this.descriptors;
  }

  override getTraceType(): TraceType {
    return TraceType.TRANSITION;
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
