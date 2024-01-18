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

import {Transform} from 'parsers/surface_flinger/transform_utils';
import {Operation} from 'trace/tree_node/operations/operation';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {PropertyTreeNodeFactory} from 'trace/tree_node/property_tree_node_factory';

export class UpdateTransforms implements Operation<PropertyTreeNode> {
  apply(value: PropertyTreeNode): PropertyTreeNode {
    const factory = new PropertyTreeNodeFactory();

    this.updateTransform(
      value.getChildById(`${value.id}.transform`),
      value.getChildById(`${value.id}.position`),
      factory
    );

    this.updateTransform(
      value.getChildById(`${value.id}.requestedTransform`),
      value.getChildById(`${value.id}.requestedPosition`),
      factory
    );

    this.updateTransform(value.getChildById(`${value.id}.bufferTransform`), undefined, factory);

    const inputWindowInfo = value.getChildById(`${value.id}.inputWindowInfo`);
    if (inputWindowInfo) {
      this.updateTransform(
        inputWindowInfo.getChildById(`${inputWindowInfo.id}.transform`),
        undefined,
        factory
      );
    }
    return value;
  }

  private updateTransform(
    transformNode: PropertyTreeNode | undefined,
    positionNode: PropertyTreeNode | undefined,
    factory: PropertyTreeNodeFactory
  ) {
    if (!transformNode) return;
    if (transformNode.getChildByName('matrix')) return;

    const newMatrix = Transform.from(transformNode, positionNode).matrix;
    transformNode.addChild(factory.makeCalculatedProperty(transformNode.id, 'matrix', newMatrix));
    transformNode.removeChild(`${transformNode.id}.dsdx`);
    transformNode.removeChild(`${transformNode.id}.dtdx`);
    transformNode.removeChild(`${transformNode.id}.dsdy`);
    transformNode.removeChild(`${transformNode.id}.dtdy`);
  }
}
