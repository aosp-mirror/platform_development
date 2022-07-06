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
import {ArrayUtils} from '../common/utils/array_utils';
import {TraceTypeId} from "common/trace/type_id";

abstract class Parser {
  constructor(buffer: Uint8Array) {
    const magicNumber = this.getMagicNumber();
    const bufferContainsMagicNumber = ArrayUtils.equal(magicNumber, buffer.slice(0, magicNumber.length));
    if (!bufferContainsMagicNumber) {
      throw TypeError("buffer doesn't contain expected magic number");
    }

    this.traceEntriesProto = this.decodeProto(buffer);
    this.timestamps = this.traceEntriesProto.map((entryProto: any) => this.getTimestamp(entryProto));
  }

  public abstract getTraceTypeId(): TraceTypeId;

  public getTimestamps(): number[] {
    return this.timestamps;
  }

  public getTraceEntry(timestamp: number): any|undefined {
    const index = ArrayUtils.binarySearchLowerOrEqual(this.getTimestamps(), timestamp);
    if (index === undefined) {
      return undefined;
    }
    return this.processTraceEntryProto(this.traceEntriesProto[index]);
  }

  protected abstract getMagicNumber(): number[];
  protected abstract decodeProto(buffer: Uint8Array): any[];
  protected abstract getTimestamp(entryProto: any): number;
  protected abstract processTraceEntryProto(entryProto: any): any;

  private traceEntriesProto: any[];
  private timestamps: number[];
}

export {Parser};
