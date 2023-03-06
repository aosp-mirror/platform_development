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

import {WindowToken} from '../common';
import {shortenName} from '../mixin';
import {WindowContainer} from './WindowContainer';

WindowToken.fromProto = (
  proto: any,
  isActivityInTree: boolean,
  nextSeq: () => number
): WindowToken => {
  if (!proto) {
    return null;
  }

  const windowContainer = WindowContainer.fromProto(
    /* proto */ proto.windowContainer,
    /* protoChildren */ proto.windowContainer?.children ?? [],
    /* isActivityInTree */ isActivityInTree,
    /* computedZ */ nextSeq,
    /* nameOverride */ proto.hashCode.toString(16),
    /* identifierOverride */ null,
    /* tokenOverride */ proto.hashCode
  );
  const entry = new WindowToken(windowContainer);
  entry.kind = entry.constructor.name;
  entry.proto = proto;
  entry.proto.configurationContainer = proto.windowContainer?.configurationContainer;
  entry.proto.surfaceControl = proto.windowContainer?.surfaceControl;
  entry.shortName = shortenName(entry.name);
  return entry;
};

export {WindowToken};
