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
import {Component, ElementRef, EventEmitter, Inject, Input, Output} from "@angular/core";
import {PersistentStore} from "common/persistent_store";
import {Viewer, View, ViewType} from "viewers/viewer";

@Component({
  selector: "trace-view",
  template: `
    <div class="container-overlay">
    </div>
    <div class="header-items-wrapper">
      <nav mat-tab-nav-bar class="viewer-nav-bar">
        <a
          *ngFor="let tab of viewerTabs"
          mat-tab-link
          [active]="isCurrentActiveCard(tab.cardId)"
          (click)="showViewer(tab.cardId)"
          class="viewer-tab"
        >{{tab.label}}</a>
      </nav>
      <button
        color="primary"
        mat-button
        class="save-btn"
        (click)="downloadTracesButtonClick.emit()"
      >Download all traces</button>
    </div>
    <div class="trace-view-content">
    </div>
  `,
  styles: [
    `
      .container-overlay {
        z-index: 10;
        position: fixed;
        top: 0px;
        left: 0px;
        width: 100%;
        height: 100%;
        pointer-events: none;
      }

      .header-items-wrapper {
        width: 100%;
        display: flex;
        flex-direction: row;
        justify-content: space-between;
        align-items: center;
      }

      .viewer-nav-bar {
        height: 100%;
      }

      .trace-view-content {
        height: 0;
        flex-grow: 1;
      }

      .save-btn {
        height: 100%;
      }
    `
  ]
})
export class TraceViewComponent {
  @Input() viewers!: Viewer[];
  @Input() store!: PersistentStore;
  @Output() downloadTracesButtonClick = new EventEmitter<void>();

  private elementRef: ElementRef;
  private viewerTabs: ViewerTab[] = [];
  private activeViewerCardId = 0;

  constructor(@Inject(ElementRef) elementRef: ElementRef) {
    this.elementRef = elementRef;
  }

  ngOnChanges() {
    this.renderViewsTab();
    this.renderViewsOverlay();
  }

  public showViewer(cardId: number) {
    this.changeViewerVisibility(false);
    this.activeViewerCardId = cardId;
    this.changeViewerVisibility(true);
  }

  public isCurrentActiveCard(cardId: number) {
    return this.activeViewerCardId === cardId;
  }

  private renderViewsTab() {
    this.activeViewerCardId = 0;
    this.viewerTabs = [];

    const views: View[] = this.viewers
      .map(viewer => viewer.getViews())
      .flat()
      .filter(view => (view.type === ViewType.TAB));

    for (const [cardCounter, view] of views.entries()) {
      if (!view) {
        continue;
      }

      // create tab for viewer nav bar
      const tab = {
        label: view.title,
        cardId: cardCounter,
      };
      this.viewerTabs.push(tab);

      // add properties to view and add view to trace view card
      (view as any).store = this.store;
      view.htmlElement.id = `card-${cardCounter}`;
      view.htmlElement.style.display = this.isActiveViewerCard(cardCounter) ? "" : "none";

      const traceViewContent = this.elementRef.nativeElement.querySelector(".trace-view-content")!;
      traceViewContent.appendChild(view.htmlElement);
    }
  }

  private renderViewsOverlay() {
    const views: View[] = this.viewers
      .map(viewer => viewer.getViews())
      .flat()
      .filter(view => (view.type === ViewType.OVERLAY));

    views.forEach(view => {
      view.htmlElement.style.pointerEvents = "all";
      view.htmlElement.style.position = "absolute";
      view.htmlElement.style.bottom = "10%";
      view.htmlElement.style.right = "0px";

      const containerOverlay = this.elementRef.nativeElement.querySelector(".container-overlay");
      if (!containerOverlay) {
        throw new Error("Failed to find overlay container sub-element");
      }

      containerOverlay!.appendChild(view.htmlElement);
    });
  }

  private isActiveViewerCard(cardId: number) {
    return this.activeViewerCardId === cardId;
  }

  private changeViewerVisibility(show: boolean) {
    const view = document.querySelector(`#card-${this.activeViewerCardId}`);
    if (view) {
      (view as HTMLElement).style.display = show ? "" : "none";
      (view as any).active = show;
    }
  }
}

interface ViewerTab {
  label: string,
  cardId: number
}