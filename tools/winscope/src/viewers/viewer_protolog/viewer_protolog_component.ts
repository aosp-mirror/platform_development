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
import {CdkVirtualScrollViewport} from '@angular/cdk/scrolling';
import {Component, ElementRef, Inject, Input, ViewChild} from '@angular/core';
import {MatSelectChange} from '@angular/material/select';
import {Events} from './events';
import {UiData} from './ui_data';

@Component({
  selector: 'viewer-protolog',
  template: `
    <div class="card-grid container">
      <div class="filters">
        <div class="log-level">
          <mat-form-field appearance="fill">
            <mat-label>Log level</mat-label>
            <mat-select (selectionChange)="onLogLevelsChange($event)" multiple>
              <mat-option *ngFor="let level of uiData.allLogLevels" [value]="level">
                {{ level }}
              </mat-option>
            </mat-select>
          </mat-form-field>
        </div>
        <div class="tag">
          <mat-form-field appearance="fill">
            <mat-label>Tags</mat-label>
            <mat-select (selectionChange)="onTagsChange($event)" multiple>
              <mat-option *ngFor="let tag of uiData.allTags" [value]="tag">
                {{ tag }}
              </mat-option>
            </mat-select>
          </mat-form-field>
        </div>
        <div class="source-file">
          <mat-form-field appearance="fill">
            <mat-label>Source files</mat-label>
            <mat-select (selectionChange)="onSourceFilesChange($event)" multiple>
              <mat-option *ngFor="let file of uiData.allSourceFiles" [value]="file">
                {{ file }}
              </mat-option>
            </mat-select>
          </mat-form-field>
        </div>
        <div class="text">
          <mat-form-field appearance="fill">
            <mat-label>Search text</mat-label>
            <input matInput [(ngModel)]="searchString" (input)="onSearchStringChange()" />
          </mat-form-field>
        </div>
      </div>
      <cdk-virtual-scroll-viewport itemSize="16" class="scroll-messages">
        <div
          *cdkVirtualFor="let message of uiData.messages; let i = index"
          class="message"
          [class.current-message]="isCurrentMessage(i)">
          <div class="time">
            <span class="mat-body-1">{{ message.time }}</span>
          </div>
          <div class="log-level">
            <span class="mat-body-1">{{ message.level }}</span>
          </div>
          <div class="tag">
            <span class="mat-body-1">{{ message.tag }}</span>
          </div>
          <div class="source-file">
            <span class="mat-body-1">{{ message.at }}</span>
          </div>
          <div class="text">
            <span class="mat-body-1">{{ message.text }}</span>
          </div>
        </div>
      </cdk-virtual-scroll-viewport>
    </div>
  `,
  styles: [
    `
      .container {
        padding: 16px;
        box-sizing: border-box;
        display: flex;
        flex-direction: column;
      }

      .filters {
        display: flex;
        flex-direction: row;
        margin-top: 16px;
      }

      .scroll-messages {
        height: 100%;
        flex: 1;
      }

      .message {
        display: flex;
        flex-direction: row;
        overflow-wrap: anywhere;
      }

      .message.current-message {
        background-color: #365179;
        color: white;
      }

      .time {
        flex: 2;
      }

      .log-level {
        flex: 1;
      }

      .filters .log-level {
        flex: 3;
      }

      .tag {
        flex: 2;
      }

      .source-file {
        flex: 4;
      }

      .text {
        flex: 10;
      }

      .filters div {
        margin: 4px;
      }

      .message div {
        margin: 4px;
      }

      mat-form-field {
        width: 100%;
      }
    `,
  ],
})
export class ViewerProtologComponent {
  constructor(@Inject(ElementRef) elementRef: ElementRef) {
    this.elementRef = elementRef;
  }

  @Input()
  set inputData(data: UiData) {
    this.uiData = data;
    if (this.uiData.currentMessageIndex !== undefined && this.scrollComponent) {
      this.scrollComponent.scrollToIndex(this.uiData.currentMessageIndex);
    }
  }

  onLogLevelsChange(event: MatSelectChange) {
    this.emitEvent(Events.LogLevelsFilterChanged, event.value);
  }

  onTagsChange(event: MatSelectChange) {
    this.emitEvent(Events.TagsFilterChanged, event.value);
  }

  onSourceFilesChange(event: MatSelectChange) {
    this.emitEvent(Events.SourceFilesFilterChanged, event.value);
  }

  onSearchStringChange() {
    this.emitEvent(Events.SearchStringFilterChanged, this.searchString);
  }

  isCurrentMessage(index: number): boolean {
    return index === this.uiData.currentMessageIndex;
  }

  private emitEvent(event: string, data: any) {
    const customEvent = new CustomEvent(event, {
      bubbles: true,
      detail: data,
    });
    this.elementRef.nativeElement.dispatchEvent(customEvent);
  }

  @ViewChild(CdkVirtualScrollViewport) scrollComponent!: CdkVirtualScrollViewport;

  uiData: UiData = UiData.EMPTY;
  private searchString = '';
  private elementRef: ElementRef;
}
