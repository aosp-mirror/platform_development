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

import {Box3D} from 'common/geometry/box3d';
import {Distance} from 'common/geometry/distance';
import {RectLabel} from './rect_label';
import {UiRect3D} from './ui_rect3d';

export interface Scene {
  boundingBox: Box3D;
  camera: Camera;
  rects: UiRect3D[];
  labels: RectLabel[];
}

interface Camera {
  rotationAngleX: number;
  rotationAngleY: number;
  zoomFactor: number;
  panScreenDistance: Distance;
}
