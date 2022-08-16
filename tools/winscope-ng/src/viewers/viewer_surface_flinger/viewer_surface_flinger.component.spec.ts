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
import {ViewerSurfaceFlingerComponent} from "./viewer_surface_flinger.component";

import { HierarchyComponent } from "viewers/hierarchy.component";
import { PropertiesComponent } from "viewers/properties.component";
import { RectsComponent } from "viewers/rects.component";
import { MatIconModule } from "@angular/material/icon";
import { MatCardModule } from "@angular/material/card";
import { ComponentFixtureAutoDetect } from "@angular/core/testing";
import { CUSTOM_ELEMENTS_SCHEMA } from "@angular/core";

describe("ViewerSurfaceFlingerComponent", () => {
  let fixture: ComponentFixture<ViewerSurfaceFlingerComponent>;
  let component: ViewerSurfaceFlingerComponent;
  let htmlElement: HTMLElement;

  beforeAll(async () => {
    await TestBed.configureTestingModule({
      providers: [
        { provide: ComponentFixtureAutoDetect, useValue: true }
      ],
      imports: [
        MatIconModule,
        MatCardModule
      ],
      declarations: [
        ViewerSurfaceFlingerComponent,
        HierarchyComponent,
        PropertiesComponent,
        RectsComponent
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ViewerSurfaceFlingerComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
  });

  it("can be created", () => {
    expect(component).toBeTruthy();
  });

  it("creates rects view", () => {
    const rectsView = htmlElement.querySelector(".rects-view");
    expect(rectsView).toBeTruthy();
  });

  it("creates hierarchy view", () => {
    const hierarchyView = htmlElement.querySelector("#sf-hierarchy-view");
    expect(hierarchyView).toBeTruthy();
  });

  it("creates properties view", () => {
    const propertiesView = htmlElement.querySelector("#sf-properties-view");
    expect(propertiesView).toBeTruthy();
  });
});
