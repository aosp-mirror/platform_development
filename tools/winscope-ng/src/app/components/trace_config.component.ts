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
import { EnableConfiguration, SelectionConfiguration, TraceConfiguration, TraceConfigurationMap } from "trace_collection/trace_collection_utils";

@Component({
  selector: "trace-config",
  template: `
    <div class="card-block">
      <p class="subtitle">Trace targets</p>
      <ul class="checkboxes">
        <div *ngFor="let traceKey of objectKeys(traces)">
          <mat-checkbox
            class="trace-box"
            [checked]="traces[traceKey].run"
            [indeterminate]="traces[traceKey].isTraceCollection ? someTraces(traces[traceKey]) : false"
            (change)="changeRunTrace($event.checked, traces[traceKey])"
          >{{traces[traceKey].name}}</mat-checkbox>
        </div>
      </ul>

      <div *ngFor="let traceKey of advancedConfigTraces()">
        <p class="subtitle">{{traces[traceKey].name}} configuration</p>
        <div>
          <div class="config-opt" *ngIf="traces[traceKey].config?.enableConfigs">
            <mat-checkbox
              *ngFor="let enableConfig of traceEnableConfigs(traces[traceKey])"
              class="enable-config"
              [disabled]="!traces[traceKey].run && !traces[traceKey].isTraceCollection"
              [(ngModel)]="enableConfig.enabled"
              (ngModelChange)="changeTraceCollectionConfig(traces[traceKey])"
            >{{enableConfig.name}}</mat-checkbox>

          <div class="config-opt" *ngIf="traces[traceKey].config?.selectionConfigs">
            <mat-form-field
              appearance="fill"
              class="config-selection"
              *ngFor="let selectionConfig of traceSelectionConfigs(traces[traceKey])"
            ><mat-label>{{selectionConfig.name}}</mat-label>
            <mat-select class="selected-value" [(value)]="selectionConfig.value" [disabled]="!traces[traceKey].run">
              <mat-option
                *ngFor="let option of selectionConfig.options"
                value="{{option}}"
              >{{ option }}</mat-option>
            </mat-select>
            </mat-form-field>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .checkboxes {
        columns: 3 10em;
        padding: 0;
      }
      .config-opt {
        position: relative;
        display: inline-block;
      }
    `
  ]
})

export class TraceConfigComponent {
  objectKeys = Object.keys;
  @Input() traces!: TraceConfigurationMap;

  public advancedConfigTraces() {
    const advancedConfigs: Array<string> = [];
    Object.keys(this.traces).forEach((traceKey: string) => {
      if (this.traces[traceKey].config) {
        advancedConfigs.push(traceKey);
      }
    });
    return advancedConfigs;
  }

  public traceEnableConfigs(trace: TraceConfiguration): Array<EnableConfiguration> {
    if (trace.config) {
      return trace.config.enableConfigs;
    } else {
      return [];
    }
  }

  public traceSelectionConfigs(trace: TraceConfiguration): Array<SelectionConfiguration> {
    if (trace.config) {
      return trace.config.selectionConfigs;
    } else {
      return [];
    }
  }

  public someTraces(trace: TraceConfiguration): boolean {
    return this.traceEnableConfigs(trace).filter(trace => trace.enabled).length > 0
      && !trace.run;
  }

  public changeRunTrace(run: boolean, trace: TraceConfiguration): void {
    trace.run = run;
    if (trace.isTraceCollection) {
      this.traceEnableConfigs(trace).forEach((c: EnableConfiguration) => (c.enabled = run));
    }
  }

  public changeTraceCollectionConfig(trace: TraceConfiguration): void {
    if (trace.isTraceCollection) {
      trace.run =  this.traceEnableConfigs(trace).every((c: EnableConfiguration) => c.enabled);
    }
  }
}
