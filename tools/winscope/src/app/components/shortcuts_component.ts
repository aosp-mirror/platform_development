/*
 * Copyright (C) 2024 The Android Open Source Project
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
import {Component, Inject} from '@angular/core';
import {MatIconRegistry} from '@angular/material/icon';
import {DomSanitizer} from '@angular/platform-browser';
import {overlayPanelStyles} from 'app/styles/overlay_panel.styles';
import {getRootUrl} from 'common/url_utils';

@Component({
  selector: 'shortcuts-panel',
  template: `
    <h2 class="dialog-title" mat-dialog-title>
      <span> ESSENTIAL SHORTCUTS </span>
      <button mat-dialog-close class="close-button" mat-icon-button>
        <mat-icon> close </mat-icon>
      </button>
    </h2>
    <mat-dialog-content>
      <div class="mat-title"> Timeline </div>
      <div class="grouped-shortcuts">
        <div class="key-shortcut even-width mat-body-1">
          <div class="key"> W </div>
          <span class="action"> Zoom in </span>
        </div>
        <div class="key-shortcut even-width mat-body-1">
          <div class="key"> S </div>
          <span class="action"> Zoom out </span>
        </div>
        <div class="key-shortcut even-width mat-body-1">
          <div class="key"> A </div>
          <span class="action"> Move slider left </span>
        </div>
        <div class="key-shortcut even-width mat-body-1">
          <div class="key"> D </div>
          <span class="action"> Move slider right </span>
        </div>
        <div class="pointer-shortcut mat-body-1">
          <mat-icon class="trackpad-icon" svgIcon="trackpad_right_click"></mat-icon>
          <span class="action">
            <span class="italic-text"> Right click </span>
            <span> Open context menu for bookmarks </span>
          </span>
        </div>
        <div class="pointer-shortcut mat-body-1">
          <mat-icon class="trackpad-icon enlarge" svgIcon="trackpad_vertical_scroll"></mat-icon>
          <span class="action">
            <span class="italic-text"> Vertical Scroll </span>
            <span> Zoom in/out </span>
          </span>
        </div>
        <div class="pointer-shortcut mat-body-1">
          <mat-icon class="trackpad-icon tall enlarge" svgIcon="trackpad_horizontal_scroll"></mat-icon>
          <span class="action">
            <span class="italic-text"> Horizontal Scroll </span>
            <span> Move slider left/right </span>
          </span>
        </div>
      </div>

      <div class="shortcuts-row">
        <div class="shortcuts-row-section">
          <div class="mat-title"> 3D View </div>
          <div class="grouped-shortcuts">
            <div class="pointer-shortcut mat-body-1">
              <mat-icon class="trackpad-icon enlarge" svgIcon="trackpad_vertical_scroll"></mat-icon>
              <span class="action">
                <span class="italic-text"> Vertical Scroll </span>
                <span> Zoom in/out </span>
              </span>
            </div>
          </div>
        </div>

        <div class="shortcuts-row-section">
          <div class="mat-title"> Global </div>
          <div class="grouped-shortcuts">
            <div class="key-shortcut mat-body-1">
              <div class="key"> <mat-icon class="material-symbols-outlined"> arrow_left_alt </mat-icon> </div>
              <span class="action"> Previous state </span>
            </div>
            <div class="key-shortcut mat-body-1">
              <div class="key"> <mat-icon class="material-symbols-outlined"> arrow_right_alt </mat-icon> </div>
              <span class="action"> Next state </span>
            </div>
          </div>
        </div>
      </div>
    </mat-dialog-content>
  `,
  styles: [
    `
      .dialog-title {
        display: flex;
        justify-content: space-between;
      }
      .shortcuts-row {
        display: flex;
        flex-direction: row;
        width: 80%;
        justify-content: space-between;
      }
      .grouped-shortcuts {
        display: flex;
        flex-wrap: wrap;
        flex-direction: row;
        justify-content: space-between;
      }
      .key-shortcut, .pointer-shortcut {
        display: flex;
        flex-direction: row;
        padding: 12px 0px;
      }
      .key-shortcut {
        align-items: center;
      }
      .key-shortcut.even-width {
        min-width: 202px;
      }
      .pointer-shortcut:not(:has(.tall)) {
        align-items: center;
      }
      .pointer-shortcut:has(.tall) {
        align-items: end;
      }
      .key, .trackpad-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0px 4px;
      }
      .key {
        border-radius: 8px;
        width: 35px;
        height: 35px;
        background-color: var(--icon-accent-color);
        border: 1px solid #7e7e7e;
        font-size: 18px;
      }
      .trackpad-icon {
        width: 40px;
        height: 40px;
      }
      .enlarge {
        height: 55px;
        width: 55px;
      }
      .action {
        padding: 12px;
        display: flex;
        flex-direction: column;
      }
      .italic-text {
        font-style: italic;
      }
    `,
    overlayPanelStyles,
  ],
})
export class ShortcutsComponent {
  constructor(
    @Inject(MatIconRegistry) private matIconRegistry: MatIconRegistry,
    @Inject(DomSanitizer) private domSanitizer: DomSanitizer,
  ) {
    this.matIconRegistry.addSvgIcon(
      'trackpad_right_click',
      this.domSanitizer.bypassSecurityTrustResourceUrl(
        getRootUrl() + 'trackpad_right_click.svg',
      ),
    );
    this.matIconRegistry.addSvgIcon(
      'trackpad_vertical_scroll',
      this.domSanitizer.bypassSecurityTrustResourceUrl(
        getRootUrl() + 'trackpad_vertical_scroll.svg',
      ),
    );
    this.matIconRegistry.addSvgIcon(
      'trackpad_horizontal_scroll',
      this.domSanitizer.bypassSecurityTrustResourceUrl(
        getRootUrl() + 'trackpad_horizontal_scroll.svg',
      ),
    );
  }
}
