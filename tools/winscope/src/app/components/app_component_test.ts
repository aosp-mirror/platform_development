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
import {CommonModule} from '@angular/common';
import {ChangeDetectionStrategy} from '@angular/core';
import {ComponentFixture, ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatSelectModule} from '@angular/material/select';
import {MatSliderModule} from '@angular/material/slider';
import {MatSnackBarModule} from '@angular/material/snack-bar';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatTooltipModule} from '@angular/material/tooltip';

import {ViewerSurfaceFlingerComponent} from 'viewers/viewer_surface_flinger/viewer_surface_flinger_component';
import {AdbProxyComponent} from './adb_proxy_component';
import {AppComponent} from './app_component';
import {MatDrawer, MatDrawerContainer, MatDrawerContent} from './bottomnav/bottom_drawer_component';
import {CollectTracesComponent} from './collect_traces_component';
import {MiniTimelineComponent} from './timeline/mini_timeline_component';
import {TimelineComponent} from './timeline/timeline_component';
import {TraceConfigComponent} from './trace_config_component';
import {TraceViewComponent} from './trace_view_component';
import {UploadTracesComponent} from './upload_traces_component';
import {WebAdbComponent} from './web_adb_component';

describe('AppComponent', () => {
  let fixture: ComponentFixture<AppComponent>;
  let component: AppComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
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
      ],
    })
      .overrideComponent(AppComponent, {
        set: {changeDetection: ChangeDetectionStrategy.Default},
      })
      .compileComponents();
    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('has the expected title', () => {
    expect(component.title).toEqual('winscope');
  });

  it('renders the page title', () => {
    expect(htmlElement.querySelector('.app-title')?.innerHTML).toContain('Winscope');
  });

  it('displays correct elements when no data loaded', () => {
    component.dataLoaded = false;
    fixture.detectChanges();
    expect(htmlElement.querySelector('.welcome-info')).toBeTruthy();
    expect(htmlElement.querySelector('.active-trace-file-info')).toBeFalsy();
    expect(htmlElement.querySelector('.collect-traces-card')).toBeTruthy();
    expect(htmlElement.querySelector('.upload-traces-card')).toBeTruthy();
    expect(htmlElement.querySelector('.viewers')).toBeFalsy();
  });

  it('displays correct elements when data loaded', () => {
    component.dataLoaded = true;
    fixture.detectChanges();
    expect(htmlElement.querySelector('.welcome-info')).toBeFalsy();
    expect(htmlElement.querySelector('.active-trace-file-info')).toBeTruthy();
    expect(htmlElement.querySelector('.collect-traces-card')).toBeFalsy();
    expect(htmlElement.querySelector('.upload-traces-card')).toBeFalsy();
    expect(htmlElement.querySelector('.viewers')).toBeTruthy();
  });
});
