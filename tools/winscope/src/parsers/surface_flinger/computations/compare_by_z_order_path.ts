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

import {assertDefined} from 'common/assert_utils';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

export function compareByZOrderPath(
  a: HierarchyTreeNode,
  b: HierarchyTreeNode,
): number {
  const aZOrderPath: number[] = assertDefined(
    a.getEagerPropertyByName('zOrderPath'),
  )
    .getAllChildren()
    .map((child) => child.getValue());
  const bZOrderPath: number[] = assertDefined(
    b.getEagerPropertyByName('zOrderPath'),
  )
    .getAllChildren()
    .map((child) => child.getValue());

  const zipLength = Math.min(aZOrderPath.length, bZOrderPath.length);
  for (let i = 0; i < zipLength; ++i) {
    const zOrderA = aZOrderPath[i];
    const zOrderB = bZOrderPath[i];
    if (zOrderA > zOrderB) return -1;
    if (zOrderA < zOrderB) return 1;
  }
  // When z-order is the same, the layer with larger ID is on top
  return a.id > b.id ? -1 : 1;
}
