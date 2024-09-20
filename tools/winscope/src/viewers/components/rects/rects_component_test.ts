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
import {Component, ViewChild} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatButtonModule} from '@angular/material/button';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatIconTestingModule} from '@angular/material/icon/testing';
import {MatSelectModule} from '@angular/material/select';
import {MatSliderModule} from '@angular/material/slider';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {Box3D} from 'common/geometry/box3d';
import {TransformMatrix} from 'common/geometry/transform_matrix';
import {PersistentStore} from 'common/persistent_store';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {TraceType} from 'trace/trace_type';
import {VISIBLE_CHIP} from 'viewers/common/chip';
import {DisplayIdentifier} from 'viewers/common/display_identifier';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
import {RectsComponent} from 'viewers/components/rects/rects_component';
import {UiRect} from 'viewers/components/rects/ui_rect';
import {UserOptionsComponent} from 'viewers/components/user_options_component';
import {Camera} from './camera';
import {Canvas} from './canvas';
import {ColorType} from './color_type';
import {RectLabel} from './rect_label';
import {ShadingMode} from './shading_mode';
import {UiRect3D} from './ui_rect3d';
import {UiRectBuilder} from './ui_rect_builder';

describe('RectsComponent', () => {
  const rectGroup0 = makeRectWithGroupId(0);
  const rectGroup1 = makeRectWithGroupId(1);
  const rectGroup2 = makeRectWithGroupId(2);

  let component: TestHostComponent;
  let fixture: ComponentFixture<TestHostComponent>;
  let htmlElement: HTMLElement;
  let updateViewPositionSpy: jasmine.Spy<(camera: Camera, box: Box3D) => void>;
  let updateRectsSpy: jasmine.Spy<(rects: UiRect3D[]) => void>;
  let updateLabelsSpy: jasmine.Spy<(labels: RectLabel[]) => void>;
  let renderViewSpy: jasmine.Spy<() => void>;

  beforeEach(async () => {
    updateViewPositionSpy = spyOn(Canvas.prototype, 'updateViewPosition');
    updateRectsSpy = spyOn(Canvas.prototype, 'updateRects');
    updateLabelsSpy = spyOn(Canvas.prototype, 'updateLabels');
    renderViewSpy = spyOn(Canvas.prototype, 'renderView');
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        MatDividerModule,
        MatSliderModule,
        MatButtonModule,
        MatTooltipModule,
        MatIconModule,
        MatIconTestingModule,
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
      schemas: [],
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
    resetSpies();

    checkAllSpiesCalled(0);
    component.rects = [rectGroup0];
    fixture.detectChanges();
    checkAllSpiesCalled(1);
    component.rects = [rectGroup0];
    fixture.detectChanges();
    checkAllSpiesCalled(2);
  });

  it('draws scene when rotation slider changes', () => {
    fixture.detectChanges();
    resetSpies();
    const slider = assertDefined(htmlElement.querySelector('.slider-rotation'));

    checkAllSpiesCalled(0);
    slider.dispatchEvent(new MouseEvent('mousedown'));
    expect(updateViewPositionSpy).toHaveBeenCalledTimes(1);
    expect(updateRectsSpy).toHaveBeenCalledTimes(0);
    expect(updateLabelsSpy).toHaveBeenCalledTimes(1);
    expect(renderViewSpy).toHaveBeenCalledTimes(1);
  });

  it('draws scene when spacing slider changes', () => {
    fixture.detectChanges();
    resetSpies();
    const slider = assertDefined(htmlElement.querySelector('.slider-spacing'));

    checkAllSpiesCalled(0);
    slider.dispatchEvent(new MouseEvent('mousedown'));
    checkAllSpiesCalled(1);
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

  it('renders display selector', async () => {
    component.rects = [rectGroup0];
    component.displays = [
      {displayId: 0, groupId: 0, name: 'Display 0', isActive: false},
      {displayId: 1, groupId: 1, name: 'Display 1', isActive: false},
      {displayId: 2, groupId: 2, name: 'Display 2', isActive: false},
    ];
    await checkSelectedDisplay([0]);
  });

  it('handles display change by checkbox', async () => {
    component.rects = [rectGroup0, rectGroup1];
    component.displays = [
      {displayId: 0, groupId: 0, name: 'Display 0', isActive: false},
      {displayId: 1, groupId: 1, name: 'Display 1', isActive: false},
      {displayId: 2, groupId: 2, name: 'Display 2', isActive: false},
    ];
    await checkSelectedDisplay([0]);

    const trigger = assertDefined(
      htmlElement.querySelector('.displays-section .mat-select-trigger'),
    );
    (trigger as HTMLElement).click();
    fixture.detectChanges();

    const options = document.querySelectorAll<HTMLElement>(
      '.mat-select-panel .mat-option',
    );

    options.item(1).click();
    await checkSelectedDisplay([0, 1]);

    options.item(0).click();
    await checkSelectedDisplay([1]);

    options.item(1).click();
    await checkSelectedDisplay([]);
    const placeholder = assertDefined(
      htmlElement.querySelector('.placeholder-text'),
    );
    expect(placeholder.textContent?.trim()).toEqual('No displays selected.');
  });

  it('handles display change by "only" button', async () => {
    component.rects = [rectGroup0, rectGroup1];
    component.displays = [
      {displayId: 0, groupId: 0, name: 'Display 0', isActive: false},
      {displayId: 1, groupId: 1, name: 'Display 1', isActive: false},
      {displayId: 2, groupId: 2, name: 'Display 2', isActive: false},
    ];
    await checkSelectedDisplay([0]);

    const trigger = assertDefined(
      htmlElement.querySelector('.displays-section .mat-select-trigger'),
    );
    (trigger as HTMLElement).click();
    fixture.detectChanges();

    const onlyButtons = document.querySelectorAll<HTMLElement>(
      '.mat-select-panel .mat-option .option-only-button',
    );

    const display0Button = onlyButtons.item(0);
    const display1Button = onlyButtons.item(1);

    // no change
    display0Button.click();
    await checkSelectedDisplay([0]);

    display1Button.click();
    await checkSelectedDisplay([1]);

    assertDefined(display0Button.parentElement).click();
    await checkSelectedDisplay([0, 1]);
    display0Button.click();
    await checkSelectedDisplay([0]);
  });

  it('tracks selected display', async () => {
    component.displays = [
      {displayId: 10, groupId: 0, name: 'Display 0', isActive: false},
      {displayId: 20, groupId: 1, name: 'Display 1', isActive: false},
    ];
    component.rects = [rectGroup0, rectGroup1];
    await checkSelectedDisplay([0]);

    component.displays = [
      {displayId: 20, groupId: 2, name: 'Display 1', isActive: false},
      {displayId: 10, groupId: 1, name: 'Display 0', isActive: false},
    ];
    await checkSelectedDisplay([0], [1]);
  });

  it('updates scene on separation slider change', () => {
    component.rects = [rectGroup0, rectGroup0];
    fixture.detectChanges();
    updateSeparationSlider();

    checkAllSpiesCalled(2);
    const rectsBefore = assertDefined(updateRectsSpy.calls.first().args[0]);
    const rectsAfter = assertDefined(updateRectsSpy.calls.mostRecent().args[0]);

    expect(rectsBefore[0].topLeft.z).toEqual(200);
    expect(rectsAfter[0].topLeft.z).toEqual(12);
  });

  it('updates scene on rotation slider change', () => {
    component.rects = [rectGroup0];
    fixture.detectChanges();
    updateRotationSlider();

    expect(updateViewPositionSpy).toHaveBeenCalledTimes(2);
    expect(updateRectsSpy).toHaveBeenCalledTimes(1);
    expect(updateLabelsSpy).toHaveBeenCalledTimes(2);
    expect(renderViewSpy).toHaveBeenCalledTimes(2);

    const cameraBefore = assertDefined(
      updateViewPositionSpy.calls.first().args[0],
    );
    const cameraAfter = assertDefined(
      updateViewPositionSpy.calls.mostRecent().args[0],
    );

    expect(cameraAfter.rotationAngleX).toEqual(
      cameraBefore.rotationAngleX * 0.5,
    );
    expect(cameraAfter.rotationAngleY).toEqual(
      cameraBefore.rotationAngleY * 0.5,
    );
  });

  it('updates scene on shading mode change', () => {
    component.rects = [rectGroup0];
    fixture.detectChanges();

    updateShadingMode(ShadingMode.GRADIENT, ShadingMode.WIRE_FRAME);
    updateShadingMode(ShadingMode.WIRE_FRAME, ShadingMode.OPACITY);

    expect(updateViewPositionSpy).toHaveBeenCalledTimes(1);
    expect(updateRectsSpy).toHaveBeenCalledTimes(3);
    expect(updateLabelsSpy).toHaveBeenCalledTimes(1);
    expect(renderViewSpy).toHaveBeenCalledTimes(3);

    const rectsGradient = assertDefined(updateRectsSpy.calls.first().args[0]);
    const rectsWireFrame = assertDefined(updateRectsSpy.calls.argsFor(1).at(0));
    const rectsOpacity = assertDefined(
      updateRectsSpy.calls.mostRecent().args[0],
    );

    expect(rectsGradient[0].colorType).toEqual(ColorType.VISIBLE);
    expect(rectsGradient[0].darkFactor).toEqual(1);

    expect(rectsWireFrame[0].colorType).toEqual(ColorType.EMPTY);
    expect(rectsWireFrame[0].darkFactor).toEqual(1);

    expect(rectsOpacity[0].colorType).toEqual(ColorType.VISIBLE_WITH_OPACITY);
    expect(rectsOpacity[0].darkFactor).toEqual(0.5);
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

  it('defaults initial selection to first active display with rects', async () => {
    component.rects = [rectGroup0, rectGroup1];
    component.displays = [
      {displayId: 10, groupId: 1, name: 'Display 0', isActive: false},
      {displayId: 20, groupId: 0, name: 'Display 1', isActive: true},
    ];
    await checkSelectedDisplay([1], [0]);
  });

  it('defaults initial selection to first display with non-display rects and groupId 0', async () => {
    component.displays = [
      {displayId: 10, groupId: 1, name: 'Display 0', isActive: true},
      {displayId: 20, groupId: 0, name: 'Display 1', isActive: false},
    ];
    component.rects = [rectGroup0];
    await checkSelectedDisplay([1], [0]);
  });

  it('defaults initial selection to first display with non-display rects and groupId non-zero', async () => {
    component.displays = [
      {displayId: 10, groupId: 0, name: 'Display 0', isActive: false},
      {displayId: 20, groupId: 1, name: 'Display 1', isActive: false},
    ];
    component.rects = [rectGroup1];
    await checkSelectedDisplay([1]);
  });

  it('handles change from zero to one display', async () => {
    component.displays = [];
    component.rects = [];
    await checkSelectedDisplay([]);
    const placeholder = assertDefined(
      htmlElement.querySelector('.placeholder-text'),
    );
    expect(placeholder.textContent?.trim()).toEqual('No rects found.');

    component.rects = [rectGroup0];
    component.displays = [
      {displayId: 10, groupId: 0, name: 'Display 0', isActive: false},
    ];
    await checkSelectedDisplay([0]);
  });

  it('draws mini rects with non-present group id', () => {
    component.displays = [
      {displayId: 10, groupId: 0, name: 'Display 0', isActive: false},
    ];
    fixture.detectChanges();
    component.rects = [rectGroup0];
    component.miniRects = [rectGroup2];
    resetSpies();
    fixture.detectChanges();
    checkAllSpiesCalled(2);
    expect(
      updateRectsSpy.calls
        .all()
        .forEach((call) => expect(call.args[0].length).toEqual(1)),
    );
  });

  it('draws mini rects with default spacing, rotation and shading mode', () => {
    component.displays = [
      {displayId: 10, groupId: 0, name: 'Display 0', isActive: false},
    ];
    fixture.detectChanges();

    updateSeparationSlider();
    updateRotationSlider();
    updateShadingMode(ShadingMode.GRADIENT, ShadingMode.WIRE_FRAME);

    component.rects = [rectGroup0, rectGroup0];
    component.miniRects = [rectGroup0, rectGroup0];
    resetSpies();
    fixture.detectChanges();
    checkAllSpiesCalled(2);

    const largeRectsCamera = assertDefined(
      updateViewPositionSpy.calls.first().args[0],
    );
    const miniRectsCamera = assertDefined(
      updateViewPositionSpy.calls.mostRecent().args[0],
    );

    expect(largeRectsCamera.rotationAngleX).toEqual(
      miniRectsCamera.rotationAngleX * 0.5,
    );
    expect(largeRectsCamera.rotationAngleY).toEqual(
      miniRectsCamera.rotationAngleY * 0.5,
    );
    const largeRects = assertDefined(updateRectsSpy.calls.first().args[0]);
    const miniRects = assertDefined(updateRectsSpy.calls.mostRecent().args[0]);

    expect(largeRects[0].colorType).toEqual(ColorType.EMPTY);
    expect(miniRects[0].colorType).toEqual(ColorType.VISIBLE);

    expect(largeRects[0].topLeft.z).toEqual(12);
    expect(miniRects[0].topLeft.z).toEqual(200);
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

  it('updates scene on pinned items change', () => {
    component.rects = [rectGroup0];
    fixture.detectChanges();
    resetSpies();

    component.pinnedItems = [
      UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder().setId('test-id').setName('0').build(),
      ),
    ];
    fixture.detectChanges();
    expect(updateViewPositionSpy).toHaveBeenCalledTimes(0);
    expect(updateRectsSpy).toHaveBeenCalledTimes(1);
    expect(updateLabelsSpy).toHaveBeenCalledTimes(0);
    expect(renderViewSpy).toHaveBeenCalledTimes(1);
    expect(updateRectsSpy.calls.mostRecent().args[0][0].isPinned).toBeTrue();
  });

  it('emits rect id on rect click', () => {
    component.rects = [rectGroup0];
    fixture.detectChanges();

    const testString = 'test_id';
    spyOn(Canvas.prototype, 'getClickedRectId').and.returnValue(testString);
    let id: string | undefined;
    htmlElement.addEventListener(ViewerEvents.HighlightedIdChange, (event) => {
      id = (event as CustomEvent).detail.id;
    });

    const canvas = assertDefined(
      htmlElement.querySelector<HTMLElement>('.large-rects-canvas'),
    );
    canvas.click();
    fixture.detectChanges();
    expect(id).toEqual(testString);
  });

  it('pans view without emitting rect id', () => {
    component.rects = [rectGroup0];
    fixture.detectChanges();
    const cameraBefore: Camera =
      updateViewPositionSpy.calls.mostRecent().args[0];
    expect(cameraBefore.panScreenDistance.dx).toEqual(0);
    expect(cameraBefore.panScreenDistance.dy).toEqual(0);
    resetSpies();

    const testString = 'test_id';
    spyOn(Canvas.prototype, 'getClickedRectId').and.returnValue(testString);
    let id: string | undefined;
    htmlElement.addEventListener(ViewerEvents.HighlightedIdChange, (event) => {
      id = (event as CustomEvent).detail.id;
    });

    const canvas = assertDefined(
      htmlElement.querySelector<HTMLElement>('.large-rects-canvas'),
    );
    canvas.dispatchEvent(new MouseEvent('mousedown'));
    const mouseMoveEvent = new MouseEvent('mousemove');
    Object.defineProperty(mouseMoveEvent, 'movementX', {value: 5});
    Object.defineProperty(mouseMoveEvent, 'movementY', {value: 10});
    document.dispatchEvent(mouseMoveEvent);
    document.dispatchEvent(new MouseEvent('mouseup'));
    fixture.detectChanges();

    expect(updateViewPositionSpy).toHaveBeenCalledTimes(1);
    expect(updateRectsSpy).not.toHaveBeenCalled();
    expect(updateLabelsSpy).not.toHaveBeenCalled();
    expect(renderViewSpy).toHaveBeenCalled();

    const cameraAfter: Camera =
      updateViewPositionSpy.calls.mostRecent().args[0];
    expect(cameraAfter.panScreenDistance.dx).toEqual(5);
    expect(cameraAfter.panScreenDistance.dy).toEqual(10);

    canvas.click();
    fixture.detectChanges();
    expect(id).toBeUndefined();

    canvas.click();
    fixture.detectChanges();
    expect(id).toEqual(testString);
  });

  function resetSpies() {
    [
      updateViewPositionSpy,
      updateRectsSpy,
      updateLabelsSpy,
      renderViewSpy,
    ].forEach((spy) => spy.calls.reset());
  }

  async function checkSelectedDisplay(
    displayNumbers: number[],
    testIds?: number[],
  ) {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const displaySelect = assertDefined(
      htmlElement.querySelector('.displays-select'),
    );
    expect(displaySelect.textContent?.trim()).toEqual(
      displayNumbers
        .map((displayNumber) => `Display ${displayNumber}`)
        .join(', '),
    );
    const drawnRects: UiRect3D[] = updateRectsSpy.calls.mostRecent().args[0];
    expect(drawnRects.length).toEqual(displayNumbers.length);
    drawnRects.forEach((rect, index) => {
      expect(rect.id).toEqual(
        `test-id ${testIds ? testIds[index] : displayNumbers[index]}`,
      );
      if (index > 0) expect(rect.transform.ty).toBeGreaterThan(0);
    });
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
      .setIsActiveDisplay(false)
      .setId('test-id ' + groupId)
      .setGroupId(groupId)
      .setIsClickable(false)
      .setCornerRadius(0)
      .setDepth(0)
      .setOpacity(0.5)
      .build();
  }

  function checkAllSpiesCalled(times: number) {
    [
      updateViewPositionSpy,
      updateRectsSpy,
      updateLabelsSpy,
      renderViewSpy,
    ].forEach((spy) => expect(spy).toHaveBeenCalledTimes(times));
  }

  @Component({
    selector: 'host-component',
    template: `
      <rects-view
        title="TestRectsView"
        [store]="store"
        [rects]="rects"
        [miniRects]="miniRects"
        [displays]="displays"
        [isStackBased]="isStackBased"
        [shadingModes]="shadingModes"
        [userOptions]="userOptions"
        [dependencies]="dependencies"
        [pinnedItems]="pinnedItems"></rects-view>
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
    pinnedItems: UiHierarchyTreeNode[] = [];

    @ViewChild(RectsComponent)
    rectsComponent: RectsComponent | undefined;
  }
});
