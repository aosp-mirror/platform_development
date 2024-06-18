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
import {NotifyLogViewCallbackType} from 'viewers/common/abstract_log_viewer_presenter';
import {
  LogEntry,
  LogFieldName,
  LogFieldValue,
} from 'viewers/common/ui_data_log';
import {Presenter} from './presenter';
import {TransactionsEntryType, UiData} from './ui_data';

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
    const emptyTrace = new TraceBuilder<PropertyTreeNode>()
      .setEntries([])
      .build();
    let outputData: UiData | undefined;
    const notifyViewCallback = (data: UiData) => {
      outputData = data;
    };

    const presenterEmptyTrace = new Presenter(
      emptyTrace,
      new InMemoryStorage(),
      notifyViewCallback as NotifyLogViewCallbackType,
    );
    expect(outputData).toEqual(UiData.EMPTY);

    await presenterEmptyTrace.onAppEvent(
      TracePositionUpdate.fromTimestamp(
        TimestampConverterUtils.makeRealTimestamp(10n),
      ),
    );
    expect(outputData).toEqual(UiData.EMPTY);
  });

  it('processes trace position update and computes output UI data', async () => {
    await presenter.onAppEvent(createTracePositionUpdate(0));
    checkInitialTracePositionUpdate();

    expect(getFilterOptions(LogFieldName.PID)).toEqual([
      'N/A',
      '0',
      '515',
      '1593',
      '2022',
      '2322',
      '2463',
      '3300',
    ]);
    expect(getFilterOptions(LogFieldName.UID)).toEqual([
      'N/A',
      '1000',
      '1003',
      '10169',
      '10235',
      '10239',
    ]);
    expect(getFilterOptions(LogFieldName.TRANSACTION_TYPE)).toEqual([
      'DISPLAY_CHANGED',
      'LAYER_ADDED',
      'LAYER_CHANGED',
      'LAYER_DESTROYED',
      'LAYER_HANDLE_DESTROYED',
      'NO_OP',
    ]);

    expect(getFilterOptions(LogFieldName.TRANSACTION_ID).length).toEqual(1295);
    expect(getFilterOptions(LogFieldName.LAYER_OR_DISPLAY_ID).length).toEqual(
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
    expect(assertDefined(outputUiData).currentIndex).toEqual(13);
    expect(assertDefined(outputUiData).scrollToIndex).toEqual(13);
  });

  it('filters entries according to transaction ID filter', async () => {
    await checkFilter(
      LogFieldName.TRANSACTION_ID,
      [],
      undefined,
      TOTAL_OUTPUT_ENTRIES,
    );
    await checkFilter(
      LogFieldName.TRANSACTION_ID,
      ['2211908157465'],
      ['2211908157465'],
    );
  });

  it('filters entries according to VSYNC ID filter', async () => {
    await checkFilter(
      LogFieldName.VSYNC_ID,
      [],
      undefined,
      TOTAL_OUTPUT_ENTRIES,
    );
    await checkFilter(LogFieldName.VSYNC_ID, ['1'], [1]);
    await checkFilter(LogFieldName.VSYNC_ID, ['1', '3', '10'], [1, 3, 10]);
  });

  it('filters entries according to PID filter', async () => {
    await checkFilter(
      LogFieldName.PID,
      [],
      ['N/A', '0', '515', '1593', '2022', '2322', '2463', '3300'],
    );
    await checkFilter(LogFieldName.PID, ['0'], ['0']);
    await checkFilter(LogFieldName.PID, ['0', '515'], ['0', '515']);
  });

  it('filters entries according to UID filter', async () => {
    await checkFilter(
      LogFieldName.UID,
      [],
      ['N/A', '1000', '1003', '10169', '10235', '10239'],
    );
    await checkFilter(LogFieldName.UID, ['1000'], ['1000']);
    await checkFilter(LogFieldName.UID, ['1000', '1003'], ['1000', '1003']);
  });

  it('filters entries according to type filter', async () => {
    await checkFilter(
      LogFieldName.TRANSACTION_TYPE,
      [],
      [
        TransactionsEntryType.DISPLAY_CHANGED,
        TransactionsEntryType.LAYER_ADDED,
        TransactionsEntryType.LAYER_CHANGED,
        TransactionsEntryType.LAYER_DESTROYED,
        TransactionsEntryType.LAYER_HANDLE_DESTROYED,
        TransactionsEntryType.NO_OP,
      ],
    );
    await checkFilter(
      LogFieldName.TRANSACTION_TYPE,
      [TransactionsEntryType.LAYER_ADDED],
      [TransactionsEntryType.LAYER_ADDED],
    );
    await checkFilter(
      LogFieldName.TRANSACTION_TYPE,
      [
        TransactionsEntryType.LAYER_ADDED,
        TransactionsEntryType.LAYER_DESTROYED,
      ],
      [
        TransactionsEntryType.LAYER_ADDED,
        TransactionsEntryType.LAYER_DESTROYED,
      ],
    );
  });

  it('filters entries according to layer or display ID filter', async () => {
    await checkFilter(
      LogFieldName.LAYER_OR_DISPLAY_ID,
      [],
      undefined,
      TOTAL_OUTPUT_ENTRIES,
    );
    await checkFilter(LogFieldName.LAYER_OR_DISPLAY_ID, ['1'], ['1']);
    await checkFilter(LogFieldName.LAYER_OR_DISPLAY_ID, ['1', '3'], ['1', '3']);
  });

  it('includes no op transitions', async () => {
    await checkFilter(
      LogFieldName.TRANSACTION_TYPE,
      [TransactionsEntryType.NO_OP],
      [TransactionsEntryType.NO_OP],
    );

    for (const entry of assertDefined(outputUiData).entries) {
      expect(getFieldValue(entry, LogFieldName.LAYER_OR_DISPLAY_ID)).toEqual(
        '',
      );
      expect(getFieldValue(entry, LogFieldName.FLAGS)).toEqual('');
      expect(entry.propertiesTree).toEqual(undefined);
    }
  });

  it('filters entries according to flags filter', async () => {
    expect(assertDefined(outputUiData).entries.length).toEqual(
      TOTAL_OUTPUT_ENTRIES,
    );

    await presenter.onFilterChange(LogFieldName.FLAGS, []);
    expect(assertDefined(outputUiData).entries.length).toEqual(
      TOTAL_OUTPUT_ENTRIES,
    );

    await presenter.onFilterChange(LogFieldName.FLAGS, ['Crop']);
    expect(assertDefined(outputUiData).entries.length).toEqual(980);

    await presenter.onFilterChange(LogFieldName.FLAGS, [
      'STRING_WITH_NO_MATCHES',
    ]);
    expect(assertDefined(outputUiData).entries.length).toEqual(0);
  });

  it('updates selected entry ui data when entry clicked', async () => {
    await presenter.onAppEvent(createTracePositionUpdate(0));
    checkInitialTracePositionUpdate();

    const newIndex = 10;
    await presenter.onLogEntryClick(newIndex);
    checkSelectedEntryUiData(newIndex);
    expect(assertDefined(outputUiData).scrollToIndex).toEqual(undefined); // no scrolling

    // does not remove selection when entry clicked again
    await presenter.onLogEntryClick(newIndex);
    checkSelectedEntryUiData(newIndex);
    expect(assertDefined(outputUiData).scrollToIndex).toEqual(undefined);
  });

  it('updates selected entry ui data when entry changed by key press', async () => {
    await presenter.onAppEvent(createTracePositionUpdate(0));
    checkInitialTracePositionUpdate();
    expect(assertDefined(outputUiData).selectedIndex).toEqual(undefined);

    await presenter.onArrowDownPress();
    checkSelectedAndScrollIndices(1);

    await presenter.onArrowUpPress();
    checkSelectedAndScrollIndices(0);

    // robust to attempt to go before first entry
    await presenter.onArrowUpPress();
    checkSelectedAndScrollIndices(0);

    // robust to attempt to go beyond last entry
    const finalIndex = assertDefined(outputUiData).entries.length - 1;
    await presenter.onLogEntryClick(finalIndex);
    await presenter.onArrowDownPress();
    expect(assertDefined(outputUiData).selectedIndex).toEqual(finalIndex);
  });

  it('computes current index', async () => {
    await presenter.onAppEvent(createTracePositionUpdate(0));
    expect(assertDefined(outputUiData).currentIndex).toEqual(0);

    await presenter.onAppEvent(createTracePositionUpdate(10));
    expect(assertDefined(outputUiData).currentIndex).toEqual(13);
  });

  it('updates current index when filters change', async () => {
    await presenter.onAppEvent(createTracePositionUpdate(10));

    await presenter.onFilterChange(LogFieldName.PID, []);
    expect(assertDefined(outputUiData).currentIndex).toEqual(13);

    await presenter.onFilterChange(LogFieldName.PID, ['0']);
    expect(assertDefined(outputUiData).currentIndex).toEqual(10);

    await presenter.onFilterChange(LogFieldName.PID, ['0', '515']);
    expect(assertDefined(outputUiData).currentIndex).toEqual(11);

    await presenter.onFilterChange(LogFieldName.PID, ['0', '515', 'N/A']);
    expect(assertDefined(outputUiData).currentIndex).toEqual(13);
  });

  async function setUpTestEnvironment() {
    trace = new TraceBuilder<PropertyTreeNode>().setParser(parser).build();
    presenter = new Presenter(trace, new InMemoryStorage(), ((data: UiData) => {
      outputUiData = data;
    }) as NotifyLogViewCallbackType);
    await presenter.onAppEvent(createTracePositionUpdate(0)); // trigger initialization
  }

  function createTracePositionUpdate(entryIndex: number): TracePositionUpdate {
    const entry = trace.getEntry(entryIndex);
    return TracePositionUpdate.fromTraceEntry(entry);
  }

  function checkInitialTracePositionUpdate() {
    const uiData = assertDefined(outputUiData);
    expect(uiData.currentIndex).toEqual(0);
    expect(uiData.selectedIndex).toBeUndefined();
    expect(uiData.scrollToIndex).toEqual(0);
    expect(assertDefined(uiData.propertiesTree).id).toEqual(
      assertDefined(uiData.entries[0].propertiesTree).id,
    );
  }

  function checkSelectedEntryUiData(index: number | undefined) {
    const uiData = assertDefined(outputUiData);
    expect(uiData.currentIndex).toEqual(0);
    expect(uiData.selectedIndex).toEqual(index);
    if (index !== undefined) {
      expect(assertDefined(uiData.propertiesTree).id).toEqual(
        assertDefined(uiData.entries[index].propertiesTree).id,
      );
    }
  }

  function checkSelectedAndScrollIndices(index: number | undefined) {
    checkSelectedEntryUiData(index);
    expect(assertDefined(outputUiData).scrollToIndex).toEqual(index);
  }

  function getFilterOptions(logFieldName: LogFieldName): string[] {
    return assertDefined(
      outputUiData?.filters.find((f) => f.name === logFieldName)?.options,
    );
  }

  async function checkFilter(
    name: LogFieldName,
    filterValues: string[],
    expectedFieldValues?: LogFieldValue[],
    numberOfFieldValues?: number,
  ) {
    await presenter.onFilterChange(name, filterValues);
    const fieldValues = assertDefined(outputUiData).entries.map((entry) =>
      getFieldValue(entry, name),
    );
    if (expectedFieldValues !== undefined) {
      expect(new Set(fieldValues)).toEqual(new Set(expectedFieldValues));
    } else if (numberOfFieldValues !== undefined) {
      expect(fieldValues.length).toEqual(numberOfFieldValues);
    }
  }

  function getFieldValue(entry: LogEntry, logFieldName: LogFieldName) {
    return entry.fields.find((f) => f.name === logFieldName)?.value;
  }
});
