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
import {utf8Encode} from 'common/string_utils';
import {
  TimestampConverterUtils,
  timestampEqualityTester,
} from 'common/time/test_utils';
import {Timestamp} from 'common/time/time';
import Long from 'long';
import {perfetto} from 'protos/perfetto/trace/static';
import {LegacyParserProvider} from 'test/unit/fixture_utils';
import {CoarseVersion} from 'trace/coarse_version';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {CONFIG_32, CONFIG_64} from './legacy_to_perfetto_configs';

interface ExpectedInternedData {
  packetIndex: number;
  str: string;
  iid: number;
}

interface ExpectedMessagePacket {
  packetIndex: number;
  sequenceFlags: perfetto.protos.TracePacket.SequenceFlags;
  timestamp: Long;
  messageId: Long;
  strParamIids: number[];
  sint64Params: Long[];
  doubleParams: number[];
  booleanParams: number[];
}

interface ExpectedMessage {
  message: string;
  ts: string;
  at: string;
  level: string;
  tag: string;
}

abstract class ParserProtologTest {
  abstract readonly traceFile: string;
  abstract readonly timestampCount: number;
  abstract readonly first3ExpectedRealTimestamps: Timestamp[];
  abstract readonly expectedConfig: perfetto.protos.IProtoLogViewerConfig;
  abstract readonly internedData1: ExpectedInternedData;
  abstract readonly internedData2: ExpectedInternedData;
  abstract readonly messagePacketWithInternedStrings: ExpectedMessagePacket;
  abstract readonly messagePacketNoInternedStrings: ExpectedMessagePacket;
  abstract readonly expectedFirstMessage: ExpectedMessage;

  execute() {
    describe('ParserProtologTest', () => {
      const [sequenceId, trustedUid, trustedPid] = [10, 3, 5];
      let parser: Parser<PropertyTreeNode>;

      beforeAll(async () => {
        jasmine.addCustomEqualityTester(timestampEqualityTester);
        parser = await new LegacyParserProvider()
          .addFilename(this.traceFile)
          .getParser<PropertyTreeNode>();
      });

      it('has expected trace type', () => {
        expect(parser.getTraceType()).toEqual(TraceType.PROTO_LOG);
      });

      it('has expected coarse version', () => {
        expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LEGACY);
      });

      it('has expected length', () => {
        expect(parser.getLengthEntries()).toEqual(this.timestampCount);
      });

      it('provides timestamps', () => {
        const timestamps = assertDefined(parser.getTimestamps());
        expect(timestamps.length).toEqual(this.timestampCount);

        expect(timestamps.slice(0, 3)).toEqual(
          this.first3ExpectedRealTimestamps,
        );
      });

      it('does not provide entry', () => {
        expect(parser.getEntry).toThrow();
      });

      it('converts to valid perfetto packets', async () => {
        const packets = parser.convertToPerfettoPackets!(
          sequenceId,
          trustedUid,
          trustedPid,
        );
        expect(
          packets.filter((packet) => packet.protologMessage).length,
        ).toEqual(this.timestampCount);

        const firstPacket = packets[0];
        expect(firstPacket.trustedPacketSequenceId).toEqual(sequenceId);
        expect(firstPacket.sequenceFlags).toEqual(
          perfetto.protos.TracePacket.SequenceFlags
            .SEQ_INCREMENTAL_STATE_CLEARED,
        );
        expect(firstPacket.trustedUid).toEqual(trustedUid);
        expect(firstPacket.trustedPid).toEqual(trustedPid);
        expect(firstPacket.internedData).toBeNull();
        expect(firstPacket.protologViewerConfig).toBeNull();
        expect(firstPacket.protologMessage).toBeNull();

        const viewerConfigPacket = packets[1];
        expect(viewerConfigPacket.trustedPacketSequenceId).toEqual(sequenceId);
        expect(viewerConfigPacket.sequenceFlags).toEqual(
          perfetto.protos.TracePacket.SequenceFlags.SEQ_UNSPECIFIED,
        );
        expect(viewerConfigPacket.protologViewerConfig).toEqual(
          this.expectedConfig,
        );
        expect(viewerConfigPacket.trustedUid).toEqual(trustedUid);
        expect(viewerConfigPacket.trustedPid).toEqual(0);
        expect(viewerConfigPacket.internedData).toBeNull();
        expect(viewerConfigPacket.protologMessage).toBeNull();

        checkInternedDataPacket(packets, this.internedData1);
        checkInternedDataPacket(packets, this.internedData2);

        checkMessagePacket(packets, this.messagePacketNoInternedStrings);
        checkMessagePacket(packets, this.messagePacketWithInternedStrings);
      });

      it('converts to valid perfetto trace', async () => {
        const perfettoParser = await new LegacyParserProvider()
          .addFilename(this.traceFile)
          .setConvertToPerfetto(true)
          .getParser<PropertyTreeNode>();

        expect(perfettoParser.getTimestamps()?.slice(0, 3)).toEqual(
          this.first3ExpectedRealTimestamps,
        );

        const message = await perfettoParser.getEntry(0);

        expect(
          assertDefined(message.getChildByName('text')).formattedValue(),
        ).toEqual(this.expectedFirstMessage.message);
        expect(
          assertDefined(message.getChildByName('timestamp')).formattedValue(),
        ).toEqual(this.expectedFirstMessage.ts);
        expect(
          assertDefined(message.getChildByName('tag')).formattedValue(),
        ).toEqual(this.expectedFirstMessage.tag);
        expect(
          assertDefined(message.getChildByName('level')).formattedValue(),
        ).toEqual(this.expectedFirstMessage.level);
        expect(
          assertDefined(message.getChildByName('at')).formattedValue(),
        ).toEqual(this.expectedFirstMessage.at);
      });

      function checkMessagePacket(
        packets: perfetto.protos.TracePacket[],
        expectedMsg: ExpectedMessagePacket,
      ) {
        const packet = packets[expectedMsg.packetIndex];
        expect(packet.trustedPacketSequenceId).toEqual(sequenceId);
        expect(packet.sequenceFlags).toEqual(expectedMsg.sequenceFlags);
        expect(packet.trustedUid).toEqual(trustedUid);
        expect(packet.trustedPid).toEqual(trustedPid);
        const ts1 = expectedMsg.timestamp;
        ts1.unsigned = true;
        expect(packet.timestamp).toEqual(ts1);
        expect(packet.protologMessage?.messageId).toEqual(
          expectedMsg.messageId,
        );
        expect(packet.protologMessage?.strParamIids).toEqual(
          expectedMsg.strParamIids,
        );
        expect(packet.protologMessage?.booleanParams).toEqual(
          expectedMsg.booleanParams,
        );
        expect(packet.protologMessage?.doubleParams).toEqual(
          expectedMsg.doubleParams,
        );
        expect(packet.protologMessage?.sint64Params).toEqual(
          expectedMsg.sint64Params,
        );
        expect(packet.protologViewerConfig).toBeNull();
        expect(packet.internedData).toBeNull();
      }

      function checkInternedDataPacket(
        packets: perfetto.protos.TracePacket[],
        expectedData: ExpectedInternedData,
      ) {
        const packet = packets[expectedData.packetIndex];
        expect(packet.trustedPacketSequenceId).toEqual(sequenceId);
        expect(packet.sequenceFlags).toEqual(
          perfetto.protos.TracePacket.SequenceFlags.SEQ_UNSPECIFIED,
        );
        expect(packet.trustedUid).toEqual(trustedUid);
        expect(packet.trustedPid).toEqual(trustedPid);
        expect(packet.internedData?.protologStringArgs).toEqual([
          perfetto.protos.InternedString.fromObject({
            iid: Long.fromNumber(expectedData.iid),
            str: utf8Encode(expectedData.str),
          }),
        ]);
        expect(packet.protologViewerConfig).toBeNull();
        expect(packet.protologMessage).toBeNull();
      }
    });
  }
}

class ParserProtolog32Test extends ParserProtologTest {
  override readonly traceFile =
    'traces/elapsed_and_real_timestamp/ProtoLog32.pb';
  override readonly timestampCount = 50;
  override readonly first3ExpectedRealTimestamps = [
    TimestampConverterUtils.makeRealTimestamp(1655727125377266486n),
    TimestampConverterUtils.makeRealTimestamp(1655727125377336718n),
    TimestampConverterUtils.makeRealTimestamp(1655727125377350430n),
  ];
  override readonly expectedConfig = CONFIG_32;
  override readonly internedData1: ExpectedInternedData = {
    packetIndex: 2,
    iid: 1,
    str: 'ITYPE_IME',
  };
  override readonly internedData2: ExpectedInternedData = {
    packetIndex: 3,
    iid: 2,
    str: 'false',
  };
  override readonly messagePacketNoInternedStrings: ExpectedMessagePacket = {
    packetIndex: 50,
    sequenceFlags: perfetto.protos.TracePacket.SequenceFlags.SEQ_UNSPECIFIED,
    timestamp: Long.fromNumber(850755642097),
    messageId: Long.fromNumber(1984782949),
    strParamIids: [],
    sint64Params: [],
    booleanParams: [],
    doubleParams: [],
  };
  override readonly messagePacketWithInternedStrings: ExpectedMessagePacket = {
    packetIndex: 4,
    sequenceFlags:
      perfetto.protos.TracePacket.SequenceFlags.SEQ_NEEDS_INCREMENTAL_STATE,
    timestamp: Long.fromNumber(850746266486),
    messageId: Long.fromNumber(2070726247),
    strParamIids: [1, 2, 2],
    sint64Params: [],
    booleanParams: [],
    doubleParams: [],
  };
  override readonly expectedFirstMessage: ExpectedMessage = {
    message:
      'InsetsSource updateVisibility for ITYPE_IME, serverVisible: false clientVisible: false',
    ts: '2022-06-20, 12:12:05.377',
    tag: 'WindowManager',
    level: 'DEBUG',
    at: 'com/android/server/wm/InsetsSourceProvider.java',
  };
}

class ParserProtolog64Test extends ParserProtologTest {
  override readonly traceFile =
    'traces/elapsed_and_real_timestamp/ProtoLog64.pb';
  override readonly timestampCount = 4615;
  override readonly first3ExpectedRealTimestamps = [
    TimestampConverterUtils.makeRealTimestamp(1709196806399529939n),
    TimestampConverterUtils.makeRealTimestamp(1709196806399763866n),
    TimestampConverterUtils.makeRealTimestamp(1709196806400297151n),
  ];
  override readonly expectedConfig = CONFIG_64;
  override readonly internedData1: ExpectedInternedData = {
    packetIndex: 5,
    iid: 1,
    str: 'ActivityRecord{e361a5d u0 com.google.android.gm/.ConversationListActivityGmail',
  };
  override readonly internedData2: ExpectedInternedData = {
    packetIndex: 6,
    iid: 2,
    str: 'null',
  };
  override readonly messagePacketNoInternedStrings: ExpectedMessagePacket = {
    packetIndex: 2,
    sequenceFlags: perfetto.protos.TracePacket.SequenceFlags.SEQ_UNSPECIFIED,
    timestamp: Long.fromNumber(1315553529939),
    messageId: Long.fromString('1665699123574159131'),
    strParamIids: [],
    sint64Params: [],
    booleanParams: [0],
    doubleParams: [],
  };
  override readonly messagePacketWithInternedStrings: ExpectedMessagePacket = {
    packetIndex: 9,
    sequenceFlags:
      perfetto.protos.TracePacket.SequenceFlags.SEQ_NEEDS_INCREMENTAL_STATE,
    timestamp: Long.fromNumber(1315574594310),
    messageId: Long.fromString('-6873410057142191118'),
    strParamIids: [1, 2, 3, 4],
    sint64Params: [],
    booleanParams: [],
    doubleParams: [],
  };
  override readonly expectedFirstMessage: ExpectedMessage = {
    message: 'Starting activity when config will change = false',
    ts: '2024-02-29, 08:53:26.400',
    tag: 'WindowManager',
    level: 'VERBOSE',
    at: 'com/android/server/wm/ActivityStarter.java',
  };
}

class ParserProtologMissingConfigTest extends ParserProtologTest {
  override readonly traceFile =
    'traces/elapsed_and_real_timestamp/ProtoLogMissingConfigMessage.pb';
  override readonly timestampCount = 7295;
  override readonly first3ExpectedRealTimestamps = [
    TimestampConverterUtils.makeRealTimestamp(1669053909777144978n),
    TimestampConverterUtils.makeRealTimestamp(1669053909778011697n),
    TimestampConverterUtils.makeRealTimestamp(1669053909778800707n),
  ];
  override readonly expectedConfig = CONFIG_32;
  override readonly internedData1: ExpectedInternedData = {
    packetIndex: 2,
    iid: 1,
    str: 'NotificationShade',
  };
  override readonly internedData2: ExpectedInternedData = {
    packetIndex: 4,
    iid: 2,
    str: 'Window{f199162 u0 NotificationShade}',
  };
  override readonly messagePacketNoInternedStrings: ExpectedMessagePacket = {
    packetIndex: 92,
    sequenceFlags: perfetto.protos.TracePacket.SequenceFlags.SEQ_UNSPECIFIED,
    timestamp: Long.fromNumber(24398203599667),
    messageId: Long.fromString('1381227466'),
    strParamIids: [],
    sint64Params: [Long.fromNumber(2), Long.fromNumber(0)],
    booleanParams: [],
    doubleParams: [],
  };
  override readonly messagePacketWithInternedStrings: ExpectedMessagePacket = {
    packetIndex: 3,
    sequenceFlags:
      perfetto.protos.TracePacket.SequenceFlags.SEQ_NEEDS_INCREMENTAL_STATE,
    timestamp: Long.fromNumber(24398190144978),
    messageId: Long.fromNumber(585096182),
    strParamIids: [1],
    sint64Params: [],
    booleanParams: [1],
    doubleParams: [],
  };
  override readonly expectedFirstMessage: ExpectedMessage = {
    message: 'SURFACE isColorSpaceAgnostic=true: NotificationShade',
    ts: '2022-11-21, 18:05:09.777',
    tag: 'WindowManager',
    level: 'INFO',
    at: 'com/android/server/wm/WindowSurfaceController.java',
  };
}

describe('32', () => {
  new ParserProtolog32Test().execute();
});

describe('64', () => {
  new ParserProtolog64Test().execute();
});

describe('Missing config', () => {
  new ParserProtologMissingConfigTest().execute();
});
