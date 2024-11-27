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

import {InMemoryStorage} from 'common/in_memory_storage';
import {TraceSearchQueryAlreadyRun} from 'messaging/user_warnings';
import {
  NewSearchTrace,
  TracePositionUpdate,
  TraceSearchFailed,
  TraceSearchRemovalRequest,
  TraceSearchRequest,
} from 'messaging/winscope_event';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {UnitTestUtils} from 'test/unit/utils';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {QueryResult} from 'trace_processor/query_result';
import {
  DeleteSavedQueryClickDetail,
  QueryClickDetail,
  SaveQueryClickDetail,
  TimestampClickDetail,
  ViewerEvents,
} from 'viewers/common/viewer_events';
import {Presenter} from './presenter';
import {Search, SearchResult, UiData} from './ui_data';

describe('PresenterSearch', () => {
  let presenter: Presenter;
  let uiData: UiData;
  let userNotifierChecker: UserNotifierChecker;
  let element: HTMLElement;
  let emitEventSpy: jasmine.Spy;

  beforeAll(() => {
    userNotifierChecker = new UserNotifierChecker();
    jasmine.addCustomEqualityTester(searchEqualityTester);
  });

  beforeEach(() => {
    presenter = new Presenter(
      new Traces(),
      new InMemoryStorage(),
      (newData: UiData) => (uiData = newData),
    );
    userNotifierChecker.reset();
    element = document.createElement('div');
    presenter.addEventListeners(element);
    emitEventSpy = jasmine.createSpy();
    presenter.setEmitEvent(emitEventSpy);
  });

  it('adds event listeners', () => {
    let spy: jasmine.Spy = spyOn(presenter, 'onSearchQueryClick');
    let testQuery = 'search query';
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.SearchQueryClick, {
        detail: new QueryClickDetail(testQuery),
      }),
    );
    expect(spy).toHaveBeenCalledWith(testQuery);

    spy = spyOn(presenter, 'onResetQueryClick');
    testQuery = 'reset query';
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.ResetQueryClick, {
        detail: new QueryClickDetail(testQuery),
      }),
    );
    expect(spy).toHaveBeenCalledWith(testQuery);

    spy = spyOn(presenter, 'onSaveQueryClick');
    const saveQueryDetail = new SaveQueryClickDetail('save query', 'foo');
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.SaveQueryClick, {
        detail: saveQueryDetail,
      }),
    );
    expect(spy).toHaveBeenCalledWith(
      saveQueryDetail.query,
      saveQueryDetail.name,
    );

    spy = spyOn(presenter, 'onDeleteSavedQueryClick');
    const deleteQueryDetail = new DeleteSavedQueryClickDetail(
      new Search('delete query', 'bar'),
    );
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.DeleteSavedQueryClick, {
        detail: deleteQueryDetail,
      }),
    );
    expect(spy).toHaveBeenCalledWith(deleteQueryDetail.search);
  });

  it('handles search for successful query with zero rows', async () => {
    await runSearchWithNoRowsAndCheckUiData(
      'successful empty query',
      UnitTestUtils.makeEmptyTrace(TraceType.SEARCH),
    );
  });

  it('handles search for successful query with non-zero rows', async () => {
    const testQuery = 'successful non-empty query';
    presenter.onSearchQueryClick(testQuery);
    expect(emitEventSpy).toHaveBeenCalledOnceWith(
      new TraceSearchRequest(testQuery),
    );

    const time100 = TimestampConverterUtils.makeRealTimestamp(100n);
    const [spyQueryResult, spyIter] =
      UnitTestUtils.makeSearchTraceSpies(time100);
    const trace = new TraceBuilder<QueryResult>()
      .setEntries([spyQueryResult])
      .setTimestamps([time100])
      .setType(TraceType.SEARCH)
      .build();

    await presenter.onAppEvent(new NewSearchTrace(trace));
    const expectedSearchResult = new SearchResult(testQuery, [], []);
    expect(uiData.currentSearches).toEqual([expectedSearchResult]);
    expect(uiData.lastTraceFailed).toEqual(false);

    await presenter.onAppEvent(
      TracePositionUpdate.fromTraceEntry(trace.getEntry(0)),
    );
    expect(uiData.currentSearches.length).toEqual(1);
    expect(uiData.currentSearches[0].currentIndex).toEqual(0);
    expect(uiData.currentSearches[0].headers.length).toEqual(2);
    expect(uiData.currentSearches[0].entries.length).toEqual(1);
    expect(uiData.lastTraceFailed).toEqual(false);
    expect(uiData.recentSearches).toEqual([new Search(testQuery)]);

    // adds event listeners and emit event for search presenter
    emitEventSpy.calls.reset();
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.TimestampClick, {
        detail: new TimestampClickDetail(undefined, time100),
      }),
    );
    expect(emitEventSpy).toHaveBeenCalledOnceWith(
      TracePositionUpdate.fromTimestamp(time100, true),
    );
  });

  it('handles search for query already run', () => {
    const testQuery = 'run already query';
    presenter.onSearchQueryClick(testQuery);
    emitEventSpy.calls.reset();

    presenter.onSearchQueryClick(testQuery);
    expect(
      userNotifierChecker.expectNotified([new TraceSearchQueryAlreadyRun()]),
    );
    expect(uiData.lastTraceFailed).toEqual(true);
    expect(emitEventSpy).not.toHaveBeenCalled();
  });

  it('handles search for unsuccessful query', async () => {
    const testQuery = 'unsuccessful query';
    presenter.onSearchQueryClick(testQuery);
    await presenter.onAppEvent(new TraceSearchFailed());
    expect(uiData.lastTraceFailed).toEqual(true);
    expect(uiData.currentSearches).toEqual([]);
    expect(uiData.recentSearches).toEqual([]);
  });

  it('handles new search after previous query already run', () => {
    const testQuery = 'run twice query';
    presenter.onSearchQueryClick(testQuery);
    emitEventSpy.calls.reset();

    presenter.onSearchQueryClick(testQuery);
    expect(
      userNotifierChecker.expectNotified([new TraceSearchQueryAlreadyRun()]),
    );
    expect(uiData.lastTraceFailed).toEqual(true);
    expect(emitEventSpy).not.toHaveBeenCalled();

    const newQuery = 'never run query';
    presenter.onSearchQueryClick(newQuery);
    expect(emitEventSpy).toHaveBeenCalledWith(new TraceSearchRequest(newQuery));
  });

  it('handles reset query click, keeping recent searches', async () => {
    const testQuery = 'reset query';
    const trace = UnitTestUtils.makeEmptyTrace(TraceType.SEARCH);
    await runSearchWithNoRowsAndCheckUiData(testQuery, trace);
    emitEventSpy.calls.reset();

    await presenter.onResetQueryClick(testQuery);
    expect(emitEventSpy).toHaveBeenCalledOnceWith(
      new TraceSearchRemovalRequest(trace),
    );
    expect(uiData.currentSearches).toEqual([]);
    expect(uiData.recentSearches).toEqual([new Search(testQuery)]);
    emitEventSpy.calls.reset();

    const newQuery = 'new reset query';
    await runSearchWithNoRowsAndCheckUiData(newQuery, trace);
    emitEventSpy.calls.reset();

    // check removed presenter cannot still affect ui data
    element.dispatchEvent(new CustomEvent(ViewerEvents.ArrowDownPress));
    expect(uiData.currentSearches.length).toEqual(1);

    await presenter.onResetQueryClick(newQuery);
    expect(emitEventSpy).toHaveBeenCalledOnceWith(
      new TraceSearchRemovalRequest(trace),
    );
    expect(uiData.currentSearches).toEqual([]);
    expect(uiData.recentSearches).toEqual([
      new Search(newQuery),
      new Search(testQuery),
    ]);
  });

  it('handles save query click', () => {
    const testQuery = 'save query';
    const testName = 'save name';
    presenter.onSaveQueryClick(testQuery, testName);
    const testSearch = new Search(testQuery, testName);
    expect(uiData.savedSearches).toEqual([testSearch]);

    const newQuery = 'new save query';
    const newName = 'new save name';
    presenter.onSaveQueryClick(newQuery, newName);
    const newSearch = new Search(newQuery, newName);
    expect(uiData.savedSearches).toEqual([newSearch, testSearch]);
  });

  it('handles delete saved query click', () => {
    const testQuery = 'delete query';
    const testName = 'delete name';
    const testSearch = new Search(testQuery, testName);
    presenter.onDeleteSavedQueryClick(testSearch);
    expect(uiData.savedSearches).toEqual([]);

    presenter.onSaveQueryClick(testQuery, testName);
    expect(uiData.savedSearches).toEqual([testSearch]);

    presenter.onDeleteSavedQueryClick(uiData.savedSearches[0]);
    expect(uiData.savedSearches).toEqual([]);
  });

  function searchEqualityTester(first: any, second: any): boolean | undefined {
    if (first instanceof Search && second instanceof Search) {
      return first.query === second.query && first.name === second.name;
    }
    return undefined;
  }

  async function runSearchWithNoRowsAndCheckUiData(
    testQuery: string,
    trace: Trace<QueryResult>,
  ) {
    presenter.onSearchQueryClick(testQuery);
    expect(emitEventSpy).toHaveBeenCalledOnceWith(
      new TraceSearchRequest(testQuery),
    );
    await presenter.onAppEvent(new NewSearchTrace(trace));
    expect(uiData.currentSearches).toEqual([
      new SearchResult(testQuery, [], []),
    ]);
    expect(uiData.lastTraceFailed).toEqual(false);
    const expectedSearch = new Search(testQuery);
    expect(uiData.recentSearches[0]).toEqual(expectedSearch);
  }
});
