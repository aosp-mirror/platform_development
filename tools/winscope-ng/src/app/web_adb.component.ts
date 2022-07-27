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
import {Component} from "@angular/core";

@Component({
  selector: "web-adb",
  template: `
      <div id="info-message">
        <mat-icon class="adb-icon">info</mat-icon>
        <span class="adb-info">Add new device</span>
      </div>
      <div>
        <p>Click the button below to follow instructions in the Chrome pop-up.</p>
        <p>Selecting a device will kill all existing ADB connections.</p>
      </div>
      <div>
        <button mat-raised-button>Select a device</button>
      </div>
  `,
  styles: [".icon-message {vertical-align: middle;}"]
})
export class WebAdbComponent {
  adbDevice = null;
}
