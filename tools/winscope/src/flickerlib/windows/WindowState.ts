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

import { WindowState, Rect } from "../common"
import { VISIBLE_CHIP } from '../treeview/Chips'

import { applyMixins } from '../mixin'

import WindowContainer from "./WindowContainer"

export class WindowStateMixin {
  visible: boolean

  get kind() {
    return "WindowState"
  }

  static fromProto(proto) {
    const windowContainer = WindowContainer.fromProto(proto.windowContainer,
                                                      proto.identifier)

    const frame = (proto.windowFrames ?? proto).frame ?? {}
    const rect = new Rect(frame.left ?? 0, frame.top ?? 0, frame.right ?? 0, frame.bottom ?? 0)

    const windowState =
      new WindowState(windowContainer, /* childWindows */[], rect)

    const obj = Object.assign({}, proto)
    Object.assign(obj, windowContainer.obj)
    delete obj.windowContainer
    windowState.attachObject(obj)

    return windowState
  }

  get chips() {
    return this.visible ? [VISIBLE_CHIP] : []
  }
}

applyMixins(WindowState, [WindowStateMixin])

export default WindowState
