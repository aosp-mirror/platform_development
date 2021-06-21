import { detectAndDecode, decodeAndTransformProto, FILE_TYPES } from '../src/decode';
import fs from 'fs';
import path from 'path';

const layers_traces = [
  require('./traces/layers_trace/layers_trace_emptyregion.pb'),
  require('./traces/layers_trace/layers_trace_invalid_layer_visibility.pb'),
  require('./traces/layers_trace/layers_trace_orphanlayers.pb'),
  require('./traces/layers_trace/layers_trace_root.pb'),
  require('./traces/layers_trace/layers_trace_root_aosp.pb'),
];

describe("Proto Transformations", () => {
  it("can transform surface flinger traces", () => {
    for (const trace of layers_traces) {
      fs.readFileSync(path.resolve(__dirname, trace));
      const traceBuffer = fs.readFileSync(path.resolve(__dirname, trace));

      const buffer = new Uint8Array(traceBuffer);
      const data = decodeAndTransformProto(buffer, FILE_TYPES.layers_trace, true);

      expect(true).toBe(true);
    }
  });
});