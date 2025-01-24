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
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {
  FormControl,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
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
  ViewersLoaded,
  ViewersUnloaded,
} from 'messaging/winscope_event';
import {TracesBuilder} from 'test/unit/traces_builder';
import {waitToBeCalled} from 'test/utils';
import {ViewerSurfaceFlingerComponent} from 'viewers/viewer_surface_flinger/viewer_surface_flinger_component';
import {AdbProxyComponent} from './adb_proxy_component';
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
import {WebAdbComponent} from './web_adb_component';

describe('AppComponent', () => {
  let fixture: ComponentFixture<AppComponent>;
  let component: AppComponent;
  let htmlElement: HTMLElement;
  let downloadTracesSpy: jasmine.Spy;

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
      ],
      declarations: [
        AdbProxyComponent,
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
        WebAdbComponent,
        ShortcutsComponent,
        SnackBarComponent,
      ],
    })
      .overrideComponent(AppComponent, {
        set: {changeDetection: ChangeDetectionStrategy.Default},
      })
      .compileComponents();
    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    component.filenameFormControl = new FormControl(
      'winscope',
      Validators.compose([
        Validators.required,
        Validators.pattern(FileUtils.DOWNLOAD_FILENAME_REGEX),
      ]),
    );
    downloadTracesSpy = spyOn(Download, 'fromUrl');
    fixture.detectChanges();
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
    fixture.detectChanges();
    checkHomepage();
  });

  it('displays correct elements when data loaded', () => {
    goToTraceView();
    checkTraceViewPage();

    spyOn(component, 'dumpsUploaded').and.returnValue(true);
    fixture.detectChanges();
    expect(htmlElement.querySelector('.refresh-dumps')).toBeTruthy();
  });

  it('returns to homepage on upload new button click', async () => {
    goToTraceView();
    checkTraceViewPage();

    assertDefined(
      htmlElement.querySelector<HTMLButtonElement>('.upload-new'),
    ).click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    await fixture.whenStable();
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
    assertDefined(
      htmlElement.querySelector<HTMLButtonElement>('.refresh-dumps'),
    ).click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    await fixture.whenStable();
    checkHomepage();
    expect(winscopeEventSpy).toHaveBeenCalledWith(new AppRefreshDumpsRequest());
  });

  it('shows download progress bar', () => {
    component.showDataLoadedElements = true;
    fixture.detectChanges();
    expect(
      htmlElement.querySelector('.download-files-section mat-progress-bar'),
    ).toBeNull();

    component.onProgressUpdate('Progress update', 10);
    fixture.detectChanges();
    expect(
      htmlElement.querySelector('.download-files-section mat-progress-bar'),
    ).toBeTruthy();

    component.onOperationFinished(true);
    fixture.detectChanges();
    expect(
      htmlElement.querySelector('.download-files-section mat-progress-bar'),
    ).toBeNull();
  });

  it('downloads traces on download button click and shows download progress bar', async () => {
    component.showDataLoadedElements = true;
    fixture.detectChanges();
    clickDownloadTracesButton();
    expect(
      htmlElement.querySelector('.download-files-section mat-progress-bar'),
    ).toBeTruthy();
    await waitToBeCalled(downloadTracesSpy);
  });

  it('downloads traces after valid file name change', async () => {
    component.showDataLoadedElements = true;
    fixture.detectChanges();

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
    fixture.detectChanges();
    expect(pageTitle.getTitle()).toBe('Winscope | test_archive');
  });

  it('does not download traces if invalid file name chosen', () => {
    component.showDataLoadedElements = true;
    fixture.detectChanges();

    clickEditFilenameButton();
    updateFilenameInputAndDownloadTraces('w?n$cope', false);
    expect(downloadTracesSpy).not.toHaveBeenCalled();
  });

  it('behaves as expected when entering valid then invalid then valid file names', async () => {
    component.showDataLoadedElements = true;
    fixture.detectChanges();

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
    fixture.detectChanges();
    clickEditFilenameButton();
    const inputField = assertDefined(
      htmlElement.querySelector('.file-name-input-field'),
    );
    const inputEl = assertDefined(
      htmlElement.querySelector<HTMLInputElement>(
        '.file-name-input-field input',
      ),
    );
    inputEl.value = 'valid_file_name';

    inputField.dispatchEvent(new KeyboardEvent('keydown', {key: 'Enter'}));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledTimes(1);

    inputField.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape'}));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledTimes(2);

    inputField.dispatchEvent(new FocusEvent('focusout'));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledTimes(3);
  });

  it('downloads traces from upload traces section', () => {
    const traces = assertDefined(component.tracePipeline.getTraces());
    spyOn(traces, 'getSize').and.returnValue(1);
    fixture.detectChanges();
    const downloadButtonClickSpy = spyOn(
      component,
      'onDownloadTracesButtonClick',
    );

    const downloadButton = assertDefined(
      htmlElement.querySelector<HTMLElement>('upload-traces .download-btn'),
    );
    downloadButton.click();
    fixture.detectChanges();
    expect(downloadButtonClickSpy).toHaveBeenCalledOnceWith(
      component.uploadTracesComponent,
    );
  });

  it('opens shortcuts dialog', () => {
    expect(document.querySelector('shortcuts-panel')).toBeFalsy();
    const shortcutsButton = assertDefined(
      htmlElement.querySelector<HTMLElement>('.shortcuts'),
    );
    shortcutsButton.click();
    fixture.detectChanges();
    expect(document.querySelector('shortcuts-panel')).toBeTruthy();
  });

  it('sets snackbar opener to global user notifier', () => {
    expect(document.querySelector('snack-bar')).toBeFalsy();
    UserNotifier.add(new NoValidFiles());
    UserNotifier.notify();
    expect(document.querySelector('snack-bar')).toBeTruthy();
  });

  it('does not open new snackbar until existing snackbar has been dismissed', async () => {
    expect(document.querySelector('snack-bar')).toBeFalsy();
    const firstMessage = new NoValidFiles();
    UserNotifier.add(firstMessage);
    UserNotifier.notify();
    fixture.detectChanges();
    await fixture.whenRenderingDone();
    let snackbar = assertDefined(document.querySelector('snack-bar'));
    expect(snackbar.textContent).toContain(firstMessage.getMessage());

    const secondMessage = new FailedToInitializeTimelineData();
    UserNotifier.add(secondMessage);
    UserNotifier.notify();
    fixture.detectChanges();
    await fixture.whenRenderingDone();
    snackbar = assertDefined(document.querySelector('snack-bar'));
    expect(snackbar.textContent).toContain(firstMessage.getMessage());

    const closeButton = assertDefined(
      snackbar.querySelector<HTMLElement>('.snack-bar-action'),
    );
    closeButton.click();
    fixture.detectChanges();
    await fixture.whenRenderingDone();
    snackbar = assertDefined(document.querySelector('snack-bar'));
    expect(snackbar.textContent).toContain(secondMessage.getMessage());
  });

  function goToTraceView() {
    component.dataLoaded = true;
    component.showDataLoadedElements = true;
    component.timelineData.initialize(
      new TracesBuilder().build(),
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    fixture.detectChanges();
  }

  function updateFilenameInputAndDownloadTraces(name: string, valid: boolean) {
    const inputEl = assertDefined(
      htmlElement.querySelector<HTMLInputElement>(
        '.file-name-input-field input',
      ),
    );
    const checkButton = assertDefined(
      htmlElement.querySelector('.check-button'),
    );
    inputEl.value = name;
    inputEl.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    checkButton.dispatchEvent(new Event('click'));
    fixture.detectChanges();

    const saveButton = assertDefined(
      htmlElement.querySelector<HTMLButtonElement>('.save-button'),
    );
    if (valid) {
      assertDefined(htmlElement.querySelector('.download-file-info'));
      expect(saveButton.disabled).toBeFalse();
      clickDownloadTracesButton();
    } else {
      expect(htmlElement.querySelector('.download-file-info')).toBeFalsy();
      expect(saveButton.disabled).toBeTrue();
    }
  }

  function clickDownloadTracesButton() {
    const downloadButton = assertDefined(
      htmlElement.querySelector('.save-button'),
    );
    downloadButton.dispatchEvent(new Event('click'));
    fixture.detectChanges();
  }

  function clickEditFilenameButton() {
    const pencilButton = assertDefined(
      htmlElement.querySelector('.edit-button'),
    );
    pencilButton.dispatchEvent(new Event('click'));
    fixture.detectChanges();
  }

  function checkHomepage() {
    expect(htmlElement.querySelector('.welcome-info')).toBeTruthy();
    expect(htmlElement.querySelector('.collect-traces-card')).toBeTruthy();
    expect(htmlElement.querySelector('.upload-traces-card')).toBeTruthy();
    expect(htmlElement.querySelector('.viewers')).toBeFalsy();
    expect(htmlElement.querySelector('.upload-new')).toBeFalsy();
    expect(htmlElement.querySelector('timeline')).toBeFalsy();
    checkPermanentHeaderItems();
  }

  function checkTraceViewPage() {
    expect(htmlElement.querySelector('.welcome-info')).toBeFalsy();
    expect(htmlElement.querySelector('.save-button')).toBeTruthy();
    expect(htmlElement.querySelector('.collect-traces-card')).toBeFalsy();
    expect(htmlElement.querySelector('.upload-traces-card')).toBeFalsy();
    expect(htmlElement.querySelector('.viewers')).toBeTruthy();
    expect(htmlElement.querySelector('.upload-new')).toBeTruthy();
    expect(htmlElement.querySelector('timeline')).toBeTruthy();
    checkPermanentHeaderItems();
  }

  function checkPermanentHeaderItems() {
    expect(htmlElement.querySelector('.app-title')).toBeTruthy();
    expect(htmlElement.querySelector('.shortcuts')).toBeTruthy();
    expect(htmlElement.querySelector('.documentation')).toBeTruthy();
    expect(htmlElement.querySelector('.report-bug')).toBeTruthy();
    expect(htmlElement.querySelector('.dark-mode')).toBeTruthy();
  }
});
