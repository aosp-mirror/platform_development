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

import {Point} from 'common/geometry_types';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

export class Rect {
  constructor(
    readonly x: number,
    readonly y: number,
    readonly w: number,
    readonly h: number,
  ) {}

  static from(node: PropertyTreeNode): Rect {
    const left = node.getChildByName('left')?.getValue() ?? 0;
    const top = node.getChildByName('top')?.getValue() ?? 0;
    const right = node.getChildByName('right')?.getValue() ?? 0;
    const bottom = node.getChildByName('bottom')?.getValue() ?? 0;
    return new Rect(left, top, right - left, bottom - top);
  }

  isAlmostEqual(other: Rect, eps: number): boolean {
    const isClose = (a: number, b: number) => Math.abs(a - b) <= eps;
    return (
      isClose(this.x, other.x) &&
      isClose(this.y, other.y) &&
      isClose(this.w, other.w) &&
      isClose(this.h, other.h)
    );
  }

  containsPoint(point: Point): boolean {
    return (
      this.x <= point.x &&
      point.x <= this.x + this.w &&
      this.y <= point.y &&
      point.y <= this.y + this.h
    );
  }

  cropRect(other: Rect): Rect {
    const maxLeft = Math.max(this.x, other.x);
    const minRight = Math.min(this.x + this.w, other.x + other.w);
    const maxTop = Math.max(this.y, other.y);
    const minBottom = Math.min(this.y + this.h, other.y + other.h);
    return new Rect(maxLeft, maxTop, minRight - maxLeft, minBottom - maxTop);
  }

  containsRect(other: Rect): boolean {
    return (
      this.w > 0 &&
      this.h > 0 &&
      this.x <= other.x &&
      this.y <= other.y &&
      this.x + this.w >= other.x + other.w &&
      this.y + this.h >= other.y + other.h
    );
  }

  intersectsRect(other: Rect): boolean {
    if (
      this.x < other.x + other.w &&
      other.x < this.x + this.w &&
      this.y <= other.y + other.h &&
      other.y <= this.y + this.h
    ) {
      let [x, y, w, h] = [this.x, this.y, this.w, this.h];

      if (this.x < other.x) {
        x = other.x;
      }
      if (this.y < other.y) {
        y = other.y;
      }
      if (this.x + this.w > other.x + other.w) {
        w = other.w;
      }
      if (this.y + this.h > other.y + other.h) {
        h = other.h;
      }

      return !new Rect(x, y, w, h).isEmpty();
    }

    return false;
  }

  isEmpty(): boolean {
    const [x, y, w, h] = [this.x, this.y, this.w, this.h];
    const nullValuePresent =
      x === -1 || y === -1 || x + w === -1 || y + h === -1;
    const nullHeightOrWidth = w <= 0 || h <= 0;
    return nullValuePresent || nullHeightOrWidth;
  }
}
