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
  NgZone,
  ViewChild,
  ViewEncapsulation,
} from '@angular/core';
import {createCustomElement} from '@angular/elements';
import {FormControl, Validators} from '@angular/forms';
import {Title} from '@angular/platform-browser';
import {AbtChromeExtensionProtocol} from 'abt_chrome_extension/abt_chrome_extension_protocol';
import {Mediator} from 'app/mediator';
import {TimelineData} from 'app/timeline_data';
import {TRACE_INFO} from 'app/trace_info';
import {TracePipeline} from 'app/trace_pipeline';
import {FileUtils} from 'common/file_utils';
import {globalConfig} from 'common/global_config';
import {PersistentStore} from 'common/persistent_store';
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {Timestamp} from 'common/time';
import {CrossToolProtocol} from 'cross_tool/cross_tool_protocol';
import {
  AppFilesCollected,
  AppFilesUploaded,
  AppInitialized,
  AppResetRequest,
  AppTraceViewRequest,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {WinscopeEventListener} from 'messaging/winscope_event_listener';
import {MockStorage} from 'test/unit/mock_storage';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {proxyClient, ProxyState} from 'trace_collection/proxy_client';
import {
  TraceConfigurationMap,
  TRACES,
} from 'trace_collection/trace_collection_utils';
import {ViewerInputMethodComponent} from 'viewers/components/viewer_input_method_component';
import {View, Viewer} from 'viewers/viewer';
import {ViewerProtologComponent} from 'viewers/viewer_protolog/viewer_protolog_component';
import {ViewerScreenRecordingComponent} from 'viewers/viewer_screen_recording/viewer_screen_recording_component';
import {ViewerSurfaceFlingerComponent} from 'viewers/viewer_surface_flinger/viewer_surface_flinger_component';
import {ViewerTransactionsComponent} from 'viewers/viewer_transactions/viewer_transactions_component';
import {ViewerTransitionsComponent} from 'viewers/viewer_transitions/viewer_transitions_component';
import {ViewerViewCaptureComponent} from 'viewers/viewer_view_capture/viewer_view_capture_component';
import {ViewerWindowManagerComponent} from 'viewers/viewer_window_manager/viewer_window_manager_component';
import {CollectTracesComponent} from './collect_traces_component';
import {SnackBarOpener} from './snack_bar_opener';
import {TimelineComponent} from './timeline/timeline_component';
import {TraceViewComponent} from './trace_view_component';
import {UploadTracesComponent} from './upload_traces_component';

@Component({
  selector: 'app-root',
  template: `
    <mat-toolbar class="toolbar">
      <div class="horizontal-align vertical-align">
        <span class="app-title fixed">Winscope</span>
        <div class="horizontal-align vertical-align active" *ngIf="showDataLoadedElements">
          <button
            *ngIf="activeTrace"
            mat-icon-button
            [disabled]="true">
            <mat-icon
              class="icon"
              [matTooltip]="TRACE_INFO[activeTrace.type].name"
              [style]="{color: TRACE_INFO[activeTrace.type].color}">
              {{ TRACE_INFO[activeTrace.type].icon }}
            </mat-icon>
          </button>
          <span class="trace-file-info mat-body-1" [matTooltip]="activeTraceFileInfo">
            {{ activeTraceFileInfo }}
          </span>
        </div>
      </div>

      <div class="horizontal-align vertical-align fixed">
        <mat-icon class="material-symbols-outlined" *ngIf="showDataLoadedElements">description</mat-icon>
        <div *ngIf="showDataLoadedElements" class="file-descriptor vertical-align">
          <span *ngIf="!isEditingFilename" class="download-file-info mat-body-2">
            {{ filenameFormControl.value }}
          </span>
          <span *ngIf="!isEditingFilename" class="download-file-ext mat-body-2">.zip</span>
          <mat-form-field
            class="file-name-input-field"
            *ngIf="isEditingFilename"
            floatLabel="always"
            (keydown.enter)="onCheckIconClick()"
            (focusout)="onCheckIconClick()"
            matTooltip="Allowed: A-Z a-z 0-9 . _ - #">
            <mat-label>Edit file name</mat-label>
            <input matInput class="right-align" [formControl]="filenameFormControl" />
            <span matSuffix>.zip</span>
          </mat-form-field>
          <button
            *ngIf="isEditingFilename"
            mat-icon-button
            class="check-button"
            matTooltip="Submit file name"
            (click)="onCheckIconClick()">
            <mat-icon>check</mat-icon>
          </button>
          <button
            *ngIf="!isEditingFilename"
            mat-icon-button
            class="edit-button"
            matTooltip="Edit file name"
            (click)="onPencilIconClick()">
            <mat-icon>edit</mat-icon>
          </button>
          <button
            mat-icon-button
            *ngIf="!isEditingFilename"
            matTooltip="Download all traces"
            class="save-button"
            (click)="onDownloadTracesButtonClick()">
            <mat-icon class="material-symbols-outlined">download</mat-icon>
          </button>
        </div>

        <div *ngIf="showDataLoadedElements" class="icon-divider"></div>
        <button
          *ngIf="showDataLoadedElements"
          color="primary"
          mat-icon-button
          matTooltip="Upload or collect new trace"
          class="upload-new"
          (click)="onUploadNewButtonClick()">
          <mat-icon class="material-symbols-outlined">upload</mat-icon>
        </button>

        <button
          mat-icon-button
          matTooltip="Documentation"
          class="documentation"
          (click)="
            goToLink('https://source.android.com/docs/core/graphics/tracing-win-transitions')
          ">
          <mat-icon>menu_book</mat-icon>
        </button>

        <button
          mat-icon-button
          class="report-bug"
          matTooltip="Report bug"
          (click)="goToLink('https://b.corp.google.com/issues/new?component=909476')">
          <mat-icon>bug_report</mat-icon>
        </button>

        <button
          mat-icon-button
          class="dark-mode"
          matTooltip="Switch to {{ isDarkModeOn ? 'light' : 'dark' }} mode"
          (click)="setDarkMode(!isDarkModeOn)">
          <mat-icon>
            {{ isDarkModeOn ? 'brightness_5' : 'brightness_4' }}
          </mat-icon>
        </button>
      </div>
    </mat-toolbar>

    <mat-divider></mat-divider>

    <mat-drawer-container autosize disableClose autoFocus>
      <mat-drawer-content>
        <ng-container *ngIf="dataLoaded; else noLoadedTracesBlock">
          <trace-view class="viewers" [viewers]="viewers" [store]="store"></trace-view>

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
              [traceConfig]="traceConfig"
              [dumpConfig]="dumpConfig"
              [storage]="traceConfigStorage"
              (filesCollected)="onFilesCollected($event)"></collect-traces>

            <upload-traces
              class="upload-traces-card homepage-card"
              [tracePipeline]="tracePipeline"
              (filesUploaded)="onFilesUploaded($event)"
              (viewTracesButtonClick)="onViewTracesButtonClick()"></upload-traces>
          </div>
        </div>
      </div>
    </ng-template>
  `,
  styles: [
    `
      .toolbar {
        gap: 10px;
        justify-content: space-between;
        min-height: 64px;
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
      .trace-file-info {
        text-overflow: ellipsis;
        overflow-x: hidden;
        max-width: 100%;
        padding-top: 3px;
        color: #063C8C;
      }
      .horizontal-align {
        justify-content: center;
      }
      .vertical-align {
        text-align: center;
        align-items: center;
        overflow-x: hidden;
        display: flex;
      }
      .fixed {
        min-width: fit-content;
      }
      .file-descriptor {
        font-size: 14px;
        padding-left: 10px;
        width: 350px;
      }
      .download-file-info {
        text-overflow: ellipsis;
        overflow-x: hidden;
        padding-top: 3px;
        max-width: 300px;
      }
      .download-file-ext {
        padding-top: 3px;
        max-width: 300px;
      }
      .file-name-input-field .right-align {
        text-align: right;
      }
      .file-name-input-field .mat-form-field-wrapper {
        padding-bottom: 10px;
        width: 300px;
      }
      .icon-divider {
        width: 1px;
        background-color: #C4C0C0;
        margin-right: 6px;
        margin-left: 6px;
        height: 20px;
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
export class AppComponent implements WinscopeEventListener {
  title = 'winscope';
  timelineData = new TimelineData();
  abtChromeExtensionProtocol = new AbtChromeExtensionProtocol();
  crossToolProtocol = new CrossToolProtocol();
  states = ProxyState;
  dataLoaded = false;
  showDataLoadedElements = false;
  activeTraceFileInfo = '';
  collapsedTimelineHeight = 0;
  TRACE_INFO = TRACE_INFO;
  isEditingFilename = false;
  store = new PersistentStore();
  viewers: Viewer[] = [];

  isDarkModeOn!: boolean;
  changeDetectorRef: ChangeDetectorRef;
  snackbarOpener: SnackBarOpener;
  tracePipeline: TracePipeline;
  mediator: Mediator;
  currentTimestamp?: Timestamp;
  activeView?: View;
  activeTrace?: Trace<object>;
  filenameFormControl = new FormControl(
    'winscope',
    Validators.compose([
      Validators.required,
      Validators.pattern(FileUtils.DOWNLOAD_FILENAME_REGEX),
    ]),
  );
  traceConfig: TraceConfigurationMap;
  dumpConfig: TraceConfigurationMap;
  traceConfigStorage: Storage;

  @ViewChild(UploadTracesComponent)
  uploadTracesComponent?: UploadTracesComponent;
  @ViewChild(CollectTracesComponent)
  collectTracesComponent?: UploadTracesComponent;
  @ViewChild(TraceViewComponent) traceViewComponent?: TraceViewComponent;
  @ViewChild(TimelineComponent) timelineComponent?: TimelineComponent;

  constructor(
    @Inject(Injector) injector: Injector,
    @Inject(ChangeDetectorRef) changeDetectorRef: ChangeDetectorRef,
    @Inject(SnackBarOpener) snackBar: SnackBarOpener,
    @Inject(Title) private pageTitle: Title,
    @Inject(NgZone) private ngZone: NgZone,
  ) {
    this.changeDetectorRef = changeDetectorRef;
    this.snackbarOpener = snackBar;
    this.tracePipeline = new TracePipeline();
    this.mediator = new Mediator(
      this.tracePipeline,
      this.timelineData,
      this.abtChromeExtensionProtocol,
      this.crossToolProtocol,
      this,
      this.snackbarOpener,
      localStorage,
    );

    const storeDarkMode = this.store.get('dark-mode');
    const prefersDarkQuery = window.matchMedia?.(
      '(prefers-color-scheme: dark)',
    );
    this.setDarkMode(
      storeDarkMode ? storeDarkMode === 'true' : prefersDarkQuery.matches,
    );

    if (!customElements.get('viewer-input-method')) {
      customElements.define(
        'viewer-input-method',
        createCustomElement(ViewerInputMethodComponent, {injector}),
      );
    }
    if (!customElements.get('viewer-protolog')) {
      customElements.define(
        'viewer-protolog',
        createCustomElement(ViewerProtologComponent, {injector}),
      );
    }
    if (!customElements.get('viewer-screen-recording')) {
      customElements.define(
        'viewer-screen-recording',
        createCustomElement(ViewerScreenRecordingComponent, {injector}),
      );
    }
    if (!customElements.get('viewer-surface-flinger')) {
      customElements.define(
        'viewer-surface-flinger',
        createCustomElement(ViewerSurfaceFlingerComponent, {injector}),
      );
    }
    if (!customElements.get('viewer-transactions')) {
      customElements.define(
        'viewer-transactions',
        createCustomElement(ViewerTransactionsComponent, {injector}),
      );
    }
    if (!customElements.get('viewer-window-manager')) {
      customElements.define(
        'viewer-window-manager',
        createCustomElement(ViewerWindowManagerComponent, {injector}),
      );
    }
    if (!customElements.get('viewer-transitions')) {
      customElements.define(
        'viewer-transitions',
        createCustomElement(ViewerTransitionsComponent, {injector}),
      );
    }
    if (!customElements.get('viewer-view-capture')) {
      customElements.define(
        'viewer-view-capture',
        createCustomElement(ViewerViewCaptureComponent, {injector}),
      );
    }

    this.traceConfigStorage =
      globalConfig.MODE === 'PROD' ? localStorage : new MockStorage();

    this.traceConfig = PersistentStoreProxy.new<TraceConfigurationMap>(
      'TracingSettings',
      TRACES['default'],
      this.traceConfigStorage,
    );
    this.dumpConfig = PersistentStoreProxy.new<TraceConfigurationMap>(
      'DumpSettings',
      {
        window_dump: {
          name: 'Window Manager',
          isTraceCollection: undefined,
          run: true,
          config: undefined,
        },
        layers_dump: {
          name: 'Surface Flinger',
          isTraceCollection: undefined,
          run: true,
          config: undefined,
        },
        screenshot: {
          name: 'Screenshot',
          isTraceCollection: undefined,
          run: true,
          config: undefined,
        },
      },
      this.traceConfigStorage,
    );
  }

  async ngAfterViewInit() {
    await this.mediator.onWinscopeEvent(new AppInitialized());
  }

  ngAfterViewChecked() {
    this.mediator.setUploadTracesComponent(this.uploadTracesComponent);
    this.mediator.setCollectTracesComponent(this.collectTracesComponent);
    this.mediator.setTraceViewComponent(this.traceViewComponent);
    this.mediator.setTimelineComponent(this.timelineComponent);
  }

  onCollapsedTimelineSizeChanged(height: number) {
    this.collapsedTimelineHeight = height;
    this.changeDetectorRef.detectChanges();
  }

  getLoadedTraceTypes(): TraceType[] {
    return this.tracePipeline.getTraces().mapTrace((trace) => trace.type);
  }

  setDarkMode(enabled: boolean) {
    document.body.classList.toggle('dark-mode', enabled);
    this.store.add('dark-mode', `${enabled}`);
    this.isDarkModeOn = enabled;
  }

  onPencilIconClick() {
    this.isEditingFilename = true;
  }

  onCheckIconClick() {
    if (this.filenameFormControl.invalid) {
      return;
    }
    this.isEditingFilename = false;
    this.pageTitle.setTitle(`Winscope | ${this.filenameFormControl.value}`);
  }

  async onDownloadTracesButtonClick() {
    if (this.filenameFormControl.invalid) {
      return;
    }
    await this.downloadTraces();
  }

  async onFilesCollected(files: File[]) {
    await this.mediator.onWinscopeEvent(new AppFilesCollected(files));
  }

  async onFilesUploaded(files: File[]) {
    await this.mediator.onWinscopeEvent(new AppFilesUploaded(files));
  }

  async onUploadNewButtonClick() {
    await this.mediator.onWinscopeEvent(new AppResetRequest());
    this.store.clear('treeView');
  }

  async onViewTracesButtonClick() {
    await this.mediator.onWinscopeEvent(new AppTraceViewRequest());
  }

  async downloadTraces() {
    const archiveBlob =
      await this.tracePipeline.makeZipArchiveWithLoadedTraceFiles();
    const archiveFilename = `${this.filenameFormControl.value}.zip`;

    const a = document.createElement('a');
    document.body.appendChild(a);
    const url = window.URL.createObjectURL(archiveBlob);
    a.href = url;
    a.download = archiveFilename;
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  }

  async onWinscopeEvent(event: WinscopeEvent) {
    await event.visit(WinscopeEventType.TABBED_VIEW_SWITCHED, async (event) => {
      this.activeView = event.newFocusedView;
      this.activeTrace = this.getActiveTrace(event.newFocusedView);
      this.activeTraceFileInfo = this.makeActiveTraceFileInfo(
        event.newFocusedView,
      );
    });

    await event.visit(WinscopeEventType.VIEWERS_LOADED, async (event) => {
      this.viewers = event.viewers;
      this.filenameFormControl.setValue(
        this.tracePipeline.getDownloadArchiveFilename(),
      );
      this.pageTitle.setTitle(`Winscope | ${this.filenameFormControl.value}`);
      this.isEditingFilename = false;

      // some elements e.g. timeline require dataLoaded to be set outside NgZone to render
      this.dataLoaded = true;
      this.changeDetectorRef.detectChanges();

      // tooltips must be rendered inside ngZone due to limitation of MatTooltip,
      // therefore toolbar elements controlled by a different boolean
      this.ngZone.run(() => {
        this.showDataLoadedElements = true;
      });
    });

    await event.visit(WinscopeEventType.VIEWERS_UNLOADED, async (event) => {
      proxyClient.adbData = [];
      this.dataLoaded = false;
      this.showDataLoadedElements = false;
      this.pageTitle.setTitle('Winscope');
      this.activeView = undefined;
      this.changeDetectorRef.detectChanges();
    });
  }

  goToLink(url: string) {
    window.open(url, '_blank');
  }

  private makeActiveTraceFileInfo(view: View): string {
    const trace = this.getActiveTrace(view);

    if (!trace) {
      return '';
    }

    return `${trace.getDescriptors().join(', ')}`;
  }

  private getActiveTrace(view: View): Trace<object> | undefined {
    let activeTrace: Trace<object> | undefined;
    this.tracePipeline.getTraces().forEachTrace((trace) => {
      if (trace.type === view.dependencies[0]) {
        activeTrace = trace;
      }
    });
    return activeTrace;
  }
}
