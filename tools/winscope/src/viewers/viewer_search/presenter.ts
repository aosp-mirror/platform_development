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
import {FunctionUtils} from 'common/function_utils';
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {Store} from 'common/store';
import {
  InitializeTraceSearchRequest,
  TracePositionUpdate,
  TraceRemoveRequest,
  TraceSearchRequest,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {EmitEvent} from 'messaging/winscope_event_emitter';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {QueryResult} from 'trace_processor/query_result';
import {
  AddQueryClickDetail,
  ClearQueryClickDetail,
  DeleteSavedQueryClickDetail,
  SaveQueryClickDetail,
  SearchQueryClickDetail,
  ViewerEvents,
} from 'viewers/common/viewer_events';
import {SearchResultPresenter} from './search_result_presenter';
import {CurrentSearch, ListedSearch, SearchResult, UiData} from './ui_data';

interface ActiveSearch {
  search: CurrentSearch;
  trace?: Trace<QueryResult>;
  resultPresenter?: SearchResultPresenter;
}

export class Presenter {
  private emitWinscopeEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;
  private uiData = UiData.createEmpty();
  private activeSearchUid = 0;
  private activeSearches: ActiveSearch[] = [];
  private savedSearches = PersistentStoreProxy.new<{searches: ListedSearch[]}>(
    'savedSearches',
    {searches: []},
    this.storage,
  );
  private viewerElement: HTMLElement | undefined;
  private runningSearch: CurrentSearch | undefined;

  constructor(
    private traces: Traces,
    private storage: Store,
    private readonly notifyViewCallback: (uiData: UiData) => void,
  ) {
    this.uiData.savedSearches = Array.from(this.savedSearches.searches);
    this.addSearch();
  }

  setEmitEvent(callback: EmitEvent) {
    this.emitWinscopeEvent = callback;
  }

  addEventListeners(htmlElement: HTMLElement) {
    this.viewerElement = htmlElement;
    htmlElement.addEventListener(
      ViewerEvents.GlobalSearchSectionClick,
      async (event) => {
        this.onGlobalSearchSectionClick();
      },
    );
    htmlElement.addEventListener(
      ViewerEvents.SearchQueryClick,
      async (event) => {
        const detail: SearchQueryClickDetail = (event as CustomEvent).detail;
        this.onSearchQueryClick(detail.query, detail.uid);
      },
    );
    htmlElement.addEventListener(ViewerEvents.SaveQueryClick, async (event) => {
      const detail: SaveQueryClickDetail = (event as CustomEvent).detail;
      this.onSaveQueryClick(detail.query, detail.name);
    });
    htmlElement.addEventListener(
      ViewerEvents.DeleteSavedQueryClick,
      async (event) => {
        const detail: DeleteSavedQueryClickDetail = (event as CustomEvent)
          .detail;
        this.onDeleteSavedQueryClick(detail.search);
      },
    );
    htmlElement.addEventListener(ViewerEvents.AddQueryClick, async (event) => {
      const detail: AddQueryClickDetail | undefined = (event as CustomEvent)
        .detail;
      this.addSearch(detail?.query);
    });
    htmlElement.addEventListener(
      ViewerEvents.ClearQueryClick,
      async (event) => {
        const detail: ClearQueryClickDetail = (event as CustomEvent).detail;
        this.onClearQueryClick(detail.uid);
      },
    );
  }

  async onAppEvent(event: WinscopeEvent) {
    await event.visit(
      WinscopeEventType.TRACE_SEARCH_INITIALIZED,
      async (event) => {
        this.uiData.searchViews = event.views;
        this.uiData.initialized = true;
        this.copyUiDataAndNotifyView();
      },
    );
    await event.visit(WinscopeEventType.TRACE_ADD_REQUEST, async (event) => {
      if (event.trace.type === TraceType.SEARCH) {
        this.showQueryResult(event.trace as Trace<QueryResult>);
      }
    });
    await event.visit(WinscopeEventType.TRACE_SEARCH_FAILED, async (event) => {
      this.onTraceSearchFailed();
    });
    for (const activeSearch of this.activeSearches.values()) {
      await activeSearch.resultPresenter?.onAppEvent(event);
    }
  }

  async onGlobalSearchSectionClick() {
    if (!this.uiData.initialized) {
      this.emitWinscopeEvent(new InitializeTraceSearchRequest());
    }
  }

  async onSearchQueryClick(query: string, uid: number) {
    const activeSearch = assertDefined(
      this.activeSearches.find((a) => a.search.uid === uid),
    );
    this.resetActiveSearch(activeSearch, query);
    this.runningSearch = activeSearch.search;
    this.emitWinscopeEvent(new TraceSearchRequest(query));
  }

  addSearch(query?: string) {
    this.activeSearchUid++;
    this.activeSearches.push({
      search: new CurrentSearch(this.activeSearchUid, query),
    });
    this.updateCurrentSearches();
  }

  async onClearQueryClick(uid: number) {
    const activeSearchIndex = this.activeSearches.findIndex(
      (a) => a.search.uid === uid,
    );
    if (activeSearchIndex === -1) {
      return;
    }
    const activeSearch = this.activeSearches.splice(activeSearchIndex, 1)[0];
    this.resetActiveSearch(activeSearch);
    this.updateCurrentSearches();
  }

  onSaveQueryClick(query: string, name: string) {
    this.uiData.savedSearches.unshift(new ListedSearch(query, name));
    this.savedSearches.searches = this.uiData.savedSearches;
    this.copyUiDataAndNotifyView();
  }

  onDeleteSavedQueryClick(savedSearch: ListedSearch) {
    this.uiData.savedSearches = this.uiData.savedSearches.filter(
      (s) => s !== savedSearch,
    );
    this.savedSearches.searches = this.uiData.savedSearches;
    this.copyUiDataAndNotifyView();
  }

  private onTraceSearchFailed() {
    this.runningSearch = undefined;
    this.uiData.lastTraceFailed = true;
    this.copyUiDataAndNotifyView();
    this.uiData.lastTraceFailed = false;
  }

  private async showQueryResult(newTrace: Trace<QueryResult>) {
    const [traceQuery] = newTrace.getDescriptors();
    if (this.uiData.recentSearches.length >= 10) {
      this.uiData.recentSearches.pop();
    }
    this.uiData.recentSearches.unshift(new ListedSearch(traceQuery));

    const activeSearch = assertDefined(
      this.activeSearches.find((a) => a.search.uid === this.runningSearch?.uid),
    );
    this.resetActiveSearch(activeSearch, traceQuery);
    this.initializeResultPresenter(activeSearch, newTrace);
    this.runningSearch = undefined;
    this.copyUiDataAndNotifyView();
  }

  private updateCurrentSearches() {
    this.uiData.currentSearches = this.activeSearches.map((a) => a.search);
    this.copyUiDataAndNotifyView();
  }

  private resetActiveSearch(activeSearch: ActiveSearch, newQuery?: string) {
    activeSearch.search.query = newQuery;
    activeSearch.search.result = undefined;
    if (activeSearch.resultPresenter) {
      activeSearch.resultPresenter.onDestroy();
      activeSearch.resultPresenter = undefined;
    }
    if (activeSearch.trace) {
      this.emitWinscopeEvent(new TraceRemoveRequest(activeSearch.trace));
      activeSearch.trace = undefined;
    }
  }

  private async initializeResultPresenter(
    activeSearch: ActiveSearch,
    newTrace: Trace<QueryResult>,
  ) {
    activeSearch.trace = newTrace;
    const firstEntry =
      newTrace.lengthEntries > 0 ? newTrace.getEntry(0) : undefined;

    const presenter = new SearchResultPresenter(
      newTrace,
      (result: SearchResult) => {
        if (activeSearch.search.result) {
          activeSearch.search.result.scrollToIndex = result.scrollToIndex;
          activeSearch.search.result.selectedIndex = result.selectedIndex;
        } else {
          activeSearch.search.result = result;
        }
        this.updateCurrentSearches();
      },
      firstEntry ? await firstEntry.getValue() : undefined,
    );
    presenter.addEventListeners(assertDefined(this.viewerElement));
    presenter.setEmitEvent(async (event) => this.emitWinscopeEvent(event));
    activeSearch.resultPresenter = presenter;

    if (firstEntry) {
      await this.emitWinscopeEvent(
        TracePositionUpdate.fromTraceEntry(firstEntry),
      );
    }
  }

  private copyUiDataAndNotifyView() {
    // Create a shallow copy of the data, otherwise the Angular OnPush change detection strategy
    // won't detect the new input
    const copy = Object.assign({}, this.uiData);
    this.notifyViewCallback(copy);
  }
}
