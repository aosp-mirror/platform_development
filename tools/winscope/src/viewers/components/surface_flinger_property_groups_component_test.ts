/*
 * Copyright (C) 2024 The Android Open Source Project
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
import {Component} from '@angular/core';
import {ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {MatDividerModule} from '@angular/material/divider';
import {MatIconModule} from '@angular/material/icon';
import {MatTooltipModule} from '@angular/material/tooltip';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {EMPTY_OBJ_STRING} from 'trace/tree_node/formatters';
import {SfCuratedProperties} from 'viewers/common/curated_properties';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {CollapsibleSectionTitleComponent} from './collapsible_section_title_component';
import {SurfaceFlingerPropertyGroupsComponent} from './surface_flinger_property_groups_component';
import {TransformMatrixComponent} from './transform_matrix_component';

describe('SurfaceFlingerPropertyGroupsComponent', () => {
  let component: TestHostComponent;
  let dom: DOMTestHelper<TestHostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      imports: [MatDividerModule, MatTooltipModule, MatIconModule],
      declarations: [
        TestHostComponent,
        SurfaceFlingerPropertyGroupsComponent,
        TransformMatrixComponent,
        CollapsibleSectionTitleComponent,
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    dom.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders flags', () => {
    dom.get('.flags').checkTextExact('Flags: HIDDEN (0x1)');
  });

  it('renders simple summary property', () => {
    dom
      .get('.summary')
      .checkTextExact('Invisible due to: reason 1, reason 2, reason 3');
  });

  it('renders interactive summary property', () => {
    const summary = dom.findAll('.summary')[1];
    summary.checkTextExact('Covered by:  1');
    summary.get('button').checkTextExact('1');
  });

  it('emits highlighted id event from layer id in summary', () => {
    checkHighlightedIdEventEmittedFromButtonClick(
      '.summary button',
      '1 coveringLayer',
    );
  });

  it('displays calculated geometry', () => {
    const calculatedDiv = dom.get('.geometry .left-column');
    expect(calculatedDiv.find('transform-matrix')).toBeDefined();
    calculatedDiv.get('.crop').checkTextExact('Crop: ' + EMPTY_OBJ_STRING);
    calculatedDiv
      .get('.final-bounds')
      .checkTextExact('Final Bounds: ' + EMPTY_OBJ_STRING);
  });

  it('displays requested geometry', () => {
    const requestedDiv = dom.get('.geometry .right-column');
    expect(requestedDiv.find('transform-matrix')).toBeDefined();
  });

  it('displays buffer info', () => {
    dom.get('.buffer .size').checkTextExact('Size: ' + EMPTY_OBJ_STRING);
    dom.get('.buffer .frame-number').checkTextExact('Frame Number: 0');
    dom.get('.buffer .transform').checkTextExact('Transform: IDENTITY');
    dom
      .get('.buffer .dest-frame')
      .checkTextExact('Destination Frame: ' + EMPTY_OBJ_STRING);
    dom
      .get('.buffer .ignore-frame')
      .checkTextExact(
        'Destination Frame ignored because item has eIgnoreDestinationFrame flag set.',
      );
  });

  it('displays hierarchy info', () => {
    dom.get('.hierarchy-info .z-order').checkTextExact('Z-order: 0');
    dom
      .get('.hierarchy-info .rel-parent')
      .checkTextExact('Relative Parent:  none');
    dom
      .get('.hierarchy-info .rel-children')
      .checkTextExact('Relative Children:  none');
  });

  it('emits highlighted id event from layer id in rel z parent', () => {
    component.properties.relativeParent = {
      layerId: '1',
      nodeId: '1 relZParent',
      name: 'relZParent',
    };
    dom.detectChanges();
    checkHighlightedIdEventEmittedFromButtonClick(
      '.hierarchy-info .rel-parent button',
      '1 relZParent',
    );
  });

  it('emits highlighted id event from layer id in rel z children', () => {
    component.properties.relativeChildren = [
      {
        layerId: '2',
        nodeId: '2 relZChild',
        name: 'relZChild',
      },
    ];
    dom.detectChanges();
    checkHighlightedIdEventEmittedFromButtonClick(
      '.hierarchy-info .rel-children button',
      '2 relZChild',
    );
  });

  it('displays simple calculated effects', () => {
    const calculatedDiv = dom.get('.effects .left-column');
    calculatedDiv.get('.shadow').checkTextExact('Shadow Radius: 1 px');
    calculatedDiv.get('.blur').checkTextExact('Blur Radius: 1 px');
    calculatedDiv.get('.corner-radius').checkTextExact('Corner Radius: 1 px');
  });

  it('displays simple requested effects', () => {
    const requestedDiv = dom.get('.effects .right-column');
    requestedDiv.get('.corner-radius').checkTextExact('Corner Radius: 1 px');
  });

  it('displays color and alpha value in effects', () => {
    dom.get('.color').checkTextExact(`Color: ${EMPTY_OBJ_STRING}, alpha: 1`);
  });

  it('displays not set message if no inputs present', () => {
    dom.get('.inputs .left-column').checkTextExact('Input Channel: not set');
  });

  it('displays input window info if available', () => {
    component.properties.hasInputChannel = true;
    dom.detectChanges();

    expect(dom.find('.inputs .left-column transform-matrix')).toBeDefined();

    const configDiv = dom.get('.inputs .right-column');
    configDiv.get('.focusable').checkTextExact('Focusable: false');
    configDiv
      .get('.crop-touch-region')
      .checkTextExact('Crop touch region with item: none');
    configDiv
      .get('.replace-touch-region')
      .checkTextExact('Replace touch region with crop: false');
    configDiv.get('.input-config').checkTextExact('Input Config: null');
  });

  it('handles collapse button click', () => {
    expect(component.collapseButtonClicked).toBeFalse();
    dom.findAndClick('collapsible-section-title button');
    expect(component.collapseButtonClicked).toBeTrue();
  });

  function checkHighlightedIdEventEmittedFromButtonClick(
    selector: string,
    expectedId: string,
  ) {
    let id = '';
    dom.addEventListener(ViewerEvents.HighlightedIdChange, (event) => {
      id = (event as CustomEvent).detail.id;
    });
    dom.findAndClick(selector);
    expect(id).toEqual(expectedId);
  }

  @Component({
    selector: 'host-component',
    template: `
      <surface-flinger-property-groups
        [properties]="properties"
        (collapseButtonClicked)="onCollapseButtonClick()"></surface-flinger-property-groups>
    `,
  })
  class TestHostComponent {
    transformNode = TreeNodeUtils.makeUiPropertyNode('transform', 'transform', {
      type: 0,
      matrix: {
        dsdx: 1,
        dsdy: 0,
        dtdx: 0,
        dtdy: 1,
        tx: 0,
        ty: 0,
      },
    });

    properties: SfCuratedProperties = {
      summary: [
        {key: 'Invisible due to', simpleValue: 'reason 1, reason 2, reason 3'},
        {
          key: 'Covered by',
          layerValues: [
            {
              layerId: '1',
              nodeId: '1 coveringLayer',
              name: 'coveringLayer',
            },
          ],
        },
      ],
      flags: 'HIDDEN (0x1)',
      calcTransform: this.transformNode,
      calcCrop: EMPTY_OBJ_STRING,
      finalBounds: EMPTY_OBJ_STRING,
      reqTransform: this.transformNode,
      bufferSize: EMPTY_OBJ_STRING,
      frameNumber: '0',
      bufferTransformType: 'IDENTITY',
      destinationFrame: EMPTY_OBJ_STRING,
      z: '0',
      relativeParent: 'none',
      relativeChildren: [],
      calcColor: `${EMPTY_OBJ_STRING}, alpha: 1`,
      calcShadowRadius: '1 px',
      calcCornerRadius: '1 px',
      calcCornerRadiusCrop: EMPTY_OBJ_STRING,
      backgroundBlurRadius: '1 px',
      reqColor: `${EMPTY_OBJ_STRING}, alpha: 1`,
      reqCornerRadius: '1 px',
      inputTransform: this.transformNode,
      inputRegion: 'null',
      focusable: 'false',
      cropTouchRegionWithItem: 'none',
      replaceTouchRegionWithCrop: 'false',
      inputConfig: 'null',
      hasInputChannel: false,
      ignoreDestinationFrame: true,
    };

    collapseButtonClicked = false;

    onCollapseButtonClick() {
      this.collapseButtonClicked = true;
    }
  }
});
