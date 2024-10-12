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
import {TextFilter} from './text_filter';
import {UiPropertyTreeNode} from './ui_property_tree_node';

export interface UiDataLog {
  entries: LogEntry[];
  selectedIndex: undefined | number;
  scrollToIndex: undefined | number;

  headers: Array<LogFieldType | LogFilter>;
  currentIndex?: undefined | number;
  propertiesTree?: undefined | UiPropertyTreeNode;
  propertiesUserOptions?: UserOptions;
  propertiesFilter?: TextFilter;
  isDarkMode?: boolean;
}

export class LogFilter {
  constructor(
    public type: LogFieldType,
    public options?: string[],
    public textFilter?: TextFilter,
  ) {}
}

export interface LogEntry {
  traceEntry: TraceEntry<PropertyTreeNode>;
  fields: LogField[];
  propertiesTree?: undefined | PropertyTreeNode;
}

export interface LogField {
  type: LogFieldType;
  value: LogFieldValue;
  icon?: string;
  iconColor?: string;
}

export type LogFieldValue = string | number | Timestamp;

export enum LogFieldType {
  TRANSACTION_ID,
  VSYNC_ID,
  PID,
  UID,
  TRANSACTION_TYPE,
  LAYER_OR_DISPLAY_ID,
  FLAGS,
  LOG_LEVEL,
  TAG,
  SOURCE_FILE,
  TEXT,
  TRANSITION_ID,
  TRANSITION_TYPE,
  SEND_TIME,
  DISPATCH_TIME,
  DURATION,
  HANDLER,
  PARTICIPANTS,
  STATUS,
  CUJ_TYPE,
  START_TIME,
  END_TIME,
  INPUT_TYPE,
  INPUT_SOURCE,
  INPUT_ACTION,
  INPUT_DEVICE_ID,
  INPUT_DISPLAY_ID,
  INPUT_EVENT_DETAILS,
  INPUT_DISPATCH_WINDOWS,
}

export const LogFieldNames: ReadonlyMap<LogFieldType, string> = new Map([
  [LogFieldType.TRANSACTION_ID, 'TX ID'],
  [LogFieldType.VSYNC_ID, 'VSYNC ID'],
  [LogFieldType.PID, 'PID'],
  [LogFieldType.UID, 'UID'],
  [LogFieldType.TRANSACTION_TYPE, 'TYPE'],
  [LogFieldType.LAYER_OR_DISPLAY_ID, 'LAYER/DISP ID'],
  [LogFieldType.FLAGS, 'Flags'],
  [LogFieldType.LOG_LEVEL, 'Log level'],
  [LogFieldType.TAG, 'Tag'],
  [LogFieldType.SOURCE_FILE, 'Source files'],
  [LogFieldType.TEXT, 'Search text'],
  [LogFieldType.TRANSITION_ID, 'Id'],
  [LogFieldType.TRANSITION_TYPE, 'Type'],
  [LogFieldType.SEND_TIME, 'Send Time'],
  [LogFieldType.DISPATCH_TIME, 'Dispatch Time'],
  [LogFieldType.DURATION, 'Duration'],
  [LogFieldType.HANDLER, 'Handler'],
  [LogFieldType.PARTICIPANTS, 'Participants'],
  [LogFieldType.STATUS, 'Status'],
  [LogFieldType.CUJ_TYPE, 'Type'],
  [LogFieldType.START_TIME, 'Start Time'],
  [LogFieldType.END_TIME, 'End Time'],
  [LogFieldType.INPUT_TYPE, 'Type'],
  [LogFieldType.INPUT_SOURCE, 'Source'],
  [LogFieldType.INPUT_ACTION, 'Action'],
  [LogFieldType.INPUT_DEVICE_ID, 'Device'],
  [LogFieldType.INPUT_DISPLAY_ID, 'Display'],
  [LogFieldType.INPUT_EVENT_DETAILS, 'Details'],
  [LogFieldType.INPUT_DISPATCH_WINDOWS, 'Target Windows'],
]);

export const LogFieldClassNames: ReadonlyMap<LogFieldType, string> = new Map([
  [LogFieldType.TRANSACTION_ID, 'transaction-id right-align'],
  [LogFieldType.VSYNC_ID, 'vsyncid right-align'],
  [LogFieldType.PID, 'pid right-align'],
  [LogFieldType.UID, 'uid right-align'],
  [LogFieldType.TRANSACTION_TYPE, 'transaction-type'],
  [LogFieldType.LAYER_OR_DISPLAY_ID, 'layer-or-display-id right-align'],
  [LogFieldType.FLAGS, 'flags'],
  [LogFieldType.LOG_LEVEL, 'log-level'],
  [LogFieldType.TAG, 'tag'],
  [LogFieldType.SOURCE_FILE, 'source-file'],
  [LogFieldType.TEXT, 'text'],
  [LogFieldType.TRANSITION_ID, 'transition-id right-align'],
  [LogFieldType.TRANSITION_TYPE, 'transition-type'],
  [LogFieldType.CUJ_TYPE, 'jank-cuj-type'],
  [LogFieldType.SEND_TIME, 'send-time time'],
  [LogFieldType.DISPATCH_TIME, 'dispatch-time time'],
  [LogFieldType.START_TIME, 'start-time time'],
  [LogFieldType.END_TIME, 'end-time time'],
  [LogFieldType.DURATION, 'duration right-align'],
  [LogFieldType.HANDLER, 'handler'],
  [LogFieldType.PARTICIPANTS, 'participants'],
  [LogFieldType.STATUS, 'status right-align'],
  [LogFieldType.INPUT_TYPE, 'input-type inline'],
  [LogFieldType.INPUT_SOURCE, 'input-source'],
  [LogFieldType.INPUT_ACTION, 'input-action'],
  [LogFieldType.INPUT_DEVICE_ID, 'input-device-id right-align'],
  [LogFieldType.INPUT_DISPLAY_ID, 'input-display-id right-align'],
  [LogFieldType.INPUT_EVENT_DETAILS, 'input-details'],
  [LogFieldType.INPUT_DISPATCH_WINDOWS, 'input-windows'],
]);
