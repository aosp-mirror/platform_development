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

import {ClipboardModule} from '@angular/cdk/clipboard';
import {
  CdkVirtualScrollViewport,
  ScrollingModule,
} from '@angular/cdk/scrolling';
import {HttpClientModule} from '@angular/common/http';
import {Type} from '@angular/core';
import {ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatSliderModule} from '@angular/material/slider';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {animationFrameScheduler} from 'rxjs';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {CollapsedSectionsComponent} from 'viewers/components/collapsed_sections_component';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
import {LogComponent} from 'viewers/components/log_component';
import {PropertiesComponent} from 'viewers/components/properties_component';
import {PropertyTreeNodeDataViewComponent} from 'viewers/components/property_tree_node_data_view_component';
import {SearchBoxComponent} from 'viewers/components/search_box_component';
import {SelectWithFilterComponent} from 'viewers/components/select_with_filter_component';
import {TreeComponent} from 'viewers/components/tree_component';
import {TreeNodeComponent} from 'viewers/components/tree_node_component';
import {ViewerInputComponent} from 'viewers/viewer_input/viewer_input_component';
import {ViewerJankCujsComponent} from 'viewers/viewer_jank_cujs/viewer_jank_cujs_component';
import {ViewerProtologComponent} from 'viewers/viewer_protolog/viewer_protolog_component';
import {ViewerTransactionsComponent} from 'viewers/viewer_transactions/viewer_transactions_component';
import {ViewerTransitionsComponent} from 'viewers/viewer_transitions/viewer_transitions_component';
import {ColumnSpec, UiDataLog} from './ui_data_log';
import {VariableHeightScrollDirective} from './variable_height_scroll_directive';

type LogViewerComponent =
  | ViewerProtologComponent
  | ViewerTransactionsComponent
  | ViewerTransitionsComponent
  | ViewerInputComponent
  | ViewerJankCujsComponent;

export abstract class AbstractLogViewerComponentTest<
  T extends LogViewerComponent,
> {
  protected readonly testSpec: ColumnSpec = {
    name: 'Test Column',
    cssClass: 'test-class',
  };
  protected readonly testField = {spec: this.testSpec, value: 'VALUE'};

  execute() {
    describe('Log viewer component', () => {
      let dom: DOMTestHelper<T>;
      let viewport: CdkVirtualScrollViewport;
      let component: T;

      describe('common', () => {
        beforeEach(async () => {
          [dom, viewport, component] = await this.setUpTestEnvironment();
        });

        it('can be created', () => {
          expect(component).toBeTruthy();
        });

        it('renders log component', () => {
          expect(dom.find('.log-view')).toBeDefined();
        });

        it('render headers as filters', () => {
          const selector = `.headers .filter.${
            this.testSpec.cssClass.split(' ')[0]
          }`;
          expect(dom.find(selector) !== undefined).toEqual(this.hasFilters);
        });

        it('renders entries with field values', () => {
          expect(dom.find('.scroll')).toBeDefined();
          const entry = dom.get(
            `.scroll .entry .${this.testSpec.cssClass.split(' ')[0]}`,
          );
          entry.checkText('VALUE');
          this.checkTimestampInTable(dom);
        });

        it('handles go to current time button', () => {
          expect(dom.find('.go-to-current-time') !== undefined).toEqual(
            this.hasCurrentTimeButton,
          );
        });

        if (this.testProperties) {
          it('renders properties', () => {
            expect(dom.find('.properties-view')).toBeDefined();
          });

          it('creates collapsed sections with no buttons', () => {
            dom.checkNoCollapsedSectionButtons();
          });

          it('handles properties section collapse/expand', () => {
            dom.checkSectionCollapseAndExpand(
              '.properties-view',
              assertDefined(this.propertiesSectionTitle),
            );
          });

          it('shows message when no entry is selected', () => {
            const data = assertDefined(component.inputData);
            (data as any).propertiesTree = undefined;
            dom.detectChanges();
            dom
              .get('.properties-view .placeholder-text')
              .checkTextExact(assertDefined(this.propertiesPlaceholder));
          });
        }
      });

      if (this.testScroll) {
        describe('scroll', () => {
          beforeEach(async () => {
            [dom, viewport] = this.setUpTestEnvironmentForScroll
              ? await this.setUpTestEnvironmentForScroll()
              : await this.setUpTestEnvironment();
          });

          it('renders initial state', () => {
            expect(dom.findAll('.entry').length).toEqual(20);
          });

          it('gets data length', () => {
            expect(viewport.getDataLength()).toEqual(200);
          });

          it('should get the rendered range', () => {
            expect(viewport.getRenderedRange()).toEqual({start: 0, end: 20});
          });

          it('should scroll to index in large jumps', () => {
            expect(dom.find(`.entry[item-id="30"]`)).toBeUndefined();
            checkScrollToIndex(30);
            expect(dom.find(`.entry[item-id="70"]`)).toBeUndefined();
            checkScrollToIndex(70);
          });

          it('should update without jumps as the user scrolls down or up', () => {
            for (let i = 1; i < 50; i++) {
              checkScrollToIndex(i);
            }
            for (let i = 49; i >= 0; i--) {
              checkScrollToIndex(i);
            }
          });

          function checkScrollToIndex(i: number) {
            viewport.scrollToIndex(i);
            viewport.elementRef.nativeElement.dispatchEvent(
              new Event('scroll'),
            );
            animationFrameScheduler.flush();
            dom.detectChanges();
            expect(dom.find(`.entry[item-id="${i}"]`)).toBeDefined();
          }
        });
      }
    });

    if (this.executeSpecializedTests) {
      this.executeSpecializedTests();
    }
  }

  protected async initializeTestEnvironment<U extends T>(
    initialUiData: UiDataLog,
    typeofViewer: Type<U>,
    addedDeclarations: object[] = [],
  ): Promise<[DOMTestHelper<U>, CdkVirtualScrollViewport, U]> {
    const declarations: object[] = [
      typeofViewer,
      SelectWithFilterComponent,
      SearchBoxComponent,
      LogComponent,
      VariableHeightScrollDirective,
    ];
    if (addedDeclarations) {
      declarations.push(...addedDeclarations);
    }
    if (this.testProperties) {
      declarations.push(
        ...[
          CollapsedSectionsComponent,
          CollapsibleSectionTitleComponent,
          PropertiesComponent,
          TreeComponent,
          TreeNodeComponent,
          PropertyTreeNodeDataViewComponent,
        ],
      );
    }
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      imports: [
        MatDividerModule,
        ScrollingModule,
        MatIconModule,
        ClipboardModule,
        MatFormFieldModule,
        MatButtonModule,
        MatInputModule,
        BrowserAnimationsModule,
        FormsModule,
        MatSelectModule,
        MatTooltipModule,
        HttpClientModule,
        MatSliderModule,
      ],
      declarations,
    }).compileComponents();

    const fixture = TestBed.createComponent<U>(typeofViewer);
    const component = fixture.componentInstance;
    const dom = new DOMTestHelper(fixture, fixture.nativeElement);
    (component as any).inputData = initialUiData;
    dom.detectChanges();
    const viewport = assertDefined(component.logComponent?.scrollComponent);
    return [dom, viewport, component];
  }

  protected abstract readonly testProperties: boolean;
  protected abstract readonly hasCurrentTimeButton: boolean;
  protected abstract readonly testScroll: boolean;
  protected readonly hasFilters: boolean = true;
  protected readonly propertiesSectionTitle?: string;
  protected readonly propertiesPlaceholder?: string;

  protected abstract setUpTestEnvironment(): Promise<
    [DOMTestHelper<T>, CdkVirtualScrollViewport, T]
  >;
  protected abstract checkTimestampInTable(dom: DOMTestHelper<T>): void;
  protected setUpTestEnvironmentForScroll?(): Promise<
    [DOMTestHelper<T>, CdkVirtualScrollViewport]
  >;
  protected executeSpecializedTests?(): void;
}
