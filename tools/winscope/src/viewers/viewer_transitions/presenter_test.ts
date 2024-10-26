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
import {LogSelectFilter} from 'viewers/common/log_filters';
import {LogHeader, UiDataLog} from 'viewers/common/ui_data_log';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

class PresenterTransitionsTest extends AbstractLogViewerPresenterTest<UiData> {
  override readonly expectedHeaders = [
    {
      header: new LogHeader({
        name: 'Id',
        cssClass: 'transition-id right-align',
      }),
    },
    {
      header: new LogHeader(
        {name: 'Type', cssClass: 'transition-type'},
        new LogSelectFilter(Array.from({length: 2}, () => '')),
      ),
      options: ['OPEN', 'TO_FRONT'],
    },
    {header: new LogHeader({name: 'Send Time', cssClass: 'send-time time'})},
    {
      header: new LogHeader({
        name: 'Dispatch Time',
        cssClass: 'dispatch-time time',
      }),
    },
    {
      header: new LogHeader({
        name: 'Duration',
        cssClass: 'duration right-align',
      }),
    },
    {
      header: new LogHeader(
        {name: 'Handler', cssClass: 'handler'},
        new LogSelectFilter(Array.from({length: 3}, () => '')),
      ),
      options: [
        'N/A',
        'com.android.wm.shell.recents.RecentsTransitionHandler',
        'com.android.wm.shell.transition.DefaultMixedHandler',
      ],
    },
    {
      header: new LogHeader(
        {name: 'Participants', cssClass: 'participants'},
        new LogSelectFilter(
          Array.from({length: 12}, () => ''),
          true,
          '250',
          '100%',
        ),
      ),
      options: [
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
    },
    {
      header: new LogHeader(
        {name: 'Flags', cssClass: 'flags'},
        new LogSelectFilter(
          Array.from({length: 2}, () => ''),
          true,
          '250',
          '100%',
        ),
      ),
      options: ['TRANSIT_FLAG_IS_RECENTS', '0'],
    },
    {
      header: new LogHeader(
        {name: 'Status', cssClass: 'status right-align'},
        new LogSelectFilter(Array.from({length: 3}, () => '')),
      ),
      options: ['MERGED', 'N/A', 'PLAYED'],
    },
  ];
  private trace: Trace<PropertyTreeNode> | undefined;
  private positionUpdate: TracePositionUpdate | undefined;

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

  override executePropertiesChecksAfterPositionUpdate(uiData: UiDataLog) {
    expect(uiData.entries.length).toEqual(4);

    const selectedTransition = assertDefined(uiData.propertiesTree);
    const wmData = assertDefined(selectedTransition.getChildByName('wmData'));
    expect(wmData.getChildByName('id')?.formattedValue()).toEqual('35');
    expect(wmData.getChildByName('type')?.formattedValue()).toEqual('OPEN');
    expect(wmData.getChildByName('createTimeNs')?.formattedValue()).toEqual(
      '2023-11-21, 13:30:33.176',
    );

    const dispatchTime = uiData.entries[0].fields[3];
    expect(dispatchTime?.propagateEntryTimestamp).toBeTrue();
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
