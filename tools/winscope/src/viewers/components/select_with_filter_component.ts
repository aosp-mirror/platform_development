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
import {Component, EventEmitter, Input, Output} from '@angular/core';
import {MatSelectChange} from '@angular/material/select';

@Component({
  selector: 'select-with-filter',
  template: `
    <mat-form-field appearance="fill" [style]="getOuterFormFieldStyle()">
      <mat-label>{{ label }}</mat-label>
      <mat-select
        (opened)="filter.focus()"
        (closed)="onSelectClosed()"
        (selectionChange)="onSelectChange($event)"
        [multiple]="multiple"
        [value]="value">
        <mat-form-field class="select-filter" [style]="getInnerFormFieldStyle()">
          <mat-label>Filter options</mat-label>
          <input matInput #filter [(ngModel)]="filterString" />
        </mat-form-field>
        <mat-option
          *ngFor="let option of options"
          [value]="option"
          [class.hidden-option]="hideOption(option)">
          {{ option }}
        </mat-option>
      </mat-select>
    </mat-form-field>
  `,
  styles: [
    `
      mat-form-field {
        width: 100%;
        font-size: 12px;
      }

      .hidden-option {
        display: none;
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
  @Input() multiple = true;
  @Input() value?: string | string[] | undefined;

  @Output() readonly selectChange = new EventEmitter<MatSelectChange>();

  filterString: string = '';

  onSelectChange(event: MatSelectChange) {
    this.selectChange.emit(event);
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

  onSelectClosed() {
    this.filterString = '';
  }

  hideOption(option: string) {
    return !option.toLowerCase().includes(this.filterString.toLowerCase());
  }
}
