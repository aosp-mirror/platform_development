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
import html2canvas from "html2canvas";

@Component({
  selector: "trace-view-header",
  template: `
    <button class="icon-button" id="toggle-btn" (click)="toggleView()">
      <mat-icon aria-hidden="true">
        {{ showTrace ? "expand_more" : "chevron_right" }}
      </mat-icon>
    </button>
    <mat-icon id="dep-icon" *ngFor="let dep of dependencies" aria-hidden="true" class="icon-button">{{TRACE_INFO[dep].icon}}</mat-icon>
    <span class="trace-card-title-text">
      {{title}}
    </span>
    <button id="save-btn" class="icon-button" (click)="saveTraces()">
      <mat-icon aria-hidden="true">save_alt</mat-icon>
    </button>
    <button id="screenshot-btn" (click)="takeScreenshot()" class="icon-button">
      <mat-icon aria-hidden="true">camera_alt</mat-icon>
    </button>
  `,
  styles: [
    ".trace-card-title {font: inherit; display: inline-block; vertical-align: middle;}",
  ]
})
export class TraceViewHeaderComponent {
  @Input() title?: string;
  @Input() dependencies?: TraceType[];
  @Input() showTrace = true;
  @Input() cardId!: number ;

  @Output() showTraceChange = new EventEmitter<boolean>();
  @Output() saveTraceChange = new EventEmitter<TraceType[]>();

  TRACE_INFO = TRACE_INFO;

  toggleView() {
    this.showTrace = !this.showTrace;
    this.showTraceChange.emit(this.showTrace);
  }

  public saveTraces() {
    this.saveTraceChange.emit(this.dependencies);
  }

  public takeScreenshot() {
    const el = document.querySelector(`#card-${this.cardId}`);
    if (el) {
      html2canvas((el as HTMLElement)).then((canvas) => {
        const uri = canvas.toDataURL();
        const filename = "Winscope-Screenshot.png";
        const link = document.createElement("a");
        if (typeof link.download === "string") {
          link.href = uri;
          link.download = filename;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
        } else {
          window.open(uri);
        }
      });
    }
  }
}
