/*
 * Copyright 2024, The Android Open Source Project
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

import {Point3D} from 'common/geometry/point3d';
import {Rect3D} from 'common/geometry/rect3d';
import {TransformMatrix} from 'common/geometry/transform_matrix';
import {ColorType} from './color_type';

export interface UiRect3D extends Rect3D {
  id: string;
  topLeft: Point3D;
  bottomRight: Point3D;
  cornerRadius: number;
  darkFactor: number;
  colorType: ColorType;
  isClickable: boolean;
  transform: TransformMatrix;
  isOversized: boolean;
  fillRegion: Rect3D[] | undefined;
  isPinned: boolean;
}
