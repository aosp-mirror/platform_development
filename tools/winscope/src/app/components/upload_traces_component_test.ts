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
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {MatListModule} from '@angular/material/list';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatTooltipModule} from '@angular/material/tooltip';
import {By} from '@angular/platform-browser';
import {FilesSource} from 'app/files_source';
import {TracePipeline} from 'app/trace_pipeline';
import {assertDefined} from 'common/assert_utils';
import {TimestampConverterUtils} from 'common/time/test_utils';
import {
  AppTraceViewRequest,
  AppTraceViewRequestHandled,
} from 'messaging/winscope_event';
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
  let fixture: ComponentFixture<UploadTracesComponent>;
  let component: UploadTracesComponent;
  let htmlElement: HTMLElement;
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
    fixture = TestBed.createComponent(UploadTracesComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    component.tracePipeline = new TracePipeline();
    validSfFile = await getFixtureFile(
      'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb',
    );
    validWmFile = await getFixtureFile(
      'traces/elapsed_and_real_timestamp/WindowManager.pb',
    );
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders the expected card title', () => {
    expect(htmlElement.querySelector('.title')?.innerHTML).toContain(
      'Upload Traces',
    );
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
    fixture.detectChanges();
    checkOnlyProgressBarShowing();

    component.onOperationFinished();
    fixture.detectChanges();
    expect(htmlElement.querySelector('load-progress')).toBeNull();
    assertDefined(htmlElement.querySelector('.drop-info'));
  });

  it('displays only load progress bar on progress update (existing files)', async () => {
    await loadFiles([validSfFile]);
    component.onProgressUpdate(undefined, undefined);
    fixture.detectChanges();
    checkOnlyProgressBarShowing();

    component.onOperationFinished();
    fixture.detectChanges();
    expect(htmlElement.querySelector('load-progress')).toBeNull();
    assertDefined(htmlElement.querySelector('.trace-actions-container'));
    assertDefined(htmlElement.querySelector('.uploaded-files'));
  });

  it('shows progress bar with custom message', () => {
    component.onProgressUpdate('Updating', undefined);
    fixture.detectChanges();
    checkOnlyProgressBarShowing('Updating');
  });

  it('updates progress bar percentage only if sufficient time has passed', () => {
    component.onProgressUpdate(undefined, 10);
    fixture.detectChanges();
    const progressBar = fixture.debugElement.query(
      By.directive(LoadProgressComponent),
    ).componentInstance as LoadProgressComponent;
    expect(progressBar.progressPercentage).toEqual(10);

    component.onProgressUpdate(undefined, 20);
    fixture.detectChanges();
    expect(progressBar.progressPercentage).toEqual(10);

    const now = Date.now();
    spyOn(Date, 'now').and.returnValue(now + 500);
    component.onProgressUpdate(undefined, 20);
    fixture.detectChanges();
    expect(progressBar.progressPercentage).toEqual(20);
  });

  it('can display uploaded traces', async () => {
    await loadFiles([validSfFile]);
    assertDefined(htmlElement.querySelector('.uploaded-files'));
    assertDefined(htmlElement.querySelector('.trace-actions-container'));
  });

  it('can remove one of two uploaded traces', async () => {
    await loadFiles([validSfFile, validWmFile]);
    expect(component.tracePipeline?.getTraces().getSize()).toBe(2);

    const spy = spyOn(component, 'onOperationFinished');
    removeTrace();
    assertDefined(htmlElement.querySelector('.uploaded-files'));
    expect(spy).toHaveBeenCalled();
    expect(component.tracePipeline?.getTraces().getSize()).toBe(1);
  });

  it('handles removal of the only uploaded trace', async () => {
    await loadFiles([validSfFile]);

    const spy = spyOn(component, 'onOperationFinished');
    removeTrace();
    assertDefined(htmlElement.querySelector('.drop-info'));
    expect(spy).toHaveBeenCalled();
    expect(component.tracePipeline?.getTraces().getSize()).toBe(0);
  });

  it('can remove all uploaded traces', async () => {
    await loadFiles([validSfFile, validWmFile]);
    expect(component.tracePipeline?.getTraces().getSize()).toBe(2);

    const spy = spyOn(component, 'onOperationFinished');
    const clearAllButton = getButton(clearAllSelector);
    clearAllButton.click();
    fixture.detectChanges();
    assertDefined(htmlElement.querySelector('.drop-info'));
    expect(spy).toHaveBeenCalled();
    expect(component.tracePipeline?.getTraces().getSize()).toBe(0);
  });

  it('can emit view traces event', async () => {
    await loadFiles([validSfFile]);

    const spy = spyOn(component.viewTracesButtonClick, 'emit');
    getButton(viewTracesSelector).click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  it('shows warning elements for traces without visualization', async () => {
    const shellTransitionFile = await getFixtureFile(
      'traces/elapsed_and_real_timestamp/shell_transition_trace.pb',
    );
    await loadFiles([shellTransitionFile]);

    expect(htmlElement.querySelector('.warning-icon')).toBeTruthy();
    expect(getButton(viewTracesSelector).disabled).toBeTrue();
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
    fixture.detectChanges();

    expect(htmlElement.querySelector('.error-icon')).toBeTruthy();
    expect(getButton(viewTracesSelector).disabled).toBeTrue();
  });

  it('emits download traces event', async () => {
    await loadFiles([validSfFile]);

    const spy = spyOn(component.downloadTracesClick, 'emit');
    const downloadTracesButton = assertDefined(
      htmlElement.querySelector<HTMLElement>('.download-btn'),
    );
    downloadTracesButton.click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  it('disables edit/view traces functionality on trace view request events', async () => {
    await loadFiles([validSfFile]);
    const buttons = [
      getButton(viewTracesSelector),
      getButton(removeTraceSelector),
      getButton(clearAllSelector),
      getButton(uploadSelector),
    ];
    const dropBox = assertDefined(
      htmlElement.querySelector<HTMLElement>('.drop-box'),
    );
    const spy = spyOn(component.filesUploaded, 'emit');

    await component.onWinscopeEvent(new AppTraceViewRequest());
    fixture.detectChanges();
    buttons.forEach((button) => {
      expect(button.disabled).toBeTrue();
    });
    dropFileAndGetTransferredFiles();
    addFileByClickAndGetTransferredFiles(true, dropBox);
    expect(spy).not.toHaveBeenCalled();

    await component.onWinscopeEvent(new AppTraceViewRequestHandled());
    fixture.detectChanges();
    buttons.forEach((button) => {
      expect(button.disabled).toBeFalse();
    });
    const files = dropFileAndGetTransferredFiles();
    expect(spy).toHaveBeenCalledOnceWith(files);
    spy.calls.reset();
    addFileByClickAndGetTransferredFiles(true, dropBox);
    expect(spy).toHaveBeenCalledOnceWith(files);
  });

  async function loadFiles(files: File[]) {
    const tracePipeline = assertDefined(component.tracePipeline);
    tracePipeline.clear();
    await tracePipeline.loadFiles(files, FilesSource.TEST, undefined);
    fixture.detectChanges();
  }

  function dropFileAndGetTransferredFiles(withFile = true): File[] {
    const dropbox = assertDefined(htmlElement.querySelector('.drop-box'));
    let dataTransfer: DataTransfer | undefined;
    if (withFile) {
      dataTransfer = new DataTransfer();
      dataTransfer.items.add(validSfFile);
    }
    dropbox.dispatchEvent(new DragEvent('drop', {dataTransfer}));
    fixture.detectChanges();
    return Array.from(dataTransfer?.files ?? []);
  }

  function addFileByClickAndGetTransferredFiles(
    withFile = true,
    clickEl: HTMLElement = getButton(uploadSelector),
  ): File[] {
    const dataTransfer = new DataTransfer();
    if (withFile) dataTransfer.items.add(validSfFile);
    const fileList = dataTransfer.files;

    const fileInput = assertDefined(
      htmlElement.querySelector<HTMLInputElement>('.drop-box input'),
    );
    clickEl.addEventListener('click', () => {
      fileInput.files = fileList;
    });

    clickEl.click();
    fixture.detectChanges();
    fileInput.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    return Array.from(fileList);
  }

  function removeTrace() {
    getButton(removeTraceSelector).click();
    fixture.detectChanges();
  }

  function getButton(selector: string): HTMLButtonElement {
    return assertDefined(
      htmlElement.querySelector<HTMLButtonElement>(selector),
    );
  }

  function checkOnlyProgressBarShowing(expectedMessage = 'Loading...') {
    const progressBar = assertDefined(
      htmlElement.querySelector('load-progress'),
    );
    expect(progressBar.textContent).toEqual(expectedMessage);
    expect(htmlElement.querySelector('.trace-actions-container')).toBeNull();
    expect(htmlElement.querySelector('.uploaded-files')).toBeNull();
    expect(htmlElement.querySelector('.drop-info')).toBeNull();
  }
});
