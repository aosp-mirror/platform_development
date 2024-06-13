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

import {Segment} from 'app/components/timeline/segment';
import {MathUtils} from 'three/src/Three';
import {DraggableCanvasObject} from './draggable_canvas_object';
import {MiniTimelineDrawer} from './mini_timeline_drawer';

export interface DrawConfig {
  fillStyle: string;
  fill: boolean;
}

export class DraggableCanvasObjectImpl implements DraggableCanvasObject {
  private draggingPosition: number | undefined;

  constructor(
    private drawer: MiniTimelineDrawer,
    private positionGetter: () => number,
    private definePathFunc: (
      ctx: CanvasRenderingContext2D,
      position: number,
    ) => void,
    private drawConfig: DrawConfig,
    private onDrag: (x: number) => void,
    private onDrop: (x: number) => void,
    private rangeGetter: () => Segment,
  ) {
    this.drawer.handler.registerDraggableObject(
      this,
      (x: number) => {
        this.draggingPosition = this.clampPositionToRange(x);
        this.onDrag(this.draggingPosition);
        this.drawer.draw();
      },
      (x: number) => {
        this.draggingPosition = undefined;
        this.onDrop(this.clampPositionToRange(x));
        this.drawer.draw();
      },
    );
  }

  get range(): Segment {
    return this.rangeGetter();
  }

  get position(): number {
    return this.draggingPosition !== undefined
      ? this.draggingPosition
      : this.positionGetter();
  }

  definePath(ctx: CanvasRenderingContext2D) {
    this.definePathFunc(ctx, this.position);
  }

  draw(ctx: CanvasRenderingContext2D) {
    this.doDraw(ctx);
    this.drawer.handler.notifyDrawnOnTop(this);
  }

  private doDraw(ctx: CanvasRenderingContext2D) {
    this.definePath(ctx);
    ctx.fillStyle = this.drawConfig.fillStyle;
    if (this.drawConfig.fill) {
      ctx.fill();
    }
  }

  private clampPositionToRange(x: number): number {
    return MathUtils.clamp(x, this.range.from, this.range.to);
  }
}
