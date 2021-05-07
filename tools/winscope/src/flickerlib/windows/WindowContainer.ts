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

import { getPropertiesForDisplay, shortenName } from '../mixin'
import { asRawTreeViewObject } from '../../utils/diff.js'

import {
    Configuration,
    ConfigurationContainer,
    toRect,
    WindowConfiguration,
    WindowContainer,
    WindowContainerChild,
 } from "../common"

import Activity from "./Activity"
import DisplayArea from "./DisplayArea"
import DisplayContent from "./DisplayContent"
import ActivityTask from "./ActivityTask"
import WindowState from "./WindowState"
import WindowToken from "./WindowToken"

WindowContainer.fromProto = function ({
    proto,
    children,
    nameOverride = null,
    identifierOverride = null,
    tokenOverride = null
}): WindowContainer {
    if (proto == null) {
        return null
    }
    const identifier = identifierOverride ?? proto.identifier
    var name = nameOverride ?? identifier?.title ?? ""
    var token = tokenOverride?.toString(16) ?? identifier?.hashCode?.toString(16) ?? ""

    const config = newConfigurationContainer(proto.configurationContainer)
    const entry = new WindowContainer(
        name,
        token,
        proto.orientation,
        proto.visible,
        config,
        children
    )

    // we remove the children property from the object to avoid it showing the
    // the properties view of the element as we can always see those elements'
    // properties by changing the target element in the hierarchy tree view.
    entry.obj = getPropertiesForDisplay(proto, entry)
    entry.kind = entry.constructor.name
    entry.shortName = shortenName(entry.name)
    entry.rawTreeViewObject = asRawTreeViewObject(entry)
    return entry
}

WindowContainer.childrenFromProto = function(proto, isActivityInTree: Boolean): WindowContainerChild {
    return DisplayContent.fromProto(proto.displayContent, isActivityInTree) ??
        DisplayArea.fromProto(proto.displayArea, isActivityInTree) ??
        ActivityTask.fromProto(proto.task, isActivityInTree) ??
        Activity.fromProto(proto.activity) ??
        WindowToken.fromProto(proto.windowToken, isActivityInTree) ??
        WindowState.fromProto(proto.window, isActivityInTree) ??
        WindowContainer.fromProto({proto: proto.windowContainer})
}

function newConfigurationContainer(proto): ConfigurationContainer {
    const entry = new ConfigurationContainer(
        newConfiguration(proto?.overrideConfiguration ?? null),
        newConfiguration(proto?.fullConfiguration ?? null),
        newConfiguration(proto?.mergedOverrideConfiguration ?? null)
    )

    entry.obj = entry
    return entry
}

function newConfiguration(proto): Configuration {
    if (proto == null) {
        return null
    }
    var windowConfiguration = null

    if (proto != null && proto.windowConfiguration != null) {
        windowConfiguration = newWindowConfiguration(proto.windowConfiguration)
    }

    return new Configuration(
        windowConfiguration,
        proto?.densityDpi ?? 0,
        proto?.orientation ?? 0,
        proto?.screenHeightDp ?? 0,
        proto?.screenHeightDp ?? 0,
        proto?.smallestScreenWidthDp ?? 0,
        proto?.screenLayout ?? 0,
        proto?.uiMode ?? 0
    )
}

function newWindowConfiguration(proto): WindowConfiguration {
    return new WindowConfiguration(
        toRect(proto.appBounds),
        toRect(proto.bounds),
        toRect(proto.maxBounds),
        proto.windowingMode,
        proto.activityType
    )
}

export default WindowContainer
