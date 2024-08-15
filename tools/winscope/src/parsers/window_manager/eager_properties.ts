/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {ProtoType} from './proto_type';

const commonEagerProperties = ['identifier', 'id', 'name'];

export const EAGER_PROPERTIES = new Map<ProtoType, string[]>([
  [
    ProtoType.WindowManagerService,
    commonEagerProperties.concat([
      'focusedApp',
      'focusedWindow',
      'focusedDisplayId',
    ]),
  ],
  [ProtoType.RootWindowContainer, commonEagerProperties.concat('focusedApp')],
  [ProtoType.WindowContainer, commonEagerProperties.concat(['visible'])],
  [
    ProtoType.DisplayContent,
    commonEagerProperties.concat([
      'resumedActivity',
      'displayInfo',
      'inputMethodControlTarget',
      'inputMethodInputTarget',
      'inputMethodTarget',
      'imeInsetsSourceProvider',
      'windowContainer',
      'focusedApp',
    ]),
  ],
  [ProtoType.DisplayArea, commonEagerProperties],
  [
    ProtoType.Task,
    commonEagerProperties.concat([
      'displayId',
      'rootTaskId',
      'createdByOrganizer',
    ]),
  ],
  [ProtoType.Activity, commonEagerProperties.concat(['state', 'visible'])],
  [ProtoType.WindowToken, commonEagerProperties],
  [
    ProtoType.WindowState,
    commonEagerProperties.concat([
      'isVisible',
      'windowFrames',
      'containingFrame',
      'parentFrame',
      'displayId',
      'frame',
      'attributes',
      'alpha',
      'animatingExit',
    ]),
  ],
  [ProtoType.TaskFragment, commonEagerProperties.concat(['displayId'])],
]);
