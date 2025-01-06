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
import {DuplicateLayerIds, MissingLayerIds} from 'messaging/user_warnings';
import {perfetto} from 'protos/surfaceflinger/latest/static';
import {android} from 'protos/surfaceflinger/udc/static';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {
  LazyPropertiesStrategyType,
  PropertiesProvider,
} from 'trace/tree_node/properties_provider';
import {PropertiesProviderBuilder} from 'trace/tree_node/properties_provider_builder';
import {PropertyTreeBuilderFromProto} from 'trace/tree_node/property_tree_builder_from_proto';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {COMMON_OPERATIONS} from './common_operations';
import {RectsComputation} from './computations/rects_computation';
import {VisibilityPropertiesComputation} from './computations/visibility_properties_computation';
import {ZOrderPathsComputation} from './computations/z_order_paths_computation';
import {DENYLIST_PROPERTIES} from './denylist_properties';
import {EAGER_PROPERTIES} from './eager_properties';
import {HierarchyTreeBuilderSf} from './hierarchy_tree_builder_sf';
import {ParserSurfaceFlinger as LegacyParserSurfaceFlinger} from './legacy/parser_surface_flinger';
import {ParserSurfaceFlinger as PerfettoParserSurfaceFlinger} from './perfetto/parser_surface_flinger';

export class EntryHierarchyTreeFactory {
  makeEntryHierarchyTree(
    entryProto: EntryType,
    layerProtos: LayerType[],
    ParserSurfaceFlinger: ParserSurfaceFlinger,
  ): HierarchyTreeNode {
    const excludesCompositionState =
      entryProto?.excludesCompositionState ?? true;
    const addExcludesCompositionState = excludesCompositionState
      ? COMMON_OPERATIONS.AddExcludesCompositionStateTrue
      : COMMON_OPERATIONS.AddExcludesCompositionStateFalse;

    const processed = new Map<number, number>();
    let missingLayerIds = false;

    const layers = layerProtos.reduce(
      (allLayerProps: PropertiesProvider[], layer: LayerType) => {
        if (layer.id === null || layer.id === undefined) {
          missingLayerIds = true;
          return allLayerProps;
        }
        const duplicateCount = processed.get(assertDefined(layer.id)) ?? 0;
        processed.set(assertDefined(layer.id), duplicateCount + 1);
        const eagerProperties = this.makeEagerPropertiesTree(
          layer,
          duplicateCount,
        );
        const lazyPropertiesStrategy = this.makeLayerLazyPropertiesStrategy(
          layer,
          duplicateCount,
        );

        const layerProps = new PropertiesProviderBuilder()
          .setEagerProperties(eagerProperties)
          .setLazyPropertiesStrategy(lazyPropertiesStrategy)
          .setCommonOperations([
            ParserSurfaceFlinger.Operations.SetFormattersLayer,
            ParserSurfaceFlinger.Operations.TranslateIntDefLayer,
          ])
          .setEagerOperations([
            ParserSurfaceFlinger.Operations.AddDefaultsLayerEager,
            COMMON_OPERATIONS.AddCompositionType,
            COMMON_OPERATIONS.UpdateTransforms,
            COMMON_OPERATIONS.AddVerboseFlags,
            addExcludesCompositionState,
          ])
          .setLazyOperations([
            ParserSurfaceFlinger.Operations.AddDefaultsLayerLazy,
          ])
          .build();
        allLayerProps.push(layerProps);
        return allLayerProps;
      },
      [] as PropertiesProvider[],
    );

    const entry = new PropertiesProviderBuilder()
      .setEagerProperties(this.makeEntryEagerPropertiesTree(entryProto))
      .setLazyPropertiesStrategy(
        this.makeEntryLazyPropertiesStrategy(entryProto),
      )
      .setCommonOperations([
        COMMON_OPERATIONS.AddDisplayProperties,
        ParserSurfaceFlinger.Operations.SetFormattersEntry,
        ParserSurfaceFlinger.Operations.TranslateIntDefEntry,
      ])
      .setEagerOperations([
        ParserSurfaceFlinger.Operations.AddDefaultsEntryEager,
      ])
      .setLazyOperations([ParserSurfaceFlinger.Operations.AddDefaultsEntryLazy])
      .build();

    const tree = new HierarchyTreeBuilderSf()
      .setRoot(entry)
      .setChildren(layers)
      .setComputations([
        new ZOrderPathsComputation(),
        new VisibilityPropertiesComputation(),
        new RectsComputation(),
      ])
      .build();

    if (missingLayerIds) {
      tree.addWarning(new MissingLayerIds());
    }
    const duplicateIds = Array.from(processed.keys()).filter(
      (layerId) => assertDefined(processed.get(layerId)) > 1,
    );
    if (duplicateIds.length > 0) {
      tree.addWarning(new DuplicateLayerIds(duplicateIds));
    }

    return tree;
  }

  private makeEagerPropertiesTree(
    layer: LayerType,
    duplicateCount: number,
  ): PropertyTreeNode {
    const denyList: string[] = [];
    let obj = layer;
    do {
      Object.getOwnPropertyNames(obj).forEach((it) => {
        if (!EAGER_PROPERTIES.includes(it)) denyList.push(it);
      });
      obj = Object.getPrototypeOf(obj);
    } while (obj);

    return new PropertyTreeBuilderFromProto()
      .setData(layer)
      .setRootId(assertDefined(layer.id))
      .setRootName(assertDefined(layer.name))
      .setDenyList(denyList)
      .setDuplicateCount(duplicateCount)
      .build();
  }

  private makeEntryEagerPropertiesTree(entry: EntryType): PropertyTreeNode {
    const denyList: string[] = [];
    let obj = entry;
    do {
      Object.getOwnPropertyNames(obj).forEach((it) => {
        if (it !== 'displays') denyList.push(it);
      });
      obj = Object.getPrototypeOf(obj);
    } while (obj);

    return new PropertyTreeBuilderFromProto()
      .setData(entry)
      .setRootId('LayerTraceEntry')
      .setRootName('root')
      .setDenyList(denyList)
      .build();
  }

  private makeLayerLazyPropertiesStrategy(
    layer: LayerType,
    duplicateCount: number,
  ): LazyPropertiesStrategyType {
    return async () => {
      return new PropertyTreeBuilderFromProto()
        .setData(layer)
        .setRootId(assertDefined(layer.id))
        .setRootName(assertDefined(layer.name))
        .setDenyList(EAGER_PROPERTIES.concat(DENYLIST_PROPERTIES))
        .setDuplicateCount(duplicateCount)
        .build();
    };
  }

  private makeEntryLazyPropertiesStrategy(
    entry: EntryType,
  ): LazyPropertiesStrategyType {
    return async () => {
      return new PropertyTreeBuilderFromProto()
        .setData(entry)
        .setRootId('LayerTraceEntry')
        .setRootName('root')
        .setDenyList(DENYLIST_PROPERTIES)
        .build();
    };
  }
}

type EntryType =
  | android.surfaceflinger.ILayersTraceProto
  | perfetto.protos.ILayersSnapshotProto;

type LayerType =
  | android.surfaceflinger.ILayerProto
  | perfetto.protos.ILayerProto;

type ParserSurfaceFlinger =
  | typeof PerfettoParserSurfaceFlinger
  | typeof LegacyParserSurfaceFlinger;
