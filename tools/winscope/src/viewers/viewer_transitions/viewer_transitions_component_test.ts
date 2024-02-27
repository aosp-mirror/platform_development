/*
 * Copyright (C) 2023 The Android Open Source Project
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

import {ScrollingModule} from '@angular/cdk/scrolling';
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {MatDividerModule} from '@angular/material/divider';
import {MatIconModule} from '@angular/material/icon';
import {assertDefined} from 'common/assert_utils';
import {TimestampType} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {Transition} from 'trace/transition';
import {TIMESTAMP_FORMATTER} from 'trace/tree_node/formatters';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {PropertiesComponent} from 'viewers/components/properties_component';
import {PropertyTreeNodeDataViewComponent} from 'viewers/components/property_tree_node_data_view_component';
import {TreeComponent} from 'viewers/components/tree_component';
import {TreeNodeComponent} from 'viewers/components/tree_node_component';
import {Events} from './events';
import {Presenter} from './presenter';
import {UiData} from './ui_data';
import {ViewerTransitionsComponent} from './viewer_transitions_component';

describe('ViewerTransitionsComponent', () => {
  let fixture: ComponentFixture<ViewerTransitionsComponent>;
  let component: ViewerTransitionsComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      imports: [MatDividerModule, ScrollingModule, MatIconModule],
      declarations: [
        ViewerTransitionsComponent,
        TreeComponent,
        TreeNodeComponent,
        PropertyTreeNodeDataViewComponent,
        PropertiesComponent,
      ],
      schemas: [],
    }).compileComponents();

    fixture = TestBed.createComponent(ViewerTransitionsComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;

    component.uiData = makeUiData();
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders entries', () => {
    expect(htmlElement.querySelector('.scroll')).toBeTruthy();

    const entry = assertDefined(htmlElement.querySelector('.scroll .entry'));
    expect(entry.innerHTML).toContain('TO_FRONT');
    expect(entry.innerHTML).toContain('10ns');
  });

  it('shows message when no transition is selected', () => {
    expect(
      htmlElement.querySelector('.properties-view .placeholder-text')
        ?.innerHTML,
    ).toContain('No selected transition');
  });

  it('emits TransitionSelected event on transition clicked', () => {
    const emitEventSpy = spyOn(component, 'emitEvent');

    const entries = htmlElement.querySelectorAll('.entry.table-row');
    const entry1 = assertDefined(entries[0]) as HTMLElement;
    const entry2 = assertDefined(entries[1]) as HTMLElement;
    const treeView = assertDefined(
      htmlElement.querySelector('.properties-view'),
    ) as HTMLElement;
    expect(
      assertDefined(treeView.querySelector('.placeholder-text')).textContent,
    ).toContain('No selected transition');

    expect(emitEventSpy).not.toHaveBeenCalled();

    const id0 = assertDefined(entry1.querySelector('.id')).textContent;
    expect(id0).toEqual('0');
    entry1.click();
    fixture.detectChanges();

    expect(emitEventSpy).toHaveBeenCalled();
    expect(emitEventSpy).toHaveBeenCalledWith(
      Events.TransitionSelected,
      jasmine.any(Object),
    );
    expect(
      emitEventSpy.calls.mostRecent().args[1].getChildByName('id')?.getValue(),
    ).toEqual(0);

    const id1 = assertDefined(entry2.querySelector('.id')).textContent;
    expect(id1).toEqual('1');
    entry2.click();
    fixture.detectChanges();

    expect(emitEventSpy).toHaveBeenCalled();
    expect(emitEventSpy).toHaveBeenCalledWith(
      Events.TransitionSelected,
      jasmine.any(Object),
    );
    expect(
      emitEventSpy.calls.mostRecent().args[1].getChildByName('id')?.getValue(),
    ).toEqual(1);
  });

  it('updates tree view on TracePositionUpdate event', async () => {
    const parser = (await UnitTestUtils.getTracesParser([
      'traces/elapsed_and_real_timestamp/wm_transition_trace.pb',
      'traces/elapsed_and_real_timestamp/shell_transition_trace.pb',
    ])) as Parser<PropertyTreeNode>;
    const trace = Trace.fromParser(parser, TimestampType.REAL);
    const traces = new Traces();
    traces.setTrace(TraceType.TRANSITION, trace);

    let treeView = assertDefined(
      htmlElement.querySelector('.properties-view'),
    ) as any as HTMLElement;
    expect(
      assertDefined(treeView.querySelector('.placeholder-text')).textContent,
    ).toContain('No selected transition');

    const presenter = new Presenter(traces, (data) => {
      component.inputData = data;
    });
    const selectedTransitionEntry = assertDefined(
      traces.getTrace(TraceType.TRANSITION)?.getEntry(2),
    );
    const selectedTransition = await selectedTransitionEntry.getValue();
    const selectedTransitionId = assertDefined(
      selectedTransition.getChildByName('id'),
    ).getValue();
    await presenter.onAppEvent(
      new TracePositionUpdate(
        TracePosition.fromTraceEntry(selectedTransitionEntry),
      ),
    );

    expect(
      assertDefined(
        component.uiData.selectedTransition
          ?.getChildByName('wmData')
          ?.getChildByName('id'),
      ).getValue(),
    ).toEqual(selectedTransitionId);

    fixture.detectChanges();

    treeView = assertDefined(
      fixture.nativeElement.querySelector('.properties-view'),
    ) as any as HTMLElement;
    const textContentWithoutWhitespaces = treeView.textContent?.replace(
      /(\s|\t|\n)*/g,
      '',
    );
    expect(textContentWithoutWhitespaces).toContain(
      `id:${selectedTransitionId}`,
    );
  });

  it('propagates timestamp on click', () => {
    let timestamp = '';
    htmlElement.addEventListener(ViewerEvents.TimestampClick, (event) => {
      timestamp = (event as CustomEvent).detail.formattedValue();
    });
    const logTimestampButton = assertDefined(
      htmlElement.querySelector('.time button'),
    ) as HTMLButtonElement;
    logTimestampButton.click();

    expect(timestamp).toEqual('20ns');
  });
});

function makeUiData(): UiData {
  let mockTransitionIdCounter = 0;

  const transitions = [
    createMockTransition(20, 30, mockTransitionIdCounter++),
    createMockTransition(42, 50, mockTransitionIdCounter++),
    createMockTransition(46, 49, mockTransitionIdCounter++),
    createMockTransition(58, 70, mockTransitionIdCounter++),
  ];

  return new UiData(transitions, undefined);
}

function createMockTransition(
  sendTimeNanos: number,
  finishTimeNanos: number,
  id: number,
): Transition {
  const transitionTree = new PropertyTreeBuilder()
    .setIsRoot(true)
    .setRootId('TransitionTraceEntry')
    .setName('transition')
    .setChildren([{name: 'id', value: id}])
    .build();

  const sendTimeNode = new PropertyTreeBuilder()
    .setRootId(transitionTree.id)
    .setName('sendTimeNs')
    .setValue(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(BigInt(sendTimeNanos)),
    )
    .setFormatter(TIMESTAMP_FORMATTER)
    .build();

  return {
    id,
    type: 'TO_FRONT',
    sendTime: sendTimeNode,
    dispatchTime: undefined,
    duration: (finishTimeNanos - sendTimeNanos).toString() + 'ns',
    merged: false,
    aborted: false,
    played: false,
    propertiesTree: transitionTree,
  };
}
