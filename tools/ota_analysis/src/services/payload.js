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
import { PayloadNonAB } from './payload_nonab.js'

const /** String */ _MAGIC = 'CrAU'
const /** Number */ _VERSION_SIZE = 8
const /** Number */ _MANIFEST_LEN_SIZE = 8
const /** Number */ _METADATA_SIGNATURE_LEN_SIZE = 4

const /** Number */ _PAYLOAD_HEADER_SIZE =
  _MAGIC.length + _VERSION_SIZE + _MANIFEST_LEN_SIZE + _METADATA_SIGNATURE_LEN_SIZE

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

class StopIteration extends Error {

}

class OTAPayloadBlobWriter extends zip.Writer {
  /**
   * A zip.Writer that is tailored for OTA payload.bin read-in.
   * Instead of reading in all the contents in payload.bin, this writer will
   * throw an 'StopIteration' error when the header is read in.
   * The header will be stored into the <payload>.
   * @param {Payload} payload
   * @param {String} contentType
   */
  constructor(payload, contentType = "") {
    super()
    this.offset = 0
    this.contentType = contentType
    this.blob = new Blob([], { type: contentType })
    this.prefixLength = 0
    this.payload = payload
  }

  async writeUint8Array(/**  @type {Uint8Array} */ array) {
    super.writeUint8Array(array)
    this.blob = new Blob([this.blob, array.buffer], { type: this.contentType })
    this.offset = this.blob.size
    // Once the prefixLength is non-zero, the address of manifest and signature
    // become known and can be read in. Otherwise the header needs to be read
    // in first to determine the prefixLength.
    if (this.offset >= _PAYLOAD_HEADER_SIZE) {
      await this.payload.readHeader(this.blob)
      this.prefixLength =
        _PAYLOAD_HEADER_SIZE
        + this.payload.manifest_len
        + this.payload.metadata_signature_len
      console.log(`Computed metadata length: ${this.prefixLength}`);
    }
    if (this.prefixLength > 0) {
      console.log(`${this.offset}/${this.prefixLength}`);
      if (this.offset >= this.prefixLength) {
        await this.payload.readManifest(this.blob)
        await this.payload.readSignature(this.blob)
      }
    }
    // The prefix has everything we need (header, manifest, signature). Once
    // the offset is beyond the prefix, no need to move on.
    if (this.offset >= this.prefixLength) {
      throw new StopIteration()
    }
  }

  getData() {
    return this.blob
  }
}

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
        let writer = new OTAPayloadBlobWriter(this, "")
        try {
          await entry.getData(writer)
        } catch (e) {
          if (e instanceof StopIteration) {
            // Exception used as a hack to stop reading from zip. NO need to do anything
            // Ideally zip.js would provide an API to partialll read a zip
            // entry, but they don't. So this is what we get
          } else {
            throw e
          }
        }
        this.payload = writer.getData()
        break
      }
      if (entry.filename == 'META-INF/com/android/metadata') {
        this.metadata = await entry.getData(new zip.TextWriter())
      }
    }
    if (!this.payload) {
      try {
        // The temporary variable manifest has to be used here, to prevent the html page
        // being rendered before everything is read in properly
        let manifest = new PayloadNonAB(this.packedFile)
        await manifest.init()
        manifest.nonAB = true
        this.manifest = manifest
      } catch (error) {
        alert('Please select a legit OTA package')
        return
      }
    }
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
        function (offset) {
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

  /**
   * Read the header of payload.bin, including the magic, header_version,
   * manifest_len, metadata_signature_len.
   */
  async readHeader(/** @type {Blob} */buffer) {
    this.buffer = await buffer.slice(0, _PAYLOAD_HEADER_SIZE).arrayBuffer()
    let /** TextDecoder */ decoder = new TextDecoder()
    try {
      this.magic = decoder.decode(
        this.buffer.slice(this.cursor, _MAGIC.length))
      if (this.magic != _MAGIC) {
        throw new Error('MAGIC is not correct, please double check.')
      }
      this.cursor += _MAGIC.length
      this.header_version = this.readInt(_VERSION_SIZE)
      this.manifest_len = this.readInt(_MANIFEST_LEN_SIZE)
      if (this.header_version == _BRILLO_MAJOR_PAYLOAD_VERSION) {
        this.metadata_signature_len = this.readInt(_METADATA_SIGNATURE_LEN_SIZE)
      }
      else {
        throw new Error(`Unexpected major version number: ${this.header_version}`)
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
  async readManifest(/** @type {Blob} */buffer) {
    buffer = await buffer.slice(
      this.cursor, this.cursor + this.manifest_len).arrayBuffer()
    this.cursor += this.manifest_len
    this.manifest = update_metadata_pb.DeltaArchiveManifest
      .decode(new Uint8Array(buffer))
    this.manifest.nonAB = false
  }

  async readSignature(/** @type {Blob} */buffer) {
    buffer = await buffer.slice(
      this.cursor, this.cursor + this.metadata_signature_len).arrayBuffer()
    this.cursor += this.metadata_signature_len
    this.metadata_signature = update_metadata_pb.Signatures
      .decode(new Uint8Array(buffer))
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
    this.parseMetadata()
  }

}

export class DefaultMap extends Map {
  /** Reload the original get method. Return the original key value if
   * the key does not exist.
   * @param {Any} key
   */
  getWithDefault(key) {
    if (!this.has(key)) return key
    return this.get(key)
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
    this.mapType = new DefaultMap()
    for (let key of Object.keys(types)) {
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
    this.mapType = new DefaultMap()
    for (let key of Object.keys(types)) {
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