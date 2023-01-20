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
import {ChangeDetectorRef, Component, Inject, Input} from '@angular/core';
import {
  EnableConfiguration,
  SelectionConfiguration,
  TraceConfiguration,
  TraceConfigurationMap,
} from 'trace_collection/trace_collection_utils';

@Component({
  selector: 'trace-config',
  template: `
    <h3 class="mat-subheading-2">Trace targets</h3>

    <div class="checkboxes">
      <mat-checkbox
        *ngFor="let traceKey of objectKeys(traces)"
        color="primary"
        class="trace-checkbox"
        [checked]="traces[traceKey].run"
        [indeterminate]="traces[traceKey].isTraceCollection ? someTraces(traces[traceKey]) : false"
        (change)="changeRunTrace($event.checked, traces[traceKey])"
        >{{ traces[traceKey].name }}</mat-checkbox
      >
    </div>

    <ng-container *ngFor="let traceKey of advancedConfigTraces()">
      <mat-divider></mat-divider>

      <h3 class="mat-subheading-2">{{ traces[traceKey].name }} configuration</h3>

      <div *ngIf="traces[traceKey].config?.enableConfigs.length > 0" class="enable-config-opt">
        <mat-checkbox
          *ngFor="let enableConfig of traceEnableConfigs(traces[traceKey])"
          color="primary"
          class="enable-config"
          [disabled]="!traces[traceKey].run && !traces[traceKey].isTraceCollection"
          [(ngModel)]="enableConfig.enabled"
          (change)="changeTraceCollectionConfig(traces[traceKey])"
          >{{ enableConfig.name }}</mat-checkbox
        >
      </div>

      <div
        *ngIf="traces[traceKey].config?.selectionConfigs.length > 0"
        class="selection-config-opt">
        <mat-form-field
          *ngFor="let selectionConfig of traceSelectionConfigs(traces[traceKey])"
          class="config-selection"
          appearance="fill">
          <mat-label>{{ selectionConfig.name }}</mat-label>

          <mat-select
            class="selected-value"
            [(value)]="selectionConfig.value"
            [disabled]="!traces[traceKey].run">
            <mat-option *ngFor="let option of selectionConfig.options" value="{{ option }}">{{
              option
            }}</mat-option>
          </mat-select>
        </mat-form-field>
      </div>
    </ng-container>
  `,
  styles: [
    `
      .checkboxes {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        column-gap: 10px;
      }
      .enable-config-opt,
      .selection-config-opt {
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
        gap: 10px;
      }
    `,
  ],
})
export class TraceConfigComponent {
  objectKeys = Object.keys;
  @Input() traces!: TraceConfigurationMap;

  constructor(@Inject(ChangeDetectorRef) private cdr: ChangeDetectorRef) { }

  advancedConfigTraces() {
    const advancedConfigs: string[] = [];
    Object.keys(this.traces).forEach((traceKey: string) => {
      if (this.traces[traceKey].config) {
        advancedConfigs.push(traceKey);
      }
    });
    return advancedConfigs;
  }

  traceEnableConfigs(trace: TraceConfiguration): EnableConfiguration[] {
    if (trace.config) {
      return trace.config.enableConfigs;
    } else {
      return [];
    }
  }

  traceSelectionConfigs(trace: TraceConfiguration): SelectionConfiguration[] {
    if (trace.config) {
      return trace.config.selectionConfigs;
    } else {
      return [];
    }
  }

  someTraces(trace: TraceConfiguration): boolean {
    return this.traceEnableConfigs(trace).filter((trace) => trace.enabled).length > 0 && !trace.run;
  }

  changeRunTrace(run: boolean, trace: TraceConfiguration): void {
    trace.run = run;
    if (trace.isTraceCollection) {
      this.traceEnableConfigs(trace).forEach((c: EnableConfiguration) => (c.enabled = run));
    }
    this.cdr.detectChanges();
  }

  changeTraceCollectionConfig(trace: TraceConfiguration): void {
    if (trace.isTraceCollection) {
      trace.run = this.traceEnableConfigs(trace).every((c: EnableConfiguration) => c.enabled);
    }
    this.cdr.detectChanges();
  }
}
