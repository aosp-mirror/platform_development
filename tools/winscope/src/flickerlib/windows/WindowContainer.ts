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
import Task from "./Task"
import TaskFragment from "./TaskFragment"
import WindowState from "./WindowState"
import WindowToken from "./WindowToken"

WindowContainer.fromProto = function (
    proto: any,
    protoChildren: any[],
    isActivityInTree: boolean,
    nameOverride: string = null,
    identifierOverride: string = null,
    tokenOverride = null,
): WindowContainer {
    if (proto == null) {
        return null;
    }

    const children = protoChildren
        .filter(it => it != null)
        .map(it => WindowContainer.childrenFromProto(it, isActivityInTree))
        .filter(it => it != null);

    const identifier = identifierOverride ?? proto.identifier;
    var name = nameOverride ?? identifier?.title ?? "";
    var token = tokenOverride?.toString(16) ?? identifier?.hashCode?.toString(16) ?? "";

    const config = createConfigurationContainer(proto.configurationContainer);
    const entry = new WindowContainer(
        name,
        token,
        proto.orientation,
        proto.surfaceControl?.layerId ?? 0,
        proto.visible,
        config,
        children
    );

    addAttributes(entry, proto);
    return entry;
}

function addAttributes(entry: WindowContainer, proto: any) {
    entry.proto = proto;
    entry.kind = entry.constructor.name;
    entry.shortName = shortenName(entry.name);
}

WindowContainer.childrenFromProto = function(proto: any, isActivityInTree: Boolean): WindowContainerChild {
    return DisplayContent.fromProto(proto.displayContent, isActivityInTree) ??
        DisplayArea.fromProto(proto.displayArea, isActivityInTree) ??
        Task.fromProto(proto.task, isActivityInTree) ??
        TaskFragment.fromProto(proto.taskFragment, isActivityInTree) ??
        Activity.fromProto(proto.activity) ??
        WindowToken.fromProto(proto.windowToken, isActivityInTree) ??
        WindowState.fromProto(proto.window, isActivityInTree) ??
        WindowContainer.fromProto(proto.windowContainer);
}

function createConfigurationContainer(proto: any): ConfigurationContainer {
    const entry = new ConfigurationContainer(
        createConfiguration(proto?.overrideConfiguration ?? null),
        createConfiguration(proto?.fullConfiguration ?? null),
        createConfiguration(proto?.mergedOverrideConfiguration ?? null)
    );

    entry.obj = entry;
    return entry;
}

function createConfiguration(proto: any): Configuration {
    if (proto == null) {
        return null;
    }
    var windowConfiguration = null;

    if (proto != null && proto.windowConfiguration != null) {
        windowConfiguration = createWindowConfiguration(proto.windowConfiguration);
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
    );
}

function createWindowConfiguration(proto: any): WindowConfiguration {
    return new WindowConfiguration(
        toRect(proto.appBounds),
        toRect(proto.bounds),
        toRect(proto.maxBounds),
        proto.windowingMode,
        proto.activityType
    );
}

export default WindowContainer;
