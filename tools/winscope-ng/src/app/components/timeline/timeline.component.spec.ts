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
import {ChangeDetectionStrategy} from "@angular/core";
import {ComponentFixture, TestBed} from "@angular/core/testing";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatButtonModule} from "@angular/material/button";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatSelectModule} from "@angular/material/select";

import {MatDrawer, MatDrawerContainer, MatDrawerContent} from "app/components/bottomnav/bottom_drawer.component";
import {TimelineComponent} from "./timeline.component";
import {ExpandedTimelineComponent} from "./expanded_timeline.component";
import {MiniTimelineComponent} from "./mini_timeline.component";
import {TimelineCoordinator} from "app/timeline_coordinator";

describe("TimelineComponent", () => {
  let fixture: ComponentFixture<TimelineComponent>;
  let component: TimelineComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        TimelineCoordinator
      ],
      imports: [
        FormsModule,
        MatButtonModule,
        MatFormFieldModule,
        MatIconModule,
        MatSelectModule,
        ReactiveFormsModule
      ],
      declarations: [
        ExpandedTimelineComponent,
        MatDrawer,
        MatDrawerContainer,
        MatDrawerContent,
        MiniTimelineComponent,
        TimelineComponent,
      ]
    }).overrideComponent(TimelineComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
    fixture = TestBed.createComponent(TimelineComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
  });

  it("can be created", () => {
    expect(component).toBeTruthy();
  });

  it("can be expanded", () => {
    const button = htmlElement.querySelector(".button-toggle-expansion");
    expect(button).toBeTruthy();

    // initially not expanded
    expect(htmlElement.querySelector("expanded-timeline")).toBeFalsy();

    button!.dispatchEvent(new Event("click"));
    expect(htmlElement.querySelector("expanded-timeline")).toBeTruthy();

    button!.dispatchEvent(new Event("click"));
    expect(htmlElement.querySelector("expanded-timeline")).toBeFalsy();
  });
});
