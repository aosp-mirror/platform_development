SECTIONS {
  TEXT_START = 0x00010000 ;
  DATA_START = 0x00200000 ;
  handlers 0x0 : { *(handlers) }
  .text TEXT_START : { *(.text) }
  .data DATA_START : { *(.data) }
  .bss  : { *(.bss) *(COMMON) }
  p00300000 0x00300000 : { *(p00300000) }
  p00400000 0x00400000 : { *(p00400000) }
}
