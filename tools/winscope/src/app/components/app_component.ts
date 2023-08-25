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
  Inject,
  Injector,
  ViewChild,
  ViewEncapsulation,
} from '@angular/core';
import {createCustomElement} from '@angular/elements';
import {AbtChromeExtensionProtocol} from 'abt_chrome_extension/abt_chrome_extension_protocol';
import {Mediator} from 'app/mediator';
import {TimelineData} from 'app/timeline_data';
import {TRACE_INFO} from 'app/trace_info';
import {TracePipeline} from 'app/trace_pipeline';
import {FileUtils} from 'common/file_utils';
import {PersistentStore} from 'common/persistent_store';
import {CrossToolProtocol} from 'cross_tool/cross_tool_protocol';
import {TraceDataListener} from 'interfaces/trace_data_listener';
import {LoadedTrace} from 'trace/loaded_trace';
import {Timestamp} from 'trace/timestamp';
import {TraceType} from 'trace/trace_type';
import {proxyClient, ProxyState} from 'trace_collection/proxy_client';
import {ViewerInputMethodComponent} from 'viewers/components/viewer_input_method_component';
import {View, Viewer} from 'viewers/viewer';
import {ViewerProtologComponent} from 'viewers/viewer_protolog/viewer_protolog_component';
import {ViewerScreenRecordingComponent} from 'viewers/viewer_screen_recording/viewer_screen_recording_component';
import {ViewerSurfaceFlingerComponent} from 'viewers/viewer_surface_flinger/viewer_surface_flinger_component';
import {ViewerTransactionsComponent} from 'viewers/viewer_transactions/viewer_transactions_component';
import {ViewerTransitionsComponent} from 'viewers/viewer_transitions/viewer_transitions_component';
import {ViewerWindowManagerComponent} from 'viewers/viewer_window_manager/viewer_window_manager_component';
import {CollectTracesComponent} from './collect_traces_component';
import {SnackBarOpener} from './snack_bar_opener';
import {TimelineComponent} from './timeline/timeline_component';
import {UploadTracesComponent} from './upload_traces_component';

@Component({
  selector: 'app-root',
  template: `
    <mat-toolbar class="toolbar">
      <span class="app-title">Winscope</span>

      <a href="http://go/winscope-legacy">
        <button color="primary" mat-button>Open legacy Winscope</button>
      </a>

      <div class="spacer">
        <mat-icon
          *ngIf="activeTrace"
          class="icon"
          [matTooltip]="TRACE_INFO[activeTrace.type].name"
          [style]="{color: TRACE_INFO[activeTrace.type].color, marginRight: '0.5rem'}">
          {{ TRACE_INFO[activeTrace.type].icon }}
        </mat-icon>
        <span *ngIf="dataLoaded" class="active-trace-file-info mat-body-2">
          {{ activeTraceFileInfo }}
        </span>
      </div>

      <button
        *ngIf="dataLoaded"
        color="primary"
        mat-stroked-button
        (click)="mediator.onWinscopeUploadNew()">
        Upload New
      </button>

      <button
        mat-icon-button
        matTooltip="Report bug"
        (click)="goToLink('https://b.corp.google.com/issues/new?component=909476')">
        <mat-icon> bug_report</mat-icon>
      </button>

      <button
        mat-icon-button
        matTooltip="Switch to {{ isDarkModeOn ? 'light' : 'dark' }} mode"
        (click)="setDarkMode(!isDarkModeOn)">
        <mat-icon>
          {{ isDarkModeOn ? 'brightness_5' : 'brightness_4' }}
        </mat-icon>
      </button>
    </mat-toolbar>

    <mat-divider></mat-divider>

    <mat-drawer-container class="example-container" autosize disableClose autoFocus>
      <mat-drawer-content>
        <ng-container *ngIf="dataLoaded; else noLoadedTracesBlock">
          <trace-view
            class="viewers"
            [viewers]="viewers"
            [store]="store"
            (downloadTracesButtonClick)="onDownloadTracesButtonClick()"
            (activeViewChanged)="onActiveViewChanged($event)"></trace-view>

          <mat-divider></mat-divider>
        </ng-container>
      </mat-drawer-content>

      <mat-drawer #drawer mode="overlay" opened="true" [baseHeight]="collapsedTimelineHeight">
        <timeline
          *ngIf="dataLoaded"
          [timelineData]="timelineData"
          [activeViewTraceTypes]="activeView?.dependencies"
          [availableTraces]="getLoadedTraceTypes()"
          (collapsedTimelineSizeChanged)="onCollapsedTimelineSizeChanged($event)"></timeline>
      </mat-drawer>
    </mat-drawer-container>

    <ng-template #noLoadedTracesBlock>
      <div class="center">
        <div class="landing-content">
          <h1 class="welcome-info mat-headline">
            Welcome to Winscope. Please select source to view traces.
          </h1>

          <div class="card-grid landing-grid">
            <collect-traces
              class="collect-traces-card homepage-card"
              (filesCollected)="mediator.onWinscopeFilesCollected($event)"
              [store]="store"></collect-traces>

            <upload-traces
              class="upload-traces-card homepage-card"
              [tracePipeline]="tracePipeline"
              (filesUploaded)="mediator.onWinscopeFilesUploaded($event)"
              (viewTracesButtonClick)="mediator.onWinscopeViewTracesRequest()"></upload-traces>
          </div>
        </div>
      </div>
    </ng-template>
  `,
  styles: [
    `
      .toolbar {
        gap: 10px;
      }
      .welcome-info {
        margin: 16px 0 6px 0;
        text-align: center;
      }
      .homepage-card {
        display: flex;
        flex-direction: column;
        flex: 1;
        overflow: auto;
        height: 820px;
      }
      .spacer {
        flex: 1;
        text-align: center;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      .viewers {
        height: 0;
        flex-grow: 1;
        display: flex;
        flex-direction: column;
        overflow: auto;
      }
      .center {
        display: flex;
        align-content: center;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        justify-items: center;
        flex-grow: 1;
      }
      .landing-content {
        width: 100%;
      }
      .landing-content .card-grid {
        max-width: 1800px;
        flex-grow: 1;
        margin: auto;
      }
    `,
  ],
  encapsulation: ViewEncapsulation.None,
})
export class AppComponent implements TraceDataListener {
  title = 'winscope';
  changeDetectorRef: ChangeDetectorRef;
  snackbarOpener: SnackBarOpener;
  tracePipeline = new TracePipeline();
  timelineData = new TimelineData();
  abtChromeExtensionProtocol = new AbtChromeExtensionProtocol();
  crossToolProtocol = new CrossToolProtocol();
  mediator: Mediator;
  states = ProxyState;
  store: PersistentStore = new PersistentStore();
  currentTimestamp?: Timestamp;
  viewers: Viewer[] = [];
  isDarkModeOn!: boolean;
  dataLoaded = false;
  activeView?: View;
  activeTrace?: LoadedTrace;
  activeTraceFileInfo = '';
  collapsedTimelineHeight = 0;
  @ViewChild(UploadTracesComponent) uploadTracesComponent?: UploadTracesComponent;
  @ViewChild(CollectTracesComponent) collectTracesComponent?: UploadTracesComponent;
  @ViewChild(TimelineComponent) timelineComponent?: TimelineComponent;
  TRACE_INFO = TRACE_INFO;

  constructor(
    @Inject(Injector) injector: Injector,
    @Inject(ChangeDetectorRef) changeDetectorRef: ChangeDetectorRef,
    @Inject(SnackBarOpener) snackBar: SnackBarOpener
  ) {
    this.changeDetectorRef = changeDetectorRef;
    this.snackbarOpener = snackBar;
    this.mediator = new Mediator(
      this.tracePipeline,
      this.timelineData,
      this.abtChromeExtensionProtocol,
      this.crossToolProtocol,
      this,
      this.snackbarOpener,
      localStorage
    );

    const storeDarkMode = this.store.get('dark-mode');
    const prefersDarkQuery = window.matchMedia?.('(prefers-color-scheme: dark)');
    this.setDarkMode(storeDarkMode ? storeDarkMode === 'true' : prefersDarkQuery.matches);

    if (!customElements.get('viewer-input-method')) {
      customElements.define(
        'viewer-input-method',
        createCustomElement(ViewerInputMethodComponent, {injector})
      );
    }
    if (!customElements.get('viewer-protolog')) {
      customElements.define(
        'viewer-protolog',
        createCustomElement(ViewerProtologComponent, {injector})
      );
    }
    if (!customElements.get('viewer-screen-recording')) {
      customElements.define(
        'viewer-screen-recording',
        createCustomElement(ViewerScreenRecordingComponent, {injector})
      );
    }
    if (!customElements.get('viewer-surface-flinger')) {
      customElements.define(
        'viewer-surface-flinger',
        createCustomElement(ViewerSurfaceFlingerComponent, {injector})
      );
    }
    if (!customElements.get('viewer-transactions')) {
      customElements.define(
        'viewer-transactions',
        createCustomElement(ViewerTransactionsComponent, {injector})
      );
    }
    if (!customElements.get('viewer-window-manager')) {
      customElements.define(
        'viewer-window-manager',
        createCustomElement(ViewerWindowManagerComponent, {injector})
      );
    }
    if (!customElements.get('viewer-transitions')) {
      customElements.define(
        'viewer-transitions',
        createCustomElement(ViewerTransitionsComponent, {injector})
      );
    }
  }

  ngAfterViewInit() {
    this.mediator.onWinscopeInitialized();
  }

  ngAfterViewChecked() {
    this.mediator.setUploadTracesComponent(this.uploadTracesComponent);
    this.mediator.setCollectTracesComponent(this.collectTracesComponent);
    this.mediator.setTimelineComponent(this.timelineComponent);
  }

  onCollapsedTimelineSizeChanged(height: number) {
    this.collapsedTimelineHeight = height;
    this.changeDetectorRef.detectChanges();
  }

  getLoadedTraceTypes(): TraceType[] {
    return this.tracePipeline.getLoadedTraces().map((trace) => trace.type);
  }

  onTraceDataLoaded(viewers: Viewer[]) {
    this.viewers = viewers;
    this.dataLoaded = true;
    this.changeDetectorRef.detectChanges();
  }

  onTraceDataUnloaded() {
    proxyClient.adbData = [];
    this.dataLoaded = false;
    this.changeDetectorRef.detectChanges();
  }

  setDarkMode(enabled: boolean) {
    document.body.classList.toggle('dark-mode', enabled);
    this.store.add('dark-mode', `${enabled}`);
    this.isDarkModeOn = enabled;
  }

  async onDownloadTracesButtonClick() {
    const traceFiles = await this.makeTraceFilesForDownload();
    const zipFileBlob = await FileUtils.createZipArchive(traceFiles);
    const zipFileName = 'winscope.zip';

    const a = document.createElement('a');
    document.body.appendChild(a);
    const url = window.URL.createObjectURL(zipFileBlob);
    a.href = url;
    a.download = zipFileName;
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  }

  onActiveViewChanged(view: View) {
    this.activeView = view;
    this.activeTrace = this.getActiveTrace(view);
    this.activeTraceFileInfo = this.makeActiveTraceFileInfo(view);
    this.timelineData.setActiveViewTraceTypes(view.dependencies);
  }

  goToLink(url: string) {
    window.open(url, '_blank');
  }

  private makeActiveTraceFileInfo(view: View): string {
    const trace = this.getActiveTrace(view);

    if (!trace) {
      return '';
    }

    return `${trace.descriptors.join(', ')}`;
  }

  private getActiveTrace(view: View): LoadedTrace | undefined {
    return this.tracePipeline
      .getLoadedTraces()
      .find((trace) => trace.type === view.dependencies[0]);
  }

  private async makeTraceFilesForDownload(): Promise<File[]> {
    const loadedFiles = this.tracePipeline.getLoadedFiles();
    return [...loadedFiles.keys()].map((traceType) => {
      const file = loadedFiles.get(traceType)!;
      const path = TRACE_INFO[traceType].downloadArchiveDir;

      const newName = path + '/' + FileUtils.removeDirFromFileName(file.file.name);
      return new File([file.file], newName);
    });
  }
}
