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
import {
  Component,
  ElementRef,
  HostListener,
  Inject,
  Input,
  OnDestroy,
  OnInit,
  SimpleChanges,
} from '@angular/core';
import {assertDefined} from 'common/assert_utils';
import {PersistentStore} from 'common/persistent_store';
import {DisplayLayerStack} from 'trace/display_layer_stack';
import {DisplayIdentifier} from 'viewers/common/display_identifier';
import {RectDblClickDetail, ViewerEvents} from 'viewers/common/viewer_events';
import {UiRect} from 'viewers/components/rects/types2d';
import {Canvas} from './canvas';
import {Mapper3D} from './mapper3d';
import {Distance2D} from './types3d';

@Component({
  selector: 'rects-view',
  template: `
    <div class="view-controls view-header">
      <div class="title-zoom">
        <h2 class="mat-title">{{ title.toUpperCase() }}</h2>
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
      <div class="top-view-controls">
        <mat-checkbox
          color="primary"
          class="show-only-visible"
          [checked]="getShowOnlyVisibleMode()"
          (change)="onShowOnlyVisibleModeChange($event.checked!)"
          >Only visible
        </mat-checkbox>
        <div class="slider-view-controls">
          <div class="slider-container">
            <p class="slider-label mat-body-1">Rotation</p>
            <mat-slider
              class="slider-rotation"
              step="0.02"
              min="0"
              max="1"
              aria-label="units"
              [value]="mapper3d.getCameraRotationFactor()"
              (input)="onRotationSliderChange($event.value)"
              (focus)="$event.target.blur()"
              color="primary"></mat-slider>
          </div>
          <div class="slider-container">
            <p class="slider-label mat-body-1">Spacing</p>
            <mat-slider
              class="slider-spacing"
              step="0.02"
              min="0.02"
              max="1"
              aria-label="units"
              [value]="getZSpacingFactor()"
              (input)="onSeparationSliderChange($event.value)"
              (focus)="$event.target.blur()"
              color="primary"></mat-slider>
          </div>
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

      <mat-tab-group
        class="grouping-tabs"
        mat-align-tabs="start"
        *ngIf="internalDisplays.length > 0"
        dynamicHeight>
        <mat-tab label="Displays">
          <div class="display-button-container display-name-buttons">
            <button
              *ngFor="let display of internalDisplays"
              [color]="getDisplayButtonColor(display.groupId)"
              mat-raised-button
              (click)="onDisplayIdChange(display)">
              {{ display.name }}
            </button>
          </div>
        </mat-tab>
        <mat-tab *ngIf="isStackBased" label="Stacks">
          <div class="display-button-container stack-buttons">
            <button
              *ngFor="let groupId of internalGroupIds"
              [color]="getStackButtonColor(groupId)"
              [matTooltip]="getStackButtonTooltip(groupId)"
              mat-raised-button
              (click)="onGroupIdChange(groupId)">
              {{ getStackButtonLabel(groupId) }}
            </button>
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [
    `
      .mat-title {
        padding-top: 16px;
      }
      .title-zoom {
        display: flex;
        flex-direction: row;
        justify-content: space-between;
      }
      .view-controls {
        display: flex;
        flex-direction: column;
      }
      .right-btn-container {
        padding-top: 8px;
      }
      .top-view-controls,
      .slider-view-controls {
        display: flex;
        flex-direction: row;
        align-items: baseline;
      }
      .top-view-controls {
        justify-content: space-between;
      }
      .slider-view-controls {
        column-gap: 10px;
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
      .grouping-tabs {
        max-height: 120px;
      }
      .display-button-container {
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
        column-gap: 10px;
        padding-bottom: 5px;
        max-height: 72px;
        overflow-y: auto;
      }
      .display-button-container button {
        font-size: 12px;
        line-height: normal;
        height: 28px;
        margin-top: 5px;
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
  @Input() zoomFactor = 1;
  @Input() store?: PersistentStore;
  @Input() isStackBased = false;
  @Input() rects: UiRect[] = [];
  @Input() miniRects: UiRect[] | undefined;
  @Input() displays: DisplayIdentifier[] = [];
  @Input() highlightedItem = '';

  private stackSelected = false;
  private internalRects: UiRect[] = [];
  private internalMiniRects?: UiRect[];
  private storeKeyShowOnlyVisibleState = '';
  private storeKeyZSpacingFactor = '';
  private internalDisplays: DisplayIdentifier[] = [];
  private internalGroupIds = new Set<number>();
  private internalHighlightedItem = '';
  private currentDisplay: DisplayIdentifier | undefined;
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
    const canvasContainer =
      this.elementRef.nativeElement.querySelector('.canvas-container');
    this.resizeObserver.observe(canvasContainer);

    this.largeRectsCanvasElement = canvasContainer.querySelector(
      '.large-rects-canvas',
    )! as HTMLCanvasElement;
    this.largeRectsLabelsElement = canvasContainer.querySelector(
      '.large-rects-labels',
    );
    this.largeRectsCanvas = new Canvas(
      this.largeRectsCanvasElement,
      this.largeRectsLabelsElement!,
    );
    this.largeRectsCanvasElement.addEventListener('mousedown', (event) =>
      this.onCanvasMouseDown(event),
    );

    if (this.store) {
      this.updateControlsFromStore();
    }

    this.currentDisplay = this.internalDisplays[0] ?? undefined;
    this.mapper3d.setCurrentGroupId(this.currentDisplay?.groupId ?? 0);
    this.mapper3d.increaseZoomFactor(this.zoomFactor - 1);
    this.drawLargeRectsAndLabels();

    this.miniRectsCanvasElement = canvasContainer.querySelector(
      '.mini-rects-canvas',
    )! as HTMLCanvasElement;
    this.miniRectsCanvas = new Canvas(this.miniRectsCanvasElement);
    if (this.miniRects) {
      this.drawMiniRects();
    }
  }

  ngOnChanges(simpleChanges: SimpleChanges) {
    if (simpleChanges['highlightedItem']) {
      this.internalHighlightedItem =
        simpleChanges['highlightedItem'].currentValue;
      this.mapper3d.setHighlightedRectId(this.internalHighlightedItem);
      if (!(simpleChanges['rects'] || simpleChanges['displays'])) {
        this.drawLargeRectsAndLabels();
      }
    }
    if (simpleChanges['rects']) {
      this.internalRects = simpleChanges['rects'].currentValue;
      if (!simpleChanges['displays']) {
        this.drawLargeRectsAndLabels();
      }
    }
    if (simpleChanges['displays']) {
      this.onDisplaysChange(simpleChanges['displays'].currentValue);
      if (this.isStackBased) {
        this.internalGroupIds = new Set(
          this.internalDisplays.map((display) => display.groupId),
        );
      }
    }
    if (simpleChanges['miniRects']) {
      this.internalMiniRects = simpleChanges['miniRects'].currentValue;
      this.drawMiniRects();
    }
  }

  ngOnDestroy() {
    this.resizeObserver?.disconnect();
  }

  onDisplaysChange(displays: DisplayIdentifier[]) {
    this.internalDisplays = displays;

    if (displays.length === 0) {
      return;
    }

    if (!this.stackSelected) {
      const curr = this.internalDisplays.find(
        (display) => display.displayId === this.currentDisplay?.displayId,
      );
      if (curr) {
        this.updateCurrentDisplay(curr);
        return;
      }
    }

    const firstDisplayWithCurrentGroupId = this.internalDisplays.find(
      (display) => display.groupId === this.mapper3d.getCurrentGroupId(),
    );
    if (!firstDisplayWithCurrentGroupId) {
      this.updateCurrentDisplay(this.internalDisplays[0]);
      return;
    }

    const displayWithCurrentDisplayId = this.internalDisplays.find(
      (display) => display.displayId === this.currentDisplay?.displayId,
    );
    if (!displayWithCurrentDisplayId) {
      this.updateCurrentDisplay(firstDisplayWithCurrentGroupId);
      return;
    }

    if (
      displayWithCurrentDisplayId.groupId !== this.mapper3d.getCurrentGroupId()
    ) {
      this.updateCurrentDisplay(displayWithCurrentDisplayId);
      return;
    }
  }

  updateControlsFromStore() {
    this.storeKeyShowOnlyVisibleState = `rectsView.${this.title}.showOnlyVisibleState`;
    this.storeKeyZSpacingFactor = `rectsView.${this.title}.zSpacingFactor`;

    if (
      assertDefined(this.store).get(this.storeKeyShowOnlyVisibleState) ===
      'true'
    ) {
      this.mapper3d.setShowOnlyVisibleMode(true);
    }
    const storedZSpacingFactor = assertDefined(this.store).get(
      this.storeKeyZSpacingFactor,
    );
    if (storedZSpacingFactor !== undefined) {
      this.mapper3d.setZSpacingFactor(Number(storedZSpacingFactor));
    }
    this.drawLargeRectsAndLabels();
  }

  onSeparationSliderChange(factor: number) {
    this.store?.add(this.storeKeyZSpacingFactor, `${factor}`);
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
    if ((event.target as HTMLElement).className === 'large-rects-canvas') {
      if (event.deltaY > 0) {
        this.doZoomOut();
      } else {
        this.doZoomIn();
      }
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
    this.store?.add(this.storeKeyShowOnlyVisibleState, `${enabled}`);
    this.mapper3d.setShowOnlyVisibleMode(enabled);
    this.drawLargeRectsAndLabels();
  }

  onDisplayIdChange(display: DisplayIdentifier) {
    this.stackSelected = false;
    this.updateCurrentDisplay(display);
  }

  onGroupIdChange(groupId: number) {
    this.stackSelected = true;
    const displaysWithGroupId = this.getDisplaysWithGroupId(groupId);
    if (
      this.currentDisplay &&
      displaysWithGroupId.length > 0 &&
      !displaysWithGroupId.includes(this.currentDisplay)
    ) {
      this.updateCurrentDisplay(displaysWithGroupId[0]);
    }
  }

  getDisplayButtonColor(groupId: number): string {
    if (this.stackSelected) return 'secondary';
    return this.getButtonColor(groupId);
  }

  getStackButtonColor(groupId: number): string {
    if (!this.stackSelected) return 'secondary';
    return this.getButtonColor(groupId);
  }

  getStackButtonTooltip(groupId: number): string {
    if (groupId === DisplayLayerStack.INVALID_LAYER_STACK) {
      return 'Invalid layer stack - associated displays off';
    }
    return 'Associated displays on';
  }

  getStackButtonLabel(groupId: number): string {
    return (
      `Stack ${groupId}: ` +
      this.getDisplaysWithGroupId(groupId)
        .map((display) => display.name)
        .join(', ')
    );
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
      }),
    );
  }

  onMiniRectDblClick(event: MouseEvent) {
    event.preventDefault();

    this.elementRef.nativeElement.dispatchEvent(
      new CustomEvent(ViewerEvents.MiniRectsDblClick, {bubbles: true}),
    );
  }

  getShowOnlyVisibleMode(): boolean {
    return this.mapper3d.getShowOnlyVisibleMode();
  }

  getZSpacingFactor(): number {
    return this.mapper3d.getZSpacingFactor();
  }

  private getButtonColor(groupId: number) {
    if (!this.currentDisplay) return 'primary';
    return this.currentDisplay.groupId === groupId ? 'primary' : 'secondary';
  }

  private updateCurrentDisplay(display: DisplayIdentifier) {
    this.currentDisplay = display;
    this.mapper3d.setCurrentGroupId(display.groupId);
    this.drawLargeRectsAndLabels();
  }

  private getDisplaysWithGroupId(groupId: number): DisplayIdentifier[] {
    return assertDefined(
      this.internalDisplays.filter((display) => display.groupId === groupId),
    );
  }

  private findClickedRectId(event: MouseEvent): string | undefined {
    const canvas = event.target as Element;
    const canvasOffset = canvas.getBoundingClientRect();

    const x =
      ((event.clientX - canvasOffset.left) / canvas.clientWidth) * 2 - 1;
    const y =
      -((event.clientY - canvasOffset.top) / canvas.clientHeight) * 2 + 1;
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
