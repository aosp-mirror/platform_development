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
  Component,
  ElementRef,
  EventEmitter,
  Inject,
  Input,
  Output,
} from '@angular/core';
import {EMPTY_OBJ_STRING} from 'trace/tree_node/formatters';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TreeNode} from 'trace/tree_node/tree_node';
import {ImeAdditionalProperties} from 'viewers/common/ime_additional_properties';
import {
  ImeContainerProperties,
  InputMethodSurfaceProperties,
} from 'viewers/common/ime_utils';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {selectedElementStyle} from './styles/selected_element.styles';
import {viewerCardInnerStyle} from './styles/viewer_card.styles';

@Component({
  selector: 'ime-additional-properties',
  template: `
    <div class="title-section">
      <collapsible-section-title
        class="view-header"
        title="WM & SF PROPERTIES"
        (collapseButtonClicked)="collapseButtonClicked.emit()"></collapsible-section-title>
    </div>

    <span class="mat-body-1 placeholder-text" *ngIf="!additionalProperties"> No IME entry found. </span>

    <div class="additional-properties-content" *ngIf="additionalProperties">
      <div *ngIf="isAllPropertiesUndefined()" class="group">
        <p class="mat-body-1">
          There is no corresponding WM / SF additionalProperties for this IME entry – no WM / SF
          entry is recorded before this IME entry in time. View later frames for WM & SF properties.
        </p>
      </div>

      <ng-container *ngIf="isImeManagerService">
        <div class="group ime-manager-service">
          <button
            *ngIf="wmHierarchyTree()"
            [color]="getButtonColor(wmHierarchyTree())"
            mat-button
            class="group-header"
            [class]="{selected: isHighlighted(wmHierarchyTree())}"
            (click)="onClickShowInPropertiesPanelWm(wmHierarchyTree(), 'Window Manager State')">
            WMState
          </button>
          <h3 *ngIf="!wmHierarchyTree()" class="group-header mat-subheading-2">WMState</h3>
          <div class="left-column wm-state">
            <p *ngIf="additionalProperties?.wm" class="mat-body-1">
              {{ wmRootLabel() }}
            </p>
            <p *ngIf="!additionalProperties?.wm" class="mat-body-1">
              There is no corresponding WMState entry.
            </p>
          </div>
        </div>
        <div *ngIf="wmInsetsSourceProvider()" class="group insets-source-provider">
          <button
            [color]="getButtonColor(wmInsetsSourceProvider())"
            mat-button
            class="group-header"
            [class]="{selected: isHighlighted(wmInsetsSourceProvider())}"
            (click)="
              onClickShowInPropertiesPanelWm(wmInsetsSourceProvider(), 'Ime Insets Source Provider')
            ">
            IME Insets Source Provider
          </button>
          <div class="left-column">
            <p class="mat-body-2">Source Frame:</p>
            <coordinates-table
              [coordinates]="wmInsetsSourceProviderSourceFrame()"></coordinates-table>
            <p class="mat-body-1">
              <span class="mat-body-2">Source Visible:</span>
              &ngsp;
              {{ wmInsetsSourceProviderSourceVisible() }}
            </p>
            <p class="mat-body-2">Source Visible Frame:</p>
            <coordinates-table
              [coordinates]="wmInsetsSourceProviderSourceVisibleFrame()"></coordinates-table>
            <p class="mat-body-1">
              <span class="mat-body-2">Position:</span>
              &ngsp;
              {{ wmInsetsSourceProviderPosition() }}
            </p>
            <p class="mat-body-1">
              <span class="mat-body-2">IsLeashReadyForDispatching:</span>
              &ngsp;
              {{ wmInsetsSourceProviderIsLeashReady() }}
            </p>
            <p class="mat-body-1">
              <span class="mat-body-2">Controllable:</span>
              &ngsp;
              {{ wmInsetsSourceProviderControllable() }}
            </p>
          </div>
        </div>
        <div *ngIf="wmImeControlTarget()" class="group ime-control-target">
          <button
          [color]="getButtonColor(wmImeControlTarget())"
            mat-button
            class="group-header ime-control-target-button"
            [class]="{selected: isHighlighted(wmImeControlTarget())}"
            (click)="onClickShowInPropertiesPanelWm(wmImeControlTarget(), 'Ime Control Target')">
            IME Control Target
          </button>
          <div class="left-column">
            <p *ngIf="wmImeControlTargetTitle()" class="mat-body-1">
              <span class="mat-body-2">Title:</span>
              &ngsp;
              {{ wmImeControlTargetTitle() }}
            </p>
          </div>
        </div>
        <div *ngIf="wmImeInputTarget()" class="group ime-input-target">
          <button
          [color]="getButtonColor(wmImeInputTarget())"
            mat-button
            class="group-header"
            [class]="{selected: isHighlighted(wmImeInputTarget())}"
            (click)="onClickShowInPropertiesPanelWm(wmImeInputTarget(), 'Ime Input Target')">
            IME Input Target
          </button>
          <div class="left-column">
            <p *ngIf="wmImeInputTargetTitle()" class="mat-body-1">
              <span class="mat-body-2">Title:</span>
              &ngsp;
              {{ wmImeInputTargetTitle() }}
            </p>
          </div>
        </div>
        <div *ngIf="wmImeLayeringTarget()" class="group ime-layering-target">
          <button
          [color]="getButtonColor(wmImeLayeringTarget())"
            mat-button
            class="group-header"
            [class]="{selected: isHighlighted(wmImeLayeringTarget())}"
            (click)="onClickShowInPropertiesPanelWm(wmImeLayeringTarget(), 'Ime Layering Target')">
            IME Layering Target
          </button>
          <div class="left-column">
            <p *ngIf="wmImeLayeringTargetTitle()" class="mat-body-1">
              <span class="mat-body-2">Title:</span>
              &ngsp;
              {{ wmImeLayeringTargetTitle() }}
            </p>
          </div>
        </div>
      </ng-container>

      <ng-container *ngIf="!isImeManagerService">
        <!-- Ime Client or Ime Service -->
        <div class="group">
          <button
            *ngIf="wmHierarchyTree()"
            [color]="getButtonColor(wmHierarchyTree())"
            mat-button
            class="group-header wm-state-button"
            [class]="{selected: isHighlighted(wmHierarchyTree())}"
            (click)="onClickShowInPropertiesPanelWm(wmHierarchyTree(), 'Window Manager State')">
            WMState
          </button>
          <h3 *ngIf="!wmHierarchyTree()" class="group-header mat-subheading-2">WMState</h3>
          <div class="left-column wm-state">
            <p *ngIf="additionalProperties?.wm" class="mat-body-1">
              {{ wmRootLabel() }}
            </p>
            <p *ngIf="!additionalProperties?.wm" class="mat-body-1">
              There is no corresponding WMState entry.
            </p>
          </div>
        </div>
        <div class="group">
          <h3 class="group-header mat-subheading-2">SFLayer</h3>
          <div class="left-column sf-state">
            <p *ngIf="additionalProperties?.sf" class="mat-body-1">
              {{ sfRootLabel() }}
            </p>
            <p *ngIf="!additionalProperties?.sf" class="mat-body-1">
              There is no corresponding SFLayer entry.
            </p>
          </div>
        </div>
        <div *ngIf="additionalProperties?.wm" class="group focus">
          <h3 class="group-header mat-subheading-2">Focus</h3>
          <div class="left-column">
            <p class="mat-body-1">
              <span class="mat-body-2">Focused App:</span>
              &ngsp;
              {{ additionalProperties.wm.wmStateProperties.focusedApp }}
            </p>
            <p class="mat-body-1">
              <span class="mat-body-2">Focused Activity:</span>
              &ngsp;
              {{ additionalProperties.wm.wmStateProperties.focusedActivity }}
            </p>
            <p class="mat-body-1">
              <span class="mat-body-2">Focused Window:</span>
              &ngsp;
              {{ additionalProperties.wm.wmStateProperties.focusedWindow ?? 'null' }}
            </p>
            <p *ngIf="additionalProperties.sf" class="mat-body-1">
              <span class="mat-body-2">Focused Window Color:</span>
              &ngsp;
              {{ formattedWindowColor() }}
            </p>
            <p class="mat-body-2">Input Control Target Frame:</p>
            <coordinates-table [coordinates]="wmControlTargetFrame()"></coordinates-table>
          </div>
        </div>
        <div class="group visibility">
          <h3 class="group-header mat-subheading-2">Visibility</h3>
          <div class="left-column">
            <p *ngIf="additionalProperties?.wm" class="mat-body-1">
              <span class="mat-body-2">InputMethod Window:</span>
              &ngsp;
              {{ additionalProperties.wm.wmStateProperties.isInputMethodWindowVisible }}
            </p>
            <p *ngIf="additionalProperties?.sf" class="mat-body-1">
              <span class="mat-body-2">InputMethod Surface:</span>
              &ngsp;
              {{ additionalProperties.sf.properties.inputMethodSurface?.isVisible ?? false }}
            </p>
          </div>
        </div>
        <div *ngIf="additionalProperties?.sf" class="group ime-container">
          <button
          [color]="getButtonColor(additionalProperties.sf.properties.imeContainer)"
            mat-button
            class="group-header ime-container-button"
            [class]="{selected: isHighlighted(additionalProperties.sf.properties.imeContainer)}"
            (click)="
              onClickShowInPropertiesPanelSf(additionalProperties.sf.properties.imeContainer)
            ">
            Ime Container
          </button>
          <div class="left-column">
            <p class="mat-body-1">
              <span class="mat-body-2">ZOrderRelativeOfId:</span>
              &ngsp;
              {{ additionalProperties.sf.properties.imeContainer.zOrderRelativeOfId }}
            </p>
            <p class="mat-body-1">
              <span class="mat-body-2">Z:</span>
              &ngsp;
              {{ additionalProperties.sf.properties.imeContainer.z }}
            </p>
          </div>
        </div>
        <div *ngIf="additionalProperties?.sf" class="group input-method-surface">
          <button
          [color]="getButtonColor(additionalProperties.sf.properties.inputMethodSurface)"
            mat-button
            class="group-header input-method-surface-button"
            [class]="{
              selected: isHighlighted(additionalProperties.sf.properties.inputMethodSurface)
            }"
            (click)="
              onClickShowInPropertiesPanelSf(additionalProperties.sf.properties.inputMethodSurface)
            ">
            Input Method Surface
          </button>
          <div class="left-column">
            <p class="mat-body-2">Screen Bounds:</p>
            <coordinates-table [coordinates]="sfImeContainerScreenBounds()"></coordinates-table>
          </div>
          <div class="right-column">
            <p class="mat-body-2">Rect:</p>
            <coordinates-table [coordinates]="sfImeContainerRect()"></coordinates-table>
          </div>
        </div>
      </ng-container>
    </div>
  `,
  styles: [
    `
      :host collapsible-section-title {
        padding-bottom: 8px;
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
    selectedElementStyle,
    viewerCardInnerStyle,
  ],
})
export class ImeAdditionalPropertiesComponent {
  @Input() additionalProperties: ImeAdditionalProperties | undefined;
  @Input() isImeManagerService: boolean | undefined;
  @Input() highlightedItem: string = '';

  @Output() collapseButtonClicked = new EventEmitter();

  constructor(@Inject(ElementRef) private elementRef: ElementRef) {}

  isHighlighted(
    item:
      | TreeNode
      | ImeContainerProperties
      | InputMethodSurfaceProperties
      | undefined,
  ): boolean {
    return item ? item.id === this.highlightedItem : false;
  }

  getButtonColor(node: TreeNode | undefined) {
    return this.isHighlighted(node) ? undefined : 'primary';
  }

  formattedWindowColor(): string {
    const color = this.additionalProperties?.sf?.properties.focusedWindowColor;
    if (!color) return EMPTY_OBJ_STRING;
    return color.formattedValue();
  }

  sfRootLabel(): string {
    const rootProps = this.additionalProperties?.sf?.properties.root;
    if (!rootProps) {
      return this.additionalProperties?.sf?.name ?? 'root';
    }

    return rootProps.timestamp;
  }

  wmRootLabel(): string {
    const timestamp =
      this.additionalProperties?.wm?.wmStateProperties.timestamp;
    if (!timestamp) {
      return this.additionalProperties?.wm?.name ?? 'root';
    }
    return timestamp;
  }

  wmHierarchyTree(): HierarchyTreeNode | undefined {
    return this.additionalProperties?.wm?.hierarchyTree;
  }

  wmInsetsSourceProvider(): PropertyTreeNode | undefined {
    return this.additionalProperties?.wm?.wmStateProperties
      .imeInsetsSourceProvider;
  }

  wmControlTargetFrame(): PropertyTreeNode | undefined {
    return this.additionalProperties?.wm?.wmStateProperties.imeInsetsSourceProvider
      ?.getChildByName('insetsSourceProvider')
      ?.getChildByName('controlTarget')
      ?.getChildByName('windowFrames')
      ?.getChildByName('frame');
  }

  wmInsetsSourceProviderPosition(): string {
    return (
      this.additionalProperties?.wm?.wmStateProperties.imeInsetsSourceProvider
        ?.getChildByName('insetsSourceProvider')
        ?.getChildByName('control')
        ?.getChildByName('position')
        ?.formattedValue() ?? 'null'
    );
  }

  wmInsetsSourceProviderIsLeashReady(): string {
    return (
      this.additionalProperties?.wm?.wmStateProperties.imeInsetsSourceProvider
        ?.getChildByName('insetsSourceProvider')
        ?.getChildByName('isLeashReadyForDispatching')
        ?.formattedValue() ?? 'null'
    );
  }

  wmInsetsSourceProviderControllable(): string {
    return (
      this.additionalProperties?.wm?.wmStateProperties.imeInsetsSourceProvider
        ?.getChildByName('insetsSourceProvider')
        ?.getChildByName('controllable')
        ?.formattedValue() ?? 'null'
    );
  }

  wmInsetsSourceProviderSourceFrame(): PropertyTreeNode | undefined {
    return this.additionalProperties?.wm?.wmStateProperties.imeInsetsSourceProvider
      ?.getChildByName('source')
      ?.getChildByName('frame');
  }

  wmInsetsSourceProviderSourceVisible(): string {
    return (
      this.additionalProperties?.wm?.wmStateProperties.imeInsetsSourceProvider
        ?.getChildByName('source')
        ?.getChildByName('visible')
        ?.formattedValue() ?? 'null'
    );
  }

  wmInsetsSourceProviderSourceVisibleFrame(): PropertyTreeNode | undefined {
    return this.additionalProperties?.wm?.wmStateProperties.imeInsetsSourceProvider
      ?.getChildByName('source')
      ?.getChildByName('visibleFrame');
  }

  wmImeControlTarget(): PropertyTreeNode | undefined {
    return this.additionalProperties?.wm?.wmStateProperties.imeControlTarget;
  }

  wmImeControlTargetTitle(): string | undefined {
    return (
      this.additionalProperties?.wm?.wmStateProperties.imeControlTarget
        ?.getChildByName('windowContainer')
        ?.getChildByName('identifier')
        ?.getChildByName('title')
        ?.formattedValue() ?? undefined
    );
  }

  wmImeInputTarget(): PropertyTreeNode | undefined {
    return this.additionalProperties?.wm?.wmStateProperties.imeInputTarget;
  }

  wmImeInputTargetTitle(): string | undefined {
    return (
      this.additionalProperties?.wm?.wmStateProperties.imeInputTarget
        ?.getChildByName('windowContainer')
        ?.getChildByName('identifier')
        ?.getChildByName('title')
        ?.formattedValue() ?? undefined
    );
  }

  wmImeLayeringTarget(): PropertyTreeNode | undefined {
    return this.additionalProperties?.wm?.wmStateProperties.imeLayeringTarget;
  }

  wmImeLayeringTargetTitle(): string | undefined {
    return (
      this.additionalProperties?.wm?.wmStateProperties.imeLayeringTarget
        ?.getChildByName('windowContainer')
        ?.getChildByName('identifier')
        ?.getChildByName('title')
        ?.formattedValue() ?? undefined
    );
  }

  sfImeContainerScreenBounds(): PropertyTreeNode | undefined {
    return (
      this.additionalProperties?.sf?.properties.inputMethodSurface
        ?.screenBounds ?? undefined
    );
  }

  sfImeContainerRect(): PropertyTreeNode | undefined {
    return (
      this.additionalProperties?.sf?.properties.inputMethodSurface?.rect ??
      undefined
    );
  }

  isAllPropertiesUndefined(): boolean {
    if (this.isImeManagerService) {
      return !this.additionalProperties?.wm;
    } else {
      return !(this.additionalProperties?.wm || this.additionalProperties?.sf);
    }
  }

  onClickShowInPropertiesPanelWm(item: TreeNode, name: string) {
    this.updateAdditionalPropertySelected(item, name);
  }

  onClickShowInPropertiesPanelSf(
    item: ImeContainerProperties | InputMethodSurfaceProperties,
  ) {
    this.updateHighlightedItem(item.id);
  }

  private updateHighlightedItem(newId: string) {
    const event: CustomEvent = new CustomEvent(
      ViewerEvents.HighlightedIdChange,
      {
        bubbles: true,
        detail: {id: newId},
      },
    );
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  private updateAdditionalPropertySelected(item: TreeNode, name: string) {
    const itemWrapper = {
      name,
      treeNode: item,
    };
    const event: CustomEvent = new CustomEvent(
      ViewerEvents.AdditionalPropertySelected,
      {
        bubbles: true,
        detail: {selectedItem: itemWrapper},
      },
    );
    this.elementRef.nativeElement.dispatchEvent(event);
  }
}
