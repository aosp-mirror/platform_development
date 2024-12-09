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
import {Component, ElementRef, Inject, Input, ViewChild} from '@angular/core';
import {FormControl, ValidationErrors, Validators} from '@angular/forms';
import {assertDefined} from 'common/assert_utils';
import {TimeDuration} from 'common/time_duration';
import {TIME_UNIT_TO_NANO} from 'common/time_units';
import {Analytics} from 'logging/analytics';
import {TraceType} from 'trace/trace_type';
import {CollapsibleSections} from 'viewers/common/collapsible_sections';
import {CollapsibleSectionType} from 'viewers/common/collapsible_section_type';
import {
  DeleteSavedQueryClickDetail,
  QueryClickDetail,
  SaveQueryClickDetail,
  ViewerEvents,
} from 'viewers/common/viewer_events';
import {timeButtonStyle} from 'viewers/components/styles/clickable_property.styles';
import {logComponentStyles} from 'viewers/components/styles/log_component.styles';
import {
  viewerCardInnerStyle,
  viewerCardStyle,
} from 'viewers/components/styles/viewer_card.styles';
import {MenuOption} from './search_list_component';
import {Search, UiData} from './ui_data';

@Component({
  selector: 'viewer-search',
  template: `
    <div class="card-grid" *ngIf="inputData">
      <collapsed-sections
        [class.empty]="sections.areAllSectionsExpanded()"
        [sections]="sections"
        (sectionChange)="sections.onCollapseStateChange($event, false)">
      </collapsed-sections>

      <div
        class="global-search"
        [class.collapsed]="sections.isSectionCollapsed(CollapsibleSectionType.GLOBAL_SEARCH)"
        (click)="onGlobalSearchClick($event)">
        <div class="title-section">
          <collapsible-section-title
            class="padded-title"
            [title]="CollapsibleSectionType.GLOBAL_SEARCH"
            (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.GLOBAL_SEARCH, true)"></collapsible-section-title>
            <span class="mat-body-2 message-with-spinner" *ngIf="initializing">
              <span>Initializing</span>
              <mat-spinner [diameter]="20"></mat-spinner>
            </span>
        </div>

          <mat-tab-group class="search-tabs">
            <mat-tab label="Search">
             <div class="body">
                <span class="mat-body-2">
                  {{globalSearchText}}
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
                  <div *ngIf="lastQueryExecutionTime" class="query-execution-time">
                    <span class="mat-body-1">
                      Executed in {{lastQueryExecutionTime}}
                    </span>
                  </div>
                  <button
                    mat-flat-button
                    class="query-button"
                    color="primary"
                    (click)="onSearchQueryClick()"
                    [disabled]="searchQueryDisabled()"> Run Search Query </button>
                </div>
                <div class="current-search" *ngFor="let search of inputData.currentSearches">
                  <span class="query">
                    <span class="mat-body-2"> Current: </span>
                    <span class="mat-body-1"> {{search.query}} </span>
                  </span>
                  <ng-container
                    [ngTemplateOutlet]="saveQueryField"
                    [ngTemplateOutletContext]="{search}"></ng-container>
                </div>
              </div>
            </mat-tab>

            <mat-tab label="Saved">
              <search-list
                class="body"
                [searches]="inputData.savedSearches"
                placeholderText="Saved queries will appear here."
                [menuOptions]="savedSearchMenuOptions"></search-list>
            </mat-tab>

            <mat-tab label="Recent">
              <search-list
                class="body"
                [searches]="inputData.recentSearches"
                placeholderText="Recent queries will appear here."
                [menuOptions]="recentSearchMenuOptions"></search-list>
            </mat-tab>

            <ng-template #saveQueryField let-search="search">
              <div class="outline-field save-field">
                <mat-form-field appearance="outline">
                  <input matInput [formControl]="saveQueryNameControl" (keydown.enter)="onSaveQueryClick(search.query)"/>
                  <mat-error *ngIf="saveQueryNameControl.invalid && saveQueryNameControl.value">Query with that name already exists.</mat-error>
                </mat-form-field>
                <button
                  mat-flat-button
                  class="query-button"
                  color="primary"
                  [disabled]="saveQueryNameControl.invalid"
                  (click)="onSaveQueryClick(search.query)"> Save Query </button>
              </div>
            </ng-template>
          </mat-tab-group>
      </div>

      <div
        class="search-results"
        [class.collapsed]="sections.isSectionCollapsed(CollapsibleSectionType.SEARCH_RESULTS)">
        <div class="title-section">
          <collapsible-section-title
            class="padded-title"
            [title]="CollapsibleSectionType.SEARCH_RESULTS"
            (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.SEARCH_RESULTS, true)"></collapsible-section-title>
        </div>
        <div class="result" *ngFor="let search of inputData.currentSearches">
          <div class="results-table">
            <log-view
              class="results-log-view"
              [entries]="search.entries"
              [headers]="search.headers"
              [selectedIndex]="search.selectedIndex"
              [scrollToIndex]="search.scrollToIndex"
              [currentIndex]="search.currentIndex"
              [traceType]="${TraceType.SEARCH}"
              [showTraceEntryTimes]="false"
              [showCurrentTimeButton]="false"></log-view>
          </div>
        </div>
      </div>

      <div
        class="how-to-search"
        [class.collapsed]="sections.isSectionCollapsed(CollapsibleSectionType.HOW_TO_SEARCH)">
        <div class="title-section">
        <collapsible-section-title
          class="padded-title"
          [title]="CollapsibleSectionType.HOW_TO_SEARCH"
          (collapseButtonClicked)="sections.onCollapseStateChange(CollapsibleSectionType.HOW_TO_SEARCH, true)"></collapsible-section-title>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .search-tabs {
        height: 100%;
      }
      .global-search .body {
        display: flex;
        flex-direction: column;
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

      .result, .results-table {
        height: 100%;
        display: flex;
        flex-direction: column;
      }
      .results-log-view {
        display: flex;
        flex-direction: column;
        overflow: auto;
        border-radius: 4px;
        background-color: var(--background-color);
        flex: 1;
      }
    `,
    viewerCardStyle,
    viewerCardInnerStyle,
    logComponentStyles,
    timeButtonStyle,
  ],
})
export class ViewerSearchComponent {
  @Input() inputData: UiData | undefined;
  @ViewChild('saveQueryField') saveQueryField: NgTemplateOutlet | undefined;

  CollapsibleSectionType = CollapsibleSectionType;
  sections = new CollapsibleSections([
    {
      type: CollapsibleSectionType.GLOBAL_SEARCH,
      label: CollapsibleSectionType.GLOBAL_SEARCH,
      isCollapsed: false,
    },
    {
      type: CollapsibleSectionType.SEARCH_RESULTS,
      label: CollapsibleSectionType.SEARCH_RESULTS,
      isCollapsed: false,
    },
    {
      type: CollapsibleSectionType.HOW_TO_SEARCH,
      label: CollapsibleSectionType.HOW_TO_SEARCH,
      isCollapsed: false,
    },
  ]);
  searchQueryControl = new FormControl('', Validators.required);
  saveQueryNameControl = new FormControl(
    '',
    assertDefined(
      Validators.compose([
        Validators.required,
        (control: FormControl) =>
          this.validateSearchQuerySaveName(
            control,
            this.inputData?.savedSearches ?? [],
          ),
      ]),
    ),
  );
  runningQuery: string | undefined;
  lastQueryExecutionTime: string | undefined;
  lastQueryStartTime: number | undefined;
  initializing = false;
  readonly savedSearchMenuOptions: MenuOption[] = [
    {
      name: 'Run Query',
      onClickCallback: (search: Search) => {
        Analytics.TraceSearch.logQueryRequested('saved');
        this.onRunQueryFromOptionsClick(search);
      },
    },
    {
      name: 'Delete Query',
      onClickCallback: (search: Search) => this.onDeleteQueryClick(search),
    },
  ];
  readonly recentSearchMenuOptions: MenuOption[] = [
    {
      name: 'Run Query',
      onClickCallback: (search: Search) => {
        Analytics.TraceSearch.logQueryRequested('recent');
        this.onRunQueryFromOptionsClick(search);
      },
    },
    {name: 'Save Query', onClickCallback: (search: Search) => {}},
  ];

  readonly globalSearchText = `
     Write an SQL query in the field below, and run the search. \
     Results will be shown in a tabular view and you can optionally visualize them in the timeline. \
  `;

  constructor(
    @Inject(ElementRef) private elementRef: ElementRef<HTMLElement>,
  ) {}

  ngAfterViewInit() {
    this.recentSearchMenuOptions[1].innerMenu = this.saveQueryField;
  }

  ngOnChanges() {
    if (this.initializing && this.inputData?.initialized) {
      this.initializing = false;
    }
    const runningQueryComplete = this.inputData?.currentSearches.some(
      (search) => search.query === this.runningQuery,
    );
    if (
      this.runningQuery &&
      (runningQueryComplete || this.inputData?.lastTraceFailed)
    ) {
      if (runningQueryComplete) {
        this.searchQueryControl.setValue(this.runningQuery);
        this.saveQueryNameControl.setValue(this.runningQuery);
      }
      const executionTimeMs =
        Date.now() - assertDefined(this.lastQueryStartTime);
      Analytics.TraceSearch.logQueryExecutionTime(executionTimeMs);
      this.lastQueryExecutionTime = new TimeDuration(
        BigInt(executionTimeMs * TIME_UNIT_TO_NANO.ms),
      ).format();
      this.lastQueryStartTime = undefined;
      this.runningQuery = undefined;
    }
  }

  onGlobalSearchClick() {
    if (!this.initializing && !this.inputData?.initialized) {
      this.initializing = true;
      const event = new CustomEvent(ViewerEvents.GlobalSearchSectionClick);
      this.elementRef.nativeElement.dispatchEvent(event);
    }
  }

  onSearchQueryClick() {
    this.runningQuery = assertDefined(this.searchQueryControl.value);
    Analytics.TraceSearch.logQueryRequested('new');
    this.dispatchSearchQueryEvent();
  }

  onSaveQueryClick(query: string) {
    if (this.saveQueryNameControl.invalid) {
      return;
    }
    const event = new CustomEvent(ViewerEvents.SaveQueryClick, {
      detail: new SaveQueryClickDetail(
        query,
        assertDefined(this.saveQueryNameControl.value),
      ),
    });
    this.elementRef.nativeElement.dispatchEvent(event);
    Analytics.TraceSearch.logQuerySaved();
    this.saveQueryNameControl.reset();
  }

  onRunQueryFromOptionsClick(search: Search) {
    this.runningQuery = search.query;
    this.dispatchSearchQueryEvent();
  }

  onDeleteQueryClick(search: Search) {
    const event = new CustomEvent(ViewerEvents.DeleteSavedQueryClick, {
      detail: new DeleteSavedQueryClickDetail(search),
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  searchQueryDisabled(): boolean {
    return (
      this.searchQueryControl.invalid ||
      !!this.runningQuery ||
      !this.inputData?.initialized
    );
  }

  currentSearchPresent(): boolean {
    return (this.inputData?.currentSearches.length ?? 0) > 0;
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

  private validateSearchQuerySaveName(
    control: FormControl,
    savedSearches: Search[],
  ): ValidationErrors | null {
    const valid =
      control.value &&
      !savedSearches.some((search) => search.name === control.value);
    return !valid ? {invalidInput: control.value} : null;
  }

  private dispatchSearchQueryEvent() {
    this.lastQueryExecutionTime = undefined;
    this.lastQueryStartTime = Date.now();
    const event = new CustomEvent(ViewerEvents.SearchQueryClick, {
      detail: new QueryClickDetail(assertDefined(this.runningQuery)),
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }
}
