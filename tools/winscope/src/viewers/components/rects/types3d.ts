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

import {TransformMatrix} from 'common/geometry_types';

export enum ColorType {
  VISIBLE,
  VISIBLE_WITH_OPACITY,
  NOT_VISIBLE,
  HIGHLIGHTED,
  HAS_CONTENT,
  EMPTY,
  HAS_CONTENT_AND_OPACITY,
}

export enum ShadingMode {
  WIRE_FRAME = 'Wire frame',
  GRADIENT = 'Shaded by gradient',
  OPACITY = 'Shaded by opacity',
}

export class Distance2D {
  constructor(public dx: number, public dy: number) {}
}

export interface Box3D {
  width: number;
  height: number;
  depth: number;
  center: Point3D;
  diagonal: number;
}

export interface Rect3D {
  id: string;
  topLeft: Point3D;
  bottomRight: Point3D;
  cornerRadius: number;
  darkFactor: number;
  colorType: ColorType;
  isClickable: boolean;
  transform: TransformMatrix;
  isOversized: boolean;
}

export interface Point3D {
  x: number;
  y: number;
  z: number;
}

export interface Label3D {
  circle: Circle3D;
  linePoints: Point3D[];
  textCenter: Point3D;
  text: string;
  isHighlighted: boolean;
  rectId: string;
}

export interface Circle3D {
  radius: number;
  center: Point3D;
}

export interface Scene3D {
  boundingBox: Box3D;
  camera: Camera;
  rects: Rect3D[];
  labels: Label3D[];
}

export interface Camera {
  rotationAngleX: number;
  rotationAngleY: number;
  zoomFactor: number;
  panScreenDistance: Distance2D;
}
