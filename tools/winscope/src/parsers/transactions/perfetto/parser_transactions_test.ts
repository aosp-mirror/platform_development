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
import {TransactionType} from 'parsers/transactions/transaction_type';
import {getPerfettoParser} from 'test/unit/fixture_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {CoarseVersion} from 'trace/coarse_version';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

describe('PerfettoParserTransactions', () => {
  let parser: Parser<HierarchyTreeNode>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(timestampEqualityTester);
    parser = await getPerfettoParser(
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

  describe('eager fetching', () => {
    it('fetches id properties', async () => {
      const entry0 = await parser.getEntry(0);
      checkIdProperties(
        assertDefined(entry0.getChildByName('LayerCreationArgs')),
        undefined,
        1n,
        undefined,
        TransactionType.LAYER_ADDED,
      );
      checkIdProperties(
        assertDefined(entry0.getChildByName('LayerState')),
        2211908157441n,
        1n,
        undefined,
        TransactionType.LAYER_CHANGED,
      );

      const entry222 = await parser.getEntry(222);
      checkIdProperties(
        assertDefined(entry222.getChildByName('DisplayState')),
        6841882902621n,
        undefined,
        4294967295n,
        TransactionType.DISPLAY_CHANGED,
      );

      const entry351 = await parser.getEntry(351);
      checkIdProperties(
        assertDefined(entry351.getChildByName(TransactionType.LAYER_DESTROYED)),
        undefined,
        62n,
        undefined,
        TransactionType.LAYER_DESTROYED,
      );
      checkIdProperties(
        assertDefined(
          entry351.getChildByName(TransactionType.LAYER_HANDLE_DESTROYED),
        ),
        undefined,
        62n,
        undefined,
        TransactionType.LAYER_HANDLE_DESTROYED,
      );
      checkIdProperties(
        assertDefined(entry351.getChildByName(TransactionType.NO_OP)),
        6841882902741n,
        undefined,
        undefined,
        TransactionType.NO_OP,
      );
    });

    it('fetches and translates flags', async () => {
      const entry0 = await parser.getEntry(0);
      const n0 = assertDefined(entry0.getChildByName('LayerState'));
      checkEagerProperty(n0, 'flagsId', 0n, 'eLayerChanged');

      const n1 = entry0.getAllChildren()[1];
      checkEagerProperty(
        n1,
        'flagsId',
        1n,
        'eFlagsChanged | eDestinationFrameChanged',
      );

      const entry222 = await parser.getEntry(222);
      const n2 = assertDefined(entry222.getChildByName('DisplayState'));
      const expectedFlags =
        'eLayerStackChanged | eDisplayProjectionChanged | eFlagsChanged';
      checkEagerProperty(n2, 'flagsId', 9n, expectedFlags);
    });

    it('fetches process properties', async () => {
      const transactions0 = (await parser.getEntry(0)).getAllChildren();
      const transactions1 = (await parser.getEntry(679)).getAllChildren();

      // translated due to uid/pid dual match though multiple uid=1003 in trace
      checkProcessProperties(transactions0[0], 515n, 1003n, 'process515');

      // not translated due to multiple uid=1003 processes in trace
      checkProcessProperties(transactions0[1], 0n, 1003n, undefined);

      // not translated due to missing process in trace
      checkProcessProperties(transactions1[0], 0n, 10239n, undefined);

      // translated due to uid/pid dual match
      checkProcessProperties(transactions1[3], 1593n, 1000n, 'process1593');

      // not translated due to zero uid/pid
      checkProcessProperties(transactions1[7], 0n, 0n, undefined);

      // translated due to single pid match
      checkProcessProperties(transactions1[8], 0n, 10169n, 'process3300');

      // translated due to single uid match
      checkProcessProperties(transactions1[9], 3300n, 0n, 'process3300');

      // not translated due to missing pid and uid
      checkProcessProperties(
        transactions1[11],
        undefined,
        undefined,
        undefined,
      );
    });

    function checkIdProperties(
      t: HierarchyTreeNode,
      txid: bigint | undefined,
      layerId: bigint | undefined,
      displayId: bigint | undefined,
      transactionType: TransactionType,
    ) {
      checkEagerProperty(t, 'transactionId', txid);
      checkEagerProperty(t, 'layerId', layerId);
      checkEagerProperty(t, 'displayId', displayId);
      checkEagerProperty(t, 'transactionType', transactionType);
    }

    function checkProcessProperties(
      t: HierarchyTreeNode,
      pid: bigint | undefined,
      uid: bigint | undefined,
      name: string | undefined,
    ) {
      checkEagerProperty(t, 'pid', pid);
      checkEagerProperty(t, 'uid', uid);
      checkEagerProperty(t, 'processName', name);
    }

    function checkEagerProperty(
      t: HierarchyTreeNode,
      name: string,
      val: any,
      formattedValue?: string,
    ) {
      const node = t.getEagerPropertyByName(name);
      expect(node?.getValue()).toEqual(val);
      if (formattedValue) {
        expect(node?.formattedValue()).toEqual(formattedValue);
      }
    }
  });

  describe('lazy property fetching', () => {
    it('transforms fake proto built from trace processor args', async () => {
      const entry0 = await parser.getEntry(0);

      expect(entry0.getChildByName('DisplayState')).toBeUndefined();

      const layerChange1 = await entry0.getAllChildren()[1].getAllProperties();

      // Add default values
      expect(layerChange1?.getChildByName('alpha')?.getValue()).toEqual(0);

      // Convert value types (bigint -> number)
      expect(layerChange1?.getChildByName('flags')?.getValue()).toEqual(256);

      // Decode enum IDs
      expect(
        layerChange1?.getChildByName('dropInputMode')?.formattedValue(),
      ).toEqual('NONE');

      const entry2 = await parser.getEntry(2);
      const layerChange2 = await entry2.getAllChildren()[0].getAllProperties();
      expect(
        layerChange2
          ?.getChildByName('bufferData')
          ?.getChildByName('pixelFormat')
          ?.formattedValue(),
      ).toEqual('PIXEL_FORMAT_RGBA_1010102');
    });

    it("decodes 'what' field", async () => {
      {
        const entry = await parser.getEntry(0);
        const layerChanges0 = await entry
          .getChildByName('LayerState')
          ?.getAllProperties();
        expect(layerChanges0?.getChildByName('what')?.formattedValue()).toEqual(
          'eLayerChanged',
        );
        const layerChanges1 = await entry
          .getAllChildren()[1]
          .getAllProperties();
        expect(layerChanges1?.getChildByName('what')?.formattedValue()).toEqual(
          'eFlagsChanged | eDestinationFrameChanged',
        );
      }
      {
        const entry = await parser.getEntry(222);
        const displayChanges = await entry
          .getChildByName('DisplayState')
          ?.getAllProperties();
        expect(
          displayChanges?.getChildByName('what')?.formattedValue(),
        ).toEqual(
          'eLayerStackChanged | eDisplayProjectionChanged | eFlagsChanged',
        );
      }
    });
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
