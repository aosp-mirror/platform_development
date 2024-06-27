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

import {Timestamp} from 'common/time';
import {TraceEntry} from 'trace/trace';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {UserOptions} from 'viewers/common/user_options';
import {UiPropertyTreeNode} from './ui_property_tree_node';

export interface UiDataLog {
  entries: LogEntry[];
  selectedIndex: undefined | number;
  scrollToIndex: undefined | number;

  filters?: LogFilter[];
  headers?: LogFieldName[];
  currentIndex?: undefined | number;
  propertiesTree?: undefined | UiPropertyTreeNode;
  propertiesUserOptions?: UserOptions;
}

export interface LogFilter {
  name: LogFieldName;
  options?: string[];
}

export interface LogEntry {
  traceEntry: TraceEntry<PropertyTreeNode>;
  fields: LogField[];
  propertiesTree?: undefined | PropertyTreeNode;
}

export interface LogField {
  name: LogFieldName;
  value: LogFieldValue;
  icon?: string;
  iconColor?: string;
}

export type LogFieldValue = string | number | Timestamp;

export enum LogFieldName {
  TRANSACTION_ID = 'TX ID',
  VSYNC_ID = 'VSYNC ID',
  PID = 'PID',
  UID = 'UID',
  TRANSACTION_TYPE = 'TYPE',
  LAYER_OR_DISPLAY_ID = 'LAYER/DISP ID',
  FLAGS = 'Flags',
  LOG_LEVEL = 'Log level',
  TAG = 'Tag',
  SOURCE_FILE = 'Source files',
  TEXT = 'Search text',
  TRANSITION_ID = 'Id',
  TRANSITION_TYPE = 'Type',
  SEND_TIME = 'Send Time',
  DISPATCH_TIME = 'Dispatch Time',
  DURATION = 'Duration',
  STATUS = 'Status',
  CUJ_TYPE = 'Type',
  START_TIME = 'Start Time',
  END_TIME = 'End Time',
}

export const LogFieldClassNames: ReadonlyMap<LogFieldName, string> = new Map([
  [LogFieldName.TRANSACTION_ID, 'transaction-id'],
  [LogFieldName.VSYNC_ID, 'vsyncid'],
  [LogFieldName.PID, 'pid'],
  [LogFieldName.UID, 'uid'],
  [LogFieldName.TRANSACTION_TYPE, 'transaction-type'],
  [LogFieldName.LAYER_OR_DISPLAY_ID, 'layer-or-display-id'],
  [LogFieldName.FLAGS, 'flags'],
  [LogFieldName.LOG_LEVEL, 'log-level'],
  [LogFieldName.TAG, 'tag'],
  [LogFieldName.SOURCE_FILE, 'source-file'],
  [LogFieldName.TEXT, 'text'],
  [LogFieldName.TRANSITION_ID, 'transition-id'],
  [LogFieldName.TRANSITION_TYPE, 'transition-type'],
  [LogFieldName.CUJ_TYPE, 'jank_cuj-type'],
  [LogFieldName.SEND_TIME, 'send-time time'],
  [LogFieldName.DISPATCH_TIME, 'dispatch-time time'],
  [LogFieldName.START_TIME, 'start-time time'],
  [LogFieldName.END_TIME, 'end-time time'],
  [LogFieldName.DURATION, 'duration'],
  [LogFieldName.STATUS, 'status'],
]);
