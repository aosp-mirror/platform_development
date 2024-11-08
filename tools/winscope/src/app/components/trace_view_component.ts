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

import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  Inject,
  Input,
  NgZone,
  SimpleChanges,
} from '@angular/core';
import {FormControl, ValidationErrors, Validators} from '@angular/forms';
import {overlayPanelStyles} from 'app/styles/overlay_panel.styles';
import {assertDefined} from 'common/assert_utils';
import {FunctionUtils} from 'common/function_utils';
import {Store} from 'common/store';
import {Analytics} from 'logging/analytics';
import {
  FilterPresetApplyRequest,
  FilterPresetSaveRequest,
  TabbedViewSwitched,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {
  EmitEvent,
  WinscopeEventEmitter,
} from 'messaging/winscope_event_emitter';
import {WinscopeEventListener} from 'messaging/winscope_event_listener';
import {TRACE_INFO} from 'trace/trace_info';
import {TraceType} from 'trace/trace_type';
import {inlineButtonStyle} from 'viewers/components/styles/clickable_property.styles';
import {View, Viewer, ViewType} from 'viewers/viewer';

interface Tab {
  view: View;
  addedToDom: boolean;
}

@Component({
  selector: 'trace-view',
  template: `
      <div class="overlay-container">
      </div>
      <div class="header-items-wrapper">
        <div class="trace-tabs-wrapper header-items-wrapper">
          <nav mat-tab-nav-bar class="tabs-navigation-bar">
            <a
                *ngFor="let tab of tabs; last as isLast"
                mat-tab-link
                [active]="isCurrentActiveTab(tab)"
                [class.active]="isCurrentActiveTab(tab)"
                [matTooltip]="getTabTooltip(tab.view)"
                matTooltipPosition="above"
                [matTooltipShowDelay]="300"
                (click)="onTabClick(tab)"
                (focus)="$event.target.blur()"
                [class.last]="isLast"
                class="tab">
              <mat-icon
                class="icon"
                [style]="{color: getTabIconColor(tab), marginRight: '0.5rem'}">
                  {{ getTabIcon(tab) }}
              </mat-icon>
              <span>
                {{ getTitle(tab.view) }}
              </span>
            </a>
          </nav>
        </div>

        <button
          [disabled]="!currentTabHasFilterPresets()"
          mat-flat-button
          cdkOverlayOrigin
          #filterPresetsTrigger="cdkOverlayOrigin"
          color="primary"
          class="filter-presets"
          (click)="onFilterPresetsClick()">
          <span class="filter-presets-label">
            <mat-icon class="material-symbols-outlined">save</mat-icon>
            <span> Filter Presets </span>
          </span>
        </button>

        <ng-template
          cdkConnectedOverlay
          [cdkConnectedOverlayOrigin]="filterPresetsTrigger"
          [cdkConnectedOverlayOpen]="isFilterPresetsPanelOpen"
          [cdkConnectedOverlayHasBackdrop]="true"
          cdkConnectedOverlayBackdropClass="cdk-overlay-transparent-backdrop"
          (backdropClick)="onFilterPresetsClick()"
        >
          <div class="overlay-panel filter-presets-panel">
            <h2 class="overlay-panel-title">
              <span> FILTER PRESETS </span>
              <button (click)="onFilterPresetsClick()" class="close-button" mat-icon-button>
                <mat-icon> close </mat-icon>
              </button>
            </h2>
            <div class="overlay-panel-content">
              <span class="mat-body-1"> Save the current configuration of filters for this trace type to access later, or select one of the existing configurations below. </span>

              <div class="overlay-panel-section save-section">
                <span class="mat-body-2 overlay-panel-section-title"> Preset Name </span>
                <div class="save-field outline-field">
                  <mat-form-field appearance="outline">
                    <input matInput [formControl]="filterPresetNameControl" (keydown.enter)="savePreset()"/>
                    <mat-error *ngIf="filterPresetNameControl.invalid && filterPresetNameControl.value">Preset with that name already exists.</mat-error>
                  </mat-form-field>
                  <button mat-flat-button color="primary" [disabled]="filterPresetNameControl.invalid" (click)="savePreset()"> Save </button>
                </div>
              </div>

              <mat-divider></mat-divider>

              <div class="overlay-panel-section existing-presets-section">
                <span class="mat-body-2 overlay-panel-section-title"> Apply a preset </span>
                <span class="mat-body-1" *ngIf="getCurrentFilterPresets().length === 0"> No existing presets found. </span>
                <div *ngFor="let preset of getCurrentFilterPresets()" class="existing-preset inline">
                  <button
                      mat-button
                      color="primary"
                      (click)="onExistingPresetClick(preset)">
                    {{ preset.split(".")[0] }}
                  </button>
                  <button mat-icon-button class="delete-button" (click)="deletePreset(preset)">
                    <mat-icon class="material-symbols-outlined"> delete </mat-icon>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </ng-template>
      </div>
      <mat-divider></mat-divider>
      <div class="trace-view-content"></div>
  `,
  styles: [
    `
      .tab.active {
        opacity: 100%;
      }

      .header-items-wrapper {
        display: flex;
        flex-direction: row;
        justify-content: space-between;
        align-items: center;
      }

      .trace-tabs-wrapper {
        overflow-x: auto;
      }

      .tabs-navigation-bar {
        height: 100%;
        border-bottom: 0px;
      }

      .trace-view-content {
        height: 100%;
        overflow: auto;
        background-color: var(--trace-view-background-color);
      }

      .tab {
        overflow-x: hidden;
        text-overflow: ellipsis;
      }

      .tab:not(.last):after {
        content: '';
        position: absolute;
        right: 0;
        height: 60%;
        width: 1px;
        background-color: #C4C0C0;
      }

      .filter-presets {
        line-height: 24px;
        padding: 0 10px;
        margin-inline: 10px;
        min-width: fit-content;
        min-height: fit-content;
      }

      .filter-presets-label {
        display: flex;
        flex-direction: row;
        align-items: center;
      }

      .filter-presets-label .mat-icon {
        margin-inline-end: 5px;
      }

      .filter-presets-panel {
        max-width: 440px;
        max-height: 500px;
        overflow-y: auto;
        border-radius: 15px;
      }

      .save-field {
        display: flex;
        align-items: center;
        font-size: 14px;
        width: 100%;
      }

      .save-field mat-form-field {
        width: 100%;
      }

      .save-field button {
        height: fit-content;
        margin-inline-start: 10px;
      }

      .existing-preset {
        display: flex;
        flex-direction: row;
        justify-content: space-between;
        align-items: center;
        width: 100%:
      }

      .existing-preset:hover {
        background-color: var(--hover-element-color);
      }

      .existing-preset:not(:hover) .delete-button {
        opacity: 0.5;
      }
    `,
    overlayPanelStyles,
    inlineButtonStyle,
  ],
})
export class TraceViewComponent
  implements WinscopeEventEmitter, WinscopeEventListener
{
  @Input() viewers: Viewer[] = [];
  @Input() store: Store | undefined;

  TRACE_INFO = TRACE_INFO;
  tabs: Tab[] = [];
  isFilterPresetsPanelOpen = false;
  filterPresetNameControl = new FormControl(
    '',
    assertDefined(
      Validators.compose([
        Validators.required,
        (control: FormControl) =>
          this.validateFilterPresetName(
            control,
            this.allFilterPresets,
            (input: string) =>
              this.makeFilterPresetName(
                input,
                assertDefined(this.getCurrentTabTraceType()),
              ),
          ),
      ]),
    ),
  );

  private currentActiveTab: undefined | Tab;
  private emitAppEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;
  private filterPresetsStoreKey = 'filterPresets';
  private allFilterPresets: string[] = [];

  constructor(
    @Inject(ElementRef) private elementRef: ElementRef,
    @Inject(ChangeDetectorRef) private changeDetectorRef: ChangeDetectorRef,
    @Inject(NgZone) private ngZone: NgZone,
  ) {}

  ngOnChanges(changes: SimpleChanges) {
    if (changes['store']?.firstChange) {
      const storedPresets = this.store?.get(this.filterPresetsStoreKey);
      if (storedPresets) {
        this.allFilterPresets = JSON.parse(storedPresets);
      }
    }
    this.renderViewsTab(changes['viewers']?.firstChange ?? false);
    this.renderViewsOverlay();
  }

  getTabIconColor(tab: Tab): string {
    if (tab.view.type === ViewType.GLOBAL_SEARCH) return '';
    const trace = tab.view.traces.at(0);
    if (!trace) return '';
    return TRACE_INFO[trace.type].color;
  }

  getTabIcon(tab: Tab): string {
    if (tab.view.type === ViewType.GLOBAL_SEARCH) {
      return TRACE_INFO[TraceType.SEARCH].icon;
    }
    const trace = tab.view.traces.at(0);
    if (!trace) return '';
    return TRACE_INFO[trace.type].icon;
  }

  async onTabClick(tab: Tab) {
    await this.showTab(tab, false);
  }

  async onWinscopeEvent(event: WinscopeEvent) {
    await event.visit(
      WinscopeEventType.TABBED_VIEW_SWITCH_REQUEST,
      async (event) => {
        const tab = this.tabs.find((tab) =>
          tab.view.traces.some((trace) => trace === event.newActiveTrace),
        );
        await this.showTab(assertDefined(tab), false);
      },
    );
  }

  setEmitEvent(callback: EmitEvent) {
    this.emitAppEvent = callback;
  }

  isCurrentActiveTab(tab: Tab) {
    return tab === this.currentActiveTab;
  }

  getTabTooltip(view: View): string {
    return view.traces.flatMap((trace) => trace.getDescriptors()).join(', ');
  }

  getTitle(view: View): string {
    const isDump = view.traces.length === 1 && view.traces.at(0)?.isDump();
    return view.title + (isDump ? ' Dump' : '');
  }

  getCurrentFilterPresets(): string[] {
    const currentTabTraceType = this.getCurrentTabTraceType();
    if (currentTabTraceType === undefined) return [];
    return this.allFilterPresets.filter((preset) =>
      preset.includes(TRACE_INFO[currentTabTraceType].name),
    );
  }

  onFilterPresetsClick() {
    this.ngZone.run(() => {
      this.isFilterPresetsPanelOpen = !this.isFilterPresetsPanelOpen;
      this.changeDetectorRef.detectChanges();
    });
  }

  async savePreset() {
    if (this.filterPresetNameControl.invalid) return;
    await this.ngZone.run(async () => {
      const value = assertDefined(this.filterPresetNameControl.value);
      const currentTabTraceType = assertDefined(this.getCurrentTabTraceType());
      const presetName = this.makeFilterPresetName(value, currentTabTraceType);

      this.allFilterPresets.push(presetName);
      if (this.store) {
        this.store?.add(
          this.filterPresetsStoreKey,
          JSON.stringify(this.allFilterPresets),
        );
      }

      this.filterPresetNameControl.reset();
      this.changeDetectorRef.detectChanges();
      await this.emitAppEvent(
        new FilterPresetSaveRequest(presetName, currentTabTraceType),
      );
    });
  }

  onExistingPresetClick(preset: string) {
    this.emitAppEvent(
      new FilterPresetApplyRequest(
        preset,
        assertDefined(this.getCurrentTabTraceType()),
      ),
    );
  }

  deletePreset(preset: string) {
    this.allFilterPresets = this.allFilterPresets.filter((p) => p !== preset);
    this.store?.clear(preset);
    this.store?.add(
      this.filterPresetsStoreKey,
      JSON.stringify(this.allFilterPresets),
    );
    this.filterPresetNameControl.updateValueAndValidity();
    this.changeDetectorRef.detectChanges();
  }

  currentTabHasFilterPresets(): boolean {
    const currentTabTraceType = this.getCurrentTabTraceType();
    return (
      currentTabTraceType !== undefined &&
      [
        TraceType.SURFACE_FLINGER,
        TraceType.WINDOW_MANAGER,
        TraceType.INPUT_METHOD_CLIENTS,
        TraceType.INPUT_METHOD_MANAGER_SERVICE,
        TraceType.INPUT_METHOD_SERVICE,
        TraceType.VIEW_CAPTURE,
      ].includes(currentTabTraceType)
    );
  }

  private getCurrentTabTraceType(): TraceType | undefined {
    return this.currentActiveTab?.view.traces.at(0)?.type;
  }

  private renderViewsTab(firstToRender: boolean) {
    this.tabs = this.viewers
      .map((viewer) => viewer.getViews())
      .flat()
      .filter((view) => view.type !== ViewType.OVERLAY)
      .map((view) => {
        return {
          view,
          addedToDom: false,
        };
      });

    this.tabs.forEach((tab) => {
      // TODO: setting "store" this way is a hack.
      //       Store should be part of View's interface.
      (tab.view.htmlElement as any).store = this.store;
    });

    if (this.tabs.length > 0) {
      const tabToShow = assertDefined(
        this.tabs.find((tab) => tab.view.type !== ViewType.GLOBAL_SEARCH),
      );
      this.showTab(tabToShow, firstToRender);
    }
  }

  private renderViewsOverlay() {
    const views: View[] = this.viewers
      .map((viewer) => viewer.getViews())
      .flat()
      .filter((view) => view.type === ViewType.OVERLAY);

    if (views.length > 1) {
      throw new Error(
        'Only one overlay view is supported. To allow more overlay views, either create more than' +
          ' one draggable containers in this component or move the cdkDrag directives into the' +
          " overlay view when the new Angular's directive composition API is available" +
          ' (https://github.com/angular/angular/issues/8785).',
      );
    }

    views.forEach((view) => {
      view.htmlElement.style.pointerEvents = 'all';
      const container = assertDefined(
        this.elementRef.nativeElement.querySelector('.overlay-container'),
      );
      container.appendChild(view.htmlElement);
    });
  }

  private async showTab(tab: Tab, firstToRender: boolean) {
    if (this.currentActiveTab) {
      this.currentActiveTab.view.htmlElement.style.display = 'none';
    }

    if (!tab.addedToDom) {
      // Workaround for b/255966194:
      // make sure that the first time a tab content is rendered
      // (added to the DOM) it has style.display == "". This fixes the
      // initialization/rendering issues with cdk-virtual-scroll-viewport
      // components inside the tab contents.
      const traceViewContent = assertDefined(
        this.elementRef.nativeElement.querySelector('.trace-view-content'),
      );
      traceViewContent.appendChild(tab.view.htmlElement);
      tab.addedToDom = true;
    } else {
      tab.view.htmlElement.style.display = '';
    }

    this.currentActiveTab = tab;

    if (!firstToRender) {
      Analytics.Navigation.logTabSwitched(tab.view.title);
      await this.emitAppEvent(new TabbedViewSwitched(tab.view));
    }
  }

  private validateFilterPresetName(
    control: FormControl,
    filterPresets: string[],
    makeFilterPresetName: (input: string) => string,
  ): ValidationErrors | null {
    const valid =
      control.value &&
      !filterPresets.includes(makeFilterPresetName(control.value));
    return !valid ? {invalidInput: control.value} : null;
  }

  private makeFilterPresetName(input: string, traceType: TraceType) {
    return input + '.' + TRACE_INFO[traceType].name;
  }
}
