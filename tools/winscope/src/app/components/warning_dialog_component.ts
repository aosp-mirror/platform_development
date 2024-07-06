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
import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';

@Component({
  selector: 'warning-dialog',
  template: `
    <h2 class="warning-dialog-title" mat-dialog-title>
      <span> Warning </span>
    </h2>
    <mat-dialog-content class="warning-content">
      <p class="warning-message mat-body-1"> {{data.message}} </p>

      <div class="warning-actions">
        <div class="warning-action-boxes">
          <mat-checkbox
            *ngFor="let option of data.options; let i = index"
            color="primary"
            (change)="updateSelectedOptions(option)"
            [class.not-last]="i < data.options.length - 1"
            >{{ option }}</mat-checkbox>
        </div>
        <div class="warning-action-buttons">
          <button *ngFor="let action of data.actions" [mat-dialog-close]="getDialogResult(action)" class="not-last" color="primary" mat-stroked-button> {{ action }} </button>
          <button [mat-dialog-close]="getDialogResult(data.closeText)" color="primary" mat-raised-button> {{ data.closeText }} </button>
        </div>
      </div>
    </mat-dialog-content>
  `,
  styles: [
    `
      .warning-dialog-title {
        display: flex;
        justify-content: space-between;
      }
      .warning-close-button {
        width: 24px;
        height: 24px;
        line-height: 24px;
      }
      .warning-content {
        overflow: visible;
      }
      .warning-message {
        white-space: pre-line;
        font-size: 16px;
      }
      .warning-actions {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 8px;
      }
      .warning-actions .not-last {
        margin-right: 8px;
      }
    `,
  ],
})
export class WarningDialogComponent {
  selectedOptions: string[] = [];

  constructor(@Inject(MAT_DIALOG_DATA) public data: WarningDialogData) {}

  updateSelectedOptions(clickedOption: string) {
    if (!this.selectedOptions.includes(clickedOption)) {
      this.selectedOptions.push(clickedOption);
    } else {
      this.selectedOptions.filter((opt) => opt !== clickedOption);
    }
  }

  getDialogResult(closeActionText?: string): WarningDialogResult {
    return {closeActionText, selectedOptions: this.selectedOptions};
  }
}

export interface WarningDialogData {
  message: string | undefined;
  actions: string[] | undefined;
  options: string[] | undefined;
  closeText: string;
}

export interface WarningDialogResult {
  closeActionText: string | undefined;
  selectedOptions: string[];
}
