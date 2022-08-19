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
  EventEmitter,
  Input,
  Output
} from "@angular/core";
import {UiData} from "./ui_data";

@Component({
  selector: "viewer-window-manager",
  template: `
    <div class="viewer-window-manager">
      <div class="title">Window Manager</div>
      <div class="input-value">Input value: {{inputData?.text}}</div>
      <div class="button"><button mat-icon-button (click)="generateOutputEvent($event)">Output event!</button></div>
    </div>
  `
})
export class ViewerWindowManagerComponent {
  @Input()
    inputData?: UiData;

  @Output()
    outputEvent = new EventEmitter<DummyEvent>(); // or EventEmitter<void>()

  public generateOutputEvent(event: MouseEvent) {
    this.outputEvent.emit(new DummyEvent());
  }
}

export class DummyEvent {
}
