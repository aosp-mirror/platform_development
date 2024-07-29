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
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Inject,
  Input,
  Output,
} from '@angular/core';
import {MatSelect, MatSelectChange} from '@angular/material/select';
import {assertDefined} from 'common/assert_utils';
import {globalConfig} from 'common/global_config';
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {
  EnableConfiguration,
  SelectionConfiguration,
  TraceConfiguration,
  TraceConfigurationMap,
} from 'trace_collection/trace_configuration';

@Component({
  selector: 'trace-config',
  template: `
    <h3 class="mat-subheading-2">{{title}}</h3>

    <div class="checkboxes">
      <mat-checkbox
        *ngFor="let traceKey of objectKeys(this.traceConfig)"
        color="primary"
        class="trace-checkbox"
        [disabled]="!this.traceConfig[traceKey].available"
        [(ngModel)]="this.traceConfig[traceKey].enabled"
        (ngModelChange)="onTraceConfigChange()"
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
          [disabled]="!this.traceConfig[traceKey].enabled"
          [(ngModel)]="enableConfig.enabled"
          (ngModelChange)="onTraceConfigChange()"
          >{{ enableConfig.name }}</mat-checkbox
        >
      </div>

      <div
        *ngIf="this.traceConfig[traceKey].config?.selectionConfigs.length > 0"
        class="selection-config-opt">
        <ng-container *ngFor="let selectionConfig of traceSelectionConfigs(this.traceConfig[traceKey])">
          <mat-form-field
            class="config-selection"
            appearance="fill">
            <mat-label>{{ selectionConfig.name }}</mat-label>

            <mat-select
              #matSelect
              disableOptionCentering
              class="selected-value"
              [attr.label]="traceKey + selectionConfig.name"
              [value]="selectionConfig.value"
              [disabled]="!this.traceConfig[traceKey].enabled"
              (selectionChange)="onSelectChange($event, selectionConfig)">
              <mat-option *ngFor="let option of selectionConfig.options" (click)="onOptionClick(matSelect, option, traceKey + selectionConfig.name)" [value]="option">{{
                option
              }}</mat-option>
            </mat-select>
          </mat-form-field>
        </ng-container>
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
      .config-panel {
        position: absolute;
        left: 0px;
        top: 100px;
      }
    `,
  ],
})
export class TraceConfigComponent {
  objectKeys = Object.keys;
  changeDetectionWorker: number | undefined;
  traceConfig: TraceConfigurationMap | undefined;

  @Input() title: string | undefined;
  @Input() traceConfigStoreKey: string | undefined;
  @Input() initialTraceConfig: TraceConfigurationMap | undefined;
  @Input() storage: Storage | undefined;
  @Output() readonly traceConfigChange =
    new EventEmitter<TraceConfigurationMap>();

  constructor(
    @Inject(ChangeDetectorRef) private changeDetectorRef: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.traceConfig = PersistentStoreProxy.new<TraceConfigurationMap>(
      assertDefined(this.traceConfigStoreKey),
      assertDefined(
        this.initialTraceConfig,
        () => 'component initialized without config',
      ),
      assertDefined(this.storage),
    );
    if (globalConfig.MODE !== 'KARMA_TEST') {
      this.changeDetectionWorker = window.setInterval(
        () => this.changeDetectorRef.detectChanges(),
        200,
      );
    }
  }

  ngOnDestroy() {
    window.clearInterval(this.changeDetectionWorker);
  }

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
      !trace.enabled &&
      this.traceEnableConfigs(trace).filter((trace) => trace.enabled).length > 0
    );
  }

  onSelectChange(event: MatSelectChange, config: SelectionConfiguration) {
    config.value = event.value;
    event.source.close();
    this.onTraceConfigChange();
  }

  onOptionClick(select: MatSelect, option: string, configName: string) {
    if (select.value === option) {
      const selectElement = assertDefined(
        document.querySelector(`mat-select[label="${configName}"]`),
      );
      (selectElement as HTMLElement).blur();
    }
  }

  onTraceConfigChange() {
    this.traceConfigChange.emit(this.traceConfig);
  }
}
