/*
 * Copyright 2019, The Android Open Source Project
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

function multiplyVec2(matrix, x, y) {
  if (!matrix) return {x, y};
  // |dsdx dsdy  tx|     | x |
  // |dtdx dtdy  ty|  x  | y |
  // |0    0     1 |     | 1 |
  return {
    x: matrix.dsdx * x + matrix.dsdy * y + matrix.tx,
    y: matrix.dtdx * x + matrix.dtdy * y + matrix.ty,
  };
}

function multiplyRect(transform, rect) {
  let matrix = transform;
  if (transform && transform.matrix) {
    matrix = transform.matrix;
  }
  //          |dsdx dsdy  tx|         | left, top         |
  // matrix = |dtdx dtdy  ty|  rect = |                   |
  //          |0    0     1 |         |     right, bottom |

  const leftTop = multiplyVec2(matrix, rect.left, rect.top);
  const rightTop = multiplyVec2(matrix, rect.right, rect.top);
  const leftBottom = multiplyVec2(matrix, rect.left, rect.bottom);
  const rightBottom = multiplyVec2(matrix, rect.right, rect.bottom);

  const outrect = {};
  outrect.left = Math.min(leftTop.x, rightTop.x, leftBottom.x, rightBottom.x);
  outrect.top = Math.min(leftTop.y, rightTop.y, leftBottom.y, rightBottom.y);
  outrect.right = Math.max(leftTop.x, rightTop.x, leftBottom.x, rightBottom.x);
  outrect.bottom = Math.max(leftTop.y, rightTop.y, leftBottom.y,
      rightBottom.y);
  return outrect;
}

export {multiplyRect};
