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
import {Component, ElementRef, HostListener, Inject, Input, OnDestroy, OnInit} from '@angular/core';
import {RectDblClickDetail, ViewerEvents} from 'viewers/common/viewer_events';
import {UiRect} from 'viewers/components/rects/types2d';
import {Canvas} from './canvas';
import {Mapper3D} from './mapper3d';
import {Distance2D} from './types3d';

@Component({
  selector: 'rects-view',
  template: `
    <div class="view-controls">
      <h2 class="mat-title">{{ title }}</h2>
      <div class="top-view-controls">
        <mat-checkbox
          color="primary"
          [checked]="mapper3d.getShowOnlyVisibleMode()"
          (change)="onShowOnlyVisibleModeChange($event.checked!)"
          >Only visible
        </mat-checkbox>
        <mat-checkbox
          *ngIf="enableShowVirtualButton"
          color="primary"
          [disabled]="mapper3d.getShowOnlyVisibleMode()"
          [checked]="mapper3d.getShowVirtualMode()"
          (change)="onShowVirtualModeChange($event.checked!)"
          >Show virtual
        </mat-checkbox>
        <div class="right-btn-container">
          <button color="primary" mat-icon-button (click)="onZoomInClick()">
            <mat-icon aria-hidden="true"> zoom_in </mat-icon>
          </button>
          <button color="primary" mat-icon-button (click)="onZoomOutClick()">
            <mat-icon aria-hidden="true"> zoom_out </mat-icon>
          </button>
          <button
            color="primary"
            mat-icon-button
            matTooltip="Restore camera settings"
            (click)="resetCamera()">
            <mat-icon aria-hidden="true"> restore </mat-icon>
          </button>
        </div>
      </div>
      <div class="slider-view-controls">
        <div class="slider-container">
          <p class="slider-label mat-body-2">Rotation</p>
          <mat-slider
            class="slider-rotation"
            step="0.02"
            min="0"
            max="1"
            aria-label="units"
            [value]="mapper3d.getCameraRotationFactor()"
            (input)="onRotationSliderChange($event.value!)"
            color="primary"></mat-slider>
        </div>
        <div class="slider-container">
          <p class="slider-label mat-body-2">Spacing</p>
          <mat-slider
            class="slider-spacing"
            step="0.02"
            min="0.02"
            max="1"
            aria-label="units"
            [value]="mapper3d.getZSpacingFactor()"
            (input)="onSeparationSliderChange($event.value!)"
            color="primary"></mat-slider>
        </div>
      </div>
    </div>
    <mat-divider></mat-divider>
    <div class="rects-content">
      <div class="canvas-container">
        <canvas
          class="large-rects-canvas"
          (click)="onRectClick($event)"
          (dblclick)="onRectDblClick($event)"
          oncontextmenu="return false"></canvas>
        <div class="large-rects-labels"></div>
        <canvas
          class="mini-rects-canvas"
          (dblclick)="onMiniRectDblClick($event)"
          oncontextmenu="return false"></canvas>
      </div>
      <div *ngIf="internalDisplayIds.length > 1" class="display-button-container">
        <button
          *ngFor="let displayId of internalDisplayIds"
          color="primary"
          mat-raised-button
          (click)="onDisplayIdChange(displayId)">
          {{ displayId }}
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .view-controls {
        display: flex;
        flex-direction: column;
      }
      .top-view-controls,
      .slider-view-controls {
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
      .slider-container {
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
      .large-rects-canvas {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        cursor: pointer;
      }
      .large-rects-labels {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        pointer-events: none;
      }
      .display-button-container {
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
        column-gap: 10px;
      }
      .mini-rects-canvas {
        cursor: pointer;
        width: 30%;
        height: 30%;
        top: 16px;
        display: block;
        position: absolute;
        z-index: 1000;
      }
    `,
  ],
})
export class RectsComponent implements OnInit, OnDestroy {
  @Input() title = 'title';
  @Input() enableShowVirtualButton: boolean = true;
  @Input() zoomFactor: number = 1;
  @Input() set rects(rects: UiRect[]) {
    this.internalRects = rects;
    this.drawLargeRectsAndLabels();
  }
  @Input() set miniRects(rects: UiRect[] | undefined) {
    this.internalMiniRects = rects;
    this.drawMiniRects();
  }

  @Input() set displayIds(ids: number[]) {
    this.internalDisplayIds = ids;
    if (!this.internalDisplayIds.includes(this.mapper3d.getCurrentDisplayId())) {
      this.mapper3d.setCurrentDisplayId(this.internalDisplayIds[0]);
      this.drawLargeRectsAndLabels();
    }
  }

  @Input() set highlightedItem(stableId: string) {
    this.internalHighlightedItem = stableId;
    this.mapper3d.setHighlightedRectId(this.internalHighlightedItem);
    this.drawLargeRectsAndLabels();
  }

  private internalRects: UiRect[] = [];
  private internalMiniRects?: UiRect[];
  private internalDisplayIds: number[] = [];
  private internalHighlightedItem: string = '';

  private mapper3d: Mapper3D;
  private largeRectsCanvas?: Canvas;
  private miniRectsCanvas?: Canvas;
  private resizeObserver: ResizeObserver;
  private largeRectsCanvasElement?: HTMLCanvasElement;
  private miniRectsCanvasElement?: HTMLCanvasElement;
  private largeRectsLabelsElement?: HTMLElement;
  private mouseMoveListener = (event: MouseEvent) => this.onMouseMove(event);
  private mouseUpListener = (event: MouseEvent) => this.onMouseUp(event);

  constructor(@Inject(ElementRef) private elementRef: ElementRef) {
    this.mapper3d = new Mapper3D();
    this.resizeObserver = new ResizeObserver((entries) => {
      this.drawLargeRectsAndLabels();
    });
  }

  ngOnInit() {
    const canvasContainer = this.elementRef.nativeElement.querySelector('.canvas-container');
    this.resizeObserver.observe(canvasContainer);

    this.largeRectsCanvasElement = canvasContainer.querySelector(
      '.large-rects-canvas'
    )! as HTMLCanvasElement;
    this.largeRectsLabelsElement = canvasContainer.querySelector('.large-rects-labels');
    this.largeRectsCanvas = new Canvas(this.largeRectsCanvasElement, this.largeRectsLabelsElement!);
    this.largeRectsCanvasElement.addEventListener('mousedown', (event) =>
      this.onCanvasMouseDown(event)
    );

    this.mapper3d.setCurrentDisplayId(this.internalDisplayIds[0] ?? 0);
    this.mapper3d.increaseZoomFactor(this.zoomFactor - 1);
    this.drawLargeRectsAndLabels();

    this.miniRectsCanvasElement = canvasContainer.querySelector(
      '.mini-rects-canvas'
    )! as HTMLCanvasElement;
    this.miniRectsCanvas = new Canvas(this.miniRectsCanvasElement);
    if (this.miniRects) {
      this.drawMiniRects();
    }
  }

  ngOnDestroy() {
    this.resizeObserver?.disconnect();
  }

  onSeparationSliderChange(factor: number) {
    this.mapper3d.setZSpacingFactor(factor);
    this.drawLargeRectsAndLabels();
  }

  onRotationSliderChange(factor: number) {
    this.mapper3d.setCameraRotationFactor(factor);
    this.drawLargeRectsAndLabels();
  }

  resetCamera() {
    this.mapper3d.resetCamera();
    this.drawLargeRectsAndLabels();
  }

  @HostListener('wheel', ['$event'])
  onScroll(event: WheelEvent) {
    if (event.deltaY > 0) {
      this.doZoomOut();
    } else {
      this.doZoomIn();
    }
  }

  onCanvasMouseDown(event: MouseEvent) {
    document.addEventListener('mousemove', this.mouseMoveListener);
    document.addEventListener('mouseup', this.mouseUpListener);
  }

  onMouseMove(event: MouseEvent) {
    const distance = new Distance2D(event.movementX, event.movementY);
    this.mapper3d.addPanScreenDistance(distance);
    this.drawLargeRectsAndLabels();
  }

  onMouseUp(event: MouseEvent) {
    document.removeEventListener('mousemove', this.mouseMoveListener);
    document.removeEventListener('mouseup', this.mouseUpListener);
  }

  onZoomInClick() {
    this.doZoomIn();
  }

  onZoomOutClick() {
    this.doZoomOut();
  }

  onShowOnlyVisibleModeChange(enabled: boolean) {
    this.mapper3d.setShowOnlyVisibleMode(enabled);
    this.drawLargeRectsAndLabels();
  }

  onShowVirtualModeChange(enabled: boolean) {
    this.mapper3d.setShowVirtualMode(enabled);
    this.drawLargeRectsAndLabels();
  }

  onDisplayIdChange(id: number) {
    this.mapper3d.setCurrentDisplayId(id);
    this.drawLargeRectsAndLabels();
  }

  onRectClick(event: MouseEvent) {
    event.preventDefault();

    const id = this.findClickedRectId(event);
    if (id !== undefined) {
      this.notifyHighlightedItem(id);
    }
  }

  onRectDblClick(event: MouseEvent) {
    event.preventDefault();

    const clickedRectId = this.findClickedRectId(event);
    if (clickedRectId === undefined) {
      return;
    }

    this.elementRef.nativeElement.dispatchEvent(
      new CustomEvent(ViewerEvents.RectsDblClick, {
        bubbles: true,
        detail: new RectDblClickDetail(clickedRectId),
      })
    );
  }

  onMiniRectDblClick(event: MouseEvent) {
    event.preventDefault();

    this.elementRef.nativeElement.dispatchEvent(
      new CustomEvent(ViewerEvents.MiniRectsDblClick, {bubbles: true})
    );
  }

  private findClickedRectId(event: MouseEvent): string | undefined {
    const canvas = event.target as Element;
    const canvasOffset = canvas.getBoundingClientRect();

    const x = ((event.clientX - canvasOffset.left) / canvas.clientWidth) * 2 - 1;
    const y = -((event.clientY - canvasOffset.top) / canvas.clientHeight) * 2 + 1;
    const z = 0;

    return this.largeRectsCanvas?.getClickedRectId(x, y, z);
  }

  private doZoomIn() {
    this.mapper3d.increaseZoomFactor();
    this.drawLargeRectsAndLabels();
  }

  private doZoomOut() {
    this.mapper3d.decreaseZoomFactor();
    this.drawLargeRectsAndLabels();
  }

  private drawLargeRectsAndLabels() {
    // TODO(b/258593034): Re-create scene only when input rects change. With the other input events
    // (rotation, spacing, ...) we can just update the camera and/or update the mesh positions.
    // We'd probably need to get rid of the intermediate layer (Scene3D, Rect3D, ... types) and
    // work directly with three.js's meshes.
    this.mapper3d.setRects(this.internalRects);
    this.largeRectsCanvas?.draw(this.mapper3d.computeScene());
  }

  private drawMiniRects() {
    // TODO(b/258593034): Re-create scene only when input rects change. With the other input events
    // (rotation, spacing, ...) we can just update the camera and/or update the mesh positions.
    // We'd probably need to get rid of the intermediate layer (Scene3D, Rect3D, ... types) and
    // work directly with three.js's meshes.
    if (this.internalMiniRects) {
      this.mapper3d.setRects(this.internalMiniRects);
      this.mapper3d.decreaseZoomFactor(this.zoomFactor - 1);
      this.miniRectsCanvas?.draw(this.mapper3d.computeScene());
      this.mapper3d.increaseZoomFactor(this.zoomFactor - 1);

      // Mapper internally sets these values to 100%. They need to be reset afterwards
      if (this.miniRectsCanvasElement) {
        this.miniRectsCanvasElement.style.width = '25%';
        this.miniRectsCanvasElement.style.height = '25%';
      }
    }
  }

  private notifyHighlightedItem(id: string) {
    const event: CustomEvent = new CustomEvent(ViewerEvents.HighlightedChange, {
      bubbles: true,
      detail: {id},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }
}
