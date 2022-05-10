/*
 * Copyright 2021, The Android Open Source Project
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


import { Tag } from "../common";
import TransitionType from "./TransitionType";

const transitionTypeMap = new Map([
  ['ROTATION', TransitionType.ROTATION],
  ['PIP_ENTER', TransitionType.PIP_ENTER],
  ['PIP_RESIZE', TransitionType.PIP_RESIZE],
  ['PIP_EXPAND', TransitionType.PIP_EXPAND],
  ['PIP_EXIT', TransitionType.PIP_EXIT],
  ['APP_LAUNCH', TransitionType.APP_LAUNCH],
  ['APP_CLOSE', TransitionType.APP_CLOSE],
  ['IME_APPEAR', TransitionType.IME_APPEAR],
  ['IME_DISAPPEAR', TransitionType.IME_DISAPPEAR],
  ['APP_PAIRS_ENTER', TransitionType.APP_PAIRS_ENTER],
  ['APP_PAIRS_EXIT', TransitionType.APP_PAIRS_EXIT],
]);

Tag.fromProto = function (proto: any): Tag {
    const tag = new Tag(
        proto.id,
        transitionTypeMap.get(proto.transition),
        proto.isStartTag,
        proto.layerId,
        proto.windowToken,
        proto.taskId
    );
    return tag;
};

export default Tag;
