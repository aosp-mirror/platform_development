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
import { DisplayArea } from "../common"
import WindowContainer from "./WindowContainer"

DisplayArea.fromProto = function (proto: any, isActivityInTree: Boolean): DisplayArea {
    if (proto == null) {
        return null;
    } else {
        const windowContainer = WindowContainer.fromProto(
            /* proto */ proto.windowContainer,
            /* protoChildren */ proto.windowContainer?.children?.reverse() ?? [],
            /* isActivityInTree */ isActivityInTree,
            /* nameOverride */ proto.name
        );

        const entry = new DisplayArea(proto.isTaskDisplayArea, windowContainer);

        addAttributes(entry, proto);
        return entry;
    }
}

function addAttributes(entry: DisplayArea, proto: any) {
    entry.proto = proto;
    entry.kind = entry.constructor.name;
    entry.shortName = shortenName(entry.name);
}

export default DisplayArea;
