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
import {CdkVirtualScrollViewport} from '@angular/cdk/scrolling';
import {Component, ElementRef, Inject, Input, ViewChild} from '@angular/core';
import {MatSelectChange} from '@angular/material/select';
import {Events} from './events';
import {UiData} from './ui_data';

@Component({
  selector: 'viewer-transactions',
  template: `
    <div class="card-grid">
      <div class="entries">
        <div class="filters">
          <div class="time"></div>
          <div class="id">
            <mat-form-field appearance="fill">
              <mat-label>TX ID</mat-label>
              <input matInput [(ngModel)]="idString" (input)="onIdSearchStringChanged()" />
            </mat-form-field>
          </div>
          <div class="vsyncid">
            <mat-form-field appearance="fill">
              <mat-label>VSYNC ID</mat-label>
              <mat-select (selectionChange)="onVSyncIdFilterChanged($event)" multiple>
                <mat-option *ngFor="let vsyncId of uiData.allVSyncIds" [value]="vsyncId">
                  {{ vsyncId }}
                </mat-option>
              </mat-select>
            </mat-form-field>
          </div>
          <div class="pid">
            <mat-form-field appearance="fill">
              <mat-label>PID</mat-label>
              <mat-select (selectionChange)="onPidFilterChanged($event)" multiple>
                <mat-option *ngFor="let pid of uiData.allPids" [value]="pid">
                  {{ pid }}
                </mat-option>
              </mat-select>
            </mat-form-field>
          </div>
          <div class="uid">
            <mat-form-field appearance="fill">
              <mat-label>UID</mat-label>
              <mat-select (selectionChange)="onUidFilterChanged($event)" multiple>
                <mat-option *ngFor="let uid of uiData.allUids" [value]="uid">
                  {{ uid }}
                </mat-option>
              </mat-select>
            </mat-form-field>
          </div>
          <div class="type">
            <mat-form-field appearance="fill">
              <mat-label>Type</mat-label>
              <mat-select (selectionChange)="onTypeFilterChanged($event)" multiple>
                <mat-option *ngFor="let type of uiData.allTypes" [value]="type">
                  {{ type }}
                </mat-option>
              </mat-select>
            </mat-form-field>
          </div>
          <div class="id">
            <mat-form-field appearance="fill">
              <mat-label>LAYER/DISP ID</mat-label>
              <mat-select (selectionChange)="onLayerIdFilterChanged($event)" multiple>
                <mat-option *ngFor="let id of uiData.allLayerAndDisplayIds" [value]="id">
                  {{ id }}
                </mat-option>
              </mat-select>
            </mat-form-field>
          </div>
          <div class="what">
            <mat-form-field appearance="fill">
              <mat-label>Search text</mat-label>
              <input matInput [(ngModel)]="whatSearchString" (input)="onWhatSearchStringChange()" />
            </mat-form-field>
          </div>
        </div>

        <cdk-virtual-scroll-viewport itemSize="24" class="scroll">
          <div
            *cdkVirtualFor="let entry of uiData.entries; let i = index"
            class="entry"
            [class.current-entry]="isCurrentEntry(i)"
            [class.selected-entry]="isSelectedEntry(i)"
            (click)="onEntryClicked(i)">
            <div class="time">
              <span class="mat-body-1">{{ entry.time }}</span>
            </div>
            <div class="id">
              <span class="mat-body-1">{{ entry.transactionId }}</span>
            </div>
            <div class="vsyncid">
              <span class="mat-body-1">{{ entry.vsyncId }}</span>
            </div>
            <div class="pid">
              <span class="mat-body-1">{{ entry.pid }}</span>
            </div>
            <div class="uid">
              <span class="mat-body-1">{{ entry.uid }}</span>
            </div>
            <div class="type">
              <span class="mat-body-1">{{ entry.type }}</span>
            </div>
            <div class="id">
              <span class="mat-body-1">{{ entry.layerOrDisplayId }}</span>
            </div>
            <div class="what">
              <span class="mat-body-1">{{ entry.what }}</span>
            </div>
          </div>
        </cdk-virtual-scroll-viewport>
      </div>

      <mat-divider [vertical]="true"></mat-divider>

      <div class="container-properties">
        <h3 class="properties-title mat-title">Properties - Proto Dump</h3>
        <tree-view
          *ngIf="uiData.currentPropertiesTree"
          class="properties-tree"
          [item]="uiData.currentPropertiesTree"></tree-view>
      </div>
    </div>
  `,
  styles: [
    `
      .entries {
        flex: 3;
        display: flex;
        flex-direction: column;
        padding: 16px;
      }

      .container-properties {
        flex: 1;
        padding: 16px;
      }

      .entries .filters {
        display: flex;
        flex-direction: row;
      }

      .entries .scroll {
        flex: 1;
        height: 100%;
      }

      .scroll .entry {
        display: flex;
        flex-direction: row;
      }

      .filters div {
        flex: 1;
        padding: 4px;
      }

      .filters .vsyncid mat-form-field {
        width: 120px;
      }

      .filters div.time {
        flex: 2;
      }

      .filters div.what {
        flex: 3;
      }

      .filters .id mat-form-field {
        width: 150px;
      }

      .filters .what {
        margin-right: 16px;
      }

      .filters .what mat-form-field {
        width: 250px;
      }

      .entry div {
        flex: 1;
        padding: 4px;
      }

      .entry div.time {
        flex: 2;
      }

      .entry div.what {
        flex: 3;
      }

      .entry.current-entry {
        color: white;
        background-color: #365179;
      }

      .entry.selected-entry {
        color: white;
        background-color: #98aecd;
      }

      mat-form-field {
        width: 100px;
      }

      ::ng-deep .mat-select-panel-wrap {
        overflow: scroll;
        overflow-x: hidden;
        max-height: 75vh;
      }
    `,
  ],
})
class ViewerTransactionsComponent {
  uiData: UiData = UiData.EMPTY;
  idString = '';
  whatSearchString = '';

  @ViewChild(CdkVirtualScrollViewport) private scrollComponent?: CdkVirtualScrollViewport;
  private elementRef: ElementRef;

  constructor(@Inject(ElementRef) elementRef: ElementRef) {
    this.elementRef = elementRef;
  }

  @Input()
  set inputData(data: UiData) {
    this.uiData = data;
    if (this.uiData.scrollToIndex !== undefined && this.scrollComponent) {
      this.scrollComponent.scrollToIndex(this.uiData.scrollToIndex);
    }
  }

  onVSyncIdFilterChanged(event: MatSelectChange) {
    this.emitEvent(Events.VSyncIdFilterChanged, event.value);
  }

  onPidFilterChanged(event: MatSelectChange) {
    this.emitEvent(Events.PidFilterChanged, event.value);
  }

  onUidFilterChanged(event: MatSelectChange) {
    this.emitEvent(Events.UidFilterChanged, event.value);
  }

  onTypeFilterChanged(event: MatSelectChange) {
    this.emitEvent(Events.TypeFilterChanged, event.value);
  }

  onLayerIdFilterChanged(event: MatSelectChange) {
    this.emitEvent(Events.LayerIdFilterChanged, event.value);
  }

  onWhatSearchStringChange() {
    this.emitEvent(Events.WhatSearchStringChanged, this.whatSearchString);
  }

  onIdSearchStringChanged() {
    this.emitEvent(Events.IdFilterChanges, this.idString);
  }

  onEntryClicked(index: number) {
    this.emitEvent(Events.EntryClicked, index);
  }

  isCurrentEntry(index: number): boolean {
    return index === this.uiData.currentEntryIndex;
  }

  isSelectedEntry(index: number): boolean {
    return index === this.uiData.selectedEntryIndex;
  }

  private emitEvent(event: string, data: any) {
    const customEvent = new CustomEvent(event, {
      bubbles: true,
      detail: data,
    });
    this.elementRef.nativeElement.dispatchEvent(customEvent);
  }
}

export {ViewerTransactionsComponent};
