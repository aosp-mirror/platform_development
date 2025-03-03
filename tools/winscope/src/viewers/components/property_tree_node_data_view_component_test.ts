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
import {TimestampConverterUtils} from 'common/time/test_utils';
import {Timestamp} from 'common/time/time';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {
  HEX_FORMATTER,
  TIMESTAMP_NODE_FORMATTER,
} from 'trace/tree_node/formatters';
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

    assertDefined(
      htmlElement.querySelector<HTMLElement>('.time-button'),
    ).click();
    fixture.detectChanges();

    expect(assertDefined(timestamp).format()).toEqual(
      '2022-07-29, 20:34:49.102',
    );
  });

  it('can emit propagatable node', () => {
    let clickedNode: UiPropertyTreeNode | undefined;
    htmlElement.addEventListener(
      ViewerEvents.PropagatePropertyClick,
      (event) => {
        clickedNode = (event as CustomEvent).detail;
      },
    );
    const node = UiPropertyTreeNode.from(
      new PropertyTreeBuilder()
        .setRootId('test node')
        .setName('property')
        .setValue(12345)
        .setFormatter(HEX_FORMATTER)
        .build(),
    );
    node.setCanPropagate(true);
    component.node = node;
    fixture.detectChanges();

    const button = assertDefined(
      htmlElement.querySelector<HTMLElement>('.inline button'),
    );
    expect(button.textContent?.trim()).toEqual('0x3039');
    button.click();
    fixture.detectChanges();
    expect(clickedNode).toEqual(node);
  });
});
