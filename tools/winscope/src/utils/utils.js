// Find the index of the last element matching the predicate in a sorted array
function findLastMatchingSorted(array, predicate) {
    let a = 0;
    let b = array.length - 1;
    while (b - a > 1) {
        const m = Math.floor((a + b) / 2);
        if (predicate(array, m)) {
            a = m;
        } else {
            b = m - 1;
        }
    }

    return predicate(array, b) ? b : a;
}

// Make sure stableId is unique (makes old versions of proto compatible)
function stableIdCompatibilityFixup(item) {
    // For backwards compatibility
    // (the only item that doesn't have a unique stable ID in the tree)
    if (item.stableId === 'winToken|-|') {
        return item.stableId + item.children[0].stableId;
    }

    return item.stableId;
}

export { findLastMatchingSorted, stableIdCompatibilityFixup }