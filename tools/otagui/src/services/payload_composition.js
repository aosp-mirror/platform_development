import { OpType } from '@/services/payload.js'

/**
 * Return a statistics over the numbers of blocks (in destination) that are
 * being operated by different installation operation (e.g. REPLACE, BSDIFF).
 * Only partitions that are being passed in will be included.
 * @param {Array<PartitionUpdate>} partitions
 * @return {Map}
 */
export function operatedBlockStatistics(partitions) {
  let operatedBlocks = new Map()
  let opType = new OpType()
  for (let partition of partitions) {
    for (let operation of partition.operations) {
      let operationType = opType.mapType.get(operation.type)
      if (!operatedBlocks.get(operationType)) {
        operatedBlocks.set(operationType, 0)
      }
      operatedBlocks.set(
        operationType,
        operatedBlocks.get(operationType) + numBlocks(operation.dstExtents)
      )
    }
  }
  return operatedBlocks
}

/**
 * Return a statistics over the disk usage of payload.bin, based on the type of
 * installation operations. Only partitions that are being passed in will be
 * included.
 * @param {Array<PartitionUpdate>} partitions
 * @return {Map}
 */
export function operatedPayloadStatistics(partitions) {
  let operatedBlocks = new Map()
  let opType = new OpType()
  for (let partition of partitions) {
    for (let operation of partition.operations) {
      let operationType = opType.mapType.get(operation.type)
      if (!operatedBlocks.get(operationType)) {
        operatedBlocks.set(operationType, 0)
      }
      operatedBlocks.set(
        operationType,
        operatedBlocks.get(operationType) + operation.dataLength
      )
    }
  }
  return operatedBlocks
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
 * @param {Array<InstallOperations} exts
 * @return {string}
 */
export function displayBlocks(exts) {
  const accumulator = (total, ext) =>
    total + '(' + ext.startBlock + ',' + ext.numBlocks + ')'
  return exts.reduce(accumulator, '')
}