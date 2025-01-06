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
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {MatDividerModule} from '@angular/material/divider';
import {MatIconModule} from '@angular/material/icon';
import {MatTooltipModule} from '@angular/material/tooltip';
import {assertDefined} from 'common/assert_utils';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {EMPTY_OBJ_STRING} from 'trace/tree_node/formatters';
import {SfCuratedProperties} from 'viewers/common/curated_properties';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {CollapsibleSectionTitleComponent} from './collapsible_section_title_component';
import {SurfaceFlingerPropertyGroupsComponent} from './surface_flinger_property_groups_component';
import {TransformMatrixComponent} from './transform_matrix_component';

describe('SurfaceFlingerPropertyGroupsComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;

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
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders flags', () => {
    const flags = assertDefined(htmlElement.querySelector('.flags'));
    expect(flags.innerHTML).toMatch('Flags:.*HIDDEN \\(0x1\\)');
  });

  it('renders simple summary property', () => {
    const summary = htmlElement.querySelectorAll('.summary').item(0);
    expect(summary.textContent).toMatch(
      'Invisible due to: reason 1, reason 2, reason 3',
    );
  });

  it('renders interactive summary property', () => {
    const summary = htmlElement.querySelectorAll('.summary').item(1);
    expect(summary.textContent).toContain('Covered by:');
    const button = assertDefined(summary.querySelector<HTMLElement>('button'));
    expect(button.textContent).toMatch('1');
  });

  it('emits highlighted id event from layer id in summary', () => {
    checkHighlightedIdEventEmittedFromButtonClick(
      '.summary button',
      '1 coveringLayer',
    );
  });

  it('displays calculated geometry', () => {
    const calculatedDiv = assertDefined(
      htmlElement.querySelector('.geometry .left-column'),
    );
    expect(calculatedDiv.querySelector('transform-matrix')).toBeTruthy();
    expect(
      assertDefined(calculatedDiv.querySelector('.crop')).innerHTML,
    ).toContain(EMPTY_OBJ_STRING);
    expect(
      assertDefined(calculatedDiv.querySelector('.final-bounds')).innerHTML,
    ).toContain(EMPTY_OBJ_STRING);
  });

  it('displays requested geometry', () => {
    const requestedDiv = assertDefined(
      htmlElement.querySelector('.geometry .right-column'),
    );
    expect(requestedDiv.querySelector('transform-matrix')).toBeTruthy();
  });

  it('displays buffer info', () => {
    const sizeDiv = htmlElement.querySelector('.buffer .size');
    expect(assertDefined(sizeDiv).innerHTML).toContain(EMPTY_OBJ_STRING);
    const currFrameDiv = htmlElement.querySelector('.buffer .frame-number');
    expect(assertDefined(currFrameDiv).innerHTML).toContain('0');
    const transformDiv = htmlElement.querySelector('.buffer .transform');
    expect(assertDefined(transformDiv).innerHTML).toContain('IDENTITY');
    const destFrameDiv = htmlElement.querySelector('.buffer .dest-frame');
    expect(assertDefined(destFrameDiv).innerHTML).toContain(EMPTY_OBJ_STRING);
    const ignoreFrameDiv = htmlElement.querySelector('.buffer .ignore-frame');
    expect(assertDefined(ignoreFrameDiv).innerHTML).toContain(
      'Destination Frame ignored because item has eIgnoreDestinationFrame flag set.',
    );
  });

  it('displays hierarchy info', () => {
    const zDiv = htmlElement.querySelector('.hierarchy-info .z-order');
    expect(assertDefined(zDiv).innerHTML).toContain('0');
    const relParentDiv = htmlElement.querySelector(
      '.hierarchy-info .rel-parent',
    );
    expect(assertDefined(relParentDiv).innerHTML).toContain('none');
    const relChildrenDiv = assertDefined(
      htmlElement.querySelector('.hierarchy-info .rel-children'),
    );
    expect(relChildrenDiv.innerHTML).toContain('none');
  });

  it('emits highlighted id event from layer id in rel z parent', () => {
    component.properties.relativeParent = {
      layerId: '1',
      nodeId: '1 relZParent',
      name: 'relZParent',
    };
    fixture.detectChanges();
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
    fixture.detectChanges();
    checkHighlightedIdEventEmittedFromButtonClick(
      '.hierarchy-info .rel-children button',
      '2 relZChild',
    );
  });

  it('displays simple calculated effects', () => {
    const calculatedDiv = assertDefined(
      htmlElement.querySelector('.effects .left-column'),
    );
    expect(
      assertDefined(calculatedDiv.querySelector('.shadow')).innerHTML,
    ).toContain('1 px');
    expect(
      assertDefined(calculatedDiv.querySelector('.blur')).innerHTML,
    ).toContain('1 px');
    expect(
      assertDefined(calculatedDiv.querySelector('.corner-radius')).innerHTML,
    ).toContain('1 px');
  });

  it('displays simple requested effects', () => {
    const calculatedDiv = assertDefined(
      htmlElement.querySelector('.effects .right-column'),
    );
    expect(
      assertDefined(calculatedDiv.querySelector('.corner-radius')).innerHTML,
    ).toContain('1 px');
  });

  it('displays color and alpha value in effects', () => {
    const colorDiv = htmlElement.querySelector('.color');
    expect(assertDefined(colorDiv).innerHTML).toContain(
      `${EMPTY_OBJ_STRING}, alpha: 1`,
    );
  });

  it('displays not set message if no inputs present', () => {
    const noInputsMessage = htmlElement.querySelector('.inputs .left-column');
    expect(assertDefined(noInputsMessage).innerHTML).toContain('not set');
  });

  it('displays input window info if available', () => {
    component.properties.hasInputChannel = true;
    fixture.detectChanges();

    expect(
      htmlElement.querySelector('.inputs .left-column transform-matrix'),
    ).toBeTruthy();

    const configDiv = assertDefined(
      htmlElement.querySelector('.inputs .right-column'),
    );
    expect(
      assertDefined(configDiv.querySelector('.focusable')).innerHTML,
    ).toContain('false');
    expect(
      assertDefined(configDiv.querySelector('.crop-touch-region')).innerHTML,
    ).toContain('none');
    expect(
      assertDefined(configDiv.querySelector('.replace-touch-region')).innerHTML,
    ).toContain('false');
    expect(
      assertDefined(configDiv.querySelector('.input-config')).innerHTML,
    ).toContain('null');
  });

  it('handles collapse button click', () => {
    expect(component.collapseButtonClicked).toBeFalse();
    const collapseButton = assertDefined(
      htmlElement.querySelector<HTMLElement>(
        'collapsible-section-title button',
      ),
    );
    collapseButton.click();
    fixture.detectChanges();
    expect(component.collapseButtonClicked).toBeTrue();
  });

  function checkHighlightedIdEventEmittedFromButtonClick(
    selector: string,
    expectedId: string,
  ) {
    let id = '';
    htmlElement.addEventListener(ViewerEvents.HighlightedIdChange, (event) => {
      id = (event as CustomEvent).detail.id;
    });
    const button = assertDefined(
      htmlElement.querySelector<HTMLElement>(selector),
    );
    button.click();
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
