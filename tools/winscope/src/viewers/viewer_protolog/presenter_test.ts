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
import {TracePositionUpdate} from 'messaging/winscope_event';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {Trace} from 'trace/trace';
import {
  DEFAULT_PROPERTY_FORMATTER,
  TIMESTAMP_NODE_FORMATTER,
} from 'trace/tree_node/formatters';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {NotifyLogViewCallbackType} from 'viewers/common/abstract_log_viewer_presenter';
import {
  LogEntry,
  LogFieldName,
  LogFieldValue,
} from 'viewers/common/ui_data_log';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

describe('ViewerProtoLogPresenter', () => {
  let presenter: Presenter;
  let trace: Trace<PropertyTreeNode>;
  let positionUpdate10: TracePositionUpdate;
  let positionUpdate11: TracePositionUpdate;
  let positionUpdate12: TracePositionUpdate;
  let outputUiData: undefined | UiData;
  const TOTAL_OUTPUT_ENTRIES = 3;

  beforeEach(async () => {
    const time10 = TimestampConverterUtils.makeRealTimestamp(10n);
    const time11 = TimestampConverterUtils.makeRealTimestamp(11n);
    const time12 = TimestampConverterUtils.makeRealTimestamp(12n);
    const elapsedTime10 = TimestampConverterUtils.makeElapsedTimestamp(10n);
    const elapsedTime20 = TimestampConverterUtils.makeElapsedTimestamp(20n);
    const elapsedTime30 = TimestampConverterUtils.makeElapsedTimestamp(30n);

    const entries = [
      new PropertyTreeBuilder()
        .setRootId('ProtologTrace')
        .setName('message')
        .setChildren([
          {name: 'text', value: 'text0', formatter: DEFAULT_PROPERTY_FORMATTER},
          {
            name: 'timestamp',
            value: elapsedTime10,
            formatter: TIMESTAMP_NODE_FORMATTER,
          },
          {name: 'tag', value: 'tag0', formatter: DEFAULT_PROPERTY_FORMATTER},
          {
            name: 'level',
            value: 'level0',
            formatter: DEFAULT_PROPERTY_FORMATTER,
          },
          {
            name: 'at',
            value: 'sourcefile0',
            formatter: DEFAULT_PROPERTY_FORMATTER,
          },
        ])
        .build(),

      new PropertyTreeBuilder()
        .setRootId('ProtologTrace')
        .setName('message')
        .setChildren([
          {name: 'text', value: 'text1', formatter: DEFAULT_PROPERTY_FORMATTER},
          {
            name: 'timestamp',
            value: elapsedTime20,
            formatter: TIMESTAMP_NODE_FORMATTER,
          },
          {name: 'tag', value: 'tag1', formatter: DEFAULT_PROPERTY_FORMATTER},
          {
            name: 'level',
            value: 'level1',
            formatter: DEFAULT_PROPERTY_FORMATTER,
          },
          {
            name: 'at',
            value: 'sourcefile1',
            formatter: DEFAULT_PROPERTY_FORMATTER,
          },
        ])
        .build(),

      new PropertyTreeBuilder()
        .setRootId('ProtologTrace')
        .setName('message')
        .setChildren([
          {name: 'text', value: 'text2', formatter: DEFAULT_PROPERTY_FORMATTER},
          {
            name: 'timestamp',
            value: elapsedTime30,
            formatter: TIMESTAMP_NODE_FORMATTER,
          },
          {name: 'tag', value: 'tag2', formatter: DEFAULT_PROPERTY_FORMATTER},
          {
            name: 'level',
            value: 'level2',
            formatter: DEFAULT_PROPERTY_FORMATTER,
          },
          {
            name: 'at',
            value: 'sourcefile2',
            formatter: DEFAULT_PROPERTY_FORMATTER,
          },
        ])
        .build(),
    ];

    trace = new TraceBuilder<PropertyTreeNode>()
      .setEntries(entries)
      .setTimestamps([time10, time11, time12])
      .build();

    positionUpdate10 = TracePositionUpdate.fromTimestamp(time10);
    positionUpdate11 = TracePositionUpdate.fromTimestamp(time11);
    positionUpdate12 = TracePositionUpdate.fromTimestamp(time12);

    outputUiData = undefined;
    const callback = (data: UiData) => {
      outputUiData = data;
    };
    presenter = new Presenter(trace, callback as NotifyLogViewCallbackType);
    await presenter.onAppEvent(positionUpdate10); // trigger initialization
  });

  it('is robust to empty trace', async () => {
    const trace = new TraceBuilder<PropertyTreeNode>().setEntries([]).build();

    let outputData: UiData | undefined;
    const callback = (data: UiData) => {
      outputData = data;
    };
    const presenterEmptyTrace = new Presenter(
      trace,
      callback as NotifyLogViewCallbackType,
    );
    expect(outputData).toEqual(UiData.EMPTY);

    await presenterEmptyTrace.onAppEvent(
      TracePositionUpdate.fromTimestamp(
        TimestampConverterUtils.makeRealTimestamp(10n),
      ),
    );
    expect(outputData).toEqual(UiData.EMPTY);
  });

  it('processes trace position updates', async () => {
    await presenter.onAppEvent(positionUpdate10);

    const uiData = assertDefined(outputUiData);
    expect(getFilterOptions(LogFieldName.LOG_LEVEL)).toEqual([
      'level0',
      'level1',
      'level2',
    ]);
    expect(getFilterOptions(LogFieldName.TAG)).toEqual([
      'tag0',
      'tag1',
      'tag2',
    ]);
    expect(getFilterOptions(LogFieldName.SOURCE_FILE)).toEqual([
      'sourcefile0',
      'sourcefile1',
      'sourcefile2',
    ]);
    expect(uiData.entries.length).toEqual(3);
    expect(uiData.currentIndex).toEqual(0);
  });

  it('filters entries according to log level filter', async () => {
    await checkFilter(
      LogFieldName.LOG_LEVEL,
      [],
      undefined,
      TOTAL_OUTPUT_ENTRIES,
    );

    await checkFilter(LogFieldName.LOG_LEVEL, ['level1'], ['level1']);

    await checkFilter(
      LogFieldName.LOG_LEVEL,
      ['level0', 'level1', 'level2'],
      ['level0', 'level1', 'level2'],
    );
  });

  it('filters entries according to tag filter', async () => {
    await checkFilter(LogFieldName.TAG, [], undefined, TOTAL_OUTPUT_ENTRIES);

    await checkFilter(LogFieldName.TAG, ['tag1'], ['tag1']);

    await checkFilter(
      LogFieldName.TAG,
      ['tag0', 'tag1', 'tag2'],
      ['tag0', 'tag1', 'tag2'],
    );
  });

  it('filters entries according to source file filter', async () => {
    await checkFilter(
      LogFieldName.SOURCE_FILE,
      [],
      undefined,
      TOTAL_OUTPUT_ENTRIES,
    );

    await checkFilter(
      LogFieldName.SOURCE_FILE,
      ['sourcefile1'],
      ['sourcefile1'],
    );

    await checkFilter(
      LogFieldName.SOURCE_FILE,
      ['sourcefile0', 'sourcefile1', 'sourcefile2'],
      ['sourcefile0', 'sourcefile1', 'sourcefile2'],
    );
  });

  it('filters entries according to text filter', async () => {
    await checkFilter(LogFieldName.TEXT, '', undefined, TOTAL_OUTPUT_ENTRIES);

    await checkFilter(LogFieldName.TEXT, 'text', ['text0', 'text1', 'text2']);

    await checkFilter(LogFieldName.TEXT, 'text0', ['text0']);

    await checkFilter(LogFieldName.TEXT, 'text1', ['text1']);
  });

  it('updates selected entry ui data when entry clicked', async () => {
    await presenter.onAppEvent(positionUpdate10);
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
    await presenter.onAppEvent(positionUpdate10);
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
    await presenter.onAppEvent(positionUpdate10);
    expect(assertDefined(outputUiData).currentIndex).toEqual(0);

    await presenter.onAppEvent(positionUpdate11);
    expect(assertDefined(outputUiData).currentIndex).toEqual(1);
  });

  it('updates current index when filters change', async () => {
    await presenter.onAppEvent(positionUpdate10);

    await presenter.onFilterChange(LogFieldName.LOG_LEVEL, []);
    expect(assertDefined(outputUiData).currentIndex).toEqual(0);

    await presenter.onFilterChange(LogFieldName.LOG_LEVEL, ['level0']);
    expect(assertDefined(outputUiData).currentIndex).toEqual(0);

    await presenter.onAppEvent(positionUpdate11);

    await presenter.onFilterChange(LogFieldName.LOG_LEVEL, []);
    expect(assertDefined(outputUiData).currentIndex).toEqual(1);

    await presenter.onFilterChange(LogFieldName.LOG_LEVEL, ['level0']);
    expect(assertDefined(outputUiData).currentIndex).toEqual(0);

    await presenter.onFilterChange(LogFieldName.LOG_LEVEL, ['level1']);
    expect(assertDefined(outputUiData).currentIndex).toEqual(0);

    await presenter.onFilterChange(LogFieldName.LOG_LEVEL, [
      'level0',
      'level1',
    ]);
    expect(assertDefined(outputUiData).currentIndex).toEqual(1);
  });

  function checkInitialTracePositionUpdate() {
    const uiData = assertDefined(outputUiData);
    expect(uiData.currentIndex).toEqual(0);
    expect(uiData.selectedIndex).toBeUndefined();
    expect(uiData.scrollToIndex).toEqual(0);
  }

  function checkSelectedEntryUiData(index: number | undefined) {
    const uiData = assertDefined(outputUiData);
    expect(uiData.currentIndex).toEqual(0);
    expect(uiData.selectedIndex).toEqual(index);
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
    filterValue: string[] | string,
    expectedFieldValues?: LogFieldValue[],
    numberOfFieldValues?: number,
  ) {
    await presenter.onFilterChange(name, filterValue);
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
