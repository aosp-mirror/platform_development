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

import { WindowToken } from "../common"

import { applyMixins } from '../mixin'

import WindowContainer from "./WindowContainer"

export class WindowContainerMixin {
  get kind() {
    return "WindowToken"
  }

  static fromProto(proto) {
    const windowContainer = WindowContainer.fromProto(proto.windowContainer,
                                                      null)

    const windowToken = new WindowToken(windowContainer)

    const obj = Object.assign({}, proto)
    Object.assign(obj, windowContainer.obj)
    delete obj.windowContainer
    windowToken.attachObject(obj)

    return windowToken
  }
}

applyMixins(WindowToken, [WindowContainerMixin])

export default WindowToken
