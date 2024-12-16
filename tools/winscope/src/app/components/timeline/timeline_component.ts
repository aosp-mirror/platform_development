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
import {TimelineData} from 'app/timeline_data';
import {assertDefined} from 'common/assert_utils';
import {FunctionUtils} from 'common/function_utils';
import {PersistentStore} from 'common/persistent_store';
import {StringUtils} from 'common/string_utils';
import {TimeRange, Timestamp, TimestampFormatType} from 'common/time';
import {TimestampUtils} from 'common/timestamp_utils';
import {Analytics} from 'logging/analytics';
import {
  ActiveTraceChanged,
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
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TRACE_INFO} from 'trace/trace_info';
import {TracePosition} from 'trace/trace_position';
import {TraceType, TraceTypeUtils} from 'trace/trace_type';
import {multlineTooltip} from 'viewers/components/styles/tooltip.styles';
import {MiniTimelineComponent} from './mini-timeline/mini_timeline_component';

@Component({
  selector: 'timeline',
  encapsulation: ViewEncapsulation.None,
  template: `
    <div
      *ngIf="isDisabled"
      class="disabled-message user-notification mat-body-1"> Timeline disabled due to ongoing search query </div>
    <div [class.disabled-component]="isDisabled">
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
          (onTraceClicked)="onExpandedTimelineTraceClicked($event)"
          (onMouseXRatioUpdate)="updateExpandedTimelineMouseXRatio($event)"
          id="expanded-timeline"></expanded-timeline>
      </div>
      <div class="navbar-toggle">
        <div class="navbar" #collapsedTimeline>
          <ng-template [ngIf]="timelineData.hasTimestamps()">
            <div id="time-selector">
              <form [formGroup]="timestampForm" class="time-selector-form">
                <mat-form-field
                  class="time-input human"
                  appearance="fill"
                  (keydown.esc)="$event.target.blur()"
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
                      (cdkCopyToClipboardCopied)="onTimeCopied('human')"
                      matSuffix>
                      <mat-icon>content_copy</mat-icon>
                    </button>
                  </div>
                </mat-form-field>
                <mat-form-field
                  class="time-input nano"
                  appearance="fill"
                  (keydown.esc)="$event.target.blur()"
                  (keydown.enter)="onKeydownEnterNanosecondsTimeInputField($event)"
                  (change)="onNanosecondsInputTimeChange($event)">
                  <mat-icon
                    class="bookmark-icon"
                    [class.material-symbols-outlined]="!currentPositionBookmarked()"
                    matTooltip="bookmark timestamp"
                    (click)="toggleBookmarkCurrentPosition($event)"
                    matPrefix>flag</mat-icon>
                  <input matInput name="nsTimeInput" [formControl]="selectedNsFormControl" />
                  <div class="field-suffix" matSuffix>
                    <button
                      mat-icon-button
                      [matTooltip]="getCopyPositionTooltip(selectedNsFormControl.value)"
                      matTooltipClass="multline-tooltip"
                      [cdkCopyToClipboard]="selectedNsFormControl.value"
                      (cdkCopyToClipboardCopied)="onTimeCopied('ns')"
                      matSuffix>
                      <mat-icon>content_copy</mat-icon>
                    </button>
                  </div>
                </mat-form-field>
              </form>
              <div class="time-controls">
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
                      *ngFor="let trace of sortedTraces"
                      [value]="trace"
                      [matTooltip]="trace.getDescriptors().join(', ')"
                      matTooltipPosition="right"
                      [style]="{
                        color: 'var(--blue-text-color)',
                        opacity: isOptionDisabled(trace) ? 0.5 : 1.0
                      }"
                      [disabled]="isOptionDisabled(trace)"
                      (click)="applyNewTraceSelection(trace)">
                      <mat-icon
                        [style]="{
                          color: TRACE_INFO[trace.type].color
                        }"
                      >{{ TRACE_INFO[trace.type].icon }}</mat-icon>
                      {{ getTitle(trace) }}
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
                        [style]="{color: TRACE_INFO[selectedTrace.type].color}"
                        [matTooltip]="getTraceTooltip(selectedTrace)"
                        #tooltip="matTooltip"
                        (mouseenter)="tooltip.disabled = false"
                        (mouseleave)="tooltip.disabled = true">
                        {{ TRACE_INFO[selectedTrace.type].icon }}
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
              *ngIf="timelineData.hasMoreThanOneDistinctTimestamp()"
              [timelineData]="timelineData"
              [currentTracePosition]="getCurrentTracePosition()"
              [selectedTraces]="selectedTraces"
              [initialZoom]="initialZoom"
              [expandedTimelineScrollEvent]="expandedTimelineScrollEvent"
              [expandedTimelineMouseXRatio]="expandedTimelineMouseXRatio"
              [bookmarks]="bookmarks"
              [store]="store"
              (onTracePositionUpdate)="updatePosition($event)"
              (onSeekTimestampUpdate)="updateSeekTimestamp($event)"
              (onRemoveAllBookmarks)="removeAllBookmarks()"
              (onToggleBookmark)="toggleBookmarkRange($event.range, $event.rangeContainsBookmark)"
              (onTraceClicked)="onMiniTimelineTraceClicked($event)"
              id="mini-timeline"
              #miniTimeline></mini-timeline>
          </ng-template>
          <div
            *ngIf="!timelineData.hasMoreThanOneDistinctTimestamp()"
            class="no-timeline-msg">
              <p class="mat-body-2">No timeline to show!</p>
              <p
                *ngIf="timelineData.hasTimestamps()"
                class="mat-body-1">Only a single timestamp has been recorded.</p>
              <p
                *ngIf="!timelineData.hasTimestamps()"
                class="mat-body-1">All loaded traces contain no timestamps.</p>
          </div>
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
        max-height: 20vh;
        overflow: auto;
      }
      #toggle {
        width: fit-content;
        position: absolute;
        top: -41px;
        right: 0px;
        z-index: 1000;
        border: 1px solid #3333;
        border-bottom: 0px;
        border-right: 0px;
        border-top-left-radius: 6px;
        border-top-right-radius: 6px;
        background-color: var(--drawer-color);
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
        flex-direction: row;
        border-bottom: 1px solid #3333;
        border-top: 1px solid #3333;
        max-height: 60vh;
        overflow: hidden;
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
        background-color: var(--drawer-block-primary);
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
      .bookmark-icon {
        cursor: pointer;
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
        background-color: var(--drawer-block-secondary);
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
        max-height: 60vh;
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
      #expanded-timeline {
        flex-grow: 1;
        overflow-y: auto;
        overflow-x: hidden;
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
        background-color: var(--drawer-block-primary);
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
      .select-traces-panel {
        max-height: 60vh;
        overflow-y: auto;
        overflow-x: hidden;
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
      .no-timeline-msg {
        padding: 1rem;
        align-items: center;
        display: flex;
        flex-direction: column;
        width: 100%;
      }
      .disabled-message {
        z-index: 100;
        position: absolute;
        top: 10%;
        left: 50%;
        opacity: 1;
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

  @Input() timelineData: TimelineData | undefined;
  @Input() allTraces: Traces | undefined;
  @Input() store: PersistentStore | undefined;

  @Output() readonly collapsedTimelineSizeChanged = new EventEmitter<number>();

  @ViewChild('collapsedTimeline') private collapsedTimelineRef:
    | ElementRef
    | undefined;

  @ViewChild('miniTimeline') miniTimeline: MiniTimelineComponent | undefined;

  videoUrl: SafeUrl | undefined;

  initialZoom: TimeRange | undefined = undefined;
  selectedTraces: Array<Trace<object>> = [];
  sortedTraces: Array<Trace<object>> = [];
  selectedTracesFormControl = new FormControl<Array<Trace<object>>>([]);
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
  bookmarks: Timestamp[] = [];
  isDisabled = false;

  private expanded = false;
  private emitEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;
  private expandedTimelineScrollEvent: WheelEvent | undefined;
  private expandedTimelineMouseXRatio: number | undefined;
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

    // sorted to be displayed in order corresponding to viewer tabs
    this.sortedTraces =
      this.allTraces
        ?.mapTrace((trace) => trace)
        .sort((a, b) => TraceTypeUtils.compareByDisplayOrder(a.type, b.type)) ??
      [];

    const storedDeselectedTraces = this.getStoredDeselectedTraceTypes();
    this.selectedTraces = this.sortedTraces.filter((trace) => {
      return (
        timelineData.hasTrace(trace) &&
        (!storedDeselectedTraces.includes(trace.type) ||
          timelineData.getActiveTrace() === trace ||
          !timelineData.hasMoreThanOneDistinctTimestamp())
      );
    });
    this.selectedTracesFormControl = new FormControl<Array<Trace<object>>>(
      this.selectedTraces,
    );

    const initialTraceToCropZoom = this.selectedTraces.find((trace) => {
      return (
        trace.type !== TraceType.SCREEN_RECORDING &&
        TraceTypeUtils.isTraceTypeWithViewer(trace.type) &&
        trace.lengthEntries > 0
      );
    });
    if (initialTraceToCropZoom) {
      this.initialZoom = new TimeRange(
        initialTraceToCropZoom.getEntry(0).getTimestamp(),
        timelineData.getFullTimeRange().to,
      );
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
      throw new Error(
        'A trace position should be available by the time the timeline is loaded',
      );
    }

    return position;
  }

  getSelectedTracesToShow(): Array<Trace<object>> {
    const sortedSelectedTraces = this.getSelectedTracesSortedByDisplayOrder();
    return sortedSelectedTraces.length > 8
      ? sortedSelectedTraces.slice(0, 7)
      : sortedSelectedTraces.slice(0, 8);
  }

  async onWinscopeEvent(event: WinscopeEvent) {
    await event.visit(WinscopeEventType.TRACE_POSITION_UPDATE, async () => {
      this.updateTimeInputValuesToCurrentTimestamp();
    });
    await event.visit(WinscopeEventType.ACTIVE_TRACE_CHANGED, async (event) => {
      await this.miniTimeline?.drawer?.draw();
      this.updateSelectedTraces(event.trace);
    });
    await event.visit(WinscopeEventType.DARK_MODE_TOGGLED, async (event) => {
      const activeTrace = this.timelineData?.getActiveTrace();
      if (activeTrace === undefined) {
        return;
      }
      await this.miniTimeline?.drawer?.draw();
    });
    await event.visit(WinscopeEventType.TRACE_ADD_REQUEST, async (event) => {
      this.sortedTraces.unshift(event.trace);
      this.sortedTraces.sort((a, b) =>
        TraceTypeUtils.compareByDisplayOrder(a.type, b.type),
      );
      this.selectedTracesFormControl.setValue(
        (this.selectedTracesFormControl.value ?? []).concat([event.trace]),
      );
      this.applyNewTraceSelection(event.trace);
      await this.miniTimeline?.drawer?.draw();
    });
    await event.visit(WinscopeEventType.TRACE_REMOVE_REQUEST, async (event) => {
      this.sortedTraces = this.sortedTraces.filter(
        (trace) => trace !== event.trace,
      );
      this.selectedTracesFormControl.setValue(
        this.selectedTracesFormControl.value?.filter(
          (trace) => trace !== event.trace,
        ) ?? [],
      );
      this.applyNewTraceSelection(event.trace);
      await this.miniTimeline?.drawer?.draw();
    });
    await event.visit(
      WinscopeEventType.INITIALIZE_TRACE_SEARCH_REQUEST,
      async () => this.setIsDisabled(true),
    );
    await event.visit(WinscopeEventType.TRACE_SEARCH_REQUEST, async () =>
      this.setIsDisabled(true),
    );
    await event.visit(WinscopeEventType.TRACE_SEARCH_INITIALIZED, async () =>
      this.setIsDisabled(false),
    );
    await event.visit(WinscopeEventType.TRACE_SEARCH_COMPLETED, async () =>
      this.setIsDisabled(false),
    );
  }

  async toggleExpand() {
    this.expanded = !this.expanded;
    this.changeDetectorRef.detectChanges();
    if (this.expanded) {
      Analytics.Navigation.logExpandedTimelineOpened();
    }
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

  isOptionDisabled(trace: Trace<object>) {
    const timelineData = assertDefined(this.timelineData);
    return (
      !timelineData.hasTrace(trace) || timelineData.getActiveTrace() === trace
    );
  }

  applyNewTraceSelection(clickedTrace: Trace<object>) {
    this.selectedTraces =
      this.selectedTracesFormControl.value ??
      this.sortedTraces.filter((trace) => {
        return assertDefined(this.timelineData).hasTrace(trace);
      });
    this.updateStoredDeselectedTraceTypes(clickedTrace);
  }

  getTitle(trace: Trace<object>): string {
    if (trace.type === TraceType.VIEW_CAPTURE) {
      return TRACE_INFO[trace.type].name + ' ' + trace.getDescriptors()[0];
    }
    return TRACE_INFO[trace.type].name + (trace.isDump() ? ' Dump' : '');
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
      this.isDisabled ||
      this.isInputFormFocused ||
      !assertDefined(this.timelineData).hasMoreThanOneDistinctTimestamp()
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
    const activeTrace = this.timelineData?.getActiveTrace();
    if (!activeTrace) {
      return false;
    }
    return (
      assertDefined(this.timelineData).getPreviousEntryFor(activeTrace) !==
      undefined
    );
  }

  hasNextEntry(): boolean {
    const activeTrace = this.timelineData?.getActiveTrace();
    if (!activeTrace) {
      return false;
    }
    return (
      assertDefined(this.timelineData).getNextEntryFor(activeTrace) !==
      undefined
    );
  }

  async moveToPreviousEntry() {
    const activeTrace = this.timelineData?.getActiveTrace();
    if (!activeTrace) {
      return;
    }
    const timelineData = assertDefined(this.timelineData);
    timelineData.moveToPreviousEntryFor(activeTrace);
    const position = assertDefined(timelineData.getCurrentPosition());
    await this.emitEvent(new TracePositionUpdate(position));
  }

  async moveToNextEntry() {
    const activeTrace = this.timelineData?.getActiveTrace();
    if (!activeTrace) {
      return;
    }
    const timelineData = assertDefined(this.timelineData);
    timelineData.moveToNextEntryFor(activeTrace);
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

    Analytics.Navigation.logTimeInput('human');
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

    Analytics.Navigation.logTimeInput('ns');
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

  updateExpandedTimelineMouseXRatio(mouseXRatio: number | undefined) {
    this.expandedTimelineMouseXRatio = mouseXRatio;
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

  onTimeCopied(type: 'ns' | 'human') {
    Analytics.Navigation.logTimeCopied(type);
  }

  getUTCOffset(): string {
    return assertDefined(
      this.timelineData?.getTimestampConverter(),
    ).getUTCOffset();
  }

  currentPositionBookmarked(): boolean {
    const currentTimestampNs =
      this.getCurrentTracePosition().timestamp.getValueNs();
    return this.bookmarks.some((bm) => bm.getValueNs() === currentTimestampNs);
  }

  toggleBookmarkCurrentPosition(event: PointerEvent) {
    const currentTimestamp = this.getCurrentTracePosition().timestamp;
    this.toggleBookmarkRange(new TimeRange(currentTimestamp, currentTimestamp));
    event.stopPropagation();
  }

  toggleBookmarkRange(range: TimeRange, rangeContainsBookmark?: boolean) {
    if (rangeContainsBookmark === undefined) {
      rangeContainsBookmark = this.bookmarks.some((bookmark) =>
        range.containsTimestamp(bookmark),
      );
    }
    const clickedNs = (range.from.getValueNs() + range.to.getValueNs()) / 2n;
    if (rangeContainsBookmark) {
      const closestBookmark = this.bookmarks.reduce((prev, curr) => {
        if (clickedNs - curr.getValueNs() < 0) return prev;
        return Math.abs(Number(curr.getValueNs() - clickedNs)) <
          Math.abs(Number(prev.getValueNs() - clickedNs))
          ? curr
          : prev;
      });
      this.bookmarks = this.bookmarks.filter(
        (bm) => bm.getValueNs() !== closestBookmark.getValueNs(),
      );
    } else {
      this.bookmarks = this.bookmarks.concat([
        assertDefined(
          this.timelineData?.getTimestampConverter(),
        ).makeTimestampFromNs(clickedNs),
      ]);
    }
    Analytics.Navigation.logTimeBookmark();
  }

  removeAllBookmarks() {
    this.bookmarks = [];
  }

  async onMiniTimelineTraceClicked(eventData: [Trace<object>, Timestamp]) {
    const [trace, timestamp] = eventData;
    await this.emitEvent(new ActiveTraceChanged(trace));
    await this.updatePosition(
      assertDefined(this.timelineData).makePositionFromActiveTrace(timestamp),
    );
    this.changeDetectorRef.detectChanges();
  }

  async onExpandedTimelineTraceClicked(trace: Trace<object>) {
    await this.emitEvent(new ActiveTraceChanged(trace));
    this.changeDetectorRef.detectChanges();
  }

  getTraceTooltip(trace: Trace<object>) {
    let tooltip = TRACE_INFO[trace.type].name;
    if (trace.type === TraceType.SCREEN_RECORDING) {
      tooltip += ' ' + trace.getDescriptors()[0].split('.')[0];
    }
    if (trace.type === TraceType.VIEW_CAPTURE) {
      tooltip += ' ' + trace.getDescriptors()[0];
    }
    return tooltip;
  }

  private updateSelectedTraces(trace: Trace<object> | undefined) {
    if (!trace) {
      return;
    }

    if (!this.selectedTraces.includes(trace)) {
      // Create new object to make sure we trigger an update on Mini Timeline child component
      this.selectedTraces = [...this.selectedTraces, trace];
      this.selectedTracesFormControl.setValue(this.selectedTraces);
    }
  }

  private updateTimeInputValuesToCurrentTimestamp() {
    const currentTimestampNs =
      this.getCurrentTracePosition().timestamp.getValueNs();
    const timelineData = assertDefined(this.timelineData);

    const formattedCurrentTimestamp = assertDefined(
      timelineData.getTimestampConverter(),
    )
      .makeTimestampFromNs(currentTimestampNs)
      .format(TimestampFormatType.DROP_DATE);
    this.selectedTimeFormControl.setValue(formattedCurrentTimestamp);
    this.selectedNsFormControl.setValue(`${currentTimestampNs} ns`);
  }

  private getSelectedTracesSortedByDisplayOrder(): Array<Trace<object>> {
    return this.selectedTraces
      .slice()
      .sort((a, b) => TraceTypeUtils.compareByDisplayOrder(a.type, b.type));
  }

  private getStoredDeselectedTraceTypes(): TraceType[] {
    const storedDeselectedTraces = this.store?.get(
      this.storeKeyDeselectedTraces,
    );
    return JSON.parse(storedDeselectedTraces ?? '[]');
  }

  private updateStoredDeselectedTraceTypes(clickedTrace: Trace<object>) {
    if (!this.store) {
      return;
    }

    let storedDeselected = this.getStoredDeselectedTraceTypes();
    if (
      this.selectedTraces.includes(clickedTrace) &&
      storedDeselected.includes(clickedTrace.type)
    ) {
      storedDeselected = storedDeselected.filter(
        (stored) => stored !== clickedTrace.type,
      );
    } else if (
      !this.selectedTraces.includes(clickedTrace) &&
      !storedDeselected.includes(clickedTrace.type)
    ) {
      Analytics.Navigation.logTraceTimelineDeselected(
        TRACE_INFO[clickedTrace.type].name,
      );
      storedDeselected.push(clickedTrace.type);
    }

    this.store.add(
      this.storeKeyDeselectedTraces,
      JSON.stringify(storedDeselected),
    );
  }

  private validateNsFormat(control: FormControl): ValidationErrors | null {
    const valid = TimestampUtils.isNsFormat(control.value ?? '');
    return !valid ? {invalidInput: control.value} : null;
  }

  private setIsDisabled(value: boolean) {
    this.isDisabled = value;
    this.changeDetectorRef.detectChanges();
  }
}
