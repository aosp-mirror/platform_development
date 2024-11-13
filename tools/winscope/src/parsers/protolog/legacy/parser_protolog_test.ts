/*
 * Copyright (C) 2022 The Android Open Source Project
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
import {Timestamp} from 'common/time';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {CoarseVersion} from 'trace/coarse_version';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

interface ExpectedMessage {
  'message': string;
  'ts': string;
  'at': string;
  'level': string;
  'tag': string;
}

const genProtoLogTest =
  (
    traceFile: string,
    timestampCount: number,
    first3ExpectedRealTimestamps: Timestamp[],
    expectedFirstMessage: ExpectedMessage,
  ) =>
  () => {
    let parser: Parser<PropertyTreeNode>;

    beforeAll(async () => {
      jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
      parser = (await UnitTestUtils.getParser(
        traceFile,
      )) as Parser<PropertyTreeNode>;
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.PROTO_LOG);
    });

    it('has expected coarse version', () => {
      expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LEGACY);
    });

    it('has expected length', () => {
      expect(parser.getLengthEntries()).toEqual(timestampCount);
    });

    it('provides timestamps', () => {
      const timestamps = assertDefined(parser.getTimestamps());
      expect(timestamps.length).toEqual(timestampCount);

      expect(timestamps.slice(0, 3)).toEqual(first3ExpectedRealTimestamps);
    });

    it('reconstructs human-readable log message', async () => {
      const message = await parser.getEntry(0);

      expect(
        assertDefined(message.getChildByName('text')).formattedValue(),
      ).toEqual(expectedFirstMessage['message']);
      expect(
        assertDefined(message.getChildByName('timestamp')).formattedValue(),
      ).toEqual(expectedFirstMessage['ts']);
      expect(
        assertDefined(message.getChildByName('tag')).formattedValue(),
      ).toEqual(expectedFirstMessage['tag']);
      expect(
        assertDefined(message.getChildByName('level')).formattedValue(),
      ).toEqual(expectedFirstMessage['level']);
      expect(
        assertDefined(message.getChildByName('at')).formattedValue(),
      ).toEqual(expectedFirstMessage['at']);
    });

    it('reconstructs human-readable log message (REAL time)', async () => {
      const message = await parser.getEntry(0);

      expect(
        assertDefined(message.getChildByName('text')).formattedValue(),
      ).toEqual(expectedFirstMessage['message']);
      expect(
        assertDefined(message.getChildByName('timestamp')).formattedValue(),
      ).toEqual(expectedFirstMessage['ts']);
      expect(
        assertDefined(message.getChildByName('tag')).formattedValue(),
      ).toEqual(expectedFirstMessage['tag']);
      expect(
        assertDefined(message.getChildByName('level')).formattedValue(),
      ).toEqual(expectedFirstMessage['level']);
      expect(
        assertDefined(message.getChildByName('at')).formattedValue(),
      ).toEqual(expectedFirstMessage['at']);
    });
  };

describe('ParserProtoLog', () => {
  describe(
    '32',
    genProtoLogTest(
      'traces/elapsed_and_real_timestamp/ProtoLog32.pb',
      50,
      [
        TimestampConverterUtils.makeRealTimestamp(1655727125377266486n),
        TimestampConverterUtils.makeRealTimestamp(1655727125377336718n),
        TimestampConverterUtils.makeRealTimestamp(1655727125377350430n),
      ],
      {
        'message':
          'InsetsSource updateVisibility for ITYPE_IME, serverVisible: false clientVisible: false',
        'ts': '2022-06-20, 12:12:05.377',
        'tag': 'WindowManager',
        'level': 'DEBUG',
        'at': 'com/android/server/wm/InsetsSourceProvider.java',
      },
    ),
  );
  describe(
    '64',
    genProtoLogTest(
      'traces/elapsed_and_real_timestamp/ProtoLog64.pb',
      4615,
      [
        TimestampConverterUtils.makeRealTimestamp(1709196806399529939n),
        TimestampConverterUtils.makeRealTimestamp(1709196806399763866n),
        TimestampConverterUtils.makeRealTimestamp(1709196806400297151n),
      ],
      {
        'message': 'Starting activity when config will change = false',
        'ts': '2024-02-29, 08:53:26.400',
        'tag': 'WindowManager',
        'level': 'VERBOSE',
        'at': 'com/android/server/wm/ActivityStarter.java',
      },
    ),
  );
  describe(
    'Missing config message',
    genProtoLogTest(
      'traces/elapsed_and_real_timestamp/ProtoLogMissingConfigMessage.pb',
      7295,
      [
        TimestampConverterUtils.makeRealTimestamp(1669053909777144978n),
        TimestampConverterUtils.makeRealTimestamp(1669053909778011697n),
        TimestampConverterUtils.makeRealTimestamp(1669053909778800707n),
      ],
      {
        'message': 'SURFACE isColorSpaceAgnostic=true: NotificationShade',
        'ts': '2022-11-21, 18:05:09.777',
        'tag': 'WindowManager',
        'level': 'INFO',
        'at': 'com/android/server/wm/WindowSurfaceController.java',
      },
    ),
  );
});
