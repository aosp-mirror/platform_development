/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {CdkVirtualScrollViewport} from '@angular/cdk/scrolling';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {getTracesParser} from 'test/unit/fixture_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {Parser} from 'trace/parser';
import {TraceEntry} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {AbstractLogViewerComponentTest} from 'viewers/common/abstract_log_viewer_component_test';
import {LogEntry, LogHeader} from 'viewers/common/ui_data_log';
import {CujEntry, UiData} from './ui_data';
import {ViewerJankCujsComponent} from './viewer_jank_cujs_component';

class ViewerJankCujsComponentTest extends AbstractLogViewerComponentTest<ViewerJankCujsComponent> {
  protected override readonly testProperties = false;
  protected override readonly testScroll = false;
  protected override readonly hasCurrentTimeButton = false;
  protected override readonly hasFilters = false;

  protected override checkTimestampInTable(
    dom: DOMTestHelper<ViewerJankCujsComponent>,
  ): void {
    expect(dom.find('.scroll .entry .time')).toBeUndefined();
  }

  protected async setUpTestEnvironment(): Promise<
    [
      DOMTestHelper<ViewerJankCujsComponent>,
      CdkVirtualScrollViewport,
      ViewerJankCujsComponent,
    ]
  > {
    const parser = (
      await getTracesParser([
        'traces/elapsed_and_real_timestamp/eventlog.winscope',
      ])
    ).tracesParser as Parser<PropertyTreeNode>;

    const trace = new TraceBuilder<PropertyTreeNode>()
      .setParser(parser)
      .setType(TraceType.CUJS)
      .build();

    const entry = trace.getEntry(0);
    const cujEntries = [
      this.createMockCujEntry(entry),
      this.createMockCujEntry(entry),
      this.createMockCujEntry(entry),
      this.createMockCujEntry(entry),
    ];

    const uiData = UiData.createEmpty();
    uiData.headers = [new LogHeader(this.testSpec)];
    uiData.entries = cujEntries;
    uiData.selectedIndex = 0;

    return this.initializeTestEnvironment(uiData, ViewerJankCujsComponent);
  }

  private createMockCujEntry(entry: TraceEntry<PropertyTreeNode>): LogEntry {
    return new CujEntry(
      entry,
      [
        this.testField,
        this.testField,
        this.testField,
        this.testField,
        this.testField,
      ],
      undefined,
    );
  }
}

describe('ViewerJankCujsComponent', () => {
  new ViewerJankCujsComponentTest().execute();
});
