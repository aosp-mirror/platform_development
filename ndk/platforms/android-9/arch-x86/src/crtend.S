	.section .preinit_array, "aw"
	.long 0

	.section .init_array, "aw"
	.long 0

	.section .fini_array, "aw"
	.long 0

	.section	.eh_frame,"a",@progbits
	.align 4
	.type	__FRAME_END__, @object
	.size	__FRAME_END__, 4
__FRAME_END__:
	.zero	4
