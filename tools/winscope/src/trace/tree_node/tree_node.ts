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

  constructor(public id: string, public name: string) {}

  addChild(newChild: this): void {
    const currIndex = this.children.findIndex((child) => child.id === newChild.id);
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

  getChildById(id: string): this | undefined {
    return this.children.find((child) => child.id === id);
  }

  getAllChildren(): this[] {
    return this.children;
  }

  forEachNodeDfs(callback: (node: this) => void) {
    callback(this);
    this.children.forEach((child) => {
      child.forEachNodeDfs(callback);
    });
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

  abstract isRoot(): boolean;
}
