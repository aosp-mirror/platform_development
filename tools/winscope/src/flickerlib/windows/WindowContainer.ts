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

import { WindowContainer } from "../common"

import { applyMixins } from '../mixin'

import IClickableTreeViewElement from '../treeview/IClickableTreeViewElement'
import { TreeViewObject } from '../treeview/types'

import DisplayArea from "./DisplayArea"
import DisplayContent from "./DisplayContent"
import Task from "./Task"
import ActivityRecord from "./ActivityRecord"
import WindowToken from "./WindowToken"
import WindowState from "./WindowState"

import { CompatibleFeatures } from '@/utils/compatibility.js'

export class WindowContainerMixin implements IClickableTreeViewElement {
  title: string
  windowHashCode: number
  childrenWindows
  visible: boolean
  rawTreeViewObject

  _obj: { name: string }
  _children

  get kind() {
    return "WindowContainer"
  }

  get name() {
    if (this.obj && this.obj.name) {
      return this.obj.name
    }

    if (["WindowContainer", "Task"].includes(this.title)) {
      return null
    }

    return `${removeRedundancyInName(this.title)}@${this.windowHashCode}`
  }

  get shortName() {
    return this.name ? shortenName(this.name) : null
  }

  get stableId() {
    return this.windowHashCode
  }

  get children() {
    return this._children ?? this.childrenWindows
  }

  set children(children) {
    this._children = children
  }

  get chips() {
    return []
  }

  set obj(obj) {
    this._obj = obj
  }

  get obj() {
    return this._obj
  }

  static fromProto(proto, parent_identifier) {
    const children = proto.children.map(
      child => transformWindowContainerChildProto(child))

    // A kind of hacky way to check if the proto is from a device which
    // didn't have the changes required for the diff viewer to work properly
    // We required that all elements have a stable identifier in order to do the
    // diff properly. In theory properties diff would still work, but are
    // currently disabled. If required in the future don't hesitate to make
    // the required changes.
    if (!proto.identifier) {
      CompatibleFeatures.DiffVisualization = false;
    }

    const fallback_title = parent_identifier?.title ?? ""
    const fallback_hashCode = parent_identifier?.hashCode ?? ""
    const title = proto.identifier?.title ?? fallback_title
    const hashCode = proto.identifier?.hashCode ?? fallback_hashCode
    const visible = proto.visible
    const windowContainer =
      new WindowContainer(children, title, hashCode, visible)

    const obj = Object.assign({}, proto)
    // we remove the children property from the object to avoid it showing the
    // the properties view of the element as we can always see those elements'
    // properties by changing the target element in the hierarchy tree view.
    delete obj.children
    windowContainer.attachObject(obj)

    return windowContainer
  }

  attachObject(obj) {
    this._obj = obj
  }

  asRawTreeViewObject(): TreeViewObject {
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
      };
    }

    return this.rawTreeViewObject;
  }
}

applyMixins(WindowContainer, [WindowContainerMixin])

function transformWindowContainerChildProto(proto) {
  if (proto.displayArea != null) {
    return DisplayArea.fromProto(proto.displayArea)
  }

  if (proto.displayContent != null) {
    return DisplayContent.fromProto(proto.displayContent)
  }

  if (proto.task != null) {
    return Task.fromProto(proto.task)
  }

  if (proto.activity != null) {
    return ActivityRecord.fromProto(proto.activity)
  }

  if (proto.windowToken != null) {
    return WindowToken.fromProto(proto.windowToken)
  }

  if (proto.window != null) {
    return WindowState.fromProto(proto.window)
  }

  throw new Error("Unhandled WindowContainerChildProto case...")
}

function removeRedundancyInName(name: string): string {
  if (!name.includes('/')) {
    return name
  }

  const split = name.split('/')
  const pkg = split[0]
  var clazz = split.slice(1).join("/")

  if (clazz.startsWith("$pkg.")) {
    clazz = clazz.slice(pkg.length + 1)

    return "$pkg/$clazz"
  }

  return name
}

function shortenName(name: string): string {
  const classParts = name.split(".")

  if (classParts.length <= 3) {
    return name
  }

  const className = classParts.slice(-1)[0] // last element

  return `${classParts[0]}.${classParts[1]}.(...).${className}`
}

export default WindowContainer
