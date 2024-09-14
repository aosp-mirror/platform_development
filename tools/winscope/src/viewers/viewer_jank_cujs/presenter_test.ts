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
import {UiData} from './ui_data';

class PresenterJankCujsTest extends AbstractLogViewerPresenterTest<UiData> {
  private trace: Trace<PropertyTreeNode> | undefined;
  private positionUpdate: TracePositionUpdate | undefined;
  private secondPositionUpdate: TracePositionUpdate | undefined;

  override readonly shouldExecuteHeaderTests = true;
  override readonly shouldExecuteFilterTests = false;
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

  override async createPresenterWithEmptyTrace(
    callback: NotifyLogViewCallbackType<UiData>,
  ): Promise<Presenter> {
    const trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.CUJS)
      .setEntries([])
      .build();
    return new Presenter(trace, callback);
  }

  override async createPresenter(
    callback: NotifyLogViewCallbackType<UiData>,
  ): Promise<Presenter> {
    const trace = assertDefined(this.trace);
    const traces = new Traces();
    traces.addTrace(trace);

    const presenter = new Presenter(trace, callback);
    await presenter.onAppEvent(this.getPositionUpdate()); // trigger initialization
    return presenter;
  }

  override getPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.positionUpdate);
  }

  override getSecondPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.secondPositionUpdate);
  }

  override executePropertiesChecksAfterPositionUpdate(uiData: UiData) {
    const cujTypeValues = uiData.entries.map((entry) => {
      return entry.fields[0].value;
    });
    expect(cujTypeValues).toEqual([
      'CUJ_LAUNCHER_QUICK_SWITCH (11)',
      'CUJ_LAUNCHER_APP_CLOSE_TO_HOME (9)',
      'CUJ_LAUNCHER_APP_SWIPE_TO_RECENTS (66)',
      'CUJ_LAUNCHER_OPEN_ALL_APPS (25)',
      'CUJ_LAUNCHER_CLOSE_ALL_APPS_SWIPE (67)',
      'CUJ_LAUNCHER_APP_LAUNCH_FROM_ICON (8)',
      'CUJ_SPLASHSCREEN_EXIT_ANIM (39)',
      'CUJ_LAUNCHER_QUICK_SWITCH (11)',
      'CUJ_LAUNCHER_APP_CLOSE_TO_HOME (9)',
      'CUJ_LAUNCHER_APP_SWIPE_TO_RECENTS (66)',
      'CUJ_NOTIFICATION_SHADE_EXPAND_COLLAPSE (0)',
      'CUJ_NOTIFICATION_SHADE_EXPAND_COLLAPSE (0)',
      'CUJ_NOTIFICATION_SHADE_QS_EXPAND_COLLAPSE (5)',
      'CUJ_NOTIFICATION_SHADE_QS_EXPAND_COLLAPSE (5)',
      'CUJ_NOTIFICATION_SHADE_EXPAND_COLLAPSE (0)',
      'CUJ_NOTIFICATION_SHADE_EXPAND_COLLAPSE (0)',
    ]);
  }
}

describe('PresenterJankCujsTest', () => {
  new PresenterJankCujsTest().execute();
});
