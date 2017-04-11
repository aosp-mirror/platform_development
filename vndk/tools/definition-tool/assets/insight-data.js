(function () {
    var strs = [
        '/system/lib/libc.so',
        '/system/lib/libm.so',
        '/system/lib/libdl.so',
        '/system/lib64/libc.so',
        '/system/lib64/libm.so',
        '/system/lib64/libdl.so',
        'll-ndk',
        'hl-ndk',
    ];
    var mods = [
        [0, 32, [6], [[1, 2]], []],
        [1, 32, [6], [], [0]],
        [2, 32, [6], [], [0]],
        [3, 64, [6], [[5], [4]], []],
        [4, 64, [7], [[5]], [3]],
        [5, 64, [7], [], [3, 4]],
    ];
    insight.init(document, strs, mods);
})();
