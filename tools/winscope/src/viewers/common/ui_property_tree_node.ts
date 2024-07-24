/*
 * Copyright 2024, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {assertDefined} from 'common/assert_utils';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {DiffNode} from './diff_node';
import {DiffType} from './diff_type';

export class UiPropertyTreeNode extends PropertyTreeNode implements DiffNode {
  private diff: DiffType = DiffType.NONE;
  private displayName: string = this.name;
  private oldValue = 'null';

  static from(node: PropertyTreeNode): UiPropertyTreeNode {
    const displayNode = new UiPropertyTreeNode(
      node.id,
      node.name,
      node.source,
      (node as UiPropertyTreeNode).value,
    );
    if ((node as UiPropertyTreeNode).formatter) {
      displayNode.setFormatter(
        assertDefined((node as UiPropertyTreeNode).formatter),
      );
    }

    displayNode.setIsRoot(node.isRoot());

    const children = [...node.getAllChildren()].sort((a, b) =>
      a.name < b.name ? -1 : 1,
    );

    children.forEach((child) => {
      displayNode.addOrReplaceChild(UiPropertyTreeNode.from(child));
    });
    return displayNode;
  }

  setDiff(diff: DiffType): void {
    this.diff = diff;
  }

  getDiff(): DiffType {
    return this.diff;
  }

  setDisplayName(name: string) {
    this.displayName = name;
  }

  getDisplayName(): string {
    return this.displayName;
  }

  setOldValue(value: string) {
    this.oldValue = value;
  }

  getOldValue(): string {
    return this.oldValue;
  }
}
