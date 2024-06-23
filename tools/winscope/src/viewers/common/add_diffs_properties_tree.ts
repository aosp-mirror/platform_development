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

import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {AddDiffs} from './add_diffs';
import {DiffType} from './diff_type';

export class AddDiffsPropertiesTree extends AddDiffs<UiPropertyTreeNode> {
  protected override addDiffsToNewRoot = false;

  protected override processOldNode(oldNode: UiPropertyTreeNode): void {
    //do nothing
  }

  protected override processModifiedNodes(
    newNode: UiPropertyTreeNode,
    oldNode: UiPropertyTreeNode,
  ): void {
    newNode.setDiff(DiffType.MODIFIED);
    newNode.setOldValue(
      oldNode.formattedValue() !== '' ? oldNode.formattedValue() : 'null',
    );
  }
}
