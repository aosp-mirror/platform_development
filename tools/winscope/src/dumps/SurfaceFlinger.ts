/*
 * Copyright 2020, The Android Open Source Project
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

import { FILE_TYPES, DUMP_TYPES } from "@/decode.js";
import DumpBase from "./DumpBase";
import LayersTraceEntry from '../flickerlib/layers/LayerTraceEntry';
import LayersTrace from '../flickerlib/LayersTrace';

export default class SurfaceFlinger extends DumpBase {
  sfDumpFile: any;
  data: any;

  constructor(files) {
    const sfDumpFile = files[FILE_TYPES.SURFACE_FLINGER_DUMP];
    super(sfDumpFile.data, files);
    this.sfDumpFile = sfDumpFile;
  }

  get type() {
    return DUMP_TYPES.SURFACE_FLINGER;
  }

  static fromProto(proto: any): LayersTrace {
    const source = null;
    const entry =  LayersTraceEntry.fromProto(
      /* protos */ proto.layers,
      /* displays */ proto.displays,
      /* timestamp */ 0,
      /* hwcBlob */ ""
    );
    return new LayersTrace([entry], source);
  }
}