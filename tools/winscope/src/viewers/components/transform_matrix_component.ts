/*
 * Copyright (C) 2024 The Android Open Source Project
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
import {assertDefined} from 'common/assert_utils';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';

@Component({
  selector: 'transform-matrix',
  template: `
    <div *ngIf="matrix" class="matrix">
      <p class="mat-body-1">
        {{ getVal('dsdx') }}
      </p>
      <p class="mat-body-1">
        {{ getVal('dtdx') }}
      </p>
      <p class="mat-body-1" matTooltip="Translate x">
        {{ getVal('tx') }}
      </p>

      <p class="mat-body-1">
        {{ getVal('dtdy') }}
      </p>
      <p class="mat-body-1">
        {{ getVal('dsdy') }}
      </p>
      <p class="mat-body-1" matTooltip="Translate y">
        {{ getVal('ty') }}
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
  @Input() matrix: UiPropertyTreeNode | undefined;

  getVal(name: string): string {
    return (
      assertDefined(this.matrix).getChildByName(name)?.formattedValue() ??
      'null'
    );
  }
}
