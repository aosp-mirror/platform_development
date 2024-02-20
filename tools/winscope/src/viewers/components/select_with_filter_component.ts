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
import {
  Component,
  EventEmitter,
  Input,
  Output,
  SimpleChanges,
} from '@angular/core';
import {MatSelectChange} from '@angular/material/select';

@Component({
  selector: 'select-with-filter',
  template: `
    <mat-form-field appearance="fill" [style]="getOuterFormFieldStyle()">
      <mat-label>{{ label }}</mat-label>
      <mat-select (opened)="filter.focus()" (selectionChange)="onSelectChange($event)" multiple>
        <mat-form-field class="select-filter" [style]="getInnerFormFieldStyle()">
          <mat-label>Filter options</mat-label>
          <input matInput #filter [(ngModel)]="filterString" (input)="onOptionsFilterChange()" />
        </mat-form-field>
        <mat-option *ngFor="let option of filteredOptions" [value]="option">
          {{ option }}
        </mat-option>
      </mat-select>
    </mat-form-field>
  `,
  styles: [
    `
      ::ng-deep .mat-select-panel-wrap {
        overflow: scroll;
        overflow-x: hidden;
        max-height: 75vh;
      }

      mat-form-field {
        width: 100%;
        font-size: 12px;
      }
    `,
  ],
})
export class SelectWithFilterComponent {
  @Input() label: string = '';
  @Input() options: string[] = [];
  @Input() outerFilterWidth = '75';
  @Input() innerFilterWidth = '100';
  @Input() flex = 'none';

  @Output() readonly selectChange = new EventEmitter<MatSelectChange>();

  filterString: string = '';
  filteredOptions: string[] = this.options;

  ngOnChanges(changes: SimpleChanges) {
    if (changes['options']) {
      this.updateSelectOptions();
    }
  }

  updateSelectOptions() {
    this.filteredOptions = this.options.filter((option) =>
      option.includes(this.filterString),
    );
  }

  onSelectChange(event: MatSelectChange) {
    this.selectChange.emit(event);
  }

  onOptionsFilterChange() {
    this.updateSelectOptions();
  }

  getOuterFormFieldStyle() {
    return {
      flex: this.flex,
      width: this.outerFilterWidth + 'px',
    };
  }

  getInnerFormFieldStyle() {
    return {
      flex: 'none',
      paddingTop: '2px',
      paddingLeft: '10px',
      paddingRight: '20px',
      width: this.innerFilterWidth + 'px',
    };
  }
}
