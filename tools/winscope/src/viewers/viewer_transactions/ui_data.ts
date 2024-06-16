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

import {AbsoluteEntryIndex} from 'trace/index_types';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {UserOptions} from 'viewers/common/user_options';

class UiData {
  constructor(
    public allVSyncIds: string[],
    public allPids: string[],
    public allUids: string[],
    public allTypes: string[],
    public allLayerAndDisplayIds: string[],
    public allTransactionIds: string[],
    public allFlags: string[],
    public entries: UiDataEntry[],
    public currentEntryIndex: undefined | number,
    public selectedEntryIndex: undefined | number,
    public scrollToIndex: undefined | number,
    public currentPropertiesTree: undefined | UiPropertyTreeNode,
    public propertiesUserOptions: UserOptions,
  ) {}

  static EMPTY = new UiData(
    [],
    [],
    [],
    [],
    [],
    [],
    [],
    [],
    undefined,
    undefined,
    undefined,
    undefined,
    {},
  );
}

class UiDataEntry {
  constructor(
    public traceIndex: AbsoluteEntryIndex,
    public time: PropertyTreeNode,
    public vsyncId: number,
    public pid: string,
    public uid: string,
    public type: string,
    public layerOrDisplayId: string,
    public transactionId: string,
    public what: string,
    public propertiesTree: PropertyTreeNode | undefined,
  ) {}
}

class UiDataEntryType {
  static DISPLAY_ADDED = 'DISPLAY_ADDED';
  static DISPLAY_REMOVED = 'DISPLAY_REMOVED';
  static DISPLAY_CHANGED = 'DISPLAY_CHANGED';
  static LAYER_ADDED = 'LAYER_ADDED';
  static LAYER_DESTROYED = 'LAYER_DESTROYED';
  static LAYER_CHANGED = 'LAYER_CHANGED';
  static LAYER_HANDLE_DESTROYED = 'LAYER_HANDLE_DESTROYED';
  static NO_OP = 'NO_OP';
}

export {UiData, UiDataEntry, UiDataEntryType};
