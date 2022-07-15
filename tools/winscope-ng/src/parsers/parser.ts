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
import {ArrayUtils} from "../common/utils/array_utils";
import {TraceTypeId} from "common/trace/type_id";

abstract class Parser {
  protected constructor(trace: Blob) {
    this.trace = trace;
  }

  public async parse() {
    const traceBuffer = new Uint8Array(await this.trace.arrayBuffer());

    const magicNumber = this.getMagicNumber();
    if (magicNumber !== undefined)
    {
      const bufferContainsMagicNumber = ArrayUtils.equal(magicNumber, traceBuffer.slice(0, magicNumber.length));
      if (!bufferContainsMagicNumber) {
        throw TypeError("buffer doesn't contain expected magic number");
      }
    }

    this.decodedEntries = this.decodeTrace(traceBuffer);
    this.timestamps = this.decodedEntries.map((entry: any) => this.getTimestamp(entry));
  }

  public abstract getTraceTypeId(): TraceTypeId;

  public getTimestamps(): number[] {
    return this.timestamps;
  }

  public getTraceEntry(timestamp: number): undefined|any {
    const index = ArrayUtils.binarySearchLowerOrEqual(this.getTimestamps(), timestamp);
    if (index === undefined) {
      return undefined;
    }
    return this.processDecodedEntry(this.decodedEntries[index]);
  }

  public getTraceEntries(): any[] {
    throw new Error("Batch retrieval of trace entries not implemented for this parser!" +
                    " Note that the usage of this functionality is discouraged," +
                    " since creating all the trace entry objects may consume too much memory.");
  }

  protected abstract getMagicNumber(): undefined|number[];
  protected abstract decodeTrace(trace: Uint8Array): any[];
  protected abstract getTimestamp(decodedEntry: any): number;
  protected abstract processDecodedEntry(decodedEntry: any): any;

  protected trace: Blob;
  protected decodedEntries: any[] = [];
  protected timestamps: number[] = [];
}

export {Parser};
