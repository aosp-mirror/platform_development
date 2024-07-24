/*
 * Copyright (C) 2022 The Android Open Source Project
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

import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

export interface UiDataMessage {
  readonly originalIndex: number;
  readonly text: string;
  readonly time: PropertyTreeNode;
  readonly tag: string;
  readonly level: string;
  readonly at: string;
}

export class UiData {
  constructor(
    public allLogLevels: string[],
    public allTags: string[],
    public allSourceFiles: string[],
    public messages: UiDataMessage[],
    public currentMessageIndex: undefined | number,
    public selectedMessageIndex: undefined | number,
  ) {}

  static EMPTY = new UiData([], [], [], [], undefined, undefined);
}
