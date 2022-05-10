/**
 * @fileoverview Offer functions that can be used to parse the partitionUpdate
 * and then do statistics over it. One can use analysePartitions to specify the
 * partitions been analysed and metrics.
 */

import { OpType, MergeOpType } from '@/services/payload.js'
import { EchartsData } from '@/services/echarts_data.js'
import { MapParser } from '@/services/map_parser.js'

/**
 * Add a <value> to a element associated to <key>. If the element dose not
 * exists than its value will be initialized to zero.
 * @param {Map} map
 * @param {String} key
 * @param {Number} value
 */
function addNumberToMap(map, key, value) {
  if (!map.get(key)) {
    map.set(key, 0)
  }
  map.set(key, map.get(key) + value)
}

/**
 * Return a statistics over the numbers of blocks (in destination) that are
 * being operated by different installation operation (e.g. REPLACE, BSDIFF).
 * Only partitions that are being passed in will be included.
 * @param {Array<PartitionUpdate>} partitions
 * @return {Map}
 */
export function operatedBlockStatistics(partitions) {
  let /** Map */ operatedBlocks = new Map()
  let /** OpType */ opType = new OpType()
  for (let partition of partitions) {
    for (let operation of partition.operations) {
      let operationType = opType.mapType.getWithDefault(operation.type)
      addNumberToMap(
        operatedBlocks,
        operationType,
        numBlocks(operation.dstExtents))
    }
  }
  return operatedBlocks
}

export function mergeOperationStatistics(partitions, blockSize) {
  let /** Map */ mergeOperations = new Map()
  let /** MergeOpType */ opType = new MergeOpType()
  let /** Number */ totalBlocks = 0
  for (let partition of partitions) {
    for (let operation of partition.mergeOperations) {
      let operationType = opType.mapType.getWithDefault(operation.type)
      addNumberToMap(
        mergeOperations,
        operationType,
        operation.dstExtent.numBlocks)
    }
    // The total blocks number should be rounded up
    totalBlocks += Math.ceil(partition.newPartitionInfo.size / blockSize)
  }
  // The COW merge operation is default to be COW_replace and not shown in
  // the manifest info. We have to mannually add that part of operations,
  // by subtracting the total blocks with other blocks.
  mergeOperations.forEach((value, key) => totalBlocks -= value)
  mergeOperations.set('COW_REPLACE', totalBlocks)
  return mergeOperations
}

/**
 * Return a statistics over the disk usage of payload.bin, based on the type of
 * installation operations. Only partitions that are being passed in will be
 * included.
 * @param {Array<PartitionUpdate>} partitions
 * @return {Map}
 */
export function operatedPayloadStatistics(partitions) {
  let /** Map */ operatedBlocks = new Map()
  let /** OpType */ opType = new OpType()
  for (let partition of partitions) {
    for (let operation of partition.operations) {
      let operationType = opType.mapType.getWithDefault(operation.type)
      addNumberToMap(
        operatedBlocks,
        operationType,
        operation.dataLength)
    }
  }
  return operatedBlocks
}

/**
 * Return a statistics over the disk usage of each file types in a OTA package.
 * A target file has to be provided and address-filename maps will be built.
 * Only partitions that are being passed in will be included.
 * @param {Array<PartitionUpdate>} partitions
 * @param {Number} blockSize
 * @param {File} targetFile
 * @return {Map}
 */
export async function operatedExtensionStatistics(partitions, blockSize, targetFile) {
  let /** Map */ operatedExtensions = new Map()
  if (!targetFile) {
    return operatedExtensions
  }
  let buildMap = new MapParser(targetFile)
  await buildMap.init()
  for (let partition of partitions) {
    await buildMap.add(
      partition.partitionName,
      Math.ceil(partition.newPartitionInfo.size / blockSize))
    for (let operation of partition.operations) {
      if (!operation.hasOwnProperty('dataLength')) continue
      let operatedFileNames = buildMap.query(
        partition.partitionName,
        operation.dstExtents)
      let extentDataLength = distributeExtensions(
        operatedFileNames,
        operation.dstExtents,
        operation.dataLength
      )
      extentDataLength.forEach((value, key) => {
        addNumberToMap(
          operatedExtensions,
          key,
          value
        )
      })
    }
  }
  return operatedExtensions
}

/**
 * Analyse the given partitions using the given metrics.
 * @param {String} metrics
 * @param {Array<PartitionUpdate>} partitions
 * @return {EchartsData}
 */
export async function analysePartitions(metrics, partitions, blockSize = 4096, targetFile = null) {
  let /** Map */statisticsData
  let /** Echartsdata */ echartsData
  switch (metrics) {
  case 'blocks':
    statisticsData = operatedBlockStatistics(partitions)
    echartsData = new EchartsData(
      statisticsData,
      'Operated blocks in target build',
      'blocks'
    )
    break
  case 'payload':
    statisticsData = operatedPayloadStatistics(partitions)
    echartsData = new EchartsData(
      statisticsData,
      'Payload disk usage',
      'bytes'
    )
    break
  case 'COWmerge':
    statisticsData = mergeOperationStatistics(partitions, blockSize)
    echartsData = new EchartsData(
      statisticsData,
      'COW merge operations',
      'blocks'
    )
    break
  case 'extensions':
    try {
      statisticsData = await operatedExtensionStatistics(partitions, blockSize, targetFile)
    }
    catch (err) {
      throw err
    }
    echartsData = new EchartsData(
      statisticsData,
      'Size of operated filename extensions',
      'bytes'
    )
  }
  if (echartsData) {
    return echartsData
  } else {
    throw 'Please double check if this is a proper AB OTA package.'
  }
}

/**
 * Calculate the number of blocks being operated
 * @param {Array<InstallOperations>} exts
 * @return {number}
 */
export function numBlocks(exts) {
  const accumulator = (total, ext) => total + ext.numBlocks
  return exts.reduce(accumulator, 0)
}

/**
 * Return a string that indicates the blocks being operated
 * in the manner of (start_block, block_length)
 * @param {Array<InstallOperations>} exts
 * @return {string}
 */
export function displayBlocks(exts) {
  const accumulator = (total, ext) =>
    total + '(' + ext.startBlock + ',' + ext.numBlocks + ')'
  return exts.reduce(accumulator, '')
}

/**
 * Return a map with pairs of (file extension, data length used by this
 * extension). The total data length will be distributed by the blocks ratio
 * of each extent.
 * @param {Array<String>} filenames
 * @param {Array<InstallOperations>} exts
 * @param {Number} length
 * @return {Map}
 */
export function distributeExtensions(filenames, exts, length) {
  let totalBlocks = numBlocks(exts)
  let distributedLengths = new Map()
  for (let i = 0; i < filenames.length; i++) {
    addNumberToMap(
      distributedLengths,
      name2Extension(filenames[i]),
      Math.round(length * exts[i].numBlocks / totalBlocks)
    )
  }
  return distributedLengths
}

/**
 * convert a filename into extension, for example:
 * '//system/apex/com.android.adbd.apex' => 'apex'
 * @param {String} filename
 * @return {String}
 */
export function name2Extension(filename) {
  let elements = filename.split('.')
  if (elements.length>1) {
    return elements[elements.length - 1]
  } else if (elements[0]==='unknown') {
    return 'unknown'
  } else {
    return 'no-extension'
  }
}