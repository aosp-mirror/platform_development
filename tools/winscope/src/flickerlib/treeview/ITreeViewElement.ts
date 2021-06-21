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

import Chip from "./Chip"
import { TreeViewObject } from "./types"

export default interface ITreeViewElement {
  kind: String
  name: String
  shortName: String
  stableId: Number | String
  chips: Chip[]
  children: ITreeViewElement[]

  // This is used for compatibility with the "legacy" Winscope infrastructure
  // where a class object would cause things to not work properly so this should
  // return a raw javascript object with the relevant information.
  // IMPORTANT: The implementation of this function should always return the
  // same object every time it is called and not generate a new object.
  asRawTreeViewObject(): TreeViewObject
}