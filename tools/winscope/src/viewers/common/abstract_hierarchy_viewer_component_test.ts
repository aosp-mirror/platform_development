/*
 * Copyright (C) 2025 The Android Open Source Project
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
import {Type} from '@angular/core';
import {ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatSliderModule} from '@angular/material/slider';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {CollapsedSectionsComponent} from 'viewers/components/collapsed_sections_component';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
import {HierarchyComponent} from 'viewers/components/hierarchy_component';
import {PropertiesComponent} from 'viewers/components/properties_component';
import {RectsComponent} from 'viewers/components/rects/rects_component';
import {SearchBoxComponent} from 'viewers/components/search_box_component';
import {UserOptionsComponent} from 'viewers/components/user_options_component';

export abstract class AbstractHierarchyViewerComponentTest<T extends object> {
  execute() {
    describe('Hierarchy viewer component', () => {
      let dom: DOMTestHelper<T>;
      let component: T;

      beforeEach(async () => {
        [dom, component] = await this.setUpTestEnvironment();
      });

      it('creates hierarchy view', () => {
        expect(dom.find('.hierarchy-view')).toBeDefined();
      });

      it('creates properties view', () => {
        expect(dom.find('.properties-view')).toBeDefined();
      });

      it('creates collapsed sections with no buttons', () => {
        dom.checkNoCollapsedSectionButtons();
      });

      it('handles hierarchy section collapse/expand', () => {
        dom.checkSectionCollapseAndExpand(
          '.hierarchy-view',
          this.hierarchyTitle,
        );
      });

      it('handles properties section collapse/expand', () => {
        dom.checkSectionCollapseAndExpand(
          '.properties-view',
          this.propertiesTitle,
        );
      });

      if (this.testRects) {
        it('creates rects view', () => {
          expect(dom.find('.rects-view')).toBeDefined();
        });

        it('handles rects section collapse/expand', () => {
          dom.checkSectionCollapseAndExpand(
            '.rects-view',
            assertDefined(this.rectsTitle),
          );
        });
      }
    });

    if (this.executeSpecializedTests) {
      this.executeSpecializedTests();
    }
  }

  protected async initializeTestEnvironment<U extends T>(
    typeofViewer: Type<U>,
    addedDeclarations: object[] = [],
  ): Promise<[DOMTestHelper<U>, U]> {
    const declarations: object[] = [
      typeofViewer,
      HierarchyComponent,
      PropertiesComponent,
      RectsComponent,
      CollapsedSectionsComponent,
      CollapsibleSectionTitleComponent,
      UserOptionsComponent,
      SearchBoxComponent,
    ];
    if (addedDeclarations) {
      declarations.push(...addedDeclarations);
    }
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      imports: [
        CommonModule,
        MatIconModule,
        MatDividerModule,
        MatCheckboxModule,
        MatSliderModule,
        MatFormFieldModule,
        MatInputModule,
        BrowserAnimationsModule,
        FormsModule,
        MatTooltipModule,
        MatButtonModule,
        MatSelectModule,
        HttpClientModule,
      ],
      declarations,
    }).compileComponents();

    const fixture = TestBed.createComponent<U>(typeofViewer);
    const component = fixture.componentInstance;
    const dom = new DOMTestHelper(fixture, fixture.nativeElement);
    dom.detectChanges();
    return [dom, component];
  }

  protected abstract readonly testRects: boolean;
  protected abstract readonly hierarchyTitle: string;
  protected abstract readonly propertiesTitle: string;
  protected readonly rectsTitle?: string;

  protected abstract setUpTestEnvironment(): Promise<[DOMTestHelper<T>, T]>;
  protected executeSpecializedTests?(): void;
}
