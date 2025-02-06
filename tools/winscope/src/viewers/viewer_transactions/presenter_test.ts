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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANYf KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {assertDefined} from 'common/assert_utils';
import {InMemoryStorage} from 'common/store/in_memory_storage';
import {TimeUtils} from 'common/time/time_utils';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {NotifyLogViewCallbackType} from 'viewers/common/abstract_log_viewer_presenter';
import {AbstractLogViewerPresenterTest} from 'viewers/common/abstract_log_viewer_presenter_test';
import {LogSelectFilter} from 'viewers/common/log_filters';
import {LogHeader} from 'viewers/common/ui_data_log';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

class PresenterTransactionsTest extends AbstractLogViewerPresenterTest<UiData> {
  override readonly expectedHeaders = [
    {
      header: new LogHeader(
        {name: 'TX ID', cssClass: 'transaction-id right-align'},
        new LogSelectFilter(Array.from({length: 1295}, () => '')),
      ),
    },
    {
      header: new LogHeader(
        {name: 'VSYNC ID', cssClass: 'vsyncid right-align'},
        new LogSelectFilter(Array.from({length: 710}, () => '')),
      ),
    },
    {
      header: new LogHeader(
        {name: 'PID', cssClass: 'pid right-align'},
        new LogSelectFilter(Array.from({length: 8}, () => '')),
      ),
      options: ['N/A', '0', '515', '1593', '2022', '2322', '2463', '3300'],
    },
    {
      header: new LogHeader(
        {name: 'UID', cssClass: 'uid right-align'},
        new LogSelectFilter(Array.from({length: 6}, () => '')),
      ),
      options: ['N/A', '1000', '1003', '10169', '10235', '10239'],
    },
    {
      header: new LogHeader(
        {name: 'TYPE', cssClass: 'transaction-type'},
        new LogSelectFilter(Array.from({length: 6}, () => '')),
      ),
      options: [
        'DISPLAY_CHANGED',
        'LAYER_ADDED',
        'LAYER_CHANGED',
        'LAYER_DESTROYED',
        'LAYER_HANDLE_DESTROYED',
        'NO_OP',
      ],
    },
    {
      header: new LogHeader(
        {name: 'LAYER/DISP ID', cssClass: 'layer-or-display-id right-align'},
        new LogSelectFilter(Array.from({length: 117}, () => '')),
      ),
    },
    {
      header: new LogHeader(
        {name: 'Flags', cssClass: 'flags'},
        new LogSelectFilter(
          Array.from({length: 30}, () => ''),
          true,
          '250',
          '100%',
        ),
      ),
    },
  ];
  private trace: Trace<PropertyTreeNode> | undefined;
  private positionUpdate: TracePositionUpdate | undefined;

  override executeSpecializedTests() {
    describe('Specialized tests', () => {
      let presenter: Presenter;
      let uiData: UiData;

      beforeAll(async () => {
        await this.setUpTestEnvironment();
      });

      beforeEach(async () => {
        presenter = await this.createPresenter((newData: UiData) => {
          uiData = newData;
        });
      });

      it('keeps properties related to what has changed regardless of hide defaults', async () => {
        await presenter.onAppEvent(this.getPositionUpdate());
        await TimeUtils.wait(() => !uiData.isFetchingData);
        await presenter.onLogEntryClick(10);
        expect(
          assertDefined(uiData.propertiesTree).getAllChildren().length,
        ).toEqual(8);
        expect(
          uiData.propertiesTree?.getChildByName('transformToDisplayInverse'),
        ).toBeDefined();
        expect(
          uiData.propertiesTree?.getChildByName('destinationFrame'),
        ).toBeDefined();
        expect(
          uiData.propertiesTree?.getChildByName('autoRefresh'),
        ).toBeDefined();

        await presenter.onLogEntryClick(279);
        expect(uiData.propertiesTree?.getChildByName('flags')).toBeDefined();
        expect(uiData.propertiesTree?.getChildByName('parentId')).toBeDefined();
        expect(
          uiData.propertiesTree?.getChildByName('relativeParentId'),
        ).toBeDefined();
        expect(
          uiData.propertiesTree?.getChildByName('transformToDisplayInverse'),
        ).toBeUndefined();
        expect(
          uiData.propertiesTree?.getChildByName('destinationFrame'),
        ).toBeUndefined();
        expect(
          uiData.propertiesTree?.getChildByName('autoRefresh'),
        ).toBeUndefined();

        await presenter.onLogEntryClick(584);
        expect(uiData.propertiesTree?.getChildByName('flags')).toBeDefined();
        expect(uiData.propertiesTree?.getChildByName('layerId')).toBeDefined();
        expect(uiData.propertiesTree?.getChildByName('x')).toBeDefined();
        expect(uiData.propertiesTree?.getChildByName('y')).toBeDefined();
        expect(uiData.propertiesTree?.getChildByName('z')).toBeDefined();
        expect(
          uiData.propertiesTree?.getChildByName('parentId'),
        ).toBeUndefined();
        expect(
          uiData.propertiesTree?.getChildByName('relativeParentId'),
        ).toBeUndefined();
      });
    });
  }

  override async setUpTestEnvironment(): Promise<void> {
    const parser = (await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/Transactions.pb',
    )) as Parser<PropertyTreeNode>;
    this.trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.TRANSACTIONS)
      .setParser(parser)
      .build();
    this.positionUpdate = TracePositionUpdate.fromTraceEntry(
      this.trace.getEntry(0),
    );
  }

  override async createPresenterWithEmptyTrace(
    callback: NotifyLogViewCallbackType<UiData>,
  ): Promise<Presenter> {
    const trace = UnitTestUtils.makeEmptyTrace(TraceType.TRANSACTIONS);
    return new Presenter(trace, new InMemoryStorage(), callback);
  }

  override async createPresenter(
    callback: NotifyLogViewCallbackType<UiData>,
  ): Promise<Presenter> {
    const presenter = new Presenter(
      assertDefined(this.trace),
      new InMemoryStorage(),
      callback,
    );
    await presenter.onAppEvent(this.getPositionUpdate()); // trigger initialization
    return presenter;
  }

  override getPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.positionUpdate);
  }
}

describe('PresenterTransactions', () => {
  new PresenterTransactionsTest().execute();
});
