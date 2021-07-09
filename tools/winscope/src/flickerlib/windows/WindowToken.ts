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

import { shortenName } from '../mixin'
import { asRawTreeViewObject } from '../../utils/diff.js'
import { WindowToken } from "../common"
import WindowContainer from "./WindowContainer"

WindowToken.fromProto = function (proto, isActivityInTree: Boolean): WindowToken {
    if (proto == null) {
        return null
    }

    const children = proto.windowContainer.children.reverse()
        .filter(it => it != null)
        .map(it => WindowContainer.childrenFromProto(it, isActivityInTree))
    const windowContainer = WindowContainer.fromProto({proto: proto.windowContainer,
        children: children, tokenOverride: proto.hashCode})
    if (windowContainer == null) {
        throw "Window container should not be null: " + JSON.stringify(proto)
    }
    const entry = new WindowToken(windowContainer)
    entry.kind = entry.constructor.name
    entry.proto = proto
    entry.shortName = shortenName(entry.name)
    entry.rawTreeViewObject = asRawTreeViewObject(entry)

    console.warn("Created ", entry.kind, " stableId=", entry.stableId)
    return entry
}

export default WindowToken
