/*
 * Copyright (C) 2025 The Android Open Source Project
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

export abstract class AbstractPropertyTreeBuilder<T> {
  protected data: T | undefined;
  private rootId: string | number = 'UnknownRootId';
  protected rootName: string | undefined = 'UnknownRootName';
  private duplicateCount = 0;

  setData(value: T | undefined): this {
    this.data = value;
    return this;
  }

  setRootId(value: string | number): this {
    this.rootId = value;
    return this;
  }

  setRootName(value: string): this {
    this.rootName = value;
    return this;
  }

  setDuplicateCount(value: number): this {
    this.duplicateCount = value;
    return this;
  }

  build(): PropertyTreeNode {
    if (this.data === undefined) {
      throw new Error('data not set');
    }
    if (this.rootId === undefined) {
      throw new Error('rootId not set');
    }
    if (this.rootName === undefined) {
      throw new Error('rootName not set');
    }
    const rootId = this.makeNodeId();
    return this.buildPropertiesTree(rootId);
  }

  private makeNodeId(): string {
    let nodeId = `${this.rootId} ${this.rootName}`;
    if (this.duplicateCount > 0) {
      nodeId += ` duplicate(${this.duplicateCount})`;
    }
    return nodeId;
  }

  protected abstract buildPropertiesTree(rootNodeId: string): PropertyTreeNode;
}
