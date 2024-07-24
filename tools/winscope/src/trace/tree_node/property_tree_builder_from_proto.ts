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
import {PropertyTreeNodeFactory} from './property_tree_node_factory';

export class PropertyTreeBuilderFromProto {
  private denylistProperties: string[] = [];
  private duplicateCount = 0;
  private proto: any | undefined;
  private rootId: string | number = 'UnknownRootId';
  private rootName: string | undefined = 'UnknownRootName';
  private visitProtoType = true;

  setData(value: any): this {
    this.proto = value;
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

  setDenyList(value: string[]): this {
    this.denylistProperties = value;
    return this;
  }

  setDuplicateCount(value: number): this {
    this.duplicateCount = value;
    return this;
  }

  setVisitPrototype(value: boolean): this {
    this.visitProtoType = value;
    return this;
  }

  build(): PropertyTreeNode {
    if (this.proto === undefined) {
      throw Error('proto not set');
    }
    if (this.rootId === undefined) {
      throw Error('rootId not set');
    }
    if (this.rootName === undefined) {
      throw Error('rootName not set');
    }
    const factory = new PropertyTreeNodeFactory(
      this.denylistProperties,
      this.visitProtoType,
    );

    return factory.makeProtoProperty(this.makeNodeId(), '', this.proto);
  }

  private makeNodeId(): string {
    let nodeId = `${this.rootId} ${this.rootName}`;
    if (this.duplicateCount > 0) {
      nodeId += ` duplicate(${this.duplicateCount})`;
    }
    return nodeId;
  }
}
