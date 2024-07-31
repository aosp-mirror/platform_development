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
import {FilesSource} from 'app/files_source';
import {TracePipeline} from 'app/trace_pipeline';
import {assertDefined} from 'common/assert_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {LoadProgressComponent} from './load_progress_component';
import {UploadTracesComponent} from './upload_traces_component';

describe('UploadTracesComponent', () => {
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
    validSfFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb',
    );
    validWmFile = await UnitTestUtils.getFixtureFile(
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
    const dropbox = assertDefined(htmlElement.querySelector('.drop-box'));

    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(validSfFile);
    dropbox?.dispatchEvent(new DragEvent('drop', {dataTransfer}));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledWith(Array.from(dataTransfer.files));
  });

  it('displays load progress bar', () => {
    component.isLoadingFiles = true;
    fixture.detectChanges();
    assertDefined(htmlElement.querySelector('load-progress'));
  });

  it('can display uploaded traces', async () => {
    await loadFiles([validSfFile]);
    fixture.detectChanges();
    assertDefined(htmlElement.querySelector('.uploaded-files'));
    assertDefined(htmlElement.querySelector('.trace-actions-container'));
  });

  it('can remove one of two uploaded traces', async () => {
    await loadFiles([validSfFile, validWmFile]);
    fixture.detectChanges();
    expect(component.tracePipeline?.getTraces().getSize()).toBe(2);

    const spy = spyOn(component, 'onOperationFinished');
    const removeButton = assertDefined(
      htmlElement.querySelector('.uploaded-files button'),
    );
    (removeButton as HTMLButtonElement).click();
    fixture.detectChanges();
    assertDefined(htmlElement.querySelector('.uploaded-files'));
    expect(spy).toHaveBeenCalled();
    expect(component.tracePipeline?.getTraces().getSize()).toBe(1);
  });

  it('handles removal of the only uploaded trace', async () => {
    await loadFiles([validSfFile]);
    fixture.detectChanges();

    const spy = spyOn(component, 'onOperationFinished');
    const removeButton = assertDefined(
      htmlElement.querySelector('.uploaded-files button'),
    );
    (removeButton as HTMLButtonElement).click();
    fixture.detectChanges();
    assertDefined(htmlElement.querySelector('.drop-info'));
    expect(spy).toHaveBeenCalled();
    expect(component.tracePipeline?.getTraces().getSize()).toBe(0);
  });

  it('can remove all uploaded traces', async () => {
    await loadFiles([validSfFile, validWmFile]);
    fixture.detectChanges();
    expect(component.tracePipeline?.getTraces().getSize()).toBe(2);

    const spy = spyOn(component, 'onOperationFinished');
    const clearAllButton = assertDefined(
      htmlElement.querySelector('.clear-all-btn'),
    );
    (clearAllButton as HTMLButtonElement).click();
    fixture.detectChanges();
    assertDefined(htmlElement.querySelector('.drop-info'));
    expect(spy).toHaveBeenCalled();
    expect(component.tracePipeline?.getTraces().getSize()).toBe(0);
  });

  it('can emit view traces event', async () => {
    await loadFiles([validSfFile]);
    fixture.detectChanges();

    const spy = spyOn(component.viewTracesButtonClick, 'emit');
    const viewTracesButton = assertDefined(
      htmlElement.querySelector('.load-btn'),
    );
    (viewTracesButton as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  it('shows warning elements for traces without visualization', async () => {
    const shellTransitionFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/shell_transition_trace.pb',
    );
    await loadFiles([shellTransitionFile]);
    fixture.detectChanges();

    expect(htmlElement.querySelector('.warning-icon')).toBeTruthy();
    const viewTracesButton = assertDefined(
      htmlElement.querySelector('.load-btn'),
    );
    expect((viewTracesButton as HTMLButtonElement).disabled).toBeTrue();
  });

  it('emits download traces event', async () => {
    await loadFiles([validSfFile]);
    fixture.detectChanges();

    const spy = spyOn(component.downloadTracesClick, 'emit');
    const downloadTracesButton = assertDefined(
      htmlElement.querySelector('.download-btn'),
    );
    (downloadTracesButton as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  async function loadFiles(files: File[]) {
    const tracePipeline = assertDefined(component.tracePipeline);
    tracePipeline.clear();
    await tracePipeline.loadFiles(files, FilesSource.TEST, undefined);
  }
});
