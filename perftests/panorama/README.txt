How to run and verify the benchmark:

1) adb push $OUT/data/local/tmp/panorama_bench /data/local/tmp
2) adb push input /data/panorama_input
3) adb shell /data/local/tmp/panorama_bench /data/panorama_input/test /data/panorama.ppm

Sample output:

38 frames loaded
Iteration 0: 1454x330 moasic created: 4.33 seconds (2.05 + 2.28)
Iteration 1: 1454x330 moasic created: 4.26 seconds (1.83 + 2.44)
Iteration 2: 1454x330 moasic created: 5.57 seconds (2.73 + 2.84)
Iteration 3: 1454x330 moasic created: 5.15 seconds (2.33 + 2.82)
Iteration 4: 1454x330 moasic created: 6.22 seconds (2.05 + 4.16)
Iteration 5: 1454x330 moasic created: 6.31 seconds (2.16 + 4.15)
Iteration 6: 1454x330 moasic created: 5.04 seconds (2.03 + 3.01)
Iteration 7: 1454x330 moasic created: 6.30 seconds (3.47 + 2.83)
Iteration 8: 1454x330 moasic created: 6.57 seconds (1.83 + 4.73)
Iteration 9: 1454x330 moasic created: 6.27 seconds (2.28 + 4.00)
Total elapsed time: 56.02 seconds

The first number in the parenthesis is the time to align the frames; the second
number is the time to stitch them.

The total elapsed time is the interesting number for benchmarking.

The result of the benchmark can be verified by pulling the the output
photo off the device and comparing it against the golden reference:

1) adb pull /data/panorama.ppm .
2) diff panorama.ppm output/golden.ppm
