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

const commonDenylistProperties = [
  'prototype',
  'rootWindowContainer',
  'children',
];

export const WM_DENYLIST_PROPERTIES = new Map<WmProtoType, string[]>([
  [
    WmProtoType.WindowManagerService,
    commonDenylistProperties.concat(['windowContainer']),
  ],
  [WmProtoType.RootWindowContainer, commonDenylistProperties],
  [WmProtoType.WindowContainer, commonDenylistProperties],
  [
    WmProtoType.DisplayContent,
    commonDenylistProperties.concat(['windowContainer']),
  ],
  [WmProtoType.DisplayArea, commonDenylistProperties],
  [WmProtoType.Task, commonDenylistProperties.concat(['windowContainer'])],
  [WmProtoType.Activity, commonDenylistProperties],
  [WmProtoType.WindowToken, commonDenylistProperties],
  [WmProtoType.WindowState, commonDenylistProperties],
  [
    WmProtoType.TaskFragment,
    commonDenylistProperties.concat(['windowContainer']),
  ],
]);
