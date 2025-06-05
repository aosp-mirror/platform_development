/*
 * Copyright (C) 2023 The Android Open Source Project
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

import {Component, ElementRef, Inject} from '@angular/core';
import {MatSnackBarRef, MAT_SNACK_BAR_DATA} from '@angular/material/snack-bar';

@Component({
  selector: 'snack-bar',
  template: `
    <div class="snack-bar-container">
      <div class="message-container">
        <p *ngFor="let message of messages" class="message mat-body-1">
          {{ message }}
        </p>
      </div>
      <div class="snack-bar-actions">
        <button
          color="primary"
          mat-button
          class="copy-button"
          [cdkCopyToClipboard]="formatMessages()">Copy</button>
        <button
          color="primary"
          mat-button
          class="close-button"
          (click)="snackBarRef.dismiss()">Close</button>
      </div>
    </div>
  `,
  styles: [
    `
      .snack-bar-container {
        display: flex;
        flex-direction: column;
        white-space: pre-line;
      }
      .message-container {
        display: flex;
        flex-direction: column;
        white-space: pre-line;
        max-height: 200px;
        overflow-y: auto;
      }
      .message {
        padding-block-end: 4px;
      }
      .snack-bar-actions {
        display: flex;
        justify-content: center;
      }
    `,
  ],
})
export class SnackBarComponent {
  constructor(
    @Inject(MatSnackBarRef)
    public snackBarRef: MatSnackBarRef<SnackBarComponent>,
    @Inject(MAT_SNACK_BAR_DATA) public messages: string[],
    @Inject(ElementRef) public elementRef: ElementRef,
  ) {}

  formatMessages(): string {
    return this.messages.join('\n\n');
  }
}
