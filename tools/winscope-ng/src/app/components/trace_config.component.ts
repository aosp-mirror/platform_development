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
import { Component, Input } from "@angular/core";
import { EnableConfiguration, SelectionConfiguration, TraceConfiguration } from "trace_collection/trace_collection_utils";

@Component({
  selector: "trace-config",
  template: `
    <div class="card-block">
      <div>
        <mat-checkbox
          class="trace-box"
          [checked]="trace.run"
          [indeterminate]="trace.isTraceCollection ? someTraces() : false"
          (change)="changeRunTrace($event.checked)"
        >{{trace.name}}</mat-checkbox>

        <div class="adv-config" *ngIf="trace.config">
          <mat-checkbox
            *ngFor="let enableConfig of traceEnableConfigs()"
            class="enable-config"
            [disabled]="!trace.run && !trace.isTraceCollection"
            [(ngModel)]="enableConfig.enabled"
            (ngModelChange)="changeTraceCollectionConfig()"
          >{{enableConfig.name}}</mat-checkbox>

          <div class="selection" *ngIf="trace.config.selectionConfigs">
            <mat-form-field
              appearance="fill"
              class="config-selection"
              *ngFor="let selectionConfig of traceSelectionConfigs()"
            ><mat-label>{{selectionConfig.name}}</mat-label>
            <mat-select class="selected-value" [(value)]="selectionConfig.value" [disabled]="!trace.run">
              <mat-option
                *ngFor="let option of selectionConfig.options"
                value="{{option}}"
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
  @Input()
    trace: TraceConfiguration = {};

  public traceEnableConfigs(): Array<EnableConfiguration> {
    if (this.trace.config) {
      return this.trace.config.enableConfigs;
    } else {
      return [];
    }
  }

  public traceSelectionConfigs(): Array<SelectionConfiguration> {
    if (this.trace.config) {
      return this.trace.config.selectionConfigs;
    } else {
      return [];
    }
  }

  public someTraces(): boolean {
    return this.traceEnableConfigs().filter(trace => trace.enabled).length > 0
      && !this.trace.run;
  }

  public changeRunTrace(run: boolean): void {
    this.trace.run = run;
    if (this.trace.isTraceCollection) {
      this.traceEnableConfigs().forEach((c: EnableConfiguration) => (c.enabled = run));
    }
  }

  public changeTraceCollectionConfig(): void {
    if (this.trace.isTraceCollection) {
      this.trace.run =  this.traceEnableConfigs().every((c: EnableConfiguration) => c.enabled);
    }
  }
}
