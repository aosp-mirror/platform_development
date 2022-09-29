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

@Component({
  selector: "coordinates-table",
  template: `
    <span class="coord-null-value" *ngIf="!hasCoordinates()">null</span>
    <div class="coord-table-wrapper" *ngIf="hasCoordinates()">
      <table class="table">
        <tr class="header-row">
          <td>Left</td>
          <td>Top</td>
          <td>Right</td>
          <td>Bottom</td>
        </tr>
        <tr>
          <td>{{ coordinates.left }}</td>
          <td>{{ coordinates.top }}</td>
          <td>{{ coordinates.right }}</td>
          <td>{{ coordinates.bottom }}</td>
        </tr>
      </table>
    </div>
  `,
  styles: [
    `
      .coord-null-value {
        color: rgba(0, 0, 0, 0.75);
      }

      .coord-table-wrapper {
        margin-left: 10px;
        display: inline-flex;
        padding: 3px 0px;
      }

      .coord-table-wrapper td, .coord-table-wrapper th {
        height: auto;
        border: 1px solid ar(--default-border);
      }

      .coord-table-wrapper .header-row td {
        color: gray;
        font-weight: 600;
      }
    `
  ],
})

export class CoordinatesTableComponent {
  @Input() coordinates!: any;

  hasCoordinates() {
    return this.coordinates.left || this.coordinates.top ||
        this.coordinates.right || this.coordinates.bottom;
  }
}
