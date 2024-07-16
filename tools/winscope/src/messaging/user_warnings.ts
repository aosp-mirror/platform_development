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
  getDescriptor(): string {
    return 'no valid files';
  }

  getMessage(): string {
    return `No valid trace files found`;
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

export class InvalidPerfettoTrace extends UserWarning {
  constructor(
    private readonly descriptor: string,
    private readonly parserErrorMessages: string[],
  ) {
    super();
  }

  getDescriptor(): string {
    return 'invalid perfetto trace';
  }

  getMessage(): string {
    return `${this.descriptor}: ${this.parserErrorMessages.join(', ')}`;
  }
}

export class CannotVisualizeAllTraces extends UserWarning {
  constructor(private readonly errorMessage: string) {
    super();
  }

  getDescriptor(): string {
    return 'cannot visualize all traces';
  }

  getMessage(): string {
    return `Cannot visualize all traces: ${this.errorMessage}.\nTry removing some traces.`;
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
