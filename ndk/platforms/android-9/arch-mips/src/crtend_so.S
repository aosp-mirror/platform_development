	.section .init_array, "aw"
	.long 0

	.section .fini_array, "aw"
	.long 0

	.section .ctors, "aw", @progbits
	.type __CTOR_END__, @object
__CTOR_END__:
	.long 0
	
	.section .dtors, "aw", @progbits
	.type __DTOR_END__, @object
__DTOR_END__:
	.long 0

	.section	.eh_frame,"a",@progbits
	.align 4
	.type	__FRAME_END__, @object
	.size	__FRAME_END__, 4
__FRAME_END__:
	.zero	4
	
