/*
 * Copyright (C) 2024 The Android Open Source Project
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
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {CoarseVersion} from 'trace/coarse_version';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

describe('Perfetto ParserKeyEvent', () => {
  let parser: Parser<PropertyTreeNode>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(timestampEqualityTester);
    parser = await UnitTestUtils.getPerfettoParser(
      TraceType.INPUT_KEY_EVENT,
      'traces/perfetto/input-events.perfetto-trace',
    );
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.INPUT_KEY_EVENT);
  });

  it('has expected coarse version', () => {
    expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LATEST);
  });

  it('provides timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps());

    expect(timestamps.length).toEqual(2);

    const expected = [
      TimestampConverterUtils.makeRealTimestamp(1718386905115026232n),
      TimestampConverterUtils.makeRealTimestamp(1718386905123057319n),
    ];
    expect(timestamps).toEqual(expected);
  });

  it('retrieves trace entry from timestamp', async () => {
    const entry = await parser.getEntry(1);
    expect(entry.id).toEqual('AndroidKeyEvent entry');
  });

  it('transforms fake key event proto built from trace processor args', async () => {
    const entry = await parser.getEntry(0);
    const keyEvent = assertDefined(entry.getChildByName('keyEvent'));

    expect(keyEvent?.getChildByName('eventId')?.getValue()).toEqual(759309047);
    expect(keyEvent?.getChildByName('action')?.formattedValue()).toEqual(
      'ACTION_DOWN',
    );
    expect(keyEvent?.getChildByName('source')?.formattedValue()).toEqual(
      'SOURCE_KEYBOARD',
    );
    expect(keyEvent?.getChildByName('flags')?.formattedValue()).toEqual(
      'FLAG_FROM_SYSTEM',
    );
    expect(keyEvent?.getChildByName('deviceId')?.getValue()).toEqual(2);
    expect(keyEvent?.getChildByName('displayId')?.getValue()).toEqual(-1);
    expect(keyEvent?.getChildByName('metaState')?.formattedValue()).toEqual(
      '0x0',
    );
    expect(keyEvent?.getChildByName('keyCode')?.getValue()).toEqual(24);
    expect(keyEvent?.getChildByName('scanCode')?.getValue()).toEqual(115);
  });

  it('merges key event with all associated dispatch events', async () => {
    const entry = await parser.getEntry(0);
    const windowDispatchEvents = assertDefined(
      entry.getChildByName('windowDispatchEvents'),
    );

    expect(windowDispatchEvents?.getAllChildren().length).toEqual(2);
    expect(
      windowDispatchEvents
        ?.getChildByName('0')
        ?.getChildByName('windowId')
        ?.getValue(),
    ).toEqual(212n);
    expect(
      windowDispatchEvents
        ?.getChildByName('1')
        ?.getChildByName('windowId')
        ?.getValue(),
    ).toEqual(0n);
  });

  it('supports VSYNCID custom query', async () => {
    const trace = new TraceBuilder()
      .setType(TraceType.INPUT_KEY_EVENT)
      .setParser(parser)
      .build();
    const entries = await trace
      .sliceEntries(0, 2)
      .customQuery(CustomQueryType.VSYNCID);
    const values = entries.map((entry) => entry.getValue());
    expect(values).toEqual([89114n, 89115n]);
  });
});
