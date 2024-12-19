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

import {CdkAccordionItem} from '@angular/cdk/accordion';
import {NgTemplateOutlet} from '@angular/common';
import {
  Component,
  ElementRef,
  Inject,
  Input,
  QueryList,
  SimpleChanges,
  ViewChild,
  ViewChildren,
} from '@angular/core';
import {FormControl, ValidationErrors, Validators} from '@angular/forms';
import {MatTabGroup} from '@angular/material/tabs';
import {assertDefined} from 'common/assert_utils';
import {TimeDuration} from 'common/time/time_duration';
import {TIME_UNIT_TO_NANO} from 'common/time/time_units';
import {Analytics} from 'logging/analytics';
import {TraceType} from 'trace/trace_type';
import {CollapsibleSections} from 'viewers/common/collapsible_sections';
import {CollapsibleSectionType} from 'viewers/common/collapsible_section_type';
import {
  AddQueryClickDetail,
  ClearQueryClickDetail,
  DeleteSavedQueryClickDetail,
  SaveQueryClickDetail,
  SearchQueryClickDetail,
  ViewerEvents,
} from 'viewers/common/viewer_events';
import {
  viewerCardInnerStyle,
  viewerCardStyle,
} from 'viewers/components/styles/viewer_card.styles';
import {ActiveSearchComponent} from './active_search_component';
import {ListItemOption} from './search_list_component';
import {CurrentSearch, ListedSearch, UiData} from './ui_data';

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

        <mat-tab-group class="search-tabs" (animationDone)="onSearchTabChanged()">
          <mat-tab label="Search">
            <div class="body">
              <span class="mat-body-2">
                {{globalSearchText}}
              </span>

              <ng-container *ngFor="let section of searchSections; let i = index">
                <mat-divider *ngIf="i > 0" class="section-divider"></mat-divider>
                <active-search
                  [canClear]="searchSections.length > 1"
                  [isSearchInitialized]="inputData.initialized"
                  [executedQuery]="getExecutedQueryForSearchSection(section.uid)"
                  [saveQueryField]="saveQueryField"
                  [lastTraceFailed]="inputData.lastTraceFailed ?? false"
                  [canAdd]="i === searchSections.length - 1"
                  [label]="getQueryLabel(section.uid)"
                  [saveQueryNameControl]="section.saveQueryNameControl"
                  [lastQueryExecutionTime]="section.lastQueryExecutionTime"
                  [runningQuery]="runningQueryUid === section.uid"
                  (clearQueryClick)="clearQuery(section.uid)"
                  (searchQueryClick)="searchQuery($event, section.uid)"
                  (addQueryClick)="addQuery()"></active-search>
              </ng-container>
            </div>
          </mat-tab>

          <mat-tab label="Saved">
            <search-list
              class="body"
              [searches]="inputData.savedSearches"
              placeholderText="Saved queries will appear here."
              [listItemOptions]="savedSearchOptions"></search-list>
          </mat-tab>

          <mat-tab label="Recent">
            <search-list
              class="body"
              [searches]="inputData.recentSearches"
              placeholderText="Recent queries will appear here."
              [listItemOptions]="recentSearchOptions"
              [control]="menuSaveQueryNameControl"></search-list>
          </mat-tab>

          <ng-template #saveQueryField let-query="query" let-control="control">
            <div class="outline-field save-field">
              <mat-form-field appearance="outline">
                <input matInput [formControl]="control" (keydown.enter)="onSaveQueryClick(query, control)"/>
                <mat-error *ngIf="control.invalid && control.value">Query with that name already exists.</mat-error>
              </mat-form-field>
              <button
                mat-flat-button
                class="query-button"
                color="primary"
                [disabled]="control.invalid"
                (click)="onSaveQueryClick(query, control)"> Save </button>
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
        <div class="results-placeholder placeholder-text mat-body-1" *ngIf="showResultsPlaceholder()"> Run a search to view tabulated results. </div>
        <mat-tab-group class="result-tabs">
          <mat-tab *ngFor="let curr of getCurrentSearchesWithResults()" [label]="getQueryLabel(curr.uid)">
            <div class="result">
              <div class="results-table">
                <log-view
                  class="results-log-view"
                  [entries]="curr.result.entries"
                  [headers]="curr.result.headers"
                  [selectedIndex]="curr.result.selectedIndex"
                  [scrollToIndex]="curr.result.scrollToIndex"
                  [currentIndex]="curr.result.currentIndex"
                  [traceType]="${TraceType.SEARCH}"
                  [showTraceEntryTimes]="false"
                  [showCurrentTimeButton]="false"
                  [padEntries]="false"></log-view>
              </div>
            </div>
          </mat-tab>
        </mat-tab-group>
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

        <div class="body">
          <span class="mat-body-1">
            Run custom SQL queries on Perfetto traces. Use specialized SQL views to aid with searching:
          </span>

          <cdk-accordion class="how-to-accordion" [multi]="true">
            <cdk-accordion-item *ngFor="let searchView of searchViews" class="accordion-item" #accordionItem="cdkAccordionItem">
              <span
                class="mat-body-1 accordion-item-header"
                (click)="onHeaderClick(accordionItem)">
                <mat-icon>
                  {{ accordionItem.expanded ? 'arrow_drop_down' : 'chevron_right' }}
                </mat-icon>
                <code>{{searchView.name}}</code>
              </span>
              <div *ngIf="accordionItem.expanded" class="accordion-item-body">
                <span class="mat-body-1">
                  Use to search {{searchView.dataType}} data.
                </span>
                <span class="mat-body-2">Spec:</span>
                <table>
                  <tr *ngFor="let c of searchView.spec">
                    <td><code>{{c.name}}</code></td>
                    <td class="mat-body-1">{{c.desc}}</td>
                  </tr>
                </table>
                <span class="mat-body-2">
                  Examples:
                </span>
                <ng-container *ngFor="let example of searchView.examples">
                  <pre><code>{{example.query}}</code></pre>
                  <span class="mat-body-1 indented"><i>{{example.desc}}</i></span>
                </ng-container>
              </div>
            </cdk-accordion-item>
          </cdk-accordion>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .search-tabs, .result-tabs {
        height: 100%;
      }
      .message-with-spinner {
        display: flex;
        flex-direction: row;
        align-items: center;
        justify-content: space-between;
      }
      .global-search .body {
        display: flex;
        flex-direction: column;
      }
      .section-divider {
        margin-top: 18px;
      }
      active-search {
        display: flex;
        flex-direction: column;
        margin-top: 12px;
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

      .how-to-search .body {
        display: flex;
        flex-direction: column;
        padding: 12px;
      }
      .how-to-search .how-to-accordion {
        display: flex;
        flex-direction: column;
        min-width: fit-content;
      }
      .how-to-search .accordion-item {
        border: 1px solid var(--border-color);
      }
      .how-to-search .accordion-item + .accordion-item {
        border-top: none;
      }
      .how-to-search .accordion-item:first-child {
        border-top-left-radius: 4px;
        border-top-right-radius: 4px;
      }
      .how-to-search .accordion-item:last-child {
        border-bottom-left-radius: 4px;
        border-bottom-right-radius: 4px;
      }
      .how-to-search .accordion-item-header {
        width: 100%;
        display: flex;
        flex-direction: row;
        align-items: center;
        cursor: pointer;
      }
      .how-to-search .accordion-item-body {
        padding: 8px;
        display: flex;
        flex-direction: column;
      }
      .how-to-search table {
        border-spacing: 0;
      }
      .how-to-search table td {
        border-left: 1px solid var(--border-color);
        border-top: 1px solid var(--border-color);
        padding-left: 4px;
        padding-right: 4px;
      }
      .how-to-search table tr:first-child td:first-child {
        border-top-left-radius: 4px;
      }
      .how-to-search table tr:first-child td:last-child {
        border-top-right-radius: 4px;
      }
      .how-to-search table tr:last-child td:first-child {
        border-bottom-left-radius: 4px;
      }
      .how-to-search table tr:last-child td:last-child {
        border-bottom-right-radius: 4px;
      }
      .how-to-search table tr:last-child td {
        border-bottom: 1px solid var(--border-color);
      }
      .how-to-search table tr td:last-child {
        border-right: 1px solid var(--border-color);
      }
      .how-to-search .body .indented {
        margin-inline-start: 5px;
      }
      .how-to-search code {
        font-size: 12px;
      }
      .how-to-search pre {
        white-space: pre-wrap;
        word-break: break-word;
        border-radius: 4px;
        padding: 0px 4px;
        margin: 0;
        margin-block: 5px;
        background: var(--drawer-block-primary);
      }
    `,
    viewerCardStyle,
    viewerCardInnerStyle,
  ],
})
export class ViewerSearchComponent {
  @Input() inputData: UiData | undefined;
  @ViewChild('saveQueryField') saveQueryField: NgTemplateOutlet | undefined;
  @ViewChildren(MatTabGroup) matTabGroups: QueryList<MatTabGroup> | undefined;
  @ViewChildren(ActiveSearchComponent) activeSearchComponents:
    | QueryList<ActiveSearchComponent>
    | undefined;

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
  searchSections: SearchSection[] = [];
  initializing = false;
  menuSaveQueryNameControl = this.makeSaveQueryNameControl();
  runningQueryUid: number | undefined;

  private runFromOptions = false;
  private editFromOptions = false;
  private readonly editOption: ListItemOption = {
    name: 'Edit',
    icon: 'edit',
    onClickCallback: (search: ListedSearch) => {
      this.onEditQueryClick(search);
    },
  };
  private readonly saveOption: ListItemOption = {
    name: 'Save',
    icon: 'save',
  };
  readonly savedSearchOptions: ListItemOption[] = [
    {
      name: 'Run',
      icon: 'play_arrow',
      onClickCallback: (search: ListedSearch) => {
        Analytics.TraceSearch.logQueryRequested('saved');
        this.onRunQueryFromOptionsClick(search);
      },
    },
    this.editOption,
    {
      name: 'Delete',
      icon: 'delete',
      onClickCallback: (search: ListedSearch) => {
        this.onDeleteQueryClick(search);
      },
    },
  ];
  readonly recentSearchOptions: ListItemOption[] = [
    {
      name: 'Run',
      icon: 'play_arrow',
      onClickCallback: (search: ListedSearch) => {
        Analytics.TraceSearch.logQueryRequested('recent');
        this.onRunQueryFromOptionsClick(search);
      },
    },
    this.editOption,
    this.saveOption,
  ];

  readonly globalSearchText = `
     Write an SQL query in the field below, and run the search. \
     Results will be shown in a tabular view and you can optionally visualize them in the timeline. \
  `;
  readonly searchViews: SearchView[] = [
    {
      name: 'sf_layer_search',
      dataType: 'SurfaceFlinger layer',
      spec: [
        {
          name: 'state_id',
          desc: 'Unique id of entry to which layer belongs',
        },
        {name: 'ts', desc: 'Timestamp of entry to which layer belongs'},
        {name: 'layer_id', desc: 'Layer id'},
        {name: 'parent_id', desc: 'Layer id of parent'},
        {name: 'layer_name', desc: 'Layer name'},
        {
          name: 'property',
          desc: 'Property name accounting for repeated fields',
        },
        {
          name: 'flat_property',
          desc: 'Property name not accounting for repeated fields',
        },
        {name: 'value', desc: 'Property value in string format'},
        {
          name: 'previous_value',
          desc: 'Property value from previous entry in string format',
        },
      ],
      examples: [
        {
          query: `SELECT ts, value, previous_value FROM sf_layer_search
  WHERE layer_name='Taskbar#97'
  AND property='color.a'
  AND value!=previous_value`,
          desc: 'returns timestamp, current and previous values of alpha for Taskbar#97, for states where alpha changed from previous state',
        },
        {
          query: `SELECT ts, value, previous_value FROM sf_layer_search
  WHERE layer_name LIKE 'Wallpaper%'
  AND property='bounds.bottom'
  AND cast_int!(value) <= 2400`,
          desc: 'returns timestamp, current and previous values of bottom bound for layers that start with "Wallpaper", for states where bottom bound <= 2400',
        },
      ],
    },
    {
      name: 'sf_hierarchy_root_search',
      dataType: 'SurfaceFlinger root',
      spec: [
        {
          name: 'state_id',
          desc: 'Unique id of entry',
        },
        {name: 'ts', desc: 'Timestamp of entry'},
        {
          name: 'property',
          desc: 'Property name accounting for repeated fields',
        },
        {
          name: 'flat_property',
          desc: 'Property name not accounting for repeated fields',
        },
        {name: 'value', desc: 'Property value in string format'},
        {
          name: 'previous_value',
          desc: 'Property value from previous entry in string format',
        },
      ],
      examples: [
        {
          query: `SELECT STATE.* FROM sf_hierarchy_root_search STATE_WITH_DISPLAY_ON
INNER JOIN sf_hierarchy_root_search STATE
  ON STATE.state_id = STATE_WITH_DISPLAY_ON.state_id
  AND STATE_WITH_DISPLAY_ON.flat_property='displays.layer_stack'
  AND STATE_WITH_DISPLAY_ON.value!='4294967295'
  AND STATE.property LIKE CONCAT(
    SUBSTRING(
        STATE_WITH_DISPLAY_ON.property,
        0,
        instr(STATE_WITH_DISPLAY_ON.property, ']')
    ),
    '%'
  )`,
          desc: 'returns all properties for displays with valid layer stack from all states',
        },
      ],
    },
    {
      name: 'transactions_search',
      dataType:
        'the Transactions trace, including transactions, added/destroyed layers and added/removed/changed displays',
      spec: [
        {
          name: 'state_id',
          desc: 'Unique id of entry to which proto property belongs',
        },
        {
          name: 'ts',
          desc: 'Timestamp of entry to which proto property belongs',
        },
        {
          name: 'transaction_id',
          desc: 'Transaction id if available',
        },
        {
          name: 'property',
          desc: 'Property name accounting for repeated fields',
        },
        {
          name: 'flat_property',
          desc: 'Property name not accounting for repeated fields',
        },
        {name: 'value', desc: 'Property value in string format'},
      ],
      examples: [
        {
          query: `SELECT ts, transaction_id FROM transactions_search
  WHERE flat_property='transactions.layer_changes.x'
  AND value='-54.0'`,
          desc: 'returns timestamp and transaction id when layer x position was changed to -54.0',
        },
        {
          query: `SELECT ts FROM transactions_search
  WHERE flat_property='added_layers.name'
  AND value='ImeContainer'`,
          desc: 'returns timestamp when ImeContainer layer was added',
        },
      ],
    },
  ];

  constructor(
    @Inject(ElementRef) private elementRef: ElementRef<HTMLElement>,
  ) {}

  ngAfterViewInit() {
    this.saveOption.menu = this.saveQueryField;
  }

  ngOnChanges(simpleChanges: SimpleChanges) {
    if (this.initializing && this.inputData?.initialized) {
      this.initializing = false;
    }
    this.updateSearchSections(simpleChanges);
    if (this.tryPropagateRunFromOptions()) {
      return;
    }
    this.tryHandleQueryCompleted();
  }

  ngAfterContentChecked() {
    this.tryPropagateEditFromOptions();
  }

  onGlobalSearchClick() {
    if (!this.initializing && !this.inputData?.initialized) {
      this.initializing = true;
      const event = new CustomEvent(ViewerEvents.GlobalSearchSectionClick);
      this.elementRef.nativeElement.dispatchEvent(event);
    }
  }

  searchQuery(query: string, uid: number) {
    this.runningQueryUid = uid;
    const section = assertDefined(
      this.searchSections.find((s) => s.uid === uid),
    );
    section.lastQueryExecutionTime = undefined;
    section.lastQueryStartTime = Date.now();
    const event = new CustomEvent(ViewerEvents.SearchQueryClick, {
      detail: new SearchQueryClickDetail(query, uid),
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  onSaveQueryClick(query: string, control: FormControl) {
    if (control.invalid) {
      return;
    }
    const event = new CustomEvent(ViewerEvents.SaveQueryClick, {
      detail: new SaveQueryClickDetail(query, assertDefined(control.value)),
    });
    this.elementRef.nativeElement.dispatchEvent(event);
    Analytics.TraceSearch.logQuerySaved();
    control.reset();
  }

  onHeaderClick(accordionItem: CdkAccordionItem) {
    accordionItem.toggle();
  }

  clearQuery(uid: number) {
    const event = new CustomEvent(ViewerEvents.ClearQueryClick, {
      detail: new ClearQueryClickDetail(uid),
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  addQuery(query?: string) {
    const event = new CustomEvent(ViewerEvents.AddQueryClick, {
      detail: query ? new AddQueryClickDetail(query) : undefined,
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  getCurrentSearchesWithResults(): CurrentSearch[] {
    return assertDefined(this.inputData).currentSearches.filter(
      (search) => search.result !== undefined,
    );
  }

  getCurrentSearchByUid(uid: number): CurrentSearch | undefined {
    return this.inputData?.currentSearches.find((search) => search.uid === uid);
  }

  getExecutedQueryForSearchSection(uid: number): string | undefined {
    return this.runningQueryUid !== uid
      ? this.getCurrentSearchByUid(uid)?.query
      : undefined;
  }

  getQueryLabel(uid: number): string {
    return 'Query ' + uid;
  }

  showResultsPlaceholder(): boolean {
    return (
      this.runningQueryUid === undefined &&
      this.getCurrentSearchesWithResults().length === 0
    );
  }

  onSearchTabChanged() {
    const finalComponent = assertDefined(this.activeSearchComponents).last;
    if (assertDefined(this.matTabGroups).first.selectedIndex === 0) {
      finalComponent.elementRef.nativeElement.scrollIntoView();
    }
  }

  private updateSearchSections(simpleChanges: SimpleChanges) {
    const currentSearches = this.inputData?.currentSearches;
    const previousSearches: CurrentSearch[] | undefined =
      simpleChanges['inputData']?.previousValue?.currentSearches;
    currentSearches?.forEach((search) => {
      if (!this.searchSections.some((s) => s.uid === search.uid)) {
        this.searchSections.push({
          uid: search.uid,
          saveQueryNameControl: this.makeSaveQueryNameControl(),
        });
      }
    });
    previousSearches?.forEach((search) => {
      if (!currentSearches?.some((curr) => curr.uid === search.uid)) {
        const i = this.searchSections.findIndex((a) => a.uid === search.uid);
        this.searchSections.splice(i, 1);
      }
    });
  }

  private tryPropagateRunFromOptions(): boolean {
    if (
      this.runFromOptions &&
      this.runningQueryUid === undefined &&
      this.inputData?.currentSearches
    ) {
      const lastSearch =
        this.inputData.currentSearches[
          this.inputData.currentSearches.length - 1
        ];
      this.searchQuery(assertDefined(lastSearch.query), lastSearch.uid);
      this.runFromOptions = false;
      return true;
    }
    return false;
  }

  private tryPropagateEditFromOptions() {
    if (this.editFromOptions) {
      const currentSearches = assertDefined(this.inputData).currentSearches;
      if (currentSearches.length !== this.activeSearchComponents?.length) {
        return;
      }
      const lastSearch = currentSearches[currentSearches.length - 1];
      if (lastSearch.query) {
        this.updateLastSectionTextAndShowTab(lastSearch.query);
        this.editFromOptions = false;
      }
    }
  }

  private updateLastSectionTextAndShowTab(text: string) {
    assertDefined(
      this.activeSearchComponents?.get(this.searchSections.length - 1),
    ).updateText(text);
    assertDefined(this.matTabGroups).first.selectedIndex = 0;
  }

  private tryHandleQueryCompleted() {
    const currentSearch =
      this.runningQueryUid !== undefined
        ? this.getCurrentSearchByUid(this.runningQueryUid)
        : undefined;

    if (this.runningQueryUid !== undefined && currentSearch !== undefined) {
      const sectionIndex = this.searchSections.findIndex(
        (s) => s.uid === this.runningQueryUid,
      );
      const section = this.searchSections[sectionIndex];

      if (!this.inputData?.lastTraceFailed) {
        this.activeSearchComponents
          ?.get(sectionIndex)
          ?.updateText(currentSearch?.query ?? '');
        section.saveQueryNameControl.setValue(
          this.getQueryLabel(assertDefined(this.runningQueryUid)),
        );
        assertDefined(this.matTabGroups).last.selectedIndex = sectionIndex;
      }

      const executionTimeMs =
        Date.now() - assertDefined(section.lastQueryStartTime);
      Analytics.TraceSearch.logQueryExecutionTime(executionTimeMs);
      section.lastQueryExecutionTime = new TimeDuration(
        BigInt(executionTimeMs * TIME_UNIT_TO_NANO.ms),
      ).format();
      section.lastQueryStartTime = undefined;

      this.runningQueryUid = undefined;
    }
  }

  private onRunQueryFromOptionsClick(search: ListedSearch) {
    const lastUid = this.getLastUid();
    if (this.getCurrentSearchByUid(lastUid)?.result) {
      this.runFromOptions = true;
      this.addQuery(search.query);
    } else {
      this.searchQuery(search.query, lastUid);
    }
  }

  private getLastUid(): number {
    return this.searchSections[this.searchSections.length - 1].uid;
  }

  private onEditQueryClick(search: ListedSearch) {
    const currentSearches = assertDefined(this.inputData).currentSearches;
    const lastCurrentSearch = currentSearches[currentSearches.length - 1];
    if (lastCurrentSearch.result !== undefined) {
      this.editFromOptions = true;
      this.addQuery(search.query);
      return;
    }
    this.updateLastSectionTextAndShowTab(search.query);
  }

  private onDeleteQueryClick(search: ListedSearch) {
    const event = new CustomEvent(ViewerEvents.DeleteSavedQueryClick, {
      detail: new DeleteSavedQueryClickDetail(search),
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  private makeSaveQueryNameControl() {
    return new FormControl(
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
  }

  private validateSearchQuerySaveName(
    control: FormControl,
    savedSearches: ListedSearch[],
  ): ValidationErrors | null {
    const valid =
      control.value &&
      !savedSearches.some((search) => search.name === control.value);
    return !valid ? {invalidInput: control.value} : null;
  }
}

interface SearchView {
  name: string;
  dataType: string;
  spec: Array<{name: string; desc: string}>;
  examples: Array<{query: string; desc: string}>;
}

interface SearchSection {
  uid: number;
  saveQueryNameControl: FormControl;
  lastQueryExecutionTime?: string;
  lastQueryStartTime?: number;
}
