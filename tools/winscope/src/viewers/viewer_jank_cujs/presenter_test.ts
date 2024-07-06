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
import {TracesBuilder} from 'test/unit/traces_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {NotifyLogViewCallbackType} from 'viewers/common/abstract_log_viewer_presenter';
import {AbstractLogViewerPresenterTest} from 'viewers/common/abstract_log_viewer_presenter_test';
import {Presenter} from './presenter';

class PresenterJankCujsTest extends AbstractLogViewerPresenterTest {
  private trace: Trace<PropertyTreeNode> | undefined;
  private positionUpdate: TracePositionUpdate | undefined;
  private secondPositionUpdate: TracePositionUpdate | undefined;

  override readonly shouldExecuteHeaderTests = true;
  override readonly shouldExecuteFilterTests = false;
  override readonly shouldExecuteCurrentIndexTests = false;
  override readonly shouldExecutePropertiesTests = true;

  override readonly totalOutputEntries = 16;
  override readonly expectedIndexOfFirstPositionUpdate = 0;
  override readonly expectedIndexOfSecondPositionUpdate = 2;
  override readonly logEntryClickIndex = 3;

  override async setUpTestEnvironment(): Promise<void> {
    const parser = (await UnitTestUtils.getTracesParser([
      'traces/eventlog.winscope',
    ])) as Parser<PropertyTreeNode>;

    this.trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.CUJS)
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
    callback: NotifyLogViewCallbackType,
  ): Presenter {
    const traces = new TracesBuilder()
      .setEntries(TraceType.TRANSITION, [])
      .build();
    const trace = assertDefined(traces.getTrace(TraceType.TRANSITION));
    return new Presenter(trace, callback);
  }

  override async createPresenter(
    callback: NotifyLogViewCallbackType,
  ): Promise<Presenter> {
    const trace = assertDefined(this.trace);
    const traces = new Traces();
    traces.addTrace(trace);

    const presenter = new Presenter(
      trace,
      callback as NotifyLogViewCallbackType,
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

describe('PresenterJankCujsTest', () => {
  new PresenterJankCujsTest().execute();
});
