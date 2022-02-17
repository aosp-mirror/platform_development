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

import ObjectFormatter from "./ObjectFormatter"

/**
 * Get the properties of a WM object for display.
 *
 * @param entry WM hierarchy element
 * @param proto Associated proto object
 */
export function getPropertiesForDisplay(entry: any): any {
    if (!entry) {
        return
    }

    let obj: any = {}
    const properties = ObjectFormatter.getProperties(entry)
    properties.forEach(prop => obj[prop] = entry[prop]);

    // we remove the children property from the object to avoid it showing the
    // the properties view of the element as we can always see those elements'
    // properties by changing the target element in the hierarchy tree view.
    if (obj.children) delete obj.children
    if (obj.proto) delete obj.proto

    obj.proto = Object.assign({}, entry.proto)
    if (obj.proto.children) delete obj.proto.children
    if (obj.proto.childWindows) delete obj.proto.childWindows
    if (obj.proto.childrenWindows) delete obj.proto.childrenWindows
    if (obj.proto.childContainers) delete obj.proto.childContainers
    if (obj.proto.windowToken) delete obj.proto.windowToken
    if (obj.proto.rootDisplayArea) delete obj.proto.rootDisplayArea
    if (obj.proto.rootWindowContainer) delete obj.proto.rootWindowContainer
    if (obj.proto.windowContainer?.children) delete obj.proto.windowContainer.children
    obj = ObjectFormatter.format(obj)

    return obj
}

export function shortenName(name: any): string {
    const classParts = (name + "").split(".")
    if (classParts.length <= 3) {
        return name
    }
    const className = classParts.slice(-1)[0] // last element
    return `${classParts[0]}.${classParts[1]}.(...).${className}`
}
