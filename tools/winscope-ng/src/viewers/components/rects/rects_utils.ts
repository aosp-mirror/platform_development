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
import { Point, Rectangle, RectMatrix, RectTransform } from "viewers/viewer_surface_flinger/ui_data";

export const RectsUtils = {
  multiplyMatrix(matrix:any, corner: Point): Point {
    if (!matrix) return corner;
    // |dsdx dsdy  tx|     | x |     |x*dsdx + y*dsdy + tx|
    // |dtdx dtdy  ty|  x  | y |  =  |x*dtdx + y*dtdy + ty|
    // |0    0     1 |     | 1 |     |         1          |
    return {
      x: matrix.dsdx * corner.x + matrix.dsdy * corner.y + matrix.tx,
      y: matrix.dtdx * corner.x + matrix.dtdy * corner.y + matrix.ty,
    };
  },

  transformRect(matrix: RectMatrix | RectTransform, rect:Rectangle): Rectangle {
    //          | dsdx   dsdy   tx |         | left   top     1    |
    // matrix = | dtdx   dtdy   ty |  rect = |   1     1      1    |
    //          |  0      0      1 |         |   1   right  bottom |
    const tl = this.multiplyMatrix(matrix, rect.topLeft);
    const tr = this.multiplyMatrix(matrix, {x:rect.bottomRight.x, y:rect.topLeft.y});
    const bl = this.multiplyMatrix(matrix, {x:rect.topLeft.x, y:rect.bottomRight.y});
    const br = this.multiplyMatrix(matrix, rect.bottomRight);

    const left = Math.min(tl.x, tr.x, bl.x, br.x);
    const top = Math.max(tl.y, tr.y, bl.y, br.y);
    const right = Math.max(tl.x, tr.x, bl.x, br.x);
    const bottom = Math.min(tl.y, tr.y, bl.y, br.y);

    const outrect: Rectangle = {
      topLeft: {x: left, y: top},
      bottomRight: {x: right, y: bottom},
      label: rect.label,
      transform: rect.transform,
      isVisible: rect.isVisible,
      isDisplay: rect.isDisplay,
      height: Math.abs(top - bottom),
      width: Math.abs(right - left),
      ref: rect.ref,
      id: rect.id,
      displayId: rect.displayId,
      isVirtual: rect.isVirtual
    };
    return outrect;
  }
};
