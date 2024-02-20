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
import {DEFAULT_PROPERTY_TREE_NODE_FACTORY} from 'trace/tree_node/property_tree_node_factory';

export class UpdateTransforms implements Operation<PropertyTreeNode> {
  apply(value: PropertyTreeNode): void {
    this.updateTransform(
      value.getChildByName('transform'),
      value.getChildByName('position'),
    );

    this.updateTransform(
      value.getChildByName('requestedTransform'),
      value.getChildByName('requestedPosition'),
    );

    this.updateTransform(value.getChildByName('bufferTransform'), undefined);

    const inputWindowInfo = value.getChildByName('inputWindowInfo');
    if (inputWindowInfo) {
      this.updateTransform(
        inputWindowInfo.getChildByName('transform'),
        undefined,
      );
    }
  }

  private updateTransform(
    transformNode: PropertyTreeNode | undefined,
    positionNode: PropertyTreeNode | undefined,
  ) {
    if (!transformNode) return;
    if (transformNode.getChildByName('matrix')) return;

    const newMatrix = Transform.from(transformNode, positionNode).matrix;
    transformNode.addOrReplaceChild(
      DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
        transformNode.id,
        'matrix',
        newMatrix,
      ),
    );
    transformNode.removeChild(`${transformNode.id}.dsdx`);
    transformNode.removeChild(`${transformNode.id}.dtdx`);
    transformNode.removeChild(`${transformNode.id}.dsdy`);
    transformNode.removeChild(`${transformNode.id}.dtdy`);
  }
}
