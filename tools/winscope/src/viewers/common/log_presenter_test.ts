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

import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TextFilter} from 'viewers/common/text_filter';
import {LogSelectFilter, LogTextFilter} from './log_filters';
import {LogPresenter} from './log_presenter';
import {LogEntry, LogHeader} from './ui_data_log';

describe('LogPresenter', () => {
  let presenter: LogPresenter<LogEntry>;
  const timestamp1 = TimestampConverterUtils.makeElapsedTimestamp(1n);
  const timestamp2 = TimestampConverterUtils.makeElapsedTimestamp(2n);
  const timestamp3 = TimestampConverterUtils.makeElapsedTimestamp(3n);
  const timestamp4 = TimestampConverterUtils.makeElapsedTimestamp(4n);
  const trace = new TraceBuilder<PropertyTreeNode>()
    .setType(TraceType.TRANSACTIONS)
    .setEntries([
      new PropertyTreeBuilder()
        .setRootId('Test Trace')
        .setName('entry 1')
        .build(),
      new PropertyTreeBuilder()
        .setRootId('Test Trace')
        .setName('entry 2')
        .build(),
      new PropertyTreeBuilder()
        .setRootId('Test Trace')
        .setName('entry 3')
        .build(),
      new PropertyTreeBuilder()
        .setRootId('Test Trace')
        .setName('entry 4')
        .build(),
    ])
    .setTimestamps([timestamp1, timestamp2, timestamp3, timestamp4])
    .build();

  const STRING_COLUMN = {
    name: 'String Column',
    cssClass: 'string-column',
  };
  const NUMBER_COLUMN = {
    name: 'Number Column',
    cssClass: 'number-column',
  };
  const TIMESTAMP_COLUMN = {
    name: 'Timestamp Column',
    cssClass: 'timestamp-column',
  };
  let stringFilter: LogTextFilter;
  let numberFilter: LogSelectFilter;
  let headers: LogHeader[];
  let testEntries: LogEntry[];

  describe('time-ordered entries', () => {
    beforeEach(async () => {
      presenter = new LogPresenter();
      testEntries = await buildTestEntries();
      presenter.setAllEntries(testEntries);
      expectAllIndicesUndefined();

      stringFilter = new LogTextFilter(new TextFilter('stringValue'));
      numberFilter = new LogSelectFilter(['0', '1', '2', '3']);
      headers = [
        new LogHeader(STRING_COLUMN, stringFilter),
        new LogHeader(NUMBER_COLUMN, numberFilter),
        new LogHeader(TIMESTAMP_COLUMN),
      ];
    });

    it('applies filters to existing entries when headers updated', async () => {
      presenter.setHeaders(headers);
      expect(presenter.getHeaders()).toEqual(headers);
      expect(presenter.getFilteredEntries()).toEqual([
        testEntries[0],
        testEntries[2],
      ]);
      expectAllIndicesUndefined();
    });

    it('applies existing filters when entries updated', async () => {
      presenter.setAllEntries([]);
      expectAllIndicesUndefined();

      presenter.setHeaders(headers);
      expect(presenter.getHeaders()).toEqual(headers);
      expectAllIndicesUndefined();

      presenter.setAllEntries(testEntries);
      expect(presenter.getFilteredEntries()).toEqual([
        testEntries[0],
        testEntries[2],
      ]);
      expectAllIndicesUndefined();
    });

    it('applies log entry click', () => {
      // selects index
      presenter.applyLogEntryClick(1);
      expect(presenter.getSelectedIndex()).toEqual(1);
      expect(presenter.getCurrentIndex()).toBeUndefined();
      expect(presenter.getScrollToIndex()).toBeUndefined();

      // on same index, clears scroll but leaves current and selected unchanged
      presenter.applyTracePositionUpdate(trace.getEntry(0));
      presenter.applyLogEntryClick(1);
      expect(presenter.getSelectedIndex()).toEqual(1);
      expect(presenter.getCurrentIndex()).toEqual(0);
      expect(presenter.getScrollToIndex()).toBeUndefined();
    });

    it('applies arrow down press', () => {
      // selects and scrolls to first index if no selected or current index
      presenter.applyArrowDownPress();
      expect(presenter.getSelectedIndex()).toEqual(0);
      expect(presenter.getScrollToIndex()).toEqual(0);
      expect(presenter.getCurrentIndex()).toBeUndefined();

      // selects next index after selected index
      presenter.applyArrowDownPress();
      expect(presenter.getSelectedIndex()).toEqual(1);
      expect(presenter.getScrollToIndex()).toEqual(1);
      expect(presenter.getCurrentIndex()).toBeUndefined();

      // handles index out of range
      presenter.applyArrowDownPress();
      presenter.applyArrowDownPress();
      presenter.applyArrowDownPress();
      expect(presenter.getSelectedIndex()).toEqual(3);
      expect(presenter.getScrollToIndex()).toEqual(3);
      expect(presenter.getCurrentIndex()).toBeUndefined();

      // selects next index after current index
      presenter.applyTracePositionUpdate(trace.getEntry(0));
      presenter.applyArrowDownPress();
      expect(presenter.getSelectedIndex()).toEqual(1);
      expect(presenter.getScrollToIndex()).toEqual(1);
      expect(presenter.getCurrentIndex()).toEqual(0);

      // handles no entries
      presenter.setAllEntries([]);
      presenter.applyArrowDownPress();
      expectAllIndicesUndefined();
    });

    it('applies arrow up press', () => {
      // selects first index if no selected or current index
      presenter.applyArrowUpPress();
      expect(presenter.getSelectedIndex()).toEqual(0);
      expect(presenter.getScrollToIndex()).toEqual(0);
      expect(presenter.getCurrentIndex()).toBeUndefined();

      // selects index before selected index
      presenter.applyLogEntryClick(2);
      presenter.applyArrowUpPress();
      expect(presenter.getSelectedIndex()).toEqual(1);
      expect(presenter.getScrollToIndex()).toEqual(1);
      expect(presenter.getCurrentIndex()).toBeUndefined();

      // handles index out of range
      presenter.applyArrowUpPress();
      presenter.applyArrowUpPress();
      presenter.applyArrowUpPress();
      expect(presenter.getSelectedIndex()).toEqual(0);
      expect(presenter.getScrollToIndex()).toEqual(0);
      expect(presenter.getCurrentIndex()).toBeUndefined();

      // selects index before current index
      presenter.applyTracePositionUpdate(trace.getEntry(1));
      presenter.applyArrowUpPress();
      expect(presenter.getSelectedIndex()).toEqual(0);
      expect(presenter.getScrollToIndex()).toEqual(0);
      expect(presenter.getCurrentIndex()).toEqual(1);

      // handles no entries
      presenter.setAllEntries([]);
      presenter.applyArrowDownPress();
      expectAllIndicesUndefined();
    });

    it('applies trace position update', () => {
      // updates current index, clears selected index, scrolls to current index
      presenter.applyTracePositionUpdate(trace.getEntry(1));
      expect(presenter.getCurrentIndex()).toEqual(1);
      expect(presenter.getSelectedIndex()).toBeUndefined();
      expect(presenter.getScrollToIndex()).toEqual(1);

      // if no current entry, current index undefined
      presenter.applyTracePositionUpdate(undefined);
      expect(presenter.getCurrentIndex()).toBeUndefined();
      expect(presenter.getSelectedIndex()).toBeUndefined();
      expect(presenter.getScrollToIndex()).toBeUndefined();

      // if current entry filtered out, returns next entry by time
      updateStringFilterAndCheckEntries('stringValue', [
        testEntries[0],
        testEntries[2],
      ]);
      presenter.applyTracePositionUpdate(trace.getEntry(1));
      expect(presenter.getCurrentIndex()).toEqual(1);
      expect(presenter.getSelectedIndex()).toBeUndefined();
      expect(presenter.getScrollToIndex()).toEqual(1);

      // handles no filtered entries
      updateStringFilterAndCheckEntries('no matches', []);
      presenter.applyTracePositionUpdate(trace.getEntry(1));
      expectAllIndicesUndefined();
      updateStringFilterAndCheckEntries('', testEntries);

      // handles no entries
      presenter.setAllEntries([]);
      presenter.applyTracePositionUpdate(trace.getEntry(1));
      presenter.applyArrowDownPress();
      expectAllIndicesUndefined();
    });

    it('applies trace position update for non time-ordered entries', () => {
      const presenter = new LogPresenter(false);
      presenter.setAllEntries([testEntries[3]].concat(testEntries.slice(0, 3)));
      expectAllIndicesUndefined();

      presenter.applyTracePositionUpdate(trace.getEntry(1));
      expect(presenter.getCurrentIndex()).toEqual(2);
      expect(presenter.getSelectedIndex()).toBeUndefined();
      expect(presenter.getScrollToIndex()).toEqual(2);

      presenter.applyTracePositionUpdate(trace.getEntry(3));
      expect(presenter.getCurrentIndex()).toEqual(0);
      expect(presenter.getSelectedIndex()).toBeUndefined();
      expect(presenter.getScrollToIndex()).toEqual(0);
    });

    it('applies text filter change', () => {
      updateStringFilterAndCheckEntries('stringValue', [
        testEntries[0],
        testEntries[2],
      ]);
      updateStringFilterAndCheckEntries('no matches', []);
      updateStringFilterAndCheckEntries('', testEntries);
    });

    it('applies select filter change', () => {
      updateNumberFilterAndCheckEntries(['0'], [testEntries[0]]);
      updateNumberFilterAndCheckEntries(
        ['0', '3'],
        [testEntries[0], testEntries[3]],
      );
      updateNumberFilterAndCheckEntries([], testEntries);
    });
  });

  async function buildTestEntries(): Promise<LogEntry[]> {
    return [
      {
        traceEntry: trace.getEntry(0),
        fields: [
          {
            spec: STRING_COLUMN,
            value: 'stringValue',
          },
          {
            spec: NUMBER_COLUMN,
            value: 0,
          },
          {
            spec: TIMESTAMP_COLUMN,
            value: timestamp1,
          },
        ],
        propertiesTree: await trace.getEntry(0).getValue(),
      },
      {
        traceEntry: trace.getEntry(1),
        fields: [
          {
            spec: STRING_COLUMN,
            value: 'differentValue',
          },
          {
            spec: NUMBER_COLUMN,
            value: 1,
          },
          {
            spec: TIMESTAMP_COLUMN,
            value: timestamp2,
          },
        ],
        propertiesTree: await trace.getEntry(1).getValue(),
      },
      {
        traceEntry: trace.getEntry(2),
        fields: [
          {
            spec: STRING_COLUMN,
            value: 'stringValue',
          },
          {
            spec: NUMBER_COLUMN,
            value: 2,
          },
          {
            spec: TIMESTAMP_COLUMN,
            value: timestamp3,
          },
        ],
        propertiesTree: await trace.getEntry(2).getValue(),
      },
      {
        traceEntry: trace.getEntry(3),
        fields: [
          {
            spec: STRING_COLUMN,
            value: 'differentValue',
          },
          {
            spec: NUMBER_COLUMN,
            value: 3,
          },
          {
            spec: TIMESTAMP_COLUMN,
            value: timestamp4,
          },
        ],
        propertiesTree: await trace.getEntry(3).getValue(),
      },
    ];
  }

  function expectAllIndicesUndefined() {
    expect(presenter.getCurrentIndex()).toBeUndefined();
    expect(presenter.getSelectedIndex()).toBeUndefined();
    expect(presenter.getScrollToIndex()).toBeUndefined();
  }

  function updateStringFilterAndCheckEntries(
    value: string,
    expectedEntries: LogEntry[],
  ) {
    stringFilter.updateFilterValue([value]);
    presenter.applyTextFilterChange(headers[0], stringFilter.textFilter);
    expect(presenter.getFilteredEntries()).toEqual(expectedEntries);
  }

  function updateNumberFilterAndCheckEntries(
    value: string[],
    expectedEntries: LogEntry[],
  ) {
    presenter.applySelectFilterChange(headers[1], value);
    expect(presenter.getFilteredEntries()).toEqual(expectedEntries);
  }
});
