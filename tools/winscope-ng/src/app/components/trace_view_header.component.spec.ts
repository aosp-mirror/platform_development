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
import { TraceViewHeaderComponent } from "./trace_view_header.component";
import { MatIconModule } from "@angular/material/icon";
import { MatButtonModule } from "@angular/material/button";
import { TraceType } from "common/trace/trace_type";

describe("TraceViewHeaderComponent", () => {
  let fixture: ComponentFixture<TraceViewHeaderComponent>;
  let component: TraceViewHeaderComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        MatIconModule,
        MatButtonModule
      ],
      declarations: [TraceViewHeaderComponent]
    }).compileComponents();
    fixture = TestBed.createComponent(TraceViewHeaderComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    component.dependencies = [TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER];
  });

  it("can be created", () => {
    expect(component).toBeTruthy();
  });

  it("check that toggle button is displayed, expanded on default", () => {
    component.showTrace = true;
    fixture.detectChanges();
    const toggleButton = htmlElement.querySelector("#toggle-btn");
    expect(toggleButton).toBeTruthy();
    const chevronIcon = toggleButton?.querySelector("mat-icon");
    expect(chevronIcon).toBeTruthy;
    expect(chevronIcon?.innerHTML).toContain("expand_more");
  });

  it("check that toggle button icon is a right chevron when minimised ", () => {
    component.showTrace = false;
    fixture.detectChanges();
    const toggleButton = htmlElement.querySelector("#toggle-btn");
    const chevronIcon = toggleButton?.querySelector("mat-icon");
    expect(chevronIcon?.innerHTML).toContain("chevron_right");
  });

  it("check that clicking toggle button causes view to minimise", async () => {
    component.showTrace = true;
    fixture.detectChanges();
    spyOn(component, "toggleView").and.callThrough();
    const button: HTMLButtonElement | null = htmlElement.querySelector("#toggle-btn");
    expect(button).toBeInstanceOf(HTMLButtonElement);
    button?.dispatchEvent(new Event("click"));
    await fixture.whenStable();
    expect(component.toggleView).toHaveBeenCalled();
    fixture.detectChanges();
    expect (htmlElement.querySelector("#toggle-btn")?.querySelector("mat-icon")?.innerHTML).toContain("chevron_right");
  });

  it("check that dependency icons show", () => {
    fixture.detectChanges();
    const dependencyIcons = htmlElement.querySelectorAll("#dep-icon");
    expect(dependencyIcons).toBeTruthy();
    expect(dependencyIcons.length).toBe(2);
  });

  it("check that title is displayed", () => {
    component.title = "Surface Flinger, Window Manager";
    fixture.detectChanges();
    const title = htmlElement.querySelector(".trace-card-title-text");
    expect(title).toBeTruthy();
    expect(title?.innerHTML).toContain("Surface Flinger");
    expect(title?.innerHTML).toContain("Window Manager");
  });

  it("check that save button is displayed", () => {
    fixture.detectChanges();
    const saveButton = htmlElement.querySelectorAll("#save-btn");
    expect(saveButton).toBeTruthy();
  });

  it("check that clicking save button emits", async () => {
    spyOn(component, "saveTraces").and.callThrough();
    spyOn(component.saveTraceChange, "emit");
    const button: HTMLButtonElement | null = htmlElement.querySelector("#save-btn");
    expect(button).toBeInstanceOf(HTMLButtonElement);
    button?.dispatchEvent(new Event("click"));
    await fixture.whenStable();
    expect(component.saveTraces).toHaveBeenCalled();
    expect(component.saveTraceChange.emit).toHaveBeenCalled();
  });

  it("check that screenshot button is displayed", () => {
    fixture.detectChanges();
    const screenshotButton = htmlElement.querySelectorAll("#screenshot-btn");
    expect(screenshotButton).toBeTruthy();
  });
});
