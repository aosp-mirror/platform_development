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

import {Rect} from 'common/geometry/rect';
import {Region} from 'common/geometry/region';
import {PropertyTreeNode} from './tree_node/property_tree_node';

export class GeometryFactory {
  static makeRect(node: PropertyTreeNode): Rect {
    const left = node.getChildByName('left')?.getValue() ?? 0;
    const top = node.getChildByName('top')?.getValue() ?? 0;
    const right = node.getChildByName('right')?.getValue() ?? 0;
    const bottom = node.getChildByName('bottom')?.getValue() ?? 0;
    return new Rect(left, top, right - left, bottom - top);
  }

  static makeRegion(node: PropertyTreeNode): Region {
    const rects =
      node
        .getChildByName('rect')
        ?.getAllChildren()
        .map((rectNode) => GeometryFactory.makeRect(rectNode)) ?? [];
    return new Region(rects);
  }
}
