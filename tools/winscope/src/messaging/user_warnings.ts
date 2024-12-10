/*
 * Copyright (C) 2023 The Android Open Source Project
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

import {TimeRange} from 'common/time';
import {TimeDuration} from 'common/time_duration';
import {TRACE_INFO} from 'trace/trace_info';
import {TraceType} from 'trace/trace_type';
import {UserWarning} from './user_warning';

export class CorruptedArchive extends UserWarning {
  constructor(private readonly file: File) {
    super();
  }

  getDescriptor(): string {
    return 'corrupted archive';
  }

  getMessage(): string {
    return `${this.file.name}: corrupted archive`;
  }
}

export class NoValidFiles extends UserWarning {
  constructor(private traces?: string[]) {
    super();
  }
  getDescriptor(): string {
    return 'no valid files';
  }

  getMessage(): string {
    return (
      'No valid trace files found' +
      (this.traces ? ` for ${this.traces.join(', ')}` : '')
    );
  }
}

export class TraceHasOldData extends UserWarning {
  constructor(
    private readonly descriptor: string,
    private readonly timeGap?: TimeRange,
  ) {
    super();
  }

  getDescriptor(): string {
    return 'old trace';
  }

  getMessage(): string {
    const elapsedTime = this.timeGap
      ? new TimeDuration(
          this.timeGap.to.getValueNs() - this.timeGap.from.getValueNs(),
        )
      : undefined;
    return (
      `${this.descriptor}: discarded because data is old` +
      (this.timeGap ? `er than ${elapsedTime?.format()}` : '')
    );
  }
}

export class TraceOverridden extends UserWarning {
  constructor(
    private readonly descriptor: string,
    private readonly overridingType?: TraceType,
  ) {
    super();
  }

  getDescriptor(): string {
    return 'trace overridden';
  }

  getMessage(): string {
    if (this.overridingType !== undefined) {
      return `${this.descriptor}: overridden by another trace of type ${
        TraceType[this.overridingType]
      }`;
    }
    return `${this.descriptor}: overridden by another trace of same type`;
  }
}

export class UnsupportedFileFormat extends UserWarning {
  constructor(private readonly descriptor: string) {
    super();
  }

  getDescriptor(): string {
    return 'unsupported format';
  }

  getMessage(): string {
    return `${this.descriptor}: unsupported format`;
  }
}

export class InvalidLegacyTrace extends UserWarning {
  constructor(
    private readonly descriptor: string,
    private readonly errorMessage: string,
  ) {
    super();
  }

  getDescriptor(): string {
    return 'invalid legacy trace';
  }

  getMessage(): string {
    return `${this.descriptor}: ${this.errorMessage}`;
  }
}

export class InvalidPerfettoTrace extends UserWarning {
  constructor(
    private readonly descriptor: string,
    private readonly errorMessages: string[],
  ) {
    super();
  }

  getDescriptor(): string {
    return 'invalid perfetto trace';
  }

  getMessage(): string {
    return `${this.descriptor}: ${this.errorMessages.join(', ')}`;
  }
}

export class FailedToCreateTracesParser extends UserWarning {
  constructor(
    private readonly traceType: TraceType,
    private readonly errorMessage: string,
  ) {
    super();
  }

  getDescriptor(): string {
    return 'failed to create traces parser';
  }

  getMessage(): string {
    return `Failed to create ${TRACE_INFO[this.traceType].name} parser: ${
      this.errorMessage
    }`;
  }
}

export class CannotVisualizeTraceEntry extends UserWarning {
  constructor(private readonly errorMessage: string) {
    super();
  }

  getDescriptor(): string {
    return 'cannot visualize trace entry';
  }

  getMessage(): string {
    return this.errorMessage;
  }
}

export class FailedToInitializeTimelineData extends UserWarning {
  getDescriptor(): string {
    return 'failed to initialize timeline data';
  }

  getMessage(): string {
    return 'Cannot visualize all traces: Failed to initialize timeline data.\nTry removing some traces.';
  }
}

export class IncompleteFrameMapping extends UserWarning {
  constructor(private readonly errorMessage: string) {
    super();
  }

  getDescriptor(): string {
    return 'incomplete frame mapping';
  }

  getMessage(): string {
    return `Error occurred in frame mapping: ${this.errorMessage}`;
  }
}

export class NoTraceTargetsSelected extends UserWarning {
  getDescriptor(): string {
    return 'No trace targets selected';
  }

  getMessage(): string {
    return 'No trace targets selected.';
  }
}

export class MissingVsyncId extends UserWarning {
  constructor(private readonly tableName: string) {
    super();
  }

  getDescriptor(): string {
    return 'missing vsync id';
  }

  getMessage(): string {
    return `missing vsync_id value for one or more entries in ${this.tableName}`;
  }
}

export class ProxyTraceTimeout extends UserWarning {
  getDescriptor(): string {
    return 'proxy trace timeout';
  }

  getMessage(): string {
    return 'Errors occurred during tracing: trace timed out';
  }
}

export class ProxyTracingWarnings extends UserWarning {
  constructor(private readonly warnings: string[]) {
    super();
  }

  getDescriptor(): string {
    return 'proxy tracing warnings';
  }

  getMessage(): string {
    return `Trace collection warning: ${this.warnings.join(', ')}`;
  }
}

export class ProxyTracingErrors extends UserWarning {
  constructor(private readonly errorMessages: string[]) {
    super();
  }

  getDescriptor(): string {
    return 'proxy tracing errors';
  }

  getMessage(): string {
    return `Trace collection errors: ${this.errorMessages.join(', ')}`;
  }
}

export class MissingLayerIds extends UserWarning {
  getDescriptor(): string {
    return 'missing layer ids';
  }

  getMessage(): string {
    return 'Cannot parse some layers due to null or undefined layer id';
  }
}

export class DuplicateLayerIds extends UserWarning {
  constructor(private readonly layerIds: number[]) {
    super();
  }

  getDescriptor(): string {
    return 'duplicate layer id';
  }

  getMessage(): string {
    const optionalPlural = this.layerIds.length > 1 ? 's' : '';
    const layerIds = this.layerIds.join(', ');
    return `Duplicate SF layer id${optionalPlural} ${layerIds} found - adding as "Duplicate" to the hierarchy`;
  }
}

export class MonotonicScreenRecording extends UserWarning {
  getDescriptor(): string {
    return 'monotonic screen recording';
  }

  getMessage(): string {
    return `Screen recording may not be synchronized with the
      other traces. Metadata contains monotonic time instead of elapsed.`;
  }
}

export class CannotParseAllTransitions extends UserWarning {
  getDescriptor(): string {
    return 'cannot parse all transitions';
  }

  getMessage(): string {
    return 'Cannot parse all transitions. Some may be missing in Transitions viewer.';
  }
}

export class TraceSearchQueryFailed extends UserWarning {
  constructor(private readonly errorMessage: string) {
    super();
  }

  getDescriptor(): string {
    return 'trace search query failed';
  }

  getMessage(): string {
    return `Search query failed: ${this.errorMessage}`;
  }
}
