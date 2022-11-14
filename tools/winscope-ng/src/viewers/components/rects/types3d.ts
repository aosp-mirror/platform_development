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

enum ColorType {
  VISIBLE,
  NOT_VISIBLE,
  HIGHLIGHTED
}

interface Box3D {
  width: number,
  height: number,
  depth: number,
  center: Point3D;
  diagonal: number;
}

interface Rect3D {
  id: number;
  center: Point3D;
  width: number;
  height: number;
  darkFactor: number;
  colorType: ColorType;
  isClickable: boolean;
  transform: Transform3D;
}

interface Transform3D {
  dsdx: number;
  dsdy: number;
  tx: number;
  dtdx: number;
  dtdy: number;
  ty: number;
}

interface Point3D {
  x: number;
  y: number;
  z: number;
}

interface Label3D {
  circle: Circle3D;
  linePoints: Point3D[];
  textCenter: Point3D;
  text: string;
  isHighlighted: boolean;
  rectId: number;
}

interface Circle3D {
  radius: number;
  center: Point3D;
}

interface Scene3D {
  boundingBox: Box3D;
  camera: Camera;
  rects: Rect3D[];
  labels: Label3D[];
}

interface Camera {
  rotationFactor: number;
  zoomFactor: number;
}

export {Box3D, Circle3D, ColorType, Label3D, Point3D, Rect3D, Scene3D, Transform3D};
