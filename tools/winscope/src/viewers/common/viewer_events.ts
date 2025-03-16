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

import {Timestamp} from 'common/time';
import {TraceEntry} from 'trace/trace';
import {TextFilter} from 'viewers/common/text_filter';
import {Search} from 'viewers/viewer_search/ui_data';
import {LogHeader} from './ui_data_log';

export enum ViewerEvents {
  HighlightedNodeChange = 'HighlightedNodeChange',
  HighlightedIdChange = 'HighlightedIdChange',

  HierarchyPinnedChange = 'HierarchyPinnedChange',
  HierarchyUserOptionsChange = 'HierarchyUserOptionsChange',
  HierarchyFilterChange = 'HierarchyFilterChange',
  RectShowStateChange = 'RectShowStateChange',

  PropertiesUserOptionsChange = 'PropertiesUserOptionsChange',
  PropertiesFilterChange = 'PropertiesFilterChange',
  DispatchPropertiesFilterChange = 'DispatchPropertiesFilterChange',
  HighlightedPropertyChange = 'HighlightedPropertyChange',

  RectsUserOptionsChange = 'RectsUserOptionsChange',

  AdditionalPropertySelected = 'AdditionalPropertySelected',
  RectsDblClick = 'RectsDblClick',
  MiniRectsDblClick = 'MiniRectsDblClick',

  TimestampClick = 'TimestampClick',
  LogEntryClick = 'LogEntryClick',
  LogFilterChange = 'LogFilterChange',
  LogTextFilterChange = 'LogTextFilterChange',
  ArrowDownPress = 'ArrowDownPress',
  ArrowUpPress = 'ArrowUpPress',

  GlobalSearchSectionClick = 'GlobalSearchSectionClick',
  SearchQueryClick = 'SearchQueryClick',
  SaveQueryClick = 'SaveQueryClick',
  DeleteSavedQueryClick = 'DeleteSavedQueryClick',
}

export class RectDblClickDetail {
  constructor(public clickedRectId: string) {}
}

export class TimestampClickDetail {
  constructor(
    public entry?: TraceEntry<object>,
    public timestamp?: Timestamp,
  ) {}
}

export class LogFilterChangeDetail {
  constructor(public header: LogHeader, public value: string[]) {}
}

export class LogTextFilterChangeDetail {
  constructor(public header: LogHeader, public filter: TextFilter) {}
}

export class QueryClickDetail {
  constructor(public query: string) {}
}

export class SaveQueryClickDetail {
  constructor(public query: string, public name: string) {}
}

export class DeleteSavedQueryClickDetail {
  constructor(public search: Search) {}
}
