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
import {Component, ElementRef, Inject, Input} from '@angular/core';
import {ImeAdditionalProperties} from 'viewers/common/ime_additional_properties';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {ViewerEvents} from 'viewers/common/viewer_events';

@Component({
  selector: 'ime-additional-properties',
  template: `
    <h2 class="view-header mat-title">WM & SF Properties</h2>
    <div class="additional-properties-content">
      <div *ngIf="isAllPropertiesNull()" class="group">
        <p class="mat-body-1">
          There is no corresponding WM / SF additionalProperties for this IME entry – no WM / SF
          entry is recorded before this IME entry in time. View later frames for WM & SF properties.
        </p>
      </div>

      <ng-container *ngIf="isImeManagerService">
        <div class="group">
          <button
            *ngIf="wmProtoOrNull()"
            color="primary"
            mat-button
            class="group-header"
            [class]="{selected: isHighlighted(wmProtoOrNull())}"
            (click)="onClickShowInPropertiesPanel(wmProtoOrNull(), additionalProperties.wm?.name)">
            WMState
          </button>
          <h3 *ngIf="!wmProtoOrNull()" class="group-header mat-subheading-2">WMState</h3>
          <div class="left-column">
            <p *ngIf="additionalProperties.wm" class="mat-body-1">
              {{ additionalProperties.wm.name }}
            </p>
            <p *ngIf="!additionalProperties.wm" class="mat-body-1">
              There is no corresponding WMState entry.
            </p>
          </div>
        </div>
        <div *ngIf="wmInsetsSourceProviderOrNull()" class="group">
          <button
            color="primary"
            mat-button
            class="group-header"
            [class]="{selected: isHighlighted(wmInsetsSourceProviderOrNull())}"
            (click)="
              onClickShowInPropertiesPanel(
                wmInsetsSourceProviderOrNull(),
                'Ime Insets Source Provider'
              )
            ">
            IME Insets Source Provider
          </button>
          <div class="left-column">
            <p class="mat-body-2">Source Frame:</p>
            <coordinates-table
              [coordinates]="wmInsetsSourceProviderSourceFrameOrNull()"></coordinates-table>
            <p class="mat-body-1">
              <span class="mat-body-2">Source Visible:</span>
              &ngsp;
              {{ wmInsetsSourceProviderSourceVisibleOrNull() }}
            </p>
            <p class="mat-body-2">Source Visible Frame:</p>
            <coordinates-table
              [coordinates]="wmInsetsSourceProviderSourceVisibleFrameOrNull()"></coordinates-table>
            <p class="mat-body-1">
              <span class="mat-body-2">Position:</span>
              &ngsp;
              {{ wmInsetsSourceProviderPositionOrNull() }}
            </p>
            <p class="mat-body-1">
              <span class="mat-body-2">IsLeashReadyForDispatching:</span>
              &ngsp;
              {{ wmInsetsSourceProviderIsLeashReadyOrNull() }}
            </p>
            <p class="mat-body-1">
              <span class="mat-body-2">Controllable:</span>
              &ngsp;
              {{ wmInsetsSourceProviderControllableOrNull() }}
            </p>
          </div>
        </div>
        <div *ngIf="wmImeControlTargetOrNull()" class="group">
          <button
            color="primary"
            mat-button
            class="group-header"
            [class]="{selected: isHighlighted(wmImeControlTargetOrNull())}"
            (click)="
              onClickShowInPropertiesPanel(wmImeControlTargetOrNull(), 'Ime Control Target')
            ">
            IME Control Target
          </button>
          <div class="left-column">
            <p *ngIf="wmImeControlTargetTitleOrNull()" class="mat-body-1">
              <span class="mat-body-2">Title:</span>
              &ngsp;
              {{ wmImeControlTargetTitleOrNull() }}
            </p>
          </div>
        </div>
        <div *ngIf="wmImeInputTargetOrNull()" class="group">
          <button
            color="primary"
            mat-button
            class="group-header"
            [class]="{selected: isHighlighted(wmImeInputTargetOrNull())}"
            (click)="onClickShowInPropertiesPanel(wmImeInputTargetOrNull(), 'Ime Input Target')">
            IME Input Target
          </button>
          <div class="left-column">
            <p *ngIf="wmImeInputTargetTitleOrNull()" class="mat-body-1">
              <span class="mat-body-2">Title:</span>
              &ngsp;
              {{ wmImeInputTargetTitleOrNull() }}
            </p>
          </div>
        </div>
        <div *ngIf="wmImeLayeringTargetOrNull()" class="group">
          <button
            color="primary"
            mat-button
            class="group-header"
            [class]="{selected: isHighlighted(wmImeLayeringTargetOrNull())}"
            (click)="
              onClickShowInPropertiesPanel(wmImeLayeringTargetOrNull(), 'Ime Layering Target')
            ">
            IME Layering Target
          </button>
          <div class="left-column">
            <p *ngIf="wmImeLayeringTargetTitleOrNull()" class="mat-body-1">
              <span class="mat-body-2">Title:</span>
              &ngsp;
              {{ wmImeLayeringTargetTitleOrNull() }}
            </p>
          </div>
        </div>
      </ng-container>

      <ng-container *ngIf="!isImeManagerService">
        <!-- Ime Client or Ime Service -->
        <div class="group">
          <button
            *ngIf="wmProtoOrNull()"
            color="primary"
            mat-button
            class="group-header"
            [class]="{selected: isHighlighted(wmProtoOrNull())}"
            (click)="onClickShowInPropertiesPanel(wmProtoOrNull(), additionalProperties.wm?.name)">
            WMState
          </button>
          <h3 *ngIf="!wmProtoOrNull()" class="group-header mat-subheading-2">WMState</h3>
          <div class="left-column">
            <p *ngIf="additionalProperties.wm" class="mat-body-1">
              {{ additionalProperties.wm.name }}
            </p>
            <p *ngIf="!additionalProperties.wm" class="mat-body-1">
              There is no corresponding WMState entry.
            </p>
          </div>
        </div>
        <div class="group">
          <h3 class="group-header mat-subheading-2">SFLayer</h3>
          <div class="left-column">
            <p *ngIf="additionalProperties.sf" class="mat-body-1">
              {{ additionalProperties.sf.name }}
            </p>
            <p *ngIf="!additionalProperties.sf" class="mat-body-1">
              There is no corresponding SFLayer entry.
            </p>
          </div>
        </div>
        <div *ngIf="additionalProperties.wm" class="group">
          <h3 class="group-header mat-subheading-2">Focus</h3>
          <div class="left-column">
            <p class="mat-body-1">
              <span class="mat-body-2">Focused App:</span>
              &ngsp;
              {{ additionalProperties.wm.focusedApp }}
            </p>
            <p class="mat-body-1">
              <span class="mat-body-2">Focused Activity:</span>
              &ngsp;
              {{ additionalProperties.wm.focusedActivity }}
            </p>
            <p class="mat-body-1">
              <span class="mat-body-2">Focused Window:</span>
              &ngsp;
              {{ additionalProperties.wm.focusedWindow }}
            </p>
            <p *ngIf="additionalProperties.sf" class="mat-body-1">
              <span class="mat-body-2">Focused Window Color:</span>
              &ngsp;
              {{ additionalProperties.sf.focusedWindow?.color }}
            </p>
            <p class="mat-body-2">Input Control Target Frame:</p>
            <coordinates-table [coordinates]="wmControlTargetFrameOrNull()"></coordinates-table>
          </div>
        </div>
        <div class="group">
          <h3 class="group-header mat-subheading-2">Visibility</h3>
          <div class="left-column">
            <p *ngIf="additionalProperties.wm" class="mat-body-1">
              <span class="mat-body-2">InputMethod Window:</span>
              &ngsp;
              {{ additionalProperties.wm.isInputMethodWindowVisible }}
            </p>
            <p *ngIf="additionalProperties.sf" class="mat-body-1">
              <span class="mat-body-2">InputMethod Surface:</span>
              &ngsp;
              {{ additionalProperties.sf.inputMethodSurface?.isInputMethodSurfaceVisible ?? false }}
            </p>
          </div>
        </div>
        <div *ngIf="additionalProperties.sf" class="group">
          <button
            color="primary"
            mat-button
            class="group-header"
            [class]="{selected: isHighlighted(additionalProperties.sf.imeContainer)}"
            (click)="onClickShowInPropertiesPanel(additionalProperties.sf.imeContainer)">
            Ime Container
          </button>
          <div class="left-column">
            <p class="mat-body-1">
              <span class="mat-body-2">ZOrderRelativeOfId:</span>
              &ngsp;
              {{ additionalProperties.sf.imeContainer.zOrderRelativeOfId }}
            </p>
            <p class="mat-body-1">
              <span class="mat-body-2">Z:</span>
              &ngsp;
              {{ additionalProperties.sf.imeContainer.z }}
            </p>
          </div>
        </div>
        <div *ngIf="additionalProperties.sf" class="group">
          <button
            color="primary"
            mat-button
            class="group-header"
            [class]="{selected: isHighlighted(additionalProperties.sf.inputMethodSurface)}"
            (click)="onClickShowInPropertiesPanel(additionalProperties.sf.inputMethodSurface)">
            Input Method Surface
          </button>
          <div class="left-column">
            <p class="mat-body-2">ScreenBounds:</p>
            <coordinates-table
              [coordinates]="sfImeContainerScreenBoundsOrNull()"></coordinates-table>
          </div>
          <div class="right-column">
            <p class="mat-body-2">Rect:</p>
            <coordinates-table [coordinates]="sfImeContainerRectOrNull()"></coordinates-table>
          </div>
        </div>
      </ng-container>
    </div>
  `,
  styles: [
    `
      .view-header {
        border-bottom: 1px solid var(--border-color);
      }

      .additional-properties-content {
        height: 0;
        flex-grow: 1;
        overflow-y: auto;
      }

      .group {
        padding: 8px;
        display: flex;
        flex-direction: row;
        border-bottom: 1px solid var(--border-color);
      }

      .mat-body-1 {
        overflow-wrap: anywhere;
      }

      .group-header {
        height: 100%;
        width: 80px;
        padding: 0;
        text-align: center;
        line-height: normal;
        white-space: normal;
      }

      p.group-header {
        color: gray;
      }

      .left-column {
        flex: 1;
        padding: 0 5px;
      }

      .right-column {
        flex: 1;
        padding: 0 5px;
      }
    `,
  ],
})
export class ImeAdditionalPropertiesComponent {
  @Input() additionalProperties!: ImeAdditionalProperties;
  @Input() isImeManagerService?: boolean;
  @Input() highlightedItems: string[] = [];

  constructor(@Inject(ElementRef) private elementRef: ElementRef) {}

  isHighlighted(item: any) {
    return UiTreeUtils.isHighlighted(item, this.highlightedItems);
  }

  formatProto(item: any) {
    if (item?.prettyPrint) {
      return item.prettyPrint();
    }
  }

  wmProtoOrNull() {
    return this.additionalProperties.wm?.proto;
  }

  wmInsetsSourceProviderOrNull() {
    return this.additionalProperties.wm?.protoImeInsetsSourceProvider
      ? Object.assign(
          {name: 'Ime Insets Source Provider'},
          this.additionalProperties.wm.protoImeInsetsSourceProvider
        )
      : null;
  }

  wmControlTargetFrameOrNull() {
    return (
      this.additionalProperties.wm?.protoImeInsetsSourceProvider?.insetsSourceProvider
        ?.controlTarget?.windowFrames?.frame || 'null'
    );
  }

  wmInsetsSourceProviderPositionOrNull() {
    return (
      this.additionalProperties.wm?.protoImeInsetsSourceProvider?.insetsSourceProvider?.control
        ?.position || 'null'
    );
  }

  wmInsetsSourceProviderIsLeashReadyOrNull() {
    return (
      this.additionalProperties.wm?.protoImeInsetsSourceProvider?.insetsSourceProvider
        ?.isLeashReadyForDispatching || 'null'
    );
  }

  wmInsetsSourceProviderControllableOrNull() {
    return (
      this.additionalProperties.wm?.protoImeInsetsSourceProvider?.insetsSourceProvider
        ?.controllable || 'null'
    );
  }

  wmInsetsSourceProviderSourceFrameOrNull() {
    return (
      this.additionalProperties.wm?.protoImeInsetsSourceProvider?.insetsSourceProvider?.source
        ?.frame || 'null'
    );
  }

  wmInsetsSourceProviderSourceVisibleOrNull() {
    return (
      this.additionalProperties.wm?.protoImeInsetsSourceProvider?.insetsSourceProvider?.source
        ?.visible || 'null'
    );
  }

  wmInsetsSourceProviderSourceVisibleFrameOrNull() {
    return (
      this.additionalProperties.wm?.protoImeInsetsSourceProvider?.insetsSourceProvider?.source
        ?.visibleFrame || 'null'
    );
  }

  wmImeControlTargetOrNull() {
    return this.additionalProperties?.wm?.protoImeControlTarget
      ? Object.assign(
          {name: 'IME Control Target'},
          this.additionalProperties.wm.protoImeControlTarget
        )
      : null;
  }

  wmImeControlTargetTitleOrNull() {
    return (
      this.additionalProperties?.wm?.protoImeControlTarget?.windowContainer?.identifier?.title ||
      'null'
    );
  }

  wmImeInputTargetOrNull() {
    return this.additionalProperties?.wm?.protoImeInputTarget
      ? Object.assign({name: 'IME Input Target'}, this.additionalProperties.wm.protoImeInputTarget)
      : null;
  }

  wmImeInputTargetTitleOrNull() {
    return (
      this.additionalProperties?.wm?.protoImeInputTarget?.windowContainer?.identifier?.title ||
      'null'
    );
  }
  wmImeLayeringTargetOrNull() {
    return this.additionalProperties?.wm?.protoImeLayeringTarget
      ? Object.assign(
          {name: 'IME Layering Target'},
          this.additionalProperties.wm.protoImeLayeringTarget
        )
      : null;
  }

  wmImeLayeringTargetTitleOrNull() {
    return (
      this.additionalProperties?.wm?.protoImeLayeringTarget?.windowContainer?.identifier?.title ||
      'null'
    );
  }

  sfImeContainerScreenBoundsOrNull() {
    return this.additionalProperties.sf?.inputMethodSurface?.screenBounds || 'null';
  }

  sfImeContainerRectOrNull() {
    return this.additionalProperties.sf?.inputMethodSurface?.rect || 'null';
  }

  isAllPropertiesNull() {
    if (this.isImeManagerService) {
      return !this.additionalProperties.wm;
    } else {
      return !(this.additionalProperties.wm || this.additionalProperties.sf);
    }
  }

  onClickShowInPropertiesPanel(item: any, name?: string) {
    if (item.id) {
      this.updateHighlightedItems(item.id);
    } else {
      this.updateAdditionalPropertySelected(item, name);
    }
  }

  private updateHighlightedItems(newId: string) {
    const event: CustomEvent = new CustomEvent(ViewerEvents.HighlightedChange, {
      bubbles: true,
      detail: {id: newId},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  private updateAdditionalPropertySelected(item: any, name?: string) {
    const itemWrapper = {
      name,
      proto: item,
    };
    const event: CustomEvent = new CustomEvent(ViewerEvents.AdditionalPropertySelected, {
      bubbles: true,
      detail: {selectedItem: itemWrapper},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }
}
