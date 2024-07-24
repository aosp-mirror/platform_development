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

import {WmProtoType} from './wm_proto_type';

const commonEagerProperties = ['identifier', 'id', 'name'];

export const WM_EAGER_PROPERTIES = new Map<WmProtoType, string[]>([
  [
    WmProtoType.WindowManagerService,
    commonEagerProperties.concat([
      'focusedApp',
      'focusedWindow',
      'focusedDisplayId',
    ]),
  ],
  [WmProtoType.RootWindowContainer, commonEagerProperties],
  [WmProtoType.WindowContainer, commonEagerProperties.concat(['visible'])],
  [
    WmProtoType.DisplayContent,
    commonEagerProperties.concat([
      'resumedActivity',
      'displayInfo',
      'inputMethodControlTarget',
      'inputMethodInputTarget',
      'inputMethodTarget',
      'imeInsetsSourceProvider',
      'windowContainer',
    ]),
  ],
  [WmProtoType.DisplayArea, commonEagerProperties],
  [
    WmProtoType.Task,
    commonEagerProperties.concat([
      'displayId',
      'rootTaskId',
      'createdByOrganizer',
    ]),
  ],
  [WmProtoType.Activity, commonEagerProperties.concat(['state', 'visible'])],
  [WmProtoType.WindowToken, commonEagerProperties],
  [
    WmProtoType.WindowState,
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
  [WmProtoType.TaskFragment, commonEagerProperties.concat(['displayId'])],
]);
