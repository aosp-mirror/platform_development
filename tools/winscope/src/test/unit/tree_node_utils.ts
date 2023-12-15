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

import {TransformType} from 'parsers/surface_flinger/transform_utils';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {OperationChain} from 'trace/tree_node/operations/operation_chain';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';
import {PropertySource, PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {PropertyTreeNodeFactory} from 'trace/tree_node/property_tree_node_factory';

export class TreeNodeUtils {
  static makeRectNode(
    left: number | undefined,
    top: number | undefined,
    right: number | undefined,
    bottom: number | undefined,
    id = 'test node'
  ): PropertyTreeNode {
    const rect = new PropertyTreeNode(`${id}.rect`, 'rect', PropertySource.PROTO, undefined);
    if (left !== undefined) {
      rect.addChild(new PropertyTreeNode(`${id}.rect.left`, 'left', PropertySource.PROTO, left));
    }
    if (top !== undefined) {
      rect.addChild(new PropertyTreeNode(`${id}.rect.top`, 'top', PropertySource.PROTO, top));
    }
    if (right !== undefined) {
      rect.addChild(new PropertyTreeNode(`${id}.rect.right`, 'right', PropertySource.PROTO, right));
    }
    if (bottom !== undefined) {
      rect.addChild(
        new PropertyTreeNode(`${id}.rect.bottom`, 'bottom', PropertySource.PROTO, bottom)
      );
    }
    return rect;
  }

  static makeColorNode(
    r: number | undefined,
    g: number | undefined,
    b: number | undefined,
    a: number | undefined
  ): PropertyTreeNode {
    const color = new PropertyTreeNode('test node.color', 'color', PropertySource.PROTO, undefined);
    if (r !== undefined) {
      color.addChild(new PropertyTreeNode('test node.color.r', 'r', PropertySource.PROTO, r));
    }
    if (g !== undefined) {
      color.addChild(new PropertyTreeNode('test node.color.g', 'g', PropertySource.PROTO, g));
    }
    if (b !== undefined) {
      color.addChild(new PropertyTreeNode('test node.color.b', 'b', PropertySource.PROTO, b));
    }
    if (a !== undefined) {
      color.addChild(new PropertyTreeNode('test node.color.a', 'a', PropertySource.PROTO, a));
    }
    return color;
  }

  static makeBufferNode(): PropertyTreeNode {
    const buffer = new PropertyTreeNode(
      'test node.buffer',
      'buffer',
      PropertySource.PROTO,
      undefined
    );
    buffer.addChild(
      new PropertyTreeNode('test node.buffer.height', 'height', PropertySource.PROTO, 0)
    );
    buffer.addChild(
      new PropertyTreeNode('test node.buffer.width', 'width', PropertySource.PROTO, 1)
    );
    buffer.addChild(
      new PropertyTreeNode('test node.buffer.stride', 'stride', PropertySource.PROTO, 0)
    );
    buffer.addChild(
      new PropertyTreeNode('test node.buffer.format', 'format', PropertySource.PROTO, 1)
    );
    return buffer;
  }

  static makeTransformNode(type: TransformType): PropertyTreeNode {
    const transform = new PropertyTreeNode(
      'test node.transform',
      'transform',
      PropertySource.PROTO,
      undefined
    );
    transform.addChild(
      new PropertyTreeNode('test node.transform.type', 'type', PropertySource.PROTO, type)
    );
    return transform;
  }

  static makeSizeNode(w: number | undefined, h: number | undefined): PropertyTreeNode {
    const size = new PropertyTreeNode('test node.size', 'size', PropertySource.PROTO, undefined);
    if (w !== undefined) {
      size.addChild(new PropertyTreeNode('test node.size.w', 'w', PropertySource.PROTO, w));
    }
    if (h !== undefined) {
      size.addChild(new PropertyTreeNode('test node.size.h', 'h', PropertySource.PROTO, h));
    }
    return size;
  }

  static makePositionNode(x: number | undefined, y: number | undefined): PropertyTreeNode {
    const pos = new PropertyTreeNode('test node.pos', 'pos', PropertySource.PROTO, undefined);
    if (x !== undefined) {
      pos.addChild(new PropertyTreeNode('test node.pos.x', 'x', PropertySource.PROTO, x));
    }
    if (y !== undefined) {
      pos.addChild(new PropertyTreeNode('test node.pos.y', 'y', PropertySource.PROTO, y));
    }
    return pos;
  }

  static makeHierarchyNode(proto: any): HierarchyTreeNode {
    const id = `${proto.id}`;
    const name = proto.name;
    const rootId = `${id} ${name}`;
    const propertiesTree = new PropertyTreeNode(rootId, name, PropertySource.PROTO, null);

    const factory = new PropertyTreeNodeFactory();
    for (const [key, value] of Object.entries(proto)) {
      const prop = factory.makeProtoProperty(rootId, key, value);
      propertiesTree.addChild(prop);
    }

    const provider = new PropertiesProvider(
      propertiesTree,
      async () => propertiesTree,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>()
    );

    return new HierarchyTreeNode(rootId, name, provider);
  }

  static makePropertyNode(rootId: string, name: string, value: any): PropertyTreeNode {
    return new PropertyTreeNodeFactory().makeProtoProperty(rootId, name, value);
  }

  static makeCalculatedPropertyNode(rootId: string, name: string, value: any): PropertyTreeNode {
    return new PropertyTreeNodeFactory().makeCalculatedProperty(rootId, name, value);
  }
}
