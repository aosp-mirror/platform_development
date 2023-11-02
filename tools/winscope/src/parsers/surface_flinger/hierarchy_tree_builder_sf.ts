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

import {assertDefined} from 'common/assert_utils';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {HierarchyTreeNodeBuilder} from 'trace/tree_node/hierarchy_tree_node_builder';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {PropertyTreeNodeFactory} from 'trace/tree_node/property_tree_node_factory';
import {RectsComputation} from './computations/rects_computation';
import {VisibilityPropertiesComputation} from './computations/visibility_properties_computation';
import {ZOrderPathsComputation} from './computations/z_order_paths_computation';

export class HierarchyTreeBuilderSf {
  private processed = new Map<number, number>();
  private entry: PropertiesProvider | undefined;
  private layers: PropertiesProvider[] | undefined;
  private excludesCompositionState: boolean = false;

  setEntry(value: PropertiesProvider): this {
    this.entry = value;
    return this;
  }

  setLayers(value: PropertiesProvider[]): this {
    this.layers = value;
    return this;
  }

  setExcludesCompositionState(value: boolean): this {
    this.excludesCompositionState = value;
    return this;
  }

  build(): HierarchyTreeNode {
    if (!this.entry) {
      throw Error('entry not set');
    }
    if (!this.layers) {
      throw Error('layers not set');
    }

    const idToLayer = this.buildIdToLayerMap(this.layers);
    const rootLayers = this.findRootLayers(this.layers);

    const rootChildren = rootLayers.map((layer) => {
      return this.buildSubtree(layer, this.excludesCompositionState, idToLayer);
    });

    const root = new HierarchyTreeNodeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setPropertiesProvider(this.entry)
      .setChildren(rootChildren)
      .build();

    const rootWithZOrderPaths = new ZOrderPathsComputation().setRoot(root).execute();

    const displays = root.getEagerPropertyByName('displays')?.getAllChildren() ?? [];

    const rootWithVisibility = new VisibilityPropertiesComputation()
      .setRoot(rootWithZOrderPaths)
      .setDisplays(displays)
      .execute();

    const finalRoot = new RectsComputation()
      .setHierarchyRoot(rootWithVisibility)
      .setDisplays(displays)
      .execute();

    return finalRoot;
  }

  private buildIdToLayerMap(layers: PropertiesProvider[]): Map<number, PropertiesProvider[]> {
    const map = layers.reduce((map, layer) => {
      const layerProperties = layer.getEagerProperties();
      const id = assertDefined(layerProperties.getChildByName('id')).getValue();
      const curr = map.get(id);
      if (curr) {
        curr.push(layer);
        console.warn(`Duplicate layer id ${id} found. Adding it as duplicate to the hierarchy`);
        layer.addEagerProperty(
          new PropertyTreeNodeFactory().makeCalculatedProperty(
            layerProperties.id,
            'isDuplicate',
            true
          )
        );
      } else {
        map.set(id, [layer]);
      }
      return map;
    }, new Map<number, PropertiesProvider[]>());
    return map;
  }

  private findRootLayers(layers: PropertiesProvider[]): PropertiesProvider[] {
    return layers.filter((layer) => {
      const hasParent =
        assertDefined(layer.getEagerProperties().getChildByName('parent')).getValue() !== -1;
      return !hasParent;
    });
  }

  private buildSubtree(
    layer: PropertiesProvider,
    excludesCompositionState: boolean,
    idToLayer: Map<number, PropertiesProvider[]>
  ): HierarchyTreeNode {
    const eagerProperties = layer.getEagerProperties();
    const id = assertDefined(eagerProperties.getChildByName('id')).getValue();
    const name = assertDefined(eagerProperties.getChildByName('name')).getValue();
    const duplicateCount = this.processed.get(id) ?? 0;
    this.processed.set(id, duplicateCount + 1);

    const childIds = assertDefined(eagerProperties.getChildByName('children')).getAllChildren();

    const children: HierarchyTreeNode[] = [];
    childIds.forEach((childId: PropertyTreeNode) => {
      const numberId = Number(childId.getValue());
      assertDefined(idToLayer.get(numberId)).forEach((childLayer) => {
        children.push(this.buildSubtree(childLayer, excludesCompositionState, idToLayer));
      });
    });

    return new HierarchyTreeNodeBuilder()
      .setId(id)
      .setName(name)
      .setDuplicateCount(duplicateCount)
      .setPropertiesProvider(layer)
      .setChildren(children)
      .build();
  }
}
