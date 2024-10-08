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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANYf KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {assertDefined} from 'common/assert_utils';
import {InMemoryStorage} from 'common/in_memory_storage';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {ParserBuilder} from 'test/unit/parser_builder';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TracesBuilder} from 'test/unit/traces_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {NotifyLogViewCallbackType} from 'viewers/common/abstract_log_viewer_presenter';
import {AbstractLogViewerPresenterTest} from 'viewers/common/abstract_log_viewer_presenter_test';
import {TextFilter} from 'viewers/common/text_filter';
import {
  LogFieldType,
  LogFieldValue,
  UiDataLog,
} from 'viewers/common/ui_data_log';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

class PresenterTransitionsTest extends AbstractLogViewerPresenterTest<UiData> {
  private trace: Trace<PropertyTreeNode> | undefined;
  private positionUpdate: TracePositionUpdate | undefined;
  private secondPositionUpdate: TracePositionUpdate | undefined;

  override readonly shouldExecuteHeaderTests = true;
  override readonly shouldExecuteFilterTests = true;
  override readonly shouldExecutePropertiesTests = true;

  override readonly totalOutputEntries = 4;
  override readonly expectedIndexOfFirstPositionUpdate = 3;
  override readonly expectedIndexOfSecondPositionUpdate = 1;
  override readonly expectedInitialFilterOptions = new Map<
    LogFieldType,
    string[] | number
  >([
    [LogFieldType.TRANSITION_TYPE, ['OPEN', 'TO_FRONT']],
    [
      LogFieldType.HANDLER,
      [
        'N/A',
        'com.android.wm.shell.recents.RecentsTransitionHandler',
        'com.android.wm.shell.transition.DefaultMixedHandler',
      ],
    ],
    [
      LogFieldType.PARTICIPANTS,
      [
        '47',
        '67',
        '398',
        '471',
        '472',
        '489',
        '0xc3df4d',
        '0x5ba3da0',
        '0x97b5518',
        '0xa884527',
        '0xb887160',
        '0xc5f6ee4',
      ],
    ],
    [LogFieldType.FLAGS, ['TRANSIT_FLAG_IS_RECENTS', '0']],
    [LogFieldType.STATUS, ['MERGED', 'N/A', 'PLAYED']],
  ]);
  override readonly filterValuesToSet = new Map<
    LogFieldType,
    Array<string | string[]>
  >([
    [LogFieldType.TRANSITION_TYPE, [[], ['CLOSE'], ['OPEN']]],
    [LogFieldType.HANDLER, [[], ['N/A']]],
    [LogFieldType.PARTICIPANTS, [[], ['0x5ba3da0']]],
    [LogFieldType.FLAGS, [[], ['TRANSIT_FLAG_IS_RECENTS']]],
    [LogFieldType.STATUS, [[], ['MERGED', 'PLAYED']]],
  ]);
  override readonly expectedFieldValuesAfterFilter = new Map<
    LogFieldType,
    Array<LogFieldValue[] | number>
  >([
    [LogFieldType.TRANSITION_TYPE, [['OPEN', 'TO_FRONT'], [], ['OPEN']]],
    [
      LogFieldType.HANDLER,
      [
        [
          'N/A',
          'com.android.wm.shell.recents.RecentsTransitionHandler',
          'com.android.wm.shell.transition.DefaultMixedHandler',
        ],
        ['N/A'],
      ],
    ],
    [
      LogFieldType.PARTICIPANTS,
      [
        [
          'Layers: 398, 47\nWindows: 0xb887160, 0x97b5518',
          'Layers: 47, 398, 67\nWindows: 0x97b5518, 0xb887160, 0xa884527',
          'Layers: 471, 47\nWindows: 0xc3df4d, 0x97b5518',
          'Layers: 489, 472\nWindows: 0x5ba3da0, 0xc5f6ee4',
        ],
        ['Layers: 489, 472\nWindows: 0x5ba3da0, 0xc5f6ee4'],
      ],
    ],
    [
      LogFieldType.FLAGS,
      [['TRANSIT_FLAG_IS_RECENTS', '0'], ['TRANSIT_FLAG_IS_RECENTS']],
    ],
    [
      LogFieldType.STATUS,
      [
        ['MERGED', 'N/A', 'PLAYED'],
        ['MERGED', 'PLAYED'],
      ],
    ],
  ]);
  override readonly logEntryClickIndex = 2;
  override readonly filterNameForCurrentIndexTest =
    LogFieldType.TRANSITION_TYPE;
  override readonly filterChangeForCurrentIndexTest = ['OPEN'];
  override readonly secondFilterChangeForCurrentIndexTest = [
    'OPEN',
    'TO_FRONT',
  ];
  override readonly expectedCurrentIndexAfterFilterChange = 2;
  override readonly expectedCurrentIndexAfterSecondFilterChange = 1;
  override readonly numberOfUnfilteredProperties = 2;
  override readonly propertiesFilter = new TextFilter('shellData', []);
  override readonly numberOfFilteredProperties = 1;

  override async setUpTestEnvironment(): Promise<void> {
    const parser = await UnitTestUtils.getPerfettoParser(
      TraceType.TRANSITION,
      'traces/perfetto/shell_transitions_trace.perfetto-trace',
    );

    this.trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.TRANSITION)
      .setParser(parser)
      .build();

    this.positionUpdate = TracePositionUpdate.fromTraceEntry(
      this.trace.getEntry(0),
    );
    this.secondPositionUpdate = TracePositionUpdate.fromTraceEntry(
      this.trace.getEntry(2),
    );
  }

  override async createPresenterWithEmptyTrace(
    callback: NotifyLogViewCallbackType<UiData>,
  ): Promise<Presenter> {
    const traces = new TracesBuilder()
      .setEntries(TraceType.TRANSITION, [])
      .build();
    const trace = assertDefined(traces.getTrace(TraceType.TRANSITION));
    return new Presenter(trace, traces, new InMemoryStorage(), callback);
  }

  override async createPresenter(
    callback: NotifyLogViewCallbackType<UiData>,
    trace = this.trace,
    positionUpdate = assertDefined(this.getPositionUpdate()),
  ): Promise<Presenter> {
    const transitionTrace = assertDefined(trace);
    const traces = new Traces();
    traces.addTrace(transitionTrace);

    const presenter = new Presenter(
      transitionTrace,
      traces,
      new InMemoryStorage(),
      callback,
    );
    await presenter.onAppEvent(positionUpdate); // trigger initialization
    return presenter;
  }

  override getPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.positionUpdate);
  }

  override getSecondPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.secondPositionUpdate);
  }

  override executePropertiesChecksAfterPositionUpdate(uiData: UiDataLog) {
    expect(uiData.entries.length).toEqual(4);

    const selectedTransition = assertDefined(uiData.propertiesTree);
    const wmData = assertDefined(selectedTransition.getChildByName('wmData'));
    expect(wmData.getChildByName('id')?.formattedValue()).toEqual('35');
    expect(wmData.getChildByName('type')?.formattedValue()).toEqual('OPEN');
    expect(wmData.getChildByName('createTimeNs')?.formattedValue()).toEqual(
      '2023-11-21, 13:30:33.176',
    );
  }

  override executeSpecializedTests() {
    describe('Specialized tests', () => {
      it('robust to corrupted transitions trace', async () => {
        const timestamp10 = TimestampConverterUtils.makeRealTimestamp(10n);
        const trace = new TraceBuilder<PropertyTreeNode>()
          .setType(TraceType.TRANSITION)
          .setParser(
            new ParserBuilder<PropertyTreeNode>()
              .setIsCorrupted(true)
              .setEntries([
                new PropertyTreeBuilder()
                  .setRootId('TransitionsTraceEntry')
                  .setName('transition0')
                  .build(),
              ])
              .setTimestamps([timestamp10])
              .build(),
          )
          .build();
        const positionUpdate = TracePositionUpdate.fromTimestamp(timestamp10);
        let uiData: UiData | undefined;
        const presenter = await this.createPresenter(
          (newData) => {
            uiData = newData;
          },
          trace,
          positionUpdate,
        );
        await presenter.onAppEvent(positionUpdate);
        expect(uiData?.entries).toEqual([]);
      });
    });
  }
}

describe('PresenterTransitions', () => {
  new PresenterTransitionsTest().execute();
});
