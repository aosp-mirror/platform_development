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
  Input
} from "@angular/core";
import { UiData } from "./ui_data";
import { TRACE_INFO } from "app/trace_info";
import { TraceType } from "common/trace/trace_type";

@Component({
  selector: "viewer-surface-flinger",
  template: `
      <div fxLayout="row wrap" fxLayoutGap="10px grid" class="card-grid">
        <mat-card class="rects-view">
          <rects-view
            [rects]="inputData?.rects ?? []"
            [displayIds]="inputData?.displayIds ?? []"
            [highlighted]="inputData?.highlighted ?? ''"
            class="rects-view"
          ></rects-view>
        </mat-card>
        <mat-card id="sf-hierarchy-view" class="hierarchy-view">
          <hierarchy-view></hierarchy-view>
        </mat-card>
        <mat-card id="sf-properties-view" class="properties-view">
          <properties-view></properties-view>
        </mat-card>
      </div>
  `,
  styles: [
    "@import 'https://fonts.googleapis.com/icon?family=Material+Icons';",
    "mat-icon {margin: 5px}",
    "viewer-surface-flinger {font-family: Arial, Helvetica, sans-serif;}",
    ".trace-card-title {display: inline-block; vertical-align: middle;}",
    ".header-button {background: none; border: none; display: inline-block; vertical-align: middle;}",
    ".card-grid {width: 100%;height: 100%;display: flex;flex-direction: row;overflow: auto;}",
    ".rects-view {font: inherit; flex: none !important;width: 400px;margin: 8px;}",
    ".hierarchy-view, .properties-view {font: inherit; flex: 1;margin: 8px;min-width: 400px;min-height: 50rem;max-height: 50rem;}",
  ]
})
export class ViewerSurfaceFlingerComponent {
  @Input()
    inputData?: UiData;

  TRACE_INFO = TRACE_INFO;
  TraceType = TraceType;
}
