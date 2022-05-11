import { decodeAndTransformProto, FILE_TYPES, FILE_DECODERS } from '../src/decode';
import Tag from '../src/flickerlib/tags/Tag';
import Error from '../src/flickerlib/errors/Error';
import { TaggingEngine } from '../src/flickerlib/common.js';
import fs from 'fs';
import path from 'path';

const tagTrace = '../spec/traces/tag_trace.winscope';
const errorTrace = '../spec/traces/error_trace.winscope';

describe("Tag Transformation", () => {
  it("can transform tag traces", () => {
    const buffer = new Uint8Array(fs.readFileSync(path.resolve(__dirname, tagTrace)));

    const data = decodeAndTransformProto(buffer, FILE_DECODERS[FILE_TYPES.TAG_TRACE].decoderParams, true);

    expect(data.entries[0].timestamp.toString()).toEqual('159979677861');
    expect(data.entries[1].timestamp.toString()).toEqual('161268519083');
    expect(data.entries[2].timestamp.toString()).toEqual('161126718496');
    expect(data.entries[3].timestamp.toString()).toEqual('161613497398');
    expect(data.entries[4].timestamp.toString()).toEqual('161227062777');
    expect(data.entries[5].timestamp.toString()).toEqual('161268519083');
    expect(data.entries[6].timestamp.toString()).toEqual('161825945076');
    expect(data.entries[7].timestamp.toString()).toEqual('162261072567');

    expect(data.entries[0].tags).toEqual([new Tag(12345,"PIP_ENTER",true,1,"",0)]);
    expect(data.entries[1].tags).toEqual([new Tag(12345,"PIP_ENTER",false,2,"",2)]);
    expect(data.entries[2].tags).toEqual([new Tag(67890,"ROTATION",true,3,"",3)]);
    expect(data.entries[3].tags).toEqual([new Tag(67890,"ROTATION",false,4,"",4)]);
    expect(data.entries[4].tags).toEqual([new Tag(9876,"PIP_EXIT",true,5,"",5)]);
    expect(data.entries[5].tags).toEqual([new Tag(9876,"PIP_EXIT",false,6,"",6)]);
    expect(data.entries[6].tags).toEqual([new Tag(54321,"IME_APPEAR",true,7,"",7)]);
    expect(data.entries[7].tags).toEqual([new Tag(54321,"IME_APPEAR",false,8,"",8)]);
  })
});

describe("Detect Tag", () => {
  it("can detect tags", () => {
    const wmFile = '../spec/traces/regular_rotation_in_last_state_wm_trace.winscope'
    const layersFile = '../spec/traces/regular_rotation_in_last_state_layers_trace.winscope'
    const wmBuffer = new Uint8Array(fs.readFileSync(path.resolve(__dirname, wmFile)));
    const layersBuffer = new Uint8Array(fs.readFileSync(path.resolve(__dirname, layersFile)));

    const wmTrace = decodeAndTransformProto(wmBuffer, FILE_DECODERS[FILE_TYPES.WINDOW_MANAGER_TRACE].decoderParams, true);
    const layersTrace = decodeAndTransformProto(layersBuffer, FILE_DECODERS[FILE_TYPES.SURFACE_FLINGER_TRACE].decoderParams, true);

    const engine = new TaggingEngine(wmTrace, layersTrace, (text) => { console.log(text) });
    const tagTrace = engine.run();
    expect(tagTrace.size).toEqual(4);
    expect(tagTrace.entries[0].timestamp.toString()).toEqual('280186737540384');
    expect(tagTrace.entries[1].timestamp.toString()).toEqual('280187243649340');
    expect(tagTrace.entries[2].timestamp.toString()).toEqual('280188522078113');
    expect(tagTrace.entries[3].timestamp.toString()).toEqual('280189020672174');
  })
});

describe("Error Transformation", () => {
  it("can transform error traces", () => {
    const buffer = new Uint8Array(fs.readFileSync(path.resolve(__dirname, errorTrace)));

    const data = decodeAndTransformProto(buffer, FILE_DECODERS[FILE_TYPES.ERROR_TRACE].decoderParams, true);

    expect(data.entries[0].timestamp.toString()).toEqual('161401263106');
    expect(data.entries[1].timestamp.toString()).toEqual('161126718496');
    expect(data.entries[2].timestamp.toString()).toEqual('162261072567');

    expect(data.entries[0].errors).toEqual([new Error("","",33,"",33)]);
    expect(data.entries[1].errors).toEqual([new Error("","",66,"",66)]);
    expect(data.entries[2].errors).toEqual([new Error("","",99,"",99)]);
  })
});