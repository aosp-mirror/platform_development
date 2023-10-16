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
import {
  CrossPlatform,
  ShellTransitionData,
  Transition,
  TransitionChange,
  TransitionType,
  WmTransitionData,
} from 'trace/flickerlib/common';
import {TimestampType} from 'trace/timestamp';
import {UiData} from './ui_data';
import {ViewerTransitionsComponent} from './viewer_transitions_component';

describe('ViewerTransitionsComponent', () => {
  let fixture: ComponentFixture<ViewerTransitionsComponent>;
  let component: ViewerTransitionsComponent;
  let htmlElement: HTMLElement;

  beforeAll(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      imports: [MatDividerModule, ScrollingModule],
      declarations: [ViewerTransitionsComponent],
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
});

function makeUiData(): UiData {
  const transitions = [
    createMockTransition(10, 20, 30),
    createMockTransition(40, 42, 50),
    createMockTransition(45, 46, 49),
    createMockTransition(55, 58, 70),
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
  finishTimeNanos: number
): Transition {
  const createTime = CrossPlatform.timestamp.fromString(createTimeNanos.toString(), null, null);
  const sendTime = CrossPlatform.timestamp.fromString(sendTimeNanos.toString(), null, null);
  const abortTime = null;
  const finishTime = CrossPlatform.timestamp.fromString(finishTimeNanos.toString(), null, null);

  const startTransactionId = '-1';
  const finishTransactionId = '-1';
  const type = TransitionType.TO_FRONT;
  const changes: TransitionChange[] = [];

  return new Transition(
    id++,
    new WmTransitionData(
      createTime,
      sendTime,
      abortTime,
      finishTime,
      startTransactionId,
      finishTransactionId,
      type,
      changes
    ),
    new ShellTransitionData()
  );
}

let id = 0;
