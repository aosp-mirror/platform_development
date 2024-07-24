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

import {Component, ElementRef, Inject, Input} from '@angular/core';
import {TRACE_INFO} from 'app/trace_info';
import {assertDefined} from 'common/assert_utils';
import {FunctionUtils} from 'common/function_utils';
import {PersistentStore} from 'common/persistent_store';
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
import {View, Viewer, ViewType} from 'viewers/viewer';

interface Tab {
  view: View;
  addedToDom: boolean;
}

@Component({
  selector: 'trace-view',
  template: `
    <div class="overlay">
      <div class="draggable-container" cdkDrag cdkDragBoundary=".overlay">
        <!--
        TODO:
        this draggable div is a temporary hack. We should remove the div and move the cdkDrag
        directives into the overlay view (e.g. ViewerScreenReocordingComponent) as soon as the new
        Angular's directive composition API is available
        (https://github.com/angular/angular/issues/8785).
         -->
      </div>
    </div>
    <div class="header-items-wrapper">
      <nav mat-tab-nav-bar class="tabs-navigation-bar">
        <a
          *ngFor="let tab of tabs; last as isLast"
          mat-tab-link
          [active]="isCurrentActiveTab(tab)"
          [class.active]="isCurrentActiveTab(tab)"
          (click)="onTabClick(tab)"
          (focus)="$event.target.blur()"
          [class.last]="isLast"
          class="tab">
          <mat-icon
            class="icon"
            [matTooltip]="TRACE_INFO[tab.view.traceType].name"
            [style]="{color: TRACE_INFO[tab.view.traceType].color, marginRight: '0.5rem'}">
            {{ TRACE_INFO[tab.view.traceType].icon }}
          </mat-icon>
          <span>
            {{ tab.view.title }}
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

      .overlay {
        z-index: 30;
        position: fixed;
        top: 0px;
        left: 0px;
        width: 100%;
        height: 100%;
        pointer-events: none;
      }

      .overlay .draggable-container {
        position: absolute;
        right: 0;
        top: 20vh;
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

  ngOnChanges() {
    this.renderViewsTab();
    this.renderViewsOverlay();
  }

  async onTabClick(tab: Tab) {
    await this.showTab(tab);
  }

  async onWinscopeEvent(event: WinscopeEvent) {
    await event.visit(
      WinscopeEventType.TABBED_VIEW_SWITCH_REQUEST,
      async (event) => {
        const tab = this.tabs.find(
          (tab) => tab.view.traceType === event.newFocusedViewId,
        );
        if (tab) {
          await this.showTab(tab);
        }
      },
    );
  }

  setEmitEvent(callback: EmitEvent) {
    this.emitAppEvent = callback;
  }

  isCurrentActiveTab(tab: Tab) {
    return tab === this.currentActiveTab;
  }

  private renderViewsTab() {
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
      this.showTab(this.tabs[0]);
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
        this.elementRef.nativeElement.querySelector(
          '.overlay .draggable-container',
        ),
      );
      container.appendChild(view.htmlElement);
    });
  }

  private async showTab(tab: Tab) {
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

    await this.emitAppEvent(new TabbedViewSwitched(tab.view));
  }
}
