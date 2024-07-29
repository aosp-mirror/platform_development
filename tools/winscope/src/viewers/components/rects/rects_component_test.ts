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

import {CommonModule} from '@angular/common';
import {HttpClientModule} from '@angular/common/http';
import {Component, CUSTOM_ELEMENTS_SCHEMA, ViewChild} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatButtonModule} from '@angular/material/button';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatSelectModule} from '@angular/material/select';
import {MatSliderModule} from '@angular/material/slider';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {TransformMatrix} from 'common/geometry_types';
import {PersistentStore} from 'common/persistent_store';
import {TraceType} from 'trace/trace_type';
import {VISIBLE_CHIP} from 'viewers/common/chip';
import {DisplayIdentifier} from 'viewers/common/display_identifier';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
import {RectsComponent} from 'viewers/components/rects/rects_component';
import {UiRect} from 'viewers/components/rects/types2d';
import {UserOptionsComponent} from 'viewers/components/user_options_component';
import {Canvas} from './canvas';
import {ColorType, ShadingMode} from './types3d';
import {UiRectBuilder} from './ui_rect_builder';

describe('RectsComponent', () => {
  let component: TestHostComponent;
  let fixture: ComponentFixture<TestHostComponent>;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        MatDividerModule,
        MatSliderModule,
        MatButtonModule,
        MatTooltipModule,
        MatIconModule,
        HttpClientModule,
        MatSelectModule,
        BrowserAnimationsModule,
        MatFormFieldModule,
      ],
      declarations: [
        TestHostComponent,
        RectsComponent,
        CollapsibleSectionTitleComponent,
        UserOptionsComponent,
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders rotation slider', () => {
    const slider = htmlElement.querySelector('mat-slider.slider-rotation');
    expect(slider).toBeTruthy();
  });

  it('renders separation slider', () => {
    const slider = htmlElement.querySelector('mat-slider.slider-spacing');
    expect(slider).toBeTruthy();
  });

  it('renders canvas', () => {
    const rectsCanvas = htmlElement.querySelector('.large-rects-canvas');
    expect(rectsCanvas).toBeTruthy();
  });

  it('draws scene when input data changes', async () => {
    fixture.detectChanges();
    spyOn(Canvas.prototype, 'draw').and.callThrough();

    const inputRect = makeRectWithGroupId(0);

    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(0);
    component.rects = [inputRect];
    fixture.detectChanges();
    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(1);
    component.rects = [inputRect];
    fixture.detectChanges();
    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(2);
  });

  it('draws scene when rotation slider changes', () => {
    fixture.detectChanges();
    spyOn(Canvas.prototype, 'draw').and.callThrough();
    const slider = assertDefined(htmlElement.querySelector('.slider-rotation'));

    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(0);

    slider.dispatchEvent(new MouseEvent('mousedown'));
    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(1);
  });

  it('draws scene when spacing slider changes', () => {
    fixture.detectChanges();
    spyOn(Canvas.prototype, 'draw').and.callThrough();
    const slider = assertDefined(htmlElement.querySelector('.slider-spacing'));

    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(0);

    slider.dispatchEvent(new MouseEvent('mousedown'));
    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(1);
  });

  it('unfocuses spacing slider on click', () => {
    fixture.detectChanges();
    const spacingSlider = assertDefined(
      htmlElement.querySelector('.slider-spacing'),
    );
    checkSliderUnfocusesOnClick(spacingSlider, 0.02);
  });

  it('unfocuses rotation slider on click', () => {
    fixture.detectChanges();
    const rotationSlider = assertDefined(
      htmlElement.querySelector('.slider-rotation'),
    );
    checkSliderUnfocusesOnClick(rotationSlider, 1);
  });

  it('renders display selector', () => {
    component.displays = [
      {displayId: 0, groupId: 0, name: 'Display 0'},
      {displayId: 1, groupId: 1, name: 'Display 1'},
      {displayId: 2, groupId: 2, name: 'Display 2'},
    ];
    fixture.detectChanges();
    checkSelectedDisplay('Display 0');
  });

  it('handles display change', () => {
    component.displays = [
      {displayId: 0, groupId: 0, name: 'Display 0'},
      {displayId: 1, groupId: 1, name: 'Display 1'},
      {displayId: 2, groupId: 2, name: 'Display 2'},
    ];
    fixture.detectChanges();
    checkSelectedDisplay('Display 0');

    const trigger = assertDefined(
      htmlElement.querySelector('.displays-section .mat-select-trigger'),
    );
    (trigger as HTMLElement).click();
    fixture.detectChanges();

    let groupId = 0;
    htmlElement.addEventListener(ViewerEvents.RectGroupIdChange, (event) => {
      groupId = (event as CustomEvent).detail.groupId;
    });

    const option = document
      .querySelectorAll('.mat-select-panel .mat-option')
      .item(1);
    (option as HTMLElement).click();
    fixture.detectChanges();
    checkSelectedDisplay('Display 1');
    expect(groupId).toEqual(1);
  });

  it('tracks selected display', () => {
    component.displays = [
      {displayId: 10, groupId: 0, name: 'Display 0'},
      {displayId: 20, groupId: 1, name: 'Display 1'},
    ];
    fixture.detectChanges();
    checkSelectedDisplay('Display 0');

    component.displays = [
      {displayId: 20, groupId: 2, name: 'Display 1'},
      {displayId: 10, groupId: 1, name: 'Display 0'},
    ];
    fixture.detectChanges();
    checkSelectedDisplay('Display 0');
  });

  it('updates scene on separation slider change', () => {
    const inputRect = makeRectWithGroupId(0);
    component.rects = [inputRect, inputRect];
    const spy = spyOn(Canvas.prototype, 'draw').and.callThrough();
    fixture.detectChanges();
    updateSeparationSlider();

    expect(spy).toHaveBeenCalledTimes(2);
    const sceneBefore = assertDefined(spy.calls.first().args.at(0));
    const sceneAfter = assertDefined(spy.calls.mostRecent().args.at(0));

    expect(sceneBefore.rects[0].topLeft.z).toEqual(200);
    expect(sceneAfter.rects[0].topLeft.z).toEqual(12);
  });

  it('updates scene on rotation slider change', () => {
    const inputRect = makeRectWithGroupId(0);
    component.rects = [inputRect];
    const spy = spyOn(Canvas.prototype, 'draw').and.callThrough();
    fixture.detectChanges();
    updateRotationSlider();

    expect(spy).toHaveBeenCalledTimes(2);
    const sceneBefore = assertDefined(spy.calls.first().args.at(0));
    const sceneAfter = assertDefined(spy.calls.mostRecent().args.at(0));

    expect(sceneAfter.camera.rotationAngleX).toEqual(
      sceneBefore.camera.rotationAngleX * 0.5,
    );
    expect(sceneAfter.camera.rotationAngleY).toEqual(
      sceneBefore.camera.rotationAngleY * 0.5,
    );
  });

  it('updates scene on shading mode change', () => {
    const inputRect = makeRectWithGroupId(0);
    component.rects = [inputRect];
    const spy = spyOn(Canvas.prototype, 'draw').and.callThrough();
    fixture.detectChanges();

    updateShadingMode(ShadingMode.GRADIENT, ShadingMode.WIRE_FRAME);
    updateShadingMode(ShadingMode.WIRE_FRAME, ShadingMode.OPACITY);

    expect(spy).toHaveBeenCalledTimes(3);
    const sceneGradient = assertDefined(spy.calls.first().args.at(0));
    const sceneWireFrame = assertDefined(spy.calls.argsFor(1).at(0));
    const sceneOpacity = assertDefined(spy.calls.mostRecent().args.at(0));

    expect(sceneGradient.rects[0].colorType).toEqual(ColorType.VISIBLE);
    expect(sceneGradient.rects[0].darkFactor).toEqual(1);

    expect(sceneWireFrame.rects[0].colorType).toEqual(ColorType.EMPTY);
    expect(sceneWireFrame.rects[0].darkFactor).toEqual(1);

    expect(sceneOpacity.rects[0].colorType).toEqual(
      ColorType.VISIBLE_WITH_OPACITY,
    );
    expect(sceneOpacity.rects[0].darkFactor).toEqual(0.5);
  });

  it('uses stored rects view settings', () => {
    fixture.detectChanges();

    updateSeparationSlider();
    updateShadingMode(ShadingMode.GRADIENT, ShadingMode.WIRE_FRAME);

    const newFixture = TestBed.createComponent(TestHostComponent);
    newFixture.detectChanges();
    const newRectsComponent = assertDefined(
      newFixture.componentInstance.rectsComponent,
    );
    expect(newRectsComponent.getZSpacingFactor()).toEqual(0.06);
    expect(newRectsComponent.getShadingMode()).toEqual(ShadingMode.WIRE_FRAME);
  });

  it('defaults initial selection to first display with non-display rects and groupId 0', () => {
    const inputRect = makeRectWithGroupId(0);
    component.rects = [inputRect];
    component.displays = [
      {displayId: 10, groupId: 1, name: 'Display 0'},
      {displayId: 20, groupId: 0, name: 'Display 1'},
    ];
    fixture.detectChanges();
    checkSelectedDisplay('Display 1');
  });

  it('defaults initial selection to first display with non-display rects and groupId non-zero', () => {
    const inputRect = makeRectWithGroupId(1);
    component.rects = [inputRect];
    component.displays = [
      {displayId: 10, groupId: 0, name: 'Display 0'},
      {displayId: 20, groupId: 1, name: 'Display 1'},
    ];
    fixture.detectChanges();
    checkSelectedDisplay('Display 1');
  });

  it('draws mini rects with non-present group id', () => {
    fixture.detectChanges();
    const inputRect = makeRectWithGroupId(0);
    const miniRect = makeRectWithGroupId(2);
    component.rects = [inputRect];
    component.displays = [{displayId: 10, groupId: 0, name: 'Display 0'}];
    component.miniRects = [miniRect];
    const spy = spyOn(Canvas.prototype, 'draw').and.callThrough();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledTimes(2);
    expect(
      spy.calls
        .all()
        .forEach((call) => expect(call.args[0].rects.length).toEqual(1)),
    );
  });

  it('draws mini rects with default spacing, rotation and shading mode', () => {
    fixture.detectChanges();

    updateSeparationSlider();
    updateRotationSlider();
    updateShadingMode(ShadingMode.GRADIENT, ShadingMode.WIRE_FRAME);

    const inputRect = makeRectWithGroupId(0);
    component.rects = [inputRect, inputRect];
    component.displays = [{displayId: 10, groupId: 0, name: 'Display 0'}];
    component.miniRects = [inputRect, inputRect];
    const spy = spyOn(Canvas.prototype, 'draw').and.callThrough();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledTimes(2);

    const largeRectsScene = assertDefined(spy.calls.first().args.at(0));
    const miniRectsScene = assertDefined(spy.calls.mostRecent().args.at(0));

    expect(largeRectsScene.camera.rotationAngleX).toEqual(
      miniRectsScene.camera.rotationAngleX * 0.5,
    );
    expect(largeRectsScene.camera.rotationAngleY).toEqual(
      miniRectsScene.camera.rotationAngleY * 0.5,
    );

    expect(largeRectsScene.rects[0].colorType).toEqual(ColorType.EMPTY);
    expect(miniRectsScene.rects[0].colorType).toEqual(ColorType.VISIBLE);

    expect(largeRectsScene.rects[0].topLeft.z).toEqual(12);
    expect(miniRectsScene.rects[0].topLeft.z).toEqual(200);
  });

  it('handles collapse button click', () => {
    fixture.detectChanges();
    const spy = spyOn(
      assertDefined(component.rectsComponent).collapseButtonClicked,
      'emit',
    );
    const collapseButton = assertDefined(
      htmlElement.querySelector('collapsible-section-title button'),
    ) as HTMLButtonElement;
    collapseButton.click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  function checkSelectedDisplay(selectedDisplay: string) {
    const displaySelect = assertDefined(
      htmlElement.querySelector('.displays-select'),
    );
    expect(displaySelect.innerHTML).toContain(selectedDisplay);
  }

  function findAndClickElement(selector: string) {
    const el = assertDefined(
      htmlElement.querySelector(selector),
    ) as HTMLElement;
    el.click();
    fixture.detectChanges();
  }

  function checkSliderUnfocusesOnClick(slider: Element, expectedValue: number) {
    const rectsComponent = assertDefined(component.rectsComponent);
    slider.dispatchEvent(new MouseEvent('mousedown'));
    expect(rectsComponent.getZSpacingFactor()).toEqual(expectedValue);
    htmlElement.dispatchEvent(
      new KeyboardEvent('keydown', {key: 'ArrowRight'}),
    );
    expect(rectsComponent.getZSpacingFactor()).toEqual(expectedValue);
    htmlElement.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowLeft'}));
    expect(rectsComponent.getZSpacingFactor()).toEqual(expectedValue);
  }

  function updateSeparationSlider() {
    const rectsComponent = assertDefined(component.rectsComponent);
    expect(rectsComponent.getZSpacingFactor()).toEqual(1);
    rectsComponent.onSeparationSliderChange(0.06);
    fixture.detectChanges();
    expect(rectsComponent.getZSpacingFactor()).toEqual(0.06);
  }

  function updateRotationSlider() {
    const rectsComponent = assertDefined(component.rectsComponent);
    rectsComponent.onRotationSliderChange(0.5);
    fixture.detectChanges();
  }

  function updateShadingMode(before: ShadingMode, after: ShadingMode) {
    const rectsComponent = assertDefined(component.rectsComponent);
    expect(rectsComponent.getShadingMode()).toEqual(before);
    findAndClickElement('.right-btn-container button.shading-mode');
    expect(rectsComponent.getShadingMode()).toEqual(after);
  }

  function makeRectWithGroupId(groupId: number, isVisible = true): UiRect {
    return new UiRectBuilder()
      .setX(0)
      .setY(0)
      .setWidth(1)
      .setHeight(1)
      .setLabel('rectangle1')
      .setTransform(
        TransformMatrix.from({
          dsdx: 1,
          dsdy: 0,
          dtdx: 0,
          dtdy: 1,
          tx: 0,
          ty: 0,
        }),
      )
      .setIsVisible(isVisible)
      .setIsDisplay(false)
      .setId('test-id-1234')
      .setGroupId(groupId)
      .setIsClickable(false)
      .setCornerRadius(0)
      .setDepth(0)
      .setOpacity(0.5)
      .build();
  }

  @Component({
    selector: 'host-component',
    template: `
      <rects-view
        title="TestRectsView"
        [store]="store"
        [rects]="rects"
        [isStackBased]="isStackBased"
        [displays]="displays"
        [miniRects]="miniRects"
        [shadingModes]="shadingModes"
        [userOptions]="userOptions"
        [dependencies]="dependencies"></rects-view>
    `,
  })
  class TestHostComponent {
    store = new PersistentStore();
    rects: UiRect[] = [];
    displays: DisplayIdentifier[] = [];
    miniRects: UiRect[] = [];
    isStackBased = false;
    shadingModes = [
      ShadingMode.GRADIENT,
      ShadingMode.WIRE_FRAME,
      ShadingMode.OPACITY,
    ];
    userOptions = {
      showOnlyVisible: {
        name: 'Show only',
        chip: VISIBLE_CHIP,
        enabled: false,
      },
    };
    dependencies = [TraceType.SURFACE_FLINGER];

    @ViewChild(RectsComponent)
    rectsComponent: RectsComponent | undefined;
  }
});
