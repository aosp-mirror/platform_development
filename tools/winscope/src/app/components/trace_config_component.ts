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
import {Component, EventEmitter, Input, Output} from '@angular/core';
import {assertDefined} from 'common/assert_utils';
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
        *ngFor="let traceKey of objectKeys(this.traceConfig)"
        color="primary"
        class="trace-checkbox"
        [checked]="this.traceConfig[traceKey].run"
        [indeterminate]="
          this.traceConfig[traceKey].isTraceCollection
            ? someTraces(this.traceConfig[traceKey])
            : false
        "
        (change)="changeRunTrace($event.checked, this.traceConfig[traceKey])"
        >{{ this.traceConfig[traceKey].name }}</mat-checkbox
      >
    </div>

    <ng-container *ngFor="let traceKey of advancedConfigTraces()">
      <mat-divider></mat-divider>

      <h3 class="mat-subheading-2">{{ this.traceConfig[traceKey].name }} configuration</h3>

      <div
        *ngIf="this.traceConfig[traceKey].config?.enableConfigs.length > 0"
        class="enable-config-opt">
        <mat-checkbox
          *ngFor="let enableConfig of traceEnableConfigs(this.traceConfig[traceKey])"
          color="primary"
          class="enable-config"
          [disabled]="
            !this.traceConfig[traceKey].run && !this.traceConfig[traceKey].isTraceCollection
          "
          [(ngModel)]="enableConfig.enabled"
          (change)="changeTraceCollectionConfig(this.traceConfig[traceKey])"
          >{{ enableConfig.name }}</mat-checkbox
        >
      </div>

      <div
        *ngIf="this.traceConfig[traceKey].config?.selectionConfigs.length > 0"
        class="selection-config-opt">
        <mat-form-field
          *ngFor="let selectionConfig of traceSelectionConfigs(this.traceConfig[traceKey])"
          class="config-selection"
          appearance="fill">
          <mat-label>{{ selectionConfig.name }}</mat-label>

          <mat-select
            class="selected-value"
            [(value)]="selectionConfig.value"
            [disabled]="!this.traceConfig[traceKey].run">
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

  @Input() traceConfig: TraceConfigurationMap | undefined;
  @Output() readonly traceConfigChange =
    new EventEmitter<TraceConfigurationMap>();

  advancedConfigTraces() {
    const advancedConfigs: string[] = [];
    Object.keys(assertDefined(this.traceConfig)).forEach((traceKey: string) => {
      if (assertDefined(this.traceConfig)[traceKey].config) {
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
    return (
      !trace.run &&
      this.traceEnableConfigs(trace).filter((trace) => trace.enabled).length > 0
    );
  }

  changeRunTrace(run: boolean, trace: TraceConfiguration): void {
    trace.run = run;
    if (trace.isTraceCollection) {
      this.traceEnableConfigs(trace).forEach(
        (c: EnableConfiguration) => (c.enabled = run),
      );
    }
  }

  changeTraceCollectionConfig(trace: TraceConfiguration): void {
    if (trace.isTraceCollection) {
      trace.run = this.traceEnableConfigs(trace).every(
        (c: EnableConfiguration) => c.enabled,
      );
    }
  }
}
