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
import { Task, toRect } from "../common"
import WindowContainer from "./WindowContainer"

Task.fromProto = function (proto: any, isActivityInTree: Boolean): Task {
    if (proto == null) {
        return null;
    } else {
        const windowContainerProto = proto.taskFragment?.windowContainer ?? proto.windowContainer;
        const windowContainer = WindowContainer.fromProto(
            /* proto */ windowContainerProto,
            /* protoChildren */ windowContainerProto?.children?.reverse() ?? [],
            /* isActivityInTree */ isActivityInTree
        );

        const entry = new Task(
            proto.taskFragment?.activityType ?? proto.activityType,
            proto.fillsParent,
            toRect(proto.bounds),
            proto.id,
            proto.rootTaskId,
            proto.taskFragment?.displayId,
            toRect(proto.lastNonFullscreenBounds),
            proto.realActivity,
            proto.origActivity,
            proto.resizeMode,
            proto.resumedActivity?.title ?? "",
            proto.animatingBounds,
            proto.surfaceWidth,
            proto.surfaceHeight,
            proto.createdByOrganizer,
            proto.taskFragment?.minWidth ?? proto.minWidth,
            proto.taskFragment?.minHeight ?? proto.minHeight,
            windowContainer
        );

        addAttributes(entry, proto);
        return entry;
    }
}

function addAttributes(entry: Task, proto: any) {
    entry.proto = proto;
    entry.kind = entry.constructor.name;
    entry.shortName = shortenName(entry.name);
}

export default Task;
