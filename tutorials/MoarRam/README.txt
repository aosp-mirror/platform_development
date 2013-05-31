This is a simple app that can be used as a tutorial or reference benchmark
for the development of the DDMS native heap tracker feature. It contains 3
unique paths to allocate heap chunks:

1) Java_com_android_benchmark_moarram_MainActivity_add32ByteBlocksNative in
   foo.c (libmoarram-foo.so). Each invocation will allocate 32 bytes.
2) Java_com_android_benchmark_moarram_MainActivity_add2MByteBlocksNative in
   bar.c (libmoarram-bar.so). Each invocation will allocate 2M bytes.
3) Java_com_android_benchmark_moarram_MainActivity_addVariableSizedBlocksNative
   in baz.c (libmoarram-baz.so). Each invocation will allocate 17 or 71 bytes,
   depending on the active button in a radio group.

Each allocation can be freed by clicking the corresponding free button in the
UI.
