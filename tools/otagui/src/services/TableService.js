export function TableSort(arr, key, sortOrder, offset, limit) {
  let orderNumber = 1
  if (sortOrder==="desc") {
    orderNumber = -1
  }
  return arr.sort(function(a, b) {
    var keyA = a[key],
      keyB = b[key];
    if (keyA < keyB) return -orderNumber;
    if (keyA > keyB) return orderNumber;
    return 0;
  }).slice(offset, offset + limit);
}