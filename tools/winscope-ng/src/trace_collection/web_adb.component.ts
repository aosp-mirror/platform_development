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
    <div class="title">Unable to connect to Web ADB</div>
    <div class="md-body-2">
      <p>Instructions for connecting via Web ADB.</p>
    </div>
    <div class="md-layout">
      <button mat-raised-button class="md-accent" (click)="restart()">Retry</button>
    </div>
  `,
})
export class WebAdbComponent {
  public restart() {
    console.log("Try connecting again");
  }
}
