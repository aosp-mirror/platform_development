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
import {
  TimestampConverterUtils,
  timestampEqualityTester,
} from 'common/time/test_utils';
import {getPerfettoParser} from 'test/unit/fixture_utils';
import {CoarseVersion} from 'trace/coarse_version';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

describe('PerfettoParserTransitions', () => {
  describe('valid trace', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      jasmine.addCustomEqualityTester(timestampEqualityTester);
      parser = await getPerfettoParser(
        TraceType.TRANSITION,
        'traces/perfetto/shell_transitions_trace.perfetto-trace',
      );
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.TRANSITION);
    });

    it('has expected coarse version', () => {
      expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LATEST);
    });

    it('provides timestamps', () => {
      const expected = [
        TimestampConverterUtils.makeRealTimestamp(1700573425448299306n),
        TimestampConverterUtils.makeRealTimestamp(1700573426522433299n),
        TimestampConverterUtils.makeRealTimestamp(1700573433040642612n),
        TimestampConverterUtils.makeRealTimestamp(1700573433279358351n),
      ];
      const actual = assertDefined(parser.getTimestamps());
      expect(actual).toEqual(expected);
    });

    it('extracts eager properties', async () => {
      const entry = await parser.getEntry(0);

      expect(entry.getEagerPropertyByName('transitionId')?.getValue()).toEqual(
        32n,
      );
      expect(
        entry.getEagerPropertyByName('transitionType')?.formattedValue(),
      ).toEqual('OPEN');

      expect(
        entry.getEagerPropertyByName('sendTimeNs')?.formattedValue(),
      ).toEqual('2023-11-21, 13:30:25.442');
      expect(
        entry.getEagerPropertyByName('dispatchTimeNs')?.formattedValue(),
      ).toEqual('2023-11-21, 13:30:25.448');
      expect(
        entry.getEagerPropertyByName('durationNs')?.formattedValue(),
      ).toEqual('528 ms');

      const layerParticipants = assertDefined(
        entry.getEagerPropertyByName('layers'),
      ).getAllChildren();
      expect(layerParticipants.length).toEqual(2);
      expect(layerParticipants[0].getValue()).toEqual(47n);
      expect(layerParticipants[1].getValue()).toEqual(398n);

      const windowParticipants = assertDefined(
        entry.getEagerPropertyByName('windows'),
      ).getAllChildren();
      expect(windowParticipants.length).toEqual(2);
      expect(windowParticipants[0].getValue()).toEqual(159077656n);
      expect(windowParticipants[1].getValue()).toEqual(193491296n);

      expect(entry.getEagerPropertyByName('handler')?.formattedValue()).toEqual(
        'com.android.wm.shell.transition.DefaultMixedHandler',
      );
      expect(entry.getEagerPropertyByName('status')?.formattedValue()).toEqual(
        'PLAYED',
      );

      const entryWithFlags = await parser.getEntry(1);
      expect(
        entryWithFlags.getEagerPropertyByName('flags')?.formattedValue(),
      ).toEqual('TRANSIT_FLAG_IS_RECENTS');
    });

    it('decodes lazy transition properties', async () => {
      const entry = await parser.getEntry(0);

      const properties = await entry.getAllProperties();

      expect(properties.getChildByName('id')?.getValue()).toEqual(32);
      expect(
        properties.getChildByName('createTimeNs')?.formattedValue(),
      ).toEqual('2023-11-21, 13:30:25.429');
      expect(properties.getChildByName('sendTimeNs')?.formattedValue()).toEqual(
        '2023-11-21, 13:30:25.442',
      );
      expect(
        properties.getChildByName('finishTimeNs')?.formattedValue(),
      ).toEqual('2023-11-21, 13:30:25.970');
      expect(entry.getEagerPropertyByName('status')?.getValue()).toEqual(
        'played',
      );

      expect(
        assertDefined(
          properties.getChildByName('startingWindowRemoveTimeNs'),
        ).formattedValue(),
      ).toEqual('2023-11-21, 13:30:25.565');
      expect(
        assertDefined(
          properties.getChildByName('startTransactionId'),
        ).formattedValue(),
      ).toEqual('5811090758076');
      expect(
        assertDefined(
          properties.getChildByName('finishTransactionId'),
        ).formattedValue(),
      ).toEqual('5811090758077');
      expect(
        assertDefined(properties.getChildByName('type')).formattedValue(),
      ).toEqual('OPEN');

      const targets = assertDefined(
        properties.getChildByName('targets'),
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
        assertDefined(targets[0].getChildByName('flags')).formattedValue(),
      ).toEqual('FLAG_MOVED_TO_TOP');
      expect(
        assertDefined(targets[1].getChildByName('flags')).formattedValue(),
      ).toEqual('FLAG_SHOW_WALLPAPER');

      expect(
        assertDefined(
          properties.getChildByName('dispatchTimeNs'),
        ).formattedValue(),
      ).toEqual('2023-11-21, 13:30:25.448');
      expect(properties.getChildByName('mergeRequestTime')).toBeUndefined();
      expect(properties.getChildByName('mergeTime')).toBeUndefined();
      expect(properties.getChildByName('shellAbortTimeNs')).toBeUndefined();
      expect(properties.getChildByName('mergeTarget')).toBeUndefined();
      expect(
        assertDefined(properties.getChildByName('handler')).formattedValue(),
      ).toEqual('com.android.wm.shell.transition.DefaultMixedHandler');

      const entryWithFlags = await parser.getEntry(1);
      expect(
        assertDefined(
          (await entryWithFlags.getAllProperties()).getChildByName('flags'),
        ).formattedValue(),
      ).toEqual('TRANSIT_FLAG_IS_RECENTS');
    });
  });
});
