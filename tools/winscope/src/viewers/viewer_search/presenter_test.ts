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
import {TimestampConverterUtils} from 'common/time/test_utils';
import {
  InitializeTraceSearchRequest,
  TraceAddRequest,
  TracePositionUpdate,
  TraceRemoveRequest,
  TraceSearchFailed,
  TraceSearchInitialized,
  TraceSearchRequest,
} from 'messaging/winscope_event';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {UnitTestUtils} from 'test/unit/utils';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {QueryResult} from 'trace_processor/query_result';
import {
  ClearQueryClickDetail,
  DeleteSavedQueryClickDetail,
  SaveQueryClickDetail,
  SearchQueryClickDetail,
  TimestampClickDetail,
  ViewerEvents,
} from 'viewers/common/viewer_events';
import {Presenter} from './presenter';
import {CurrentSearch, ListedSearch, SearchResult, UiData} from './ui_data';

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
    let spy: jasmine.Spy = spyOn(presenter, 'onGlobalSearchSectionClick');
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.GlobalSearchSectionClick),
    );
    expect(spy).toHaveBeenCalled();

    spy = spyOn(presenter, 'onSearchQueryClick');
    const testQuery = 'search query';
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.SearchQueryClick, {
        detail: new SearchQueryClickDetail(testQuery, 1),
      }),
    );
    expect(spy).toHaveBeenCalledWith(testQuery, 1);

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
      new ListedSearch('delete query', 'bar'),
    );
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.DeleteSavedQueryClick, {
        detail: deleteQueryDetail,
      }),
    );
    expect(spy).toHaveBeenCalledWith(deleteQueryDetail.search);

    spy = spyOn(presenter, 'addSearch');
    element.dispatchEvent(new CustomEvent(ViewerEvents.AddQueryClick));
    expect(spy).toHaveBeenCalled();

    spy = spyOn(presenter, 'onClearQueryClick');
    const clickQueryDetail = new ClearQueryClickDetail(2);
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.ClearQueryClick, {
        detail: clickQueryDetail,
      }),
    );
    expect(spy).toHaveBeenCalledWith(2);
  });

  it('handles trace search initialization', async () => {
    await presenter.onGlobalSearchSectionClick();
    expect(emitEventSpy).toHaveBeenCalledOnceWith(
      new InitializeTraceSearchRequest(),
    );
    expect(uiData.initialized).toBeFalse();

    await presenter.onAppEvent(new TraceSearchInitialized(['test_view']));
    expect(uiData.initialized).toBeTrue();
    expect(uiData.searchViews).toEqual(['test_view']);

    emitEventSpy.calls.reset();
    await presenter.onGlobalSearchSectionClick();
    expect(emitEventSpy).not.toHaveBeenCalled();
  });

  it('handles search for successful query with zero rows', async () => {
    const query = 'successful empty query';
    await runSearchWithNoRowsAndCheckUiData(
      query,
      UnitTestUtils.makeEmptyTrace(TraceType.SEARCH, [query, '1']),
    );
  });

  it('handles search for successful query with non-zero rows', async () => {
    const testQuery = 'successful non-empty query';
    await presenter.onSearchQueryClick(testQuery, 1);
    expect(emitEventSpy).toHaveBeenCalledOnceWith(
      new TraceSearchRequest(testQuery),
    );

    const time100 = TimestampConverterUtils.makeRealTimestamp(100n);
    const [spyQueryResult, spyIter] =
      UnitTestUtils.makeSearchTraceSpies(time100);
    const trace = new TraceBuilder<QueryResult>()
      .setEntries([spyQueryResult])
      .setTimestamps([time100])
      .setDescriptors([testQuery])
      .setType(TraceType.SEARCH)
      .build();

    await presenter.onAppEvent(new TraceAddRequest(trace));
    const expectedSearch = new CurrentSearch(
      1,
      testQuery,
      new SearchResult([], []),
    );
    expect(uiData.currentSearches).toEqual([expectedSearch]);
    expect(uiData.lastTraceFailed).toEqual(false);

    await presenter.onAppEvent(
      TracePositionUpdate.fromTraceEntry(trace.getEntry(0)),
    );
    expect(uiData.currentSearches.length).toEqual(1);
    expect(uiData.currentSearches[0].result?.currentIndex).toEqual(0);
    expect(uiData.currentSearches[0].result?.headers.length).toEqual(2);
    expect(uiData.currentSearches[0].result?.entries.length).toEqual(1);
    expect(uiData.lastTraceFailed).toEqual(false);
    expect(uiData.recentSearches).toEqual([new ListedSearch(testQuery)]);

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

  it('runs same query twice with separate uids', async () => {
    const query = 'successful query';
    await runSearchWithNoRowsAndCheckUiData(
      query,
      UnitTestUtils.makeEmptyTrace(TraceType.SEARCH, [query]),
    );
    emitEventSpy.calls.reset();
    presenter.addSearch();
    await runSearchWithNoRowsAndCheckUiData(
      query,
      UnitTestUtils.makeEmptyTrace(TraceType.SEARCH, [query]),
      2,
      [
        new CurrentSearch(1, query, new SearchResult([], [])),
        new CurrentSearch(2, query, new SearchResult([], [])),
      ],
    );
  });

  it('handles non-search trace added event', async () => {
    const currData = uiData;
    const trace = UnitTestUtils.makeEmptyTrace(TraceType.SURFACE_FLINGER);
    await presenter.onAppEvent(new TraceAddRequest(trace));
    expect(uiData).toEqual(currData);
  });

  it('handles search for unsuccessful query', async () => {
    const testQuery = 'unsuccessful query';
    presenter.onSearchQueryClick(testQuery, 1);
    await presenter.onAppEvent(new TraceSearchFailed());
    expect(uiData.lastTraceFailed).toEqual(true);
    expect(uiData.currentSearches).toEqual([new CurrentSearch(1, testQuery)]);
    expect(uiData.recentSearches).toEqual([]);
  });

  it('clears current search result when query run again, keeping both in recent searches', async () => {
    const testQuery = 'query to be overwritten';
    const trace = UnitTestUtils.makeEmptyTrace(TraceType.SEARCH, [testQuery]);
    await runSearchWithNoRowsAndCheckUiData(testQuery, trace);
    emitEventSpy.calls.reset();

    await presenter.onSearchQueryClick(testQuery, 1);
    expect(emitEventSpy).toHaveBeenCalledWith(new TraceRemoveRequest(trace));
    expect(emitEventSpy).toHaveBeenCalledWith(
      new TraceSearchRequest(testQuery),
    );
    expect(uiData.currentSearches.length).toEqual(1);
    emitEventSpy.calls.reset();

    await presenter.onAppEvent(new TraceSearchFailed());
    expect(uiData.currentSearches.length).toEqual(1);
    expect(uiData.recentSearches).toEqual([new ListedSearch(testQuery)]);
    emitEventSpy.calls.reset();

    const newQuery = 'new query';
    const newTrace = UnitTestUtils.makeEmptyTrace(TraceType.SEARCH, [newQuery]);
    await runSearchWithNoRowsAndCheckUiData(newQuery, newTrace);
    emitEventSpy.calls.reset();

    // check removed presenter cannot still affect ui data
    element.dispatchEvent(new CustomEvent(ViewerEvents.ArrowDownPress));
    expect(uiData.currentSearches.length).toEqual(1);

    await presenter.onSearchQueryClick(newQuery, 1);
    expect(emitEventSpy).toHaveBeenCalledWith(new TraceRemoveRequest(newTrace));
    expect(emitEventSpy).toHaveBeenCalledWith(new TraceSearchRequest(newQuery));
    expect(uiData.currentSearches.length).toEqual(1);
    expect(uiData.recentSearches).toEqual([
      new ListedSearch(newQuery),
      new ListedSearch(testQuery),
    ]);
  });

  it('handles save query click', () => {
    const testQuery = 'save query';
    const testName = 'save name';
    presenter.onSaveQueryClick(testQuery, testName);
    const testSearch = new ListedSearch(testQuery, testName);
    expect(uiData.savedSearches).toEqual([testSearch]);

    const newQuery = 'new save query';
    const newName = 'new save name';
    presenter.onSaveQueryClick(newQuery, newName);
    const newSearch = new ListedSearch(newQuery, newName);
    expect(uiData.savedSearches).toEqual([newSearch, testSearch]);
  });

  it('handles delete saved query click', () => {
    const testQuery = 'delete query';
    const testName = 'delete name';
    const testSearch = new ListedSearch(testQuery, testName);
    presenter.onDeleteSavedQueryClick(testSearch);
    expect(uiData.savedSearches).toEqual([]);

    presenter.onSaveQueryClick(testQuery, testName);
    expect(uiData.savedSearches).toEqual([testSearch]);

    presenter.onDeleteSavedQueryClick(uiData.savedSearches[0]);
    expect(uiData.savedSearches).toEqual([]);
  });

  it('handles clear query click', async () => {
    const testQuery = 'clear query';
    const trace = UnitTestUtils.makeEmptyTrace(TraceType.SEARCH, [
      testQuery,
      '1',
    ]);
    await runSearchWithNoRowsAndCheckUiData(testQuery, trace);

    await presenter.onClearQueryClick(0);
    expect(uiData.currentSearches.length).toEqual(1);
    await presenter.onClearQueryClick(1);
    expect(uiData.currentSearches.length).toEqual(0);
  });

  it('retains at most 10 recent searches', async () => {
    for (let i = 0; i < 12; i++) {
      const testQuery = 'recent query';
      const trace = UnitTestUtils.makeEmptyTrace(TraceType.SEARCH, [
        testQuery,
        '1',
      ]);
      await presenter.onSearchQueryClick(testQuery, 1);
      await presenter.onAppEvent(new TraceAddRequest(trace));
    }
    expect(uiData.currentSearches.length).toEqual(1);
    expect(uiData.recentSearches.length).toEqual(10);
  });

  function searchEqualityTester(first: any, second: any): boolean | undefined {
    if (first instanceof ListedSearch && second instanceof ListedSearch) {
      return first.query === second.query && first.name === second.name;
    }
    return undefined;
  }

  async function runSearchWithNoRowsAndCheckUiData(
    testQuery: string,
    trace: Trace<QueryResult>,
    uid = 1,
    expectedCurrentSearches = [
      new CurrentSearch(uid, testQuery, new SearchResult([], [])),
    ],
  ) {
    await presenter.onSearchQueryClick(testQuery, uid);
    expect(emitEventSpy).toHaveBeenCalledOnceWith(
      new TraceSearchRequest(testQuery),
    );
    await presenter.onAppEvent(new TraceAddRequest(trace));
    expect(uiData.currentSearches).toEqual(expectedCurrentSearches);
    expect(uiData.lastTraceFailed).toEqual(false);
    expect(uiData.recentSearches[0]).toEqual(new ListedSearch(testQuery));
  }
});
