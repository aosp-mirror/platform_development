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
import { ProxyState } from "./proxy_client";
import { ConfigurationOptions, SelectionConfiguration } from "./collect_traces.component";


@Component({
  selector: "trace-config",
  template: `
    <div class="card-block" *ngIf="status===states.START_TRACE">
        <mat-checkbox class="md-primary" [checked]="defaultCheck">{{name}}</mat-checkbox>
        <div class="adv-config" *ngIf="configs">
          <mat-checkbox class="md-primary" *ngFor="let enableConfig of traceEnableConfigs()">{{enableConfig}}</mat-checkbox>
          <div class="selection">
            <mat-form-field appearance="fill" class="config-selection" *ngFor="let con of traceSelectionConfigs()">
            <mat-label>{{con.name}}</mat-label>
            <mat-select>
                <mat-option
                  *ngFor="let option of con.options"
                  [value]="con.value"
                >{{ option }}</mat-option>
              </mat-select>
            </mat-form-field>
          </div>
        </div>
    </div>
  `,
  styles: [".adv-config {margin-left: 5rem;}"],
})

export class TraceConfigComponent {
  states = ProxyState;
  objectKeys = Object.keys;

  @Input()
    name = "";

  @Input()
    configs: ConfigurationOptions | null = null;

  @Input()
    defaultCheck: boolean | undefined = false;

  @Input()
    status = this.states.UNAUTH;

  @Output()
    statusChange = new EventEmitter<ProxyState>();

  public traceEnableConfigs(): Array<string> {
    if (this.configs && this.configs.enableConfigs) {
      return this.configs.enableConfigs;
    } else {
      return [];
    }
  }

  public traceSelectionConfigs(): Array<SelectionConfiguration> {
    if (this.configs) {
      return this.configs.selectionConfigs;
    } else {
      return [];
    }
  }

  public restart() {
    this.status = this.states.CONNECTING;
    this.statusChange.emit(this.status);
  }

  public resetLastDevice() {
    this.restart();
  }
}
