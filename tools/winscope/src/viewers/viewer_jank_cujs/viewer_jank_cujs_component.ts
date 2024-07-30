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
import {Component, Input, ViewChild} from '@angular/core';
import {TraceType} from 'trace/trace_type';
import {LogComponent} from 'viewers/common/log_component';
import {viewerCardStyle} from 'viewers/components/styles/viewer_card.styles';
import {UiData} from './ui_data';

@Component({
  selector: 'viewer-jank-cujs',
  template: `
    <div class="card-grid">
       <log-view
        class="log-view"
        [selectedIndex]="inputData?.selectedIndex"
        [scrollToIndex]="inputData?.scrollToIndex"
        [currentIndex]="inputData?.currentIndex"
        [entries]="inputData?.entries"
        [headers]="inputData?.headers"
        [traceType]="${TraceType.CUJS}"
        [showTraceEntryTimes]="false"
        [showCurrentTimeButton]="false">
      </log-view>
    </div>
  `,
  styles: [viewerCardStyle],
})
export class ViewerJankCujsComponent {
  @Input() inputData: UiData | undefined;

  @ViewChild(LogComponent)
  logComponent?: LogComponent;
}
