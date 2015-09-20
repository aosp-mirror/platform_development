#include "asm_multiarch.h"

	.section .init_array, "aw"
	ASM_ALIGN_TO_PTR_SIZE
	ASM_PTR_SIZE(0)

	.section .fini_array, "aw"
	ASM_ALIGN_TO_PTR_SIZE
	ASM_PTR_SIZE(0)
