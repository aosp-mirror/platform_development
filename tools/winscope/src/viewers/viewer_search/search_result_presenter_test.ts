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
import {TimestampConverterUtils} from 'common/time/test_utils';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {QueryResult, Row, RowIterator} from 'trace_processor/query_result';
import {NotifyLogViewCallbackType} from 'viewers/common/abstract_log_viewer_presenter';
import {AbstractLogViewerPresenterTest} from 'viewers/common/abstract_log_viewer_presenter_test';
import {LogHeader} from 'viewers/common/ui_data_log';
import {SearchResultPresenter} from './search_result_presenter';
import {SearchResult} from './ui_data';

class SearchResultPresenterTest extends AbstractLogViewerPresenterTest<SearchResult> {
  override readonly expectedHeaders = [
    {
      header: new LogHeader({
        name: 'ts',
        cssClass: 'search-result',
      }),
    },
    {
      header: new LogHeader({
        name: 'property',
        cssClass: 'search-result',
      }),
    },
    {
      header: new LogHeader({
        name: 'value',
        cssClass: 'search-result',
      }),
    },
  ];
  private trace: Trace<QueryResult> | undefined;
  private positionUpdate: TracePositionUpdate | undefined;
  private spyIter: jasmine.SpyObj<RowIterator<Row>> | undefined;

  override async setUpTestEnvironment(): Promise<void> {
    const time100 = TimestampConverterUtils.makeRealTimestamp(100n);
    const [spyQueryResult, spyIter] = UnitTestUtils.makeSearchTraceSpies(
      time100,
      123,
    );
    this.spyIter = spyIter;
    this.trace = new TraceBuilder<QueryResult>()
      .setEntries([spyQueryResult])
      .setTimestamps([time100])
      .setType(TraceType.SEARCH)
      .build();
    this.positionUpdate = TracePositionUpdate.fromTraceEntry(
      this.trace.getEntry(0),
    );
  }

  override resetTestEnvironment() {
    assertDefined(this.spyIter).valid.and.returnValue(true);
  }

  override async createPresenterWithEmptyTrace(
    callback: NotifyLogViewCallbackType<SearchResult>,
  ): Promise<SearchResultPresenter> {
    const time100 = TimestampConverterUtils.makeRealTimestamp(100n);
    const [spyQueryResult, spyIter] = UnitTestUtils.makeSearchTraceSpies(
      time100,
      123,
    );
    this.spyIter = spyIter;
    const trace = UnitTestUtils.makeEmptyTrace(TraceType.SEARCH);
    return new SearchResultPresenter(
      trace,
      callback,
      (valueNs: bigint) => TimestampConverterUtils.makeRealTimestamp(valueNs),
      spyQueryResult,
    );
  }

  override async createPresenter(
    callback: NotifyLogViewCallbackType<SearchResult>,
    trace = assertDefined(this.trace),
    positionUpdate?: TracePositionUpdate,
  ): Promise<SearchResultPresenter> {
    const presenter = new SearchResultPresenter(
      trace,
      callback,
      (valueNs: bigint) => TimestampConverterUtils.makeRealTimestamp(valueNs),
      await trace.getEntry(0).getValue(),
    );
    if (positionUpdate) {
      await presenter.onAppEvent(positionUpdate); // trigger initialization
    }
    return presenter;
  }

  override getPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.positionUpdate);
  }

  override executePropertiesChecksAfterPositionUpdate(result: SearchResult) {
    const firstEntry = assertDefined(this.trace).getEntry(0);
    expect(result.entries).toEqual([
      {
        traceEntry: firstEntry,
        fields: [
          {
            spec: this.expectedHeaders[0].header.spec,
            value: firstEntry.getTimestamp(),
          },
          {spec: this.expectedHeaders[1].header.spec, value: 'test_property'},
          {spec: this.expectedHeaders[2].header.spec, value: 123},
        ],
        propertiesTree: undefined,
      },
    ]);
  }

  override executeSpecializedTests() {
    describe('Specialized tests', () => {
      let result: SearchResult;

      it("does not convert 'ts' column value to timestamp if entry timestamp is not valid", async () => {
        const time0 = TimestampConverterUtils.makeZeroTimestamp();
        const [spyQueryResult, spyIter] =
          UnitTestUtils.makeSearchTraceSpies(time0);
        const trace = new TraceBuilder<QueryResult>()
          .setEntries([spyQueryResult])
          .setTimestamps([time0])
          .setType(TraceType.SEARCH)
          .build();
        await this.createPresenter(
          (newResult) => {
            result = newResult;
          },
          trace,
          TracePositionUpdate.fromTraceEntry(trace.getEntry(0)),
        );
        expect(result.entries[0].fields[0].value).toEqual(0);
      });

      describe('value conversions', () => {
        let presenter: SearchResultPresenter;

        beforeAll(async () => {
          await this.setUpTestEnvironment();
        });

        beforeEach(async () => {
          presenter = await this.createPresenter((newResult) => {
            result = newResult;
          }, undefined);
          this.resetTestEnvironment();
        });

        it("converts 'value' column string value to timestamp if 'property' value ends in 'time_ns'", async () => {
          this.spyIter?.get
            .withArgs('property')
            .and.returnValue('test_time_ns');
          this.spyIter?.get.withArgs('value').and.returnValue('123');
          await presenter.onAppEvent(assertDefined(this.getPositionUpdate()));
          expect(result.entries[0].fields[1].value).toEqual('test_time_ns');
          expect(result.entries[0].fields[2].value).toEqual(
            TimestampConverterUtils.makeRealTimestamp(123n),
          );
        });

        it("converts value to 'NULL' if null", async () => {
          this.spyIter?.get.withArgs('value').and.returnValue(null);
          await presenter.onAppEvent(assertDefined(this.getPositionUpdate()));
          expect(result.entries[0].fields[2].value).toEqual('NULL');
        });

        it('converts value to number if bigint', async () => {
          this.spyIter?.get.withArgs('value').and.returnValue(321n);
          await presenter.onAppEvent(assertDefined(this.getPositionUpdate()));
          expect(result.entries[0].fields[2].value).toEqual(321);
        });

        it("converts value to '[]' if Uint8Array", async () => {
          this.spyIter?.get.withArgs('value').and.returnValue(new Uint8Array());
          await presenter.onAppEvent(assertDefined(this.getPositionUpdate()));
          expect(result.entries[0].fields[2].value).toEqual('[]');
        });
      });
    });
  }
}

describe('SearchResultPresenterTest', () => {
  new SearchResultPresenterTest().execute();
});
