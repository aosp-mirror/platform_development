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
import { TableProperties } from "viewers/common/table_properties";

@Component({
  selector: "properties-table",
  template: `
   <div class="properties-table-wrapper">
    <table class="table">
      <tr *ngFor="let entry of objectEntries(properties)">
        <td class="table-cell-name">
          <span>{{ entry[0] }}</span>
        </td>
        <td class="table-cell-value">
          <span>{{ entry[1] != undefined ? entry[1] : 'undefined' }}</span>
        </td>
      </tr>
    </table>
  </div>
  `,
  styles: [
    `
      .properties-table-wrapper {
        border-bottom: 1px solid var(--default-border);
      }

      .properties-table-wrapper .table-cell-name {
        background-color: rgba(158, 192, 200, 0.281);
        border: 1px solid var(--default-border);
        height: 20px;
        padding: 0;
        width: 20%;
      }

      .properties-table-wrapper .table-cell-value {
        overflow-wrap: anywhere;
        border: 1px solid var(--default-border);
        height: 20px;
        padding: 0;
        width: 80%;
      }

      .properties-table-wrapper table {
        height: auto;
        border-collapse: collapse;
        width: 100%;
      }

      .properties-table-wrapper span {
        padding: 5px;
      }
    `
  ],
})

export class PropertiesTableComponent {
  objectEntries = Object.entries;

  @Input() properties!: TableProperties;

}
