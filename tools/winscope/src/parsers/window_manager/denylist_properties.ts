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

const commonDenylistProperties = [
  'prototype',
  'rootWindowContainer',
  'children',
];

export const DENYLIST_PROPERTIES = new Map<ProtoType, string[]>([
  [
    ProtoType.WindowManagerService,
    commonDenylistProperties.concat(['windowContainer']),
  ],
  [ProtoType.RootWindowContainer, commonDenylistProperties],
  [ProtoType.WindowContainer, commonDenylistProperties],
  [
    ProtoType.DisplayContent,
    commonDenylistProperties.concat(['windowContainer']),
  ],
  [ProtoType.DisplayArea, commonDenylistProperties],
  [ProtoType.Task, commonDenylistProperties.concat(['windowContainer'])],
  [ProtoType.Activity, commonDenylistProperties],
  [ProtoType.WindowToken, commonDenylistProperties],
  [ProtoType.WindowState, commonDenylistProperties],
  [
    ProtoType.TaskFragment,
    commonDenylistProperties.concat(['windowContainer']),
  ],
]);
