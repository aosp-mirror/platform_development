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
  NgZone,
  Output,
} from '@angular/core';
import {MatSelect, MatSelectChange} from '@angular/material/select';
import {assertDefined} from 'common/assert_utils';
import {globalConfig} from 'common/global_config';
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {Store} from 'common/store';
import {
  EnableConfiguration,
  SelectionConfiguration,
  TraceConfigurationMap,
} from 'trace_collection/trace_configuration';
import {userOptionStyle} from 'viewers/components/styles/user_option.styles';

@Component({
  selector: 'trace-config',
  template: `
    <h3 class="mat-subheading-2">{{title}}</h3>

    <div class="checkboxes" [style.height]="getTraceCheckboxContainerHeight()">
      <mat-checkbox
        *ngFor="let traceKey of getSortedTraceKeys()"
        color="primary"
        class="trace-checkbox"
        [disabled]="!this.traceConfig[traceKey].available"
        [(ngModel)]="this.traceConfig[traceKey].enabled"
        (ngModelChange)="onTraceConfigChange()"
        >{{ this.traceConfig[traceKey].name }}</mat-checkbox>
    </div>

    <ng-container *ngFor="let traceKey of getSortedConfigKeys()">
      <mat-divider></mat-divider>

      <h3 class="config-heading mat-subheading-2">{{ this.traceConfig[traceKey].name }} configuration</h3>

      <div
        *ngIf="this.traceConfig[traceKey].config && this.traceConfig[traceKey].config.enableConfigs.length > 0"
        class="enable-config-opt">
        <mat-checkbox
          *ngFor="let enableConfig of getSortedConfigs(this.traceConfig[traceKey].config.enableConfigs)"
          color="primary"
          class="enable-config"
          [disabled]="!this.traceConfig[traceKey].enabled"
          [(ngModel)]="enableConfig.enabled"
          (ngModelChange)="onTraceConfigChange()"
          >{{ enableConfig.name }}</mat-checkbox
        >
      </div>

      <div
        *ngIf="this.traceConfig[traceKey].config && this.traceConfig[traceKey].config.selectionConfigs.length > 0"
        class="selection-config-opt">
        <ng-container *ngFor="let selectionConfig of getSortedConfigs(this.traceConfig[traceKey].config.selectionConfigs)">
          <div class="config-selection-with-desc" [class.wide-field]="selectionConfig.wideField">
            <mat-form-field
              class="config-selection"
              [class.wide-field]="selectionConfig.wideField"
              appearance="fill">
              <mat-label>{{ selectionConfig.name }}</mat-label>

              <mat-select
                #matSelect
                [multiple]="isMultipleSelect(selectionConfig)"
                disableOptionCentering
                class="selected-value"
                [attr.label]="traceKey + selectionConfig.name"
                [value]="selectionConfig.value"
                [disabled]="!this.traceConfig[traceKey].enabled || selectionConfig.options.length === 0"
                (selectionChange)="onSelectChange($event, selectionConfig)">
                <span class="mat-option" *ngIf="matSelect.multiple || selectionConfig.optional">
                  <button
                    *ngIf="matSelect.multiple"
                    mat-flat-button
                    class="user-option"
                    [color]="matSelect.value.length === selectionConfig.options.length ? 'primary' : undefined"
                    [class.not-enabled]="matSelect.value.length !== selectionConfig.options.length"
                    (click)="onAllButtonClick(matSelect, selectionConfig)"> All </button>
                  <button
                    *ngIf="selectionConfig.optional && !matSelect.multiple"
                    mat-flat-button
                    class="user-option"
                    [color]="matSelect.value.length === 0 ? 'primary' : undefined"
                    [class.not-enabled]="matSelect.value.length > 0"
                    (click)="onNoneButtonClick(matSelect, selectionConfig)"> None </button>
                </span>
                <mat-option
                  *ngFor="let option of selectionConfig.options"
                  (click)="onOptionClick(matSelect, option, traceKey + selectionConfig.name)"
                  [value]="option"
                  (mouseenter)="onSelectOptionHover($event, option)"
                  [matTooltip]="option"
                  [matTooltipDisabled]="disableOptionTooltip(option, optionEl)"
                  matTooltipPosition="right">
                    <span #optionEl> {{ option }} </span>
                </mat-option>
              </mat-select>
            </mat-form-field>
            <span class="config-desc" *ngIf="selectionConfig.desc"> {{selectionConfig.desc}} </span>
          </div>
        </ng-container>
      </div>
    </ng-container>
  `,
  styles: [
    `
      .checkboxes {
        display: flex;
        flex-direction: column;
        flex-wrap: wrap;
      }
      .enable-config-opt,
      .selection-config-opt {
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
        gap: 10px;
      }
      .config-selection-with-desc {
        display: flex;
        flex-direction: column;
      }
      .wide-field {
        width: 100%;
      }
      .config-panel {
        position: absolute;
        left: 0px;
        top: 100px;
      }
    `,
    userOptionStyle,
  ],
})
export class TraceConfigComponent {
  changeDetectionWorker: number | undefined;
  traceConfig: TraceConfigurationMap | undefined;

  @Input() title: string | undefined;
  @Input() traceConfigStoreKey: string | undefined;
  @Input() initialTraceConfig: TraceConfigurationMap | undefined;
  @Input() storage: Store | undefined;
  @Output() readonly traceConfigChange =
    new EventEmitter<TraceConfigurationMap>();

  private tooltipsWithStablePosition = new Set<string>();

  constructor(
    @Inject(ChangeDetectorRef) private changeDetectorRef: ChangeDetectorRef,
    @Inject(NgZone) private ngZone: NgZone,
  ) {}

  ngOnInit() {
    this.traceConfig = PersistentStoreProxy.new<TraceConfigurationMap>(
      assertDefined(this.traceConfigStoreKey),
      assertDefined(
        JSON.parse(JSON.stringify(assertDefined(this.initialTraceConfig))),
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
    this.traceConfigChange.emit(this.traceConfig);
  }

  ngOnDestroy() {
    window.clearInterval(this.changeDetectionWorker);
  }

  getTraceCheckboxContainerHeight(): string {
    const config = assertDefined(this.traceConfig);
    return Math.ceil(Object.keys(config).length / 3) * 24 + 'px';
  }

  getSortedTraceKeys(): string[] {
    const config = assertDefined(this.traceConfig);
    return Object.keys(config).sort((a, b) => {
      return config[a].name < config[b].name ? -1 : 1;
    });
  }

  getSortedConfigKeys(): string[] {
    const advancedConfigs: string[] = [];
    Object.keys(assertDefined(this.traceConfig)).forEach((traceKey: string) => {
      if (assertDefined(this.traceConfig)[traceKey].config) {
        advancedConfigs.push(traceKey);
      }
    });
    return advancedConfigs.sort();
  }

  getSortedConfigs(
    configs: EnableConfiguration[] | SelectionConfiguration[],
  ): EnableConfiguration[] | SelectionConfiguration[] {
    return configs.sort((a, b) => {
      return a.name < b.name ? -1 : 1;
    });
  }

  onSelectOptionHover(event: MouseEvent, option: string) {
    if (this.tooltipsWithStablePosition.has(option)) {
      return;
    }
    this.ngZone.run(() => {
      (event.target as HTMLElement).dispatchEvent(new Event('mouseleave'));
      this.tooltipsWithStablePosition.add(option);
      this.changeDetectorRef.detectChanges();
      (event.target as HTMLElement).dispatchEvent(new Event('mouseenter'));
    });
  }

  disableOptionTooltip(option: string, optionText: HTMLElement): boolean {
    const optionEl = assertDefined(optionText.parentElement);
    return (
      !this.tooltipsWithStablePosition.has(option) ||
      optionEl.offsetWidth >= optionText.offsetWidth
    );
  }

  onSelectChange(event: MatSelectChange, config: SelectionConfiguration) {
    config.value = event.value;
    if (!event.source.multiple) {
      event.source.close();
    }
    this.onTraceConfigChange();
  }

  onNoneButtonClick(select: MatSelect, config: SelectionConfiguration) {
    if (config.value.length > 0) {
      select.value = '';
      config.value = '';
      this.onTraceConfigChange();
    }
  }

  onAllButtonClick(select: MatSelect, config: SelectionConfiguration) {
    if (config.value.length !== config.options.length) {
      config.value = config.options;
      select.value = config.options;
    } else {
      config.value = [];
      select.value = [];
    }
    this.onTraceConfigChange();
  }

  onOptionClick(select: MatSelect, option: string, configName: string) {
    if (select.value === option) {
      const selectElement = assertDefined(
        document.querySelector<HTMLElement>(
          `mat-select[label="${configName}"]`,
        ),
      );
      selectElement.blur();
    }
  }

  onTraceConfigChange() {
    this.traceConfigChange.emit(this.traceConfig);
  }

  isMultipleSelect(config: SelectionConfiguration): boolean {
    return Array.isArray(config.value);
  }
}
