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
  SimpleChange,
  SimpleChanges,
} from '@angular/core';
import {CanColor} from '@angular/material/core';
import {MatIconRegistry} from '@angular/material/icon';
import {DomSanitizer} from '@angular/platform-browser';
import {assertDefined} from 'common/assert_utils';
import {PersistentStore} from 'common/persistent_store';
import {UrlUtils} from 'common/url_utils';
import {Analytics} from 'logging/analytics';
import {DisplayLayerStack} from 'trace/display_layer_stack';
import {DisplayIdentifier} from 'viewers/common/display_identifier';
import {RectDblClickDetail, ViewerEvents} from 'viewers/common/viewer_events';
import {UiRect} from 'viewers/components/rects/types2d';
import {multlineTooltip} from 'viewers/components/styles/tooltip.styles';
import {Canvas} from './canvas';
import {Mapper3D} from './mapper3d';
import {Distance2D, ShadingMode} from './types3d';

@Component({
  selector: 'rects-view',
  template: `
    <div class="view-controls view-header">
      <div class="title-zoom">
        <h2 class="mat-title">{{ title.toUpperCase() }}</h2>
        <div class="right-btn-container">
          <button
            color="accent"
            class="shading-mode"
            (mouseenter)="onInteractionStart(shadingModeButton)"
            (mouseleave)="onInteractionEnd(shadingModeButton)"
            mat-icon-button
            [matTooltip]="getShadingMode()"
            [disabled]="shadingModes.length < 2"
            (click)="onShadingModeButtonClicked()" #shadingModeButton>
            <mat-icon *ngIf="mapper3d.isWireFrame()" class="material-symbols-outlined" aria-hidden="true"> deployed_code </mat-icon>
            <mat-icon *ngIf="mapper3d.isShadedByGradient()" svgIcon="cube_partial_shade"></mat-icon>
            <mat-icon *ngIf="mapper3d.isShadedByOpacity()" svgIcon="cube_full_shade"></mat-icon>
          </button>
          <button
            color="accent"
            (mouseenter)="onInteractionStart(zoomInButton)"
            (mouseleave)="onInteractionEnd(zoomInButton)"
            mat-icon-button
            (click)="onZoomInClick()" #zoomInButton>
            <mat-icon aria-hidden="true"> zoom_in </mat-icon>
          </button>
          <button
            color="accent"
            (mouseenter)="onInteractionStart(zoomOutButton)"
            (mouseleave)="onInteractionEnd(zoomOutButton)"
            mat-icon-button
            (click)="onZoomOutClick()" #zoomOutButton>
            <mat-icon aria-hidden="true"> zoom_out </mat-icon>
          </button>
          <button
            color="accent"
            (mouseenter)="onInteractionStart(resetZoomButton)"
            (mouseleave)="onInteractionEnd(resetZoomButton)"
            mat-icon-button
            matTooltip="Restore camera settings"
            (click)="resetCamera()" #resetZoomButton>
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
              color="accent"
              (mousedown)="onInteractionStart(rotationSlider)"
              (mouseup)="onInteractionEnd(rotationSlider)" #rotationSlider></mat-slider>
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
              color="accent"
              (mousedown)="onInteractionStart(spacingSlider)"
              (mouseup)="onInteractionEnd(spacingSlider)" #spacingSlider></mat-slider>
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
        (selectedTabChange)="blurTab()"
        dynamicHeight>
        <mat-tab [label]="groupLabel">
          <div class="display-button-container display-name-buttons">
            <button
              #tooltip="matTooltip"
              *ngFor="let display of internalDisplays"
              [color]="getDisplayButtonColor(display.groupId)"
              [matTooltip]="getDisplayButtonTooltip(display, displayButtonText)"
              [matTooltipDisabled]="shouldDisableTooltip(displayButtonText)"
              matTooltipClass="multline-tooltip"
              (mouseenter)="shouldDisableTooltip(displayButtonText) ? tooltip.disabled = true : tooltip.disabled = false"
              (mouseleave)="tooltip.disabled = true"
              mat-raised-button
              (click)="onDisplayIdChange(display)">
              <span #displayButtonText> {{ display.name }} </span>
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
        max-height: 160px;
      }
      .display-button-container {
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
        column-gap: 10px;
        padding-bottom: 5px;
        max-height: 108px;
        overflow-y: auto;
      }
      .display-button-container button {
        font-size: 12px;
        line-height: normal;
        height: 28px;
        margin-top: 5px;
        max-width: 200px;
        overflow-x: hidden;
        text-overflow: ellipsis;
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
    multlineTooltip,
  ],
})
export class RectsComponent implements OnInit, OnDestroy {
  @Input() title = 'title';
  @Input() zoomFactor = 1;
  @Input() store?: PersistentStore;
  @Input() rects: UiRect[] = [];
  @Input() miniRects: UiRect[] | undefined;
  @Input() displays: DisplayIdentifier[] = [];
  @Input() highlightedItem = '';
  @Input() groupLabel = 'Displays';
  @Input() isStackBased = false;
  @Input() shadingModes: ShadingMode[] = [ShadingMode.GRADIENT];

  private internalRects: UiRect[] = [];
  private internalMiniRects?: UiRect[];
  private storeKeyShowOnlyVisibleState = '';
  private storeKeyZSpacingFactor = '';
  private storeKeyShadingMode = '';
  private internalDisplays: DisplayIdentifier[] = [];
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

  private static readonly ZOOM_SCROLL_RATIO = 0.3;

  constructor(
    @Inject(ElementRef) private elementRef: ElementRef,
    @Inject(MatIconRegistry) private matIconRegistry: MatIconRegistry,
    @Inject(DomSanitizer) private domSanitizer: DomSanitizer,
  ) {
    this.mapper3d = new Mapper3D();
    this.resizeObserver = new ResizeObserver((entries) => {
      this.drawLargeRectsAndLabels();
    });
    this.matIconRegistry.addSvgIcon(
      'cube_full_shade',
      this.domSanitizer.bypassSecurityTrustResourceUrl(
        UrlUtils.getRootUrl() + 'cube_full_shade.svg',
      ),
    );
    this.matIconRegistry.addSvgIcon(
      'cube_partial_shade',
      this.domSanitizer.bypassSecurityTrustResourceUrl(
        UrlUtils.getRootUrl() + 'cube_partial_shade.svg',
      ),
    );
  }

  ngOnInit() {
    this.mapper3d.setAllowedShadingModes(this.shadingModes);

    const canvasContainer: HTMLElement =
      this.elementRef.nativeElement.querySelector('.canvas-container');
    this.resizeObserver.observe(canvasContainer);

    const isDarkMode = () => this.store?.get('dark-mode') === 'true';

    this.largeRectsCanvasElement = canvasContainer.querySelector(
      '.large-rects-canvas',
    )! as HTMLCanvasElement;
    this.largeRectsLabelsElement = assertDefined(
      canvasContainer.querySelector('.large-rects-labels'),
    ) as HTMLElement;
    this.largeRectsCanvas = new Canvas(
      this.largeRectsCanvasElement,
      this.largeRectsLabelsElement,
      isDarkMode,
    );
    this.largeRectsCanvasElement.addEventListener('mousedown', (event) =>
      this.onCanvasMouseDown(event),
    );

    if (this.store) {
      this.updateControlsFromStore();
    }

    this.currentDisplay =
      this.internalDisplays.length > 0
        ? this.getFirstDisplayWithRectsOrFirstDisplay(this.internalDisplays)
        : undefined;
    this.mapper3d.increaseZoomFactor(this.zoomFactor - 1);
    this.drawLargeRectsAndLabels();

    this.miniRectsCanvasElement = canvasContainer.querySelector(
      '.mini-rects-canvas',
    )! as HTMLCanvasElement;
    this.miniRectsCanvas = new Canvas(
      this.miniRectsCanvasElement,
      undefined,
      isDarkMode,
    );
    if (this.miniRects && this.miniRects.length > 0) {
      this.drawMiniRects();
    }
  }

  blurTab() {
    (document.activeElement as HTMLElement).blur();
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
    let displayChange = false;
    if (simpleChanges['displays']) {
      const curr: DisplayIdentifier[] = simpleChanges['displays'].currentValue;
      const prev: DisplayIdentifier[] | null =
        simpleChanges['displays'].previousValue;
      displayChange =
        curr.length > 0 &&
        !curr.every((d, index) => d.displayId === prev?.at(index)?.displayId);
    }
    if (simpleChanges['rects']) {
      this.internalRects = simpleChanges['rects'].currentValue;
      if (!displayChange) {
        this.drawLargeRectsAndLabels();
      }
    }
    if (displayChange) {
      this.onDisplaysChange(simpleChanges['displays']);
    }
    if (simpleChanges['miniRects']) {
      this.internalMiniRects = simpleChanges['miniRects'].currentValue;
      this.drawMiniRects();
    }
  }

  ngOnDestroy() {
    this.resizeObserver?.disconnect();
  }

  onDisplaysChange(change: SimpleChange) {
    const displays = change.currentValue;
    this.internalDisplays = displays;

    if (displays.length === 0) {
      return;
    }

    if (change.firstChange) {
      this.updateCurrentDisplay(
        this.getFirstDisplayWithRectsOrFirstDisplay(this.internalDisplays),
      );
      return;
    }

    const curr = this.internalDisplays.find(
      (display) => display.displayId === this.currentDisplay?.displayId,
    );
    if (curr) {
      this.updateCurrentDisplay(curr);
      return;
    }

    const displaysWithCurrentGroupId = this.internalDisplays.filter(
      (display) => display.groupId === this.mapper3d.getCurrentGroupId(),
    );
    if (displaysWithCurrentGroupId.length === 0) {
      this.updateCurrentDisplay(
        this.getFirstDisplayWithRectsOrFirstDisplay(this.internalDisplays),
      );
      return;
    }

    this.updateCurrentDisplay(
      this.getFirstDisplayWithRectsOrFirstDisplay(displaysWithCurrentGroupId),
    );
    return;
  }

  updateControlsFromStore() {
    this.storeKeyShowOnlyVisibleState = `rectsView.${this.title}.showOnlyVisibleState`;
    this.storeKeyZSpacingFactor = `rectsView.${this.title}.zSpacingFactor`;
    this.storeKeyShadingMode = `rectsView.${this.title}.shadingMode`;

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

    const storedShadingMode = assertDefined(this.store).get(
      this.storeKeyShadingMode,
    );
    if (
      storedShadingMode !== undefined &&
      this.shadingModes.includes(storedShadingMode as ShadingMode)
    ) {
      this.mapper3d.setShadingMode(storedShadingMode as ShadingMode);
    }
  }

  onSeparationSliderChange(factor: number) {
    Analytics.Navigation.logRectSettingsChanged('z spacing', factor);
    this.store?.add(this.storeKeyZSpacingFactor, `${factor}`);
    this.mapper3d.setZSpacingFactor(factor);
    this.drawLargeRectsAndLabels();
  }

  onRotationSliderChange(factor: number) {
    this.mapper3d.setCameraRotationFactor(factor);
    this.drawLargeRectsAndLabels();
  }

  resetCamera() {
    Analytics.Navigation.logZoom('reset', 'rects');
    this.mapper3d.resetCamera();
    this.drawLargeRectsAndLabels();
  }

  @HostListener('wheel', ['$event'])
  onScroll(event: WheelEvent) {
    if ((event.target as HTMLElement).className === 'large-rects-canvas') {
      if (event.deltaY > 0) {
        Analytics.Navigation.logZoom('scroll', 'rects', 'out');
        this.doZoomOut(RectsComponent.ZOOM_SCROLL_RATIO);
      } else {
        Analytics.Navigation.logZoom('scroll', 'rects', 'in');
        this.doZoomIn(RectsComponent.ZOOM_SCROLL_RATIO);
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
    Analytics.Navigation.logZoom('button', 'rects', 'in');
    this.doZoomIn();
  }

  onZoomOutClick() {
    Analytics.Navigation.logZoom('button', 'rects', 'out');
    this.doZoomOut();
  }

  onShowOnlyVisibleModeChange(enabled: boolean) {
    Analytics.Navigation.logRectSettingsChanged('only visible', enabled);
    this.store?.add(this.storeKeyShowOnlyVisibleState, `${enabled}`);
    this.mapper3d.setShowOnlyVisibleMode(enabled);
    this.drawLargeRectsAndLabels();
  }

  onDisplayIdChange(display: DisplayIdentifier) {
    this.updateCurrentDisplay(display);
    const event = new CustomEvent(ViewerEvents.RectGroupIdChange, {
      bubbles: true,
      detail: {groupId: display.groupId},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  getDisplayButtonColor(groupId: number) {
    if (!this.currentDisplay) return 'primary';
    return this.currentDisplay.groupId === groupId ? 'primary' : 'secondary';
  }

  shouldDisableTooltip(buttonText: HTMLElement): boolean {
    return !this.isStackBased && buttonText.offsetWidth < 200;
  }

  getDisplayButtonTooltip(
    display: DisplayIdentifier,
    buttonText: HTMLElement,
  ): string {
    const hasLongDisplayName = buttonText.offsetWidth >= 200;
    const displayName = hasLongDisplayName ? display.name + '\n' : '';
    if (display.groupId === DisplayLayerStack.INVALID_LAYER_STACK) {
      return (
        displayName +
        (this.isStackBased ? 'Invalid layer stack - display off' : '')
      );
    }
    return displayName + (this.isStackBased ? 'display on' : '');
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

  getShadingMode(): ShadingMode {
    return this.mapper3d.getShadingMode();
  }

  onShadingModeButtonClicked() {
    this.mapper3d.updateShadingMode();
    const newMode = this.mapper3d.getShadingMode();
    Analytics.Navigation.logRectSettingsChanged('shading mode', newMode);
    this.store?.add(this.storeKeyShadingMode, newMode);
    this.drawLargeRectsAndLabels();
  }

  onInteractionStart(button: CanColor) {
    button.color = 'primary';
  }

  onInteractionEnd(button: CanColor) {
    button.color = 'accent';
  }

  private getFirstDisplayWithRectsOrFirstDisplay(
    displays: DisplayIdentifier[],
  ): DisplayIdentifier {
    return (
      displays.find((display) =>
        this.internalRects.some(
          (rect) => !rect.isDisplay && rect.groupId === display.groupId,
        ),
      ) ?? assertDefined(displays.at(0))
    );
  }

  private updateCurrentDisplay(display: DisplayIdentifier) {
    this.currentDisplay = display;
    this.mapper3d.setCurrentGroupId(display.groupId);
    this.drawLargeRectsAndLabels();
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

  private doZoomIn(ratio = 1) {
    this.mapper3d.increaseZoomFactor(ratio);
    this.drawLargeRectsAndLabels();
  }

  private doZoomOut(ratio = 1) {
    this.mapper3d.decreaseZoomFactor(ratio);
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
      const largeRectShadingMode = this.mapper3d.getShadingMode();
      const largeRectGroupId = this.mapper3d.getCurrentGroupId();
      const largeRectZSpacing = this.mapper3d.getZSpacingFactor();
      const largeRectCameraRotation = this.mapper3d.getCameraRotationFactor();

      this.mapper3d.setShadingMode(ShadingMode.GRADIENT);
      this.mapper3d.setCurrentGroupId(this.internalMiniRects[0]?.groupId);
      this.mapper3d.resetToOrthogonalState();

      this.mapper3d.setRects(this.internalMiniRects);
      this.mapper3d.decreaseZoomFactor(this.zoomFactor - 1);
      this.miniRectsCanvas?.draw(this.mapper3d.computeScene());
      this.mapper3d.increaseZoomFactor(this.zoomFactor - 1);

      // Mapper internally sets these values to 100%. They need to be reset afterwards
      if (this.miniRectsCanvasElement) {
        this.miniRectsCanvasElement.style.width = '25%';
        this.miniRectsCanvasElement.style.height = '25%';
      }

      this.mapper3d.setShadingMode(largeRectShadingMode);
      this.mapper3d.setCurrentGroupId(largeRectGroupId);
      this.mapper3d.setZSpacingFactor(largeRectZSpacing);
      this.mapper3d.setCameraRotationFactor(largeRectCameraRotation);
    }
  }

  private notifyHighlightedItem(id: string) {
    const event: CustomEvent = new CustomEvent(
      ViewerEvents.HighlightedIdChange,
      {
        bubbles: true,
        detail: {id},
      },
    );
    this.elementRef.nativeElement.dispatchEvent(event);
  }
}
