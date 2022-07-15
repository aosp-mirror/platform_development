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
import {ComponentFixture, TestBed} from "@angular/core/testing";
import {AppComponent} from "./app.component";

describe("AppComponent", () => {
  let fixture: ComponentFixture<AppComponent>;
  let component: AppComponent;
  let htmlElement: HTMLElement;

  beforeAll(async () => {
    await TestBed.configureTestingModule({
      declarations: [
        AppComponent
      ],
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

  it("renders the title", () => {
    expect(htmlElement.querySelector("div#title")?.innerHTML).toContain("Winscope Viewer 2.0");
  });
});
