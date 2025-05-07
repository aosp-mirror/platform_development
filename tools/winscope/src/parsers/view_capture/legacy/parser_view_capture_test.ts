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
import {utf8Encode} from 'common/string_utils';
import {
  TimestampConverterUtils,
  timestampEqualityTester,
} from 'common/time/test_utils';
import Long from 'long';
import {perfetto} from 'protos/perfetto/trace/static';
import {LegacyParserProvider} from 'test/unit/fixture_utils';
import {CoarseVersion} from 'trace/coarse_version';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

describe('ParserViewCapture', () => {
  let parser: Parser<HierarchyTreeNode>;
  let trace: Trace<HierarchyTreeNode>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(timestampEqualityTester);
    parser = await new LegacyParserProvider()
      .addFilename(
        'traces/elapsed_and_real_timestamp/com.google.android.apps.nexuslauncher_0.vc',
      )
      .getParser<HierarchyTreeNode>();
    trace = Trace.fromParser(parser);
    jasmine.DEFAULT_TIMEOUT_INTERVAL = 10000;
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.VIEW_CAPTURE);
  });

  it('has expected coarse version', () => {
    expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LEGACY);
  });

  it('has expected descriptors', () => {
    expect(parser.getDescriptors()).toEqual([
      '.Taskbar',
      'com.google.android.apps.nexuslauncher_0.vc',
    ]);
  });

  it('provides timestamps', () => {
    const expected = [
      TimestampConverterUtils.makeRealTimestamp(1691692936292808460n),
      TimestampConverterUtils.makeRealTimestamp(1691692936301385080n),
      TimestampConverterUtils.makeRealTimestamp(1691692936309419870n),
    ];
    expect(assertDefined(parser.getTimestamps()).slice(0, 3)).toEqual(expected);
  });

  it('does not provide entry', () => {
    expect(parser.getEntry).toThrow();
  });

  it('converts to valid perfetto packets', async () => {
    const packets = parser.convertToPerfettoPackets!(10, 2, 3);
    expect(packets.length).toEqual(2000);
    expect(packets[0].trustedPacketSequenceId).toEqual(10);
    expect(packets[0].timestamp).toEqual(
      Long.fromString(BigInt(181114412436130).toString()),
    );
    expect(packets[0].timestampClockId).toEqual(
      perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.BOOTTIME,
    );
    expect(packets[0].trustedUid).toEqual(2);
    expect(packets[0].trustedPid).toEqual(3);
    expect(packets[0].sequenceFlags).toEqual(3);
    expect(packets[1].sequenceFlags).toEqual(
      perfetto.protos.TracePacket.SequenceFlags.SEQ_NEEDS_INCREMENTAL_STATE,
    );

    const vcData = assertDefined(
      packets[0].winscopeExtensions?.[
        '.perfetto.protos.WinscopeExtensionsImpl.viewcapture'
      ],
    );
    expect(vcData.packageNameIid).toEqual(1);
    expect(vcData.windowNameIid).toEqual(1);
    expect(vcData.views?.length).toEqual(17);

    const internedData = assertDefined(packets[0].internedData);

    expect(internedData.viewcapturePackageName?.length).toEqual(1);
    expect(internedData.viewcapturePackageName?.[0].iid).toEqual(
      Long.fromNumber(1, true),
    );
    expect(internedData.viewcapturePackageName?.[0].str).toEqual(
      utf8Encode('com.google.android.apps.nexuslauncher'),
    );

    expect(internedData.viewcaptureWindowName?.length).toEqual(1);
    expect(internedData.viewcaptureWindowName?.[0].iid).toEqual(
      Long.fromNumber(1, true),
    );
    expect(internedData.viewcaptureWindowName?.[0].str).toEqual(
      utf8Encode('.Taskbar'),
    );

    expect(internedData.viewcaptureClassName?.length).toEqual(68);
    expect(internedData.viewcaptureClassName?.[3].iid).toEqual(
      Long.fromNumber(3, true),
    );
    expect(internedData.viewcaptureClassName?.[3].str).toEqual(
      utf8Encode('com.android.launcher3.views.DoubleShadowBubbleTextView'),
    );

    expect(internedData.viewcaptureViewId?.length).toEqual(11);
    expect(internedData.viewcaptureViewId?.[1].iid).toEqual(
      Long.fromNumber(2, true),
    );
    expect(internedData.viewcaptureViewId?.[1].str).toEqual(
      utf8Encode('id/taskbar_view'),
    );

    expect(packets[1].internedData).toBeNull();
  });

  it('converts to valid perfetto trace', async () => {
    const perfettoParser = await new LegacyParserProvider()
      .addFilename(
        'traces/elapsed_and_real_timestamp/com.google.android.apps.nexuslauncher_0.vc',
      )
      .setConvertToPerfetto(true)
      .getParser<HierarchyTreeNode>();
    expect(perfettoParser.getTimestamps()?.slice(0, 3)).toEqual([
      TimestampConverterUtils.makeRealTimestamp(1691692936292808460n),
      TimestampConverterUtils.makeRealTimestamp(1691692936301385080n),
      TimestampConverterUtils.makeRealTimestamp(1691692936309419870n),
    ]);

    const entry = await perfettoParser.getEntry(1);
    expect(entry.name).toEqual(
      'com.android.launcher3.taskbar.TaskbarDragLayer@265160962',
    );
    expect(entry.getRects()?.length).toEqual(1);
  });
});
