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
    this.updateDeprecatedTransform(
      value.getChildByName('transform'),
      value.getChildByName('position'),
    );

    this.updateDeprecatedTransform(
      value.getChildByName('requestedTransform'),
      value.getChildByName('requestedPosition'),
    );

    this.updateDeprecatedTransform(
      value.getChildByName('bufferTransform'),
      undefined,
    );

    const inputWindowInfo = value.getChildByName('inputWindowInfo');
    if (inputWindowInfo) {
      this.updateDeprecatedTransform(
        inputWindowInfo.getChildByName('transform'),
        undefined,
      );
    }
  }

  private updateDeprecatedTransform(
    transformNode: PropertyTreeNode | undefined,
    positionNode: PropertyTreeNode | undefined,
  ) {
    if (!transformNode) return;
    if (transformNode.getChildByName('matrix')) return;

    this.adjustDeprecatedTransformNode(transformNode);

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

  // Adjust the transform node to correct for the transforms that were incorrectly
  // written to the proto. Any transforms that are newly logged from the platform should
  // be carefully written to the proto correctly, and should not be adjusted here.
  private adjustDeprecatedTransformNode(transformNode: PropertyTreeNode) {
    // Get the corrected values from the transform node.
    const dsdx = transformNode.getChildByName('dsdx')?.getValue() ?? 0;
    const dtdx = transformNode.getChildByName('dsdy')?.getValue() ?? 0;
    const dtdy = transformNode.getChildByName('dtdx')?.getValue() ?? 0;
    const dsdy = transformNode.getChildByName('dtdy')?.getValue() ?? 0;

    const id = transformNode.id;
    transformNode.addOrReplaceChild(
      DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeProtoProperty(id, 'dsdx', dsdx),
    );
    transformNode.addOrReplaceChild(
      DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeProtoProperty(id, 'dtdx', dtdx),
    );
    transformNode.addOrReplaceChild(
      DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeProtoProperty(id, 'dtdy', dtdy),
    );
    transformNode.addOrReplaceChild(
      DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeProtoProperty(id, 'dsdy', dsdy),
    );
  }
}
