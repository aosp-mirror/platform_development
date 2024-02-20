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
import {TimestampType} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

describe('Perfetto ParserTransitions', () => {
  describe('valid trace', () => {
    let parser: Parser<PropertyTreeNode>;

    beforeAll(async () => {
      jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
      parser = await UnitTestUtils.getPerfettoParser(
        TraceType.TRANSITION,
        'traces/perfetto/shell_transitions_trace.perfetto-trace',
      );
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.TRANSITION);
    });

    it('provides elapsed timestamps', () => {
      const expected = [
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(479602824452n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(480676958445n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(487195167758n),
      ];
      const actual = assertDefined(
        parser.getTimestamps(TimestampType.ELAPSED),
      ).slice(0, 3);
      expect(actual).toEqual(expected);
    });

    it('provides real timestamps', () => {
      const expected = [
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1700573903102738218n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1700573904176872211n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1700573910695081524n),
      ];
      const actual = assertDefined(
        parser.getTimestamps(TimestampType.REAL),
      ).slice(0, 3);
      expect(actual).toEqual(expected);
    });

    it('decodes transition properties', async () => {
      const entry = await parser.getEntry(0, TimestampType.REAL);
      const wmDataNode = assertDefined(entry.getChildByName('wmData'));
      const shellDataNode = assertDefined(entry.getChildByName('shellData'));

      expect(entry.getChildByName('id')?.getValue()).toEqual(32n);
      expect(
        wmDataNode.getChildByName('createTimeNs')?.formattedValue(),
      ).toEqual('2023-11-21T13:38:23.083364560');
      expect(wmDataNode.getChildByName('sendTimeNs')?.formattedValue()).toEqual(
        '2023-11-21T13:38:23.096319557',
      );
      expect(
        wmDataNode.getChildByName('finishTimeNs')?.formattedValue(),
      ).toEqual('2023-11-21T13:38:23.624691628');
      expect(entry.getChildByName('merged')?.getValue()).toBeFalse();
      expect(entry.getChildByName('played')?.getValue()).toBeTrue();
      expect(entry.getChildByName('aborted')?.getValue()).toBeFalse();

      expect(
        assertDefined(
          wmDataNode.getChildByName('startingWindowRemoveTimeNs'),
        ).formattedValue(),
      ).toEqual('2023-11-21T13:38:23.219566424');
      expect(
        assertDefined(
          wmDataNode.getChildByName('startTransactionId'),
        ).formattedValue(),
      ).toEqual('5811090758076');
      expect(
        assertDefined(
          wmDataNode.getChildByName('finishTransactionId'),
        ).formattedValue(),
      ).toEqual('5811090758077');
      expect(
        assertDefined(wmDataNode.getChildByName('type')).formattedValue(),
      ).toEqual('OPEN');

      const targets = assertDefined(
        wmDataNode.getChildByName('targets'),
      ).getAllChildren();
      expect(targets.length).toEqual(2);
      expect(
        assertDefined(targets[0].getChildByName('layerId')).formattedValue(),
      ).toEqual('398');
      expect(
        assertDefined(targets[1].getChildByName('layerId')).formattedValue(),
      ).toEqual('47');
      expect(
        assertDefined(targets[0].getChildByName('mode')).formattedValue(),
      ).toEqual('TO_FRONT');
      expect(
        assertDefined(targets[1].getChildByName('mode')).formattedValue(),
      ).toEqual('TO_BACK');

      expect(
        assertDefined(
          shellDataNode.getChildByName('dispatchTimeNs'),
        ).formattedValue(),
      ).toEqual('2023-11-21T13:38:23.102738218');
      expect(shellDataNode.getChildByName('mergeRequestTime')).toBeUndefined();
      expect(shellDataNode.getChildByName('mergeTime')).toBeUndefined();
      expect(shellDataNode.getChildByName('abortTimeNs')).toBeUndefined();
      expect(shellDataNode.getChildByName('mergeTarget')).toBeUndefined();
      expect(
        assertDefined(shellDataNode.getChildByName('handler')).formattedValue(),
      ).toEqual('com.android.wm.shell.transition.DefaultMixedHandler');
    });

    it('applies timezone info to real timestamps only', async () => {
      const parserWithTimezoneInfo = await UnitTestUtils.getPerfettoParser(
        TraceType.TRANSITION,
        'traces/perfetto/shell_transitions_trace.perfetto-trace',
        true,
      );
      expect(parserWithTimezoneInfo.getTraceType()).toEqual(
        TraceType.TRANSITION,
      );

      expect(
        assertDefined(
          parserWithTimezoneInfo.getTimestamps(TimestampType.ELAPSED),
        )[0],
      ).toEqual(NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(479602824452n));
      expect(
        assertDefined(
          parserWithTimezoneInfo.getTimestamps(TimestampType.REAL),
        )[0],
      ).toEqual(
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1700593703102738218n),
      );

      const entry = await parserWithTimezoneInfo.getEntry(
        0,
        TimestampType.REAL,
      );
      const wmDataNode = assertDefined(entry.getChildByName('wmData'));
      const shellDataNode = assertDefined(entry.getChildByName('shellData'));

      expect(
        wmDataNode.getChildByName('createTimeNs')?.formattedValue(),
      ).toEqual('2023-11-21T19:08:23.083364560');
      expect(wmDataNode.getChildByName('sendTimeNs')?.formattedValue()).toEqual(
        '2023-11-21T19:08:23.096319557',
      );
      expect(
        wmDataNode.getChildByName('finishTimeNs')?.formattedValue(),
      ).toEqual('2023-11-21T19:08:23.624691628');

      expect(
        assertDefined(
          wmDataNode.getChildByName('startingWindowRemoveTimeNs'),
        ).formattedValue(),
      ).toEqual('2023-11-21T19:08:23.219566424');

      expect(
        assertDefined(
          shellDataNode.getChildByName('dispatchTimeNs'),
        ).formattedValue(),
      ).toEqual('2023-11-21T19:08:23.102738218');
    });
  });
});
