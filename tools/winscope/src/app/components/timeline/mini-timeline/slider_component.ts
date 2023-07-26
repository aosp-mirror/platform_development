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
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import {Color} from 'app/colors';
import {TimeRange} from 'app/timeline_data';
import {assertDefined} from 'common/assert_utils';
import {Timestamp} from 'trace/timestamp';
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
        <div class="left cropper"></div>
        <div class="handle"></div>
        <div class="right cropper"></div>
      </div>
      <div class="cursor" [style]="{left: cursorOffset + 'px'}"></div>
    </div>
  `,
  styles: [
    `
      #timeline-slider-box {
        position: relative;
        margin: 5px 0;
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
        background: ${Color.GUIDE_BAR_LIGHT};
      }

      .selection.line {
        background: ${Color.SELECTOR_COLOR};
      }

      .slider {
        display: flex;
        justify-content: space-between;
        cursor: grab;
        position: absolute;
      }

      .handle {
        flex-grow: 1;
        background: ${Color.SELECTION_BACKGROUND};
        cursor: grab;
      }

      .cropper {
        width: 5px;
        background: ${Color.SELECTOR_COLOR};
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

  @Output() onZoomChanged = new EventEmitter<TimeRange>();

  dragging = false;
  sliderWidth = 0;
  dragPosition = {x: 0, y: 0};
  viewInitialized = false;
  cursorOffset = 0;

  @ViewChild('sliderBox', {static: false}) sliderBox!: ElementRef;

  ngOnChanges(changes: SimpleChanges) {
    if (changes['zoomRange'] !== undefined && !this.dragging) {
      const zoomRange = changes['zoomRange'].currentValue as TimeRange;
      this.syncDragPositionTo(zoomRange);
    }

    if (changes['currentPosition']) {
      const currentPosition = changes['currentPosition'].currentValue as TracePosition;
      this.syncCursosPositionTo(currentPosition.timestamp);
    }
  }

  syncDragPositionTo(zoomRange: TimeRange) {
    this.sliderWidth = this.computeSliderWidth();
    const middleOfZoomRange = zoomRange.from.plus(zoomRange.to.minus(zoomRange.from).div(2n));

    this.dragPosition = {
      // Calculation to account for there being a min width of the slider
      x: this.getTransformer().transform(middleOfZoomRange) - this.sliderWidth / 2,
      y: 0,
    };
  }

  syncCursosPositionTo(timestamp: Timestamp) {
    this.cursorOffset = this.getTransformer().transform(timestamp);
  }

  getTransformer(): Transformer {
    const width = this.viewInitialized ? this.sliderBox.nativeElement.offsetWidth : 0;
    return new Transformer(assertDefined(this.fullRange), {from: 0, to: width});
  }

  ngAfterViewInit(): void {
    this.viewInitialized = true;
  }

  @HostListener('window:resize', ['$event'])
  onResize(event: Event) {
    this.syncDragPositionTo(assertDefined(this.zoomRange));
    this.syncCursosPositionTo(assertDefined(this.currentPosition).timestamp);
  }

  computeSliderWidth() {
    const minSliderWidth = 15;
    const transformer = this.getTransformer();
    let width =
      transformer.transform(assertDefined(this.zoomRange).to) -
      transformer.transform(assertDefined(this.zoomRange).from);
    if (width < minSliderWidth) {
      width = minSliderWidth;
    }

    return width;
  }

  onSlideStart(e: any) {
    this.dragging = true;
    document.body.classList.add('inheritCursors');
    document.body.style.cursor = 'grabbing';
  }

  onSlideEnd(e: any) {
    this.dragging = false;
    this.syncDragPositionTo(assertDefined(this.zoomRange));
    document.body.classList.remove('inheritCursors');
    document.body.style.cursor = 'unset';
  }

  onSliderMove(e: any) {
    const zoomRange = assertDefined(this.zoomRange);
    const newX = e.source.freeDragPosition.x + e.distance.x;
    // Calculation to adjust for min width slider
    const from = this.getTransformer()
      .untransform(newX + this.sliderWidth / 2)
      .minus(zoomRange.to.minus(zoomRange.from).div(2n));

    const to = new Timestamp(
      assertDefined(this.zoomRange).to.getType(),
      from.getValueNs() +
        (assertDefined(this.zoomRange).to.getValueNs() -
          assertDefined(this.zoomRange).from.getValueNs())
    );

    this.onZoomChanged.emit({from, to});
  }

  startMoveLeft(e: any) {
    e.preventDefault();

    const startPos = e.pageX;
    const startOffset = this.getTransformer().transform(assertDefined(this.zoomRange).from);

    const listener = (event: any) => {
      const movedX = event.pageX - startPos;
      const from = this.getTransformer().untransform(startOffset + movedX);
      const to = assertDefined(this.zoomRange).to;

      this.onZoomChanged.emit({from, to});
    };
    addEventListener('mousemove', listener);

    const mouseUpListener = () => {
      removeEventListener('mousemove', listener);
      removeEventListener('mouseup', mouseUpListener);
    };
    addEventListener('mouseup', mouseUpListener);
  }
}
