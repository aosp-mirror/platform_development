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

import {Item} from 'trace/item';

export abstract class TreeNode implements Item {
  protected children: this[] = [];

  constructor(readonly id: string, readonly name: string) {}

  addOrReplaceChild(newChild: this): void {
    const currIndex = this.children.findIndex(
      (child) => child.id === newChild.id,
    );
    if (currIndex !== -1) {
      this.children[currIndex] = newChild;
    } else {
      this.children.push(newChild);
    }
  }

  removeChild(childId: string) {
    this.children = this.children.filter((child) => child.id !== childId);
  }

  removeAllChildren() {
    this.children = [];
  }

  getChildByName(name: string): this | undefined {
    return this.children.find((child) => child.name === name);
  }

  getAllChildren(): readonly this[] {
    return this.children;
  }

  forEachNodeDfs(callback: (node: this) => void, reverseChildren = false) {
    callback(this);

    if (reverseChildren) {
      for (let i = this.children.length - 1; i > -1; i--) {
        this.children[i].forEachNodeDfs(callback, reverseChildren);
      }
    } else {
      this.children.forEach((child) =>
        child.forEachNodeDfs(callback, reverseChildren),
      );
    }
  }

  findDfs(targetNodeFilter: (node: this) => boolean): this | undefined {
    if (targetNodeFilter(this)) {
      return this;
    }

    for (const child of this.children.values()) {
      const nodeFound = child.findDfs(targetNodeFilter);
      if (nodeFound) return nodeFound;
    }

    return undefined;
  }

  filterDfs(
    predicate: (node: this) => boolean,
    reverseChildren = false,
  ): this[] {
    const result: this[] = [];

    this.forEachNodeDfs((node) => {
      if (predicate(node)) {
        result.push(node);
      }
    }, reverseChildren);

    return result;
  }

  abstract isRoot(): boolean;
}
