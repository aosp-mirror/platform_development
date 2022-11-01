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
import {CdkVirtualScrollViewport} from "@angular/cdk/scrolling";
import {
  Component, ElementRef, Inject, Input, ViewChild
} from "@angular/core";
import {MatSelectChange} from "@angular/material/select";
import {Events} from "./events";
import {UiData} from "./ui_data";

@Component({
  selector: "viewer-transactions",
  template: `
    <div class="card-grid">
      <div class="entries">
        <div class="filters">
          <div class="time">
          </div>
          <div class="vsyncid" style="display:table">
            <span class="mat-body-1"
                  style="display:table-cell; vertical-align:middle">VSYNC ID</span>
          </div>
          <div class="pid">
            <mat-form-field appearance="fill">
              <mat-label>PID</mat-label>
              <mat-select (selectionChange)="onPidFilterChanged($event)"
                          multiple>
                <mat-option *ngFor="let pid of uiData.allPids"
                            [value]="pid">
                  {{pid}}
                </mat-option>
              </mat-select>
            </mat-form-field>
          </div>
          <div class="uid">
            <mat-form-field appearance="fill">
              <mat-label>UID</mat-label>
              <mat-select (selectionChange)="onUidFilterChanged($event)"
                          multiple>
                <mat-option *ngFor="let uid of uiData.allUids"
                            [value]="uid">
                  {{uid}}
                </mat-option>
              </mat-select>
            </mat-form-field>
          </div>
          <div class="type">
            <mat-form-field appearance="fill">
              <mat-label>Type</mat-label>
              <mat-select (selectionChange)="onTypeFilterChanged($event)"
                          multiple>
                <mat-option *ngFor="let type of uiData.allTypes"
                            [value]="type">
                  {{type}}
                </mat-option>
              </mat-select>
            </mat-form-field>
          </div>
          <div class="id">
            <mat-form-field appearance="fill" style="width: 200px;">
              <mat-label>LAYER/DISPLAY ID</mat-label>
              <mat-select (selectionChange)="onIdFilterChanged($event)"
                          multiple>
                <mat-option *ngFor="let id of uiData.allIds"
                            [value]="id">
                  {{id}}
                </mat-option>
              </mat-select>
            </mat-form-field>
          </div>
        </div>

        <cdk-virtual-scroll-viewport itemSize="24" class="scroll">
          <div *cdkVirtualFor="let entry of uiData.entries; let i = index;"
               class="entry"
               [class.current-entry]="isCurrentEntry(i)"
               [class.selected-entry]="isSelectedEntry(i)"
               (click)="onEntryClicked(i)">
            <div class="time">
              <span class="mat-body-1">{{entry.time}}</span>
            </div>
            <div class="vsyncid">
              <span class="mat-body-1">{{entry.vsyncId}}</span>
            </div>
            <div class="pid">
              <span class="mat-body-1">{{entry.pid}}</span>
            </div>
            <div class="uid">
              <span class="mat-body-1">{{entry.uid}}</span>
            </div>
            <div class="type">
              <span class="mat-body-1">{{entry.type}}</span>
            </div>
            <div class="id">
              <span class="mat-body-1">{{entry.id}}</span>
            </div>
          </div>
        </cdk-virtual-scroll-viewport>
      </div>

      <mat-divider [vertical]="true"></mat-divider>

      <div class="container-properties">
        <h3 class="properties-title mat-title">Properties - Proto Dump</h3>
        <tree-view
            class="properties-tree"
            [item]="uiData.currentPropertiesTree"
        ></tree-view>
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
        margin-right: 8px;
      }

      .entry div {
        flex: 1;
        margin: 4px;
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
    `,
  ]
})
class ViewerTransactionsComponent {
  constructor(@Inject(ElementRef) elementRef: ElementRef) {
    this.elementRef = elementRef;
  }

  @Input()
  public set inputData(data: UiData) {
    this.uiData = data;
    if (this.uiData.scrollToIndex !== undefined && this.scrollComponent) {
      this.scrollComponent.scrollToIndex(this.uiData.scrollToIndex);
    }
  }

  public onPidFilterChanged(event: MatSelectChange) {
    this.emitEvent(Events.PidFilterChanged, event.value);
  }

  public onUidFilterChanged(event: MatSelectChange) {
    this.emitEvent(Events.UidFilterChanged, event.value);
  }

  public onTypeFilterChanged(event: MatSelectChange) {
    this.emitEvent(Events.TypeFilterChanged, event.value);
  }

  public onIdFilterChanged(event: MatSelectChange) {
    this.emitEvent(Events.IdFilterChanged, event.value);
  }

  public onEntryClicked(index: number) {
    this.emitEvent(Events.EntryClicked, index);
  }

  public isCurrentEntry(index: number): boolean {
    return index === this.uiData.currentEntryIndex;
  }

  public isSelectedEntry(index: number): boolean {
    return index === this.uiData.selectedEntryIndex;
  }

  private emitEvent(event: string, data: any) {
    const customEvent = new CustomEvent(
      event,
      {
        bubbles: true,
        detail: data
      });
    this.elementRef.nativeElement.dispatchEvent(customEvent);
  }

  @ViewChild(CdkVirtualScrollViewport) scrollComponent?: CdkVirtualScrollViewport;

  public uiData: UiData = UiData.EMPTY;
  private elementRef: ElementRef;
}

export {ViewerTransactionsComponent};
