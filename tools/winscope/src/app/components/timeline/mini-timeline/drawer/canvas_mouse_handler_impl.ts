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

import {assertDefined} from 'common/assert_utils';
import {Point} from 'common/geometry_types';
import {
  CanvasMouseHandler,
  DragListener,
  DropListener,
} from './canvas_mouse_handler';
import {DraggableCanvasObject} from './draggable_canvas_object';
import {MiniTimelineDrawer} from './mini_timeline_drawer';

/**
 * Canvas mouse handling implementation
 * @docs-private
 */
export class CanvasMouseHandlerImpl implements CanvasMouseHandler {
  // Ordered top most element to bottom most
  private draggableObjects: DraggableCanvasObject[] = [];
  private draggingObject: DraggableCanvasObject | undefined = undefined;

  private onDrag = new Map<DraggableCanvasObject, DragListener>();
  private onDrop = new Map<DraggableCanvasObject, DropListener>();

  constructor(
    private drawer: MiniTimelineDrawer,
    private defaultCursor = 'auto',
    private onUnhandledMouseDown: (point: Point) => void = (point) => {},
  ) {
    this.drawer.canvas.addEventListener('mousemove', (event) => {
      this.handleMouseMove(event);
    });
    this.drawer.canvas.addEventListener('mousedown', (event) => {
      this.handleMouseDown(event);
    });
    this.drawer.canvas.addEventListener('mouseup', (event) => {
      this.handleMouseUp(event);
    });
    this.drawer.canvas.addEventListener('mouseout', (event) => {
      this.handleMouseUp(event);
    });
  }

  registerDraggableObject(
    draggableObject: DraggableCanvasObject,
    onDrag: DragListener,
    onDrop: DropListener,
  ) {
    this.onDrag.set(draggableObject, onDrag);
    this.onDrop.set(draggableObject, onDrop);
  }

  notifyDrawnOnTop(draggableObject: DraggableCanvasObject) {
    const foundIndex = this.draggableObjects.indexOf(draggableObject);
    if (foundIndex !== -1) {
      this.draggableObjects.splice(foundIndex, 1);
    }
    this.draggableObjects.unshift(draggableObject);
  }

  private handleMouseDown(e: MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    const mousePoint = this.getPos(e);

    const clickedObject = this.objectAt(mousePoint);
    if (clickedObject !== undefined) {
      this.draggingObject = clickedObject;
    } else {
      this.onUnhandledMouseDown(mousePoint);
    }
    this.updateCursor(mousePoint);
  }

  private handleMouseMove(e: MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    const mousePoint = this.getPos(e);

    if (this.draggingObject !== undefined) {
      const onDragCallback = this.onDrag.get(this.draggingObject);
      if (onDragCallback !== undefined) {
        onDragCallback(mousePoint.x, mousePoint.y);
      }
    }

    this.updateCursor(mousePoint);
  }

  private handleMouseUp(e: MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    const mousePoint = this.getPos(e);

    if (this.draggingObject !== undefined) {
      const onDropCallback = this.onDrop.get(this.draggingObject);
      if (onDropCallback !== undefined) {
        onDropCallback(mousePoint.x, mousePoint.y);
      }
    }

    this.draggingObject = undefined;
    this.updateCursor(mousePoint);
  }

  private getPos(e: MouseEvent): Point {
    let mouseX = e.offsetX;
    const mouseY = e.offsetY;

    if (mouseX < this.drawer.padding.left) {
      mouseX = this.drawer.padding.left;
    }

    if (mouseX > this.drawer.getWidth() - this.drawer.padding.right) {
      mouseX = this.drawer.getWidth() - this.drawer.padding.right;
    }

    return {x: mouseX, y: mouseY};
  }

  private updateCursor(mousePoint: Point) {
    const hoverObject = this.objectAt(mousePoint);
    if (hoverObject !== undefined) {
      if (hoverObject === this.draggingObject) {
        this.drawer.canvas.style.cursor = 'grabbing';
      } else {
        this.drawer.canvas.style.cursor = 'grab';
      }
    } else {
      this.drawer.canvas.style.cursor = this.defaultCursor;
    }
  }

  private objectAt(mousePoint: Point): DraggableCanvasObject | undefined {
    for (const object of this.draggableObjects) {
      const ctx = assertDefined(this.drawer.canvas.getContext('2d'));
      object.definePath(ctx);
      if (ctx.isPointInPath(mousePoint.x, mousePoint.y)) {
        return object;
      }
    }

    return undefined;
  }
}
