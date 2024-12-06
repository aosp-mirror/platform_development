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
  ];
  private trace: Trace<QueryResult> | undefined;
  private positionUpdate: TracePositionUpdate | undefined;
  private spyIter: jasmine.SpyObj<RowIterator<Row>> | undefined;

  override async setUpTestEnvironment(): Promise<void> {
    const time100 = TimestampConverterUtils.makeRealTimestamp(100n);
    const [spyQueryResult, spyIter] =
      UnitTestUtils.makeSearchTraceSpies(time100);
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
    const [spyQueryResult, spyIter] =
      UnitTestUtils.makeSearchTraceSpies(time100);
    this.spyIter = spyIter;
    const trace = UnitTestUtils.makeEmptyTrace(TraceType.SEARCH);
    return new SearchResultPresenter(
      'fake query',
      trace,
      callback,
      spyQueryResult,
    );
  }

  override async createPresenter(
    callback: NotifyLogViewCallbackType<SearchResult>,
    trace = assertDefined(this.trace),
    positionUpdate = assertDefined(this.getPositionUpdate()),
  ): Promise<SearchResultPresenter> {
    const presenter = new SearchResultPresenter(
      'successful query',
      trace,
      callback,
      await trace.getEntry(0).getValue(),
    );
    await presenter.onAppEvent(positionUpdate); // trigger initialization
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
          {spec: this.expectedHeaders[1].header.spec, value: 'test_value'},
        ],
        propertiesTree: undefined,
      },
    ]);
  }
}

describe('SearchResultPresenterTest', () => {
  new SearchResultPresenterTest().execute();
});
