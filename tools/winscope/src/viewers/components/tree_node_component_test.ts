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
import {Component, NO_ERRORS_SCHEMA, ViewChild} from '@angular/core';
import {ComponentFixture, ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {MatIconModule} from '@angular/material/icon';
import {TreeNodeComponent} from './tree_node_component';
import {TreeNodeDataViewComponent} from './tree_node_data_view_component';

describe('TreeNodeComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatIconModule],
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      declarations: [TreeNodeComponent, TreeNodeDataViewComponent, TestHostComponent],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('can generate data view component', () => {
    component.treeNodeComponent.isPropertiesTreeNode = jasmine.createSpy().and.returnValue(false);
    fixture.detectChanges();
    const treeNodeDataView = htmlElement.querySelector('tree-node-data-view');
    expect(treeNodeDataView).toBeTruthy();
  });

  it('can trigger tree toggle on click of chevron', () => {
    component.treeNodeComponent.showChevron = jasmine.createSpy().and.returnValue(true);
    fixture.detectChanges();

    const spy = spyOn(component.treeNodeComponent.toggleTreeChange, 'emit');
    const toggleButton = htmlElement.querySelector('.toggle-tree-btn');
    expect(toggleButton).toBeTruthy();
    (toggleButton as HTMLButtonElement).click();
    expect(spy).toHaveBeenCalled();
  });

  it('can trigger tree expansion on click of expand tree button', () => {
    const spy = spyOn(component.treeNodeComponent.expandTreeChange, 'emit');
    const expandButton = htmlElement.querySelector('.expand-tree-btn');
    expect(expandButton).toBeTruthy();
    (expandButton as HTMLButtonElement).click();
    expect(spy).toHaveBeenCalled();
  });

  it('can trigger node pin on click of star', () => {
    component.treeNodeComponent.showPinNodeIcon = jasmine.createSpy().and.returnValue(true);
    fixture.detectChanges();

    const spy = spyOn(component.treeNodeComponent.pinNodeChange, 'emit');
    const pinNodeButton = htmlElement.querySelector('.pin-node-btn');
    expect(pinNodeButton).toBeTruthy();
    (pinNodeButton as HTMLButtonElement).click();
    expect(spy).toHaveBeenCalledWith(component.item);
  });

  @Component({
    selector: 'host-component',
    template: `
      <tree-node
        [item]="item"
        [isCollapsed]="false"
        [isPinned]="false"
        [isInPinnedSection]="false"
        [hasChildren]="true"
        [isSelected]="isSelected"></tree-node>
    `,
  })
  class TestHostComponent {
    item = {
      kind: 'entry',
      name: 'LayerTraceEntry',
      stableId: '4',
      children: [{stableId: 'child'}],
    };

    isSelected = false;

    @ViewChild(TreeNodeComponent)
    treeNodeComponent!: TreeNodeComponent;
  }
});
