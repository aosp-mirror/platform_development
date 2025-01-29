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

import {LogEntry, LogHeader} from 'viewers/common/ui_data_log';

export class UiData {
  currentSearches: CurrentSearch[] = [];
  savedSearches: ListedSearch[] = [];
  recentSearches: ListedSearch[] = [];
  lastTraceFailed = false;
  initialized = false;
  searchViews: string[] = [];

  static createEmpty() {
    return new UiData();
  }
}

export class CurrentSearch {
  constructor(
    readonly uid: number,
    public query?: string,
    public result?: SearchResult,
  ) {}
}

export class SearchResult {
  selectedIndex: undefined | number;
  scrollToIndex: undefined | number;
  currentIndex: undefined | number;
  isFetchingData = false;

  constructor(readonly headers: LogHeader[], readonly entries: LogEntry[]) {}
}

export class ListedSearch {
  readonly timeMs: number;
  constructor(readonly query: string, readonly name = query) {
    this.timeMs = Date.now();
  }
}
