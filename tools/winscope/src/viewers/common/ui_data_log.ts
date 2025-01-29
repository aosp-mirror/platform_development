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

import {Timestamp} from 'common/time/time';
import {TraceEntry} from 'trace/trace';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TextFilter} from 'viewers/common/text_filter';
import {UserOptions} from 'viewers/common/user_options';
import {LogFilter} from './log_filters';
import {UiPropertyTreeNode} from './ui_property_tree_node';

export interface UiDataLog {
  entries: LogEntry[];
  selectedIndex: undefined | number;
  scrollToIndex: undefined | number;
  currentIndex: undefined | number;
  isFetchingData: boolean;

  headers: LogHeader[];
  propertiesTree?: undefined | UiPropertyTreeNode;
  propertiesUserOptions?: UserOptions;
  propertiesFilter?: TextFilter;
  isDarkMode?: boolean;
}

export interface ColumnSpec {
  name: string;
  cssClass: string;
}

export class LogHeader {
  constructor(public spec: ColumnSpec, public filter?: LogFilter) {}
}

export interface LogEntry {
  traceEntry: TraceEntry<object>;
  fields: LogField[];
  propertiesTree?: undefined | PropertyTreeNode;
}

export interface LogField {
  spec: ColumnSpec;
  value: LogFieldValue;
  icon?: string;
  iconColor?: string;
  propagateEntryTimestamp?: boolean;
}

export type LogFieldValue = string | number | Timestamp;
