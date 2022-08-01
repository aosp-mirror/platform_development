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
import { Component , ViewChild } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { RectsComponent } from "./rects.component";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatCardModule } from "@angular/material/card";
import { MatRadioModule } from "@angular/material/radio";
import { MatSliderModule } from "@angular/material/slider";
import { CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA } from "@angular/core";
import { Rectangle } from "../ui_data/ui_data_surface_flinger";
import { CanvasService } from "./canvas.service";

describe("RectsComponent", () => {
  let component: TestHostComponent;
  let fixture: ComponentFixture<TestHostComponent>;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        MatCheckboxModule,
        MatCardModule,
        MatSliderModule,
        MatRadioModule
      ],
      declarations: [RectsComponent, TestHostComponent],
      providers: [CanvasService],
      schemas: [NO_ERRORS_SCHEMA, CUSTOM_ELEMENTS_SCHEMA]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    fixture.detectChanges();
  });

  it("can be created", () => {
    expect(component).toBeTruthy();
  });

  it("check that layer separation slider is rendered", () => {
    fixture.detectChanges();
    const slider = htmlElement.querySelector("mat-slider");
    expect(slider).toBeTruthy();
  });

  it("check that layer separation slider causes view to change", () => {
    const slider = htmlElement.querySelector("mat-slider");
    spyOn(component.rectsComponent.canvasService, "updateLayerSeparation");
    slider?.dispatchEvent(new MouseEvent("mousedown"));
    fixture.detectChanges();
    expect(component.rectsComponent.canvasService.updateLayerSeparation).toHaveBeenCalled();
  });

  it("check that rects canvas is rendered", () => {
    fixture.detectChanges();
    const rectsCanvas = htmlElement.querySelector("#rects-canvas");
    expect(rectsCanvas).toBeTruthy();
  });

  it("check that canvas is refreshed if rects are present", async () => {
    component.addRects([
      {
        topLeft: {x:0, y:0},
        bottomRight: {x:1, y:-1},
        label: "rectangle1",
        transform: {
          matrix: {
            dsdx: 1,
            dsdy: 0,
            dtdx: 0,
            dtdy: 1,
            tx: 0,
            ty: 0
          }
        },
        height: 1,
        width: 1,
        isVisible: true,
        isDisplay: false,
        ref: null,
        id: 12345,
        stackId: 0,
      }
    ]);
    spyOn(component.rectsComponent, "updateVariablesBeforeRefresh").and.callThrough();
    fixture.detectChanges();
    await new Promise( resolve => setTimeout(resolve, 4000));
    expect(component.rectsComponent.updateVariablesBeforeRefresh).toHaveBeenCalled();
  });

  @Component({
    selector: "host-component",
    template: "<rects-view [rects]=\"rects ?? []\"></rects-view>"
  })
  class TestHostComponent {
    public rects: Rectangle[];

    addRects(newRects: Rectangle[]) {
      this.rects = newRects;
    }

    @ViewChild(RectsComponent)
    public rectsComponent: RectsComponent;
  }
});
