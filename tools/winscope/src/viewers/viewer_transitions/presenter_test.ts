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
import {UiDataLog} from 'viewers/common/ui_data_log';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

class PresenterTransitionsTest extends AbstractLogViewerPresenterTest<UiData> {
  private trace: Trace<PropertyTreeNode> | undefined;
  private positionUpdate: TracePositionUpdate | undefined;
  private secondPositionUpdate: TracePositionUpdate | undefined;

  override readonly shouldExecuteHeaderTests = true;
  override readonly shouldExecuteFilterTests = false;
  override readonly shouldExecutePropertiesTests = true;

  override readonly totalOutputEntries = 4;
  override readonly expectedIndexOfFirstPositionUpdate = 3;
  override readonly expectedIndexOfSecondPositionUpdate = 1;
  override readonly logEntryClickIndex = 2;

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

  override createPresenterWithEmptyTrace(
    callback: NotifyLogViewCallbackType<UiData>,
  ): Presenter {
    const traces = new TracesBuilder()
      .setEntries(TraceType.TRANSITION, [])
      .build();
    const trace = assertDefined(traces.getTrace(TraceType.TRANSITION));
    return new Presenter(trace, traces, callback);
  }

  override async createPresenter(
    callback: NotifyLogViewCallbackType<UiData>,
    trace = this.trace,
    positionUpdate = assertDefined(this.getPositionUpdate()),
  ): Promise<Presenter> {
    const transitionTrace = assertDefined(trace);
    const traces = new Traces();
    traces.addTrace(transitionTrace);

    const presenter = new Presenter(transitionTrace, traces, callback);
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
