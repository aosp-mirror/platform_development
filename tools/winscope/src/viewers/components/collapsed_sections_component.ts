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
import {CollapsibleSections} from 'viewers/common/collapsible_sections';
import {CollapsibleSectionType} from 'viewers/common/collapsible_section_type';

@Component({
  selector: 'collapsed-sections',
  template: `
      <ng-container *ngFor="let section of sections.getCollapsedSections()">
        <span class="collapsed-section" (click)="onCollapsedSectionClick(section.type)">
            <span class="collapsed-section-text"> {{section.label.toUpperCase()}} </span>
            <button mat-icon-button> <mat-icon> arrow_right </mat-icon> </button>
        </span>
      </ng-container>
    `,
  styles: [
    `
      :host {
          font: 12px 'Roboto', sans-serif;
          font-weight: bold;
          margin: 4px 4px 4px 0px;
      }
      :host.empty {
          display: none;
      }
      .collapsed-section {
          cursor: pointer;
          padding-top: 5px;
          margin-bottom: 4px;
          background-color: var(--side-bar-color);
          color: var(--contrast-text-color);
          border-radius: 0px 4px 4px 0px;
          display: flex;
          flex-direction: column;
          align-items: center;
      }
      .collapsed-section-text {
          rotate: 180deg;
          writing-mode: vertical-lr;
      }
      .mat-icon-button {
          height: 22px;
          width: 22px;
      }
      .mat-icon {
          font-size: 22px;
          width: 22px;
          height: 22px;
          line-height: 22px;
          display: flex;
      }
    `,
  ],
})
export class CollapsedSectionsComponent {
  @Input() sections: CollapsibleSections | undefined;
  @Output() sectionChange = new EventEmitter<CollapsibleSectionType>();

  onCollapsedSectionClick(sectionType: CollapsibleSectionType) {
    this.sectionChange.emit(sectionType);
  }
}
