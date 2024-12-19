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

import {NgTemplateOutlet} from '@angular/common';
import {
  Component,
  ElementRef,
  EventEmitter,
  Inject,
  Input,
  Output,
  ViewChild,
} from '@angular/core';
import {FormControl, Validators} from '@angular/forms';
import {assertDefined} from 'common/assert_utils';
import {Analytics} from 'logging/analytics';

@Component({
  selector: 'active-search',
  template: `
    <span class="header">
      <span class="mat-body-2"> {{label}} </span>
      <button
        mat-button
        class="query-button end-align-button clear-button"
        color="primary"
        (click)="clearQueryClick.emit()"
        *ngIf="canClear">
        <mat-icon> delete </mat-icon>
        <span> Clear </span>
      </button>
    </span>
    <mat-form-field appearance="outline" class="query-field padded-field">
      <textarea matInput [formControl]="searchQueryControl" (keydown)="onTextAreaKeydown($event)" [readonly]="runningQuery"></textarea>
      <mat-error *ngIf="searchQueryControl.invalid && searchQueryControl.value">Enter valid SQL query.</mat-error>
    </mat-form-field>

    <div class="query-actions">
      <div *ngIf="runningQuery" class="running-query-message">
        <mat-icon class="material-symbols-outlined"> timer </mat-icon>
        <span class="mat-body-2 message-with-spinner">
          <span>Calculating results </span>
          <mat-spinner [diameter]="20"></mat-spinner>
        </span>
      </div>
      <span *ngIf="lastQueryExecutionTime" class="query-execution-time mat-body-1">
       Executed in {{lastQueryExecutionTime}}
      </span>
      <button
        mat-flat-button
        class="query-button search-button"
        color="primary"
        (click)="onSearchQueryClick()"
        [disabled]="searchQueryDisabled()"> Run Search Query </button>
    </div>
    <div class="current-search" *ngIf="executedQuery">
      <span class="query">
        <span class="mat-body-2"> Last executed: </span>
        <span class="mat-body-1"> {{executedQuery}} </span>
      </span>
      <ng-container
        *ngIf="!lastTraceFailed"
        [ngTemplateOutlet]="saveQueryField"
        [ngTemplateOutletContext]="{query: executedQuery, control: saveQueryNameControl}"></ng-container>
    </div>
    <button
      *ngIf="canAdd"
      [disabled]="!executedQuery || lastTraceFailed"
      mat-stroked-button
      class="query-button add-button"
      color="primary"
      (click)="addQueryClick.emit()"> + Add Query </button>
  `,
  styles: [
    `
      .header {
        justify-content: space-between;
        display: flex;
        align-items: center;
      }
      .query-field {
        height: fit-content;
      }
      .query-field textarea {
        height: 300px;
      }
      .query-button {
        width: fit-content;
        line-height: 24px;
        padding: 0 10px;
      }
      .end-align-button {
        align-self: end;
      }
      .query-actions {
        display: flex;
        flex-direction: row;
        justify-content: end;
        column-gap: 10px;
        align-items: center;
      }
      .running-query-message {
        display: flex;
        flex-direction: row;
        align-items: center;
        color: #FF8A00;
      }
      .current-search {
        padding: 10px 0px;
      }
      .current-search .query {
        display: flex;
        flex-direction: column;
      }
      .message-with-spinner {
        display: flex;
        flex-direction: row;
        align-items: center;
        justify-content: space-between;
      }
    `,
  ],
})
export class ActiveSearchComponent {
  @Input() canClear = false;
  @Input() canAdd = false;
  @Input() isSearchInitialized = false;
  @Input() lastTraceFailed = false;
  @Input() executedQuery: string | undefined;
  @Input() saveQueryField: NgTemplateOutlet | undefined;
  @Input() label: string | undefined;
  @Input() lastQueryExecutionTime: string | undefined;
  @Input() saveQueryNameControl: FormControl | undefined;
  @Input() runningQuery = false;

  @Output() clearQueryClick = new EventEmitter();
  @Output() searchQueryClick = new EventEmitter<string>();
  @Output() addQueryClick = new EventEmitter();

  @ViewChild(HTMLTextAreaElement) textArea: HTMLTextAreaElement | undefined;

  searchQueryControl = new FormControl('', Validators.required);

  constructor(
    @Inject(ElementRef) readonly elementRef: ElementRef<HTMLElement>,
  ) {}

  updateText(text: string) {
    this.searchQueryControl.setValue(text);
    this.textArea?.focus();
  }

  searchQueryDisabled(): boolean {
    return (
      this.searchQueryControl.invalid ||
      this.runningQuery ||
      !this.isSearchInitialized
    );
  }

  onTextAreaKeydown(event: KeyboardEvent) {
    event.stopPropagation();
    if (
      event.key === 'Enter' &&
      !event.shiftKey &&
      !this.searchQueryDisabled()
    ) {
      event.preventDefault();
      this.onSearchQueryClick();
    }
  }

  onSearchQueryClick() {
    Analytics.TraceSearch.logQueryRequested('new');
    this.searchQueryClick.emit(assertDefined(this.searchQueryControl.value));
  }
}
