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
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {currentElementStyle} from 'viewers/components/styles/current_element.styles';
import {selectedElementStyle} from 'viewers/components/styles/selected_element.styles';
import {timeButtonStyle} from 'viewers/components/styles/timestamp_button.styles';
import {Events} from './events';
import {UiData} from './ui_data';

@Component({
  selector: 'viewer-protolog',
  template: `
    <div class="card-grid container">
      <div class="filters">
        <div class="log-level">
          <select-with-filter
            label="Log level"
            flex="3"
            [options]="uiData.allLogLevels"
            outerFilterWidth="225"
            (selectChange)="onLogLevelsChange($event)">
          </select-with-filter>
        </div>
        <div class="tag">
          <select-with-filter
            label="Tags"
            flex="2"
            [options]="uiData.allTags"
            outerFilterWidth="150"
            innerFilterWidth="150"
            (selectChange)="onTagsChange($event)">
          </select-with-filter>
        </div>
        <div class="source-file">
          <select-with-filter
            label="Source files"
            flex="4"
            [options]="uiData.allSourceFiles"
            outerFilterWidth="300"
            innerFilterWidth="300"
            (selectChange)="onSourceFilesChange($event)">
          </select-with-filter>
        </div>
        <div class="text">
          <mat-form-field appearance="fill" (keydown.enter)="$event.target.blur()">
            <mat-label>Search text</mat-label>
            <input matInput name="protologTextInput" [(ngModel)]="searchString" (input)="onSearchStringChange()" />
          </mat-form-field>
        </div>

        <button
          color="primary"
          mat-stroked-button
          class="go-to-current-time"
          (click)="onGoToCurrentTimeClick()">
          Go to Current Time
        </button>
      </div>
      <cdk-virtual-scroll-viewport
        protologVirtualScroll
        class="scroll-messages"
        [scrollItems]="uiData.messages">
        <div
          *cdkVirtualFor="let message of uiData.messages; let i = index"
          class="message"
          [attr.item-id]="i"
          [class.current]="isCurrentMessage(i)"
          [class.selected]="isSelectedMessage(i)"
          (click)="onMessageClicked(i)">
          <div class="time">
            <button
              mat-button
              [color]="isCurrentMessage(i) ? 'secondary' : 'primary'"
              (click)="onTimestampClicked(message.time)">
              {{ message.time.formattedValue() }}
            </button>
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

      .filters .text mat-form-field {
        width: 80%;
      }

      .go-to-current-time {
        margin-top: 4px;
        font-size: 12px;
        height: 65%;
        width: fit-content;
      }

      .filters div {
        margin: 4px;
      }

      .message div {
        margin: 4px;
      }

      mat-form-field {
        width: 100%;
        font-size: 12px;
      }
    `,
    selectedElementStyle,
    currentElementStyle,
    timeButtonStyle,
  ],
})
export class ViewerProtologComponent {
  uiData: UiData = UiData.EMPTY;

  private searchString = '';
  private lastClicked = '';
  private lastSelectedMessage: undefined | number;

  @ViewChild(CdkVirtualScrollViewport)
  scrollComponent?: CdkVirtualScrollViewport;

  constructor(@Inject(ElementRef) private elementRef: ElementRef) {}

  @Input()
  set inputData(data: UiData) {
    this.uiData = data;
    if (
      this.lastSelectedMessage === undefined &&
      this.uiData.currentMessageIndex !== undefined &&
      this.scrollComponent &&
      this.lastClicked !==
        this.uiData.messages[
          this.uiData.currentMessageIndex
        ].time.formattedValue()
    ) {
      this.scrollComponent.scrollToIndex(this.uiData.currentMessageIndex);
    }

    this.lastSelectedMessage = undefined;
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

  onGoToCurrentTimeClick() {
    if (this.uiData.currentMessageIndex !== undefined && this.scrollComponent) {
      this.scrollComponent.scrollToIndex(this.uiData.currentMessageIndex);
    }
  }

  onTimestampClicked(timestamp: PropertyTreeNode) {
    this.lastClicked = timestamp.formattedValue();
    this.emitEvent(ViewerEvents.TimestampClick, timestamp);
  }

  onMessageClicked(index: number) {
    this.lastSelectedMessage = index;
    this.emitEvent(Events.MessageClicked, index);
  }

  isCurrentMessage(index: number): boolean {
    return index === this.uiData.currentMessageIndex;
  }

  isSelectedMessage(index: number): boolean {
    return index === this.uiData.selectedMessageIndex;
  }

  private emitEvent(event: string, data: any) {
    const customEvent = new CustomEvent(event, {
      bubbles: true,
      detail: data,
    });
    this.elementRef.nativeElement.dispatchEvent(customEvent);
  }
}
