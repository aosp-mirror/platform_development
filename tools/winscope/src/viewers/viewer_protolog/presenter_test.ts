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
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {
  DEFAULT_PROPERTY_FORMATTER,
  TIMESTAMP_NODE_FORMATTER,
} from 'trace/tree_node/formatters';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {NotifyLogViewCallbackType} from 'viewers/common/abstract_log_viewer_presenter';
import {AbstractLogViewerPresenterTest} from 'viewers/common/abstract_log_viewer_presenter_test';
import {LogSelectFilter, LogTextFilter} from 'viewers/common/log_filters';
import {TextFilter} from 'viewers/common/text_filter';
import {LogHeader} from 'viewers/common/ui_data_log';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

class PresenterProtologTest extends AbstractLogViewerPresenterTest<UiData> {
  override readonly expectedHeaders = [
    {
      header: new LogHeader(
        {name: 'Log Level', cssClass: 'log-level'},
        new LogSelectFilter(Array.from({length: 3}, () => '')),
      ),
      options: ['level0', 'level1', 'level2'],
    },
    {
      header: new LogHeader(
        {name: 'Tag', cssClass: 'tag'},
        new LogSelectFilter(Array.from({length: 3}, () => '')),
      ),
      options: ['tag0', 'tag1', 'tag2'],
    },
    {
      header: new LogHeader(
        {name: 'Source files', cssClass: 'source-file'},
        new LogSelectFilter(Array.from({length: 3}, () => '')),
      ),
      options: ['sourcefile0', 'sourcefile1', 'sourcefile2'],
    },
    {
      header: new LogHeader(
        {name: 'Search text', cssClass: 'text'},
        new LogTextFilter(new TextFilter()),
      ),
    },
  ];
  private trace: Trace<PropertyTreeNode> | undefined;
  private positionUpdate: TracePositionUpdate | undefined;

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

    this.positionUpdate = TracePositionUpdate.fromTraceEntry(
      this.trace.getEntry(0),
    );
  }

  override async createPresenterWithEmptyTrace(
    callback: NotifyLogViewCallbackType<UiData>,
  ): Promise<Presenter> {
    const emptyTrace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.PROTO_LOG)
      .setEntries([])
      .build();
    return new Presenter(emptyTrace, callback, new InMemoryStorage());
  }

  override async createPresenter(
    callback: NotifyLogViewCallbackType<UiData>,
  ): Promise<Presenter> {
    const presenter = new Presenter(
      assertDefined(this.trace),
      callback,
      new InMemoryStorage(),
    );
    await presenter.onAppEvent(this.getPositionUpdate()); // trigger initialization
    return presenter;
  }

  override getPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.positionUpdate);
  }
}

describe('PresenterProtolog', () => {
  new PresenterProtologTest().execute();
});
