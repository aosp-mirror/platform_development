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
import { Component, Input } from "@angular/core";
import { Transform } from "common/trace/flickerlib/common";

@Component({
  selector: "transform-matrix",
  template: `
      <div class="matrix" *ngIf="transform" [matTooltip]="transform.getTypeAsString()">
        <div class="cell">{{ formatFloat(transform.matrix.dsdx) }}</div>
        <div class="cell">{{ formatFloat(transform.matrix.dsdy) }}</div>
        <div class="cell" matTooltip="Translate x">
          {{ formatFloat(transform.matrix.tx) }}
        </div>

        <div class="cell">{{ formatFloat(transform.matrix.dtdx) }}</div>
        <div class="cell">{{ formatFloat(transform.matrix.dtdy) }}</div>
        <div class="cell" matTooltip="Translate y">
          {{ formatFloat(transform.matrix.ty) }}
        </div>

        <div class="cell">0</div>
        <div class="cell">0</div>
        <div class="cell">1</div>
      </div>
  `,
  styles: [
    `
      .matrix {
        display: grid;
        grid-gap: 1px;
        grid-template-columns: repeat(3, 1fr);
      }

      .cell {
        padding-left: 10px;
        background-color: #F8F9FA;
      }
    `
  ],
})

export class TransformMatrixComponent {
  @Input() transform!: Transform;
  @Input() formatFloat!: (num: number) => number;
}
