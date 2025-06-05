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
import {ClipboardModule} from '@angular/cdk/clipboard';
import {OverlayModule} from '@angular/cdk/overlay';
import {CommonModule} from '@angular/common';
import {HttpClientModule} from '@angular/common/http';
import {ChangeDetectionStrategy} from '@angular/core';
import {ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {
  FormControl,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatDialogModule} from '@angular/material/dialog';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatListModule} from '@angular/material/list';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatSelectModule} from '@angular/material/select';
import {MatSliderModule} from '@angular/material/slider';
import {MatSnackBarModule} from '@angular/material/snack-bar';
import {MatTabsModule} from '@angular/material/tabs';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatTooltipModule} from '@angular/material/tooltip';
import {Title} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {Download} from 'common/download';
import {FileUtils} from 'common/file_utils';
import {TimestampConverterUtils} from 'common/time/test_utils';
import {UserNotifier} from 'common/user_notifier';
import {
  FailedToInitializeTimelineData,
  NoValidFiles,
} from 'messaging/user_warnings';
import {
  AppRefreshDumpsRequest,
  BugreportFileSelected,
  BugreportFileSelectionRequest,
  ViewersLoaded,
  ViewersUnloaded,
} from 'messaging/winscope_event';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {waitToBeCalled} from 'test/unit/spy_utils';
import {TracesBuilder} from 'test/unit/traces_builder';
import {ViewerSurfaceFlingerComponent} from 'viewers/viewer_surface_flinger/viewer_surface_flinger_component';
import {AppComponent} from './app_component';
import {
  MatDrawer,
  MatDrawerContainer,
  MatDrawerContent,
} from './bottomnav/bottom_drawer_component';
import {CollectTracesComponent} from './collect_traces_component';
import {ShortcutsComponent} from './shortcuts_component';
import {SnackBarComponent} from './snack_bar_component';
import {MiniTimelineComponent} from './timeline/mini-timeline/mini_timeline_component';
import {TimelineComponent} from './timeline/timeline_component';
import {TraceConfigComponent} from './trace_config_component';
import {TraceViewComponent} from './trace_view_component';
import {UploadTracesComponent} from './upload_traces_component';
import {WarningDialogComponent} from './warning_dialog_component';
import {WdpSetupComponent} from './wdp_setup_component';
import {WinscopeProxySetupComponent} from './winscope_proxy_setup_component';

describe('AppComponent', () => {
  let component: AppComponent;
  let downloadTracesSpy: jasmine.Spy;
  let dom: DOMTestHelper<AppComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [Title, {provide: ComponentFixtureAutoDetect, useValue: true}],
      imports: [
        CommonModule,
        FormsModule,
        MatCardModule,
        MatButtonModule,
        MatDividerModule,
        MatFormFieldModule,
        MatIconModule,
        MatSelectModule,
        MatSliderModule,
        MatSnackBarModule,
        MatToolbarModule,
        MatTooltipModule,
        ReactiveFormsModule,
        MatInputModule,
        BrowserAnimationsModule,
        ClipboardModule,
        MatDialogModule,
        HttpClientModule,
        MatListModule,
        MatProgressBarModule,
        OverlayModule,
        MatTabsModule,
        MatCheckboxModule,
      ],
      declarations: [
        WinscopeProxySetupComponent,
        WdpSetupComponent,
        AppComponent,
        CollectTracesComponent,
        MatDrawer,
        MatDrawerContainer,
        MatDrawerContent,
        MiniTimelineComponent,
        TimelineComponent,
        TraceConfigComponent,
        TraceViewComponent,
        UploadTracesComponent,
        ViewerSurfaceFlingerComponent,
        ShortcutsComponent,
        SnackBarComponent,
        WarningDialogComponent,
      ],
    })
      .overrideComponent(AppComponent, {
        set: {changeDetection: ChangeDetectionStrategy.Default},
      })
      .compileComponents();
    const fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    component.filenameFormControl = new FormControl(
      'winscope',
      Validators.compose([
        Validators.required,
        Validators.pattern(FileUtils.DOWNLOAD_FILENAME_REGEX),
      ]),
    );
    downloadTracesSpy = spyOn(Download, 'fromUrl');
    dom.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('has the expected title', () => {
    expect(component.title).toEqual('winscope');
  });

  it('shows permanent header items on homepage', () => {
    checkPermanentHeaderItems();
  });

  it('displays correct elements when no data loaded', () => {
    component.dataLoaded = false;
    component.showDataLoadedElements = false;
    dom.detectChanges();
    checkHomepage();
  });

  it('displays correct elements when data loaded', () => {
    goToTraceView();
    checkTraceViewPage();

    spyOn(component, 'dumpsUploaded').and.returnValue(true);
    dom.detectChanges();
    expect(dom.find('.refresh-dumps')).toBeTruthy();
  });

  it('returns to homepage on upload new button click', async () => {
    goToTraceView();
    checkTraceViewPage();
    await dom.clickAndWaitStable('.upload-new');
    await dom.detectChangesAndWaitStable();
    checkHomepage();
  });

  it('sends event on refresh dumps button click', async () => {
    spyOn(component, 'dumpsUploaded').and.returnValue(true);
    goToTraceView();
    checkTraceViewPage();

    const winscopeEventSpy = spyOn(
      component.mediator,
      'onWinscopeEvent',
    ).and.callThrough();
    await dom.clickAndWaitStable('.refresh-dumps');
    await dom.detectChangesAndWaitStable();
    checkHomepage();
    expect(winscopeEventSpy).toHaveBeenCalledWith(new AppRefreshDumpsRequest());
  });

  it('shows download progress bar', () => {
    component.showDataLoadedElements = true;
    dom.detectChanges();
    expect(
      dom.find('.download-files-section mat-progress-bar'),
    ).toBeUndefined();

    component.onProgressUpdate('Progress update', 10);
    dom.detectChanges();
    expect(dom.find('.download-files-section mat-progress-bar')).toBeTruthy();

    component.onOperationFinished(true);
    dom.detectChanges();
    expect(
      dom.find('.download-files-section mat-progress-bar'),
    ).toBeUndefined();
  });

  it('downloads traces on download button click and shows download progress bar', async () => {
    component.showDataLoadedElements = true;
    dom.detectChanges();
    clickDownloadTracesButton();
    expect(dom.find('.download-files-section mat-progress-bar')).toBeTruthy();
    await waitToBeCalled(downloadTracesSpy);
  });

  it('downloads traces after valid file name change', async () => {
    component.showDataLoadedElements = true;
    dom.detectChanges();

    clickEditFilenameButton();
    updateFilenameInputAndDownloadTraces('Winscope2', true);
    await waitToBeCalled(downloadTracesSpy);
    expect(downloadTracesSpy).toHaveBeenCalledOnceWith(
      jasmine.any(String),
      'Winscope2.zip',
    );

    downloadTracesSpy.calls.reset();

    // check it works twice in a row
    clickEditFilenameButton();
    updateFilenameInputAndDownloadTraces('win_scope', true);
    await waitToBeCalled(downloadTracesSpy);
    expect(downloadTracesSpy).toHaveBeenCalledOnceWith(
      jasmine.any(String),
      'win_scope.zip',
    );
  });

  it('changes page title based on archive name', async () => {
    const pageTitle = TestBed.inject(Title);
    component.timelineData.initialize(
      new TracesBuilder().build(),
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );

    await component.onWinscopeEvent(new ViewersUnloaded());
    expect(pageTitle.getTitle()).toBe('Winscope');

    component.tracePipeline.getDownloadArchiveFilename = jasmine
      .createSpy()
      .and.returnValue('test_archive');
    await component.onWinscopeEvent(new ViewersLoaded([]));
    dom.detectChanges();
    expect(pageTitle.getTitle()).toBe('Winscope | test_archive');
  });

  it('does not download traces if invalid file name chosen', () => {
    component.showDataLoadedElements = true;
    dom.detectChanges();

    clickEditFilenameButton();
    updateFilenameInputAndDownloadTraces('w?n$cope', false);
    expect(downloadTracesSpy).not.toHaveBeenCalled();
  });

  it('behaves as expected when entering valid then invalid then valid file names', async () => {
    component.showDataLoadedElements = true;
    dom.detectChanges();

    clickEditFilenameButton();
    updateFilenameInputAndDownloadTraces('Winscope2', true);
    await waitToBeCalled(downloadTracesSpy);
    expect(downloadTracesSpy).toHaveBeenCalledOnceWith(
      jasmine.any(String),
      'Winscope2.zip',
    );
    downloadTracesSpy.calls.reset();

    clickEditFilenameButton();
    updateFilenameInputAndDownloadTraces('w?n$cope', false);
    expect(downloadTracesSpy).not.toHaveBeenCalled();

    updateFilenameInputAndDownloadTraces('win.scope', true);
    await waitToBeCalled(downloadTracesSpy);
    expect(downloadTracesSpy).toHaveBeenCalledOnceWith(
      jasmine.any(String),
      'win.scope.zip',
    );
  });

  it('validates filename on enter key, escape key or focus out', () => {
    const spy = spyOn(component, 'trySubmitFilename');

    component.showDataLoadedElements = true;
    dom.detectChanges();
    clickEditFilenameButton();
    const inputField = dom.get('.file-name-input-field');
    inputField.get('input').updateValue('valid_file_name');

    inputField.keydownEnter();
    expect(spy).toHaveBeenCalledTimes(1);

    inputField.keydownEsc();
    expect(spy).toHaveBeenCalledTimes(2);

    inputField.focusOut();
    expect(spy).toHaveBeenCalledTimes(3);
  });

  it('downloads traces from upload traces section', () => {
    const traces = assertDefined(component.tracePipeline.getTraces());
    spyOn(traces, 'getSize').and.returnValue(1);
    dom.detectChanges();
    const downloadButtonClickSpy = spyOn(
      component,
      'onDownloadTracesButtonClick',
    );
    dom.findAndClick('upload-traces .download-btn');
    expect(downloadButtonClickSpy).toHaveBeenCalledOnceWith(
      component.uploadTracesComponent,
    );
  });

  it('shows cross tool sync button', async () => {
    component.showDataLoadedElements = true;
    dom.detectChanges();
    const fileDescriptor = dom.get('.file-descriptor');
    expect(fileDescriptor.find('.cross-tool-sync-button')).toBeUndefined();

    spyOn(component.crossToolProtocol, 'isConnected').and.returnValue(true);
    dom.detectChanges();
    const syncButton = fileDescriptor.get('.cross-tool-sync-button');
    await syncButton.checkTooltip('Cross Tool Sync ON (Click to turn OFF)');
    syncButton.checkClassName('mat-primary', true);
    syncButton.checkClassName('mat-accent', false);

    syncButton.click();
    await syncButton.checkTooltip('Cross Tool Sync OFF (Click to turn ON)');
    syncButton.checkClassName('mat-accent', true);
    syncButton.checkClassName('mat-primary', false);

    syncButton.click();
    await syncButton.checkTooltip('Cross Tool Sync ON (Click to turn OFF)');
    syncButton.checkClassName('mat-primary', true);
    syncButton.checkClassName('mat-accent', false);
  });

  it('shows warning icon for packet loss', async () => {
    component.showDataLoadedElements = true;
    dom.detectChanges();
    const fileDescriptor = dom.get('.file-descriptor');
    fileDescriptor.checkClassName('file-warning', false);
    expect(fileDescriptor.find('.warning-icon')).toBeUndefined();

    const spy = spyOn(component.tracePipeline, 'lostPackets').and.returnValue(
      1,
    );
    dom.detectChanges();
    fileDescriptor.checkClassName('file-warning', true);
    const warningIcon = fileDescriptor.get('.warning-icon');
    await warningIcon.checkTooltip(
      '1 Perfetto packet lost during tracing - data may be incomplete',
    );

    spy.and.returnValue(4);
    dom.detectChanges();
    await warningIcon.checkTooltip(
      '4 Perfetto packets lost during tracing - data may be incomplete',
    );
  });

  it('opens shortcuts dialog', () => {
    expect(dom.findInDocument('shortcuts-panel')).toBeUndefined();
    dom.findAndClick('.shortcuts');
    expect(dom.findInDocument('shortcuts-panel')).toBeTruthy();
  });

  it('sets snackbar opener to global user notifier', () => {
    expect(dom.findInDocument('snack-bar')).toBeUndefined();
    UserNotifier.add(new NoValidFiles());
    UserNotifier.notify();
    expect(dom.findInDocument('snack-bar')).toBeTruthy();
  });

  it('does not open new snackbar until existing snackbar has been dismissed', async () => {
    expect(dom.findInDocument('snack-bar')).toBeUndefined();
    const firstMessage = new NoValidFiles();
    UserNotifier.add(firstMessage);
    UserNotifier.notify();
    await dom.detectChangesAndRenderingDone();
    let snackbar = dom.getSnackBar();
    snackbar.checkText(firstMessage.getMessage());

    const secondMessage = new FailedToInitializeTimelineData();
    UserNotifier.add(secondMessage);
    UserNotifier.notify();
    await dom.detectChangesAndRenderingDone();
    snackbar = dom.getSnackBar();
    snackbar.checkText(firstMessage.getMessage());

    snackbar.findAndClick('.snack-bar-actions .close-button');
    await dom.whenRenderingDone();
    snackbar = dom.getSnackBar();
    snackbar.checkText(secondMessage.getMessage());
  });

  it('shows bugreport selection dialog', async () => {
    expect(dom.findInDocument('warning-dialog')).toBeUndefined();
    let eventHandled = false;
    component
      .onWinscopeEvent(new BugreportFileSelectionRequest(['f1', 'f2']))
      .then(() => {
        eventHandled = true;
      });
    await dom.whenStable();
    const dialog = dom.getInDocument('warning-dialog');
    expect(eventHandled).toBeFalse();

    dialog
      .get('.warning-message')
      .checkTextExact('Multiple Perfetto traces found. Select one to process:');
    const [option1, option2] = dialog.findAll(
      '.warning-action-boxes .mat-checkbox',
    );
    option1.checkTextExact('f1');
    option2.checkTextExact('f2');
    option2.dispatchEvent(new Event('change'));
    await dom.whenStable();
    expect(eventHandled).toBeFalse();

    const mediatorSpy = spyOn(component.mediator, 'onWinscopeEvent');
    const actions = dialog.findAll('.warning-action-buttons button');
    expect(actions.length).toEqual(1);
    actions[0].click();
    await dom.whenStable();
    expect(eventHandled).toBeTrue();
    expect(mediatorSpy).toHaveBeenCalledOnceWith(
      new BugreportFileSelected('f2'),
    );
    expect(dom.findInDocument('warning-dialog')).toBeUndefined();
  });

  function goToTraceView() {
    component.dataLoaded = true;
    component.showDataLoadedElements = true;
    component.timelineData.initialize(
      new TracesBuilder().build(),
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    dom.detectChanges();
  }

  function updateFilenameInputAndDownloadTraces(name: string, valid: boolean) {
    dom.findAndDispatchInput('.file-name-input-field', name);
    dom.findAndClick('.check-button');

    const saveButton = dom.get('.save-button');
    if (valid) {
      expect(dom.find('.download-file-info')).toBeTruthy();
      saveButton.checkDisabled(false);
      clickDownloadTracesButton();
    } else {
      expect(dom.find('.download-file-info')).toBeUndefined();
      saveButton.checkDisabled(true);
    }
  }

  function clickDownloadTracesButton() {
    dom.findAndClick('.save-button');
  }

  function clickEditFilenameButton() {
    dom.findAndClick('.edit-button');
  }

  function checkHomepage() {
    expect(dom.find('.welcome-info')).toBeTruthy();
    expect(dom.find('.collect-traces-card')).toBeTruthy();
    expect(dom.find('.upload-traces-card')).toBeTruthy();
    expect(dom.find('.viewers')).toBeUndefined();
    expect(dom.find('.upload-new')).toBeUndefined();
    expect(dom.find('timeline')).toBeUndefined();
    checkPermanentHeaderItems();
  }

  function checkTraceViewPage() {
    expect(dom.find('.welcome-info')).toBeUndefined();
    expect(dom.find('.save-button')).toBeTruthy();
    expect(dom.find('.collect-traces-card')).toBeUndefined();
    expect(dom.find('.upload-traces-card')).toBeUndefined();
    expect(dom.find('.viewers')).toBeTruthy();
    expect(dom.find('.upload-new')).toBeTruthy();
    expect(dom.find('timeline')).toBeTruthy();
    checkPermanentHeaderItems();
  }

  function checkPermanentHeaderItems() {
    expect(dom.find('.app-title')).toBeTruthy();
    expect(dom.find('.shortcuts')).toBeTruthy();
    expect(dom.find('.documentation')).toBeTruthy();
    expect(dom.find('.report-bug')).toBeTruthy();
    expect(dom.find('.dark-mode')).toBeTruthy();
  }
});
