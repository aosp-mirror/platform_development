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
import {Component} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatDividerModule} from '@angular/material/divider';
import {assertDefined} from 'common/assert_utils';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {ImeAdditionalProperties} from 'viewers/common/ime_additional_properties';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {CoordinatesTableComponent} from './coordinates_table_component';
import {ImeAdditionalPropertiesComponent} from './ime_additional_properties_component';

describe('ImeAdditionalPropertiesComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatDividerModule],
      declarations: [
        ImeAdditionalPropertiesComponent,
        TestHostComponent,
        CoordinatesTableComponent,
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    htmlElement.addEventListener(
      ViewerEvents.HighlightedChange,
      component.onHighlightedChange,
    );
    htmlElement.addEventListener(
      ViewerEvents.AdditionalPropertySelected,
      component.onAdditionalPropertySelectedChange,
    );
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('shows client or service sf properties', () => {
    expect(htmlElement.querySelector('.ime-container')).toBeDefined();
    expect(htmlElement.querySelector('.input-method-surface')).toBeDefined();
  });

  it('emits update additional property tree event on wm state button click', () => {
    const button = assertDefined(
      htmlElement.querySelector('.wm-state-button'),
    ) as HTMLButtonElement;
    expect(button.className).not.toContain('selected');
    button.click();
    fixture.detectChanges();
    expect(component.additionalPropertieTreeName).toEqual(
      'Window Manager State',
    );
    expect(button.className).toContain('selected');
  });

  it('propagates new ime container layer on button click', () => {
    const button = assertDefined(
      htmlElement.querySelector('.ime-container-button'),
    ) as HTMLButtonElement;
    expect(button.className).not.toContain('selected');
    button.click();
    fixture.detectChanges();
    expect(component.highlightedItem).toEqual('123');
    expect(button.className).toContain('selected');
  });

  it('propagates new input method surface layer on button click', () => {
    const button = assertDefined(
      htmlElement.querySelector('.input-method-surface-button'),
    ) as HTMLButtonElement;
    expect(button.className).not.toContain('selected');
    button.click();
    fixture.detectChanges();
    expect(component.highlightedItem).toEqual('456');
    expect(button.className).toContain('selected');
  });

  it('shows ime manager service wm properties', () => {
    component.isImeManagerService = true;
    fixture.detectChanges();
    const imeManagerService = assertDefined(
      htmlElement.querySelector('.ime-manager-service'),
    );
    expect(
      assertDefined(imeManagerService.querySelector('.wm-state')).textContent,
    ).toContain('1970-01-01T00:00:00.000000000');
    expect(
      imeManagerService.querySelector('.ime-control-target-button'),
    ).toBeDefined();
  });

  it('propagates new property tree node window on button click', () => {
    component.isImeManagerService = true;
    fixture.detectChanges();
    const button = assertDefined(
      htmlElement.querySelector('.ime-control-target-button'),
    ) as HTMLButtonElement;
    expect(button.className).not.toContain('selected');
    button.click();
    fixture.detectChanges();
    expect(component.additionalPropertieTreeName).toEqual('Ime Control Target');
    expect(button.className).toContain('selected');
  });

  @Component({
    selector: 'host-component',
    template: `
      <ime-additional-properties
        [highlightedItem]="highlightedItem"
        [isImeManagerService]="isImeManagerService"
        [additionalProperties]="additionalProperties"></ime-additional-properties>
    `,
  })
  class TestHostComponent {
    isImeManagerService = false;

    additionalProperties = new ImeAdditionalProperties(
      {
        id: 'wmStateId',
        name: 'wmState',
        wmStateProperties: {
          timestamp: '1970-01-01T00:00:00.000000000',
          focusedApp: 'exampleFocusedApp',
          focusedWindow: undefined,
          focusedActivity: undefined,
          isInputMethodWindowVisible: false,
          imeControlTarget: TreeNodeUtils.makePropertyNode(
            'DisplayContent.inputMethodControlTarget',
            'inputMethodControlTarget',
            null,
          ),
          imeInputTarget: undefined,
          imeLayeringTarget: undefined,
          imeInsetsSourceProvider: undefined,
        },
        hierarchyTree: TreeNodeUtils.makeHierarchyNode({name: 'wmStateProto'}),
      },
      {
        id: 'ime',
        name: 'imeLayers',
        properties: {
          imeContainer: {id: '123', zOrderRelativeOfId: -1, z: 0},
          inputMethodSurface: {id: '456', isVisible: false},
          focusedWindowColor: undefined,
          root: undefined,
        },
        taskLayerOfImeContainer: undefined,
        taskLayerOfImeSnapshot: undefined,
      },
    );
    highlightedItem = '';
    additionalPropertieTreeName: string | undefined;

    onHighlightedChange = (event: Event) => {
      this.highlightedItem = (event as CustomEvent).detail.id;
    };
    onAdditionalPropertySelectedChange = (event: Event) => {
      this.highlightedItem = (
        event as CustomEvent
      ).detail.selectedItem.treeNode.id;
      this.additionalPropertieTreeName = (
        event as CustomEvent
      ).detail.selectedItem.name;
    };
  }
});
