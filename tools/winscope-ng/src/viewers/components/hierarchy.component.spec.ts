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
import {ComponentFixture, TestBed, ComponentFixtureAutoDetect} from "@angular/core/testing";
import { HierarchyComponent } from "./hierarchy.component";
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { PersistentStore } from "common/persistent_store";
import { CommonModule } from "@angular/common";
import { MatInputModule } from "@angular/material/input";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { HierarchyTree } from "viewers/common/tree_utils";
import { HierarchyTreeBuilder } from "test/unit/hierarchy_tree_builder";

describe("HierarchyComponent", () => {
  let fixture: ComponentFixture<HierarchyComponent>;
  let component: HierarchyComponent;
  let htmlElement: HTMLElement;

  beforeAll(async () => {
    await TestBed.configureTestingModule({
      providers: [
        { provide: ComponentFixtureAutoDetect, useValue: true }
      ],
      declarations: [
        HierarchyComponent
      ],
      imports: [
        CommonModule,
        MatInputModule,
        MatFormFieldModule,
        MatCheckboxModule,
        BrowserAnimationsModule
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(HierarchyComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;

    component.tree = new HierarchyTreeBuilder().setName("BaseLayerTraceEntry").setKind("entry").setStableId("BaseEntry")
      .setChildren([new HierarchyTreeBuilder().setName("Child1").setStableId("3 Child1").build()])
      .build();

    component.store = new PersistentStore();
    component.userOptions = {
      onlyVisible: {
        name: "Only visible",
        enabled: false
      },
    };
    component.pinnedItems = [component.tree];
    component.diffClass = jasmine.createSpy().and.returnValue("none");
  });

  it("can be created", () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it("creates title", () => {
    fixture.detectChanges();
    const title = htmlElement.querySelector(".hierarchy-title");
    expect(title).toBeTruthy();
  });

  it("creates view controls", () => {
    fixture.detectChanges();
    const viewControls = htmlElement.querySelector(".view-controls");
    expect(viewControls).toBeTruthy();
  });

  it("creates initial tree elements", () => {
    fixture.detectChanges();
    const tree = htmlElement.querySelector(".tree-wrapper");
    expect(tree).toBeTruthy();
  });
});
