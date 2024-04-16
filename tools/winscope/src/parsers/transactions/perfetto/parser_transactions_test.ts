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
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {CoarseVersion} from 'trace/coarse_version';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

describe('Perfetto ParserTransactions', () => {
  let parser: Parser<PropertyTreeNode>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
    parser = await UnitTestUtils.getPerfettoParser(
      TraceType.TRANSACTIONS,
      'traces/perfetto/transactions_trace.perfetto-trace',
    );
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.TRANSACTIONS);
  });

  it('has expected coarse version', () => {
    expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LATEST);
  });

  it('provides timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps());

    expect(timestamps.length).toEqual(712);

    const expected = [
      TimestampConverterUtils.makeRealTimestamp(1659507541051480997n),
      TimestampConverterUtils.makeRealTimestamp(1659507541118452067n),
      TimestampConverterUtils.makeRealTimestamp(1659507542621651001n),
    ];
    expect(timestamps.slice(0, 3)).toEqual(expected);
  });

  it('retrieves trace entry from timestamp', async () => {
    const entry = await parser.getEntry(1);
    expect(entry.id).toEqual('TransactionsTraceEntry entry');
  });

  it('transforms fake proto built from trace processor args', async () => {
    const entry0 = await parser.getEntry(0);
    const entry2 = await parser.getEntry(2);

    // Add empty arrays
    expect(entry0.getChildByName('addedDisplays')?.getAllChildren()).toEqual(
      [],
    );
    expect(entry0.getChildByName('destroyedLayers')?.getAllChildren()).toEqual(
      [],
    );
    expect(entry0.getChildByName('removedDisplays')?.getAllChildren()).toEqual(
      [],
    );
    expect(
      entry0.getChildByName('destroyedLayerHandles')?.getAllChildren(),
    ).toEqual([]);
    expect(entry0.getChildByName('displays')?.getAllChildren()).toEqual([]);

    // Add default values
    expect(
      entry0
        .getChildByName('transactions')
        ?.getChildByName('1')
        ?.getChildByName('pid')
        ?.getValue(),
    ).toEqual(0);

    // Convert value types (bigint -> number)
    expect(
      entry0
        .getChildByName('transactions')
        ?.getChildByName('1')
        ?.getChildByName('uid')
        ?.getValue(),
    ).toEqual(1003);

    // Decode enum IDs
    expect(
      entry0
        .getChildByName('transactions')
        ?.getChildByName('0')
        ?.getChildByName('layerChanges')
        ?.getChildByName('0')
        ?.getChildByName('dropInputMode')
        ?.formattedValue(),
    ).toEqual('NONE');

    expect(
      entry2
        .getChildByName('transactions')
        ?.getChildByName('0')
        ?.getChildByName('layerChanges')
        ?.getChildByName('0')
        ?.getChildByName('bufferData')
        ?.getChildByName('pixelFormat')
        ?.formattedValue(),
    ).toEqual('PIXEL_FORMAT_RGBA_1010102');
  });

  it("decodes 'what' field in proto", async () => {
    {
      const entry = await parser.getEntry(0);
      const transactions = assertDefined(entry.getChildByName('transactions'));
      expect(
        transactions
          .getChildByName('0')
          ?.getChildByName('layerChanges')
          ?.getChildByName('0')
          ?.getChildByName('what')
          ?.formattedValue(),
      ).toEqual('eLayerChanged');

      expect(
        transactions
          .getChildByName('1')
          ?.getChildByName('layerChanges')
          ?.getChildByName('0')
          ?.getChildByName('what')
          ?.formattedValue(),
      ).toEqual('eFlagsChanged | eDestinationFrameChanged');
    }
    {
      const entry = await parser.getEntry(222);
      const transactions = assertDefined(entry.getChildByName('transactions'));

      expect(
        transactions
          .getChildByName('1')
          ?.getChildByName('displayChanges')
          ?.getChildByName('0')
          ?.getChildByName('what')
          ?.formattedValue(),
      ).toEqual(
        'eLayerStackChanged | eDisplayProjectionChanged | eFlagsChanged',
      );
    }
  });

  it('supports VSYNCID custom query', async () => {
    const trace = new TraceBuilder()
      .setType(TraceType.TRANSACTIONS)
      .setParser(parser)
      .build();
    const entries = await trace
      .sliceEntries(0, 3)
      .customQuery(CustomQueryType.VSYNCID);
    const values = entries.map((entry) => entry.getValue());
    expect(values).toEqual([1n, 2n, 3n]);
  });
});
