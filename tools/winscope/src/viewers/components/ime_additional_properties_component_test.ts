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
    htmlElement.addEventListener(ViewerEvents.HighlightedChange, component.onHighlightedChange);
    htmlElement.addEventListener(
      ViewerEvents.AdditionalPropertySelected,
      component.onAdditionalPropertySelectedChange
    );
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('emits update additional property tree event on wm state button click', () => {
    const button: HTMLButtonElement | null = htmlElement.querySelector('.wm-state');
    assertDefined(button).click();
    fixture.detectChanges();
    expect(component.additionalPropertieTree).toBe('wmState');
  });

  it('propagates new ime container layer on button click', () => {
    const button: HTMLButtonElement | null = htmlElement.querySelector('.ime-container');
    assertDefined(button).click();
    fixture.detectChanges();
    expect(component.highlightedItem).toBe('imeContainerId');
  });

  it('propagates new input method surface layer on button click', () => {
    const button: HTMLButtonElement | null = htmlElement.querySelector('.input-method-surface');
    assertDefined(button).click();
    fixture.detectChanges();
    expect(component.highlightedItem).toBe('inputMethodSurfaceId');
  });

  @Component({
    selector: 'host-component',
    template: `
      <ime-additional-properties
        [highlightedItem]="highlightedItem"
        [isImeManagerService]="false"
        [additionalProperties]="additionalProperties"></ime-additional-properties>
    `,
  })
  class TestHostComponent {
    additionalProperties = new ImeAdditionalProperties(
      {
        name: 'wmState',
        stableId: 'wmStateId',
        focusedApp: 'exampleFocusedApp',
        focusedWindow: null,
        focusedActivity: null,
        isInputMethodWindowVisible: false,
        protoImeControlTarget: null,
        protoImeInputTarget: null,
        protoImeLayeringTarget: null,
        protoImeInsetsSourceProvider: null,
        proto: {name: 'wmStateProto'},
      },
      {
        name: 'imeLayers',
        imeContainer: {name: 'imeContainer', stableId: 'imeContainerId'},
        inputMethodSurface: {name: 'inputMethodSurface', stableId: 'inputMethodSurfaceId'},
        focusedWindow: undefined,
        taskOfImeContainer: undefined,
        taskOfImeSnapshot: undefined,
      }
    );
    highlightedItem = '';
    additionalPropertieTree = '';

    onHighlightedChange = (event: Event) => {
      this.highlightedItem = (event as CustomEvent).detail.id;
    };
    onAdditionalPropertySelectedChange = (event: Event) => {
      this.additionalPropertieTree = (event as CustomEvent).detail.selectedItem.name;
    };
  }
});
