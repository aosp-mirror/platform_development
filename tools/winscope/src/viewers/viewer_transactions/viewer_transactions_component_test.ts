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
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {TraceType} from 'trace/trace_type';
import {TransactionColumnType} from 'trace/transactions/transaction_column_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {AbstractLogViewerComponentTest} from 'viewers/common/abstract_log_viewer_component_test';
import {LogSelectFilter} from 'viewers/common/log_filters';
import {LogHeader} from 'viewers/common/ui_data_log';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {TransactionsEntry, UiData} from './ui_data';
import {ViewerTransactionsComponent} from './viewer_transactions_component';

class ViewerTransactionsComponentTest extends AbstractLogViewerComponentTest<ViewerTransactionsComponent> {
  protected override readonly testProperties = true;
  protected override readonly hasCurrentTimeButton = true;
  protected override readonly testScroll = true;
  protected override readonly propertiesSectionTitle =
    'PROPERTIES - PROTO DUMP';
  protected override readonly propertiesPlaceholder =
    'No current or selected transaction with additional properties.';

  protected override checkTimestampInTable(
    dom: DOMTestHelper<ViewerTransactionsComponent>,
  ): void {
    const entryTimestamp = dom.get('.scroll .entry .time');
    entryTimestamp.checkTextExact('1ns');
  }

  protected async setUpTestEnvironment(): Promise<
    [
      DOMTestHelper<ViewerTransactionsComponent>,
      CdkVirtualScrollViewport,
      ViewerTransactionsComponent,
    ]
  > {
    const hierarchyTree = new HierarchyTreeBuilder()
      .setId('Transactions')
      .setName('tree')
      .build();

    const propertiesTree = new PropertyTreeBuilder()
      .setRootId('Transactions')
      .setName('tree')
      .setValue(null)
      .build();

    const ts = TimestampConverterUtils.makeElapsedTimestamp(1n);

    const trace = new TraceBuilder<HierarchyTreeNode>()
      .setEntries([hierarchyTree, hierarchyTree])
      .setTimestamps([ts, ts])
      .build();

    const entry1 = new TransactionsEntry(
      trace.getEntry(0),
      Array.from({length: 8}, () => this.testField),
      async () => propertiesTree,
    );
    entry1.fields[7].spec = {
      name: 'Test Column',
      cssClass: 'test-class',
      columnType: TransactionColumnType.FLAGS,
    };

    const uiData = new UiData(
      [new LogHeader(this.testSpec, new LogSelectFilter([]))],
      [entry1],
      1,
      0,
      0,
      UiPropertyTreeNode.from(propertiesTree),
      {},
    );
    return this.initializeTestEnvironment(uiData, ViewerTransactionsComponent);
  }

  protected override async setUpTestEnvironmentForScroll(): Promise<
    [DOMTestHelper<ViewerTransactionsComponent>, CdkVirtualScrollViewport]
  > {
    const hierarchyTree = new HierarchyTreeBuilder()
      .setId('Transactions')
      .setName('tree')
      .build();

    const propertiesTree = new PropertyTreeBuilder()
      .setRootId('Transactions')
      .setName('tree')
      .setValue(null)
      .build();

    const ts = TimestampConverterUtils.makeElapsedTimestamp(1n);

    const trace = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.TRANSACTIONS)
      .setEntries([hierarchyTree, hierarchyTree])
      .setTimestamps([ts, ts])
      .build();

    const uiData = new UiData(
      [],
      [],
      0,
      0,
      0,
      UiPropertyTreeNode.from(propertiesTree),
      {},
    );
    const shortMessage = 'flag1 | flag2';
    const longMessage = shortMessage.repeat(20);
    const traceEntry = trace.getEntry(0);

    for (let i = 0; i < 200; i++) {
      const entry = new TransactionsEntry(
        traceEntry,
        Array.from({length: 8}, () => this.testField).concat([
          {
            spec: {
              name: 'Test Column Flags',
              cssClass: 'test-class-flags',
              columnType: TransactionColumnType.FLAGS,
            },
            value: i % 2 === 0 ? shortMessage : longMessage,
          },
        ]),
        async () => propertiesTree,
      );
      uiData.entries.push(entry);
    }

    const [dom, viewport] = await this.initializeTestEnvironment(
      uiData,
      ViewerTransactionsComponent,
    );
    return [dom, viewport];
  }
}

describe('ViewerTransactionsComponent', () => {
  new ViewerTransactionsComponentTest().execute();
});
