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
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {NotifyLogViewCallbackType} from 'viewers/common/abstract_log_viewer_presenter';
import {AbstractLogViewerPresenterTest} from 'viewers/common/abstract_log_viewer_presenter_test';
import {LogHeader} from 'viewers/common/ui_data_log';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

class PresenterJankCujsTest extends AbstractLogViewerPresenterTest<UiData> {
  override readonly expectedHeaders = [
    {
      header: new LogHeader({
        name: 'Type',
        cssClass: 'jank-cuj-type',
      }),
    },
    {
      header: new LogHeader({
        name: 'Start Time',
        cssClass: 'start-time time',
      }),
    },
    {
      header: new LogHeader({
        name: 'End Time',
        cssClass: 'end-time time',
      }),
    },
    {
      header: new LogHeader({
        name: 'Duration',
        cssClass: 'duration right-align',
      }),
    },
    {
      header: new LogHeader({
        name: 'Status',
        cssClass: 'status right-align',
      }),
    },
  ];
  private trace: Trace<PropertyTreeNode> | undefined;
  private positionUpdate: TracePositionUpdate | undefined;

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
  }

  override async createPresenterWithEmptyTrace(
    callback: NotifyLogViewCallbackType<UiData>,
  ): Promise<Presenter> {
    const trace = UnitTestUtils.makeEmptyTrace(TraceType.CUJS);
    return new Presenter(trace, new InMemoryStorage(), callback);
  }

  override async createPresenter(
    callback: NotifyLogViewCallbackType<UiData>,
  ): Promise<Presenter> {
    const trace = assertDefined(this.trace);
    const traces = new Traces();
    traces.addTrace(trace);

    const presenter = new Presenter(trace, new InMemoryStorage(), callback);
    await presenter.onAppEvent(this.getPositionUpdate()); // trigger initialization
    return presenter;
  }

  override getPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.positionUpdate);
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
