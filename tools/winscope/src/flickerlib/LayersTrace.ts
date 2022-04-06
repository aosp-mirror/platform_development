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

import { LayersTrace } from "./common"
import LayerTraceEntryLazy from './layers/LayerTraceEntryLazy'

LayersTrace.fromProto = function (proto: any): LayersTrace {
    const entries = []
    for (const entryProto of proto.entry) {
        const transformedEntry = new LayerTraceEntryLazy(
            /* protos */ entryProto.layers.layers,
            /* displays */ entryProto.displays,
            /* timestamp */ entryProto.elapsedRealtimeNanos,
            /* hwcBlob */ entryProto.hwcBlob);

        entries.push(transformedEntry);
    }
    const source = null;
    const trace = new LayersTrace(entries, source);
    return trace;
}

export default LayersTrace;
