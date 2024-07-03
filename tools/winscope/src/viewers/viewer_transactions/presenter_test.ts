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
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {NotifyLogViewCallbackType} from 'viewers/common/abstract_log_viewer_presenter';
import {AbstractLogViewerPresenterTest} from 'viewers/common/abstract_log_viewer_presenter_test';
import {
  LogEntry,
  LogFieldType,
  LogFieldValue,
} from 'viewers/common/ui_data_log';
import {Presenter} from './presenter';
import {TransactionsEntryType, UiData} from './ui_data';

class PresenterTransactionsTest extends AbstractLogViewerPresenterTest {
  private trace: Trace<PropertyTreeNode> | undefined;
  private positionUpdate: TracePositionUpdate | undefined;
  private secondPositionUpdate: TracePositionUpdate | undefined;

  override readonly shouldExecuteHeaderTests = false;
  override readonly shouldExecuteFilterTests = true;
  override readonly shouldExecuteCurrentIndexTests = true;
  override readonly shouldExecutePropertiesTests = true;

  override readonly totalOutputEntries = 1647;
  override readonly expectedIndexOfSecondPositionUpdate = 13;
  override readonly expectedInitialFilterOptions = new Map<
    LogFieldType,
    string[] | number
  >([
    [
      LogFieldType.PID,
      ['N/A', '0', '515', '1593', '2022', '2322', '2463', '3300'],
    ],
    [LogFieldType.UID, ['N/A', '1000', '1003', '10169', '10235', '10239']],
    [
      LogFieldType.TRANSACTION_TYPE,
      [
        'DISPLAY_CHANGED',
        'LAYER_ADDED',
        'LAYER_CHANGED',
        'LAYER_DESTROYED',
        'LAYER_HANDLE_DESTROYED',
        'NO_OP',
      ],
    ],
    [LogFieldType.TRANSACTION_ID, 1295],
    [LogFieldType.LAYER_OR_DISPLAY_ID, 117],
  ]);
  override readonly filterValuesToSet = new Map<LogFieldType, string[][]>([
    [LogFieldType.TRANSACTION_ID, [[], ['2211908157465']]],
    [LogFieldType.VSYNC_ID, [[], ['1'], ['1', '3', '10']]],
    [LogFieldType.PID, [[], ['0'], ['0', '515']]],
    [LogFieldType.UID, [[], ['1000'], ['1000', '1003']]],
    [
      LogFieldType.TRANSACTION_TYPE,
      [
        [],
        [TransactionsEntryType.LAYER_ADDED],
        [
          TransactionsEntryType.LAYER_ADDED,
          TransactionsEntryType.LAYER_DESTROYED,
        ],
      ],
    ],
    [LogFieldType.LAYER_OR_DISPLAY_ID, [[], ['1'], ['1', '3']]],
    [LogFieldType.FLAGS, [[], ['Crop'], ['STRING_WITH_NO_MATCHES']]],
  ]);
  override readonly expectedFieldValuesAfterFilter = new Map<
    LogFieldType,
    Array<LogFieldValue[] | number>
  >([
    [LogFieldType.TRANSACTION_ID, [this.totalOutputEntries, ['2211908157465']]],
    [LogFieldType.VSYNC_ID, [this.totalOutputEntries, [1], [1, 3, 10]]],
    [
      LogFieldType.PID,
      [
        ['N/A', '0', '515', '1593', '2022', '2322', '2463', '3300'],
        ['0'],
        ['0', '515'],
      ],
    ],
    [
      LogFieldType.UID,
      [
        ['N/A', '1000', '1003', '10169', '10235', '10239'],
        ['1000'],
        ['1000', '1003'],
      ],
    ],
    [
      LogFieldType.TRANSACTION_TYPE,
      [
        [
          TransactionsEntryType.DISPLAY_CHANGED,
          TransactionsEntryType.LAYER_ADDED,
          TransactionsEntryType.LAYER_CHANGED,
          TransactionsEntryType.LAYER_DESTROYED,
          TransactionsEntryType.LAYER_HANDLE_DESTROYED,
          TransactionsEntryType.NO_OP,
        ],
        [TransactionsEntryType.LAYER_ADDED],
        [
          TransactionsEntryType.LAYER_ADDED,
          TransactionsEntryType.LAYER_DESTROYED,
        ],
      ],
    ],
    [
      LogFieldType.LAYER_OR_DISPLAY_ID,
      [this.totalOutputEntries, ['1'], ['1', '3']],
    ],
    [LogFieldType.FLAGS, [this.totalOutputEntries, 980, 0]],
  ]);
  override readonly logEntryClickIndex = 10;
  override readonly filterNameForCurrentIndexTest = LogFieldType.PID;
  override readonly filterChangeForCurrentIndexTest = ['0'];
  override readonly secondFilterChangeForCurrentIndexTest = ['0', '515'];
  override readonly expectedCurrentIndexAfterFilterChange = 10;
  override readonly expectedCurrentIndexAfterSecondFilterChange = 11;

  override executeSpecializedTests() {
    describe('Specialized tests', () => {
      let presenter: Presenter;
      let uiData: UiData;

      beforeAll(async () => {
        await this.setUpTestEnvironment();
      });

      beforeEach(async () => {
        const notifyViewCallback = (newData: UiData) => {
          uiData = newData;
        };
        presenter = await this.createPresenter(
          notifyViewCallback as NotifyLogViewCallbackType,
        );
      });

      it('includes no op transitions', async () => {
        await presenter.onFilterChange(LogFieldType.TRANSACTION_TYPE, [
          TransactionsEntryType.NO_OP,
        ]);
        const fieldValues = assertDefined(uiData).entries.map((entry) =>
          getFieldValue(entry, LogFieldType.TRANSACTION_TYPE),
        );
        expect(new Set(fieldValues)).toEqual(
          new Set([TransactionsEntryType.NO_OP]),
        );

        for (const entry of assertDefined(uiData).entries) {
          expect(
            getFieldValue(entry, LogFieldType.LAYER_OR_DISPLAY_ID),
          ).toEqual('');
          expect(getFieldValue(entry, LogFieldType.FLAGS)).toEqual('');
          expect(entry.propertiesTree).toEqual(undefined);
        }
      });

      function getFieldValue(entry: LogEntry, logFieldName: LogFieldType) {
        return entry.fields.find((f) => f.type === logFieldName)?.value;
      }
    });
  }

  override async setUpTestEnvironment(): Promise<void> {
    const parser = (await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/Transactions.pb',
    )) as Parser<PropertyTreeNode>;
    this.trace = new TraceBuilder<PropertyTreeNode>().setParser(parser).build();
    this.positionUpdate = TracePositionUpdate.fromTraceEntry(
      this.trace.getEntry(0),
    );
    this.secondPositionUpdate = TracePositionUpdate.fromTraceEntry(
      this.trace.getEntry(10),
    );
  }

  override createPresenterWithEmptyTrace(
    callback: NotifyLogViewCallbackType,
  ): Presenter {
    const emptyTrace = new TraceBuilder<PropertyTreeNode>()
      .setEntries([])
      .build();
    return new Presenter(emptyTrace, new InMemoryStorage(), callback);
  }

  override async createPresenter(
    callback: NotifyLogViewCallbackType,
  ): Promise<Presenter> {
    const presenter = new Presenter(
      assertDefined(this.trace),
      new InMemoryStorage(),
      callback,
    );
    await presenter.onAppEvent(this.getPositionUpdate()); // trigger initialization
    return presenter;
  }

  override getPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.positionUpdate);
  }

  override getSecondPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.secondPositionUpdate);
  }
}

describe('PresenterTransactions', () => {
  new PresenterTransactionsTest().execute();
});
