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
import { Component, ElementRef, Inject, Input } from "@angular/core";
import { ImeAdditionalProperties } from "viewers/common/ime_additional_properties";
import { UiTreeUtils } from "viewers/common/ui_tree_utils";
import { ViewerEvents } from "viewers/common/viewer_events";

@Component({
  selector: "ime-additional-properties",
  template: `
    <mat-card-header class="view-header">
      <div class="title-filter">
        <span class="additional-properties-title">WM & SF Properties</span>
      </div>
    </mat-card-header>
    <mat-card-content class="additional-properties-content">
      <div *ngIf="isAllPropertiesNull()" class="group">
        There is no corresponding WM / SF additionalProperties for this IME entry –
        no WM / SF entry is recorded before this IME entry in time.
        View later frames for WM & SF properties.
      </div>

      <div *ngIf="isImeManagerService">
        <div class="group">
          <button
              class="text-button group-header"
              *ngIf="wmProtoOrNull()"
              [class]="{ 'selected': isHighlighted(wmProtoOrNull()) }"
              (click)="onClickShowInPropertiesPanel(wmProtoOrNull(), additionalProperties.wm?.name)">
            WMState
          </button>
          <span class="group-header" *ngIf="!wmProtoOrNull()">WMState</span>
          <div class="full-width">
            <span class="value" *ngIf="additionalProperties.wm">{{
              additionalProperties.wm.name }}</span>
            <span *ngIf="!additionalProperties.wm">There is no corresponding WMState entry.</span>
          </div>
        </div>
        <div class="group" *ngIf="wmInsetsSourceProviderOrNull()">
          <button
              class="text-button group-header"
              [class]="{ 'selected': isHighlighted(wmInsetsSourceProviderOrNull()) }"
              (click)="onClickShowInPropertiesPanel(wmInsetsSourceProviderOrNull(), 'Ime Insets Source Provider')">
            IME Insets Source Provider
          </button>
          <div class="full-width">
            <div></div>
            <span class="key">Source Frame:</span>
            <coordinates-table
              [coordinates]="wmInsetsSourceProviderSourceFrameOrNull()"
            ></coordinates-table>
            <div></div>
            <span class="key">Source Visible:</span>
            <span class="value">{{
                wmInsetsSourceProviderSourceVisibleOrNull() }}</span>
            <div></div>
            <span class="key">Source Visible Frame:</span>
            <coordinates-table
              [coordinates]="wmInsetsSourceProviderSourceVisibleFrameOrNull()"
            ></coordinates-table>
            <div></div>
            <span class="key">Position:</span>
            <span class="value">{{ wmInsetsSourceProviderPositionOrNull() }}</span>
            <div></div>
            <span class="key">IsLeashReadyForDispatching:</span>
            <span class="value">{{
                wmInsetsSourceProviderIsLeashReadyOrNull() }}</span>
            <div></div>
            <span class="key">Controllable:</span>
            <span class="value">{{
                wmInsetsSourceProviderControllableOrNull() }}</span>
            <div></div>
          </div>
        </div>
        <div class="group" *ngIf="wmImeControlTargetOrNull()">
          <button
              class="text-button group-header"
              [class]="{ 'selected': isHighlighted(wmImeControlTargetOrNull()) }"
              (click)="onClickShowInPropertiesPanel(wmImeControlTargetOrNull(), 'Ime Control Target')">
            IME Control Target
          </button>
          <div class="full-width">
            <span class="key" *ngIf="wmImeControlTargetTitleOrNull()">Title:</span>
            <span class="value" *ngIf="wmImeControlTargetTitleOrNull()">{{
                wmImeControlTargetTitleOrNull() }}</span>
          </div>
        </div>
        <div class="group" *ngIf="wmImeInputTargetOrNull()">
          <button
              class="text-button group-header"
              [class]="{ 'selected': isHighlighted(wmImeInputTargetOrNull()) }"
              (click)="onClickShowInPropertiesPanel(wmImeInputTargetOrNull(), 'Ime Input Target')">
            IME Input Target
          </button>
          <div class="full-width">
            <span class="key" *ngIf="wmImeInputTargetTitleOrNull()">Title:</span>
            <span class="value" *ngIf="wmImeInputTargetTitleOrNull()">{{
                wmImeInputTargetTitleOrNull() }}</span>
          </div>
        </div>
        <div class="group" *ngIf="wmImeLayeringTargetOrNull()">
          <button
              class="text-button group-header"
              [class]="{ 'selected': isHighlighted(wmImeLayeringTargetOrNull()) }"
              (click)="onClickShowInPropertiesPanel(wmImeLayeringTargetOrNull(), 'Ime Layering Target')">
            IME Layering Target
          </button>
          <div class="full-width">
            <span class="key" *ngIf="wmImeLayeringTargetTitleOrNull()">Title:</span>
            <span class="value" *ngIf="wmImeLayeringTargetTitleOrNull()">{{
                wmImeLayeringTargetTitleOrNull() }}</span>
          </div>
        </div>
      </div>

      <div *ngIf="!isImeManagerService">
        <!-- Ime Client or Ime Service -->
        <div class="group">
          <button
              class="text-button group-header"
              *ngIf="wmProtoOrNull()"
              [class]="{ 'selected': isHighlighted(wmProtoOrNull()) }"
              (click)="onClickShowInPropertiesPanel(wmProtoOrNull(), additionalProperties.wm?.name)">
            WMState
          </button>
          <span class="group-header" *ngIf="!wmProtoOrNull()">WMState</span>
          <div class="full-width">
            <span class="value" *ngIf="additionalProperties.wm">{{
              additionalProperties.wm.name }}</span>
            <span *ngIf="!additionalProperties.wm">There is no corresponding WMState entry.</span>
          </div>
        </div>
        <div class="group">
          <span class="group-header">SFLayer</span>
          <div class="full-width">
            <span class="value" *ngIf="additionalProperties.sf">{{
              additionalProperties.sf.name }}</span>
            <span *ngIf="!additionalProperties.sf">There is no corresponding SFLayer entry.</span>
          </div>
        </div>
        <div class="group" *ngIf="additionalProperties.wm">
          <span class="group-header">Focus</span>
          <div class="full-width">
            <span class="key">Focused App:</span>
            <span class="value">{{ additionalProperties.wm.focusedApp }}</span>
            <div></div>
            <span class="key">Focused Activity:</span>
            <span class="value">{{ additionalProperties.wm.focusedActivity }}</span>
            <div></div>
            <span class="key">Focused Window:</span>
            <span class="value">{{ additionalProperties.wm.focusedWindow }}</span>
            <div></div>
            <span class="key" *ngIf="additionalProperties.sf">Focused Window Color:</span>
            <span class="value" *ngIf="additionalProperties.sf">{{
              additionalProperties.sf.focusedWindow.color
              }}</span>
            <div></div>
            <span class="key">Input Control Target Frame:</span>
            <coordinates-table
              [coordinates]="wmControlTargetFrameOrNull()"
            ></coordinates-table>
            <div></div>
          </div>
        </div>
        <div class="group">
          <span class="group-header">Visibility</span>
          <div class="full-width">
            <span class="key" *ngIf="additionalProperties.wm">InputMethod Window:</span>
            <span class="value" *ngIf="additionalProperties.wm">{{
              additionalProperties.wm.isInputMethodWindowVisible
              }}</span>
            <div></div>
            <span class="key" *ngIf="additionalProperties.sf">InputMethod Surface:</span>
            <span class="value" *ngIf="additionalProperties.sf">{{
              additionalProperties.sf.inputMethodSurface.isInputMethodSurfaceVisible }}</span>
            <div></div>
          </div>
        </div>
        <div class="group" *ngIf="additionalProperties.sf">
          <button
              class="text-button group-header"
              [class]="{ 'selected': isHighlighted(additionalProperties.sf.imeContainer) }"
              (click)="onClickShowInPropertiesPanel(additionalProperties.sf.imeContainer)">
            Ime Container
          </button>
          <div class="full-width">
            <span class="key">ZOrderRelativeOfId:</span>
            <span class="value">{{
              additionalProperties.sf.imeContainer.zOrderRelativeOfId
              }}</span>
            <div></div>
            <span class="key">Z:</span>
            <span class="value">{{ additionalProperties.sf.imeContainer.z }}</span>
            <div></div>
          </div>
        </div>
        <div class="group" *ngIf="additionalProperties.sf">
          <button
              class="text-button group-header"
              [class]="{
                'selected': isHighlighted(additionalProperties.sf.inputMethodSurface)
              }"
              (click)="onClickShowInPropertiesPanel(
                additionalProperties.sf.inputMethodSurface)">
            Input Method Surface
          </button>
          <div class="full-width">
            <span class="key">ScreenBounds:</span>
            <coordinates-table
              [coordinates]="sfImeContainerScreenBoundsOrNull()"
            ></coordinates-table>
            <div></div>
            <span class="key">Rect:</span>
            <coordinates-table
              [coordinates]="sfImeContainerRectOrNull()"
            ></coordinates-table>
            <div></div>
          </div>
        </div>
      </div>
    </mat-card-content>
  `,
  styles: [
    `
      .view-header {
        width: 100%;
        height: 2.5rem;
        border-bottom: 1px solid var(--default-border);
      }

      .title-filter {
        position: relative;
        display: flex;
        align-items: center;
        width: 100%;
        margin-bottom: 12px;
      }

      .additional-properties-title {
        font-weight: medium;
        font-size: 16px;
      }

      .additional-properties-content {
        display: flex;
        flex-direction: column;
        height: 375px;
        overflow-y: auto;
        overflow-x: hidden;
      }

      .container {
        overflow: auto;
      }

      .group {
        padding: 0.5rem;
        border-bottom: thin solid rgba(0, 0, 0, 0.12);
        flex-direction: row;
        display: flex;
      }

      .group .key {
        font-weight: bold;
      }

      .group .value {
        color: rgba(0, 0, 0, 0.75);
        word-break: break-all !important;
      }

      .group-header {
        justify-content: center;
        text-align: left;
        padding: 0px 5px;
        width: 95px;
        display: inline-block;
        font-size: bigger;
        color: grey;
        word-break: break-word;
      }

      .left-column {
        width: 320px;
        max-width: 100%;
        display: inline-block;
        vertical-align: top;
        overflow: auto;
        padding-right: 20px;
      }

      .right-column {
        width: 320px;
        max-width: 100%;
        display: inline-block;
        vertical-align: top;
        overflow: auto;
      }

      .full-width {
        width: 100%;
        display: inline-block;
        vertical-align: top;
        overflow: auto;
      }

      .column-header {
        font-weight: medium;
        font-size: smaller;
      }

      .element-summary {
        padding-top: 1rem;
      }

      .element-summary .key {
        font-weight: bold;
      }

      .element-summary .value {
        color: rgba(0, 0, 0, 0.75);
      }

      .tree-view {
        overflow: auto;
      }

      .text-button {
        border: none;
        cursor: pointer;
        font-size: 14px;
        font-family: roboto;
        color: blue;
        text-decoration: underline;
        text-decoration-color: blue;
        background-color: inherit;
      }

      .text-button:focus {
        color: purple;
      }

      .text-button.selected {
        color: purple;
      }


    `
  ],
})

export class ImeAdditionalPropertiesComponent {
  @Input() additionalProperties!: ImeAdditionalProperties;
  @Input() isImeManagerService?: boolean;
  @Input() highlightedItems: Array<string> = [];

  constructor(
    @Inject(ElementRef) private elementRef: ElementRef,
  ) {}

  public isHighlighted(item: any) {
    return UiTreeUtils.isHighlighted(item, this.highlightedItems);
  }

  public formatProto(item: any) {
    if (item?.prettyPrint) {
      return item.prettyPrint();
    }
  }

  public wmProtoOrNull() {
    return this.additionalProperties.wm?.proto;
  }

  public wmInsetsSourceProviderOrNull() {
    return this.additionalProperties.wm?.protoImeInsetsSourceProvider ?
      Object.assign({ "name": "Ime Insets Source Provider" },
        this.additionalProperties.wm.protoImeInsetsSourceProvider) :
      null;
  }

  public wmControlTargetFrameOrNull() {
    return this.additionalProperties.wm?.protoImeInsetsSourceProvider
      ?.insetsSourceProvider?.controlTarget?.windowFrames?.frame || "null";
  }

  public wmInsetsSourceProviderPositionOrNull() {
    return this.additionalProperties.wm?.protoImeInsetsSourceProvider
      ?.insetsSourceProvider?.control?.position || "null";
  }

  public wmInsetsSourceProviderIsLeashReadyOrNull() {
    return this.additionalProperties.wm?.protoImeInsetsSourceProvider
      ?.insetsSourceProvider?.isLeashReadyForDispatching || "null";
  }

  public wmInsetsSourceProviderControllableOrNull() {
    return this.additionalProperties.wm?.protoImeInsetsSourceProvider
      ?.insetsSourceProvider?.controllable || "null";
  }

  public wmInsetsSourceProviderSourceFrameOrNull() {
    return this.additionalProperties.wm?.protoImeInsetsSourceProvider
      ?.insetsSourceProvider?.source?.frame || "null";
  }

  public wmInsetsSourceProviderSourceVisibleOrNull() {
    return this.additionalProperties.wm?.protoImeInsetsSourceProvider
      ?.insetsSourceProvider?.source?.visible || "null";
  }

  public wmInsetsSourceProviderSourceVisibleFrameOrNull() {
    return this.additionalProperties.wm?.protoImeInsetsSourceProvider
      ?.insetsSourceProvider?.source?.visibleFrame || "null";
  }

  public wmImeControlTargetOrNull() {
    return this.additionalProperties?.wm?.protoImeControlTarget ?
      Object.assign({ "name": "IME Control Target" },
        this.additionalProperties.wm.protoImeControlTarget) :
      null;
  }

  public wmImeControlTargetTitleOrNull() {
    return this.additionalProperties?.wm?.protoImeControlTarget?.windowContainer
      ?.identifier?.title || "null";
  }

  public wmImeInputTargetOrNull() {
    return this.additionalProperties?.wm?.protoImeInputTarget ?
      Object.assign({ "name": "IME Input Target" },
        this.additionalProperties.wm.protoImeInputTarget) :
      null;
  }

  public wmImeInputTargetTitleOrNull() {
    return this.additionalProperties?.wm?.protoImeInputTarget?.windowContainer
      ?.identifier?.title || "null";
  }
  public wmImeLayeringTargetOrNull() {
    return this.additionalProperties?.wm?.protoImeLayeringTarget ?
      Object.assign({ "name": "IME Layering Target" },
        this.additionalProperties.wm.protoImeLayeringTarget) :
      null;
  }

  public wmImeLayeringTargetTitleOrNull() {
    return this.additionalProperties?.wm?.protoImeLayeringTarget?.windowContainer
      ?.identifier?.title || "null";
  }

  public sfImeContainerScreenBoundsOrNull() {
    return this.additionalProperties.sf?.inputMethodSurface.screenBounds || "null";
  }

  public sfImeContainerRectOrNull() {
    return this.additionalProperties.sf?.inputMethodSurface.rect || "null";
  }

  public isAllPropertiesNull() {
    if (this.isImeManagerService) {
      return !this.additionalProperties.wm;
    } else {
      return !(this.additionalProperties.wm ||
        this.additionalProperties.sf);
    }
  }

  public onClickShowInPropertiesPanel(item: any, name?: string) {
    if (item.id) {
      this.updateHighlightedItems(item.id);
    } else {
      this.updateAdditionalPropertySelected(item, name);
    }
  }

  private updateHighlightedItems(newId: string) {
    const event: CustomEvent = new CustomEvent(
      ViewerEvents.HighlightedChange,
      {
        bubbles: true,
        detail: { id: newId }
      });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  private updateAdditionalPropertySelected(item: any, name?: string) {
    const itemWrapper = {
      name: name,
      proto: item,
    };
    const event: CustomEvent = new CustomEvent(
      ViewerEvents.AdditionalPropertySelected,
      {
        bubbles: true,
        detail: { selectedItem: itemWrapper }
      });
    this.elementRef.nativeElement.dispatchEvent(event);
  }
}
