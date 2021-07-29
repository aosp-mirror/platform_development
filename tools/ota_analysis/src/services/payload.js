/**
 * @fileoverview Class paypload is used to read in and
 * parse the payload.bin file from a OTA.zip file.
 * Class OpType creates a Map that can resolve the
 * operation type.
 * @package zip.js
 * @package protobufjs
 */

import * as zip from '@zip.js/zip.js/dist/zip-full.min.js'
import { chromeos_update_engine as update_metadata_pb } from './update_metadata_pb.js'

const /** String */ _MAGIC = 'CrAU'
const /** Number */ _VERSION_SIZE = 8
const /** Number */ _MANIFEST_LEN_SIZE = 8
const /** Number */ _METADATA_SIGNATURE_LEN_SIZE = 4
const /** Number */ _BRILLO_MAJOR_PAYLOAD_VERSION = 2
export const /** Array<Object> */ MetadataFormat = [
  {
    prefix: 'pre-build',
    key: 'preBuild',
    name: 'Pre-build'
  },
  {
    prefix: 'pre-build-incremental',
    key: 'preBuildVersion',
    name: 'Pre-build version'
  },
  {
    prefix: 'post-build',
    key: 'postBuild',
    name: 'Post-build'
  },
  {
    prefix: 'post-build-incremental',
    key: 'postBuildVersion',
    name: 'Post-build version'
  }
]

export class Payload {
  /**
   * This class parses the metadata of a OTA package.
   * @param {File} file A OTA.zip file read from user's machine.
   */
  constructor(file) {
    this.packedFile = new zip.ZipReader(new zip.BlobReader(file))
    this.cursor = 0
  }

  /**
   * Unzip the OTA package, get payload.bin and metadata
   */
  async unzip() {
    let /** Array<Entry> */ entries = await this.packedFile.getEntries()
    this.payload = null
    for (let entry of entries) {
      if (entry.filename == 'payload.bin') {
        //TODO: only read in the manifest instead of the whole payload
        this.payload = await entry.getData(new zip.BlobWriter())
      }
      if (entry.filename == 'META-INF/com/android/metadata') {
        this.metadata = await entry.getData(new zip.TextWriter())
      }
    }
    if (!this.payload) {
      alert('Please select a legit OTA package')
      return
    }
    this.buffer = await this.payload.arrayBuffer()
  }

  /**
   * Read in an integer from binary bufferArray.
   * @param {Int} size the size of a integer being read in
   * @return {Int} an integer.
   */
  readInt(size) {
    let /** DataView */ view = new DataView(
      this.buffer.slice(this.cursor, this.cursor + size))
    if (typeof view.getBigUint64 !== "function") {
      view.getBigUint64 =
              function(offset) {
                const a = BigInt(view.getUint32(offset))
                const b = BigInt(view.getUint32(offset + 4))
                const bigNumber = a * 4294967296n + b
                return bigNumber
              }
    }
    this.cursor += size
    switch (size) {
    case 2:
      return view.getUInt16(0)
    case 4:
      return view.getUint32(0)
    case 8:
      return Number(view.getBigUint64(0))
    default:
      throw 'Cannot read this integer with size ' + size
    }
  }

  readHeader() {
    let /** TextDecoder */ decoder = new TextDecoder()
    try {
      this.magic = decoder.decode(
        this.buffer.slice(this.cursor, _MAGIC.length))
      this.cursor += _MAGIC.length
      if (this.magic != _MAGIC) {
        alert('MAGIC is not correct, please double check.')
      }
      this.header_version = this.readInt(_VERSION_SIZE)
      this.manifest_len = this.readInt(_MANIFEST_LEN_SIZE)
      if (this.header_version == _BRILLO_MAJOR_PAYLOAD_VERSION) {
        this.metadata_signature_len = this.readInt(_METADATA_SIGNATURE_LEN_SIZE)
      }
    } catch (err) {
      console.log(err)
      return
    }
  }

  /**
   * Read in the manifest in an OTA.zip file.
   * The structure of the manifest can be found in:
   * aosp/system/update_engine/update_metadata.proto
   */
  readManifest() {
    let /** Array<Uint8> */ manifest_raw = new Uint8Array(this.buffer.slice(
      this.cursor, this.cursor + this.manifest_len
    ))
    this.cursor += this.manifest_len
    this.manifest = update_metadata_pb.DeltaArchiveManifest
      .decode(manifest_raw)
  }

  readSignature() {
    let /** Array<Uint8>*/ signature_raw = new Uint8Array(this.buffer.slice(
      this.cursor, this.cursor + this.metadata_signature_len
    ))
    this.cursor += this.metadata_signature_len
    this.metadata_signature = update_metadata_pb.Signatures
      .decode(signature_raw)
  }

  parseMetadata() {
    for (let formatter of MetadataFormat) {
      let regex = new RegExp(formatter.prefix + '.+')
      if (this.metadata.match(regex)) {
        this[formatter.key] =
          trimEntry(this.metadata.match(regex)[0], formatter.prefix)
      } else this[formatter.key] = ''
    }
  }

  async init() {
    await this.unzip()
    this.readHeader()
    this.readManifest()
    this.readSignature()
    this.parseMetadata()
  }

}

export class OpType {
  /**
   * OpType.mapType create a map that could resolve the operation
   * types. The operation types are encoded as numbers in
   * update_metadata.proto and must be decoded before any usage.
   */
  constructor() {
    let /** Array<{String: Number}>*/ types = update_metadata_pb.InstallOperation.Type
    this.mapType = new Map()
    for (let key in types) {
      this.mapType.set(types[key], key)
    }
  }
}

export class MergeOpType {
  /**
   * MergeOpType create a map that could resolve the COW merge operation
   * types. This is very similar to OpType class except that one is for
   * installation operations.
   */
  constructor() {
    let /** Array<{String: Number}>*/ types =
      update_metadata_pb.CowMergeOperation.Type
    this.mapType = new Map()
    for (let key in types) {
      this.mapType.set(types[key], key)
    }
  }
}

export function octToHex(bufferArray, space = true, maxLine = 16) {
  let hex_table = ''
  for (let i = 0; i < bufferArray.length; i++) {
    if (bufferArray[i].toString(16).length === 2) {
      hex_table += bufferArray[i].toString(16) + (space ? ' ' : '')
    } else {
      hex_table += '0' + bufferArray[i].toString(16) + (space ? ' ' : '')
    }
    if ((i + 1) % maxLine == 0) {
      hex_table += '\n'
    }
  }
  return hex_table
}

/**
 * Trim the prefix in an entry. This is required because the lookbehind
 * regular expression is not supported in safari yet.
 * @param {String} entry
 * @param {String} prefix
 * @return String
 */
function trimEntry(entry, prefix) {
  return entry.slice(prefix.length + 1, entry.length)
}