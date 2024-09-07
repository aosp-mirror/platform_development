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
import {InMemoryStorage} from 'common/in_memory_storage';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {Presenter} from './presenter';
import {UiData, UiDataEntryType} from './ui_data';

describe('PresenterTransactions', () => {
  let parser: Parser<PropertyTreeNode>;
  let trace: Trace<PropertyTreeNode>;
  let presenter: Presenter;
  let outputUiData: undefined | UiData;
  const TOTAL_OUTPUT_ENTRIES = 1647;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(TreeNodeUtils.treeNodeEqualityTester);
    parser = (await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/Transactions.pb',
    )) as Parser<PropertyTreeNode>;
  });

  beforeEach(async () => {
    outputUiData = undefined;
    await setUpTestEnvironment();
  });

  it('is robust to empty trace', async () => {
    const trace = new TraceBuilder<PropertyTreeNode>().setEntries([]).build();
    presenter = new Presenter(trace, new InMemoryStorage(), (data: UiData) => {
      outputUiData = data;
    });

    expect(outputUiData).toEqual(UiData.EMPTY);

    const expectedUiData = UiData.EMPTY;
    expectedUiData.propertiesUserOptions = {
      showDefaults: {
        name: 'Show defaults',
        enabled: false,
        tooltip: `
                If checked, shows the value of all properties.
                Otherwise, hides all properties whose value is
                the default for its data type.
              `,
      },
    };
    await presenter.onAppEvent(
      TracePositionUpdate.fromTimestamp(
        TimestampConverterUtils.makeRealTimestamp(10n),
      ),
    );
    expect(outputUiData).toEqual(UiData.EMPTY);
  });

  it('processes trace position update and computes output UI data', async () => {
    await presenter.onAppEvent(createTracePositionUpdate(0));
    checkInitialTracePositionUpdate();

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
    expect(assertDefined(outputUiData).allLayerAndDisplayIds.length).toEqual(
      117,
    );
    expect(assertDefined(outputUiData).entries.length).toEqual(
      TOTAL_OUTPUT_ENTRIES,
    );
  });

  it('processes trace position update and updates current entry and scroll position', async () => {
    await presenter.onAppEvent(createTracePositionUpdate(0));
    checkInitialTracePositionUpdate();

    await presenter.onAppEvent(createTracePositionUpdate(10));
    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(13);
    expect(assertDefined(outputUiData).scrollToIndex).toEqual(13);
  });

  it('filters entries according to transaction ID filter', () => {
    presenter.onTransactionIdFilterChanged([]);
    expect(assertDefined(outputUiData).entries.length).toEqual(
      TOTAL_OUTPUT_ENTRIES,
    );

    presenter.onTransactionIdFilterChanged(['2211908157465']);
    expect(
      new Set(
        assertDefined(outputUiData).entries.map((entry) => entry.transactionId),
      ),
    ).toEqual(new Set(['2211908157465']));
  });

  it('filters entries according to VSYNC ID filter', () => {
    presenter.onVSyncIdFilterChanged([]);
    expect(assertDefined(outputUiData).entries.length).toEqual(
      TOTAL_OUTPUT_ENTRIES,
    );

    presenter.onVSyncIdFilterChanged(['1']);
    expect(
      new Set(
        assertDefined(outputUiData).entries.map((entry) => entry.vsyncId),
      ),
    ).toEqual(new Set([1]));

    presenter.onVSyncIdFilterChanged(['1', '3', '10']);
    expect(
      new Set(
        assertDefined(outputUiData).entries.map((entry) => entry.vsyncId),
      ),
    ).toEqual(new Set([1, 3, 10]));
  });

  it('filters entries according to PID filter', () => {
    presenter.onPidFilterChanged([]);
    expect(
      new Set(assertDefined(outputUiData).entries.map((entry) => entry.pid)),
    ).toEqual(
      new Set(['N/A', '0', '515', '1593', '2022', '2322', '2463', '3300']),
    );

    presenter.onPidFilterChanged(['0']);
    expect(
      new Set(assertDefined(outputUiData).entries.map((entry) => entry.pid)),
    ).toEqual(new Set(['0']));

    presenter.onPidFilterChanged(['0', '515']);
    expect(
      new Set(assertDefined(outputUiData).entries.map((entry) => entry.pid)),
    ).toEqual(new Set(['0', '515']));
  });

  it('filters entries according to UID filter', () => {
    presenter.onUidFilterChanged([]);
    expect(
      new Set(assertDefined(outputUiData).entries.map((entry) => entry.uid)),
    ).toEqual(new Set(['N/A', '1000', '1003', '10169', '10235', '10239']));

    presenter.onUidFilterChanged(['1000']);
    expect(
      new Set(assertDefined(outputUiData).entries.map((entry) => entry.uid)),
    ).toEqual(new Set(['1000']));

    presenter.onUidFilterChanged(['1000', '1003']);
    expect(
      new Set(assertDefined(outputUiData).entries.map((entry) => entry.uid)),
    ).toEqual(new Set(['1000', '1003']));
  });

  it('filters entries according to type filter', () => {
    presenter.onTypeFilterChanged([]);
    expect(
      new Set(assertDefined(outputUiData).entries.map((entry) => entry.type)),
    ).toEqual(
      new Set([
        UiDataEntryType.DISPLAY_CHANGED,
        UiDataEntryType.LAYER_ADDED,
        UiDataEntryType.LAYER_CHANGED,
        UiDataEntryType.LAYER_DESTROYED,
        UiDataEntryType.LAYER_HANDLE_DESTROYED,
        UiDataEntryType.NO_OP,
      ]),
    );

    presenter.onTypeFilterChanged([UiDataEntryType.LAYER_ADDED]);
    expect(
      new Set(assertDefined(outputUiData).entries.map((entry) => entry.type)),
    ).toEqual(new Set([UiDataEntryType.LAYER_ADDED]));

    presenter.onTypeFilterChanged([
      UiDataEntryType.LAYER_ADDED,
      UiDataEntryType.LAYER_DESTROYED,
    ]);
    expect(
      new Set(assertDefined(outputUiData).entries.map((entry) => entry.type)),
    ).toEqual(
      new Set([UiDataEntryType.LAYER_ADDED, UiDataEntryType.LAYER_DESTROYED]),
    );
  });

  it('filters entries according to layer or display ID filter', () => {
    presenter.onLayerIdFilterChanged([]);
    expect(
      new Set(
        assertDefined(outputUiData).entries.map(
          (entry) => entry.layerOrDisplayId,
        ),
      ).size,
    ).toBeGreaterThan(20);

    presenter.onLayerIdFilterChanged(['1']);
    expect(
      new Set(
        assertDefined(outputUiData).entries.map(
          (entry) => entry.layerOrDisplayId,
        ),
      ),
    ).toEqual(new Set(['1']));

    presenter.onLayerIdFilterChanged(['1', '3']);
    expect(
      new Set(
        assertDefined(outputUiData).entries.map(
          (entry) => entry.layerOrDisplayId,
        ),
      ),
    ).toEqual(new Set(['1', '3']));
  });

  it('includes no op transitions', () => {
    presenter.onTypeFilterChanged([UiDataEntryType.NO_OP]);
    expect(
      new Set(assertDefined(outputUiData).entries.map((entry) => entry.type)),
    ).toEqual(new Set([UiDataEntryType.NO_OP]));

    for (const entry of assertDefined(outputUiData).entries) {
      expect(entry.layerOrDisplayId).toEqual('');
      expect(entry.what).toEqual('');
      expect(entry.propertiesTree).toEqual(undefined);
    }
  });

  it('filters entries according to "what" search string', () => {
    expect(assertDefined(outputUiData).entries.length).toEqual(
      TOTAL_OUTPUT_ENTRIES,
    );

    presenter.onWhatFilterChanged([]);
    expect(assertDefined(outputUiData).entries.length).toEqual(
      TOTAL_OUTPUT_ENTRIES,
    );

    presenter.onWhatFilterChanged(['Crop']);
    expect(assertDefined(outputUiData).entries.length).toBeLessThan(
      TOTAL_OUTPUT_ENTRIES,
    );

    presenter.onWhatFilterChanged(['STRING_WITH_NO_MATCHES']);
    expect(assertDefined(outputUiData).entries.length).toEqual(0);
  });

  it('updates selected entry ui data when entry clicked', async () => {
    await presenter.onAppEvent(createTracePositionUpdate(0));
    checkInitialTracePositionUpdate();

    const newIndex = 10;
    presenter.onEntryClicked(newIndex);
    checkSelectedEntryUiData(newIndex);
    expect(assertDefined(outputUiData).scrollToIndex).toEqual(undefined); // no scrolling

    // does not remove selection when entry clicked again
    presenter.onEntryClicked(newIndex);
    checkSelectedEntryUiData(newIndex);
    expect(assertDefined(outputUiData).scrollToIndex).toEqual(undefined);
  });

  it('updates selected entry ui data when entry changed by key press', async () => {
    await presenter.onAppEvent(createTracePositionUpdate(0));
    checkInitialTracePositionUpdate();

    const newIndex = 10;
    presenter.onEntryChangedByKeyPress(newIndex);
    checkSelectedEntryUiData(newIndex);
    expect(assertDefined(outputUiData).scrollToIndex).toEqual(newIndex);
  });

  it('computes current entry index', async () => {
    await presenter.onAppEvent(createTracePositionUpdate(0));
    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(0);

    await presenter.onAppEvent(createTracePositionUpdate(10));
    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(13);
  });

  it('updates current entry index when filters change', async () => {
    await presenter.onAppEvent(createTracePositionUpdate(10));

    presenter.onPidFilterChanged([]);
    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(13);

    presenter.onPidFilterChanged(['0']);
    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(10);

    presenter.onPidFilterChanged(['0', '515']);
    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(11);

    presenter.onPidFilterChanged(['0', '515', 'N/A']);
    expect(assertDefined(outputUiData).currentEntryIndex).toEqual(13);
  });

  it('formats entry time', async () => {
    await setUpTestEnvironment();
    expect(
      assertDefined(outputUiData).entries[0].time.formattedValue(),
    ).toEqual('2022-08-03, 06:19:01.051');
  });

  async function setUpTestEnvironment() {
    trace = new TraceBuilder<PropertyTreeNode>().setParser(parser).build();
    presenter = new Presenter(trace, new InMemoryStorage(), (data: UiData) => {
      outputUiData = data;
    });
    await presenter.onAppEvent(createTracePositionUpdate(0)); // trigger initialization
  }

  function createTracePositionUpdate(entryIndex: number): TracePositionUpdate {
    const entry = trace.getEntry(entryIndex);
    return TracePositionUpdate.fromTraceEntry(entry);
  }

  function checkInitialTracePositionUpdate() {
    const uiData = assertDefined(outputUiData);
    expect(uiData.currentEntryIndex).toEqual(0);
    expect(uiData.selectedEntryIndex).toBeUndefined();
    expect(uiData.scrollToIndex).toEqual(0);
    expect(assertDefined(uiData.currentPropertiesTree).id).toEqual(
      assertDefined(uiData.entries[0].propertiesTree).id,
    );
  }

  function checkSelectedEntryUiData(index: number) {
    const uiData = assertDefined(outputUiData);
    expect(uiData.currentEntryIndex).toEqual(0);
    expect(uiData.selectedEntryIndex).toEqual(index);
    expect(assertDefined(uiData.currentPropertiesTree).id).toEqual(
      assertDefined(uiData.entries[index].propertiesTree).id,
    );
  }
});
