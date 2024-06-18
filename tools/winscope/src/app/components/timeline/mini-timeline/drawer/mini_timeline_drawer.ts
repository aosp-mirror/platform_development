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

import {Point} from 'common/geometry_types';
import {Padding} from 'common/padding';
import {Trace} from 'trace/trace';
import {CanvasMouseHandler} from './canvas_mouse_handler';

export interface MiniTimelineDrawer {
  draw(): Promise<void>;
  updateHover(mousePoint: Point | undefined): Promise<void>;
  getTraceClicked(mousePoint: Point): Promise<Trace<object> | undefined>;
  getXScale(): number;
  getYScale(): number;
  getHeight(): number;
  getWidth(): number;
  getPadding(): Padding;
  getUsableRange(): {from: number; to: number};
  getClickRange(clickPos: Point): {from: number; to: number};
  canvas: HTMLCanvasElement;
  handler: CanvasMouseHandler;
}
