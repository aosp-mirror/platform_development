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
import { ChangeDetectionStrategy } from "@angular/core";
import {ComponentFixture, TestBed, ComponentFixtureAutoDetect} from "@angular/core/testing";
import { CommonModule } from "@angular/common";
import { MatCardModule } from "@angular/material/card";
import { MatButtonModule } from "@angular/material/button";
import { MatGridListModule } from "@angular/material/grid-list";
import { MatSliderModule } from "@angular/material/slider";
import { MatToolbarModule } from "@angular/material/toolbar";

import { AppComponent } from "./app.component";
import { CollectTracesComponent } from "./collect_traces.component";
import { UploadTracesComponent } from "./upload_traces.component";
import { AdbProxyComponent } from "./adb_proxy.component";
import { WebAdbComponent } from "./web_adb.component";
import { TraceConfigComponent } from "./trace_config.component";
import { ViewerSurfaceFlingerComponent } from "viewers/viewer_surface_flinger/viewer_surface_flinger.component";
import { TraceViewComponent } from "./trace_view.component";

describe("AppComponent", () => {
  let fixture: ComponentFixture<AppComponent>;
  let component: AppComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        { provide: ComponentFixtureAutoDetect, useValue: true }
      ],
      imports: [
        CommonModule,
        MatCardModule,
        MatButtonModule,
        MatGridListModule,
        MatSliderModule,
        MatToolbarModule
      ],
      declarations: [
        AppComponent,
        CollectTracesComponent,
        UploadTracesComponent,
        AdbProxyComponent,
        WebAdbComponent,
        TraceConfigComponent,
        ViewerSurfaceFlingerComponent,
        TraceViewComponent
      ],
    }).overrideComponent(AppComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
  });

  it("can be created", () => {
    expect(component).toBeTruthy();
  });

  it("has the expected title", () => {
    expect(component.title).toEqual("winscope-ng");
  });

  it("renders the page title", () => {
    expect(htmlElement.querySelector("#app-title")?.innerHTML).toContain("Winscope");
  });

  it("displays correct elements when no data loaded", async () => {
    component.dataLoaded = false;
    fixture.detectChanges();
    expect(htmlElement.querySelector(".welcome-info")).toBeTruthy();
    expect(htmlElement.querySelector("#viewers")).toBeNull();
  });

  it("displays correct elements when data loaded", async () => {
    component.dataLoaded = true;
    fixture.detectChanges();
    expect(htmlElement.querySelector("#collect-traces-card")).toBeFalsy();
    expect(htmlElement.querySelector("#upload-traces-card")).toBeFalsy();
    expect(htmlElement.querySelector("#viewers")).toBeTruthy();
  });
});
