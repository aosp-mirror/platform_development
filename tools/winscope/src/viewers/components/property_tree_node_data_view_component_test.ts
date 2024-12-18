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
import {Timestamp} from 'common/time';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TIMESTAMP_NODE_FORMATTER} from 'trace/tree_node/formatters';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {PropertyTreeNodeDataViewComponent} from './property_tree_node_data_view_component';

describe('PropertyTreeNodeDataViewComponent', () => {
  let fixture: ComponentFixture<PropertyTreeNodeDataViewComponent>;
  let component: PropertyTreeNodeDataViewComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      declarations: [PropertyTreeNodeDataViewComponent],
      imports: [MatButtonModule, BrowserAnimationsModule],
    }).compileComponents();
    fixture = TestBed.createComponent(PropertyTreeNodeDataViewComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('can emit timestamp', () => {
    let timestamp: Timestamp | undefined;
    htmlElement.addEventListener(ViewerEvents.TimestampClick, (event) => {
      timestamp = (event as CustomEvent).detail.timestamp;
    });
    const node = UiPropertyTreeNode.from(
      new PropertyTreeBuilder()
        .setRootId('test node')
        .setName('timestamp')
        .setValue(
          TimestampConverterUtils.makeRealTimestamp(1659126889102158832n),
        )
        .setFormatter(TIMESTAMP_NODE_FORMATTER)
        .build(),
    );
    component.node = node;
    fixture.detectChanges();

    const timestampButton = assertDefined(
      htmlElement.querySelector('.time-button'),
    ) as HTMLButtonElement;
    timestampButton.click();
    fixture.detectChanges();

    expect(assertDefined(timestamp).format()).toEqual(
      '2022-07-29, 20:34:49.102',
    );
  });
});
