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
import { CommonModule } from "@angular/common";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { TraceViewComponent } from "./trace_view.component";
import { MatCardModule } from "@angular/material/card";
import { CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA } from "@angular/core";
import { TraceType } from "common/trace/trace_type";

describe("TraceViewComponent", () => {
  let fixture: ComponentFixture<TraceViewComponent>;
  let component: TraceViewComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        MatCardModule
      ],
      declarations: [TraceViewComponent],
      schemas: [NO_ERRORS_SCHEMA, CUSTOM_ELEMENTS_SCHEMA]
    }).compileComponents();
    fixture = TestBed.createComponent(TraceViewComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    component.dependencies = [TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER];
    component.showTrace = true;
  });

  it("can be created", () => {
    expect(component).toBeTruthy();
  });

  it("check that mat card title and contents are displayed", () => {
    fixture.detectChanges();
    const title = htmlElement.querySelector(".trace-card-title");
    expect(title).toBeTruthy();
    const header = title?.querySelector("trace-view-header");
    expect(header).toBeTruthy();
  });

  it("check that card content is created", () => {
    fixture.detectChanges();
    const content = htmlElement.querySelector(".trace-card-content") as HTMLElement;
    expect(content).toBeTruthy();
  });
});
