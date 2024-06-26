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
import {FixedStringFormatter} from 'trace/tree_node/formatters';
import {Operation} from 'trace/tree_node/operations/operation';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';

export class UpdateTransitionChangesNames
  implements Operation<UiPropertyTreeNode>
{
  constructor(
    private readonly layerIdToName: Map<number, string>,
    private readonly windowTokenToTitle: Map<string, string>,
  ) {}

  apply(node: UiPropertyTreeNode): void {
    assertDefined(node.getChildByName('wmData'))
      .getChildByName('targets')
      ?.getAllChildren()
      .forEach((target) => {
        const layerId = target.getChildByName('layerId');
        if (layerId) {
          const layerIdValue = Number(layerId.getValue());
          const layerName = this.layerIdToName.get(layerIdValue);
          if (layerName) {
            layerId.setFormatter(
              new FixedStringFormatter(`${layerIdValue} ${layerName}`),
            );
          }
        }

        const windowId = target.getChildByName('windowId');
        if (windowId) {
          const windowIdString = windowId.getValue().toString(16);
          const windowTitle = this.windowTokenToTitle.get(windowIdString);
          windowId.setFormatter(
            new FixedStringFormatter(
              windowTitle
                ? `0x${windowIdString} (${windowTitle})`
                : `0x${windowIdString}`,
            ),
          );
        }
      });
  }
}
