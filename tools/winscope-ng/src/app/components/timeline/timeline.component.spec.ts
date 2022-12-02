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
import {ChangeDetectionStrategy} from "@angular/core";
import {ComponentFixture, TestBed} from "@angular/core/testing";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatButtonModule} from "@angular/material/button";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatSelectModule} from "@angular/material/select";

import {MatDrawer, MatDrawerContainer, MatDrawerContent} from "app/components/bottomnav/bottom_drawer.component";
import {TimelineComponent} from "./timeline.component";
import {ExpandedTimelineComponent} from "./expanded_timeline.component";
import {MiniTimelineComponent} from "./mini_timeline.component";
import {TimelineData} from "app/timeline_data";
import {TraceType} from "common/trace/trace_type";
import {RealTimestamp, Timestamp} from "common/trace/timestamp";
import {By} from "@angular/platform-browser";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {MatInputModule} from "@angular/material/input";
import { SingleTimelineComponent } from "./single_timeline.component";

describe("TimelineComponent", () => {
  let fixture: ComponentFixture<TimelineComponent>;
  let component: TimelineComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        FormsModule,
        MatButtonModule,
        MatFormFieldModule,
        MatInputModule,
        MatIconModule,
        MatSelectModule,
        ReactiveFormsModule,
        BrowserAnimationsModule,
      ],
      declarations: [
        ExpandedTimelineComponent,
        SingleTimelineComponent,
        MatDrawer,
        MatDrawerContainer,
        MatDrawerContent,
        MiniTimelineComponent,
        TimelineComponent,
      ]
    }).overrideComponent(TimelineComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
    fixture = TestBed.createComponent(TimelineComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;

    component.timelineData = new TimelineData();
  });

  it("can be created", () => {
    expect(component).toBeTruthy();
  });

  it("can be expanded", () => {
    const timestamps = [timestamp(100), timestamp(110)];
    component.timelineData.setTimelines([{
      traceType: TraceType.SURFACE_FLINGER,
      timestamps: timestamps
    }]);
    fixture.detectChanges();

    const button = htmlElement.querySelector(`.${component.TOGGLE_BUTTON_CLASS}`);
    expect(button).toBeTruthy();

    // initially not expanded
    let expandedTimelineElement = fixture.debugElement.query(By.directive(ExpandedTimelineComponent));
    expect(expandedTimelineElement).toBeFalsy();

    button!.dispatchEvent(new Event("click"));
    expandedTimelineElement = fixture.debugElement.query(By.directive(ExpandedTimelineComponent));
    expect(expandedTimelineElement).toBeTruthy();

    button!.dispatchEvent(new Event("click"));
    expandedTimelineElement = fixture.debugElement.query(By.directive(ExpandedTimelineComponent));
    expect(expandedTimelineElement).toBeFalsy();
  });

  it("handles no timestamps", () => {
    const timestamps: Timestamp[] = [];
    component.timelineData.setTimelines([{
      traceType: TraceType.SURFACE_FLINGER,
      timestamps: timestamps
    }]);
    fixture.detectChanges();

    // no expand button
    const button = htmlElement.querySelector(`.${component.TOGGLE_BUTTON_CLASS}`);
    expect(button).toBeFalsy();

    // no timelines shown
    const miniTimelineElement = fixture.debugElement.query(By.directive(MiniTimelineComponent));
    expect(miniTimelineElement).toBeFalsy();

    // error message shown
    const errorMessageContainer = htmlElement.querySelector(".no-timestamps-msg");
    expect(errorMessageContainer).toBeTruthy();
    expect(errorMessageContainer!.textContent).toContain("No timeline to show!");
  });

  it("processes active trace input and updates selected traces", () => {
    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    expect(component.wrappedActiveTrace).toEqual(TraceType.SURFACE_FLINGER);
    expect(component.selectedTraces).toEqual([TraceType.SURFACE_FLINGER]);

    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    expect(component.wrappedActiveTrace).toEqual(TraceType.SURFACE_FLINGER);
    expect(component.selectedTraces).toEqual([TraceType.SURFACE_FLINGER]);

    component.activeViewTraceTypes = [TraceType.TRANSACTIONS];
    expect(component.wrappedActiveTrace).toEqual(TraceType.TRANSACTIONS);
    expect(component.selectedTraces).toEqual([
      TraceType.SURFACE_FLINGER,
      TraceType.TRANSACTIONS
    ]);

    component.activeViewTraceTypes = [TraceType.WINDOW_MANAGER];
    expect(component.wrappedActiveTrace).toEqual(TraceType.WINDOW_MANAGER);
    expect(component.selectedTraces).toEqual([
      TraceType.SURFACE_FLINGER,
      TraceType.TRANSACTIONS,
      TraceType.WINDOW_MANAGER
    ]);

    component.activeViewTraceTypes = [TraceType.PROTO_LOG];
    expect(component.wrappedActiveTrace).toEqual(TraceType.PROTO_LOG);
    expect(component.selectedTraces).toEqual([
      TraceType.TRANSACTIONS,
      TraceType.WINDOW_MANAGER,
      TraceType.PROTO_LOG
    ]);
  });

  it("handles undefined active trace input", () => {
    component.activeViewTraceTypes = undefined;
    expect(component.wrappedActiveTrace).toBeUndefined();
    expect(component.selectedTraces).toEqual([]);

    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    expect(component.wrappedActiveTrace).toEqual(TraceType.SURFACE_FLINGER);
    expect(component.selectedTraces).toEqual([TraceType.SURFACE_FLINGER]);

    component.activeViewTraceTypes = undefined;
    expect(component.wrappedActiveTrace).toEqual(TraceType.SURFACE_FLINGER);
    expect(component.selectedTraces).toEqual([TraceType.SURFACE_FLINGER]);
  });

  it("handles some traces with no timestamps", () => {
    component.timelineData.setTimelines([{
      traceType: TraceType.SURFACE_FLINGER,
      timestamps: []
    }, {
      traceType: TraceType.WINDOW_MANAGER,
      timestamps: [timestamp(100)]
    }]);
    fixture.detectChanges();
  });

  it("next button disabled if no next entry", () => {
    component.timelineData.setTimelines([{
      traceType: TraceType.SURFACE_FLINGER,
      timestamps: [timestamp(100), timestamp(110)]
    }, {
      traceType: TraceType.WINDOW_MANAGER,
      timestamps: [timestamp(90), timestamp(101), timestamp(110), timestamp(112)]
    }]);
    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    fixture.detectChanges();

    expect(component.timelineData.currentTimestamp?.getValueNs()).toEqual(100n);

    const nextEntryButton = fixture.debugElement.query(By.css("#next_entry_button"));
    expect(nextEntryButton).toBeTruthy();
    expect(nextEntryButton.nativeElement.getAttribute("disabled")).toBeFalsy();

    component.timelineData.updateCurrentTimestamp(timestamp(90));
    fixture.detectChanges();
    expect(nextEntryButton.nativeElement.getAttribute("disabled")).toBeFalsy();

    component.timelineData.updateCurrentTimestamp(timestamp(110));
    fixture.detectChanges();
    expect(nextEntryButton.nativeElement.getAttribute("disabled")).toBeTruthy();

    component.timelineData.updateCurrentTimestamp(timestamp(112));
    fixture.detectChanges();
    expect(nextEntryButton.nativeElement.getAttribute("disabled")).toBeTruthy();
  });

  it("prev button disabled if no prev entry", () => {
    component.timelineData.setTimelines([{
      traceType: TraceType.SURFACE_FLINGER,
      timestamps: [timestamp(100), timestamp(110)]
    }, {
      traceType: TraceType.WINDOW_MANAGER,
      timestamps: [timestamp(90), timestamp(101), timestamp(110), timestamp(112)]
    }]);
    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    fixture.detectChanges();

    expect(component.timelineData.currentTimestamp?.getValueNs()).toEqual(100n);
    const prevEntryButton = fixture.debugElement.query(By.css("#prev_entry_button"));
    expect(prevEntryButton).toBeTruthy();
    expect(prevEntryButton.nativeElement.getAttribute("disabled")).toBeTruthy();

    component.timelineData.updateCurrentTimestamp(timestamp(90));
    fixture.detectChanges();
    expect(prevEntryButton.nativeElement.getAttribute("disabled")).toBeTruthy();

    component.timelineData.updateCurrentTimestamp(timestamp(110));
    fixture.detectChanges();
    expect(prevEntryButton.nativeElement.getAttribute("disabled")).toBeFalsy();

    component.timelineData.updateCurrentTimestamp(timestamp(112));
    fixture.detectChanges();
    expect(prevEntryButton.nativeElement.getAttribute("disabled")).toBeFalsy();
  });

  it("changes timestamp on next entry button press", () => {
    component.timelineData.setTimelines([{
      traceType: TraceType.SURFACE_FLINGER,
      timestamps: [timestamp(100), timestamp(110)]
    }, {
      traceType: TraceType.WINDOW_MANAGER,
      timestamps: [timestamp(90), timestamp(101), timestamp(110), timestamp(112)]
    }]);
    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    fixture.detectChanges();
    expect(component.timelineData.currentTimestamp?.getValueNs()).toEqual(100n);
    const nextEntryButton = fixture.debugElement.query(By.css("#next_entry_button"));
    expect(nextEntryButton).toBeTruthy();

    component.timelineData.updateCurrentTimestamp(timestamp(105));
    fixture.detectChanges();
    nextEntryButton.nativeElement.click();
    expect(component.timelineData.currentTimestamp?.getValueNs()).toEqual(110n);

    component.timelineData.updateCurrentTimestamp(timestamp(100));
    fixture.detectChanges();
    nextEntryButton.nativeElement.click();
    expect(component.timelineData.currentTimestamp?.getValueNs()).toEqual(110n);

    component.timelineData.updateCurrentTimestamp(timestamp(90));
    fixture.detectChanges();
    nextEntryButton.nativeElement.click();
    expect(component.timelineData.currentTimestamp?.getValueNs()).toEqual(100n);

    // No change when we are already on the last timestamp of the active trace
    component.timelineData.updateCurrentTimestamp(timestamp(110));
    fixture.detectChanges();
    nextEntryButton.nativeElement.click();
    expect(component.timelineData.currentTimestamp?.getValueNs()).toEqual(110n);

    // No change when we are after the last entry of the active trace
    component.timelineData.updateCurrentTimestamp(timestamp(112));
    fixture.detectChanges();
    nextEntryButton.nativeElement.click();
    expect(component.timelineData.currentTimestamp?.getValueNs()).toEqual(112n);
  });

  it("changes timestamp on previous entry button press", () => {
    component.timelineData.setTimelines([{
      traceType: TraceType.SURFACE_FLINGER,
      timestamps: [timestamp(100), timestamp(110)]
    }, {
      traceType: TraceType.WINDOW_MANAGER,
      timestamps: [timestamp(90), timestamp(101), timestamp(110), timestamp(112)]
    }]);
    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    fixture.detectChanges();
    expect(component.timelineData.currentTimestamp?.getValueNs()).toEqual(100n);
    const prevEntryButton = fixture.debugElement.query(By.css("#prev_entry_button"));
    expect(prevEntryButton).toBeTruthy();

    // In this state we are already on the first entry at timestamp 100, so
    // there is no entry to move to before and we just don't update the timestamp
    component.timelineData.updateCurrentTimestamp(timestamp(105));
    fixture.detectChanges();
    prevEntryButton.nativeElement.click();
    expect(component.timelineData.currentTimestamp?.getValueNs()).toEqual(105n);

    component.timelineData.updateCurrentTimestamp(timestamp(110));
    fixture.detectChanges();
    prevEntryButton.nativeElement.click();
    expect(component.timelineData.currentTimestamp?.getValueNs()).toEqual(100n);

    // Active entry here should be 110 so moving back means moving to 100.
    component.timelineData.updateCurrentTimestamp(timestamp(112));
    fixture.detectChanges();
    prevEntryButton.nativeElement.click();
    expect(component.timelineData.currentTimestamp?.getValueNs()).toEqual(100n);

    // No change when we are already on the first timestamp of the active trace
    component.timelineData.updateCurrentTimestamp(timestamp(100));
    fixture.detectChanges();
    prevEntryButton.nativeElement.click();
    expect(component.timelineData.currentTimestamp?.getValueNs()).toEqual(100n);

    // No change when we are before the first entry of the active trace
    component.timelineData.updateCurrentTimestamp(timestamp(90));
    fixture.detectChanges();
    prevEntryButton.nativeElement.click();
    expect(component.timelineData.currentTimestamp?.getValueNs()).toEqual(90n);
  });
});

function timestamp(timestamp: number): Timestamp {
  return new RealTimestamp(BigInt(timestamp));
}
