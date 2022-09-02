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
  Input,
  Inject,
  ElementRef,
} from "@angular/core";
import { TraceCoordinator } from "app/trace_coordinator";
import { PersistentStore } from "common/persistent_store";
import { Viewer } from "viewers/viewer";

@Component({
  selector: "trace-view",
  template: `
    <mat-card class="trace-card">
      <mat-card-header class="trace-view-header">
        <span class="header-items-wrapper">
          <nav mat-tab-nav-bar class="viewer-nav-bar">
            <a
              mat-tab-link
              *ngFor="let tab of viewerTabs"
              [active]="isCurrentActiveCard(tab.cardId)"
              (click)="showViewer(tab.cardId)"
              class="viewer-tab"
            >{{tab.label}}</a>
          </nav>
          <button
            mat-raised-button
            class="icon-button white-btn save-btn"
            (click)="saveTraces()"
          >Download all traces</button>
        </span>
      </mat-card-header>
      <mat-card-content class="trace-view-content">
      </mat-card-content>
    </mat-card>
  `,
  styles: [
    `
      :host /deep/ .trace-view-header .mat-card-header-text {
        margin: 0;
      }

      .header-items-wrapper {
        width: 100%;
        vertical-align: middle;
        position: relative;
        display: inline-block;
      }

      .viewer-nav-bar {
        vertical-align: middle;
        display: inline-block;
      }

      .save-btn {
        float: right;
        vertical-align: middle;
        border: none;
        height: 100%;
        margin: 0;
        display: inline-block;
      }
    `
  ]
})
export class TraceViewComponent {
  @Input() store!: PersistentStore;
  @Input() traceCoordinator!: TraceCoordinator;
  viewerTabs: ViewerTab[] = [];
  viewersAdded = false;
  activeViewerCardId = 0;

  constructor(
    @Inject(ElementRef) private elementRef: ElementRef,
  ) {}

  ngDoCheck() {
    if (this.traceCoordinator.getViewers().length > 0 && !this.viewersAdded) {
      let cardCounter = 0;
      this.viewerTabs = [];
      this.traceCoordinator.getViewers().forEach((viewer: Viewer) => {
        // create tab for viewer nav bar
        const tab = {
          label: viewer.getTitle(),
          cardId: cardCounter,
        };
        this.viewerTabs.push(tab);

        // add properties to view and add view to trace view card
        const view = viewer.getView();
        (view as any).store = this.store;
        view.id = `card-${cardCounter}`;
        view.style.display = this.isActiveViewerCard(cardCounter) ? "" : "none";

        const traceViewContent = this.elementRef.nativeElement.querySelector(".trace-view-content")!;
        traceViewContent.appendChild(view);
        cardCounter++;
      });
      this.viewersAdded = true;
    } else if (this.traceCoordinator.getViewers().length === 0  && this.viewersAdded) {
      this.activeViewerCardId = 0;
      this.viewersAdded = false;
    }
  }

  public showViewer(cardId: number) {
    this.changeViewerVisibility(false);
    this.activeViewerCardId = cardId;
    this.changeViewerVisibility(true);
  }

  public isCurrentActiveCard(cardId: number) {
    return this.activeViewerCardId === cardId;
  }

  public async saveTraces() {
    const zipFile = await this.traceCoordinator.saveTracesAsZip();
    const zipFileName = "winscope.zip";
    const a = document.createElement("a");
    document.body.appendChild(a);
    const url = window.URL.createObjectURL(zipFile);
    a.href = url;
    a.download = zipFileName;
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
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