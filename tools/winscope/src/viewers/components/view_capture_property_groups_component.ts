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
import {Component, Input} from '@angular/core';
import {VcCuratedProperties} from 'viewers/common/curated_properties';

@Component({
  selector: 'view-capture-property-groups',
  template: `
    <div *ngIf="properties" class="group view">
      <h3 class="group-header mat-subheading-2">View</h3>
      <div class="left-column class-name">
        <p class="mat-body-2">
          <span class="mat-body-1">Class: </span>
          &ngsp;
          {{ properties.className }}
        </p>
        <p class="mat-body-2 hashcode">
          <span class="mat-body-1">Hashcode: </span>
          &ngsp;
          {{ properties.hashcode }}
        </p>
      </div>
    </div>
    <mat-divider></mat-divider>
    <div *ngIf="properties" class="group geometry">
      <h3 class="group-header mat-subheading-2">Geometry</h3>
      <div class="left-column coordinates">
        <p class="column-header mat-small">Coordinates</p>
        <p class="mat-body-2 left">
          <span class="mat-body-1">Left: </span>
          &ngsp;
          {{ properties.left }}
        </p>
        <p class="mat-body-2 top">
          <span class="mat-body-1">Top: </span>
          &ngsp;
          {{ properties.top }}
        </p>
        <p class="mat-body-2 elevation">
          <span class="mat-body-1">Elevation: </span>
          &ngsp;
          {{ properties.elevation }}
        </p>
      </div>
      <div class="right-column size">
        <p class="column-header mat-small">Size</p>
        <p class="mat-body-2 height">
          <span class="mat-body-1">Height: </span>
          &ngsp;
          {{ properties.height }}
        </p>
        <p class="mat-body-2 width">
          <span class="mat-body-1">Width: </span>
          &ngsp;
          {{ properties.width }}
        </p>
      </div>
    </div>
    <div *ngIf="properties" class="group geometry">
      <h3 class="group-header mat-subheading-2"></h3>
      <div class="left-column translation">
        <p class="column-header mat-small">Translation</p>
        <p class="mat-body-2 translationx">
          <span class="mat-body-1">Translation X: </span>
          &ngsp;
          {{ properties.translationX }}
        </p>
        <p class="mat-body-2 translationy">
          <span class="mat-body-1">Translation Y: </span>
          &ngsp;
          {{ properties.translationY }}
        </p>
      </div>
      <div class="right-column scroll">
        <p class="column-header mat-small">Scroll</p>
        <p class="mat-body-2 scrollx">
          <span class="mat-body-1">Scroll X: </span>
          &ngsp;
          {{ properties.scrollX }}
        </p>
        <p class="mat-body-2 scrolly">
          <span class="mat-body-1">Scroll Y: </span>
          &ngsp;
          {{ properties.scrollY }}
        </p>
      </div>
    </div>
    <div *ngIf="properties" class="group geometry">
      <h3 class="group-header mat-subheading-2"></h3>
      <div class="left-column scale">
        <p class="column-header mat-small">Scale</p>
        <p class="mat-body-2 scalex">
          <span class="mat-body-1">Scale X: </span>
          &ngsp;
          {{ properties.scaleX }}
        </p>
        <p class="mat-body-2 scaley">
          <span class="mat-body-1">Scale Y: </span>
          &ngsp;
          {{ properties.scaleY }}
        </p>
      </div>
    </div>
    <mat-divider></mat-divider>
    <div *ngIf="properties" class="group effects">
      <h3 class="group-header mat-subheading-2">Effects</h3>
      <div class="left-column translation">
        <p class="column-header mat-small">Translation</p>
        <p class="mat-body-2 visibility">
          <span class="mat-body-1">Visibility: </span>
          &ngsp;
          {{ properties.visibility }}
        </p>
        <p class="mat-body-2 alpha">
          <span class="mat-body-1">Alpha: </span>
          &ngsp;
          {{ properties.alpha }}
        </p>
        <p class="mat-body-2 will-not-draw">
          <span class="mat-body-1">Will Not Draw: </span>
          &ngsp;
          {{ properties.willNotDraw }}
        </p>
      </div>
      <div class="right-column misc">
        <p class="column-header mat-small">Miscellaneous</p>
        <p class="mat-body-2 clip-children">
          <span class="mat-body-1">Clip Children: </span>
          &ngsp;
          {{ properties.clipChildren }}
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
  @Input() properties: VcCuratedProperties | undefined;
}
