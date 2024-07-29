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

import {TransformMatrix} from 'common/geometry_types';
import {Rect} from 'common/rect';
import {Region} from 'common/region';
import {Item} from 'trace/item';

export class TraceRect extends Rect implements Item {
  constructor(
    x: number,
    y: number,
    w: number,
    h: number,
    readonly id: string,
    readonly name: string,
    readonly cornerRadius: number,
    readonly transform: TransformMatrix,
    readonly groupId: number,
    readonly isVisible: boolean,
    readonly isDisplay: boolean,
    readonly depth: number,
    readonly opacity: number | undefined,
    readonly isSpy: boolean,
    readonly fillRegion: Region | undefined,
  ) {
    super(x, y, w, h);
  }
}
