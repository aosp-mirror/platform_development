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

@Component({
  selector: 'coordinates-table',
  template: `
    <p *ngIf="!hasCoordinates()" class="mat-body-1">null</p>
    <table *ngIf="hasCoordinates()" class="table">
      <tr class="header-row">
        <td>
          <p class="mat-body-1">Left</p>
        </td>
        <td>
          <p class="mat-body-1">Top</p>
        </td>
        <td>
          <p class="mat-body-1">Right</p>
        </td>
        <td>
          <p class="mat-body-1">Bottom</p>
        </td>
      </tr>
      <tr>
        <td>
          <p class="mat-body-1">{{ coordinates.left }}</p>
        </td>
        <td>
          <p class="mat-body-1">{{ coordinates.top }}</p>
        </td>
        <td>
          <p class="mat-body-1">{{ coordinates.right }}</p>
        </td>
        <td>
          <p class="mat-body-1">{{ coordinates.bottom }}</p>
        </td>
      </tr>
    </table>
  `,
  styles: [
    `
      .table {
        width: 100%;
        border-collapse: collapse;
      }

      .table td {
        padding: 1px 5px;
        border: 1px solid var(--border-color);
        text-align: center;
        overflow-wrap: anywhere;
      }

      .header-row td {
        color: gray;
      }
    `,
  ],
})
export class CoordinatesTableComponent {
  @Input() coordinates!: any;

  hasCoordinates() {
    return (
      this.coordinates.left ||
      this.coordinates.top ||
      this.coordinates.right ||
      this.coordinates.bottom
    );
  }
}
