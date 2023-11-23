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
import {CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA} from '@angular/core';
import {ComponentFixture, ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {MatDividerModule} from '@angular/material/divider';
import {assertDefined} from 'common/assert_utils';
import {TimestampType} from 'common/time';
import {
  CrossPlatform,
  ShellTransitionData,
  Transition,
  TransitionChange,
  TransitionType,
  WmTransitionData,
} from 'flickerlib/common';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {UnitTestUtils} from 'test/unit/utils';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {TreeComponent} from 'viewers/components/tree_component';
import {TreeNodeComponent} from 'viewers/components/tree_node_component';
import {TreeNodeDataViewComponent} from 'viewers/components/tree_node_data_view_component';
import {TreeNodePropertiesDataViewComponent} from 'viewers/components/tree_node_properties_data_view_component';
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
      imports: [MatDividerModule, ScrollingModule],
      declarations: [
        ViewerTransitionsComponent,
        TreeComponent,
        TreeNodeComponent,
        TreeNodeDataViewComponent,
        TreeNodePropertiesDataViewComponent,
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA],
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

    const entry = htmlElement.querySelector('.scroll .entry');
    expect(entry).toBeTruthy();
    expect(entry!.innerHTML).toContain('TO_FRONT');
    expect(entry!.innerHTML).toContain('10ns');
  });

  it('shows message when no transition is selected', () => {
    expect(htmlElement.querySelector('.container-properties')?.innerHTML).toContain(
      'No selected transition'
    );
  });

  it('emits TransitionSelected event on transition clicked', () => {
    const emitEventSpy = spyOn(component, 'emitEvent');

    const entries = htmlElement.querySelectorAll('.entry.table-row');
    const entry1 = assertDefined(entries[0]) as HTMLElement;
    const entry2 = assertDefined(entries[1]) as HTMLElement;
    const treeView = assertDefined(
      htmlElement.querySelector('.container-properties')
    ) as HTMLElement;
    expect(treeView.textContent).toContain('No selected transition');

    expect(emitEventSpy).not.toHaveBeenCalled();

    const id0 = assertDefined(entry1.querySelector('.id')).textContent;
    expect(id0).toBe('0');
    entry1.click();
    fixture.detectChanges();

    expect(emitEventSpy).toHaveBeenCalled();
    expect(emitEventSpy).toHaveBeenCalledWith(Events.TransitionSelected, jasmine.any(Object));
    expect(emitEventSpy.calls.mostRecent().args[1].id).toBe(0);

    const id1 = assertDefined(entry2.querySelector('.id')).textContent;
    expect(id1).toBe('1');
    entry2.click();
    fixture.detectChanges();

    expect(emitEventSpy).toHaveBeenCalled();
    expect(emitEventSpy).toHaveBeenCalledWith(Events.TransitionSelected, jasmine.any(Object));
    expect(emitEventSpy.calls.mostRecent().args[1].id).toBe(1);
  });

  it('updates tree view on TracePositionUpdate event', async () => {
    const parser = await UnitTestUtils.getTracesParser([
      'traces/elapsed_and_real_timestamp/wm_transition_trace.pb',
      'traces/elapsed_and_real_timestamp/shell_transition_trace.pb',
    ]);
    const trace = Trace.fromParser(parser, TimestampType.REAL);
    const traces = new Traces();
    traces.setTrace(TraceType.TRANSITION, trace);

    let treeView = assertDefined(
      htmlElement.querySelector('.container-properties')
    ) as any as HTMLElement;
    expect(treeView.textContent).toContain('No selected transition');

    const presenter = new Presenter(traces, (data) => {
      component.inputData = data;
    });
    const selectedTransitionEntry = assertDefined(
      traces.getTrace(TraceType.TRANSITION)?.getEntry(2)
    );
    const selectedTransition = (await selectedTransitionEntry.getValue()) as Transition;
    await presenter.onAppEvent(
      new TracePositionUpdate(TracePosition.fromTraceEntry(selectedTransitionEntry))
    );

    expect(component.uiData.selectedTransition.id).toBe(selectedTransition.id);
    expect(component.uiData.selectedTransitionPropertiesTree).toBeTruthy();

    fixture.detectChanges();

    treeView = assertDefined(
      fixture.nativeElement.querySelector('.container-properties')
    ) as any as HTMLElement;
    const textContentWithoutWhitespaces = treeView.textContent?.replace(/(\s|\t|\n)*/g, '');
    expect(textContentWithoutWhitespaces).toContain(`id:${selectedTransition.id}`);
  });
});

function makeUiData(): UiData {
  let mockTransitionIdCounter = 0;

  const transitions = [
    createMockTransition(10, 20, 30, mockTransitionIdCounter++),
    createMockTransition(40, 42, 50, mockTransitionIdCounter++),
    createMockTransition(45, 46, 49, mockTransitionIdCounter++),
    createMockTransition(55, 58, 70, mockTransitionIdCounter++),
  ];

  const selectedTransition = undefined;
  const selectedTransitionPropertiesTree = undefined;
  const timestampType = TimestampType.REAL;

  return new UiData(
    transitions,
    selectedTransition,
    timestampType,
    selectedTransitionPropertiesTree
  );
}

function createMockTransition(
  createTimeNanos: number,
  sendTimeNanos: number,
  finishTimeNanos: number,
  id: number
): Transition {
  const createTime = CrossPlatform.timestamp.fromString(createTimeNanos.toString(), null, null);
  const sendTime = CrossPlatform.timestamp.fromString(sendTimeNanos.toString(), null, null);
  const abortTime = null;
  const finishTime = CrossPlatform.timestamp.fromString(finishTimeNanos.toString(), null, null);
  const startingWindowRemoveTime = null;

  const startTransactionId = '-1';
  const finishTransactionId = '-1';
  const type = TransitionType.TO_FRONT;
  const changes: TransitionChange[] = [];

  return new Transition(
    id,
    new WmTransitionData(
      createTime,
      sendTime,
      abortTime,
      finishTime,
      startingWindowRemoveTime,
      startTransactionId,
      finishTransactionId,
      type,
      changes
    ),
    new ShellTransitionData()
  );
}
