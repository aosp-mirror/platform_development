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

import {Transition, TransitionsTrace} from 'trace/flickerlib/common';
import {Parser} from 'trace/parser';
import {Timestamp, TimestampType} from 'trace/timestamp';
import {TraceType} from 'trace/trace_type';
import {AbstractTracesParser} from './abstract_traces_parser';

export class TracesParserTransitions extends AbstractTracesParser<Transition> {
  private readonly wmTransitionTrace: Parser<object> | undefined;
  private readonly shellTransitionTrace: Parser<object> | undefined;
  private readonly descriptors: string[];

  constructor(parsers: Array<Parser<object>>) {
    super(parsers);

    const wmTransitionTraces = this.parsers.filter(
      (it) => it.getTraceType() === TraceType.WM_TRANSITION
    );
    if (wmTransitionTraces.length > 0) {
      this.wmTransitionTrace = wmTransitionTraces[0];
    }
    const shellTransitionTraces = this.parsers.filter(
      (it) => it.getTraceType() === TraceType.SHELL_TRANSITION
    );
    if (shellTransitionTraces.length > 0) {
      this.shellTransitionTrace = shellTransitionTraces[0];
    }
    if (this.wmTransitionTrace !== undefined && this.shellTransitionTrace !== undefined) {
      this.descriptors = this.wmTransitionTrace
        .getDescriptors()
        .concat(this.shellTransitionTrace.getDescriptors());
    } else {
      this.descriptors = [];
    }
  }

  override canProvideEntries(): boolean {
    return this.wmTransitionTrace !== undefined && this.shellTransitionTrace !== undefined;
  }

  getLengthEntries(): number {
    return this.getDecodedEntries().length;
  }

  getEntry(index: number, timestampType: TimestampType): Transition {
    return this.getDecodedEntries()[index];
  }

  private decodedEntries: Transition[] | undefined;
  getDecodedEntries(): Transition[] {
    if (this.decodedEntries === undefined) {
      if (this.wmTransitionTrace === undefined) {
        throw new Error('Missing WM Transition trace');
      }

      if (this.shellTransitionTrace === undefined) {
        throw new Error('Missing Shell Transition trace');
      }

      const wmTransitionEntries: Transition[] = [];
      for (let index = 0; index < this.wmTransitionTrace.getLengthEntries(); index++) {
        wmTransitionEntries.push(this.wmTransitionTrace.getEntry(index, TimestampType.REAL));
      }

      const shellTransitionEntries: Transition[] = [];
      for (let index = 0; index < this.shellTransitionTrace.getLengthEntries(); index++) {
        shellTransitionEntries.push(this.shellTransitionTrace.getEntry(index, TimestampType.REAL));
      }

      const transitionsTrace = new TransitionsTrace(
        wmTransitionEntries.concat(shellTransitionEntries)
      );

      this.decodedEntries = transitionsTrace.asCompressed().entries as Transition[];
    }

    return this.decodedEntries;
  }

  override getDescriptors(): string[] {
    return this.descriptors;
  }

  getTraceType(): TraceType {
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
