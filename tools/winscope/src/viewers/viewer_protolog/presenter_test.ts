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
import {AbstractLogViewerPresenterTest} from 'viewers/common/abstract_log_viewer_presenter_test';
import {LogFieldType, LogFieldValue} from 'viewers/common/ui_data_log';
import {Presenter} from './presenter';

class PresenterProtologTest extends AbstractLogViewerPresenterTest {
  private trace: Trace<PropertyTreeNode> | undefined;
  private positionUpdate: TracePositionUpdate | undefined;
  private secondPositionUpdate: TracePositionUpdate | undefined;

  override readonly shouldExecuteHeaderTests = false;
  override readonly shouldExecuteFilterTests = true;
  override readonly shouldExecuteCurrentIndexTests = true;
  override readonly shouldExecutePropertiesTests = false;

  override readonly totalOutputEntries = 3;
  override readonly expectedIndexOfSecondPositionUpdate = 1;
  override readonly expectedInitialFilterOptions = new Map<
    LogFieldType,
    string[] | number
  >([
    [LogFieldType.LOG_LEVEL, ['level0', 'level1', 'level2']],
    [LogFieldType.TAG, ['tag0', 'tag1', 'tag2']],
    [LogFieldType.SOURCE_FILE, ['sourcefile0', 'sourcefile1', 'sourcefile2']],
  ]);
  override readonly filterValuesToSet = new Map<
    LogFieldType,
    Array<string | string[]>
  >([
    [LogFieldType.LOG_LEVEL, [[], ['level1'], ['level0', 'level1', 'level2']]],
    [LogFieldType.TAG, [[], ['tag1'], ['tag0', 'tag1', 'tag2']]],
    [
      LogFieldType.SOURCE_FILE,
      [[], ['sourcefile1'], ['sourcefile0', 'sourcefile1', 'sourcefile2']],
    ],
    [LogFieldType.TEXT, [[], 'text', 'text0', 'text1']],
  ]);
  override readonly expectedFieldValuesAfterFilter = new Map<
    LogFieldType,
    Array<LogFieldValue[] | number>
  >([
    [
      LogFieldType.LOG_LEVEL,
      [this.totalOutputEntries, ['level1'], ['level0', 'level1', 'level2']],
    ],
    [
      LogFieldType.TAG,
      [this.totalOutputEntries, ['tag1'], ['tag0', 'tag1', 'tag2']],
    ],
    [
      LogFieldType.SOURCE_FILE,
      [
        this.totalOutputEntries,
        ['sourcefile1'],
        ['sourcefile0', 'sourcefile1', 'sourcefile2'],
      ],
    ],
    [
      LogFieldType.TEXT,
      [
        this.totalOutputEntries,
        ['text0', 'text1', 'text2'],
        ['text0'],
        ['text1'],
      ],
    ],
  ]);
  override readonly logEntryClickIndex = 10;
  override readonly filterNameForCurrentIndexTest = LogFieldType.LOG_LEVEL;
  override readonly filterChangeForCurrentIndexTest = ['level1'];
  override readonly expectedCurrentIndexAfterFilterChange = 0;
  override readonly secondFilterChangeForCurrentIndexTest = [
    'level0',
    'level1',
  ];
  override readonly expectedCurrentIndexAfterSecondFilterChange = 1;

  override async setUpTestEnvironment(): Promise<void> {
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

    this.trace = new TraceBuilder<PropertyTreeNode>()
      .setEntries(entries)
      .setTimestamps([time10, time11, time12])
      .build();

    this.positionUpdate = TracePositionUpdate.fromTimestamp(time10);
    this.secondPositionUpdate = TracePositionUpdate.fromTimestamp(time11);
  }

  override createPresenterWithEmptyTrace(
    callback: NotifyLogViewCallbackType,
  ): Presenter {
    const emptyTrace = new TraceBuilder<PropertyTreeNode>()
      .setEntries([])
      .build();
    return new Presenter(emptyTrace, callback);
  }

  override async createPresenter(
    callback: NotifyLogViewCallbackType,
  ): Promise<Presenter> {
    const presenter = new Presenter(assertDefined(this.trace), callback);
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

describe('PresenterProtolog', () => {
  new PresenterProtologTest().execute();
});
