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
import {Component, Input, OnDestroy, Inject, ElementRef, SimpleChanges, OnInit, HostListener} from "@angular/core";
import {Rectangle} from "viewers/common/rectangle";
import {Canvas} from "./canvas";
import {ViewerEvents} from "viewers/common/viewer_events";
import {Mapper3D} from "./mapper3d";

@Component({
  selector: "rects-view",
  template: `
    <div class="view-controls">
      <h2 class="mat-title">{{title}}</h2>
      <div class="top-view-controls">
        <mat-checkbox
            color="primary"
            [checked]="mapper3d.getShowOnlyVisibleMode()"
            (change)="onShowOnlyVisibleModeChange($event.checked!)"
        >Only visible
        </mat-checkbox>
        <mat-checkbox
            color="primary"
            [disabled]="mapper3d.getShowOnlyVisibleMode()"
            [checked]="mapper3d.getShowVirtualMode()"
            (change)="onShowVirtualModeChange($event.checked!)"
        >Show virtual
        </mat-checkbox>
        <div class="right-btn-container">
          <button color="primary" mat-icon-button (click)="onZoomInClick()">
            <mat-icon aria-hidden="true">
              zoom_in
            </mat-icon>
          </button>
          <button color="primary" mat-icon-button (click)="onZoomOutClick()">
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
              color="primary"
          ></mat-slider>
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
              color="primary"
          ></mat-slider>
        </div>
      </div>
    </div>
    <mat-divider></mat-divider>
    <div class="rects-content">
      <div class="canvas-container">
        <canvas class="canvas-rects" (click)="onRectClick($event)"
                oncontextmenu="return false">
        </canvas>
        <div class="canvas-labels">
        </div>
      </div>
      <div *ngIf="wrappedDisplayIds.length > 1"
           class="display-button-container">
        <button
            *ngFor="let displayId of wrappedDisplayIds"
            color="primary"
            mat-raised-button
            (click)="onDisplayIdChange(displayId)"
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
      .canvas-rects {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        cursor: pointer;
      }
      .canvas-labels {
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
    `
  ]
})

export class RectsComponent implements OnInit, OnDestroy {
  @Input() title = "title";
  @Input() set rects(rects: Rectangle[]) {
    this.wrappedRects = rects;
    this.drawScene();
  }

  @Input() set displayIds(ids: number[]) {
    this.wrappedDisplayIds = ids;
    if (!this.wrappedDisplayIds.includes(this.mapper3d.getCurrentDisplayId())) {
      this.mapper3d.setCurrentDisplayId(this.wrappedDisplayIds[0]);
      this.drawScene();
    }
  }

  @Input() set highlightedItems(ids: string[]) {
    this.wrappedHighlightedItems = ids.map(id => Number(id));
    this.mapper3d.setHighlightedRectIds(this.wrappedHighlightedItems);
    this.drawScene();
  }

  private wrappedRects: Rectangle[] = [];
  private wrappedDisplayIds: number[] = [];
  private wrappedHighlightedItems: number[] = [];

  private mapper3d: Mapper3D;
  private canvas?: Canvas;
  private resizeObserver: ResizeObserver;
  private canvasRects?: HTMLCanvasElement;
  private canvasLabels?: HTMLElement;

  constructor(
    @Inject(ElementRef) private elementRef: ElementRef,
  ) {
    this.mapper3d = new Mapper3D();
    this.resizeObserver = new ResizeObserver((entries) => {
      this.drawScene();
    });
  }

  ngOnInit() {
    const canvasContainer = this.elementRef.nativeElement.querySelector(".canvas-container");
    this.resizeObserver.observe(canvasContainer);

    this.canvasRects = canvasContainer.querySelector(".canvas-rects")! as HTMLCanvasElement;
    this.canvasLabels = canvasContainer.querySelector(".canvas-labels");
    this.canvas = new Canvas(this.canvasRects, this.canvasLabels!);

    this.mapper3d.setCurrentDisplayId(this.wrappedDisplayIds[0] ?? 0);
    this.drawScene();
  }

  ngOnDestroy() {
    this.resizeObserver?.disconnect();
  }

  public onSeparationSliderChange(factor: number) {
    this.mapper3d.setZSpacingFactor(factor);
    this.drawScene();
  }

  public onRotationSliderChange(factor: number) {
    this.mapper3d.setCameraRotationFactor(factor);
    this.drawScene();
  }

  public resetCamera() {
    this.mapper3d.resetCamera();
    this.drawScene();
  }

  @HostListener("wheel", ["$event"])
  public onScroll(event: WheelEvent) {
    if (event.deltaY > 0) {
      this.doZoomOut();
    } else {
      this.doZoomIn();
    }
  }

  public onZoomInClick() {
    this.doZoomIn();
  }

  public onZoomOutClick() {
    this.doZoomOut();
  }

  public onShowOnlyVisibleModeChange(enabled: boolean) {
    this.mapper3d.setShowOnlyVisibleMode(enabled);
    this.drawScene();
  }

  public onShowVirtualModeChange(enabled: boolean) {
    this.mapper3d.setShowVirtualMode(enabled);
    this.drawScene();
  }

  public onDisplayIdChange(id: number) {
    this.mapper3d.setCurrentDisplayId(id);
    this.drawScene();
  }

  public onRectClick(event: MouseEvent) {
    event.preventDefault();

    const canvas = (event.target as Element);
    const canvasOffset = canvas.getBoundingClientRect();

    const x = ((event.clientX-canvasOffset.left)/canvas.clientWidth) * 2 - 1;
    const y = -((event.clientY-canvasOffset.top)/canvas.clientHeight) * 2 + 1;
    const z = 0;

    const id = this.canvas?.getClickedRectId(x, y, z);
    if (id !== undefined) {
      this.notifyHighlightedItem(id);
    }
  }

  private doZoomIn() {
    this.mapper3d.increaseZoomFactor();
    this.drawScene();
  }

  private doZoomOut() {
    this.mapper3d.decreaseZoomFactor();
    this.drawScene();
  }

  private drawScene() {
    // TODO: Re-create scene only when input rects change. With the other input events
    //  (rotation, spacing, ...) we can just update the camera and/or update the mesh positions.
    //  We'd probably need to get rid of the intermediate layer (Scene3D, Rect3D, ... types) and
    //  work directly with three.js's meshes.
    this.mapper3d.setRects(this.wrappedRects);
    this.canvas?.draw(this.mapper3d.computeScene());
  }

  private notifyHighlightedItem(id: string) {
    const event: CustomEvent = new CustomEvent(
      ViewerEvents.HighlightedChange,
      {
        bubbles: true,
        detail: { id: id }
      });
    this.elementRef.nativeElement.dispatchEvent(event);
  }
}
