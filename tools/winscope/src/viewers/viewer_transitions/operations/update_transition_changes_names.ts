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

import {FixedStringFormatter, formatAsHex} from 'trace/tree_node/formatters';
import {Operation} from 'trace/tree_node/operations/operation';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

export class UpdateTransitionChangesNames
  implements Operation<PropertyTreeNode>
{
  constructor(
    private readonly layerIdToName: Map<number, string>,
    private readonly windowTokenToTitle: Map<string, string>,
  ) {}

  apply(node: PropertyTreeNode): void {
    node
      .getChildByName('wmData')
      ?.getChildByName('targets')
      ?.getAllChildren()
      .forEach((target) => {
        const layerId = target.getChildByName('layerId');
        if (layerId) {
          const layerIdValue = Number(layerId.getValue());
          const layerName = this.layerIdToName.get(layerIdValue);
          if (layerName) {
            layerId.setFormatter(
              new FixedStringFormatter(`${layerIdValue} (${layerName})`),
            );
          }
        }

        const windowId = target.getChildByName('windowId');
        if (windowId) {
          const windowIdValue = windowId.getValue();
          const windowTitle = this.windowTokenToTitle.get(
            windowIdValue.toString(16),
          );
          const windowIdString = formatAsHex(windowIdValue);
          windowId.setFormatter(
            new FixedStringFormatter(
              windowTitle
                ? `${windowIdString} (${windowTitle})`
                : `${windowIdString}`,
            ),
          );
        }
      });
  }
}
