/*
 * Copyright (C) 2023 The Android Open Source Project
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
import {perfetto} from 'protos/surfaceflinger/latest/static';
import {android} from 'protos/surfaceflinger/udc/static';
import {LazyPropertiesStrategyType} from 'trace/tree_node/properties_provider';
import {PropertyTreeBuilderFromProto} from 'trace/tree_node/property_tree_builder_from_proto';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {AddCompositionType} from './operations/add_composition_type';
import {AddDisplayProperties} from './operations/add_display_properties';
import {AddExcludesCompositionState} from './operations/add_excludes_composition_state';
import {AddVerboseFlags} from './operations/add_verbose_flags';
import {UpdateTransforms} from './operations/update_transforms';

export class ParserSfUtils {
  static readonly EAGER_PROPERTIES = [
    'id',
    'name',
    'type',
    'parent',
    'children',
    'bounds',
    'transform',
    'position',
    'requestedTransform',
    'requestedPosition',
    'bufferTransform',
    'inputWindowInfo',
    'flags',
    'z',
    'cornerRadius',
    'layerStack',
    'isOpaque',
    'activeBuffer',
    'visibleRegion',
    'color',
    'isRelativeOf',
    'zOrderRelativeOf',
    'screenBounds',
    'shadowRadius',
    'backgroundBlurRadius',
    'hwcCompositionType',
  ];

  static readonly DENYLIST_PROPERTIES = [
    'length',
    'prototype',
    'ref',
    'parent',
    'timestamp',
    'layers',
    'children',
    'name',
  ];

  static readonly OPERATIONS = {
    UpdateTransforms: new UpdateTransforms(),
    AddVerboseFlags: new AddVerboseFlags(),
    AddExcludesCompositionStateTrue: new AddExcludesCompositionState(true),
    AddExcludesCompositionStateFalse: new AddExcludesCompositionState(false),
    AddDisplayProperties: new AddDisplayProperties(),
    AddCompositionType: new AddCompositionType(),
  };

  static makeEagerPropertiesTree(
    layer: android.surfaceflinger.ILayerProto | perfetto.protos.ILayerProto,
    duplicateCount: number,
  ): PropertyTreeNode {
    const denyList: string[] = [];
    let obj = layer;
    do {
      Object.getOwnPropertyNames(obj).forEach((it) => {
        if (!ParserSfUtils.EAGER_PROPERTIES.includes(it)) denyList.push(it);
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

  static makeEntryEagerPropertiesTree(
    entry:
      | android.surfaceflinger.ILayersTraceProto
      | perfetto.protos.ILayersSnapshotProto,
  ): PropertyTreeNode {
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

  static makeLayerLazyPropertiesStrategy(
    layer: android.surfaceflinger.ILayerProto | perfetto.protos.ILayerProto,
    duplicateCount: number,
  ): LazyPropertiesStrategyType {
    return async () => {
      return new PropertyTreeBuilderFromProto()
        .setData(layer)
        .setRootId(assertDefined(layer.id))
        .setRootName(assertDefined(layer.name))
        .setDenyList(
          ParserSfUtils.EAGER_PROPERTIES.concat(
            ParserSfUtils.DENYLIST_PROPERTIES,
          ),
        )
        .setDuplicateCount(duplicateCount)
        .build();
    };
  }

  static makeEntryLazyPropertiesStrategy(
    entry:
      | android.surfaceflinger.ILayersTraceProto
      | perfetto.protos.ILayersSnapshotProto,
  ): LazyPropertiesStrategyType {
    return async () => {
      return new PropertyTreeBuilderFromProto()
        .setData(entry)
        .setRootId('LayerTraceEntry')
        .setRootName('root')
        .setDenyList(ParserSfUtils.DENYLIST_PROPERTIES)
        .build();
    };
  }
}
