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
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {Color} from 'app/colors';
import {TimelineData} from 'app/timeline_data';
import {TRACE_INFO} from 'app/trace_info';
import {assertDefined} from 'common/assert_utils';
import {FunctionUtils} from 'common/function_utils';
import {PersistentStore} from 'common/persistent_store';
import {StringUtils} from 'common/string_utils';
import {TimeRange, Timestamp} from 'common/time';
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
import {multlineTooltip} from 'viewers/components/styles/tooltip.styles';

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
        [store]="store"
        (onTracePositionUpdate)="updatePosition($event)"
        (onScrollEvent)="updateScrollEvent($event)"
        id="expanded-timeline"></expanded-timeline>
    </div>
    <div class="navbar-toggle">
    <div id="toggle" [style.background-color]="getAppBackgroundColor()" *ngIf="timelineData.hasMoreThanOneDistinctTimestamp()">
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
          <div id="time-selector" [style.background-color]="getNavbarBlockColor()">
            <form [formGroup]="timestampForm" class="time-selector-form">
              <mat-form-field
                class="time-input human"
                appearance="fill"
                (keydown.enter)="onKeydownEnterTimeInputField($event)"
                (change)="onHumanTimeInputChange($event)">
                <mat-icon
                  [matTooltip]="getHumanTimeTooltip()"
                  matTooltipClass="multline-tooltip"
                  matPrefix>schedule</mat-icon>
                <input
                  matInput
                  name="humanTimeInput"
                  [formControl]="selectedTimeFormControl" />
                <div class="field-suffix" matSuffix>
                  <span class="time-difference"> {{ getUTCOffset() }} </span>
                  <button
                    mat-icon-button
                    [matTooltip]="getCopyHumanTimeTooltip()"
                    matTooltipClass="multline-tooltip"
                    [cdkCopyToClipboard]="getHumanTime()"
                    matSuffix>
                    <mat-icon>content_copy</mat-icon>
                  </button>
                </div>
              </mat-form-field>
              <mat-form-field
                class="time-input nano"
                appearance="fill"
                (keydown.enter)="onKeydownEnterNanosecondsTimeInputField($event)"
                (change)="onNanosecondsInputTimeChange($event)">
                <mat-icon class="material-symbols-outlined" matPrefix>timer</mat-icon>
                <input matInput name="nsTimeInput" [formControl]="selectedNsFormControl" />
                <div class="field-suffix" matSuffix>
                  <button
                    mat-icon-button
                    [matTooltip]="getCopyPositionTooltip(selectedNsFormControl.value)"
                    matTooltipClass="multline-tooltip"
                    [cdkCopyToClipboard]="selectedNsFormControl.value"
                    matSuffix>
                    <mat-icon>content_copy</mat-icon>
                  </button>
                </div>
              </mat-form-field>
            </form>
            <div class="time-controls" [style.background-color]="getNavbarInnerBlockColor()">
              <button
                mat-icon-button
                id="prev_entry_button"
                matTooltip="Go to previous entry"
                (click)="moveToPreviousEntry()"
                [class.disabled]="!hasPrevEntry()"
                [disabled]="!hasPrevEntry()">
                <mat-icon>chevron_left</mat-icon>
              </button>
              <button
                mat-icon-button
                id="next_entry_button"
                matTooltip="Go to next entry"
                (click)="moveToNextEntry()"
                [class.disabled]="!hasNextEntry()"
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
                      color: getTraceSelectorTextColor(),
                      opacity: isOptionDisabled(trace) ? 0.5 : 1.0
                    }"
                    [disabled]="isOptionDisabled(trace)"
                    (click)="applyNewTraceSelection(trace)">
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
                <mat-select-trigger class="shown-selection" [style.background-color]="getNavbarBlockColor()">
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
            [initialZoom]="initialZoom"
            [expandedTimelineScrollEvent]="expandedTimelineScrollEvent"
            [store]="store"
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
        border-radius: 10px;
        margin-left: 0.5rem;
        height: 116px;
        width: 282px;
      }
      #time-selector .mat-form-field-wrapper {
        width: 100%;
      }
      #time-selector .mat-form-field-infix, #trace-selector .mat-form-field-infix {
        padding: 0 0.75rem 0 0.5rem !important;
        border-top: unset;
      }
      #time-selector .mat-form-field-flex, #time-selector .field-suffix {
        border-radius: 0;
        padding: 0;
        display: flex;
        align-items: center;
      }
      .time-selector-form {
        display: flex;
        flex-direction: column;
        height: 60px;
        width: 90%;
        justify-content: center;
        align-items: center;
        gap: 5px;
      }
      .time-selector-form mat-form-field {
        margin-bottom: -1.34375em;
        display: flex;
        width: 100%;
        font-size: 12px;
      }
      .time-selector-form input {
        text-overflow: ellipsis;
        font-weight: bold;
      }
      .time-selector-form .time-difference {
        padding-right: 2px;
      }
      #time-selector .time-controls {
        border-radius: 10px;
        margin: 0.5rem;
        display: flex;
        flex-direction: row;
        justify-content: space-between;
        width: 90%;
      }
      #time-selector .mat-icon-button {
        width: 24px;
        height: 24px;
        padding-left: 3px;
        padding-right: 3px;
      }
      #time-selector .mat-icon {
        font-size: 18px;
        width: 18px;
        height: 18px;
        line-height: 18px;
        display: flex;
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
      }
      #trace-selector .shown-selection {
        height: 116px;
        border-radius: 10px;
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
      .shown-selection .trace-icons {
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
    multlineTooltip,
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
  @Input() store: PersistentStore | undefined;

  @Output() readonly collapsedTimelineSizeChanged = new EventEmitter<number>();

  @ViewChild('collapsedTimeline') private collapsedTimelineRef:
    | ElementRef
    | undefined;

  videoUrl: SafeUrl | undefined;

  internalActiveTrace: TraceType | undefined = undefined;
  initialZoom: TimeRange | undefined = undefined;
  selectedTraces: TraceType[] = [];
  sortedAvailableTraces: TraceType[] = [];
  selectedTracesFormControl = new FormControl<TraceType[]>([]);
  selectedTimeFormControl = new FormControl('undefined');
  selectedNsFormControl = new FormControl(
    'undefined',
    Validators.compose([Validators.required, this.validateNsFormat]),
  );
  timestampForm = new FormGroup({
    selectedTime: this.selectedTimeFormControl,
    selectedNs: this.selectedNsFormControl,
  });
  TRACE_INFO = TRACE_INFO;
  isInputFormFocused = false;
  storeKeyDeselectedTraces = 'miniTimeline.deselectedTraces';

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
    const converter = assertDefined(timelineData.getTimestampConverter());
    const validatorFn: ValidatorFn = (control: AbstractControl) => {
      const valid = converter.validateHumanInput(control.value ?? '');
      return !valid ? {invalidInput: control.value} : null;
    };
    this.selectedTimeFormControl.addValidators(
      assertDefined(Validators.compose([Validators.required, validatorFn])),
    );

    const screenRecordingVideo = timelineData.getScreenRecordingVideo();
    if (screenRecordingVideo) {
      this.videoUrl = this.sanitizer.bypassSecurityTrustUrl(
        URL.createObjectURL(screenRecordingVideo),
      );
    }

    this.sortedAvailableTraces = this.availableTraces.sort((a, b) =>
      TraceTypeUtils.compareByDisplayOrder(a, b),
    ); // to display in fixed order corresponding to viewer tabs

    const storedDeselectedTraces = this.getStoredDeselectedTraces();
    this.selectedTraces = this.sortedAvailableTraces.filter(
      (availableTrace) => {
        return !storedDeselectedTraces.includes(availableTrace);
      },
    );
    this.selectedTracesFormControl = new FormControl<TraceType[]>(
      this.selectedTraces,
    );

    const initialTraceToCropZoom = this.sortedAvailableTraces.find((type) => {
      return (
        type !== TraceType.SCREEN_RECORDING &&
        TraceTypeUtils.isTraceTypeWithViewer(type) &&
        timelineData.getTraces().getTrace(type)
      );
    });
    if (initialTraceToCropZoom !== undefined) {
      const trace = assertDefined(
        timelineData.getTraces().getTrace(initialTraceToCropZoom),
      );
      this.initialZoom = {
        from: trace.getEntry(0).getTimestamp(),
        to: timelineData.getFullTimeRange().to,
      };
    }
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

  applyNewTraceSelection(clickedType: TraceType) {
    this.selectedTraces =
      this.selectedTracesFormControl.value ?? this.sortedAvailableTraces;
    this.updateStoredDeselectedTraces(clickedType);
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

  async onHumanTimeInputChange(event: Event) {
    if (event.type !== 'change' || !this.selectedTimeFormControl.valid) {
      return;
    }
    const target = event.target as HTMLInputElement;
    let input = target.value;
    // if hh:mm:ss.zz format, append date of current timestamp
    if (TimestampUtils.isRealTimeOnlyFormat(input)) {
      const date = assertDefined(
        TimestampUtils.extractDateFromHumanTimestamp(
          this.getCurrentTracePosition().timestamp.format(),
        ),
      );
      input = date + 'T' + input;
    }
    const timelineData = assertDefined(this.timelineData);
    const timestamp = assertDefined(
      timelineData.getTimestampConverter(),
    ).makeTimestampFromHuman(input);
    await this.updatePosition(
      timelineData.makePositionFromActiveTrace(timestamp),
    );
    this.updateTimeInputValuesToCurrentTimestamp();
  }

  async onNanosecondsInputTimeChange(event: Event) {
    if (event.type !== 'change' || !this.selectedNsFormControl.valid) {
      return;
    }
    const target = event.target as HTMLInputElement;
    const timelineData = assertDefined(this.timelineData);

    const timestamp = assertDefined(
      timelineData.getTimestampConverter(),
    ).makeTimestampFromNs(StringUtils.parseBigIntStrippingUnit(target.value));
    await this.updatePosition(
      timelineData.makePositionFromActiveTrace(timestamp),
    );
    this.updateTimeInputValuesToCurrentTimestamp();
  }

  onKeydownEnterTimeInputField(event: KeyboardEvent) {
    if (this.selectedTimeFormControl.valid) {
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

  getCopyPositionTooltip(position: string): string {
    return `Copy current position:\n${position}`;
  }

  getHumanTimeTooltip(): string {
    const [date, time] = this.getCurrentTracePosition()
      .timestamp.format()
      .split(', ');
    return `
      Date: ${date}
      Time: ${time}\xa0\xa0\xa0\xa0${this.getUTCOffset()}

      Edit field to update position by inputting time as
      "hh:mm:ss.zz", "YYYY-MM-DDThh:mm:ss.zz", or "YYYY-MM-DD, hh:mm:ss.zz"
    `;
  }

  getCopyHumanTimeTooltip(): string {
    return this.getCopyPositionTooltip(this.getHumanTime());
  }

  getHumanTime(): string {
    return this.getCurrentTracePosition().timestamp.format();
  }

  getUTCOffset(): string {
    return assertDefined(
      this.timelineData?.getTimestampConverter(),
    ).getUTCOffset();
  }

  getAppBackgroundColor(): string {
    return this.isDarkMode()
      ? Color.APP_BACKGROUND_DARK_MODE
      : Color.APP_BACKGROUND_LIGHT_MODE;
  }

  getNavbarBlockColor(): string {
    return this.isDarkMode()
      ? Color.NAVBAR_BLOCK_DARK_MODE
      : Color.NAVBAR_BLOCK_LIGHT_MODE;
  }

  getNavbarInnerBlockColor(): string {
    return this.isDarkMode()
      ? Color.NAVBAR_INNER_BLOCK_DARK_MODE
      : Color.NAVBAR_INNER_BLOCK_LIGHT_MODE;
  }

  getTraceSelectorTextColor(): string {
    return this.isDarkMode()
      ? Color.TRACE_SELECTOR_TEXT_DARK_MODE
      : Color.TRACE_SELECTOR_TEXT_LIGHT_MODE;
  }

  private updateTimeInputValuesToCurrentTimestamp() {
    const currentTimestampNs =
      this.getCurrentTracePosition().timestamp.getValueNs();
    const timelineData = assertDefined(this.timelineData);

    let formattedCurrentTimestamp = assertDefined(
      timelineData.getTimestampConverter(),
    )
      .makeTimestampFromNs(currentTimestampNs)
      .format();
    if (TimestampUtils.isHumanRealTimestampFormat(formattedCurrentTimestamp)) {
      formattedCurrentTimestamp = assertDefined(
        TimestampUtils.extractTimeFromHumanTimestamp(formattedCurrentTimestamp),
      );
    }
    this.selectedTimeFormControl.setValue(formattedCurrentTimestamp);
    this.selectedNsFormControl.setValue(`${currentTimestampNs} ns`);
  }

  private getSelectedTracesSortedByDisplayOrder(): TraceType[] {
    return this.selectedTraces
      .slice()
      .sort((a, b) => TraceTypeUtils.compareByDisplayOrder(a, b));
  }

  private getStoredDeselectedTraces(): TraceType[] {
    const storedDeselectedTraces = this.store?.get(
      this.storeKeyDeselectedTraces,
    );
    return JSON.parse(storedDeselectedTraces ?? '[]');
  }

  private updateStoredDeselectedTraces(clickedType: TraceType) {
    if (!this.store) {
      return;
    }

    let storedTraces = this.getStoredDeselectedTraces();
    if (
      this.selectedTraces.includes(clickedType) &&
      storedTraces.includes(clickedType)
    ) {
      storedTraces = storedTraces.filter(
        (storedTrace) => storedTrace !== clickedType,
      );
    } else if (
      !this.selectedTraces.includes(clickedType) &&
      !storedTraces.includes(clickedType)
    ) {
      storedTraces.push(clickedType);
    }

    this.store.add(this.storeKeyDeselectedTraces, JSON.stringify(storedTraces));
  }

  private validateNsFormat(control: FormControl): ValidationErrors | null {
    const valid = TimestampUtils.isNsFormat(control.value ?? '');
    return !valid ? {invalidInput: control.value} : null;
  }

  private isDarkMode(): boolean {
    return this.store?.get('dark-mode') === 'true';
  }
}
