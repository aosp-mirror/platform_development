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
import { Component, Input, Output, EventEmitter } from "@angular/core";
import { Core } from "app/core";


@Component({
  selector: "upload-traces",
  template: `
      <mat-card-title>Upload Traces</mat-card-title>
      <mat-card-content>
        <div id="inputfile">
          <input mat-input type="file" (change)="onInputFile($event)" #fileUpload>
        </div>
      </mat-card-content>
  `,

})
export class UploadTracesComponent {
  @Input()
    core: Core = new Core();

  @Output()
    coreChange = new EventEmitter<Core>();

  public async onInputFile(event: Event) {
    const files = this.getInputFiles(event);
    await this.core.bootstrap(files);

    const viewersDiv = document.querySelector("div#viewers")!;
    viewersDiv.innerHTML = "";
    this.core.getViews().forEach(view => viewersDiv!.appendChild(view) );
    this.coreChange.emit(this.core);

    const timestampsDiv = document.querySelector("div#timestamps")!;
    timestampsDiv.innerHTML = `Retrieved ${this.core.getTimestamps().length} unique timestamps`;
  }

  //TODO: extend with support for multiple files, archives, etc...
  private getInputFiles(event: Event): File[] {
    const files: any = (event?.target as HTMLInputElement)?.files;

    if (!files || !files[0]) {
      return [];
    }

    return [files[0]];
  }
}
