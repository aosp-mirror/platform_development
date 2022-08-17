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
import {ComponentFixture, TestBed} from "@angular/core/testing";
import {TraceConfigComponent} from "./trace_config.component";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatFormFieldModule } from "@angular/material/form-field";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { NO_ERRORS_SCHEMA } from "@angular/core";

describe("TraceConfigComponent", () => {
  let fixture: ComponentFixture<TraceConfigComponent>;
  let component: TraceConfigComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        MatCheckboxModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        BrowserAnimationsModule
      ],
      declarations: [TraceConfigComponent],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    fixture = TestBed.createComponent(TraceConfigComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    component.trace = {
      name: "layers_trace",
      run: false,
      config: {
        enableConfigs: [{
          name:"trace buffers",
          key:"tracebuffers",
          enabled:true
        }],
        selectionConfigs: [{
          key: "tracinglevel",
          name: "tracing level",
          options: [
            "verbose",
            "debug",
            "critical",
          ],
          value: "debug"
        }]
      }
    };
  });

  it("can be created", () => {
    expect(component).toBeTruthy();
  });

  it("check that trace checkbox ticked on default run", () => {
    component.trace.run = true;
    fixture.detectChanges();
    const box = htmlElement.querySelector(".trace-box");
    expect(box?.innerHTML).toContain("aria-checked=\"true\"");
    expect(box?.innerHTML).toContain("layers_trace");
  });

  it("check that trace checkbox not ticked on default run", () => {
    component.trace.run = false;
    fixture.detectChanges();
    const box = htmlElement.querySelector(".trace-box");
    expect(box?.innerHTML).toContain("aria-checked=\"false\"");
  });

  it("check that correct advanced enable config only shows", () => {
    component.trace.config!.selectionConfigs = [];
    fixture.detectChanges();
    const adv = htmlElement.querySelector(".adv-config");
    expect(adv).toBeTruthy();
    expect(adv?.innerHTML).toContain("trace buffers");
    expect(adv?.innerHTML).not.toContain("tracing level");
  });

  it("check that correct advanced selection config shows", () => {
    component.trace.config!.enableConfigs = [];
    fixture.detectChanges();
    const adv = htmlElement.querySelector(".adv-config");
    expect(adv).toBeTruthy();
    expect(adv?.innerHTML).not.toContain("trace buffers");
    expect(adv?.innerHTML).toContain("tracing level");
  });

  it("check that changing enable config causes box to change", async () => {
    component.trace.config!.enableConfigs[0].enabled = false;
    fixture.detectChanges();
    await fixture.whenStable();
    expect(htmlElement.querySelector(".enable-config")?.innerHTML).toContain("aria-checked=\"false\"");
  });

  it("check that changing selected config causes select to change", async () => {
    fixture.detectChanges();
    expect(htmlElement.querySelector(".selection")?.innerHTML).toContain("value=\"debug\"");
    component.trace.config!.selectionConfigs[0].value = "verbose";
    fixture.detectChanges();
    fixture.whenStable().then(() => {
      expect(htmlElement.querySelector(".selection")?.innerHTML).toContain("value=\"verbose\"");
    });
  });
});
