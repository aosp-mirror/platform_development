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
import {UnitTestUtils} from 'test/unit/utils';
import {CoarseVersion} from 'trace/coarse_version';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

describe('Perfetto ParserTransitions', () => {
  describe('valid trace', () => {
    let parser: Parser<PropertyTreeNode>;

    beforeAll(async () => {
      jasmine.addCustomEqualityTester(timestampEqualityTester);
      parser = await UnitTestUtils.getPerfettoParser(
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

    it('decodes transition properties', async () => {
      const entry = await parser.getEntry(0);
      const wmDataNode = assertDefined(entry.getChildByName('wmData'));
      const shellDataNode = assertDefined(entry.getChildByName('shellData'));

      expect(entry.getChildByName('id')?.getValue()).toEqual(32n);
      expect(
        wmDataNode.getChildByName('createTimeNs')?.formattedValue(),
      ).toEqual('2023-11-21, 13:30:25.429');
      expect(wmDataNode.getChildByName('sendTimeNs')?.formattedValue()).toEqual(
        '2023-11-21, 13:30:25.442',
      );
      expect(
        wmDataNode.getChildByName('finishTimeNs')?.formattedValue(),
      ).toEqual('2023-11-21, 13:30:25.970');
      expect(entry.getChildByName('merged')?.getValue()).toBeFalse();
      expect(entry.getChildByName('played')?.getValue()).toBeTrue();
      expect(entry.getChildByName('aborted')?.getValue()).toBeFalse();

      expect(
        assertDefined(
          wmDataNode.getChildByName('startingWindowRemoveTimeNs'),
        ).formattedValue(),
      ).toEqual('2023-11-21, 13:30:25.565');
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
        assertDefined(targets[0].getChildByName('flags')).formattedValue(),
      ).toEqual('FLAG_MOVED_TO_TOP');
      expect(
        assertDefined(targets[1].getChildByName('flags')).formattedValue(),
      ).toEqual('FLAG_SHOW_WALLPAPER');

      expect(
        assertDefined(
          shellDataNode.getChildByName('dispatchTimeNs'),
        ).formattedValue(),
      ).toEqual('2023-11-21, 13:30:25.448');
      expect(shellDataNode.getChildByName('mergeRequestTime')).toBeUndefined();
      expect(shellDataNode.getChildByName('mergeTime')).toBeUndefined();
      expect(shellDataNode.getChildByName('abortTimeNs')).toBeUndefined();
      expect(shellDataNode.getChildByName('mergeTarget')).toBeUndefined();
      expect(
        assertDefined(shellDataNode.getChildByName('handler')).formattedValue(),
      ).toEqual('com.android.wm.shell.transition.DefaultMixedHandler');

      const entryWithFlags = await parser.getEntry(1);
      const wmDataWithFlags = assertDefined(
        entryWithFlags.getChildByName('wmData'),
      );
      expect(
        assertDefined(wmDataWithFlags.getChildByName('flags')).formattedValue(),
      ).toEqual('TRANSIT_FLAG_IS_RECENTS');
    });
  });
});
