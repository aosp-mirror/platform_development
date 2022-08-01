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
import { Component, Input, OnChanges, OnDestroy, Inject, NgZone, ElementRef, SimpleChanges } from "@angular/core";
import { MatrixUtils } from "common/utils/matrix_utils";
import { Point, Rectangle, RectMatrix, RectTransform } from "ui_data/ui_data_surface_flinger";
import { interval, Subscription } from "rxjs";
import { CanvasService } from "./canvas.service";
import * as THREE from "three";

@Component({
  selector: "rects-view",
  template: `
    <mat-card-header class="view-controls">
        <mat-radio-group (change)="onChangeView($event.value)">
          <mat-radio-button class="visible-radio" [value]="true" [checked]="visibleView()">Visible</mat-radio-button>
          <mat-radio-button class="xray-radio" [value]="false" [checked]="!visibleView()">X-ray</mat-radio-button>
        </mat-radio-group>
        <mat-slider
          step="0.001"
          min="0.1"
          max="0.4"
          aria-label="units"
          [value]="getLayerSeparation()"
          (input)="canvasService.updateLayerSeparation($event.value!)"
        ></mat-slider>
        <mat-slider
          step="0.01"
          min="0.00"
          max="4"
          aria-label="units"
          [value]="xyCameraPos()"
          (input)="canvasService.updateRotation($event.value!)"
        ></mat-slider>
        <mat-checkbox
          [hidden]="visibleView()"
          class="rects-checkbox"
          [checked]="showVirtualDisplays()"
          (change)="canvasService.updateVirtualDisplays($event.checked!)"
        >Show virtual displays</mat-checkbox>
    </mat-card-header>
    <mat-card-content class="rects-content">
      <div class="canvas-container">
        <div class="zoom-container">
          <button id="zoom-btn" (click)="canvasService.updateZoom(true)">
            <mat-icon aria-hidden="true">
              zoom_in
            </mat-icon>
          </button>
          <button id="zoom-btn" (click)="canvasService.updateZoom(false)">
            <mat-icon aria-hidden="true">
              zoom_out
            </mat-icon>
          </button>
        </div>
        <canvas id="rects-canvas" (click)="onRectClick($event)">
        </canvas>
      </div>
    </mat-card-content>
  `,
  styles: [
    "@import 'https://fonts.googleapis.com/icon?family=Material+Icons';",
    ".rects-content {position: relative}",
    ".canvas-container {height: 40rem; width: 100%; position: relative}",
    "#rects-canvas {height: 40rem; width: 100%; cursor: pointer; position: absolute; top: 0px}",
    "#labels-canvas {height: 40rem; width: 100%; position: absolute; top: 0px}",
    ".view-controls {display: inline-block; position: relative; min-height: 72px}",
    ".zoom-container {position: absolute; top: 0px; z-index: 10}",
    "#zoom-btn {position:relative; display: block; background: none; border: none}",
    "mat-radio-button {font-size: 16px; font-weight: normal}",
    ".mat-radio-button, .mat-radio-button-frame {transform: scale(0.8);}",
    ".rects-checkbox {font-size: 14px; font-weight: normal}",
    "mat-icon {margin: 5px}",
    "mat-checkbox {margin-left: 5px;}",
    ".mat-checkbox .mat-checkbox-frame { transform: scale(0.7);}",
    ".mat-checkbox-checked .mat-checkbox-background {transform: scale(0.7);}",
    ".mat-checkbox-indeterminate .mat-checkbox-background {transform: scale(0.7);}",
  ]
})

export class RectsComponent implements OnChanges, OnDestroy {
  @Input()
    bounds: any;

  @Input()
    rects: Rectangle[];

  @Input()
    highlighted = "";

  constructor(
    @Inject(NgZone) private ngZone: NgZone,
    @Inject(ElementRef) private elementRef: ElementRef,
    @Inject(CanvasService) public canvasService: CanvasService
  ) {}

  ngOnDestroy() {
    if (this.canvasSubscription) {
      this.canvasSubscription.unsubscribe();
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (this.rects.length > 0) {
      //change in rects so they must undergo transformation and scaling before canvas refreshed
      this.canvasService.clearLabelElements();
      this.rects = this.rects.filter(rect => rect.isVisible || rect.isDisplay);
      this.displayRects = this.rects.filter(rect => rect.isDisplay);
      this.computeBounds();
      this.rects = this.rects.map(rect => {
        if (changes["rects"] && rect.transform) {
          return MatrixUtils.transformRect(rect.transform.matrix ??  rect.transform, rect);
        } else {
          return rect;
        }
      });
      this.scaleRects();
      this.drawRects();
    } else if (this.canvasSubscription) {
      this.canvasSubscription.unsubscribe();
    }
  }

  onRectClick(event:any) {
    this.setNormalisedMousePos(event);
    const raycaster = new THREE.Raycaster();
    raycaster.setFromCamera(this.mouse, this.canvasService.getCamera());
    // create an array containing all objects in the scene with which the ray intersects
    const intersects = raycaster.intersectObjects(this.canvasService.getTargetObjects());
    // if there is one (or more) intersections
    if (intersects.length > 0){
      if (this.highlighted === intersects[0].object.name) {
        this.highlighted = "";
        this.canvasService.updateHighlighted("");
      } else {
        this.highlighted = intersects[0].object.name;
        this.canvasService.updateHighlighted(intersects[0].object.name);
      }
      this.ngZone.run(() => {
        this.updateHighlightedRect();
      });
    }
  }

  setNormalisedMousePos(event:any) {
    event.preventDefault();
    const canvasOffset = event.target.getBoundingClientRect();
    this.mouse.x = ((event.clientX-canvasOffset.left)/event.target.clientWidth) * 2 - 1;
    this.mouse.y = -((event.clientY-canvasOffset.top)/event.target.clientHeight) * 2 + 1;
    this.mouse.z = 0;
  }

  updateHighlightedRect() {
    const event: CustomEvent = new CustomEvent("highlightedChange", {
      bubbles: true,
      detail: { layerId: this.highlighted }
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  drawRects() {
    if (this.canvasSubscription) {
      this.canvasSubscription.unsubscribe();
    }
    this.canvasService.initialise();
    this.canvasSubscription = this.drawRectsInterval.subscribe(() => {
      this.updateVariablesBeforeRefresh();
      this.canvasService.refreshCanvas();
    });
  }

  updateVariablesBeforeRefresh() {
    this.canvasService.updateRects(this.rects);
    const biggestX = Math.max(...this.rects.map(rect => rect.topLeft.x + rect.width/2));
    this.canvasService.updateIsLandscape(biggestX > this.s({x: this.boundsWidth, y:this.boundsHeight}).x/2);
  }

  onChangeView(visible: boolean) {
    this.canvasService.updateVisibleView(visible);
    this.canvasService.clearLabelElements();
  }

  scaleRects() {
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

  computeBounds(): any {
    if (this.bounds) {
      return this.bounds;
    }
    this.boundsWidth = Math.max(...this.rects.map((rect) => {
      const mat = this.getMatrix(rect);
      if (mat) {
        return MatrixUtils.transformRect(mat, rect).width;
      } else {
        return rect.width;
      }}));
    this.boundsHeight = Math.max(...this.rects.map((rect) => {
      const mat = this.getMatrix(rect);
      if (mat) {
        return MatrixUtils.transformRect(mat, rect).height;
      } else {
        return rect.height;
      }}));

    if (this.displayRects.length > 0) {
      this.boundsWidth = Math.min(this.boundsWidth, this.maxWidth());
      this.boundsHeight = Math.min(this.boundsHeight, this.maxHeight());
    }
  }

  maxWidth() {
    return Math.max(...this.displayRects.map(rect => rect.width)) * 1.2;
  }

  maxHeight() {
    return Math.max(...this.displayRects.map(rect => rect.height)) * 1.2;
  }

  // scales coordinates to canvas
  s(sourceCoordinates: Point) {
    let scale;
    if (this.boundsWidth < this.boundsHeight) {
      scale = this.canvasService.cameraHalfHeight*2 * 0.6 / this.boundsHeight;
    } else {
      scale = this.canvasService.cameraHalfWidth*2 * 0.6 / this.boundsWidth;
    }
    return {
      x: sourceCoordinates.x * scale,
      y: sourceCoordinates.y * scale,
    };
  }

  getMatrix(rect: Rectangle) {
    if (rect.transform) {
      let matrix: RectTransform | RectMatrix = rect.transform;
      if (rect.transform && rect.transform.matrix) {
        matrix = rect.transform.matrix;
      }
      return matrix;
    } else {
      return false;
    }
  }

  visibleView() {
    return this.canvasService.getVisibleView();
  }

  getLayerSeparation() {
    return this.canvasService.getLayerSeparation();
  }

  xyCameraPos() {
    return this.canvasService.getXyCameraPos();
  }

  showVirtualDisplays() {
    return this.canvasService.getShowVirtualDisplays();
  }

  private readonly _60fpsInterval = 16.66666666666667;
  private drawRectsInterval = interval(this._60fpsInterval);
  private boundsWidth = 0;
  private boundsHeight = 0;
  private displayRects: Rectangle[];
  private canvasSubscription: Subscription;
  private mouse = new THREE.Vector3(0, 0, 0);
}
