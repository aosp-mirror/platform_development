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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {CdkVirtualScrollViewport} from '@angular/cdk/scrolling';
import {TimestampConverterUtils} from 'common/time/test_utils';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {AbstractLogViewerComponentTest} from 'viewers/common/abstract_log_viewer_component_test';
import {LogSelectFilter} from 'viewers/common/log_filters';
import {LogHeader} from 'viewers/common/ui_data_log';
import {ProtologEntry, UiData} from './ui_data';
import {ViewerProtologComponent} from './viewer_protolog_component';

class ViewerProtologComponentTest extends AbstractLogViewerComponentTest<ViewerProtologComponent> {
  protected override readonly testProperties = false;
  protected override readonly hasCurrentTimeButton = true;
  protected override readonly testScroll = true;

  protected override checkTimestampInTable(
    dom: DOMTestHelper<ViewerProtologComponent>,
  ): void {
    const entryTimestamp = dom.get('.scroll .entry .time');
    entryTimestamp.checkTextExact('10ns');
  }

  protected async setUpTestEnvironment(): Promise<
    [
      DOMTestHelper<ViewerProtologComponent>,
      CdkVirtualScrollViewport,
      ViewerProtologComponent,
    ]
  > {
    const propertiesTree = new PropertyTreeBuilder()
      .setRootId('Protolog')
      .setName('tree')
      .setValue(null)
      .build();
    const ts = TimestampConverterUtils.makeElapsedTimestamp(10n);
    const trace = new TraceBuilder<PropertyTreeNode>()
      .setEntries([propertiesTree, propertiesTree])
      .setTimestamps([ts, ts])
      .build();

    const messages: ProtologEntry[] = [];
    const shortMessage = 'test information about message';
    const longMessage = shortMessage.repeat(10) + 'keep';
    const traceEntry = trace.getEntry(0);
    for (let i = 0; i < 200; i++) {
      messages.push(
        new ProtologEntry(traceEntry, [
          this.testField,
          this.testField,
          this.testField,
          {
            spec: {name: 'Test Column Text', cssClass: 'test-class-text'},
            value: i % 2 === 0 ? shortMessage : longMessage,
          },
        ]),
      );
    }
    const uiData = new UiData(
      [new LogHeader(this.testSpec, new LogSelectFilter([]))],
      messages,
      150,
      undefined,
      undefined,
    );
    return await this.initializeTestEnvironment(
      uiData,
      ViewerProtologComponent,
    );
  }
}

describe('ViewerProtologComponent', () => {
  new ViewerProtologComponentTest().execute();
});
