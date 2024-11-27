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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {assertDefined} from 'common/assert_utils';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {
  AbstractLogViewerPresenter,
  NotifyLogViewCallbackType,
} from './abstract_log_viewer_presenter';
import {LogSelectFilter, LogTextFilter} from './log_filters';
import {LogHeader, UiDataLog} from './ui_data_log';

export abstract class AbstractLogViewerPresenterTest<UiData extends UiDataLog> {
  execute() {
    describe('Common tests', () => {
      let uiData: UiData;
      let presenter: AbstractLogViewerPresenter<UiData>;
      beforeAll(async () => {
        await this.setUpTestEnvironment();
      });

      beforeEach(async () => {
        jasmine.addCustomEqualityTester(filterEqualityTester);
        presenter = await this.createPresenter((newData) => {
          uiData = newData;
        });
      });

      it('is robust to empty trace', async () => {
        const presenter = await this.createPresenterWithEmptyTrace(
          (newData: UiData) => (uiData = newData),
        );
        await presenter.onAppEvent(
          TracePositionUpdate.fromTimestamp(
            TimestampConverterUtils.makeRealTimestamp(0n),
          ),
        );
        for (const [index, expectedHeader] of this.expectedHeaders.entries()) {
          const header = uiData.headers[index];
          expect(header.spec).toEqual(expectedHeader.header.spec);
          if (expectedHeader.options) {
            expect((header.filter as LogSelectFilter)?.options).toEqual([]);
          }
        }
        if (this.executePropertiesChecksForEmptyTrace) {
          this.executePropertiesChecksForEmptyTrace(uiData);
        }
      });

      it('processes trace position updates', async () => {
        await assertDefined(presenter).onAppEvent(
          assertDefined(this.getPositionUpdate()),
        );
        for (const [index, expectedHeader] of this.expectedHeaders.entries()) {
          const header = uiData.headers[index];
          expect(header).toEqual(expectedHeader.header);
          if (expectedHeader.options) {
            expect((header.filter as LogSelectFilter).options).toEqual(
              expectedHeader.options,
            );
          }
        }
        if (this.executePropertiesChecksAfterPositionUpdate) {
          this.executePropertiesChecksAfterPositionUpdate(uiData);
        }
      });
    });

    function filterEqualityTester(
      first: any,
      second: any,
    ): boolean | undefined {
      if (first instanceof LogTextFilter && second instanceof LogTextFilter) {
        return (
          first.textFilter.filterString === second.textFilter.filterString &&
          first.textFilter.flags.length === second.textFilter.flags.length &&
          first.textFilter.flags.every(
            (flag, index) => flag === second.textFilter.flags[index],
          )
        );
      }
      if (
        first instanceof LogSelectFilter &&
        second instanceof LogSelectFilter
      ) {
        return (
          first.options.length === second.options.length &&
          first.shouldFilterBySubstring === second.shouldFilterBySubstring
        );
      }
      return undefined;
    }

    if (this.executeSpecializedTests) {
      this.executeSpecializedTests();
    }
  }

  abstract readonly expectedHeaders: Array<{
    header: LogHeader;
    options?: string[];
  }>;

  abstract setUpTestEnvironment(): Promise<void>;
  abstract createPresenter(
    callback: NotifyLogViewCallbackType<UiData>,
  ): Promise<AbstractLogViewerPresenter<UiData>>;
  abstract createPresenterWithEmptyTrace(
    callback: NotifyLogViewCallbackType<UiData>,
  ): Promise<AbstractLogViewerPresenter<UiData>>;
  abstract getPositionUpdate(): TracePositionUpdate;

  executePropertiesChecksForEmptyTrace?(uiData: UiDataLog): void;
  executePropertiesChecksAfterPositionUpdate?(uiData: UiDataLog): void;
  executeSpecializedTests?(): void;
}
