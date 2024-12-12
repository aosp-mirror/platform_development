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
import {Component, Input} from '@angular/core';
import {Search} from './ui_data';

@Component({
  selector: 'search-list',
  template: `
    <span class="mat-body-1" *ngIf="searches.length === 0">
      {{placeholderText}}
    </span>
    <div class="listed-search" *ngFor="let search of searches">
      <span
        #searchName
        class="mat-body-2 listed-search-name"
        [matTooltipDisabled]="!showTooltip(search, searchName)"
        matTooltipPosition="right"
        [matTooltip]="getTooltip(search)"> {{search.name}} </span>
      <div class="listed-search-date-options">
        <ng-container *ngFor="let opt of listItemOptions">
          <button
            mat-icon-button
            class="listed-search-option"
            *ngIf="opt.onClickCallback"
            [matTooltip]="opt.name"
            [matTooltipShowDelay]="500"
            (click)="opt.onClickCallback(search)">
            <mat-icon class="material-symbols-outlined">{{opt.icon}}</mat-icon>
          </button>
          <button
            mat-icon-button
            class="listed-search-option"
            *ngIf="opt.menu"
            [matTooltip]="opt.name"
            [matTooltipShowDelay]="500"
            (click)="searchOptionsTarget = search"
            [class.force-show]="searchOptionsTarget === search"
            [cdkMenuTriggerFor]="optionsMenu">
            <mat-icon class="material-symbols-outlined">{{opt.icon}}</mat-icon>
          </button>

          <ng-template #optionsMenu>
            <div class="context-menu" (closed)="searchOptionsTarget = undefined" cdkMenu>
              <div class="context-menu-item-container">
                <span class="context-menu-item" [cdkMenuItemDisabled]="true" cdkMenuItem>
                  <ng-container
                    [ngTemplateOutlet]="opt.menu"
                    [ngTemplateOutletContext]="{search}"></ng-container>
                </span>
              </div>
            </div>
          </ng-template>
        </ng-container>

        <span> {{formatTimeMs(search.timeMs)}} </span>
      </div>
    </div>
  `,
  styles: [
    `
      .listed-search {
        display: flex;
        flex-direction: row;
        align-items: center;
        justify-content: space-between;
      }
      .listed-search {
        width: 100%;
        column-gap: 10px;
      }
      .listed-search:hover {
          background-color: var(--hover-element-color);
      }
      .listed-search:not(:hover) .listed-search-option:not(.force-show) {
        visibility: hidden;
      }
      .listed-search-name {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      .listed-search-date-options {
        display: flex;
        flex-direction: row;
        align-items: center;
        white-space: pre-line;
        text-align: right;
      }
      .listed-search-option {
        width: fit-content;
        cursor: pointer;
        transform: scale(0.8);
      }
    `,
  ],
})
export class SearchListComponent {
  @Input() searches: Search[] = [];
  @Input() placeholderText = '';
  @Input() listItemOptions: ListItemOption[] = [];

  searchOptionsTarget: Search | undefined;

  showTooltip(search: Search, el: HTMLElement) {
    return search.name !== search.query || el.scrollWidth > el.offsetWidth;
  }

  getTooltip(search: Search) {
    if (search.name === search.query) return search.query;
    return search.name + ': ' + search.query;
  }

  formatTimeMs(timeMs: number) {
    const time = new Date(timeMs);
    return time.toTimeString().slice(0, 5) + '\n' + time.toLocaleDateString();
  }
}

export interface ListItemOption {
  name: string;
  icon: string;
  onClickCallback?: (search: Search) => void;
  menu?: NgTemplateOutlet;
}
