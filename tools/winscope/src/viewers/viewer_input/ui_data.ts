/*
 * Copyright 2024 The Android Open Source Project
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

import {TraceEntry} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {
  LogEntry,
  LogField,
  LogFieldType,
  UiDataLog,
} from 'viewers/common/ui_data_log';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';

export class UiData implements UiDataLog {
  constructor(
    public headers: LogFieldType[],
    public entries: LogEntry[],
    public selectedIndex: undefined | number,
    public scrollToIndex: undefined | number,
    public propertiesTree: undefined | UiPropertyTreeNode,
  ) {}

  selectedTraceType: TraceType | undefined;
  highlightedProperty: string = '';
  dispatchPropertiesTree: UiPropertyTreeNode | undefined;

  static readonly EMPTY = new UiData(
    [],
    [],
    undefined,
    undefined,
    undefined,
  );
}

export class InputEntry implements LogEntry {
  constructor(
    public traceEntry: TraceEntry<PropertyTreeNode>,
    public fields: LogField[],
    public propertiesTree: PropertyTreeNode | undefined,
    public dispatchPropertiesTree: PropertyTreeNode | undefined,
  ) {}
}
