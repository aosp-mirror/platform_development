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
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {TimestampUtils} from 'common/timestamp_utils';
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

export class NoCommonTimestampType extends UserWarning {
  getDescriptor(): string {
    return 'no common timestamp';
  }

  getMessage(): string {
    return 'Failed to load traces because no common timestamp type could be found';
  }
}

export class NoInputFiles extends UserWarning {
  getDescriptor(): string {
    return 'no input';
  }

  getMessage(): string {
    return `Input has no valid trace files`;
  }
}

export class TraceHasOldData extends UserWarning {
  constructor(
    private readonly descriptor: string,
    private readonly timeGap: TimeRange,
  ) {
    super();
  }

  getDescriptor(): string {
    return 'old trace';
  }

  getMessage(): string {
    const elapsedTime = NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(
      this.timeGap.to.getValueNs() - this.timeGap.from.getValueNs(),
    );
    return `${
      this.descriptor
    }: discarded because data is older than ${TimestampUtils.format(
      elapsedTime,
      true,
    )}`;
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
