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
import { Activity } from "../common"
import { VISIBLE_CHIP } from '../treeview/Chips'
import WindowContainer from "./WindowContainer"

Activity.fromProto = function (proto: any): Activity {
    if (proto == null) {
        return null;
    } else {
        const windowContainer = WindowContainer.fromProto(
            /* proto */ proto.windowToken.windowContainer,
            /* protoChildren */ proto.windowToken.windowContainer?.children?.reverse() ?? [],
            /* isActivityInTree */ true,
            /* nameOverride */ null,
            /* identifierOverride */ proto.identifier
        );

        const entry = new Activity(
            proto.name,
            proto.state,
            proto.visible,
            proto.frontOfTask,
            proto.procId,
            proto.translucent,
            windowContainer
        );

        addAttributes(entry, proto);
        return entry;
    }
}

function addAttributes(entry: Activity, proto: any) {
    entry.proto = proto;
    entry.kind = entry.constructor.name;
    entry.shortName = shortenName(entry.name);
    entry.chips = entry.isVisible ? [VISIBLE_CHIP] : [];
}

export default Activity;
