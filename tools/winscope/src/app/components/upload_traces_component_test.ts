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
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {TracePipeline} from 'app/trace_pipeline';
import {UploadTracesComponent} from './upload_traces_component';

describe('UploadTracesComponent', () => {
  let fixture: ComponentFixture<UploadTracesComponent>;
  let component: UploadTracesComponent;
  let htmlElement: HTMLElement;

  beforeAll(async () => {
    await TestBed.configureTestingModule({
      imports: [MatCardModule, MatSnackBarModule],
      providers: [MatSnackBar],
      declarations: [UploadTracesComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(UploadTracesComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    const tracePipeline = new TracePipeline();
    component.tracePipeline = tracePipeline;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });
});
