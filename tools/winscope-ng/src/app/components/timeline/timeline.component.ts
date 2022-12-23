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

import {
  ChangeDetectorRef,
  Component,
  Input,
  Inject,
  ViewEncapsulation,
  Output,
  EventEmitter,
  ViewChild,
  HostListener,
  ElementRef,
} from "@angular/core";
import { FormControl, FormGroup, Validators} from "@angular/forms";
import { DomSanitizer, SafeUrl } from "@angular/platform-browser";
import { TraceType } from "common/trace/trace_type";
import { TRACE_INFO } from "app/trace_info";
import { TimelineComponentDependencyInversion } from "./timeline_component_dependency_inversion";
import { TimelineData } from "app/timeline_data";
import { MiniTimelineComponent } from "./mini_timeline.component";
import { ElapsedTimestamp, RealTimestamp, Timestamp, TimestampType } from "common/trace/timestamp";
import { TimeUtils } from "common/utils/time_utils";

@Component({
  selector: "timeline",
  encapsulation: ViewEncapsulation.None,
  template: `
    <div id="expanded-nav" *ngIf="expanded">
        <div id="video-content" *ngIf="videoUrl !== undefined">
          <video
            *ngIf="getVideoCurrentTime() !== undefined"
            id="video"
            [currentTime]="getVideoCurrentTime()"
            [src]="videoUrl">
          </video>
          <div *ngIf="getVideoCurrentTime() === undefined" class="no-video-message">
            <p>No screenrecording frame to show</p>
            <p>Current timestamp before first screenrecording frame.</p>
          </div>
        </div>
        <expanded-timeline
          [timelineData]="timelineData"
          [currentTimestamp]="currentTimestamp"
          (onTimestampChanged)="updateCurrentTimestamp($event)"
          id="expanded-timeline"
        ></expanded-timeline>
    </div>
    <div class="navbar" #collapsedTimeline>
      <ng-template [ngIf]="timelineData.hasMoreThanOneDistinctTimestamp()">
        <div id="time-selector">
            <button mat-icon-button
              id="prev_entry_button"
              color="primary"
              (click)="moveToPreviousEntry()"
              [disabled]="!hasPrevEntry()">
                <mat-icon>chevron_left</mat-icon>
            </button>
            <form [formGroup]="timestampForm" class="time-selector-form">
                <mat-form-field
                  class="time-input"
                  appearance="fill"
                  (change)="humanElapsedTimeInputChange($event)"
                  *ngIf="!usingRealtime()">
                    <input matInput name="humanElapsedTimeInput" [formControl]="selectedElapsedTimeFormControl" />
                </mat-form-field>
                <mat-form-field
                  class="time-input"
                  appearance="fill"
                  (change)="humanRealTimeInputChanged($event)"
                  *ngIf="usingRealtime()">
                    <input matInput name="humanRealTimeInput" [formControl]="selectedRealTimeFormControl" />
                </mat-form-field>
                <mat-form-field
                  class="time-input"
                  appearance="fill"
                  (change)="nanosecondsInputTimeChange($event)">
                    <input matInput name="nsTimeInput" [formControl]="selectedNsFormControl" />
                </mat-form-field>
            </form>
            <button mat-icon-button
              id="next_entry_button"
              color="primary"
              (click)="moveToNextEntry()"
              [disabled]="!hasNextEntry()">
                <mat-icon>chevron_right</mat-icon>
            </button>
        </div>
        <div id="trace-selector">
            <mat-form-field appearance="none">
                <mat-select #traceSelector [formControl]="selectedTracesFormControl" multiple (closed)="onTraceSelectionClosed()">
                  <div class="tip">
                    Select up to 2 additional traces to display.
                  </div>
                  <mat-option
                    *ngFor="let trace of availableTraces"
                    [value]="trace"
                    [style]="{
                      color: TRACE_INFO[trace].color,
                      opacity: isOptionDisabled(trace) ? 0.5 : 1.0
                    }"
                    [disabled]="isOptionDisabled(trace)"
                  >
                    <mat-icon>{{ TRACE_INFO[trace].icon }}</mat-icon>
                    {{ TRACE_INFO[trace].name }}
                  </mat-option>
                  <div class="actions">
                    <button mat-button color="primary" (click)="traceSelector.close()">Cancel</button>
                    <button mat-flat-button color="primary" (click)="applyNewTraceSelection(); traceSelector.close()">Apply</button>
                  </div>
                  <mat-select-trigger class="shown-selection">
                    <mat-icon
                      *ngFor="let selectedTrace of selectedTraces"
                      [style]="{color: TRACE_INFO[selectedTrace].color}"
                    >
                      {{ TRACE_INFO[selectedTrace].icon }}
                    </mat-icon>
                  </mat-select-trigger>
                </mat-select>
            </mat-form-field>
        </div>
        <mini-timeline
          [timelineData]="timelineData"
          [currentTimestamp]="currentTimestamp"
          [selectedTraces]="selectedTraces"
          (changeTimestamp)="updateCurrentTimestamp($event)"
          (changeSeekTimestamp)="updateSeekTimestamp($event)"
          id="mini-timeline"
          #miniTimeline
        ></mini-timeline>
        <div id="toggle" *ngIf="timelineData.hasMoreThanOneDistinctTimestamp()">
            <button mat-icon-button
                    [class]="TOGGLE_BUTTON_CLASS"
                    color="primary"
                    aria-label="Toggle Expanded Timeline"
                    (click)="toggleExpand()">
                <mat-icon *ngIf="!expanded">expand_less</mat-icon>
                <mat-icon *ngIf="expanded">expand_more</mat-icon>
            </button>
        </div>
      </ng-template >
      <div *ngIf="!timelineData.hasTimestamps()" class="no-timestamps-msg">
        <p class="mat-body-2">No timeline to show!</p>
        <p class="mat-body-1">All loaded traces contain no timestamps!</p>
      </div>
      <div *ngIf="!timelineData.hasMoreThanOneDistinctTimestamp()" class="no-timestamps-msg">
        <p class="mat-body-2">No timeline to show!</p>
        <p class="mat-body-1">Only a single timestamp has been recorded.</p>
      </div>
    </div>
`,
  styles: [`
    .navbar {
      display: flex;
      width: 100%;
      flex-direction: row;
      align-items: center;
      justify-content: center;
    }
    #expanded-nav {
      display: flex;
      border-bottom: 1px solid #3333
    }
    #time-selector {
      display: flex;
      flex-direction: row;
      align-items: center;
      justify-content: center;
    }
    .time-selector-form {
      display: flex;
      flex-direction: column;
      width: 15em;
    }
    .time-selector-form .time-input {
      width: 100%;
      margin-bottom: -1.34375em;
      text-align: center;
    }
    #mini-timeline {
      flex-grow: 1;
      align-self: stretch;
    }
    #video-content {
      position: relative;
      min-width: 20rem;
      min-height: 35rem;
      align-self: stretch;
      text-align: center;
      border: 2px solid black;
      flex-basis: 0px;
      flex-grow: 1;
      display: flex;
      align-items: center;
    }
    #video {
      position: absolute;
      left: 0;
      top: 0;
      height: 100%;
      width: 100%;
    }
    #expanded-nav {
      display: flex;
      flex-direction: row;
    }
    #expanded-timeline {
      flex-grow: 1;
    }
    #trace-selector .mat-form-field-infix {
      width: 50px;
      padding: 0 0.75rem 0 0.5rem;
      border-top: unset;
    }
    #trace-selector .mat-icon {
      padding: 2px;
    }
    #trace-selector .shown-selection {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      height: auto;
    }
    #trace-selector .mat-select-trigger {
      height: unset;
    }
    #trace-selector .mat-form-field-wrapper {
      padding: 0;
    }
    .mat-select-panel {
      max-height: unset!important;
      font-family: 'Roboto', sans-serif;
    }
    .tip {
      padding: 1.5rem;
      font-weight: 200;
      border-bottom: solid 1px #DADCE0;
    }
    .actions {
      border-top: solid 1px #DADCE0;
      width: 100%;
      padding: 1.5rem;
      float: right;
      display: flex;
      justify-content: flex-end;
    }
    .no-video-message {
      padding: 1rem;
      font-family: 'Roboto', sans-serif;
    }
    .no-timestamps-msg {
      padding: 1rem;
      align-items: center;
      display: flex;
      flex-direction: column;
    }
  `],
})
export class TimelineComponent implements TimelineComponentDependencyInversion {
  public readonly TOGGLE_BUTTON_CLASS: string = "button-toggle-expansion";
  public readonly MAX_SELECTED_TRACES = 3;

  @Input() set activeViewTraceTypes(types: TraceType[]|undefined) {
    if (!types) {
      return;
    }

    if (types.length !== 1) {
      throw Error("Timeline component doesn't support viewers with dependencies length !== 1");
    }

    this.internalActiveTrace = types[0];

    if (!this.selectedTraces.includes(this.internalActiveTrace)) {
      this.selectedTraces.push(this.internalActiveTrace);
    }

    if (this.selectedTraces.length > this.MAX_SELECTED_TRACES) {
      // Maxed capacity so remove oldest selected trace
      this.selectedTraces = this.selectedTraces.slice(1, 1 + this.MAX_SELECTED_TRACES);
    }

    this.selectedTracesFormControl.setValue(this.selectedTraces);
  }
  public internalActiveTrace: TraceType|undefined = undefined;

  @Input() timelineData!: TimelineData;
  @Input() availableTraces: TraceType[] = [];

  @Output() collapsedTimelineSizeChanged = new EventEmitter<number>();

  @ViewChild("miniTimeline") private miniTimelineComponent!: MiniTimelineComponent;
  @ViewChild("collapsedTimeline") private collapsedTimelineRef!: ElementRef;

  selectedTraces: TraceType[] = [];
  selectedTracesFormControl = new FormControl();

  selectedElapsedTimeFormControl = new FormControl("undefined", Validators.compose([
    Validators.required,
    Validators.pattern(TimeUtils.HUMAN_ELAPSED_TIMESTAMP_REGEX)]));
  selectedRealTimeFormControl = new FormControl("undefined", Validators.compose([
    Validators.required,
    Validators.pattern(TimeUtils.HUMAN_REAL_TIMESTAMP_REGEX)]));
  selectedNsFormControl = new FormControl("undefined", Validators.compose([
    Validators.required,
    Validators.pattern(TimeUtils.NS_TIMESTAMP_REGEX)]));
  timestampForm = new FormGroup({
    selectedElapsedTime: this.selectedElapsedTimeFormControl,
    selectedRealTime: this.selectedRealTimeFormControl,
    selectedNs: this.selectedNsFormControl,
  });

  videoUrl: SafeUrl|undefined;

  private expanded = false;

  TRACE_INFO = TRACE_INFO;

  constructor(
    @Inject(DomSanitizer) private sanitizer: DomSanitizer,
    @Inject(ChangeDetectorRef) private changeDetectorRef: ChangeDetectorRef) {
  }

  ngOnInit() {
    if (this.timelineData.hasTimestamps()) {
      this.updateTimeInputValuesToCurrentTimestamp();
    }

    const screenRecordingVideo = this.timelineData.getScreenRecordingVideo();
    if (screenRecordingVideo) {
      this.videoUrl =
        this.sanitizer.bypassSecurityTrustUrl(URL.createObjectURL(screenRecordingVideo));
    }
  }

  ngAfterViewInit() {
    const height = this.collapsedTimelineRef.nativeElement.offsetHeight;
    this.collapsedTimelineSizeChanged.emit(height);
  }

  getVideoCurrentTime() {
    return this.timelineData.searchCorrespondingScreenRecordingTimeSeconds(this.currentTimestamp);
  }

  private seekTimestamp: Timestamp|undefined;

  get currentTimestamp(): Timestamp {
    if (this.seekTimestamp !== undefined) {
      return this.seekTimestamp;
    }

    const timestamp = this.timelineData.getCurrentTimestamp();
    if (timestamp === undefined) {
      throw Error("A timestamp should have been set by the time the timeline is loaded");
    }

    return timestamp;
  }

  onCurrentTimestampChanged(timestamp: Timestamp|undefined): void {
    if (!timestamp) {
      return;
    }
    this.updateTimeInputValuesToCurrentTimestamp();
  }

  toggleExpand() {
    this.expanded = !this.expanded;
    this.changeDetectorRef.detectChanges();
  }

  updateCurrentTimestamp(timestamp: Timestamp) {
    this.timelineData.setCurrentTimestamp(timestamp);
  }

  usingRealtime(): boolean {
    return this.timelineData.getTimestampType() === TimestampType.REAL;
  }

  updateSeekTimestamp(timestamp: Timestamp|undefined) {
    this.seekTimestamp = timestamp;
    this.updateTimeInputValuesToCurrentTimestamp();
  }

  private updateTimeInputValuesToCurrentTimestamp() {
    this.selectedElapsedTimeFormControl.setValue(TimeUtils.format(new ElapsedTimestamp(this.currentTimestamp.getValueNs()), false));
    this.selectedRealTimeFormControl.setValue(TimeUtils.format(new RealTimestamp(this.currentTimestamp.getValueNs())));
    this.selectedNsFormControl.setValue(`${this.currentTimestamp.getValueNs()} ns`);
  }

  isOptionDisabled(trace: TraceType) {
    if (this.internalActiveTrace === trace) {
      return true;
    }

    // Reached limit of options and is not a selected element
    if ((this.selectedTracesFormControl.value?.length ?? 0) >= this.MAX_SELECTED_TRACES
      && this.selectedTracesFormControl.value?.find((el: TraceType) => el === trace) === undefined) {
      return true;
    }

    return false;
  }

  onTraceSelectionClosed() {
    this.selectedTracesFormControl.setValue(this.selectedTraces);
  }

  applyNewTraceSelection() {
    this.selectedTraces = this.selectedTracesFormControl.value;
  }

  @HostListener("document:keydown", ["$event"])
  handleKeyboardEvent(event: KeyboardEvent) {
    switch (event.key) {
      case "ArrowLeft": {
        this.moveToPreviousEntry();
        break;
      }
      case "ArrowRight": {
        this.moveToNextEntry();
        break;
      }
    }
  }

  hasPrevEntry(): boolean {
    if (!this.internalActiveTrace ||
      (this.timelineData.getTimelines().get(this.internalActiveTrace)?.length ?? 0) === 0) {
      return false;
    }
    return this.timelineData.getPreviousTimestampFor(this.internalActiveTrace) !== undefined;
  }

  hasNextEntry(): boolean {
    if (!this.internalActiveTrace ||
      (this.timelineData.getTimelines().get(this.internalActiveTrace)?.length ?? 0) === 0) {
      return false;
    }
    return this.timelineData.getNextTimestampFor(this.internalActiveTrace) !== undefined;
  }

  moveToPreviousEntry() {
    if (!this.internalActiveTrace) {
      return;
    }
    this.timelineData.moveToPreviousTimestampFor(this.internalActiveTrace);
  }

  moveToNextEntry() {
    if (!this.internalActiveTrace) {
      return;
    }
    this.timelineData.moveToNextTimestampFor(this.internalActiveTrace);
  }

  humanElapsedTimeInputChange(event: Event) {
    if (event.type !== "change") {
      return;
    }
    const target = event.target as HTMLInputElement;
    const timestamp = TimeUtils.parseHumanElapsed(target.value);
    this.timelineData.setCurrentTimestamp(timestamp);
    this.updateTimeInputValuesToCurrentTimestamp();
  }

  humanRealTimeInputChanged(event: Event) {
    if (event.type !== "change") {
      return;
    }
    const target = event.target as HTMLInputElement;

    const timestamp = TimeUtils.parseHumanReal(target.value);
    this.timelineData.setCurrentTimestamp(timestamp);
    this.updateTimeInputValuesToCurrentTimestamp();
  }

  nanosecondsInputTimeChange(event: Event) {
    if (event.type !== "change") {
      return;
    }
    const target = event.target as HTMLInputElement;

    const timestamp = new Timestamp(this.timelineData.getTimestampType()!, BigInt(target.value));
    this.timelineData.setCurrentTimestamp(timestamp);
    this.updateTimeInputValuesToCurrentTimestamp();
  }
}
