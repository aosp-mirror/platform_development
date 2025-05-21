/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {VIRTUAL_SCROLL_STRATEGY} from '@angular/cdk/scrolling';
import {Directive, forwardRef, Input} from '@angular/core';
import {TraceType} from 'trace/trace_type';
import {VariableHeightScrollStrategy} from './variable_height_scroll_strategy';

@Directive({
  selector: '[variableHeightScroll]',
  providers: [
    {
      provide: VIRTUAL_SCROLL_STRATEGY,
      useFactory: (dir: VariableHeightScrollDirective) => dir.scrollStrategy,
      deps: [forwardRef(() => VariableHeightScrollDirective)],
    },
  ],
})
export class VariableHeightScrollDirective {
  readonly scrollStrategy = new VariableHeightScrollStrategy();

  @Input() traceType: TraceType | undefined;

  @Input() scrollItems: object[] = [];

  ngOnChanges() {
    this.scrollStrategy.updateItems(this.scrollItems);
    if (this.traceType !== undefined) {
      this.scrollStrategy.updateTraceType(this.traceType);
    }
  }
}
