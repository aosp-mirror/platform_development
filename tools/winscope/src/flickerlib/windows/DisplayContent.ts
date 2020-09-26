/*
 * Copyright 2020, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
  DisplayContent,
  Bounds
} from "../common"

import { applyMixins } from '../mixin'

import WindowContainer from "./WindowContainer"
import DisplayArea from "./DisplayArea"

export class DisplayContentMixin {
  get kind() {
    return "DisplayContent"
  }

  static fromProto(proto) {
    let rootDisplayArea;
    if (proto.rootDisplayArea.windowContainer == null) {
      // For backward compatibility
      const windowContainer = WindowContainer.fromProto(proto.windowContainer)
      rootDisplayArea = new DisplayArea(windowContainer)
    } else {
      // New protos should always be using this
      rootDisplayArea = DisplayArea.fromProto(proto.rootDisplayArea)
    }

    const bounds = new Bounds(
      proto.displayInfo.logicalWidth || 0,
      proto.displayInfo.logicalHeight || 0,
    )

    const displayContent = new DisplayContent(rootDisplayArea, bounds)

    const obj = Object.assign({}, proto)
    delete obj.windowContainer
    delete obj.rootDisplayArea
    Object.assign(obj, rootDisplayArea.obj)
    displayContent.attachObject(obj)

    return displayContent
  }
}

applyMixins(DisplayContent, [DisplayContentMixin])

export default DisplayContent