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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANYf KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {assertDefined} from 'common/assert_utils';
import {TracesBuilder} from 'test/unit/traces_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {RealTimestamp, TimestampType} from 'trace/timestamp';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {Presenter} from './presenter';
import {UiData, UiDataEntryType} from './ui_data';

describe('PresenterTransactions', () => {
  let parser: Parser<object>;
  let trace: Trace<object>;
  let traces: Traces;
  let presenter: Presenter;
  let outputUiData: undefined | UiData;
  const TOTAL_OUTPUT_ENTRIES = 1647;

  beforeAll(async () => {
    parser = await UnitTestUtils.getParser('traces/elapsed_and_real_timestamp/Transactions.pb');
  });

  beforeEach(async () => {
    outputUiData = undefined;
    await setUpTestEnvironment(TimestampType.ELAPSED);
  });

  it('is robust to empty trace', async () => {
    const traces = new TracesBuilder().setEntries(TraceType.TRANSACTIONS, []).build();
    presenter = new Presenter(traces, (data: UiData) => {
      outputUiData = data;
    });

    expect(outputUiData).toEqual(UiData.EMPTY);

    await presenter.onTracePositionUpdate(TracePosition.fromTimestamp(new RealTimestamp(10n)));
    expect(outputUiData).toEqual(UiData.EMPTY);
  });

  it('processes trace position update and computes output UI data', async () => {
    await presenter.onTracePositionUpdate(createTracePosition(0));

    expect(assertDefined(outputUiData).allPids).toEqual([
      'N/A',
      '0',
      '515',
      '1593',
      '2022',
      '2322',
      '2463',
      '3300',
    ]);
    expect(assertDefined(outputUiData).allUids).toEqual([
      'N/A',
      '1000',
      '1003',
      '10169',
      '10235',
      '10239',
    ]);
    expect(assertDefined(outputUiData).allTypes).toEqual([
      'DISPLAY_CHANGED',
      'LAYER_ADDED',
      'LAYER_CHANGED',
      'LAYER_DESTROYED',
      'LAYER_HANDLE_DESTROYED',
      'NO_OP',
    ]);

    expect(assertDefined(outputUiData).allTransactionIds.length).toEqual(1295);
    expect(assertDefined(outputUiData).allLayerAndDisplayIds.length).toEqual(117);

    expect(assertDefined(outputUiData).entries.length).toEqual(TOTAL_OUTPUT_ENTRIES);

    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(0);
    expect(assertDefined(outputUiData).selectedEntryIndex).toBeUndefined();
    expect(assertDefined(outputUiData).scrollToIndex).toEqual(0);
    expect(assertDefined(outputUiData).currentPropertiesTree).toBeDefined();
  });

  it('processes trace position update and updates current entry and scroll position', async () => {
    await presenter.onTracePositionUpdate(createTracePosition(0));
    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(0);
    expect(assertDefined(outputUiData).scrollToIndex).toEqual(0);

    await presenter.onTracePositionUpdate(createTracePosition(10));
    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(13);
    expect(assertDefined(outputUiData).scrollToIndex).toEqual(13);
  });

  it('filters entries according to transaction ID filter', () => {
    presenter.onIdFilterChanged('');
    expect(assertDefined(outputUiData).entries.length).toEqual(TOTAL_OUTPUT_ENTRIES);

    presenter.onIdFilterChanged('2211908157465');
    expect(
      new Set(assertDefined(outputUiData).entries.map((entry) => entry.transactionId))
    ).toEqual(new Set(['2211908157465']));
  });

  it('filters entries according to VSYNC ID filter', () => {
    presenter.onVSyncIdFilterChanged([]);
    expect(assertDefined(outputUiData).entries.length).toEqual(TOTAL_OUTPUT_ENTRIES);

    presenter.onVSyncIdFilterChanged(['1']);
    expect(new Set(assertDefined(outputUiData).entries.map((entry) => entry.vsyncId))).toEqual(
      new Set([1])
    );

    presenter.onVSyncIdFilterChanged(['1', '3', '10']);
    expect(new Set(assertDefined(outputUiData).entries.map((entry) => entry.vsyncId))).toEqual(
      new Set([1, 3, 10])
    );
  });

  it('filters entries according to PID filter', () => {
    presenter.onPidFilterChanged([]);
    expect(new Set(assertDefined(outputUiData).entries.map((entry) => entry.pid))).toEqual(
      new Set(['N/A', '0', '515', '1593', '2022', '2322', '2463', '3300'])
    );

    presenter.onPidFilterChanged(['0']);
    expect(new Set(assertDefined(outputUiData).entries.map((entry) => entry.pid))).toEqual(
      new Set(['0'])
    );

    presenter.onPidFilterChanged(['0', '515']);
    expect(new Set(assertDefined(outputUiData).entries.map((entry) => entry.pid))).toEqual(
      new Set(['0', '515'])
    );
  });

  it('filters entries according to UID filter', () => {
    presenter.onUidFilterChanged([]);
    expect(new Set(assertDefined(outputUiData).entries.map((entry) => entry.uid))).toEqual(
      new Set(['N/A', '1000', '1003', '10169', '10235', '10239'])
    );

    presenter.onUidFilterChanged(['1000']);
    expect(new Set(assertDefined(outputUiData).entries.map((entry) => entry.uid))).toEqual(
      new Set(['1000'])
    );

    presenter.onUidFilterChanged(['1000', '1003']);
    expect(new Set(assertDefined(outputUiData).entries.map((entry) => entry.uid))).toEqual(
      new Set(['1000', '1003'])
    );
  });

  it('filters entries according to type filter', () => {
    presenter.onTypeFilterChanged([]);
    expect(new Set(assertDefined(outputUiData).entries.map((entry) => entry.type))).toEqual(
      new Set([
        UiDataEntryType.DISPLAY_CHANGED,
        UiDataEntryType.LAYER_ADDED,
        UiDataEntryType.LAYER_CHANGED,
        UiDataEntryType.LAYER_DESTROYED,
        UiDataEntryType.LAYER_HANDLE_DESTROYED,
        UiDataEntryType.NO_OP,
      ])
    );

    presenter.onTypeFilterChanged([UiDataEntryType.LAYER_ADDED]);
    expect(new Set(assertDefined(outputUiData).entries.map((entry) => entry.type))).toEqual(
      new Set([UiDataEntryType.LAYER_ADDED])
    );

    presenter.onTypeFilterChanged([UiDataEntryType.LAYER_ADDED, UiDataEntryType.LAYER_DESTROYED]);
    expect(new Set(assertDefined(outputUiData).entries.map((entry) => entry.type))).toEqual(
      new Set([UiDataEntryType.LAYER_ADDED, UiDataEntryType.LAYER_DESTROYED])
    );
  });

  it('filters entries according to layer or display ID filter', () => {
    presenter.onLayerIdFilterChanged([]);
    expect(
      new Set(assertDefined(outputUiData).entries.map((entry) => entry.layerOrDisplayId)).size
    ).toBeGreaterThan(20);

    presenter.onLayerIdFilterChanged(['1']);
    expect(
      new Set(assertDefined(outputUiData).entries.map((entry) => entry.layerOrDisplayId))
    ).toEqual(new Set(['1']));

    presenter.onLayerIdFilterChanged(['1', '3']);
    expect(
      new Set(assertDefined(outputUiData).entries.map((entry) => entry.layerOrDisplayId))
    ).toEqual(new Set(['1', '3']));
  });

  it('includes no op transitions', () => {
    presenter.onTypeFilterChanged([UiDataEntryType.NO_OP]);
    expect(new Set(assertDefined(outputUiData).entries.map((entry) => entry.type))).toEqual(
      new Set([UiDataEntryType.NO_OP])
    );

    for (const entry of assertDefined(outputUiData).entries) {
      expect(entry.layerOrDisplayId).toEqual('');
      expect(entry.what).toEqual('');
      expect(entry.propertiesTree).toEqual({});
    }
  });

  it('filters entries according to "what" search string', () => {
    expect(assertDefined(outputUiData).entries.length).toEqual(TOTAL_OUTPUT_ENTRIES);

    presenter.onWhatSearchStringChanged('');
    expect(assertDefined(outputUiData).entries.length).toEqual(TOTAL_OUTPUT_ENTRIES);

    presenter.onWhatSearchStringChanged('Crop');
    expect(assertDefined(outputUiData).entries.length).toBeLessThan(TOTAL_OUTPUT_ENTRIES);

    presenter.onWhatSearchStringChanged('STRING_WITH_NO_MATCHES');
    expect(assertDefined(outputUiData).entries.length).toEqual(0);
  });

  it('updates selected entry and properties tree when entry is clicked', async () => {
    await presenter.onTracePositionUpdate(createTracePosition(0));
    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(0);
    expect(assertDefined(outputUiData).selectedEntryIndex).toBeUndefined();
    expect(assertDefined(outputUiData).scrollToIndex).toEqual(0);
    expect(assertDefined(outputUiData).currentPropertiesTree).toEqual(
      assertDefined(outputUiData).entries[0].propertiesTree
    );

    presenter.onEntryClicked(10);
    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(0);
    expect(assertDefined(outputUiData).selectedEntryIndex).toEqual(10);
    expect(assertDefined(outputUiData).scrollToIndex).toBeUndefined(); // no scrolling
    expect(assertDefined(outputUiData).currentPropertiesTree).toEqual(
      assertDefined(outputUiData).entries[10].propertiesTree
    );

    // remove selection when selected entry is clicked again
    presenter.onEntryClicked(10);
    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(0);
    expect(assertDefined(outputUiData).selectedEntryIndex).toBeUndefined();
    expect(assertDefined(outputUiData).scrollToIndex).toBeUndefined(); // no scrolling
    expect(assertDefined(outputUiData).currentPropertiesTree).toEqual(
      assertDefined(outputUiData).entries[0].propertiesTree
    );
  });

  it('computes current entry index', async () => {
    await presenter.onTracePositionUpdate(createTracePosition(0));
    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(0);

    await presenter.onTracePositionUpdate(createTracePosition(10));
    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(13);
  });

  it('updates current entry index when filters change', async () => {
    await presenter.onTracePositionUpdate(createTracePosition(10));

    presenter.onPidFilterChanged([]);
    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(13);

    presenter.onPidFilterChanged(['0']);
    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(10);

    presenter.onPidFilterChanged(['0', '515']);
    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(11);

    presenter.onPidFilterChanged(['0', '515', 'N/A']);
    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(13);
  });

  it('formats real time', async () => {
    await setUpTestEnvironment(TimestampType.REAL);
    expect(assertDefined(outputUiData).entries[0].time).toEqual('2022-08-03T06:19:01.051480997');
  });

  it('formats elapsed time', async () => {
    await setUpTestEnvironment(TimestampType.ELAPSED);
    expect(assertDefined(outputUiData).entries[0].time).toEqual('2s450ms981445ns');
  });

  const setUpTestEnvironment = async (timestampType: TimestampType) => {
    trace = new TraceBuilder<object>().setParser(parser).setTimestampType(timestampType).build();

    traces = new Traces();
    traces.setTrace(TraceType.TRANSACTIONS, trace);

    presenter = new Presenter(traces, (data: UiData) => {
      outputUiData = data;
    });

    await presenter.onTracePositionUpdate(createTracePosition(0)); // trigger initialization
  };

  const createTracePosition = (entryIndex: number): TracePosition => {
    return TracePosition.fromTraceEntry(trace.getEntry(entryIndex));
  };
});
