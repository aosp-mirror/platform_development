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
  Component,
  ElementRef,
  Inject,
  Input,
  SimpleChanges,
} from '@angular/core';
import {assertDefined} from 'common/assert_utils';
import {FunctionUtils} from 'common/function_utils';
import {PersistentStore} from 'common/persistent_store';
import {Analytics} from 'logging/analytics';
import {
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
          <nav mat-tab-nav-bar class="tabs-navigation-bar">
              <a
                  *ngFor="let tab of tabs; last as isLast"
                  mat-tab-link
                  [active]="isCurrentActiveTab(tab)"
                  [class.active]="isCurrentActiveTab(tab)"
                  [matTooltip]="getTabTooltip(tab.view)"
                  [matTooltipShowDelay]="300"
                  (click)="onTabClick(tab)"
                  (focus)="$event.target.blur()"
                  [class.last]="isLast"
                  class="tab">
                <mat-icon
                  class="icon"
                  [style]="{color: TRACE_INFO[tab.view.traces[0].type].color, marginRight: '0.5rem'}">
                    {{ TRACE_INFO[tab.view.traces[0].type].icon }}
                </mat-icon>
                <span>
                  {{ getTitle(tab.view) }}
                </span>
              </a>
          </nav>
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
    `,
  ],
})
export class TraceViewComponent
  implements WinscopeEventEmitter, WinscopeEventListener
{
  @Input() viewers: Viewer[] = [];
  @Input() store: PersistentStore | undefined;

  TRACE_INFO = TRACE_INFO;
  tabs: Tab[] = [];

  private elementRef: ElementRef;
  private currentActiveTab: undefined | Tab;
  private emitAppEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;

  constructor(@Inject(ElementRef) elementRef: ElementRef) {
    this.elementRef = elementRef;
  }

  ngOnChanges(changes: SimpleChanges) {
    this.renderViewsTab(changes['viewers']?.firstChange ?? false);
    this.renderViewsOverlay();
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
    const isDump = view.traces.length === 1 && view.traces[0].isDump();
    return view.title + (isDump ? ' Dump' : '');
  }

  private renderViewsTab(firstToRender: boolean) {
    this.tabs = this.viewers
      .map((viewer) => viewer.getViews())
      .flat()
      .filter((view) => view.type === ViewType.TAB)
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
      this.showTab(this.tabs[0], firstToRender);
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
      Analytics.Navigation.logTabSwitched(
        TRACE_INFO[tab.view.traces[0].type].name,
      );
      await this.emitAppEvent(new TabbedViewSwitched(tab.view));
    }
  }
}
