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
import {PersistentStoreProxy} from 'common/store/persistent_store_proxy';
import {Store} from 'common/store/store';
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
  DeleteSavedQueryClickDetail,
  QueryClickDetail,
  SaveQueryClickDetail,
  ViewerEvents,
} from 'viewers/common/viewer_events';
import {SearchResultPresenter} from './search_result_presenter';
import {Search, SearchResult, UiData} from './ui_data';

class QueryAndTrace {
  constructor(
    readonly query: string,
    public trace: Trace<QueryResult> | undefined,
  ) {}
}

export class Presenter {
  private emitWinscopeEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;
  private uiData = UiData.createEmpty();
  private runQueries: QueryAndTrace[] = [];
  private savedSearches = PersistentStoreProxy.new<{searches: Search[]}>(
    'savedSearches',
    {searches: []},
    this.storage,
  );
  private currentSearchPresenters: SearchResultPresenter[] = [];
  private viewerElement: HTMLElement | undefined;

  constructor(
    private traces: Traces,
    private storage: Store,
    private readonly notifyViewCallback: (uiData: UiData) => void,
  ) {
    this.uiData.savedSearches = Array.from(this.savedSearches.searches);
    this.copyUiDataAndNotifyView();
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
        const detail: QueryClickDetail = (event as CustomEvent).detail;
        this.onSearchQueryClick(detail.query);
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
    for (const p of this.currentSearchPresenters) {
      await p.onAppEvent(event);
    }
  }

  async onGlobalSearchSectionClick() {
    if (!this.uiData.initialized) {
      this.emitWinscopeEvent(new InitializeTraceSearchRequest());
    }
  }

  async onSearchQueryClick(query: string) {
    if (this.runQueries.length > 0) {
      this.clearQuery(this.runQueries[this.runQueries.length - 1].query);
    }
    this.runQueries.push(new QueryAndTrace(query, undefined));
    this.emitWinscopeEvent(new TraceSearchRequest(query));
  }

  private clearQuery(query: string) {
    this.uiData.currentSearches = this.uiData.currentSearches.filter(
      (s) => s.query !== query,
    );
    this.copyUiDataAndNotifyView();
    this.currentSearchPresenters = this.currentSearchPresenters.filter((p) => {
      if (p.query === query) {
        p.onDestroy();
        return false;
      }
      return true;
    });

    const runQueryIndex = this.runQueries.findIndex((r) => r.query === query);
    if (runQueryIndex !== -1) {
      const trace = this.runQueries[runQueryIndex].trace;
      if (trace) {
        this.emitWinscopeEvent(new TraceRemoveRequest(trace));
        this.runQueries.splice(runQueryIndex, 1);
      }
    }
  }

  onSaveQueryClick(query: string, name: string) {
    this.uiData.savedSearches.unshift(new Search(query, name));
    this.savedSearches.searches = this.uiData.savedSearches;
    this.copyUiDataAndNotifyView();
  }

  onDeleteSavedQueryClick(savedSearch: Search) {
    this.uiData.savedSearches = this.uiData.savedSearches.filter(
      (s) => s !== savedSearch,
    );
    this.savedSearches.searches = this.uiData.savedSearches;
    this.copyUiDataAndNotifyView();
  }

  private onTraceSearchFailed() {
    this.uiData.lastTraceFailed = true;
    this.copyUiDataAndNotifyView();
    this.uiData.lastTraceFailed = false;
  }

  private async showQueryResult(newTrace: Trace<QueryResult>) {
    const traceQuery = newTrace.getDescriptors()[0];
    const runQuery = assertDefined(
      this.runQueries.find((q) => q.query === traceQuery),
    );
    runQuery.trace = newTrace;

    if (this.uiData.recentSearches.length >= 10) {
      this.uiData.recentSearches.pop();
    }
    this.uiData.recentSearches.unshift(new Search(runQuery.query));

    this.uiData.currentSearches = [];
    for (const {query, trace} of this.runQueries) {
      if (trace) {
        const firstEntry =
          trace.lengthEntries > 0 ? trace.getEntry(0) : undefined;
        const presenter = new SearchResultPresenter(
          query,
          trace,
          (result: SearchResult) => {
            const currentSearch = this.uiData.currentSearches.find(
              (search) => search.query === result.query,
            );
            if (currentSearch) {
              currentSearch.scrollToIndex = result.scrollToIndex;
              currentSearch.selectedIndex = result.selectedIndex;
            } else {
              this.uiData.currentSearches.push(result);
            }
            this.copyUiDataAndNotifyView();
          },
          firstEntry ? await firstEntry.getValue() : undefined,
        );
        presenter.addEventListeners(assertDefined(this.viewerElement));
        presenter.setEmitEvent(async (event) => this.emitWinscopeEvent(event));
        this.currentSearchPresenters.push(presenter);
        if (firstEntry) {
          await this.emitWinscopeEvent(
            TracePositionUpdate.fromTraceEntry(firstEntry),
          );
        }
      }
    }
    this.copyUiDataAndNotifyView();
  }

  private copyUiDataAndNotifyView() {
    // Create a shallow copy of the data, otherwise the Angular OnPush change detection strategy
    // won't detect the new input
    const copy = Object.assign({}, this.uiData);
    this.notifyViewCallback(copy);
  }
}
