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
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TextFilter} from './text_filter';
import {LogFieldType} from './ui_data_log';

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
}

export class RectDblClickDetail {
  constructor(public clickedRectId: string) {}
}

export class TimestampClickDetail {
  constructor(
    public entry?: TraceEntry<PropertyTreeNode>,
    public timestamp?: Timestamp,
  ) {}
}

export class LogFilterChangeDetail {
  constructor(public type: LogFieldType, public value: string[] | string) {}
}

export class LogTextFilterChangeDetail {
  constructor(public type: LogFieldType, public filter: TextFilter) {}
}
