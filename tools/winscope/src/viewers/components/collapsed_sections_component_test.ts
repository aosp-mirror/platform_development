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

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {assertDefined} from 'common/assert_utils';
import {CollapsibleSections} from 'viewers/common/collapsible_sections';
import {CollapsibleSectionType} from 'viewers/common/collapsible_section_type';
import {CollapsedSectionsComponent} from './collapsed_sections_component';

describe('CollapsedSectionsComponent', () => {
  let fixture: ComponentFixture<CollapsedSectionsComponent>;
  let component: CollapsedSectionsComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatButtonModule, MatIconModule],
      declarations: [CollapsedSectionsComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(CollapsedSectionsComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    component.sections = new CollapsibleSections([
      {
        type: CollapsibleSectionType.RECTS,
        label: 'rects',
        isCollapsed: false,
      },
      {
        type: CollapsibleSectionType.HIERARCHY,
        label: 'hierarchy',
        isCollapsed: true,
      },
      {
        type: CollapsibleSectionType.PROPERTIES,
        label: 'properties',
        isCollapsed: false,
      },
    ]);
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('displays only collapsed sections', () => {
    let sections = htmlElement.querySelectorAll('.collapsed-section');
    expect(sections.length).toEqual(1);
    expect(sections.item(0).textContent).toContain('HIERARCHY');
    assertDefined(sections.item(0).querySelector('button'));

    assertDefined(component.sections).onCollapseStateChange(
      CollapsibleSectionType.RECTS,
      true,
    );
    fixture.detectChanges();
    sections = htmlElement.querySelectorAll('.collapsed-section');
    expect(sections.length).toEqual(2);
    expect(sections.item(0).textContent).toContain('RECTS');
    assertDefined(sections.item(0).querySelector('button'));
    expect(sections.item(1).textContent).toContain('HIERARCHY');
    assertDefined(sections.item(1).querySelector('button'));
  });

  it('emits sectionChange event', () => {
    const spy = spyOn(component.sectionChange, 'emit');
    const expandButton = assertDefined(
      htmlElement.querySelector('.collapsed-section button'),
    ) as HTMLElement;
    expandButton.click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledOnceWith(CollapsibleSectionType.HIERARCHY);
  });
});
