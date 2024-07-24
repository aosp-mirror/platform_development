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
import {TableProperties} from 'viewers/common/table_properties';

@Component({
  selector: 'properties-table',
  template: `
    <table class="table" *ngIf="properties !== undefined">
      <tr *ngFor="let entry of objectEntries(properties)">
        <td class="table-cell-name">
          <p class="mat-body-1">{{ entry[0] }}</p>
        </td>
        <td class="table-cell-value">
          <p class="mat-body-1">{{ entry[1] != undefined ? entry[1] : 'undefined' }}</p>
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

      .table-cell-name,
      .table-cell-value {
        padding: 1px 5px;
        border: 1px solid var(--border-color);
        overflow-wrap: anywhere;
      }

      .table-cell-name {
        width: 20%;
        background-color: rgba(158, 192, 200, 0.281);
      }
    `,
  ],
})
export class PropertiesTableComponent {
  objectEntries = Object.entries;

  @Input() properties: TableProperties | undefined;
}
