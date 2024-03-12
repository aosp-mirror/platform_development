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
import {Timestamp, TimestampType} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

interface ExpectedMessage {
  'message': string;
  'ts': {'real': string; 'elapsed': string};
  'at': string;
  'level': string;
  'tag': string;
}

const genProtoLogTest =
  (
    traceFile: string,
    timestampCount: number,
    first3ExpectedRealTimestamps: Timestamp[],
    first3ExpectedElapsedTimestamps: Timestamp[],
    first3WithTimezoneTimestamps: Timestamp[],
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

    it('has expected length', () => {
      expect(parser.getLengthEntries()).toEqual(timestampCount);
    });

    it('provides elapsed timestamps', () => {
      const timestamps = parser.getTimestamps(TimestampType.ELAPSED)!;
      expect(timestamps.length).toEqual(timestampCount);

      expect(timestamps.slice(0, 3)).toEqual(first3ExpectedElapsedTimestamps);
    });

    it('provides real timestamps', () => {
      const timestamps = parser.getTimestamps(TimestampType.REAL)!;
      expect(timestamps.length).toEqual(timestampCount);

      expect(timestamps.slice(0, 3)).toEqual(first3ExpectedRealTimestamps);
    });

    it('applies timezone info to real timestamps only', async () => {
      const parserWithTimezoneInfo = (await UnitTestUtils.getParser(
        traceFile,
        true,
      )) as Parser<PropertyTreeNode>;
      expect(parserWithTimezoneInfo.getTraceType()).toEqual(
        TraceType.PROTO_LOG,
      );

      expect(
        assertDefined(
          parserWithTimezoneInfo.getTimestamps(TimestampType.ELAPSED),
        ).slice(0, 3),
      ).toEqual(first3ExpectedElapsedTimestamps);

      expect(
        assertDefined(
          parserWithTimezoneInfo.getTimestamps(TimestampType.REAL),
        ).slice(0, 3),
      ).toEqual(first3WithTimezoneTimestamps);
    });

    it('reconstructs human-readable log message (ELAPSED time)', async () => {
      const message = await parser.getEntry(0, TimestampType.ELAPSED);

      expect(
        assertDefined(message.getChildByName('text')).formattedValue(),
      ).toEqual(expectedFirstMessage['message']);
      expect(
        assertDefined(message.getChildByName('timestamp')).formattedValue(),
      ).toEqual(expectedFirstMessage['ts']['elapsed']);
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
      const message = await parser.getEntry(0, TimestampType.REAL);

      expect(
        assertDefined(message.getChildByName('text')).formattedValue(),
      ).toEqual(expectedFirstMessage['message']);
      expect(
        assertDefined(message.getChildByName('timestamp')).formattedValue(),
      ).toEqual(expectedFirstMessage['ts']['real']);
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
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1655727125377266486n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1655727125377336718n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1655727125377350430n),
      ],
      [
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(850746266486n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(850746336718n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(850746350430n),
      ],
      [
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1655746925377266486n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1655746925377336718n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1655746925377350430n),
      ],
      {
        'message':
          'InsetsSource updateVisibility for ITYPE_IME, serverVisible: false clientVisible: false',
        'ts': {
          'real': '2022-06-20T12:12:05.377266486',
          'elapsed': '14m10s746ms266486ns',
        },
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
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1709196806399529939n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1709196806399763866n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1709196806400297151n),
      ],
      [
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(1315553529939n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(1315553763866n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(1315554297151n),
      ],
      [
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1709216606399529939n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1709216606399763866n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1709216606400297151n),
      ],
      {
        'message': 'Starting activity when config will change = false',
        'ts': {
          'real': '2024-02-29T08:53:26.399529939',
          'elapsed': '21m55s553ms529939ns',
        },
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
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1669053909777144978n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1669053909778011697n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1669053909778800707n),
      ],
      [
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(24398190144978n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(24398191011697n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(24398191800707n),
      ],
      [
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1669073709777144978n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1669073709778011697n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1669073709778800707n),
      ],
      {
        'message': 'SURFACE isColorSpaceAgnostic=true: NotificationShade',
        'ts': {
          'real': '2022-11-21T18:05:09.777144978',
          'elapsed': '6h46m38s190ms144978ns',
        },
        'tag': 'WindowManager',
        'level': 'INFO',
        'at': 'com/android/server/wm/WindowSurfaceController.java',
      },
    ),
  );
});
