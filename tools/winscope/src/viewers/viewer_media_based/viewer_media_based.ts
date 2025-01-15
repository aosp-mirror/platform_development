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

import {assertDefined} from 'common/assert_utils';
import {Store} from 'common/store/store';
import {MediaBasedTraceEntry} from 'trace/media_based_trace_entry';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {AbstractViewer} from 'viewers/abstract_viewer';
import {ViewType} from 'viewers/viewer';
import {Presenter} from './presenter';
import {UiData} from './ui_data';
import {ViewerMediaBasedComponent} from './viewer_media_based_component';

export abstract class ViewerMediaBased extends AbstractViewer<MediaBasedTraceEntry> {
  private traces: Array<Trace<MediaBasedTraceEntry>> | undefined;

  constructor(traces: Traces, store: Store) {
    super(undefined, traces, 'viewer-media-based', store);
  }

  override getTraces(): Array<Trace<MediaBasedTraceEntry>> {
    return assertDefined(this.traces);
  }

  protected override initializePresenter(
    trace: undefined,
    traces: Traces,
  ): Presenter {
    const type = this.getTraceTypeForViewTitle();
    this.traces = traces.getTraces(type) as Array<Trace<MediaBasedTraceEntry>>;
    const component = this.htmlElement as unknown as ViewerMediaBasedComponent;
    if (type === TraceType.SCREEN_RECORDING) {
      component.enableDoubleClick = true;
    }
    const notifyViewCallback = (uiData: UiData) => {
      component.titles = uiData.titles;
      component.currentTraceEntries = uiData.currentTraceEntries;
      component.forceMinimize = uiData.forceMinimize;
    };
    return new Presenter(this.traces, notifyViewCallback);
  }

  protected override getViewType(): ViewType {
    return ViewType.OVERLAY;
  }
}
