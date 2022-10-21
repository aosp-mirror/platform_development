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
import {CommonModule} from "@angular/common";
import {CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA} from "@angular/core";
import {ComponentFixture, TestBed} from "@angular/core/testing";
import {MatCardModule} from "@angular/material/card";
import {TraceViewComponent} from "./trace_view.component";
import {View, Viewer, ViewType} from "viewers/viewer";

class FakeViewer implements Viewer {
  constructor(title: string, content: string) {
    this.title = title;
    this.htmlElement = document.createElement("div");
    this.htmlElement.innerText = content;
  }

  notifyCurrentTraceEntries(entries: any) {
    // do nothing
  }

  getViews(): View[] {
    return [new View(ViewType.TAB, this.htmlElement, this.title)];
  }

  getDependencies(): any[] {
    return [];
  }

  private htmlElement: HTMLElement;
  private title: string;
}

describe("TraceViewComponent", () => {
  let fixture: ComponentFixture<TraceViewComponent>;
  let component: TraceViewComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TraceViewComponent],
      imports: [
        CommonModule,
        MatCardModule
      ],
      schemas: [NO_ERRORS_SCHEMA, CUSTOM_ELEMENTS_SCHEMA]
    }).compileComponents();
    fixture = TestBed.createComponent(TraceViewComponent);
    htmlElement = fixture.nativeElement;
    component = fixture.componentInstance;
    component.viewers = [
      new FakeViewer("Title0", "Content0"),
      new FakeViewer("Title1", "Content1")
    ];
    component.ngOnChanges();
    fixture.detectChanges();
  });

  it("can be created", () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it("creates viewer tabs", () => {
    const tabs: NodeList = htmlElement.querySelectorAll(".viewer-tab");
    expect(tabs.length).toEqual(2);
    expect(tabs.item(0)!.textContent).toEqual("Title0");
    expect(tabs.item(1)!.textContent).toEqual("Title1");
  });

  it("changes active viewer on click", () => {
    const tabs = htmlElement.querySelectorAll(".viewer-tab");
    const tabsContent =
      htmlElement.querySelectorAll(".trace-view-content div");

    // Initially tab 0
    fixture.detectChanges();
    expect(tabsContent.length).toEqual(2);
    expect(tabsContent[0].innerHTML).toEqual("Content0");
    expect(tabsContent[1].innerHTML).toEqual("Content1");
    expect((<any>tabsContent[0]).style?.display).toEqual("");
    expect((<any>tabsContent[1]).style?.display).toEqual("none");

    // Switch to tab 1
    tabs[1].dispatchEvent(new Event("click"));
    fixture.detectChanges();
    expect(tabsContent.length).toEqual(2);
    expect(tabsContent[0].innerHTML).toEqual("Content0");
    expect(tabsContent[1].innerHTML).toEqual("Content1");
    expect((<any>tabsContent[0]).style?.display).toEqual("none");
    expect((<any>tabsContent[1]).style?.display).toEqual("");

    // Switch to tab 0
    tabs[0].dispatchEvent(new Event("click"));
    fixture.detectChanges();
    expect(tabsContent.length).toEqual(2);
    expect(tabsContent[0].innerHTML).toEqual("Content0");
    expect(tabsContent[1].innerHTML).toEqual("Content1");
    expect((<any>tabsContent[0]).style?.display).toEqual("");
    expect((<any>tabsContent[1]).style?.display).toEqual("none");
  });

  it("emits event on download button click", () => {
    const spy = spyOn(component.downloadTracesButtonClick, "emit");

    const downloadButton: null|HTMLButtonElement =
      htmlElement.querySelector(".save-btn");
    expect(downloadButton).toBeInstanceOf(HTMLButtonElement);

    downloadButton?.dispatchEvent(new Event("click"));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledTimes(1);

    downloadButton?.dispatchEvent(new Event("click"));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledTimes(2);
  });
});
