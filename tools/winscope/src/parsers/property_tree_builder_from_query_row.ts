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

import {assertDefined} from 'common/assert_utils';
import {convertSnakeToCamelCase} from 'common/string_utils';
import {
  PropertySource,
  PropertyTreeNode,
} from 'trace/tree_node/property_tree_node';
import {PropertyTreeNodeFactory} from 'trace/tree_node/property_tree_node_factory';
import {RowIteratorBase} from 'trace_processor/query_result';
import {AbstractPropertyTreeBuilder} from './abstract_property_tree_builder';

export class PropertyTreeBuilderFromQueryRow extends AbstractPropertyTreeBuilder<RowIteratorBase> {
  private columns: string[] | undefined;

  setColumns(value: string[]): this {
    this.columns = value;
    return this;
  }

  protected override buildPropertiesTree(rootNodeId: string): PropertyTreeNode {
    if (this.columns === undefined) {
      throw new Error('columns not set');
    }
    const factory = new PropertyTreeNodeFactory();

    const rootNode = factory.makePropertyRoot(
      rootNodeId,
      assertDefined(this.rootName),
      PropertySource.TP,
      undefined,
    );

    for (const col of this.columns) {
      const val = this.data?.get(col) ?? undefined;
      if (val !== undefined) {
        const colCamelCase = convertSnakeToCamelCase(col);
        const node = factory.makeTpProperty(rootNodeId, colCamelCase, val);
        rootNode.addOrReplaceChild(node);
      }
    }

    return rootNode;
  }
}
