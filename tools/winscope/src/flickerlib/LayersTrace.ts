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
import LayerTraceEntry from './layers/LayerTraceEntry'
import { transformLayersTrace } from '../transform_sf'

LayersTrace.fromProto = function (proto): LayersTrace {
    const entries = []
    for (const entryProto of proto.entry) {
        const transformedEntry = LayerTraceEntry.fromProto({
            protos: entryProto.layers.layers,
            timestamp: entryProto.elapsedRealtimeNanos,
            hwcBlob: entryProto.hwcBlob,
            where: ``})

        entries.push(transformedEntry)
    }
    const source = null
    const sourceChecksum = null

    const original = transformLayersTrace(proto)
    const originalRects = original.children[3].rects
    let a = `Original rects\n`
    originalRects.forEach(it => a += `(${it.left}, ${it.top}) - (${it.right}, ${it.bottom}) - ${it.label}\n` )
    console.log(a)

    const trace = new LayersTrace(entries, source, sourceChecksum)
    const newRects = trace.entries[3].rects
    a = `New rects\n`
    newRects.forEach(it => a += `(${it.left}, ${it.top}) - (${it.right}, ${it.bottom}) - ${it.label}\n` )
    console.log(a)
    return trace
}

export default LayersTrace