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
import { HierarchyTreeBuilder } from "test/unit/hierarchy_tree_builder";
import { TreeComponent } from "viewers/components/tree.component";
import { TreeNodeComponent } from "viewers/components/tree_node.component";
import { TreeNodeDataViewComponent } from "viewers/components/tree_node_data_view.component";

describe("HierarchyComponent", () => {
  let fixture: ComponentFixture<HierarchyComponent>;
  let component: HierarchyComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        { provide: ComponentFixtureAutoDetect, useValue: true }
      ],
      declarations: [
        HierarchyComponent,
        TreeComponent,
        TreeNodeComponent,
        TreeNodeDataViewComponent
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

    fixture = TestBed.createComponent(HierarchyComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;

    component.tree = new HierarchyTreeBuilder().setName("Root node").setChildren([
      new HierarchyTreeBuilder().setName("Child node").build()
    ]).build();

    component.store = new PersistentStore();
    component.userOptions = {
      onlyVisible: {
        name: "Only visible",
        enabled: false
      },
    };
    component.pinnedItems = [component.tree];

    fixture.detectChanges();
  });

  it("can be created", () => {
    expect(component).toBeTruthy();
  });

  it("renders title", () => {
    const title = htmlElement.querySelector(".hierarchy-title");
    expect(title).toBeTruthy();
  });

  it("renders view controls", () => {
    const viewControls = htmlElement.querySelector(".view-controls");
    expect(viewControls).toBeTruthy();
  });

  it("renders initial tree elements", async () => {
    const treeView = htmlElement.querySelector("tree-view");
    expect(treeView).toBeTruthy();
    expect(treeView!.innerHTML).toContain("Root node");
    expect(treeView!.innerHTML).toContain("Child node");
  });
});
