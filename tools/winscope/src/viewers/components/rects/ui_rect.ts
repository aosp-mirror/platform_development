/*
 * Copyright (C) 2024, The Android Open Source Project
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

import {Rect} from 'common/geometry/rect';
import {Region} from 'common/geometry/region';
import {TransformMatrix} from 'common/geometry/transform_matrix';

export class UiRect extends Rect {
  constructor(
    x: number,
    y: number,
    w: number,
    h: number,
    readonly label: string,
    readonly isVisible: boolean,
    readonly isDisplay: boolean,
    readonly isActiveDisplay: boolean,
    readonly id: string,
    readonly groupId: number,
    readonly isClickable: boolean,
    readonly cornerRadius: number,
    readonly transform: TransformMatrix | undefined,
    readonly depth: number,
    readonly hasContent: boolean | undefined,
    readonly opacity: number | undefined,
    readonly fillRegion: Region | undefined,
  ) {
    super(x, y, w, h);
  }
}
