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
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {DisplayIdentifier} from 'viewers/common/display_identifier';
import {RectShowState} from 'viewers/common/rect_show_state';
import {
  LogEntry,
  LogField,
  LogFieldType,
  LogFilter,
  UiDataLog,
} from 'viewers/common/ui_data_log';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {UserOptions} from 'viewers/common/user_options';
import {UiRect} from 'viewers/components/rects/types2d';

export class UiData implements UiDataLog {
  constructor(
    public headers: LogFieldType[],
    public entries: LogEntry[],
    public filters: LogFilter[],
    public selectedIndex: undefined | number,
    public scrollToIndex: undefined | number,
    public currentIndex: undefined | number,
    public propertiesTree: undefined | UiPropertyTreeNode,
  ) {}

  highlightedProperty: string = '';
  dispatchPropertiesTree: UiPropertyTreeNode | undefined;

  rectsToDraw: UiRect[] | undefined;
  rectIdToShowState: Map<string, RectShowState> | undefined;
  highlightedRect = '';
  rectsUserOptions: UserOptions | undefined;
  displays: DisplayIdentifier[] = [];

  readonly dependencies: TraceType[] = [TraceType.INPUT_EVENT_MERGED];

  static createEmpty(): UiData {
    return new UiData([], [], [], undefined, undefined, undefined, undefined);
  }
}

export class InputEntry implements LogEntry {
  constructor(
    public traceEntry: TraceEntry<PropertyTreeNode>,
    public fields: LogField[],
    public propertiesTree: PropertyTreeNode | undefined,
    public dispatchPropertiesTree: PropertyTreeNode | undefined,
    public surfaceFlingerEntry: TraceEntry<HierarchyTreeNode> | undefined,
  ) {}
}
