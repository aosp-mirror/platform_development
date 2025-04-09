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
import {TimestampConverterUtils} from 'common/time/test_utils';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {TraceEntry} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {AbstractLogViewerComponentTest} from 'viewers/common/abstract_log_viewer_component_test';
import {LogSelectFilter} from 'viewers/common/log_filters';
import {LogHeader} from 'viewers/common/ui_data_log';
import {TransitionsScrollDirective} from './scroll_strategy/transitions_scroll_directive';
import {TransitionsEntry, UiData} from './ui_data';
import {ViewerTransitionsComponent} from './viewer_transitions_component';

class ViewerTransitionsComponentTest extends AbstractLogViewerComponentTest<ViewerTransitionsComponent> {
  protected override readonly testProperties = true;
  protected override readonly hasCurrentTimeButton = false;
  protected override readonly testScroll = true;
  protected override readonly propertiesSectionTitle = 'SELECTED TRANSITION';
  protected override readonly propertiesPlaceholder =
    'No current or selected transition.';

  private readonly transitionTree = new PropertyTreeBuilder()
    .setIsRoot(true)
    .setRootId('TransitionTraceEntry')
    .setName('transition')
    .build();

  protected override checkTimestampInTable(
    dom: DOMTestHelper<ViewerTransitionsComponent>,
  ): void {
    expect(dom.find('.scroll .entry .time')).toBeUndefined();
  }

  protected async setUpTestEnvironment(): Promise<
    [
      DOMTestHelper<ViewerTransitionsComponent>,
      CdkVirtualScrollViewport,
      ViewerTransitionsComponent,
    ]
  > {
    const trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.TRANSITION)
      .setEntries([this.transitionTree])
      .setTimestamps([TimestampConverterUtils.makeElapsedTimestamp(20n)])
      .build();
    const entry = trace.getEntry(0);

    const transitions = [];
    for (let i = 0; i < 200; i++) {
      transitions.push(this.createMockTransition(entry, i));
    }
    const uiData = UiData.createEmpty();
    uiData.headers = [new LogHeader(this.testSpec, new LogSelectFilter([]))];
    uiData.entries = transitions;
    uiData.selectedIndex = 0;

    return await this.initializeTestEnvironment(
      uiData,
      ViewerTransitionsComponent,
      [TransitionsScrollDirective],
    );
  }

  private createMockTransition(
    entry: TraceEntry<PropertyTreeNode>,
    i: number,
  ): TransitionsEntry {
    return new TransitionsEntry(
      entry,
      [
        this.testField,
        this.testField,
        this.testField,
        this.testField,
        this.testField,
        this.testField,
        {
          spec: this.testSpec,
          value: i % 2 === 0 ? 'VALUE' : 'VALUE'.repeat(40),
        },
      ],
      this.transitionTree,
    );
  }
}

describe('ViewerTransitionsComponent', () => {
  new ViewerTransitionsComponentTest().execute();
});
