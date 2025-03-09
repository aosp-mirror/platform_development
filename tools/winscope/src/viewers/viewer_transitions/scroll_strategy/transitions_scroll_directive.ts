/*
 * Copyright (C) 2024, The Android Open Source Project
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
import {Directive, forwardRef} from '@angular/core';
import {VariableHeightScrollDirective} from 'viewers/common/variable_height_scroll_directive';
import {TransitionsEntry} from 'viewers/viewer_transitions/ui_data';
import {TransitionsScrollStrategy} from './transitions_scroll_strategy';

@Directive({
  selector: '[transitionsVirtualScroll]',
  providers: [
    {
      provide: VIRTUAL_SCROLL_STRATEGY,
      useFactory: (dir: TransitionsScrollDirective) => dir.scrollStrategy,
      deps: [forwardRef(() => TransitionsScrollDirective)],
    },
  ],
})
export class TransitionsScrollDirective extends VariableHeightScrollDirective<TransitionsEntry> {
  scrollStrategy = new TransitionsScrollStrategy();
}
