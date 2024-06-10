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

import {Component, ElementRef, Inject, Input} from '@angular/core';
import {FunctionUtils} from 'common/function_utils';
import {TRACE_INFO} from 'trace/trace_info';
import {TraceType} from 'trace/trace_type';
import {UserOption, UserOptions} from 'viewers/common/user_options';
import {userOptionStyle} from './styles/user_option.styles';

type LogCallback = (key: string, state: boolean, name: string) => void;

@Component({
  selector: 'user-options',
  template: `
      <button
        *ngFor="let option of objectKeys(userOptions)"
        mat-flat-button
        [color]="getUserOptionButtonColor(userOptions[option])"
        [disabled]="userOptions[option].isUnavailable"
        [class.not-enabled]="!userOptions[option].enabled"
        class="user-option"
        [style.cursor]="'pointer'"
        (click)="onUserOptionChange(userOptions[option])">
        <span class="user-option-label" [class.with-chip]="!!userOptions[option].chip">
          <span> {{userOptions[option].name}} </span>
          <div *ngIf="userOptions[option].chip" class="user-option-chip"> {{userOptions[option].chip.short}} </div>
          <mat-icon  class="material-symbols-outlined" *ngIf="userOptions[option].icon"> {{userOptions[option].icon}} </mat-icon>
        </span>
      </button>
    `,
  styles: [userOptionStyle],
})
export class UserOptionsComponent {
  objectKeys = Object.keys;

  @Input() userOptions: UserOptions = {};
  @Input() eventType = '';
  @Input() traceType: TraceType | undefined;
  @Input() logCallback: LogCallback = FunctionUtils.DO_NOTHING;

  constructor(@Inject(ElementRef) private elementRef: ElementRef) {}

  getUserOptionButtonColor(option: UserOption) {
    return option.enabled ? 'primary' : undefined;
  }

  onUserOptionChange(option: UserOption) {
    option.enabled = !option.enabled;
    this.logCallback(
      option.name,
      option.enabled,
      this.traceType ? TRACE_INFO[this.traceType].name : 'unknown',
    );
    const event = new CustomEvent(this.eventType, {
      bubbles: true,
      detail: {userOptions: this.userOptions},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }
}
