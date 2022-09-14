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
import { TraceCoordinator } from "app/trace_coordinator";

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
    component.traceCoordinator = new TraceCoordinator();
    component.viewerTabs = [
      {
        label: "Surface Flinger",
        cardId: 0,
      },
      {
        label: "Window Manager",
        cardId: 1,
      }
    ];
    htmlElement = fixture.nativeElement;
  });

  it("can be created", () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it("creates viewer tabs", () => {
    fixture.detectChanges();
    const tabs = htmlElement.querySelectorAll(".viewer-tab");
    expect(tabs.length).toEqual(2);
    expect(component.activeViewerCardId).toEqual(0);
  });

  it("changes active viewer on click", async () => {
    fixture.detectChanges();
    expect(component.activeViewerCardId).toEqual(0);
    const tabs = htmlElement.querySelectorAll(".viewer-tab");
    tabs[0].dispatchEvent(new Event("click"));
    fixture.detectChanges();
    await fixture.whenStable();
    const firstId = component.activeViewerCardId;
    tabs[1].dispatchEvent(new Event("click"));
    fixture.detectChanges();
    await fixture.whenStable();
    const secondId = component.activeViewerCardId;
    expect(firstId !== secondId).toBeTrue;
  });

  it("downloads all traces", async () => {
    spyOn(component, "downloadAllTraces").and.callThrough();
    fixture.detectChanges();
    const downloadButton: HTMLButtonElement | null = htmlElement.querySelector(".save-btn");
    expect(downloadButton).toBeInstanceOf(HTMLButtonElement);
    downloadButton?.dispatchEvent(new Event("click"));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(component.downloadAllTraces).toHaveBeenCalled();
  });
});
