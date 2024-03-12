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
import {assertDefined} from 'common/assert_utils';
import {ElapsedTimestamp, RealTimestamp, TimestampType} from 'common/time';
import {Transition, TransitionType} from 'flickerlib/common';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';

describe('Perfetto ParserTransitions', () => {
  describe('valid trace', () => {
    let parser: Parser<Transition>;
    let trace: Trace<Transition>;

    beforeAll(async () => {
      parser = await UnitTestUtils.getPerfettoParser(
        TraceType.TRANSITION,
        'traces/perfetto/shell_transitions_trace.perfetto-trace'
      );
      trace = new TraceBuilder().setType(TraceType.TRANSITION).setParser(parser).build();
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.TRANSITION);
    });

    it('provides elapsed timestamps', () => {
      const expected = [
        new ElapsedTimestamp(479602824452n),
        new ElapsedTimestamp(480676958445n),
        new ElapsedTimestamp(487195167758n),
      ];
      const actual = assertDefined(parser.getTimestamps(TimestampType.ELAPSED)).slice(0, 3);
      expect(actual).toEqual(expected);
    });

    it('provides real timestamps', () => {
      const expected = [
        new RealTimestamp(1700573903102738218n),
        new RealTimestamp(1700573904176872211n),
        new RealTimestamp(1700573910695081524n),
      ];
      const actual = assertDefined(parser.getTimestamps(TimestampType.REAL)).slice(0, 3);
      expect(actual).toEqual(expected);
    });

    it('decodes transition properties', async () => {
      const entry = await parser.getEntry(0, TimestampType.REAL);

      expect(entry.id).toEqual(32);
      expect(entry.createTime.elapsedNanos.toString()).toEqual('479583450794');
      expect(entry.sendTime.elapsedNanos.toString()).toEqual('479596405791');
      expect(entry.abortTime).toEqual(null);
      expect(entry.finishTime.elapsedNanos.toString()).toEqual('480124777862');
      expect(entry.startingWindowRemoveTime.elapsedNanos.toString()).toEqual('479719652658');
      expect(entry.dispatchTime.elapsedNanos.toString()).toEqual('479602824452');
      expect(entry.mergeRequestTime).toEqual(null);
      expect(entry.mergeTime).toEqual(null);
      expect(entry.shellAbortTime).toEqual(null);
      expect(entry.startTransactionId.toString()).toEqual('5811090758076');
      expect(entry.finishTransactionId.toString()).toEqual('5811090758077');
      expect(entry.type).toEqual(TransitionType.OPEN);
      expect(entry.mergeTarget).toEqual(null);
      expect(entry.handler).toEqual('com.android.wm.shell.transition.DefaultMixedHandler');
      expect(entry.merged).toEqual(false);
      expect(entry.played).toEqual(true);
      expect(entry.aborted).toEqual(false);
      expect(entry.changes.length).toEqual(2);
      expect(entry.changes[0].layerId).toEqual(398);
      expect(entry.changes[1].layerId).toEqual(47);
      expect(entry.changes[0].transitMode).toEqual(TransitionType.TO_FRONT);
      expect(entry.changes[1].transitMode).toEqual(TransitionType.TO_BACK);
    });
  });
});
