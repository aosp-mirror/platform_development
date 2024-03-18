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
  ElementRef,
  EventEmitter,
  HostListener,
  Inject,
  Input,
  Output,
  ViewChild,
  ViewEncapsulation,
} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {TimelineData} from 'app/timeline_data';
import {TRACE_INFO} from 'app/trace_info';
import {assertDefined} from 'common/assert_utils';
import {FunctionUtils} from 'common/function_utils';
import {StringUtils} from 'common/string_utils';
import {Timestamp, TimestampType} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {TimestampUtils} from 'common/timestamp_utils';
import {
  ExpandedTimelineToggled,
  TracePositionUpdate,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {
  EmitEvent,
  WinscopeEventEmitter,
} from 'messaging/winscope_event_emitter';
import {WinscopeEventListener} from 'messaging/winscope_event_listener';
import {TracePosition} from 'trace/trace_position';
import {TraceType, TraceTypeUtils} from 'trace/trace_type';

@Component({
  selector: 'timeline',
  encapsulation: ViewEncapsulation.None,
  template: `
    <div id="expanded-nav" *ngIf="expanded">
      <div id="video-content" *ngIf="videoUrl !== undefined">
        <video
          *ngIf="getVideoCurrentTime() !== undefined"
          id="video"
          [currentTime]="getVideoCurrentTime()"
          [src]="videoUrl"></video>
        <div *ngIf="getVideoCurrentTime() === undefined" class="no-video-message">
          <p>No screenrecording frame to show</p>
          <p>Current timestamp before first screenrecording frame.</p>
        </div>
      </div>
      <expanded-timeline
        [timelineData]="timelineData"
        (onTracePositionUpdate)="updatePosition($event)"
        (onScrollEvent)="updateScrollEvent($event)"
        id="expanded-timeline"></expanded-timeline>
    </div>
    <div class="navbar-toggle">
    <div id="toggle" *ngIf="timelineData.hasMoreThanOneDistinctTimestamp()">
      <button
        mat-icon-button
        [class]="TOGGLE_BUTTON_CLASS"
        color="basic"
        aria-label="Toggle Expanded Timeline"
        (click)="toggleExpand()">
          <mat-icon *ngIf="!expanded" class="material-symbols-outlined">expand_circle_up</mat-icon>
          <mat-icon *ngIf="expanded" class="material-symbols-outlined">expand_circle_down</mat-icon>
        </button>
    </div>
      <div class="navbar" #collapsedTimeline>
        <ng-template [ngIf]="timelineData.hasMoreThanOneDistinctTimestamp()">
          <div id="time-selector">
            <form [formGroup]="timestampForm" class="time-selector-form">
              <mat-form-field
                class="time-input elapsed"
                appearance="fill"
                (keydown.enter)="onKeydownEnterElapsedTimeInputField($event)"
                (change)="onHumanElapsedTimeInputChange($event)"
                *ngIf="!usingRealtime()">
                <input
                  matInput
                  name="humanElapsedTimeInput"
                  [formControl]="selectedElapsedTimeFormControl" />
              </mat-form-field>
              <mat-form-field
                class="time-input real"
                appearance="fill"
                (keydown.enter)="onKeydownEnterRealTimeInputField($event)"
                (change)="onHumanRealTimeInputChange($event)"
                *ngIf="usingRealtime()">
                <input
                  matInput
                  name="humanRealTimeInput"
                  [formControl]="selectedRealTimeFormControl" />
              </mat-form-field>
              <mat-form-field
                class="time-input nano"
                appearance="fill"
                (keydown.enter)="onKeydownEnterNanosecondsTimeInputField($event)"
                (change)="onNanosecondsInputTimeChange($event)">
                <input matInput name="nsTimeInput" [formControl]="selectedNsFormControl" />
              </mat-form-field>
            </form>
            <div class="trace-playback">
              <button
                mat-icon-button
                id="prev_entry_button"
                color="primary"
                (click)="moveToPreviousEntry()"
                [disabled]="!hasPrevEntry()">
                <mat-icon>chevron_left</mat-icon>
              </button>
              <button
                mat-icon-button
                id="next_entry_button"
                color="primary"
                (click)="moveToNextEntry()"
                [disabled]="!hasNextEntry()">
                <mat-icon>chevron_right</mat-icon>
              </button>
              </div>
          </div>
          <div id="trace-selector">
            <mat-form-field appearance="none">
              <mat-select #traceSelector [formControl]="selectedTracesFormControl" multiple>
                <div class="select-traces-panel">
                  <div class="tip">Filter traces in the timeline</div>
                  <mat-option
                    *ngFor="let trace of sortedAvailableTraces"
                    [value]="trace"
                    [style]="{
                      color: '#6983FF',
                      opacity: isOptionDisabled(trace) ? 0.5 : 1.0
                    }"
                    [disabled]="isOptionDisabled(trace)"
                    (click)="applyNewTraceSelection()">
                    <mat-icon
                      [style]="{
                        color: TRACE_INFO[trace].color
                      }"
                    >{{ TRACE_INFO[trace].icon }}</mat-icon>
                    {{ TRACE_INFO[trace].name }}
                  </mat-option>
                  <div class="actions">
                    <button mat-flat-button color="primary" (click)="traceSelector.close()">
                      Done
                    </button>
                  </div>
                </div>
                <mat-select-trigger class="shown-selection">
                  <div class="filter-header">
                    <span class="mat-body-2"> Filter </span>
                    <mat-icon class="material-symbols-outlined">expand_circle_up</mat-icon>
                  </div>

                  <div class="trace-icons">
                    <mat-icon
                      class="trace-icon"
                      *ngFor="let selectedTrace of getSelectedTracesToShow()"
                      [style]="{color: TRACE_INFO[selectedTrace].color}">
                      {{ TRACE_INFO[selectedTrace].icon }}
                    </mat-icon>
                    <mat-icon
                      class="trace-icon"
                      *ngIf="selectedTraces.length > 8">
                      more_horiz
                    </mat-icon>
                  </div>
                </mat-select-trigger>
              </mat-select>
            </mat-form-field>
          </div>
          <mini-timeline
            [timelineData]="timelineData"
            [currentTracePosition]="getCurrentTracePosition()"
            [selectedTraces]="selectedTraces"
            [expandedTimelineScrollEvent]="expandedTimelineScrollEvent"
            (onTracePositionUpdate)="updatePosition($event)"
            (onSeekTimestampUpdate)="updateSeekTimestamp($event)"
            id="mini-timeline"
            #miniTimeline></mini-timeline>
        </ng-template>
        <div *ngIf="!timelineData.hasTimestamps()" class="no-timestamps-msg">
          <p class="mat-body-2">No timeline to show!</p>
          <p class="mat-body-1">All loaded traces contain no timestamps.</p>
        </div>
        <div
          *ngIf="timelineData.hasTimestamps() && !timelineData.hasMoreThanOneDistinctTimestamp()"
          class="no-timestamps-msg">
          <p class="mat-body-2">No timeline to show!</p>
          <p class="mat-body-1">Only a single timestamp has been recorded.</p>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .navbar-toggle {
        display: flex;
        flex-direction: column;
        align-items: end;
        position: relative;
      }
      #toggle {
        width: fit-content;
        position: absolute;
        top: -41px;
        z-index: 1000;
        border: 1px solid #3333;
        border-bottom: 0px;
        border-right: 0px;
        border-top-left-radius: 6px;
        border-top-right-radius: 6px;
        background-color: #fafafa;
      }
      .navbar {
        display: flex;
        width: 100%;
        flex-direction: row;
        align-items: center;
        justify-content: center;
      }
      #expanded-nav {
        display: flex;
        border-bottom: 1px solid #3333;
      }
      #time-selector {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
      }
      #time-selector .mat-form-field-infix {
        padding: 0 0.75rem 0 0.5rem;
        border-top: unset;
      }
      .time-selector-form {
        display: flex;
        flex-direction: column;
        height: 60px;
        width: 282px;
        border-radius: 10px;
        background-color: #EEEFF0;
      }
      .time-selector-form .time-input {
        width: 100%;
        margin-bottom: -1.34375em;
        text-align: center;
      }
      .shown-selection .trace-icon {
        font-size: 18px;
        width: 18px;
        height: 18px;
        padding-left: 4px;
        padding-right: 4px;
        padding-top: 2px;
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
        width: 80px;
        padding: 0 0.75rem 0 0.5rem;
        border-top: unset;
      }
      #trace-selector .shown-selection {
        height: 116px;
        border-radius: 10px;
        background-color: #EEEFF0;
        display: flex;
        justify-content: center;
        flex-wrap: wrap;
        align-content: flex-start;
      }
      #trace-selector .filter-header {
        padding-top: 4px;
        display: flex;
        gap: 2px;
      }
      #trace-selector .shown-selection .trace-icons {
        display: flex;
        justify-content: center;
        flex-wrap: wrap;
        align-content: flex-start;
        width: 70%;
      }
      #trace-selector .mat-select-trigger {
        height: unset;
        flex-direction: column-reverse;
      }
      #trace-selector .mat-select-arrow-wrapper {
        display: none;
      }
      #trace-selector .mat-form-field-wrapper {
        padding: 0;
      }
      :has(>.select-traces-panel) {
        max-height: unset !important;
        font-family: 'Roboto', sans-serif;
        position: relative;
        bottom: 120px;
      }
      .tip {
        padding: 16px;
        font-weight: 300;
      }
      .actions {
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
    `,
  ],
})
export class TimelineComponent
  implements WinscopeEventEmitter, WinscopeEventListener
{
  readonly TOGGLE_BUTTON_CLASS: string = 'button-toggle-expansion';
  readonly MAX_SELECTED_TRACES = 3;

  @Input() set activeViewTraceTypes(types: TraceType[] | undefined) {
    if (!types) {
      return;
    }

    if (types.length !== 1) {
      throw Error(
        "Timeline component doesn't support viewers with dependencies length !== 1",
      );
    }

    this.internalActiveTrace = types[0];

    if (!this.selectedTraces.includes(this.internalActiveTrace)) {
      // Create new object to make sure we trigger an update on Mini Timeline child component
      this.selectedTraces = [...this.selectedTraces, this.internalActiveTrace];
      this.selectedTracesFormControl.setValue(this.selectedTraces);
    }
  }

  @Input() timelineData: TimelineData | undefined;
  @Input() availableTraces: TraceType[] = [];

  @Output() readonly collapsedTimelineSizeChanged = new EventEmitter<number>();

  @ViewChild('collapsedTimeline') private collapsedTimelineRef:
    | ElementRef
    | undefined;

  videoUrl: SafeUrl | undefined;

  internalActiveTrace: TraceType | undefined = undefined;
  selectedTraces: TraceType[] = [];
  sortedAvailableTraces: TraceType[] = [];
  selectedTracesFormControl = new FormControl<TraceType[]>([]);
  selectedElapsedTimeFormControl = new FormControl(
    'undefined',
    Validators.compose([
      Validators.required,
      Validators.pattern(TimestampUtils.HUMAN_ELAPSED_TIMESTAMP_REGEX),
    ]),
  );
  selectedRealTimeFormControl = new FormControl(
    'undefined',
    Validators.compose([
      Validators.required,
      Validators.pattern(TimestampUtils.HUMAN_REAL_TIMESTAMP_REGEX),
    ]),
  );
  selectedNsFormControl = new FormControl(
    'undefined',
    Validators.compose([
      Validators.required,
      Validators.pattern(TimestampUtils.NS_TIMESTAMP_REGEX),
    ]),
  );
  timestampForm = new FormGroup({
    selectedElapsedTime: this.selectedElapsedTimeFormControl,
    selectedRealTime: this.selectedRealTimeFormControl,
    selectedNs: this.selectedNsFormControl,
  });
  TRACE_INFO = TRACE_INFO;
  isInputFormFocused = false;

  private expanded = false;
  private emitEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;
  private expandedTimelineScrollEvent: WheelEvent | undefined;
  private seekTracePosition?: TracePosition;

  constructor(
    @Inject(DomSanitizer) private sanitizer: DomSanitizer,
    @Inject(ChangeDetectorRef) private changeDetectorRef: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    const timelineData = assertDefined(this.timelineData);
    if (timelineData.hasTimestamps()) {
      this.updateTimeInputValuesToCurrentTimestamp();
    }

    const screenRecordingVideo = timelineData.getScreenRecordingVideo();
    if (screenRecordingVideo) {
      this.videoUrl = this.sanitizer.bypassSecurityTrustUrl(
        URL.createObjectURL(screenRecordingVideo),
      );
    }

    this.sortedAvailableTraces = this.availableTraces.sort((a, b) =>
      TraceTypeUtils.compareByDisplayOrder(a, b),
    ); // to display in fixed order corresponding to viewer tabs
    this.selectedTraces = this.sortedAvailableTraces;
    this.selectedTracesFormControl = new FormControl<TraceType[]>(
      this.selectedTraces,
    );
  }

  ngAfterViewInit() {
    const height = assertDefined(this.collapsedTimelineRef).nativeElement
      .offsetHeight;
    this.collapsedTimelineSizeChanged.emit(height);
  }

  setEmitEvent(callback: EmitEvent) {
    this.emitEvent = callback;
  }

  getVideoCurrentTime() {
    return assertDefined(
      this.timelineData,
    ).searchCorrespondingScreenRecordingTimeSeconds(
      this.getCurrentTracePosition(),
    );
  }

  getCurrentTracePosition(): TracePosition {
    if (this.seekTracePosition) {
      return this.seekTracePosition;
    }

    const position = assertDefined(this.timelineData).getCurrentPosition();
    if (position === undefined) {
      throw Error(
        'A trace position should be available by the time the timeline is loaded',
      );
    }

    return position;
  }

  getSelectedTracesToShow(): TraceType[] {
    const sortedSelectedTraces = this.getSelectedTracesSortedByDisplayOrder();
    return sortedSelectedTraces.length > 8
      ? sortedSelectedTraces.slice(0, 7)
      : sortedSelectedTraces.slice(0, 8);
  }

  async onWinscopeEvent(event: WinscopeEvent) {
    await event.visit(WinscopeEventType.TRACE_POSITION_UPDATE, async () => {
      this.updateTimeInputValuesToCurrentTimestamp();
    });
  }

  async toggleExpand() {
    this.expanded = !this.expanded;
    this.changeDetectorRef.detectChanges();
    await this.emitEvent(new ExpandedTimelineToggled(this.expanded));
  }

  async updatePosition(position: TracePosition) {
    assertDefined(this.timelineData).setPosition(position);
    await this.emitEvent(new TracePositionUpdate(position));
  }

  usingRealtime(): boolean {
    return (
      assertDefined(this.timelineData).getTimestampType() === TimestampType.REAL
    );
  }

  updateSeekTimestamp(timestamp: Timestamp | undefined) {
    if (timestamp) {
      this.seekTracePosition = assertDefined(
        this.timelineData,
      ).makePositionFromActiveTrace(timestamp);
    } else {
      this.seekTracePosition = undefined;
    }
    this.updateTimeInputValuesToCurrentTimestamp();
  }

  isOptionDisabled(trace: TraceType) {
    return this.internalActiveTrace === trace;
  }

  applyNewTraceSelection() {
    this.selectedTraces =
      this.selectedTracesFormControl.value ?? this.sortedAvailableTraces;
  }

  @HostListener('document:focusin', ['$event'])
  handleFocusInEvent(event: FocusEvent) {
    if (
      (event.target as HTMLInputElement)?.tagName === 'INPUT' &&
      (event.target as HTMLInputElement)?.type === 'text'
    ) {
      //check if text input field focused
      this.isInputFormFocused = true;
    }
  }

  @HostListener('document:focusout', ['$event'])
  handleFocusOutEvent(event: FocusEvent) {
    if (
      (event.target as HTMLInputElement)?.tagName === 'INPUT' &&
      (event.target as HTMLInputElement)?.type === 'text'
    ) {
      //check if text input field focused
      this.isInputFormFocused = false;
    }
  }

  @HostListener('document:keydown', ['$event'])
  async handleKeyboardEvent(event: KeyboardEvent) {
    if (
      this.isInputFormFocused ||
      !assertDefined(this.timelineData).hasTimestamps()
    ) {
      return;
    }
    if (event.key === 'ArrowLeft') {
      await this.moveToPreviousEntry();
    } else if (event.key === 'ArrowRight') {
      await this.moveToNextEntry();
    }
  }

  hasPrevEntry(): boolean {
    if (this.internalActiveTrace === undefined) {
      return false;
    }
    if (
      assertDefined(this.timelineData)
        .getTraces()
        .getTrace(this.internalActiveTrace) === undefined
    ) {
      return false;
    }
    return (
      assertDefined(this.timelineData).getPreviousEntryFor(
        this.internalActiveTrace,
      ) !== undefined
    );
  }

  hasNextEntry(): boolean {
    if (this.internalActiveTrace === undefined) {
      return false;
    }
    if (
      assertDefined(this.timelineData)
        .getTraces()
        .getTrace(this.internalActiveTrace) === undefined
    ) {
      return false;
    }
    return (
      assertDefined(this.timelineData).getNextEntryFor(
        this.internalActiveTrace,
      ) !== undefined
    );
  }

  async moveToPreviousEntry() {
    if (this.internalActiveTrace === undefined) {
      return;
    }
    const timelineData = assertDefined(this.timelineData);
    timelineData.moveToPreviousEntryFor(this.internalActiveTrace);
    const position = assertDefined(timelineData.getCurrentPosition());
    await this.emitEvent(new TracePositionUpdate(position));
  }

  async moveToNextEntry() {
    if (this.internalActiveTrace === undefined) {
      return;
    }
    const timelineData = assertDefined(this.timelineData);
    timelineData.moveToNextEntryFor(this.internalActiveTrace);
    const position = assertDefined(timelineData.getCurrentPosition());
    await this.emitEvent(new TracePositionUpdate(position));
  }

  async onHumanElapsedTimeInputChange(event: Event) {
    if (event.type !== 'change' || !this.selectedElapsedTimeFormControl.valid) {
      return;
    }
    const target = event.target as HTMLInputElement;
    const timestamp = TimestampUtils.parseHumanElapsed(target.value);
    await this.updatePosition(
      assertDefined(this.timelineData).makePositionFromActiveTrace(timestamp),
    );
    this.updateTimeInputValuesToCurrentTimestamp();
  }

  async onHumanRealTimeInputChange(event: Event) {
    if (event.type !== 'change' || !this.selectedRealTimeFormControl.valid) {
      return;
    }
    const target = event.target as HTMLInputElement;

    const timestamp = TimestampUtils.parseHumanReal(target.value);
    await this.updatePosition(
      assertDefined(this.timelineData).makePositionFromActiveTrace(timestamp),
    );
    this.updateTimeInputValuesToCurrentTimestamp();
  }

  async onNanosecondsInputTimeChange(event: Event) {
    if (event.type !== 'change' || !this.selectedNsFormControl.valid) {
      return;
    }
    const target = event.target as HTMLInputElement;
    const timelineData = assertDefined(this.timelineData);

    const timestamp = NO_TIMEZONE_OFFSET_FACTORY.makeTimestampFromType(
      assertDefined(timelineData.getTimestampType()),
      StringUtils.parseBigIntStrippingUnit(target.value),
      0n,
    );
    await this.updatePosition(
      timelineData.makePositionFromActiveTrace(timestamp),
    );
    this.updateTimeInputValuesToCurrentTimestamp();
  }

  onKeydownEnterElapsedTimeInputField(event: KeyboardEvent) {
    if (this.selectedElapsedTimeFormControl.valid) {
      (event.target as HTMLInputElement).blur();
    }
  }

  onKeydownEnterRealTimeInputField(event: KeyboardEvent) {
    if (this.selectedRealTimeFormControl.valid) {
      (event.target as HTMLInputElement).blur();
    }
  }

  onKeydownEnterNanosecondsTimeInputField(event: KeyboardEvent) {
    if (this.selectedNsFormControl.valid) {
      (event.target as HTMLInputElement).blur();
    }
  }

  updateScrollEvent(event: WheelEvent) {
    this.expandedTimelineScrollEvent = event;
  }

  private updateTimeInputValuesToCurrentTimestamp() {
    const currentNs = this.getCurrentTracePosition().timestamp.getValueNs();
    this.selectedElapsedTimeFormControl.setValue(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(currentNs),
        false,
      ),
    );
    this.selectedRealTimeFormControl.setValue(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(currentNs),
      ),
    );
    this.selectedNsFormControl.setValue(
      `${this.getCurrentTracePosition().timestamp.getValueNs()} ns`,
    );
  }

  private getSelectedTracesSortedByDisplayOrder(): TraceType[] {
    return this.selectedTraces
      .slice()
      .sort((a, b) => TraceTypeUtils.compareByDisplayOrder(a, b));
  }
}
