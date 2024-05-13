/*
 * Copyright (C) 2023 The Android Open Source Project
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
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Inject,
  Input,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import {Color} from 'app/colors';
import {assertDefined} from 'common/assert_utils';
import {Point} from 'common/geometry_types';
import {TimeRange, Timestamp} from 'common/time';
import {ComponentTimestampConverter} from 'common/timestamp_converter';
import {TracePosition} from 'trace/trace_position';
import {Transformer} from './transformer';

@Component({
  selector: 'slider',
  template: `
    <div id="timeline-slider-box" #sliderBox>
      <div class="background line"></div>
      <div
        class="slider"
        cdkDragLockAxis="x"
        cdkDragBoundary="#timeline-slider-box"
        cdkDrag
        (cdkDragMoved)="onSliderMove($event)"
        (cdkDragStarted)="onSlideStart($event)"
        (cdkDragEnded)="onSlideEnd($event)"
        [cdkDragFreeDragPosition]="dragPosition"
        [style]="{width: sliderWidth + 'px'}">
        <div class="left cropper" (mousedown)="startMoveLeft($event)"></div>
        <div class="handle" cdkDragHandle></div>
        <div class="right cropper" (mousedown)="startMoveRight($event)"></div>
      </div>
      <div class="cursor" [style]="{left: cursorOffset + 'px'}"></div>
    </div>
  `,
  styles: [
    `
      #timeline-slider-box {
        position: relative;
        margin-bottom: 5px;
      }

      #timeline-slider-box,
      .slider {
        height: 10px;
      }

      .line {
        height: 3px;
        position: absolute;
        margin: auto;
        top: 0;
        bottom: 0;
        margin: auto 0;
      }

      .background.line {
        width: 100%;
        background: ${Color.GUIDE_BAR};
      }

      .selection.line {
        background: var(--slider-border-color);
      }

      .slider {
        display: flex;
        justify-content: space-between;
        cursor: grab;
        position: absolute;
      }

      .handle {
        flex-grow: 1;
        background: var(--slider-background-color);
        cursor: grab;
      }

      .cropper {
        width: 5px;
        background: var(--slider-border-color);
      }

      .cropper.left,
      .cropper.right {
        cursor: ew-resize;
      }

      .cursor {
        width: 2px;
        height: 100%;
        position: absolute;
        pointer-events: none;
        background: ${Color.ACTIVE_POINTER};
      }
    `,
  ],
})
export class SliderComponent {
  @Input() fullRange: TimeRange | undefined;
  @Input() zoomRange: TimeRange | undefined;
  @Input() currentPosition: TracePosition | undefined;
  @Input() timestampConverter: ComponentTimestampConverter | undefined;

  @Output() readonly onZoomChanged = new EventEmitter<TimeRange>();

  dragging = false;
  sliderWidth = 0;
  dragPosition: Point = {x: 0, y: 0};
  viewInitialized = false;
  cursorOffset = 0;

  @ViewChild('sliderBox', {static: false}) sliderBox!: ElementRef;

  constructor(@Inject(ChangeDetectorRef) private cdr: ChangeDetectorRef) {}

  ngOnChanges(changes: SimpleChanges) {
    if (changes['zoomRange'] !== undefined && !this.dragging) {
      const zoomRange = changes['zoomRange'].currentValue as TimeRange;
      this.syncDragPositionTo(zoomRange);
    }

    if (changes['currentPosition']) {
      const currentPosition = changes['currentPosition']
        .currentValue as TracePosition;
      this.syncCursosPositionTo(currentPosition.timestamp);
    }
  }

  syncDragPositionTo(zoomRange: TimeRange) {
    this.sliderWidth = this.computeSliderWidth();
    const middleOfZoomRange = zoomRange.from.add(
      zoomRange.to.minus(zoomRange.from.getValueNs()).div(2n).getValueNs(),
    );

    this.dragPosition = {
      // Calculation to account for there being a min width of the slider
      x:
        this.getTransformer().transform(middleOfZoomRange) -
        this.sliderWidth / 2,
      y: 0,
    };
  }

  syncCursosPositionTo(timestamp: Timestamp) {
    this.cursorOffset = this.getTransformer().transform(timestamp);
  }

  getTransformer(): Transformer {
    const width = this.viewInitialized
      ? this.sliderBox.nativeElement.offsetWidth
      : 0;
    return new Transformer(
      assertDefined(this.fullRange),
      {from: 0, to: width},
      assertDefined(this.timestampConverter),
    );
  }

  ngAfterViewInit(): void {
    this.viewInitialized = true;
  }

  ngAfterViewChecked() {
    assertDefined(this.fullRange);
    const zoomRange = assertDefined(this.zoomRange);
    this.syncDragPositionTo(zoomRange);
    this.cdr.detectChanges();
  }

  @HostListener('window:resize', ['$event'])
  onResize(event: Event) {
    this.syncDragPositionTo(assertDefined(this.zoomRange));
    this.syncCursosPositionTo(assertDefined(this.currentPosition).timestamp);
  }

  computeSliderWidth() {
    const transformer = this.getTransformer();
    let width =
      transformer.transform(assertDefined(this.zoomRange).to) -
      transformer.transform(assertDefined(this.zoomRange).from);
    if (width < MIN_SLIDER_WIDTH) {
      width = MIN_SLIDER_WIDTH;
    }

    return width;
  }

  slideStartX: number | undefined = undefined;
  onSlideStart(e: any) {
    this.dragging = true;
    this.slideStartX = e.source.freeDragPosition.x;
    document.body.classList.add('inheritCursors');
    document.body.style.cursor = 'grabbing';
  }

  onSlideEnd(e: any) {
    this.dragging = false;
    this.slideStartX = undefined;
    this.syncDragPositionTo(assertDefined(this.zoomRange));
    document.body.classList.remove('inheritCursors');
    document.body.style.cursor = 'unset';
  }

  onSliderMove(e: any) {
    const zoomRange = assertDefined(this.zoomRange);
    let newX = this.slideStartX + e.distance.x;
    if (newX < 0) {
      newX = 0;
    }

    // Calculation to adjust for min width slider
    const from = this.getTransformer()
      .untransform(newX + this.sliderWidth / 2)
      .minus(
        zoomRange.to.minus(zoomRange.from.getValueNs()).div(2n).getValueNs(),
      );

    const to = assertDefined(this.timestampConverter).makeTimestampFromNs(
      from.getValueNs() +
        (assertDefined(this.zoomRange).to.getValueNs() -
          assertDefined(this.zoomRange).from.getValueNs()),
    );

    this.onZoomChanged.emit(new TimeRange(from, to));
  }

  startMoveLeft(e: any) {
    e.preventDefault();

    const startPos = e.pageX;
    const startOffset = this.getTransformer().transform(
      assertDefined(this.zoomRange).from,
    );

    const listener = (event: any) => {
      const movedX = event.pageX - startPos;
      let from = this.getTransformer().untransform(startOffset + movedX);
      if (from.getValueNs() < assertDefined(this.fullRange).from.getValueNs()) {
        from = assertDefined(this.fullRange).from;
      }
      if (from.getValueNs() > assertDefined(this.zoomRange).to.getValueNs()) {
        from = assertDefined(this.zoomRange).to;
      }
      const to = assertDefined(this.zoomRange).to;

      this.onZoomChanged.emit(new TimeRange(from, to));
    };
    addEventListener('mousemove', listener);

    const mouseUpListener = () => {
      removeEventListener('mousemove', listener);
      removeEventListener('mouseup', mouseUpListener);
    };
    addEventListener('mouseup', mouseUpListener);
  }

  startMoveRight(e: any) {
    e.preventDefault();

    const startPos = e.pageX;
    const startOffset = this.getTransformer().transform(
      assertDefined(this.zoomRange).to,
    );

    const listener = (event: any) => {
      const movedX = event.pageX - startPos;
      const from = assertDefined(this.zoomRange).from;
      let to = this.getTransformer().untransform(startOffset + movedX);
      if (to.getValueNs() > assertDefined(this.fullRange).to.getValueNs()) {
        to = assertDefined(this.fullRange).to;
      }
      if (to.getValueNs() < assertDefined(this.zoomRange).from.getValueNs()) {
        to = assertDefined(this.zoomRange).from;
      }

      this.onZoomChanged.emit(new TimeRange(from, to));
    };
    addEventListener('mousemove', listener);

    const mouseUpListener = () => {
      removeEventListener('mousemove', listener);
      removeEventListener('mouseup', mouseUpListener);
    };
    addEventListener('mouseup', mouseUpListener);
  }
}

export const MIN_SLIDER_WIDTH = 30;
