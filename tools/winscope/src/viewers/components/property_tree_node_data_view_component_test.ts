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
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {MatButtonModule} from '@angular/material/button';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TIMESTAMP_FORMATTER} from 'trace/tree_node/formatters';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {PropertyTreeNodeDataViewComponent} from './property_tree_node_data_view_component';

describe('PropertyTreeNodeDataViewComponent', () => {
  let fixture: ComponentFixture<PropertyTreeNodeDataViewComponent>;
  let component: PropertyTreeNodeDataViewComponent;
  let htmlElement: HTMLElement;

  beforeAll(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      declarations: [PropertyTreeNodeDataViewComponent],
      imports: [MatButtonModule, BrowserAnimationsModule],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PropertyTreeNodeDataViewComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('can emit timestamp', () => {
    let timestamp: PropertyTreeNode | undefined;
    htmlElement.addEventListener(ViewerEvents.TimestampClick, (event) => {
      timestamp = (event as CustomEvent).detail;
    });
    const node = UiPropertyTreeNode.from(
      new PropertyTreeBuilder()
        .setRootId('test node')
        .setName('timestamp')
        .setValue(
          NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659126889102158832n),
        )
        .setFormatter(TIMESTAMP_FORMATTER)
        .build(),
    );
    component.node = node;
    fixture.detectChanges();

    const timestampButton = assertDefined(
      htmlElement.querySelector('.time button'),
    ) as HTMLButtonElement;
    timestampButton.click();
    fixture.detectChanges();

    expect(assertDefined(timestamp).formattedValue()).toEqual(
      '2022-07-29T20:34:49.102158832',
    );
  });
});
