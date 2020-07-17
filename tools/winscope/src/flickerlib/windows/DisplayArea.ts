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
  DisplayArea,
} from "../common"

import { applyMixins } from '../mixin'

import WindowContainer from "./WindowContainer"

export class DisplayAreaMixin {
  get kind() {
    return "DisplayArea"
  }

  static fromProto(proto) {
    const windowContainer = WindowContainer.fromProto(proto.windowContainer)

    const displayArea = new DisplayArea(windowContainer)

    const obj = Object.assign({}, proto)
    delete obj.windowContainer
    Object.assign(obj, windowContainer.obj)
    displayArea.attachObject(obj)

    return displayArea
  }
}

applyMixins(DisplayArea, [DisplayAreaMixin])

export default DisplayArea