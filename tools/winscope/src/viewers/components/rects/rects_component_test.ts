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
import {PersistentStore} from 'common/store/persistent_store';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {waitToBeCalled} from 'test/utils';
import {TraceType} from 'trace/trace_type';
import {VISIBLE_CHIP} from 'viewers/common/chip';
import {DisplayIdentifier} from 'viewers/common/display_identifier';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {RectType, UiRectType} from 'viewers/common/ui_rect_type';
import {RectDblClickDetail, ViewerEvents} from 'viewers/common/viewer_events';
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

  beforeAll(() => {
    localStorage.clear();
  });

  beforeEach(async () => {
    updateViewPositionSpy = spyOn(Canvas.prototype, 'updateViewPosition');
    updateRectsSpy = spyOn(Canvas.prototype, 'updateRects');
    updateLabelsSpy = spyOn(Canvas.prototype, 'updateLabels');
    renderViewSpy = spyOn(Canvas.prototype, 'renderView');

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
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
  });

  afterEach(() => {
    localStorage.clear();
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
    const boundingBox = updateViewPositionSpy.calls.mostRecent().args[1];
    resetSpies();

    checkAllSpiesCalled(0);
    component.rects = [rectGroup0];
    fixture.detectChanges();
    checkAllSpiesCalled(1);
    expect(updateViewPositionSpy.calls.mostRecent().args[1]).toEqual(
      boundingBox,
    );

    component.rects = [rectGroup0];
    fixture.detectChanges();
    checkAllSpiesCalled(2);
    expect(updateViewPositionSpy.calls.mostRecent().args[1]).toEqual(
      boundingBox,
    );
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
    await checkSelectedDisplay([0], [0]);
  });

  it('handles display change by checkbox', async () => {
    component.rects = [rectGroup0, rectGroup1];
    component.displays = [
      {displayId: 0, groupId: 0, name: 'Display 0', isActive: false},
      {displayId: 1, groupId: 1, name: 'Display 1', isActive: false},
      {displayId: 2, groupId: 2, name: 'Display 2', isActive: false},
    ];
    await checkSelectedDisplay([0], [0]);
    const boundingBox = updateViewPositionSpy.calls.mostRecent().args[1];

    openDisplaysSelect();
    const options = getDisplayOptions();

    options.item(1).click();
    await checkSelectedDisplay([0, 1], [0, 1], true);
    expect(updateViewPositionSpy.calls.mostRecent().args[1]).not.toEqual(
      boundingBox,
    );

    options.item(0).click();
    await checkSelectedDisplay([1], [1], true);

    options.item(1).click();
    await checkSelectedDisplay([], [], true);
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
    await checkSelectedDisplay([0], [0]);

    openDisplaysSelect();

    const onlyButtons = document.querySelectorAll<HTMLElement>(
      '.mat-select-panel .mat-option .option-only-button',
    );

    const display0Button = onlyButtons.item(0);
    const display1Button = onlyButtons.item(1);

    // no change
    display0Button.click();
    await checkSelectedDisplay([0], [0]);

    display1Button.click();
    await checkSelectedDisplay([1], [1]);

    assertDefined(display0Button.parentElement).click();
    await checkSelectedDisplay([0, 1], [0, 1], true);
    display0Button.click();
    await checkSelectedDisplay([0], [0], true);
  });

  it('tracks selected display', async () => {
    component.displays = [
      {displayId: 10, groupId: 0, name: 'Display 0', isActive: false},
      {displayId: 20, groupId: 1, name: 'Display 1', isActive: false},
    ];
    component.rects = [rectGroup0, rectGroup1];
    await checkSelectedDisplay([0], [0]);

    component.displays = [
      {displayId: 20, groupId: 2, name: 'Display 1', isActive: false},
      {displayId: 10, groupId: 1, name: 'Display 0', isActive: false},
    ];
    await checkSelectedDisplay([0], [1], false);
  });

  it('updates scene on separation slider change', () => {
    component.rects = [rectGroup0, rectGroup0];
    fixture.detectChanges();
    const boundingBox = updateViewPositionSpy.calls.mostRecent().args[1];
    updateSeparationSlider();

    checkAllSpiesCalled(2);
    expect(updateViewPositionSpy.calls.mostRecent().args[1]).toEqual(
      boundingBox,
    );
    const rectsBefore = assertDefined(updateRectsSpy.calls.first().args[0]);
    const rectsAfter = assertDefined(updateRectsSpy.calls.mostRecent().args[0]);

    expect(rectsBefore[0].topLeft.z).toEqual(200);
    expect(rectsAfter[0].topLeft.z).toEqual(12);
  });

  it('updates scene on rotation slider change', () => {
    component.rects = [rectGroup0];
    fixture.detectChanges();
    const boundingBox = updateViewPositionSpy.calls.mostRecent().args[1];
    updateRotationSlider();

    expect(updateViewPositionSpy).toHaveBeenCalledTimes(2);
    expect(updateRectsSpy).toHaveBeenCalledTimes(1);
    expect(updateLabelsSpy).toHaveBeenCalledTimes(2);
    expect(renderViewSpy).toHaveBeenCalledTimes(2);
    expect(updateViewPositionSpy.calls.mostRecent().args[1]).toEqual(
      boundingBox,
    );

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
    const boundingBox = updateViewPositionSpy.calls.mostRecent().args[1];

    updateShadingMode(ShadingMode.GRADIENT, ShadingMode.WIRE_FRAME);
    updateShadingMode(ShadingMode.WIRE_FRAME, ShadingMode.OPACITY);

    expect(updateViewPositionSpy).toHaveBeenCalledTimes(1);
    expect(updateRectsSpy).toHaveBeenCalledTimes(3);
    expect(updateLabelsSpy).toHaveBeenCalledTimes(1);
    expect(renderViewSpy).toHaveBeenCalledTimes(3);
    expect(updateViewPositionSpy.calls.mostRecent().args[1]).toEqual(
      boundingBox,
    );

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

    updateShadingMode(ShadingMode.OPACITY, ShadingMode.GRADIENT); // cycles back to original
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

  it('uses stored selected displays if present in new trace', async () => {
    component.rects = [rectGroup0, rectGroup1];
    component.displays = [
      {displayId: 10, groupId: 0, name: 'Display 0', isActive: true},
      {displayId: 20, groupId: 1, name: 'Display 1', isActive: true},
    ];
    await checkSelectedDisplay([0], [0]);

    openDisplaysSelect();
    const options = getDisplayOptions();
    options.item(1).click();
    await checkSelectedDisplay([0, 1], [0, 1]);

    const fixtureWithSameDisplays = TestBed.createComponent(TestHostComponent);
    const componentWithSameDisplays = fixtureWithSameDisplays.componentInstance;
    componentWithSameDisplays.rects = component.rects;
    componentWithSameDisplays.displays = component.displays;
    await checkSelectedDisplay(
      [0, 1],
      [0, 1],
      false,
      fixtureWithSameDisplays,
      fixtureWithSameDisplays.nativeElement,
    );

    const fixtureWithDisplay1 = TestBed.createComponent(TestHostComponent);
    const componentWithDisplay1 = fixtureWithDisplay1.componentInstance;
    componentWithDisplay1.rects = [rectGroup1];
    componentWithDisplay1.displays = [
      {displayId: 20, groupId: 1, name: 'Display 1', isActive: true},
    ];
    await checkSelectedDisplay(
      [1],
      [1],
      false,
      fixtureWithDisplay1,
      fixtureWithDisplay1.nativeElement,
    );
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
    await checkSelectedDisplay([1], [1]);
  });

  it('handles change from zero to one display and back to zero', async () => {
    component.displays = [];
    component.rects = [];
    await checkSelectedDisplay([], []);
    const placeholder = assertDefined(
      htmlElement.querySelector('.placeholder-text'),
    );
    expect(placeholder.textContent?.trim()).toEqual('No rects found.');

    component.rects = [rectGroup0];
    component.displays = [
      {displayId: 10, groupId: 0, name: 'Display 0', isActive: false},
    ];
    await checkSelectedDisplay([0], [0], true);

    component.displays = [];
    component.rects = [];
    await checkSelectedDisplay([], []);
  });

  it('handles current display group id no longer present', async () => {
    component.rects = [rectGroup0];
    component.displays = [
      {displayId: 10, groupId: 0, name: 'Display 0', isActive: false},
    ];
    await checkSelectedDisplay([0], [0]);

    component.rects = [rectGroup1];
    component.displays = [
      {displayId: 20, groupId: 1, name: 'Display 1', isActive: false},
    ];
    await checkSelectedDisplay([1], [1]);
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

  it('redraws mini rects on change', () => {
    component.miniRects = [rectGroup0, rectGroup0];
    fixture.detectChanges();
    resetSpies();

    component.miniRects = [rectGroup0, rectGroup0];
    fixture.detectChanges();
    checkAllSpiesCalled(1);
  });

  it('handles collapse button click', () => {
    fixture.detectChanges();
    const spy = spyOn(
      assertDefined(component.rectsComponent).collapseButtonClicked,
      'emit',
    );
    findAndClickElement('collapsible-section-title button');
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
    let id: string | undefined;
    htmlElement.addEventListener(ViewerEvents.HighlightedIdChange, (event) => {
      id = (event as CustomEvent).detail.id;
    });

    const spy = spyOn(Canvas.prototype, 'getClickedRectId').and.returnValue(
      undefined,
    );
    clickLargeRectsCanvas();
    expect(id).toBeUndefined();
    spy.and.returnValue(testString);
    clickLargeRectsCanvas();
    expect(id).toEqual(testString);
  });

  it('pans view without emitting rect id', () => {
    component.rects = [rectGroup0];
    fixture.detectChanges();
    const cameraBefore = updateViewPositionSpy.calls.mostRecent().args[0];
    expect(cameraBefore.panScreenDistance.dx).toEqual(0);
    expect(cameraBefore.panScreenDistance.dy).toEqual(0);
    const boundingBoxBefore = updateViewPositionSpy.calls.mostRecent().args[1];
    resetSpies();

    const testString = 'test_id';
    spyOn(Canvas.prototype, 'getClickedRectId').and.returnValue(testString);
    let id: string | undefined;
    htmlElement.addEventListener(ViewerEvents.HighlightedIdChange, (event) => {
      id = (event as CustomEvent).detail.id;
    });

    panView();
    expect(updateViewPositionSpy).toHaveBeenCalledTimes(1);
    expect(updateRectsSpy).not.toHaveBeenCalled();
    expect(updateLabelsSpy).not.toHaveBeenCalled();
    expect(renderViewSpy).toHaveBeenCalled();

    const [cameraAfter, boundingBoxAfter] =
      updateViewPositionSpy.calls.mostRecent().args;
    expect(cameraAfter.panScreenDistance.dx).toEqual(5);
    expect(cameraAfter.panScreenDistance.dy).toEqual(10);
    expect(boundingBoxAfter).toEqual(boundingBoxBefore);

    clickLargeRectsCanvas();
    expect(id).toBeUndefined();

    clickLargeRectsCanvas();
    expect(id).toEqual(testString);
  });

  it('handles window resize', async () => {
    component.rects = [rectGroup0];
    fixture.detectChanges();
    const boundingBox = updateViewPositionSpy.calls.mostRecent().args[1];
    resetSpies();

    spyOnProperty(window, 'innerWidth').and.returnValue(window.innerWidth / 2);
    window.dispatchEvent(new Event('resize'));
    fixture.detectChanges();
    await fixture.whenStable();
    await waitToBeCalled(renderViewSpy, 1);
    expect(updateViewPositionSpy).toHaveBeenCalledTimes(1);
    expect(updateRectsSpy).not.toHaveBeenCalled();
    expect(updateLabelsSpy).not.toHaveBeenCalled();
    expect(updateViewPositionSpy.calls.mostRecent().args[1]).toEqual(
      boundingBox,
    );
  });

  it('handles change in dark mode', async () => {
    component.rects = [rectGroup0];
    component.miniRects = [rectGroup0];
    fixture.detectChanges();
    resetSpies();

    component.isDarkMode = true;
    fixture.detectChanges();
    expect(updateRectsSpy).toHaveBeenCalledTimes(2);
    expect(updateLabelsSpy).toHaveBeenCalledTimes(2);
    expect(updateViewPositionSpy).toHaveBeenCalledTimes(1); // only for mini rects
    expect(renderViewSpy).toHaveBeenCalledTimes(2);
  });

  it('handles zoom button clicks', () => {
    component.rects = [rectGroup0];
    fixture.detectChanges();
    const boundingBox = updateViewPositionSpy.calls.mostRecent().args[1];
    const zoomFactor =
      updateViewPositionSpy.calls.mostRecent().args[0].zoomFactor;
    resetSpies();

    clickZoomInButton();
    checkZoomedIn(zoomFactor);
    const zoomedInFactor =
      updateViewPositionSpy.calls.mostRecent().args[0].zoomFactor;
    expect(updateViewPositionSpy.calls.mostRecent().args[1]).toEqual(
      boundingBox,
    );
    resetSpies();

    findAndClickElement('.zoom-out-button');
    checkZoomedOut(zoomedInFactor);
    expect(updateViewPositionSpy.calls.mostRecent().args[1]).toEqual(
      boundingBox,
    );
  });

  it('handles zoom change via scroll event', () => {
    component.rects = [rectGroup0];
    fixture.detectChanges();
    const zoomFactor =
      updateViewPositionSpy.calls.mostRecent().args[0].zoomFactor;
    resetSpies();

    const rectsElement = assertDefined(htmlElement.querySelector('rects-view'));

    const zoomInEvent = new WheelEvent('wheel');
    Object.defineProperty(zoomInEvent, 'target', {
      value: htmlElement.querySelector('.large-rects-canvas'),
    });
    Object.defineProperty(zoomInEvent, 'deltaY', {value: 0});
    rectsElement.dispatchEvent(zoomInEvent);
    fixture.detectChanges();

    checkZoomedIn(zoomFactor);
    const zoomedInFactor =
      updateViewPositionSpy.calls.mostRecent().args[0].zoomFactor;
    resetSpies();

    const zoomOutEvent = new WheelEvent('wheel');
    Object.defineProperty(zoomOutEvent, 'target', {
      value: htmlElement.querySelector('.large-rects-canvas'),
    });
    Object.defineProperty(zoomOutEvent, 'deltaY', {value: 1});
    rectsElement.dispatchEvent(zoomOutEvent);
    fixture.detectChanges();
    checkZoomedOut(zoomedInFactor);
  });

  it('handles reset button click', () => {
    component.rects = [rectGroup0];
    fixture.detectChanges();
    const [camera, boundingBox] = updateViewPositionSpy.calls.mostRecent().args;

    updateRotationSlider();
    updateSeparationSlider();
    clickZoomInButton();
    panView();
    resetSpies();

    findAndClickElement('.reset-button');
    checkAllSpiesCalled(1);
    const [newCamera, newBoundingBox] =
      updateViewPositionSpy.calls.mostRecent().args;
    expect(newCamera).toEqual(camera);
    expect(newBoundingBox).toEqual(boundingBox);
  });

  it('handles change in highlighted item', () => {
    component.rects = [rectGroup0];
    fixture.detectChanges();
    expect(updateRectsSpy.calls.mostRecent().args[0][0].colorType).toEqual(
      ColorType.VISIBLE,
    );
    resetSpies();

    component.highlightedItem = rectGroup0.id;
    fixture.detectChanges();

    expect(updateViewPositionSpy).not.toHaveBeenCalled();
    expect(updateRectsSpy).toHaveBeenCalledTimes(1);
    expect(updateLabelsSpy).toHaveBeenCalledTimes(1);
    expect(renderViewSpy).toHaveBeenCalledTimes(1);
    expect(updateRectsSpy.calls.mostRecent().args[0][0].colorType).toEqual(
      ColorType.HIGHLIGHTED,
    );
  });

  it('handles rect double click', () => {
    component.rects = [rectGroup0];
    fixture.detectChanges();
    resetSpies();

    const testString = 'test_id';
    const spy = spyOn(Canvas.prototype, 'getClickedRectId').and.returnValue(
      undefined,
    );
    let detail: RectDblClickDetail | undefined;
    htmlElement.addEventListener(ViewerEvents.RectsDblClick, (event) => {
      detail = (event as CustomEvent).detail;
    });

    const canvas = assertDefined(
      htmlElement.querySelector<HTMLElement>('.large-rects-canvas'),
    );
    canvas.dispatchEvent(new MouseEvent('dblclick'));
    fixture.detectChanges();
    expect(detail).toBeUndefined();
    spy.and.returnValue(testString);

    canvas.dispatchEvent(new MouseEvent('dblclick'));
    fixture.detectChanges();
    expect(detail).toEqual(new RectDblClickDetail(testString));
  });

  it('handles mini rect double click', () => {
    component.rects = [rectGroup0];
    fixture.detectChanges();
    resetSpies();

    let miniRectDoubleClick = false;
    htmlElement.addEventListener(ViewerEvents.MiniRectsDblClick, (event) => {
      miniRectDoubleClick = true;
    });

    const canvas = assertDefined(
      htmlElement.querySelector<HTMLElement>('.mini-rects-canvas'),
    );
    canvas.dispatchEvent(new MouseEvent('dblclick'));
    fixture.detectChanges();
    expect(miniRectDoubleClick).toBeTrue();
  });

  it('does not render more that selected label if over 30 rects', () => {
    component.rects = Array.from({length: 30}, () => rectGroup0);
    fixture.detectChanges();
    expect(updateLabelsSpy.calls.mostRecent().args[0].length).toEqual(30);

    const newRect = makeRectWithGroupId(0, true, 'new rect');
    component.rects = component.rects.concat([newRect]);
    fixture.detectChanges();
    expect(updateLabelsSpy.calls.mostRecent().args[0].length).toEqual(0);

    component.highlightedItem = newRect.id;
    fixture.detectChanges();
    expect(updateLabelsSpy.calls.mostRecent().args[0].length).toEqual(1);
  });

  it('does not render more that selected label if multiple group ids', async () => {
    component.rects = [rectGroup0];
    component.displays = [
      {displayId: 0, groupId: 0, name: 'Display 0', isActive: false},
      {displayId: 1, groupId: 1, name: 'Display 1', isActive: false},
    ];
    fixture.detectChanges();
    await checkSelectedDisplay([0], [0]);
    expect(updateLabelsSpy.calls.mostRecent().args[0].length).toEqual(1);

    component.rects = component.rects.concat([rectGroup1]);
    fixture.detectChanges();
    openDisplaysSelect();
    getDisplayOptions().item(1).click();
    await checkSelectedDisplay([0, 1], [0, 1], true);

    expect(updateLabelsSpy.calls.mostRecent().args[0].length).toEqual(0);

    component.highlightedItem = rectGroup0.id;
    fixture.detectChanges();
    expect(updateLabelsSpy.calls.mostRecent().args[0].length).toEqual(1);
  });

  it('handles rect type button click', async () => {
    let clicks = 0;
    htmlElement.addEventListener(ViewerEvents.RectTypeButtonClick, () => {
      clicks++;
    });
    expect(htmlElement.querySelector('.rect-type')).toBeNull();
    component.rectType = {type: RectType.LAYERS, icon: 'layers'};
    fixture.detectChanges();
    const button = assertDefined(
      htmlElement.querySelector<HTMLElement>('.rect-type'),
    );
    expect(button.textContent?.trim()).toEqual('layers');
    UnitTestUtils.checkTooltips([button], ['Showing layers'], fixture);
    button.click();
    fixture.detectChanges();
    expect(clicks).toEqual(1);
  });

  it('shows warning for any rect type set after the first', async () => {
    component.rectType = {type: RectType.LAYERS, icon: 'layers'};
    fixture.detectChanges();
    expect(htmlElement.querySelector('.warning')).toBeNull();

    component.rectType = {type: RectType.INPUT_WINDOWS, icon: 'touch_app'};
    fixture.detectChanges();
    const warning = assertDefined(htmlElement.querySelector('.warning'));
    expect(
      warning.querySelector('.warning-message')?.textContent?.trim(),
    ).toEqual(
      'Showing input windows - change type by clicking  touch_app  icon above',
    );

    component.rectType = {type: RectType.LAYERS, icon: 'layers'};
    fixture.detectChanges();
    expect(htmlElement.querySelector('.warning')).toBeNull();
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
    testIds: number[],
    changeInBoundingBox?: boolean,
    f = fixture,
    el = htmlElement,
  ) {
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    const displaySelect = assertDefined(el.querySelector('.displays-select'));
    expect(displaySelect.textContent?.trim()).toEqual(
      displayNumbers
        .map((displayNumber) => `Display ${displayNumber}`)
        .join(', '),
    );
    const drawnRects = updateRectsSpy.calls.mostRecent().args[0];
    expect(drawnRects.length).toEqual(displayNumbers.length);
    drawnRects.forEach((rect, index) => {
      expect(rect.id).toEqual(`test-id ${testIds[index]}`);
      if (index > 0) expect(rect.transform.ty).toBeGreaterThan(0);
    });
    if (changeInBoundingBox) {
      expect(updateViewPositionSpy.calls.mostRecent().args[1]).not.toEqual(
        updateViewPositionSpy.calls.argsFor(
          updateViewPositionSpy.calls.count() - 2,
        )[1],
      );
    }
  }

  function findAndClickElement(selector: string) {
    const el = assertDefined(htmlElement.querySelector<HTMLElement>(selector));
    el.click();
    fixture.detectChanges();
  }

  function checkSliderUnfocusesOnClick(slider: Element, expectedValue: number) {
    const rectsComponent = assertDefined(component.rectsComponent);
    slider.dispatchEvent(new MouseEvent('mousedown'));
    slider.dispatchEvent(new MouseEvent('mouseup'));
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

  function makeRectWithGroupId(
    groupId: number,
    isVisible = true,
    id?: string,
  ): UiRect {
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
      .setId(id ?? 'test-id ' + groupId)
      .setGroupId(groupId)
      .setIsClickable(true)
      .setCornerRadius(0)
      .setDepth(0)
      .setOpacity(0.5)
      .build();
  }

  function panView() {
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
  }

  function clickZoomInButton() {
    findAndClickElement('.zoom-in-button');
  }

  function clickLargeRectsCanvas() {
    findAndClickElement('.large-rects-canvas');
  }

  function checkZoomedIn(oldZoomFactor: number) {
    expect(updateRectsSpy).toHaveBeenCalledTimes(0);
    expect(updateLabelsSpy).toHaveBeenCalledTimes(1);
    expect(updateViewPositionSpy).toHaveBeenCalledTimes(1);
    expect(renderViewSpy).toHaveBeenCalledTimes(1);
    expect(
      updateViewPositionSpy.calls.mostRecent().args[0].zoomFactor,
    ).toBeGreaterThan(oldZoomFactor);
  }

  function checkZoomedOut(oldZoomFactor: number) {
    expect(updateRectsSpy).toHaveBeenCalledTimes(0);
    expect(updateLabelsSpy).toHaveBeenCalledTimes(1);
    expect(updateViewPositionSpy).toHaveBeenCalledTimes(1);
    expect(renderViewSpy).toHaveBeenCalledTimes(1);
    expect(
      updateViewPositionSpy.calls.mostRecent().args[0].zoomFactor,
    ).toBeLessThan(oldZoomFactor);
  }

  function openDisplaysSelect() {
    findAndClickElement('.displays-section .mat-select-trigger');
  }

  function getDisplayOptions() {
    return document.querySelectorAll<HTMLElement>(
      '.mat-select-panel .mat-option',
    );
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
        [pinnedItems]="pinnedItems"
        [isDarkMode]="isDarkMode"
        [highlightedItem]="highlightedItem"
        [rectType]="rectType"></rects-view>
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
    isDarkMode = false;
    highlightedItem = '';
    rectType: UiRectType | undefined;

    @ViewChild(RectsComponent)
    rectsComponent: RectsComponent | undefined;
  }
});
