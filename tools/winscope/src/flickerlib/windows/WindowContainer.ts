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

import {shortenName} from '../mixin';

import {
  Configuration,
  ConfigurationContainer,
  toRect,
  WindowConfiguration,
  WindowContainer,
} from '../common';

import {Activity} from './Activity';
import {DisplayArea} from './DisplayArea';
import {DisplayContent} from './DisplayContent';
import {Task} from './Task';
import {TaskFragment} from './TaskFragment';
import {WindowState, WindowStateUtils} from './WindowState';
import {WindowToken} from './WindowToken';

WindowContainer.fromProto = (
  proto: any,
  protoChildren: any[],
  isActivityInTree: boolean,
  nextSeq: () => number,
  nameOverride: string | null = null,
  identifierOverride: string | null = null,
  tokenOverride: any = null,
  visibleOverride: boolean | null = null
): WindowContainer => {
  if (proto == null) {
    return null;
  }

  const containerOrder = nextSeq();
  const children = protoChildren
    .filter((it) => it != null)
    .map((it) => WindowContainer.childrenFromProto(it, isActivityInTree, nextSeq))
    .filter((it) => it != null);

  const identifier: any = identifierOverride ?? proto.identifier;
  const name: string = nameOverride ?? identifier?.title ?? '';
  const token: string = tokenOverride?.toString(16) ?? identifier?.hashCode?.toString(16) ?? '';

  const config = createConfigurationContainer(proto.configurationContainer);
  const entry = new WindowContainer(
    name,
    token,
    proto.orientation,
    proto.surfaceControl?.layerId ?? 0,
    visibleOverride ?? proto.visible,
    config,
    children,
    containerOrder
  );

  addAttributes(entry, proto);
  return entry;
};

function addAttributes(entry: WindowContainer, proto: any) {
  entry.proto = proto;
  entry.kind = entry.constructor.name;
  entry.shortName = shortenName(entry.name);
}

type WindowContainerChildType =
  | DisplayContent
  | DisplayArea
  | Task
  | TaskFragment
  | Activity
  | WindowToken
  | WindowState
  | WindowContainer;

WindowContainer.childrenFromProto = (
  proto: any,
  isActivityInTree: boolean,
  nextSeq: () => number
): WindowContainerChildType => {
  if (proto.displayContent !== null) {
    const windowContainer = WindowContainer.fromProto(
      /* proto */ proto.displayContent.rootDisplayArea.windowContainer,
      /* protoChildren */ proto.displayContent.rootDisplayArea.windowContainer?.children ?? [],
      /* isActivityInTree */ isActivityInTree,
      /* computedZ */ nextSeq,
      /* nameOverride */ proto.displayContent.displayInfo?.name ?? null
    );

    return DisplayContent.fromProto(windowContainer, proto.displayContent);
  }

  if (proto.displayArea !== null) {
    const windowContainer = WindowContainer.fromProto(
      /* proto */ proto.displayArea.windowContainer,
      /* protoChildren */ proto.displayArea.windowContainer?.children ?? [],
      /* isActivityInTree */ isActivityInTree,
      /* computedZ */ nextSeq,
      /* nameOverride */ proto.displayArea.name
    );

    return DisplayArea.fromProto(windowContainer, proto.displayArea);
  }

  if (proto.task !== null) {
    const windowContainerProto =
      proto.task.taskFragment?.windowContainer ?? proto.task.windowContainer;
    const windowContainer = WindowContainer.fromProto(
      /* proto */ windowContainerProto,
      /* protoChildren */ windowContainerProto?.children ?? [],
      /* isActivityInTree */ isActivityInTree,
      /* computedZ */ nextSeq
    );

    return Task.fromProto(windowContainer, proto.task);
  }

  if (proto.taskFragment !== null) {
    const windowContainer = WindowContainer.fromProto(
      /* proto */ proto.taskFragment.windowContainer,
      /* protoChildren */ proto.taskFragment.windowContainer?.children ?? [],
      /* isActivityInTree */ isActivityInTree,
      /* computedZ */ nextSeq
    );

    return TaskFragment.fromProto(windowContainer, proto.taskFragment);
  }

  if (proto.activity !== null) {
    const windowContainer = WindowContainer.fromProto(
      /* proto */ proto.activity.windowToken.windowContainer,
      /* protoChildren */ proto.activity.windowToken.windowContainer?.children ?? [],
      /* isActivityInTree */ true,
      /* computedZ */ nextSeq,
      /* nameOverride */ proto.activity.name,
      /* identifierOverride */ proto.activity.identifier
    );

    return Activity.fromProto(windowContainer, proto.activity);
  }

  if (proto.windowToken !== null) {
    const windowContainer = WindowContainer.fromProto(
      /* proto */ proto.windowToken.windowContainer,
      /* protoChildren */ proto.windowToken.windowContainer?.children ?? [],
      /* isActivityInTree */ isActivityInTree,
      /* computedZ */ nextSeq,
      /* nameOverride */ proto.windowToken.hashCode.toString(16),
      /* identifierOverride */ null,
      /* tokenOverride */ proto.windowToken.hashCode
    );

    return WindowToken.fromProto(windowContainer, proto.windowToken);
  }

  if (proto.window !== null) {
    const identifierName = WindowStateUtils.getIdentifier(proto.window);
    const name = WindowStateUtils.getName(identifierName);

    const windowContainer = WindowContainer.fromProto(
      /* proto */ proto.window.windowContainer,
      /* protoChildren */ proto.window.windowContainer?.children ?? [],
      /* isActivityInTree */ isActivityInTree,
      /* computedZ */ nextSeq,
      /* nameOverride */ name,
      /* identifierOverride */ proto.window.identifier
    );

    return WindowState.fromProto(windowContainer, proto.window, isActivityInTree);
  }

  if (proto.windowContainer !== null) {
    return WindowContainer.fromProto(proto.windowContainer, nextSeq);
  }
};

function createConfigurationContainer(proto: any): ConfigurationContainer {
  const entry = ConfigurationContainer.Companion.from(
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
  let windowConfiguration = null;

  if (proto != null && proto.windowConfiguration != null) {
    windowConfiguration = createWindowConfiguration(proto.windowConfiguration);
  }

  return Configuration.Companion.from(
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
  return WindowConfiguration.Companion.from(
    toRect(proto.appBounds),
    toRect(proto.bounds),
    toRect(proto.maxBounds),
    proto.windowingMode,
    proto.activityType
  );
}

export {WindowContainer};
