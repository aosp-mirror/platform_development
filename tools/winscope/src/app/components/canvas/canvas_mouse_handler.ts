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

import {CanvasDrawer} from './canvas_drawer';
import {DraggableCanvasObject} from './draggable_canvas_object';

export type DragListener = (x: number, y: number) => void;
export type DropListener = DragListener;

export class CanvasMouseHandler {
  // Ordered top most element to bottom most
  private draggableObjects: DraggableCanvasObject[] = [];
  private draggingObject: DraggableCanvasObject | undefined = undefined;

  private onDrag = new Map<DraggableCanvasObject, DragListener>();
  private onDrop = new Map<DraggableCanvasObject, DropListener>();

  constructor(
    private drawer: CanvasDrawer,
    private defaultCursor: string = 'auto',
    private onUnhandledMouseDown: (x: number, y: number) => void = (x, y) => {}
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
    onDrop: DropListener
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
    const {mouseX, mouseY} = this.getPos(e);

    const clickedObject = this.objectAt(mouseX, mouseY);
    if (clickedObject !== undefined) {
      this.draggingObject = clickedObject;
    } else {
      this.onUnhandledMouseDown(mouseX, mouseY);
    }
    this.updateCursor(mouseX, mouseY);
  }

  private handleMouseMove(e: MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    const {mouseX, mouseY} = this.getPos(e);

    if (this.draggingObject !== undefined) {
      const onDragCallback = this.onDrag.get(this.draggingObject);
      if (onDragCallback !== undefined) {
        onDragCallback(mouseX, mouseY);
      }
    }

    this.updateCursor(mouseX, mouseY);
  }

  private handleMouseUp(e: MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    const {mouseX, mouseY} = this.getPos(e);

    if (this.draggingObject !== undefined) {
      const onDropCallback = this.onDrop.get(this.draggingObject);
      if (onDropCallback !== undefined) {
        onDropCallback(mouseX, mouseY);
      }
    }

    this.draggingObject = undefined;
    this.updateCursor(mouseX, mouseY);
  }

  private getPos(e: MouseEvent) {
    let mouseX = e.offsetX;
    const mouseY = e.offsetY;

    if (mouseX < this.drawer.padding.left) {
      mouseX = this.drawer.padding.left;
    }

    if (mouseX > this.drawer.getWidth() - this.drawer.padding.right) {
      mouseX = this.drawer.getWidth() - this.drawer.padding.right;
    }

    return {mouseX, mouseY};
  }

  private updateCursor(mouseX: number, mouseY: number) {
    const hoverObject = this.objectAt(mouseX, mouseY);
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

  private objectAt(mouseX: number, mouseY: number): DraggableCanvasObject | undefined {
    for (const object of this.draggableObjects) {
      object.definePath(this.drawer.ctx);
      if (
        this.drawer.ctx.isPointInPath(
          mouseX * this.drawer.getXScale(),
          mouseY * this.drawer.getYScale()
        )
      ) {
        return object;
      }
    }

    return undefined;
  }
}
