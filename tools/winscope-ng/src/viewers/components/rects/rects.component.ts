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
    <mat-card-header class="view-controls">
      <div class="rects-title">
        <span>Layers</span>
      </div>
      <div class="top-view-controls">
        <div class="top-view-controls">
          <mat-checkbox
            class="rects-checkbox control-item"
            [checked]="visibleView()"
            (change)="onChangeView($event.checked!)"
          >Only visible</mat-checkbox>
          <mat-checkbox
            [disabled]="visibleView() || !hasVirtualDisplays"
            class="rects-checkbox control-item"
            [checked]="showVirtualDisplays()"
            (change)="updateVirtualDisplays($event.checked!)"
          >Show virtual</mat-checkbox>
          <div class="right-btn-container control-item">
            <button class="right-btn" (click)="updateZoom(true)">
              <mat-icon aria-hidden="true">
                zoom_in
              </mat-icon>
            </button>
            <button class="right-btn" (click)="updateZoom(false)">
              <mat-icon aria-hidden="true">
                zoom_out
              </mat-icon>
            </button>
            <button
              class="right-btn"
              (click)="resetCamera()"
              matTooltip="Restore camera settings"
            >
              <mat-icon aria-hidden="true">
                restore
              </mat-icon>
            </button>
          </div>
        </div>
      </div>
      <div class="slider-view-controls">
        <div class="slider" [class.rotation]="true">
          <span class="slider-label">Rotation</span>
          <mat-slider
            step="0.001"
            min="0"
            max="4"
            aria-label="units"
            [value]="xCameraPos()"
            (input)="updateRotation($event.value!)"
          ></mat-slider>
        </div>
        <div class="slider" [class.spacing]="true">
          <span class="slider-label">Spacing</span>
          <mat-slider
            class="spacing-slider"
            step="0.001"
            min="0.1"
            max="0.4"
            aria-label="units"
            [value]="getLayerSeparation()"
            (input)="updateLayerSeparation($event.value!)"
          ></mat-slider>
        </div>
      </div>
    </mat-card-header>
    <mat-card-content class="rects-content">
      <div class="canvas-container">
        <canvas class="rects-canvas" (click)="onRectClick($event)" oncontextmenu="return false">
        </canvas>
      </div>
      <div class="tabs" *ngIf="displayIds.length > 1">
        <button mat-raised-button *ngFor="let displayId of displayIds" (click)="changeDisplayId(displayId)">{{displayId}}</button>
      </div>
    </mat-card-content>
  `,
  styles: [
    `
      @import 'https://fonts.googleapis.com/icon?family=Material+Icons';

      :host /deep/ .mat-card-header-text {
        width: 100%;
        margin: 0;
      }
      .rects-title {
        font-size: 16px;
        font-weight: medium;
        font-family: inherit;
        width: 100%;
        margin-bottom: 12px;
      }
      .rects-content {
        position: relative;
      }
      .canvas-container {
        height: 40rem;
        width: 100%;
        position: relative;
      }
      .labels-canvas, .rects-canvas {
        height: 40rem;
        width: 100%;
        position: absolute;
        top: 0px;
      }
      .rects-canvas {
        cursor: pointer;
      }
      .view-controls {
        display: inline-block;
        position: relative;
        min-height: 4rem;
        width: 100%;
      }
      .slider-view-controls, .top-view-controls {
        display: inline-block;
        position: relative;
        height: 3rem;
        width: 100%;
      }
      .top-view-controls {
        vertical-align: middle;
      }
      .slider {
        display: inline-block;
      }
      .slider-label {
        position: absolute;
        top: 0;
      }
      .slider.spacing {
        float: right;
      }
      .slider span, .slider mat-slider {
        display: block;
        padding-left: 0px;
        padding-top: 0px;
        font-weight: bold;
      }
      .right-btn-container {
        position: relative;
        vertical-align: middle;
        float: right;
      }
      .right-btn {
        position: relative;
        display: inline-flex;
        background: none;
        border: none;
        padding: 0;
      }
      .rects-checkbox {
        font-size: 14px;
        font-weight: normal;
        margin-left: 5px;
      }
      mat-icon {
        margin: 5px
      }
      .mat-checkbox .mat-checkbox-frame, .mat-checkbox-checked .mat-checkbox-background, .mat-checkbox-indeterminate .mat-checkbox-background {
        transform: scale(0.7);
      }
      .control-item {
        position: relative;
        display: inline-block;
        vertical-align: middle;
        align-items: center;
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
  canvasInitialised = false;
  rectsComponentInitialised = false;

  constructor(
    @Inject(ElementRef) private elementRef: ElementRef,
  ) {
    this.canvasGraphics = new CanvasGraphics();
    this.currentDisplayId = this.displayIds[0] ?? 0; // default stack id is usually zero
  }

  ngOnInit() {
    this.canvas = this.elementRef.nativeElement.querySelector("canvas")! as HTMLCanvasElement;
    this.canvasContainer = this.elementRef.nativeElement.querySelector(".canvas-container")!;

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
      this.canvasGraphics.updateHighlightedItems(this.highlightedItems);
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
    this.canvasGraphics.updateVisibleView(visible);
    this.refreshCanvas();
  }

  public visibleView() {
    return this.canvasGraphics.getVisibleView();
  }

  public getLayerSeparation() {
    return this.canvasGraphics.getLayerSeparation();
  }

  public updateLayerSeparation(sep: number) {
    this.canvasGraphics.updateLayerSeparation(sep);
    this.refreshCanvas();
  }

  public updateRotation(rot: number) {
    this.canvasGraphics.updateRotation(rot);
    this.refreshCanvas();
  }

  public resetCamera() {
    this.canvasGraphics.resetCamera();
    this.refreshCanvas();
  }

  public updateZoom(zoom: boolean) {
    this.canvasGraphics.updateZoom(zoom);
    this.refreshCanvas();
  }

  public updateVirtualDisplays(show: boolean) {
    this.canvasGraphics.updateVirtualDisplays(show);
    this.refreshCanvas();
  }

  public xCameraPos() {
    return this.canvasGraphics.getXCameraPos();
  }

  public showVirtualDisplays() {
    return this.canvasGraphics.getShowVirtualDisplays();
  }

  public changeDisplayId(displayId: number) {
    this.currentDisplayId = displayId;
    this.refreshCanvas();
  }

  public onRectClick(event: MouseEvent) {
    this.setNormalisedMousePos(event);
    const raycaster = new THREE.Raycaster();
    raycaster.setFromCamera(this.mouse, this.canvasGraphics.getCamera());
    // create an array containing all objects in the scene with which the ray intersects
    const intersects = raycaster.intersectObjects(this.canvasGraphics.getTargetObjects());
    // if there is one (or more) intersections
    if (intersects.length > 0) {
      const id = intersects[0].object.name;
      this.updateHighlightedItems(id);
    }
  }

  public drawRects() {
    if (!this.canvasContainer || !this.canvas) {
      return;
    } else if (!this.canvasInitialised) {
      this.canvasGraphics.initialiseCanvas(this.canvas, this.canvasContainer);
      this.canvasInitialised = true;
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
    this.canvasGraphics.refreshCanvas();
  }

  private updateVariablesBeforeRefresh() {
    const rects = this.rects.filter(rect => rect.displayId === this.currentDisplayId);
    this.canvasGraphics.updateRects(rects);
    const biggestX = Math.max(...this.rects.map(rect => rect.topLeft.x + rect.width/2));
    this.canvasGraphics.updateIsLandscape(biggestX > this.s({x: this.boundsWidth, y:this.boundsHeight}).x/2);
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
      scale = this.canvasGraphics.CAMERA_HALF_HEIGHT * 2 * 0.6 / this.boundsHeight;
    } else {
      scale = this.canvasGraphics.CAMERA_HALF_WIDTH * 2 * 0.6 / this.boundsWidth;
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
      if (entries[0].contentRect.height > 0 && this.canvasInitialised) {
        this.refreshCanvas();
      }
    });
    this.resizeObserver.observe(this.canvasContainer!);
  }

  private canvasGraphics: CanvasGraphics;
  private boundsWidth = 0;
  private boundsHeight = 0;
  private displayRects!: Rectangle[];
  private mouse = new THREE.Vector3(0, 0, 0);
  private currentDisplayId: number;
  private resizeObserver!: ResizeObserver;
  private canvasContainer!: Element;
  private canvas!: HTMLCanvasElement;
}
