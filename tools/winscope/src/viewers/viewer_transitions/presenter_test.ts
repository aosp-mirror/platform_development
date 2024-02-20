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
import {TimestampType} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {TracesBuilder} from 'test/unit/traces_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

describe('PresenterTransitions', () => {
  it('is robust to empty trace', async () => {
    const traces = new TracesBuilder()
      .setEntries(TraceType.TRANSITION, [])
      .build();
    let outputUiData: UiData | undefined;
    const presenter = new Presenter(traces, (data: UiData) => {
      outputUiData = data;
    });

    await presenter.onAppEvent(
      TracePositionUpdate.fromTimestamp(
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(10n),
      ),
    );
    expect(outputUiData).toEqual(UiData.EMPTY);
  });

  it('updates selected transition', async () => {
    const parser = await UnitTestUtils.getPerfettoParser(
      TraceType.TRANSITION,
      'traces/perfetto/shell_transitions_trace.perfetto-trace',
    );

    const trace = new TraceBuilder<PropertyTreeNode>()
      .setParser(parser)
      .setTimestampType(TimestampType.REAL)
      .build();

    const traces = new Traces();
    traces.setTrace(TraceType.TRANSITION, trace);

    let outputUiData = UiData.EMPTY;
    const presenter = new Presenter(traces, (data: UiData) => {
      outputUiData = data;
    });

    const entry = trace.getEntry(0);
    await presenter.onAppEvent(TracePositionUpdate.fromTraceEntry(entry));

    expect(outputUiData.entries.length).toEqual(4);

    const selectedTransition = assertDefined(outputUiData.selectedTransition);
    const wmData = assertDefined(selectedTransition.getChildByName('wmData'));
    expect(wmData.getChildByName('id')?.formattedValue()).toEqual('32');
    expect(wmData.getChildByName('type')?.formattedValue()).toEqual('OPEN');
    expect(wmData.getChildByName('createTimeNs')?.formattedValue()).toEqual(
      '2023-11-21T13:38:23.083364560',
    );
  });
});
