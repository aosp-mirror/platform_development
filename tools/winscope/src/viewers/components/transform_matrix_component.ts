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
import {Component, Input} from '@angular/core';
import {Transform} from 'trace/flickerlib/common';

@Component({
  selector: 'transform-matrix',
  template: `
    <div *ngIf="transform" class="matrix" [matTooltip]="transform.getTypeAsString()">
      <p class="mat-body-1">
        {{ formatFloat(transform.matrix.dsdx) }}
      </p>
      <p class="mat-body-1">
        {{ formatFloat(transform.matrix.dsdy) }}
      </p>
      <p class="mat-body-1" matTooltip="Translate x">
        {{ formatFloat(transform.matrix.tx) }}
      </p>

      <p class="mat-body-1">
        {{ formatFloat(transform.matrix.dtdx) }}
      </p>
      <p class="mat-body-1">
        {{ formatFloat(transform.matrix.dtdy) }}
      </p>
      <p class="mat-body-1" matTooltip="Translate y">
        {{ formatFloat(transform.matrix.ty) }}
      </p>

      <p class="mat-body-1">0</p>
      <p class="mat-body-1">0</p>
      <p class="mat-body-1">1</p>
    </div>
  `,
  styles: [
    `
      .matrix {
        display: grid;
        grid-gap: 1px;
        grid-template-columns: repeat(3, 1fr);
        text-align: center;
      }
    `,
  ],
})
export class TransformMatrixComponent {
  @Input() transform!: Transform;
  @Input() formatFloat!: (num: number) => number;
}
