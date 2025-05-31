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
  getTimestampConverter,
  TimestampConverterUtils,
  timestampEqualityTester,
} from 'common/time/test_utils';
import Long from 'long';
import {FileAndParser} from 'parsers/file_and_parser';
import {perfetto} from 'protos/perfetto/trace/static';
import {com} from 'protos/transitions/udc/static';
import {convertToPerfettoTrace, getTracesParser} from 'test/unit/fixture_utils';
import {CoarseVersion} from 'trace/coarse_version';
import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TracesParserTransitions} from './traces_parser_transitions';

describe('TracesParserTransitions', () => {
  let parser: Parser<PropertyTreeNode>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(timestampEqualityTester);
    parser = (
      await getTracesParser([
        'traces/elapsed_and_real_timestamp/wm_transition_trace.pb',
        'traces/elapsed_and_real_timestamp/shell_transition_trace.pb',
      ])
    ).tracesParser as Parser<PropertyTreeNode>;
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.TRANSITION);
  });

  it('has expected coarse version', () => {
    expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LEGACY);
  });

  it('has expected descriptors', () => {
    expect(parser.getDescriptors()).toEqual([
      'wm_transition_trace.pb',
      'shell_transition_trace.pb',
    ]);
  });

  it('provides timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps());
    const expected = [
      TimestampConverterUtils.makeRealTimestamp(1683188477607285317n),
      TimestampConverterUtils.makeRealTimestamp(1683188477785406289n),
      TimestampConverterUtils.makeRealTimestamp(1683188479256449868n),
      TimestampConverterUtils.makeRealTimestamp(1683188481345929443n),
    ];
    expect(timestamps).toEqual(expected);
  });

  it('does not provide entry', () => {
    expect(parser.getEntry).toThrow();
  });

  it('sets zero timestamp if both dispatch and send time unavailable', async () => {
    const result = await getTracesParser([
      'traces/elapsed_and_real_timestamp/wm_transition_trace.pb',
      'traces/elapsed_and_real_timestamp/shell_transition_trace.pb',
    ]);
    const transitionsParser = result.tracesParser as Parser<PropertyTreeNode>;
    const wmParser = result
      .constituentParsers[0] as Parser<com.android.server.wm.shell.ITransition>;
    const shellParser = result
      .constituentParsers[1] as Parser<com.android.wm.shell.ITransition>;

    const shellEntry = await shellParser.getEntry(1);
    shellEntry.dispatchTimeNs = null;
    const shellSpy = spyOn(shellParser, 'getEntry').and.callThrough();
    shellSpy.withArgs(1).and.returnValue(Promise.resolve(shellEntry));

    const wmEntry = await wmParser.getEntry(1);
    wmEntry.sendTimeNs = null;
    const wmSpy = spyOn(wmParser, 'getEntry').and.callThrough();
    wmSpy.withArgs(1).and.returnValue(Promise.resolve(wmEntry));

    await (transitionsParser as TracesParserTransitions).parse();
    expect(transitionsParser.getTimestamps()?.at(0)).toEqual(
      TimestampConverterUtils.makeRealTimestamp(0n),
    );
  });

  it('fails to parse without both wm and shell transition traces', async () => {
    await expectAsync(
      getTracesParser([
        'traces/elapsed_and_real_timestamp/wm_transition_trace.pb',
      ]),
    ).toBeRejected();
    await expectAsync(
      getTracesParser([
        'traces/elapsed_and_real_timestamp/shell_transition_trace.pb',
      ]),
    ).toBeRejected();
  });

  it('converts to valid perfetto packets', () => {
    const packets = parser.convertToPerfettoPackets!(10);
    expect(packets.length).toEqual(5);
    packets.forEach((packet) => {
      expect(packet.trustedPacketSequenceId).toEqual(10);
    });

    const handlerMappingPacket = packets[0];

    const shellHandlerMappings =
      perfetto.protos.ShellHandlerMappings.fromObject({
        mapping: [
          {id: 2, name: 'com.android.wm.shell.transition.DefaultMixedHandler'},
          {
            id: 3,
            name: 'com.android.wm.shell.recents.RecentsTransitionHandler',
          },
        ],
      });
    expect(handlerMappingPacket.shellHandlerMappings).toEqual(
      shellHandlerMappings,
    );

    const transition6Packet = packets[1];
    const transition6 = assertDefined(transition6Packet.shellTransition);
    expect(transition6.id).toEqual(6);
    const dispatchTime6 = Long.fromString('57649649922341');
    expect(transition6Packet.timestamp).toEqual(dispatchTime6);
    expect(transition6Packet.timestampClockId).toEqual(
      perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.BOOTTIME,
    );
    expect(transition6.createTimeNs).toEqual(Long.fromString('57649586217344'));
    expect(transition6.sendTimeNs).toEqual(Long.fromString('57649646973488'));
    expect(transition6.wmAbortTimeNs).toBeUndefined();
    expect(transition6.finishTimeNs).toEqual(Long.fromString('57650183020323'));
    expect(transition6.type).toEqual(1);
    expect(transition6.targets?.length).toEqual(2);
    expect(transition6.flags).toBeUndefined();
    expect(transition6.startingWindowRemoveTimeNs).toBeUndefined();
    expect(transition6.dispatchTimeNs).toEqual(dispatchTime6);
    expect(transition6.mergeTimeNs).toBeUndefined();
    expect(transition6.mergeRequestTimeNs).toBeUndefined();
    expect(transition6.shellAbortTimeNs).toBeUndefined();
    expect(transition6.handler).toEqual(2);
    expect(transition6.mergeTarget).toBeUndefined();

    const transition7Packet = packets[2];
    const transition7 = assertDefined(transition7Packet.shellTransition);
    expect(transition7.id).toEqual(7);
    const sendTime7 = Long.fromString('57649828043313');
    expect(transition7Packet.timestamp).toEqual(sendTime7);
    expect(transition7Packet.timestampClockId).toEqual(
      perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.BOOTTIME,
    );
    expect(transition7.sendTimeNs).toEqual(sendTime7);
    expect(transition7.dispatchTimeNs).toBeUndefined();
    expect(transition7.mergeTimeNs).toEqual(Long.fromString('57649829526223'));
    expect(transition7.shellAbortTimeNs).toEqual(
      Long.fromString('57649829445249'),
    );
    expect(transition7.handler).toBeUndefined();

    const transition8 = assertDefined(packets[3].shellTransition);
    expect(transition8.id).toEqual(8);
    expect(transition8.flags).toEqual(128);

    const transition9 = assertDefined(packets[4].shellTransition);
    expect(transition9.id).toEqual(9);
    expect(transition9.mergeRequestTimeNs).toEqual(
      Long.fromString('57653389780131'),
    );
    expect(transition9.mergeTarget).toEqual(8);
  });

  it('converts to valid perfetto trace', async () => {
    const converter = getTimestampConverter();
    const perfettoParser = (
      await convertToPerfettoTrace(
        [new FileAndParser(new TraceFile(new File([], '')), parser)],
        converter,
      )
    )[0].parser as Parser<HierarchyTreeNode>;

    converter.setRealToBootTimeOffsetNs(
      assertDefined(perfettoParser.getRealToBootTimeOffsetNs()),
    );
    perfettoParser.createTimestamps();
    expect(perfettoParser.getTimestamps()).toEqual([
      TimestampConverterUtils.makeRealTimestamp(1683188477607285317n),
      TimestampConverterUtils.makeRealTimestamp(1683188477785406289n),
      TimestampConverterUtils.makeRealTimestamp(1683188479256449868n),
      TimestampConverterUtils.makeRealTimestamp(1683188481345929443n),
    ]);
    const entries = [
      await perfettoParser.getEntry(0),
      await perfettoParser.getEntry(1),
      await perfettoParser.getEntry(2),
      await perfettoParser.getEntry(3),
    ];
    const entryIds = entries.map((e) =>
      e.getEagerPropertyByName('transitionId')?.getValue(),
    );
    expect(entryIds).toEqual([6n, 7n, 8n, 9n]);

    const entry = entries[2];
    const entryProperties = await entry.getAllProperties();
    expect(entry.getEagerPropertyByName('status')?.getValue()).toEqual(
      'played',
    );

    checkEagerPropertyValue(entry, 'sendTimeNs', '2023-05-04, 08:21:19.252');
    checkPropertyValue(entryProperties, 'startTransactionId', '13086765351920');
    checkEagerPropertyValue(entry, 'flags', 'TRANSIT_FLAG_IS_RECENTS');

    const layerParticipants = assertDefined(
      entry.getEagerPropertyByName('layers'),
    );
    expect(layerParticipants.getAllChildren().length).toEqual(2);
    checkPropertyValue(layerParticipants, '0', '113');
    checkPropertyValue(layerParticipants, '1', '190');

    const windowParticipants = assertDefined(
      entry.getEagerPropertyByName('windows'),
    );
    expect(windowParticipants.getAllChildren().length).toEqual(2);
    checkPropertyValue(windowParticipants, '0', '179781688');
    checkPropertyValue(windowParticipants, '1', '184699222');

    const targets = assertDefined(
      entryProperties.getChildByName('targets'),
    ).getAllChildren();
    expect(targets.length).toEqual(2);
    checkPropertyValue(targets[0], 'layerId', '113');
    checkPropertyValue(targets[0], 'mode', 'TO_FRONT');
    checkPropertyValue(
      targets[0],
      'flags',
      'FLAG_MOVED_TO_TOP | FLAG_SHOW_WALLPAPER',
    );
    checkPropertyValue(targets[0], 'windowId', '179781688');

    checkEagerPropertyValue(
      entry,
      'handler',
      'com.android.wm.shell.recents.RecentsTransitionHandler',
    );
    checkPropertyValue(
      entryProperties,
      'handler',
      'com.android.wm.shell.recents.RecentsTransitionHandler',
    );
  });

  function checkEagerPropertyValue(
    node: HierarchyTreeNode,
    property: string,
    value: string,
  ) {
    expect(node.getEagerPropertyByName(property)?.formattedValue()).toEqual(
      value,
    );
  }

  function checkPropertyValue(
    node: PropertyTreeNode,
    property: string,
    value: string,
  ) {
    expect(node.getChildByName(property)?.formattedValue()).toEqual(value);
  }
});
