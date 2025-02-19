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

import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {AbsoluteEntryIndex} from './index_types';

export interface Transition {
  id: number;
  type: string;
  sendTime?: PropertyTreeNode;
  dispatchTime?: PropertyTreeNode;
  duration?: string;
  merged: boolean;
  aborted: boolean;
  played: boolean;
  propertiesTree: PropertyTreeNode;
  traceIndex: AbsoluteEntryIndex;
}
