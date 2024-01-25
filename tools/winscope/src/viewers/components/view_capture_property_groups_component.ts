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
import {Component, Input} from '@angular/core';
import {ViewNode} from 'trace/trace_type';

@Component({
  selector: 'view-capture-property-groups',
  template: `
    <div class="group">
      <h3 class="group-header mat-subheading-2">View</h3>
      <div class="left-column">
        <p class="mat-body-1 flags">
          <span class="mat-body-2">Class: </span>
          &ngsp;
          {{ item.className }}
        </p>
        <p class="mat-body-1 flags">
          <span class="mat-body-2">Hashcode: </span>
          &ngsp;
          {{ item.hashcode }}
        </p>
      </div>
    </div>
    <mat-divider></mat-divider>
    <div class="group">
      <h3 class="group-header mat-subheading-2">Geometry</h3>
      <div class="left-column">
        <p class="column-header mat-small">Coordinates</p>
        <p class="mat-body-1">
          <span class="mat-body-2">Left: </span>
          &ngsp;
          {{ item.left }}
        </p>
        <p class="mat-body-1">
          <span class="mat-body-2">Top: </span>
          &ngsp;
          {{ item.top }}
        </p>
        <p class="mat-body-1">
          <span class="mat-body-2">Elevation: </span>
          &ngsp;
          {{ item.elevation }}
        </p>
      </div>
      <div class="right-column">
        <p class="column-header mat-small">Size</p>
        <p class="mat-body-1">
          <span class="mat-body-2">Height: </span>
          &ngsp;
          {{ item.height }}
        </p>
        <p class="mat-body-1">
          <span class="mat-body-2">Width: </span>
          &ngsp;
          {{ item.width }}
        </p>
      </div>
    </div>
    <div class="group">
      <h3 class="group-header mat-subheading-2"></h3>
      <div class="left-column">
        <p class="column-header mat-small">Translation</p>
        <p class="mat-body-1">
          <span class="mat-body-2">Translation X: </span>
          &ngsp;
          {{ item.translationX }}
        </p>
        <p class="mat-body-1">
          <span class="mat-body-2">Translation Y: </span>
          &ngsp;
          {{ item.translationY }}
        </p>
      </div>
      <div class="right-column">
        <p class="column-header mat-small">Scroll</p>
        <p class="mat-body-1">
          <span class="mat-body-2">Scroll X: </span>
          &ngsp;
          {{ item.scrollX }}
        </p>
        <p class="mat-body-1">
          <span class="mat-body-2">Scroll Y: </span>
          &ngsp;
          {{ item.scrollY }}
        </p>
      </div>
    </div>
    <div class="group">
      <h3 class="group-header mat-subheading-2"></h3>
      <div class="left-column">
        <p class="column-header mat-small">Scale</p>
        <p class="mat-body-1">
          <span class="mat-body-2">Scale X: </span>
          &ngsp;
          {{ item.scaleX }}
        </p>
        <p class="mat-body-1">
          <span class="mat-body-2">Scale Y: </span>
          &ngsp;
          {{ item.scaleY }}
        </p>
      </div>
    </div>
    <mat-divider></mat-divider>
    <div class="group">
      <h3 class="group-header mat-subheading-2">Effects</h3>
      <div class="left-column">
        <p class="column-header mat-small">Translation</p>
        <p class="mat-body-1">
          <span class="mat-body-2">Visibility: </span>
          &ngsp;
          {{ item.visibility }}
        </p>
        <p class="mat-body-1">
          <span class="mat-body-2">Alpha: </span>
          &ngsp;
          {{ item.alpha }}
        </p>
        <p class="mat-body-1">
          <span class="mat-body-2">Will Not Draw: </span>
          &ngsp;
          {{ item.willNotDraw }}
        </p>
      </div>
      <div class="right-column">
        <p class="column-header mat-small">Miscellaneous</p>
        <p class="mat-body-1">
          <span class="mat-body-2">Clip Children: </span>
          &ngsp;
          {{ item.clipChildren }}
        </p>
      </div>
    </div>
  `,
  styles: [
    `
      .group {
        display: flex;
        flex-direction: row;
        padding: 8px;
      }

      .group-header {
        width: 80px;
        color: gray;
      }

      .left-column {
        flex: 1;
        padding: 0 5px;
      }

      .right-column {
        flex: 1;
        border: 1px solid var(--border-color);
        border-left-width: 5px;
        padding: 0 5px;
      }

      .column-header {
        color: gray;
      }
    `,
  ],
})
export class ViewCapturePropertyGroupsComponent {
  @Input() item: ViewNode;
}
