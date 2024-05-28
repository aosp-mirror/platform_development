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
  ElementRef,
  EventEmitter,
  Inject,
  Input,
  Output,
} from '@angular/core';
import {assertDefined} from 'common/assert_utils';
import {PersistentStore} from 'common/persistent_store';
import {Analytics} from 'logging/analytics';
import {TRACE_INFO} from 'trace/trace_info';
import {TraceType} from 'trace/trace_type';
import {CollapsibleSectionType} from 'viewers/common/collapsible_section_type';
import {CuratedProperties} from 'viewers/common/curated_properties';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {UserOption, UserOptions} from 'viewers/common/user_options';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {nodeStyles} from 'viewers/components/styles/node.styles';
import {searchBoxStyle} from './styles/search_box.styles';
import {userOptionStyle} from './styles/user_option.styles';
import {viewerCardInnerStyle} from './styles/viewer_card.styles';

@Component({
  selector: 'properties-view',
  template: `
    <div class="view-header">
      <div class="title-section">
       <collapsible-section-title
          class="properties-title"
          [class.padded-title]="!hasUserOptions()"
          [title]="title"
          (collapseButtonClicked)="collapseButtonClicked.emit()"></collapsible-section-title>

        <mat-form-field *ngIf="showFilter" class="search-box" (keydown.enter)="$event.target.blur()">
          <mat-label>Search</mat-label>

          <input matInput [(ngModel)]="filterString" (ngModelChange)="filterTree()" name="filter" />
        </mat-form-field>
      </div>

      <div class="view-controls" *ngIf="hasUserOptions()">
        <button
          *ngFor="let option of objectKeys(userOptions)"
          mat-flat-button
          [color]="getUserOptionButtonColor(userOptions[option])"
          [disabled]="userOptions[option].isUnavailable"
          [class.not-enabled]="!userOptions[option].enabled"
          class="user-option"
          [style.cursor]="'pointer'"
          (click)="onUserOptionChange(userOptions[option])">
          <span class="user-option-label" [class.with-chip]="!!userOptions[option].chip">
            <span> {{userOptions[option].name}} </span>
            <div *ngIf="userOptions[option].chip" class="user-option-chip"> {{userOptions[option].chip.short}} </div>
            <mat-icon  class="material-symbols-outlined" *ngIf="userOptions[option].icon"> {{userOptions[option].icon}} </mat-icon>
          </span>
        </button>
      </div>
    </div>

    <mat-divider *ngIf="hasUserOptions()"></mat-divider>

    <ng-container *ngIf="showViewCaptureFormat()">
      <view-capture-property-groups
        class="property-groups"
        [properties]="curatedProperties"></view-capture-property-groups>

      <mat-divider *ngIf="showPropertiesTree()"></mat-divider>
    </ng-container>

    <div *ngIf="showPropertiesTree()" class="properties-content">
      <div class="tree-wrapper">
        <tree-view
          [node]="propertiesTree"
          [useStoredExpandedState]="!!store"
          [itemsClickable]="true"
          [highlightedItem]="highlightedProperty"
          (highlightedChange)="onHighlightedPropertyChange($event)"></tree-view>
      </div>
    </div>

    <span class="mat-body-1 placeholder-text" *ngIf="!showPropertiesTree() && placeholderText"> {{ placeholderText }} </span>
  `,
  styles: [
    `
      .view-header {
        display: flex;
        flex-direction: column;
      }

      .padded-title {
        padding-bottom: 8px;
      }

      .property-groups {
        overflow-y: auto;
      }

      .properties-content {
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow-y: auto;
        padding: 0px 12px;
      }

      .placeholder-text {
        padding: 8px 12px;
      }
    `,
    nodeStyles,
    searchBoxStyle,
    userOptionStyle,
    viewerCardInnerStyle,
  ],
})
export class PropertiesComponent {
  CollapsibleSectionType = CollapsibleSectionType;
  objectKeys = Object.keys;
  filterString = '';

  @Input() title = 'PROPERTIES';
  @Input() showFilter = true;
  @Input() userOptions: UserOptions = {};
  @Input() placeholderText = '';
  @Input() propertiesTree: UiPropertyTreeNode | undefined;
  @Input() highlightedProperty = '';
  @Input() curatedProperties: CuratedProperties | undefined;
  @Input() displayPropertyGroups = false;
  @Input() isProtoDump = false;
  @Input() traceType: TraceType | undefined;
  @Input() store: PersistentStore | undefined;

  @Output() collapseButtonClicked = new EventEmitter();

  constructor(@Inject(ElementRef) private elementRef: ElementRef) {}

  filterTree() {
    const event = new CustomEvent(ViewerEvents.PropertiesFilterChange, {
      bubbles: true,
      detail: {filterString: this.filterString},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  onHighlightedPropertyChange(newNode: UiPropertyTreeNode) {
    const event = new CustomEvent(ViewerEvents.HighlightedPropertyChange, {
      bubbles: true,
      detail: {id: newNode.id},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  onUserOptionChange(option: UserOption) {
    option.enabled = !option.enabled;
    Analytics.Navigation.logPropertiesSettingsChanged(
      option.name,
      option.enabled,
      TRACE_INFO[assertDefined(this.traceType)].name,
    );
    const event = new CustomEvent(ViewerEvents.PropertiesUserOptionsChange, {
      bubbles: true,
      detail: {userOptions: this.userOptions},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  hasUserOptions() {
    return this.objectKeys(this.userOptions).length > 0;
  }

  showViewCaptureFormat(): boolean {
    return (
      this.traceType === TraceType.VIEW_CAPTURE &&
      this.filterString === '' &&
      // Todo: Highlight Inline in formatted ViewCapture Properties Component.
      !this.userOptions['showDiff']?.enabled &&
      this.curatedProperties !== undefined
    );
  }

  showPropertiesTree(): boolean {
    return !!this.propertiesTree && !this.showViewCaptureFormat();
  }

  getUserOptionButtonColor(option: UserOption) {
    return option.enabled ? 'primary' : undefined;
  }
}
