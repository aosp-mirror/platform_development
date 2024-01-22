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

import {perfetto} from 'protos/surfaceflinger/latest/static';
import {LayerCompositionType} from 'trace/layer_composition_type';
import {AddOperation} from 'trace/tree_node/operations/add_operation';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {PropertyTreeNodeFactory} from 'trace/tree_node/property_tree_node_factory';

export class AddCompositionType extends AddOperation<PropertyTreeNode> {
  protected override makeProperties(
    factory: PropertyTreeNodeFactory,
    value: PropertyTreeNode
  ): PropertyTreeNode[] {
    const hwcCompositionType = value.getChildByName('hwcCompositionType')?.getValue();
    let compositionType: LayerCompositionType | undefined;

    if (hwcCompositionType === perfetto.protos.HwcCompositionType.HWC_TYPE_CLIENT) {
      compositionType = LayerCompositionType.GPU;
    } else if (
      hwcCompositionType === perfetto.protos.HwcCompositionType.HWC_TYPE_DEVICE ||
      hwcCompositionType === perfetto.protos.HwcCompositionType.HWC_TYPE_SOLID_COLOR
    ) {
      compositionType = LayerCompositionType.HWC;
    }

    return compositionType === undefined
      ? []
      : [factory.makeCalculatedProperty(value.id, 'compositionType', compositionType)];
  }
}
