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

import { TagTrace } from "./common"
import TagState from "./tags/TagState"

TagTrace.fromProto = function (proto: any): TagTrace {
    const states = [];
    for (const stateProto of proto.states) {
        const transformedState = TagState.fromProto(
            stateProto.timestamp,
            stateProto.tags);

        states.push(transformedState);
    }
    const source = null;
    return new TagTrace(states, source);
}

export default TagTrace;
