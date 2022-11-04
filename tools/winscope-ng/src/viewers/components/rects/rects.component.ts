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
import { Component, Input, OnChanges, OnDestroy, Inject, ElementRef, SimpleChanges, OnInit} from "@angular/core";
import { RectsUtils } from "viewers/components/rects/rects_utils";
import { Point, Rectangle, RectMatrix, RectTransform } from "viewers/common/rectangle";
import { CanvasGraphics } from "viewers/components/rects/canvas_graphics";
import * as THREE from "three";
import { ViewerEvents } from "viewers/common/viewer_events";

@Component({
  selector: "rects-view",
  template: `
    <div class="view-controls">
      <h2 class="mat-title">Layers</h2>
      <div class="top-view-controls">
        <mat-checkbox
          color="primary"
          [checked]="visibleView()"
          (change)="onChangeView($event.checked!)"
        >Only visible</mat-checkbox>
        <mat-checkbox
          color="primary"
          [disabled]="visibleView() || !hasVirtualDisplays"
          [checked]="showVirtualDisplays()"
          (change)="updateVirtualDisplays($event.checked!)"
        >Show virtual</mat-checkbox>
        <div class="right-btn-container">
          <button color="primary" mat-icon-button (click)="updateZoom(true)">
            <mat-icon aria-hidden="true">
              zoom_in
            </mat-icon>
          </button>
          <button color="primary" mat-icon-button (click)="updateZoom(false)">
            <mat-icon aria-hidden="true">
              zoom_out
            </mat-icon>
          </button>
          <button
            color="primary"
            mat-icon-button
            matTooltip="Restore camera settings"
            (click)="resetCamera()"
          >
            <mat-icon aria-hidden="true">
              restore
            </mat-icon>
          </button>
        </div>
      </div>
      <div class="slider-view-controls">
        <div class="slider" [class.rotation]="true">
          <p class="slider-label mat-body-2">Rotation</p>
          <mat-slider
            step="0.001"
            min="0"
            max="4"
            aria-label="units"
            [value]="xCameraPos()"
            (input)="updateRotation($event.value!)"
            color="primary"
          ></mat-slider>
        </div>
        <div class="slider" [class.spacing]="true">
          <p class="slider-label mat-body-2">Spacing</p>
          <mat-slider
            class="spacing-slider"
            step="0.001"
            min="0.1"
            max="0.4"
            aria-label="units"
            [value]="getLayerSeparation()"
            (input)="updateLayerSeparation($event.value!)"
            color="primary"
          ></mat-slider>
        </div>
      </div>
    </div>
    <mat-divider></mat-divider>
    <div class="rects-content">
      <div class="canvas-container">
        <canvas class="canvas-rects" (click)="onRectClick($event)" oncontextmenu="return false">
        </canvas>
        <div class="canvas-labels">
        </div>
      </div>
      <div *ngIf="displayIds.length > 1" class="display-button-container">
        <button
          *ngFor="let displayId of displayIds"
          color="primary"
          mat-raised-button
          (click)="changeDisplayId(displayId)"
        >{{displayId}}</button>
      </div>
    </div>
  `,
  styles: [
    `
      .view-controls {
        display: flex;
        flex-direction: column;
      }
      .top-view-controls, .slider-view-controls {
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
        column-gap: 10px;
        align-items: center;
        margin-bottom: 12px;
      }
      .right-btn-container {
        margin-left: auto;
      }
      .slider-view-controls {
        justify-content: space-between;
      }
      .slider {
        position: relative;
      }
      .slider-label {
        position: absolute;
        top: 0;
      }
      .rects-content {
        height: 100%;
        width: 100%;
        display: flex;
        flex-direction: column;
      }
      .canvas-container {
        height: 100%;
        width: 100%;
        position: relative;
      }
      .canvas-rects {
        position: absolute;
        top: 0;
        cursor: pointer;
      }
      .display-button-container {
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
        column-gap: 10px;
      }
      .canvas-labels {
        position: absolute;
        top: 0;
        width: 100%;
        height: 100%;
        pointer-events: none;
      }
    `
  ]
})

export class RectsComponent implements OnInit, OnChanges, OnDestroy {
  @Input() rects!: Rectangle[];
  @Input() forceRefresh = false;
  @Input() hasVirtualDisplays = false;
  @Input() displayIds: Array<number> = [];
  @Input() highlightedItems: Array<string> = [];
  rectsComponentInitialised = false;

  constructor(
    @Inject(ElementRef) private elementRef: ElementRef,
  ) {
    this.currentDisplayId = this.displayIds[0] ?? 0; // default stack id is usually zero
  }

  ngOnInit() {
    this.canvasContainer = this.elementRef.nativeElement.querySelector(".canvas-container")!;
    this.canvasRects =
      this.elementRef.nativeElement.querySelector(".canvas-rects")! as HTMLCanvasElement;
    this.canvasLabels = this.elementRef.nativeElement.querySelector(".canvas-labels")!;

    this.canvasGraphics = new CanvasGraphics(this.canvasRects, this.canvasLabels);

    window.addEventListener("resize", () => this.refreshCanvas());
    this.addContainerResizeListener();

    this.currentDisplayId = this.displayIds[0];
    this.canvasGraphics.updateHighlightedItems(this.highlightedItems);
    if (this.rects.length > 0) {
      this.formatAndDrawRects(true);
    }
    this.rectsComponentInitialised = true; // prevent ngOnChanges from being called before ngOnInit
  }

  ngOnChanges(changes: SimpleChanges) {
    if (!this.rectsComponentInitialised) {
      return; // ngOnInit not yet called
    }
    if (changes["displayIds"]) {
      if (!this.displayIds.includes(this.currentDisplayId)) {
        this.currentDisplayId = this.displayIds[0];
      }
    }
    if (changes["highlightedItems"]) {
      this.canvasGraphics!.updateHighlightedItems(this.highlightedItems);
    }
    if (this.rects.length > 0 || changes["forceRefresh"]?.currentValue) {
      this.formatAndDrawRects(changes["rects"] !== undefined);
    }
  }

  ngOnDestroy() {
    window.removeEventListener("resize", () => this.refreshCanvas());
    this.resizeObserver!.unobserve(this.canvasContainer!);
  }

  public onChangeView(visible: boolean) {
    this.canvasGraphics!.updateVisibleView(visible);
    this.refreshCanvas();
  }

  public visibleView() {
    return this.canvasGraphics!.getVisibleView();
  }

  public getLayerSeparation() {
    return this.canvasGraphics!.getLayerSeparation();
  }

  public updateLayerSeparation(sep: number) {
    this.canvasGraphics!.updateLayerSeparation(sep);
    this.refreshCanvas();
  }

  public updateRotation(rot: number) {
    this.canvasGraphics!.updateRotation(rot);
    this.refreshCanvas();
  }

  public resetCamera() {
    this.canvasGraphics!.resetCamera();
    this.refreshCanvas();
  }

  public updateZoom(zoom: boolean) {
    this.canvasGraphics!.updateZoom(zoom);
    this.refreshCanvas();
  }

  public updateVirtualDisplays(show: boolean) {
    this.canvasGraphics!.updateVirtualDisplays(show);
    this.refreshCanvas();
  }

  public xCameraPos() {
    return this.canvasGraphics!.getXCameraPos();
  }

  public showVirtualDisplays() {
    return this.canvasGraphics!.getShowVirtualDisplays();
  }

  public changeDisplayId(displayId: number) {
    this.currentDisplayId = displayId;
    this.refreshCanvas();
  }

  public onRectClick(event: MouseEvent) {
    this.setNormalisedMousePos(event);
    const raycaster = new THREE.Raycaster();
    raycaster.setFromCamera(this.mouse, this.canvasGraphics!.getCamera());
    // create an array containing all objects in the scene with which the ray intersects
    const intersects = raycaster.intersectObjects(this.canvasGraphics!.getTargetObjects());
    // if there is one (or more) intersections
    if (intersects.length > 0) {
      const id = intersects[0].object.name;
      this.updateHighlightedItems(id);
    }
  }

  public drawRects() {
    if (!this.canvasContainer || !this.canvasRects) {
      return;
    }
    this.refreshCanvas();
  }

  private formatAndDrawRects(rectsChanged: boolean) {
    //change in rects so they must undergo transformation and scaling before canvas refreshed
    this.displayRects = this.rects.filter(rect => rect.isDisplay);
    this.computeBounds();
    this.rects = this.rects.map(rect => {
      if (rectsChanged && rect.transform) {
        return RectsUtils.transformRect(rect.transform.matrix ??  rect.transform, rect);
      } else {
        return rect;
      }
    });
    this.scaleRects();
    this.drawRects();
  }

  private setNormalisedMousePos(event: MouseEvent) {
    event.preventDefault();
    const canvas = (event.target as Element);
    const canvasOffset = canvas.getBoundingClientRect();
    this.mouse.x = ((event.clientX-canvasOffset.left)/canvas.clientWidth) * 2 - 1;
    this.mouse.y = -((event.clientY-canvasOffset.top)/canvas.clientHeight) * 2 + 1;
    this.mouse.z = 0;
  }

  private updateHighlightedItems(newId: string) {
    const event: CustomEvent = new CustomEvent(
      ViewerEvents.HighlightedChange,
      {
        bubbles: true,
        detail: { id: newId }
      });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  private scaleRects() {
    this.rects = this.rects.map(rect => {
      rect.bottomRight = this.s(rect.bottomRight);
      rect.topLeft = this.s(rect.topLeft);
      rect.height = Math.abs(rect.topLeft.y - rect.bottomRight.y);
      rect.width = Math.abs(rect.bottomRight.x - rect.topLeft.x);
      const mat = this.getMatrix(rect);
      if (mat) {
        const newTranslation = this.s({x: mat.tx!, y: mat.ty!});
        mat.tx = newTranslation.x;
        mat.ty = newTranslation.y;
      }
      return rect;
    });
  }

  public refreshCanvas() {
    this.updateVariablesBeforeRefresh();
    this.canvasGraphics!.refreshCanvas();
  }

  private updateVariablesBeforeRefresh() {
    const rects = this.rects.filter(rect => rect.displayId === this.currentDisplayId);
    this.canvasGraphics!.updateRects(rects);
    const biggestX = Math.max(...this.rects.map(rect => rect.topLeft.x + rect.width/2));
    this.canvasGraphics!.updateIsLandscape(biggestX > this.s({x: this.boundsWidth, y:this.boundsHeight}).x/2);
  }

  private computeBounds(): any {
    this.boundsWidth = Math.max(...this.rects.map((rect) => {
      const mat = this.getMatrix(rect);
      if (mat) {
        return RectsUtils.transformRect(mat, rect).width;
      } else {
        return rect.width;
      }}));
    this.boundsHeight = Math.max(...this.rects.map((rect) => {
      const mat = this.getMatrix(rect);
      if (mat) {
        return RectsUtils.transformRect(mat, rect).height;
      } else {
        return rect.height;
      }}));

    if (this.displayRects.length > 0) {
      this.boundsWidth = Math.min(this.boundsWidth, this.maxWidth());
      this.boundsHeight = Math.min(this.boundsHeight, this.maxHeight());
    }
  }

  private maxWidth(): number {
    return Math.max(...this.displayRects.map(rect => rect.width)) * 1.2;
  }

  private maxHeight(): number {
    return Math.max(...this.displayRects.map(rect => rect.height)) * 1.2;
  }

  // scales coordinates to canvas
  private s(sourceCoordinates: Point): Point {
    let scale;
    if (this.boundsWidth < this.boundsHeight) {
      scale = this.canvasGraphics!.CAMERA_HALF_HEIGHT * 2 * 0.6 / this.boundsHeight;
    } else {
      scale = this.canvasGraphics!.CAMERA_HALF_WIDTH * 2 * 0.6 / this.boundsWidth;
    }
    return {
      x: sourceCoordinates.x * scale,
      y: sourceCoordinates.y * scale,
    };
  }

  private getMatrix(rect: Rectangle): RectTransform | RectMatrix | null {
    if (rect.transform) {
      let matrix: RectTransform | RectMatrix = rect.transform;
      if (rect.transform && rect.transform.matrix) {
        matrix = rect.transform.matrix;
      }
      return matrix;
    } else {
      return null;
    }
  }

  private addContainerResizeListener() {
    this.resizeObserver = new ResizeObserver((entries) => {
      if (entries[0].contentRect.height > 0) {
        this.refreshCanvas();
      }
    });
    this.resizeObserver.observe(this.canvasContainer!);
  }

  private canvasGraphics?: CanvasGraphics;
  private boundsWidth = 0;
  private boundsHeight = 0;
  private displayRects!: Rectangle[];
  private mouse = new THREE.Vector3(0, 0, 0);
  private currentDisplayId: number;
  private resizeObserver!: ResizeObserver;
  private canvasContainer!: Element;
  private canvasRects!: HTMLCanvasElement;
  private canvasLabels!: HTMLElement;
}
