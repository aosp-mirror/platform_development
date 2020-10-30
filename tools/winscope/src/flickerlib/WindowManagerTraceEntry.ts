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

import { nanosToString, TimeUnits } from "../utils/utils.js"

import { WindowManagerTraceEntry } from "./common"

import { applyMixins } from "./mixin"

import ITreeViewElement from './treeview/IClickableTreeViewElement'
import IClickableTreeViewElement from './treeview/IClickableTreeViewElement'
import Chip from './treeview/Chip'
import WindowContainer from "./windows/WindowContainer"

class WindowManagerTraceEntryMixin implements IClickableTreeViewElement {
  common: any
  kind: string
  name: string
  shortName: string
  stableId: string
  chips: Array<Chip>
  children: Array<ITreeViewElement>
  obj: any
  rawTreeViewObject

  timestamp: number
  rootWindow

  mixinConstructor(obj) {
    const name = this.timestamp ? nanosToString(this.timestamp, TimeUnits.MILLI_SECONDS) : ""
    this.kind = "entry"
    this.name = name
    this.shortName = name
    this.stableId = "entry"
    this.children = this.rootWindow.children
    this.chips = []
    this.obj = obj
  }

  static fromProto(proto, timestamp) {
    const rootWindow =
      WindowContainer.fromProto(proto.rootWindowContainer.windowContainer, null)
    const windowManagerTraceEntry =
      new WindowManagerTraceEntry(rootWindow, timestamp)

    windowManagerTraceEntry.kind = 'service'

    windowManagerTraceEntry.focusedApp = proto.focusedApp
    windowManagerTraceEntry.focusedDisplayId = proto.focusedDisplayId
    windowManagerTraceEntry.lastOrientation = proto.lastOrientation
    windowManagerTraceEntry.policy = proto.policy
    windowManagerTraceEntry.rotation = proto.rotation
    windowManagerTraceEntry.displayFrozen = proto.displayFrozen
    windowManagerTraceEntry.inputMethodWindow = proto.inputMethodWindow

    // Remove anything that is part of the children elements
    // allows for faster loading of properties and less information cluttering
    // this applied to anywhere the proto is passed to be saved as .obj
    const obj = Object.assign({}, proto)
    obj.rootWindowContainer = {};
    Object.assign(obj.rootWindowContainer,
      proto.rootWindowContainer)
    obj.rootWindowContainer.windowContainer = {};
    Object.assign(obj.rootWindowContainer.windowContainer,
      proto.rootWindowContainer.windowContainer)
    delete obj.rootWindowContainer.windowContainer.children
    windowManagerTraceEntry.mixinConstructor(obj)

    return windowManagerTraceEntry
  }

  attachObject(obj) {
    this.obj = obj
  }

  asRawTreeViewObject() {
    // IMPORTANT: We want to always return the same tree view object and not
    // generate a new one every time this function is called.
    if (!this.rawTreeViewObject) {
      const children = this.children.map(child => child.asRawTreeViewObject())

      this.rawTreeViewObject = {
        kind: this.kind,
        name: this.name,
        shortName: this.shortName,
        stableId: this.stableId,
        chips: this.chips,
        obj: this.obj,
        children,
        ref: this,
      }
    }

    return this.rawTreeViewObject;
  }
}

applyMixins(WindowManagerTraceEntry, [WindowManagerTraceEntryMixin])

export default WindowManagerTraceEntry;
