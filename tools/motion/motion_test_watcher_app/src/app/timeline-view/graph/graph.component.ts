/*
 * Copyright 2024 Google LLC
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
  Input,
  OnChanges,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { checkNotNull } from '../../../utils/preconditions';
import { VisualTimeline } from '../../visual-timeline';
import { Feature } from '../../feature';

@Component({
  selector: 'app-graph',
  standalone: true,
  templateUrl: './graph.component.html',
  styleUrls: ['./graph.component.scss'],
})
export class GraphComponent implements OnChanges {
  constructor() {}

  @ViewChild('canvas', { read: ElementRef })
  canvas!: ElementRef<HTMLCanvasElement>;

  @Input()
  feature!: Feature;

  @Input()
  timeline!: VisualTimeline;

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.canvas) return;
    this._renderGraph();
  }

  updateCanvasSize(width: number) {
    if (!this.canvas) return;

    const canvasElement = this.canvas.nativeElement;

    if (canvasElement.width == width) {
      return;
    }

    canvasElement.width = width;
    canvasElement.height = this.feature.visualization.height;
    this._renderGraph();
  }

  get labelLeft() {
    return 0;
    // const ranges = this.feature?.property?.series?.activeRanges;
    // return ranges && ranges.length && this.timeline
    //   ? `${this.timeline.frameToPx(ranges[0].start)}px`
    //   : 0;
  }

  private _renderGraph() {
    this.feature.visualization.render(
      this.timeline,
      this.feature.dataPoints,
      this.canvas.nativeElement,
    );
  }
}
