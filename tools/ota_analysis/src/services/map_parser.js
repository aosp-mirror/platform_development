/**
 * @fileoverview Class MapParser will take in a Android build and construct
 * several file name maps (physical address: file name) according to it.
 * The map of each partitions is added by calling MapParser.add(partitionName).
 * You can query the file name being operated by calling
 * MapParser.query(address, datalength).
 */

import * as zip from '@zip.js/zip.js/dist/zip-full.min.js'

export class MapParser {
  /**
   * This class will take in a .zip Android build and construct a file type map
   * @param {File} targetFile
   */
  constructor(targetFile) {
    this.build = new zip.ZipReader(new zip.BlobReader(targetFile))
    this.mapFiles = new Map()
    this.maps = new Map()
  }

  /**
   * Find the .map entries in the .zip build file. Store them as a map with
   * pairs of (partition name: zip.js entry).
   */
  async init() {
    let /** Array<Entry> */ entries = await this.build.getEntries()
    const /** RegExp*/ regexPath = /IMAGES\/[a-z_]*\.map/g;
    const /** RegExp*/ regexName = /[\w_]+(?=\.map)/g
    entries.forEach((entry) => {
      if (entry.filename.match(regexPath)) {
        this.mapFiles.set(entry.filename.match(regexName)[0], entry)
      }
    });
  }

  /**
   * According to the .map in the build, build a map for later query.
   * @param {String} partitionName
   * @param {Number} totalLength
   */
  async add(partitionName, totalLength) {
    let /** Array<String> */ map = []
    const /** RegExp */ regexNumber = /\d+/g
    const /** Reg */ regexRange = /\d+\-\d+/g
    for (let i = 0; i < totalLength; i++) map[i] = 'unknown'
    if (this.mapFiles.get(partitionName)) {
      let /** String */mapText =
        await this.mapFiles.get(partitionName).getData(
          new zip.TextWriter()
        )
      let /** Array<String> */fileEntries = mapText.split('\n')
      // Each line of the .map file in Android build starts with the filename
      // Followed by the block address, either a number or a range, for example:
      // //system/apex/com.android.adbd.apex 54-66 66 66-2663
      for (let entry of fileEntries) {
        let /** Array<String> */ elements = entry.split(' ')
        for (let j = 1; j < elements.length; j++) {
          let /** Number */ left = 0
          let /** Number */ right = 0
          if (elements[j].match(regexRange)) {
            left = parseInt(elements[j].match(/\d+/g)[0])
            right = parseInt(elements[j].match(/\d+/g)[1])
          } else {
            left = parseInt(elements[j].match(regexNumber))
            right = parseInt(elements[j].match(regexNumber))
          }
          InsertMap(map, elements[0], left, right)
        }
      }
      this.maps.set(partitionName, map)
    }
    else {
      this.maps.set(partitionName, map)
    }
  }

  /**
   * Return the filename of given address.
   * @param {String} partitionName
   * @param {Array<PartitionUpdate>} extents
   * @return {Array<String>}
   */
  query(partitionName, extents) {
    let /** Array<String> */ names = []
    let /** Array<String> */ map = this.maps.get(partitionName)
    for (let ext of extents) {
      names.push(queryMap(map,
        ext.startBlock,
        ext.startBlock + ext.numBlocks))
    }
    return names
  }
}

/**
 * Fill in the hashtable from <left> to <right> using <name>.
 * @param {Array<String>} map
 * @param {String} name
 * @param {Number} left
 * @param {Number} right
 */
function InsertMap(map, name, left, right) {
  for (let i = left; i <= right; i++) {
    map[i] = name
  }
}

/**
 * Query the hashtable <map> using index <address>.
 * @param {Array<String>} map
 * @param {Number} left
 * @param {Number} right
 */
function queryMap(map, left, right) {
  // Assuming the consecutive blocks belong to the same file
  // Only the start block is queried here.
  if (!map[left]) {
    return 'unknown'
  }
  return map[left]
}