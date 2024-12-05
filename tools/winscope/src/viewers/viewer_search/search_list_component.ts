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
        [matTooltip]="getTooltip(search)"> {{search.name}} </span>
      <div class="listed-search-date-options">
        <span> {{formatTimeMs(search.timeMs)}} </span>
        <button
          mat-icon-button
          class="listed-search-options"
          *ngIf="menuOptions.length > 0"
          [class.force-show]="searchOptionsTarget === search"
          (click)="searchOptionsTarget = search"
          [cdkMenuTriggerFor]="optionsMenu">
          <mat-icon> more_vert </mat-icon>
        </button>

        <ng-template #optionsMenu>
          <div class="context-menu" cdkMenu>
            <div class="context-menu-item-container">
              <ng-container *ngFor="let opt of menuOptions">
                <span
                  *ngIf="opt.innerMenu"
                  class="context-menu-item"
                  cdkMenuItem
                  [cdkMenuTriggerFor]="innerMenu"> {{opt.name}} </span>
                <span
                  *ngIf="!opt.innerMenu"
                  class="context-menu-item"
                  (click)="opt.onClickCallback(search)"
                  cdkMenuItem> {{opt.name}} </span>

                  <ng-template #innerMenu>
                    <div class="context-menu inner-menu" cdkMenu>
                      <div class="context-menu-item-container">
                        <span class="context-menu-item" [cdkMenuItemDisabled]="true" cdkMenuItem>
                          <ng-container
                            [ngTemplateOutlet]="opt.innerMenu"
                            [ngTemplateOutletContext]="{search}"></ng-container>
                        </span>
                      </div>
                    </div>
                  </ng-template>
              </ng-container>
            </div>
          </div>
        </ng-template>
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
      .listed-search:not(:hover) .listed-search-options:not(.force-show) {
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
      }
      .listed-search-options {
        width: fit-content;
        cursor: pointer;
      }
    `,
  ],
})
export class SearchListComponent {
  @Input() searches: Search[] = [];
  @Input() placeholderText = '';
  @Input() menuOptions: MenuOption[] = [];

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

export interface MenuOption {
  name: string;
  onClickCallback: (search: Search) => void;
  innerMenu?: NgTemplateOutlet;
}
