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
        'bionic/libc',
        'bionic/libm',
        'bionic/libdl',
    ];
    var mods = [
        [0, 32, [6], [[1, 2]], [], [8]],
        [1, 32, [6], [], [0], [9]],
        [2, 32, [6], [], [0], [10]],
        [3, 64, [6], [[5], [4]], [], [8]],
        [4, 64, [7], [[5]], [3], [9]],
        [5, 64, [7], [], [3, 4], [9, 10]],
    ];
    insight.init(document, strs, mods);
})();
