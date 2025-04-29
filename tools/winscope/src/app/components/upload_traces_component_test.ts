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
import {TestBed} from '@angular/core/testing';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {MatListModule} from '@angular/material/list';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatTooltipModule} from '@angular/material/tooltip';
import {FilesSource} from 'app/files_source';
import {TracePipeline} from 'app/trace_pipeline';
import {assertDefined} from 'common/assert_utils';
import {TimestampConverterUtils} from 'common/time/test_utils';
import {
  AppTraceViewRequest,
  AppTraceViewRequestHandled,
  ShowTraceUploadWarning,
} from 'messaging/winscope_event';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {getFixtureFile} from 'test/unit/fixture_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {Traces} from 'trace/traces';
import {LoadProgressComponent} from './load_progress_component';
import {UploadTracesComponent} from './upload_traces_component';

describe('UploadTracesComponent', () => {
  const uploadSelector = '.upload-btn';
  const clearAllSelector = '.clear-all-btn';
  const viewTracesSelector = '.load-btn';
  const removeTraceSelector = '.uploaded-files button';
  const warningBannerSelector = '.warning-banner';
  const warningMessageSelector = '.warn-message';
  const warningCloseButtonSelector = '.warning-banner button';

  let component: UploadTracesComponent;
  let dom: DOMTestHelper<UploadTracesComponent>;
  let validSfFile: File;
  let validWmFile: File;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        MatCardModule,
        MatSnackBarModule,
        MatListModule,
        MatIconModule,
        MatProgressBarModule,
        MatTooltipModule,
      ],
      providers: [MatSnackBar],
      declarations: [UploadTracesComponent, LoadProgressComponent],
    }).compileComponents();
    const fixture = TestBed.createComponent(UploadTracesComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    component.tracePipeline = new TracePipeline();
    validSfFile = await getFixtureFile(
      'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb',
    );
    validWmFile = await getFixtureFile(
      'traces/elapsed_and_real_timestamp/WindowManager.pb',
    );
    dom.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders the expected card title', () => {
    dom.get('.title').checkText('Upload Traces');
  });

  it('handles file upload via drag and drop', () => {
    const spy = spyOn(component.filesUploaded, 'emit');
    dropFileAndGetTransferredFiles(false);
    expect(spy).not.toHaveBeenCalled();
    const files = dropFileAndGetTransferredFiles();
    expect(spy).toHaveBeenCalledOnceWith(files);
  });

  it('handles file upload via upload button click', async () => {
    await loadFiles([validSfFile]);
    const spy = spyOn(component.filesUploaded, 'emit');
    addFileByClickAndGetTransferredFiles(false);
    expect(spy).not.toHaveBeenCalled();
    const files = addFileByClickAndGetTransferredFiles();
    expect(spy).toHaveBeenCalledOnceWith(files);
  });

  it('displays only load progress bar on progress update (no existing files)', () => {
    component.onProgressUpdate(undefined, undefined);
    dom.detectChanges();
    checkOnlyProgressBarShowing();

    component.onOperationFinished();
    dom.detectChanges();
    expect(dom.find('load-progress')).toBeUndefined();
    expect(dom.find('.drop-info')).toBeDefined();
  });

  it('displays only load progress bar on progress update (existing files)', async () => {
    await loadFiles([validSfFile]);
    component.onProgressUpdate(undefined, undefined);
    dom.detectChanges();
    checkOnlyProgressBarShowing();

    component.onOperationFinished();
    dom.detectChanges();
    expect(dom.find('load-progress')).toBeUndefined();
    expect(dom.find('.trace-actions-container')).toBeDefined();
    expect(dom.find('.uploaded-files')).toBeDefined();
  });

  it('shows progress bar with custom message', () => {
    component.onProgressUpdate('Updating', undefined);
    dom.detectChanges();
    checkOnlyProgressBarShowing('Updating');
  });

  it('updates progress bar percentage only if sufficient time has passed', () => {
    component.onProgressUpdate(undefined, 10);
    dom.detectChanges();
    const progressBar = assertDefined(
      dom.findByDirective(LoadProgressComponent),
    );
    expect(progressBar.progressPercentage).toEqual(10);

    component.onProgressUpdate(undefined, 20);
    dom.detectChanges();
    expect(progressBar.progressPercentage).toEqual(10);

    const now = Date.now();
    spyOn(Date, 'now').and.returnValue(now + 500);
    component.onProgressUpdate(undefined, 20);
    dom.detectChanges();
    expect(progressBar.progressPercentage).toEqual(20);
  });

  it('can display uploaded traces', async () => {
    await loadFiles([validSfFile]);
    expect(dom.find('.uploaded-files')).toBeDefined();
    expect(dom.find('.trace-actions-container')).toBeDefined();
  });

  it('can remove one of two uploaded traces', async () => {
    await loadFiles([validSfFile, validWmFile]);
    expect(component.tracePipeline?.getTraces().getSize()).toEqual(2);

    const spy = spyOn(component, 'onOperationFinished');
    dom.findAndClick(removeTraceSelector);
    expect(dom.find('.uploaded-files')).toBeDefined();
    expect(spy).toHaveBeenCalled();
    expect(component.tracePipeline?.getTraces().getSize()).toEqual(1);
  });

  it('handles removal of the only uploaded trace', async () => {
    await loadFiles([validSfFile]);

    const spy = spyOn(component, 'onOperationFinished');
    dom.findAndClick(removeTraceSelector);
    expect(dom.find('.drop-info')).toBeDefined();
    expect(spy).toHaveBeenCalled();
    expect(component.tracePipeline?.getTraces().getSize()).toEqual(0);
  });

  it('can remove all uploaded traces', async () => {
    await loadFiles([validSfFile, validWmFile]);
    expect(component.tracePipeline?.getTraces().getSize()).toEqual(2);

    const spy = spyOn(component, 'onOperationFinished');
    dom.findAndClick(clearAllSelector);
    expect(dom.find('.drop-info')).toBeDefined();
    expect(spy).toHaveBeenCalled();
    expect(component.tracePipeline?.getTraces().getSize()).toEqual(0);
  });

  it('can emit view traces event', async () => {
    await loadFiles([validSfFile]);

    const spy = spyOn(component.viewTracesButtonClick, 'emit');
    dom.findAndClick(viewTracesSelector);
    expect(spy).toHaveBeenCalled();
  });

  it('shows warning elements for traces without visualization', async () => {
    const shellTransitionFile = await getFixtureFile(
      'traces/elapsed_and_real_timestamp/shell_transition_trace.pb',
    );
    await loadFiles([shellTransitionFile]);
    expect(dom.find('.warning-icon')).toBeDefined();
    dom.get(viewTracesSelector).checkDisabled(true);
  });

  it('shows error elements for corrupted traces', async () => {
    const corruptedTrace = new TraceBuilder<string>()
      .setEntries(['entry-0'])
      .setTimestamps([TimestampConverterUtils.makeZeroTimestamp()])
      .build();
    corruptedTrace.setCorruptedState(true);
    const traces = new Traces();
    traces.addTrace(corruptedTrace);
    spyOn(assertDefined(component.tracePipeline), 'getTraces').and.returnValue(
      traces,
    );
    dom.detectChanges();
    expect(dom.find('.error-icon')).toBeDefined();
    dom.get(viewTracesSelector).checkDisabled(true);
  });

  it('emits download traces event', async () => {
    await loadFiles([validSfFile]);
    const spy = spyOn(component.downloadTracesClick, 'emit');
    dom.findAndClick('.download-btn');
    expect(spy).toHaveBeenCalled();
  });

  it('disables edit/view traces functionality on trace view request events', async () => {
    await loadFiles([validSfFile]);
    const buttons = [
      dom.get(viewTracesSelector),
      dom.get(removeTraceSelector),
      dom.get(clearAllSelector),
      dom.get(uploadSelector),
    ];
    const dropBox = dom.get('.drop-box');
    const spy = spyOn(component.filesUploaded, 'emit');

    await component.onWinscopeEvent(new AppTraceViewRequest());
    dom.detectChanges();
    buttons.forEach((button) => {
      button.checkDisabled(true);
    });
    dropFileAndGetTransferredFiles();
    addFileByClickAndGetTransferredFiles(true, dropBox);
    expect(spy).not.toHaveBeenCalled();

    await component.onWinscopeEvent(new AppTraceViewRequestHandled());
    dom.detectChanges();
    buttons.forEach((button) => {
      button.checkDisabled(false);
    });
    const files = dropFileAndGetTransferredFiles();
    expect(spy).toHaveBeenCalledOnceWith(files);
    spy.calls.reset();
    addFileByClickAndGetTransferredFiles(true, dropBox);
    expect(spy).toHaveBeenCalledOnceWith(files);
  });

  it('displays warning banners when ShowTraceUploadWarning events received', async () => {
    const warningMessage1 = 'This is the first warning!';
    const warningMessage2 = 'This is the second warning!';
    const warningEvent1 = new ShowTraceUploadWarning(warningMessage1);
    const warningEvent2 = new ShowTraceUploadWarning(warningMessage2);

    // Initially, no banners should be visible
    expect(component.warningMessages.length).toEqual(0);
    expect(dom.findAll(warningBannerSelector).length).toEqual(0);

    // Simulate receiving the first event
    await component.onWinscopeEvent(warningEvent1);
    dom.detectChanges();

    // Assert first banner visibility and message content
    expect(component.warningMessages).toEqual([warningMessage1]);
    let bannerElements = dom.findAll(warningBannerSelector);
    expect(bannerElements.length).toEqual(1);
    bannerElements[0]
      .get(warningMessageSelector)
      .checkTextExact(warningMessage1);

    // Simulate receiving the second event
    await component.onWinscopeEvent(warningEvent2);
    dom.detectChanges();

    // Assert both banners are visible with correct messages
    expect(component.warningMessages).toEqual([
      warningMessage1,
      warningMessage2,
    ]);
    bannerElements = dom.findAll(warningBannerSelector);
    expect(bannerElements.length).toEqual(2);
    bannerElements[0]
      .get(warningMessageSelector)
      .checkTextExact(warningMessage1);
    bannerElements[1]
      .get(warningMessageSelector)
      .checkTextExact(warningMessage2);

    // Simulate receiving the first event again (should not add duplicate)
    await component.onWinscopeEvent(warningEvent1);
    dom.detectChanges();
    expect(component.warningMessages).toEqual([
      warningMessage1,
      warningMessage2,
    ]);
    expect(dom.findAll(warningBannerSelector).length).toEqual(2);
  });

  it('clears specific warning banner when its close button is clicked', async () => {
    const warningMessage1 = 'Warning 1 to dismiss';
    const warningMessage2 = 'Warning 2 to keep';
    const warningEvent1 = new ShowTraceUploadWarning(warningMessage1);
    const warningEvent2 = new ShowTraceUploadWarning(warningMessage2);

    // Show the banners first
    await component.onWinscopeEvent(warningEvent1);
    await component.onWinscopeEvent(warningEvent2);
    dom.detectChanges();
    let warningBanners = dom.findAll(warningBannerSelector);
    expect(warningBanners.length).toEqual(2);
    warningBanners[0]
      .get(warningMessageSelector)
      .checkTextExact(warningMessage1);
    warningBanners[1]
      .get(warningMessageSelector)
      .checkTextExact(warningMessage2);

    const firstBannerCloseButton = warningBanners[0].find(
      warningCloseButtonSelector,
    );
    firstBannerCloseButton!!.click();
    dom.detectChanges();

    // Assert only the first banner is removed
    warningBanners = dom.findAll(warningBannerSelector);
    expect(warningBanners.length).toEqual(1);
    warningBanners[0]
      .get(warningMessageSelector)
      .checkTextExact(warningMessage2);
  });

  it('clears all warning banners when clear all button is clicked', async () => {
    const warningMessage1 = 'Warning before clear all 1!';
    const warningMessage2 = 'Warning before clear all 2!';
    const warningEvent1 = new ShowTraceUploadWarning(warningMessage1);
    const warningEvent2 = new ShowTraceUploadWarning(warningMessage2);
    await loadFiles([validSfFile]); // Need a file to enable clear all

    // Show the banners first
    await component.onWinscopeEvent(warningEvent1);
    await component.onWinscopeEvent(warningEvent2);
    dom.detectChanges();
    expect(component.warningMessages.length).toEqual(2);
    expect(dom.findAll(warningBannerSelector).length).toEqual(2);

    // Click clear all
    dom.findAndClick(clearAllSelector);

    // Assert banners are hidden
    expect(component.warningMessages.length).toEqual(0);
    expect(dom.findAll(warningBannerSelector).length).toEqual(0);
  });

  it('warning banners are not cleared when new files are uploaded', async () => {
    const warningMessage1 = 'Warning before new load 1!';
    const warningMessage2 = 'Warning before new load 2!';
    const warningEvent1 = new ShowTraceUploadWarning(warningMessage1);
    const warningEvent2 = new ShowTraceUploadWarning(warningMessage2);

    // Show the banners first
    await component.onWinscopeEvent(warningEvent1);
    await component.onWinscopeEvent(warningEvent2);
    dom.detectChanges();
    expect(component.warningMessages.length).toEqual(2);
    expect(dom.findAll(warningBannerSelector).length).toEqual(2);

    // Start a new progress update
    component.onProgressUpdate('Loading new files...', 0);
    dom.detectChanges();

    // Assert banners are hidden
    expect(component.warningMessages.length).toEqual(2);
    expect(dom.findAll(warningBannerSelector).length).toEqual(2);
  });

  async function loadFiles(files: File[]) {
    const tracePipeline = assertDefined(component.tracePipeline);
    tracePipeline.clear();
    await tracePipeline.loadFiles(files, FilesSource.TEST, undefined);
    dom.detectChanges();
  }

  function dropFileAndGetTransferredFiles(withFile = true): File[] {
    let dataTransfer: DataTransfer | undefined;
    if (withFile) {
      dataTransfer = new DataTransfer();
      dataTransfer.items.add(validSfFile);
    }
    const dropBox = dom.get('.drop-box');
    dropBox.dispatchEvent(new DragEvent('drop', {dataTransfer}));
    return Array.from(dataTransfer?.files ?? []);
  }

  function addFileByClickAndGetTransferredFiles(
    withFile = true,
    clickEl = dom.get(uploadSelector),
  ): File[] {
    const dataTransfer = new DataTransfer();
    if (withFile) dataTransfer.items.add(validSfFile);
    const fileList = dataTransfer.files;

    const fileInput = dom.get('.drop-box input');
    const fileInputEl = fileInput.getHTMLElement<HTMLInputElement>();
    clickEl.addEventListener('click', () => {
      fileInputEl.files = fileList;
    });
    clickEl.click();
    fileInput.dispatchEvent(new Event('change'));
    return Array.from(fileList);
  }

  function checkOnlyProgressBarShowing(expectedMessage = 'Loading...') {
    dom.get('load-progress').checkTextExact(expectedMessage);
    expect(dom.find('.trace-actions-container')).toBeUndefined();
    expect(dom.find('.uploaded-files')).toBeUndefined();
    expect(dom.find('.drop-info')).toBeUndefined();
  }
});
