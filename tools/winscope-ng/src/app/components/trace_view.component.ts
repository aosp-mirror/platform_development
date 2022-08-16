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
  Component,
  Input,
  Output,
  EventEmitter
} from "@angular/core";
import { TRACE_INFO } from "../trace_info";
import { TraceType } from "common/trace/trace_type";

@Component({
  selector: "trace-view",
  template: `
    <mat-card class="trace-card">
      <mat-card-header>
        <mat-card-title class="trace-card-title" *ngIf="dependencies">
          <trace-view-header
            [title]="title"
            [(showTrace)]="showTrace"
            [dependencies]="dependencies"
            [cardId]="cardId"
            (saveTraceChange)="onSaveTraces($event)"
          ></trace-view-header>
        </mat-card-title>
      </mat-card-header>
      <mat-card-content class="trace-card-content" [hidden]="!showTrace">
      </mat-card-content>
    </mat-card>
  `,
})
export class TraceViewComponent {
  @Input() title!: string;
  @Input() dependencies!: TraceType[];
  @Input() showTrace = true;
  @Input() cardId = 0;
  @Output() saveTraces = new EventEmitter<TraceType[]>();

  TRACE_INFO = TRACE_INFO;

  onSaveTraces(dependencies: TraceType[]) {
    this.saveTraces.emit(dependencies);
  }
}
