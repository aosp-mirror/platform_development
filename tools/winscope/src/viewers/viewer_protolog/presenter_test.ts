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
import {TracesBuilder} from 'test/unit/traces_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {
  DEFAULT_PROPERTY_FORMATTER,
  TIMESTAMP_NODE_FORMATTER,
} from 'trace/tree_node/formatters';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {Presenter} from './presenter';
import {UiData, UiDataMessage} from './ui_data';

describe('ViewerProtoLogPresenter', () => {
  let presenter: Presenter;
  let inputMessages: UiDataMessage[];
  let trace: Trace<PropertyTreeNode>;
  let positionUpdate10: TracePositionUpdate;
  let positionUpdate11: TracePositionUpdate;
  let positionUpdate12: TracePositionUpdate;
  let outputUiData: undefined | UiData;

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

    inputMessages = [
      {
        traceIndex: 0,
        text: 'text0',
        time: assertDefined(entries[0].getChildByName('timestamp')),
        tag: 'tag0',
        level: 'level0',
        at: 'sourcefile0',
      },
      {
        traceIndex: 1,
        text: 'text1',
        time: assertDefined(entries[1].getChildByName('timestamp')),
        tag: 'tag1',
        level: 'level1',
        at: 'sourcefile1',
      },
      {
        traceIndex: 2,
        text: 'text2',
        time: assertDefined(entries[2].getChildByName('timestamp')),
        tag: 'tag2',
        level: 'level2',
        at: 'sourcefile2',
      },
    ];

    trace = new TraceBuilder<PropertyTreeNode>()
      .setEntries(entries)
      .setTimestamps([time10, time11, time12])
      .build();

    positionUpdate10 = TracePositionUpdate.fromTimestamp(time10);
    positionUpdate11 = TracePositionUpdate.fromTimestamp(time11);
    positionUpdate12 = TracePositionUpdate.fromTimestamp(time12);

    outputUiData = undefined;

    presenter = new Presenter(trace, (data: UiData) => {
      outputUiData = data;
    });
    await presenter.onAppEvent(positionUpdate10); // trigger initialization
  });

  it('is robust to empty trace', async () => {
    const traces = new TracesBuilder()
      .setEntries(TraceType.PROTO_LOG, [])
      .build();
    const trace = new TraceBuilder<PropertyTreeNode>().setEntries([]).build();
    presenter = new Presenter(trace, (data: UiData) => {
      outputUiData = data;
    });

    const uiData = assertDefined(outputUiData);
    expect(uiData.messages).toEqual([]);
    expect(uiData.currentMessageIndex).toBeUndefined();

    await presenter.onAppEvent(positionUpdate10);

    const newUiData = assertDefined(outputUiData);
    expect(newUiData.messages).toEqual([]);
    expect(newUiData.currentMessageIndex).toBeUndefined();
  });

  it('processes trace position updates', async () => {
    await presenter.onAppEvent(positionUpdate10);

    const uiData = assertDefined(outputUiData);
    expect(uiData.allLogLevels).toEqual(['level0', 'level1', 'level2']);
    expect(uiData.allTags).toEqual(['tag0', 'tag1', 'tag2']);
    expect(uiData.allSourceFiles).toEqual([
      'sourcefile0',
      'sourcefile1',
      'sourcefile2',
    ]);
    expect(uiData.messages).toEqual(inputMessages);
    expect(uiData.currentMessageIndex).toEqual(0);
  });

  it('updates displayed messages according to log levels filter', () => {
    expect(assertDefined(outputUiData).messages).toEqual(inputMessages);

    presenter.onLogLevelsFilterChanged([]);
    expect(assertDefined(outputUiData).messages).toEqual(inputMessages);

    presenter.onLogLevelsFilterChanged(['level1']);
    expect(assertDefined(outputUiData).messages).toEqual([inputMessages[1]]);

    presenter.onLogLevelsFilterChanged(['level0', 'level1', 'level2']);
    expect(assertDefined(outputUiData).messages).toEqual(inputMessages);
  });

  it('updates displayed messages according to tags filter', () => {
    expect(assertDefined(outputUiData).messages).toEqual(inputMessages);

    presenter.onTagsFilterChanged([]);
    expect(assertDefined(outputUiData).messages).toEqual(inputMessages);

    presenter.onTagsFilterChanged(['tag1']);
    expect(assertDefined(outputUiData).messages).toEqual([inputMessages[1]]);

    presenter.onTagsFilterChanged(['tag0', 'tag1', 'tag2']);
    expect(assertDefined(outputUiData).messages).toEqual(inputMessages);
  });

  it('updates displayed messages according to source files filter', () => {
    expect(assertDefined(outputUiData).messages).toEqual(inputMessages);

    presenter.onSourceFilesFilterChanged([]);
    expect(assertDefined(outputUiData).messages).toEqual(inputMessages);

    presenter.onSourceFilesFilterChanged(['sourcefile1']);
    expect(assertDefined(outputUiData).messages).toEqual([inputMessages[1]]);

    presenter.onSourceFilesFilterChanged([
      'sourcefile0',
      'sourcefile1',
      'sourcefile2',
    ]);
    expect(assertDefined(outputUiData).messages).toEqual(inputMessages);
  });

  it('updates displayed messages according to search string filter', () => {
    expect(assertDefined(outputUiData).messages).toEqual(inputMessages);

    presenter.onSearchStringFilterChanged('');
    expect(assertDefined(outputUiData).messages).toEqual(inputMessages);

    presenter.onSearchStringFilterChanged('text');
    expect(assertDefined(outputUiData).messages).toEqual(inputMessages);

    presenter.onSearchStringFilterChanged('text0');
    expect(assertDefined(outputUiData).messages).toEqual([inputMessages[0]]);

    presenter.onSearchStringFilterChanged('text1');
    expect(assertDefined(outputUiData).messages).toEqual([inputMessages[1]]);
  });

  it('computes current message index', async () => {
    // Position -> entry #0
    await presenter.onAppEvent(positionUpdate10);
    presenter.onLogLevelsFilterChanged([]);
    expect(assertDefined(outputUiData).currentMessageIndex).toEqual(0);

    presenter.onLogLevelsFilterChanged(['level0']);
    expect(assertDefined(outputUiData).currentMessageIndex).toEqual(0);

    presenter.onLogLevelsFilterChanged([]);
    expect(assertDefined(outputUiData).currentMessageIndex).toEqual(0);

    // Position -> entry #1
    await presenter.onAppEvent(positionUpdate11);
    presenter.onLogLevelsFilterChanged([]);
    expect(assertDefined(outputUiData).currentMessageIndex).toEqual(1);

    presenter.onLogLevelsFilterChanged(['level0']);
    expect(assertDefined(outputUiData).currentMessageIndex).toEqual(0);

    presenter.onLogLevelsFilterChanged(['level1']);
    expect(assertDefined(outputUiData).currentMessageIndex).toEqual(0);

    presenter.onLogLevelsFilterChanged(['level0', 'level1']);
    expect(assertDefined(outputUiData).currentMessageIndex).toEqual(1);

    // Position -> entry #2
    await presenter.onAppEvent(positionUpdate12);
    presenter.onLogLevelsFilterChanged([]);
    expect(assertDefined(outputUiData).currentMessageIndex).toEqual(2);
  });

  it('updates selected message index', () => {
    expect(assertDefined(outputUiData).selectedMessageIndex).toBeUndefined();
    presenter.onMessageClicked(3);
    expect(assertDefined(outputUiData).selectedMessageIndex).toEqual(3);
  });
});
