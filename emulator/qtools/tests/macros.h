#ifndef _TEST_TRACE_C_H_
#define _TEST_TRACE_C_H_

/* the base address of trace device */
#define TRACE_DEV_BASE_ADDR             0x21000000

/*the register addresses of the trace device */
#define TRACE_DEV_REG_SWITCH            0
#define TRACE_DEV_REG_FORK              1
#define TRACE_DEV_REG_EXECVE_PID        2
#define TRACE_DEV_REG_EXECVE_VMSTART    3
#define TRACE_DEV_REG_EXECVE_VMEND      4
#define TRACE_DEV_REG_EXECVE_OFFSET     5
#define TRACE_DEV_REG_EXECVE_EXEPATH    6
#define TRACE_DEV_REG_EXIT              7
#define TRACE_DEV_REG_CMDLINE           8
#define TRACE_DEV_REG_CMDLINE_LEN       9
#define TRACE_DEV_REG_MMAP_EXEPATH      10
#define TRACE_DEV_REG_INIT_PID          11
#define TRACE_DEV_REG_INIT_NAME         12
#define TRACE_DEV_REG_CLONE             13
#define TRACE_DEV_REG_DYN_SYM           50
#define TRACE_DEV_REG_DYN_SYM_ADDR      51
#define TRACE_DEV_REG_PRINT_STR         60
#define TRACE_DEV_REG_PRINT_NUM_DEC     61
#define TRACE_DEV_REG_PRINT_NUM_HEX     62
#define TRACE_DEV_REG_STOP_EMU          90
#define TRACE_DEV_REG_ENABLE            100
#define TRACE_DEV_REG_DISABLE           101

/* write a word to a trace device register */
#define DEV_WRITE_WORD(addr,value)\
    (*(volatile unsigned long *)(TRACE_DEV_BASE_ADDR + ((addr) << 2)) = (value))

/*************************************************************/
/* generates test events */

/* context switch */
#define TRACE_SWITCH(pid)            DEV_WRITE_WORD(TRACE_DEV_REG_SWITCH, (pid))
/* fork */
#define TRACE_FORK(pid)              DEV_WRITE_WORD(TRACE_DEV_REG_FORK, (pid))
/* clone */
#define TRACE_CLONE(pid)             DEV_WRITE_WORD(TRACE_DEV_REG_CLONE, (pid))
/* dump name and path of threads executed before trace device created */
#define TRACE_INIT_NAME(pid,path)\
do {\
    DEV_WRITE_WORD(TRACE_DEV_REG_INIT_PID, (pid));\
    DEV_WRITE_WORD(TRACE_DEV_REG_INIT_NAME, (unsigned long)(path));\
}while(0)
/* dump exec mapping of threads executed before trace device created */
#define TRACE_INIT_EXEC(vstart,vend,eoff,path)\
do {\
    DEV_WRITE_WORD(TRACE_DEV_REG_EXECVE_VMSTART, (vstart));\
    DEV_WRITE_WORD(TRACE_DEV_REG_EXECVE_VMEND, (vend));\
    DEV_WRITE_WORD(TRACE_DEV_REG_EXECVE_OFFSET, (eoff));\
    DEV_WRITE_WORD(TRACE_DEV_REG_EXECVE_EXEPATH, (unsigned long)(path));\
}while(0)
/* mmap */
#define TRACE_MMAP(vstart,vend,eoff,path)\
do {\
    DEV_WRITE_WORD(TRACE_DEV_REG_EXECVE_VMSTART, (vstart));\
    DEV_WRITE_WORD(TRACE_DEV_REG_EXECVE_VMEND, (vend));\
    DEV_WRITE_WORD(TRACE_DEV_REG_EXECVE_OFFSET, (eoff));\
    DEV_WRITE_WORD(TRACE_DEV_REG_MMAP_EXEPATH, (unsigned long)(path));\
}while(0)
/* execve */
#define TRACE_EXECVE(cmdlen,cmd)\
do {\
    DEV_WRITE_WORD(TRACE_DEV_REG_CMDLINE_LEN, (cmdlen));\
    DEV_WRITE_WORD(TRACE_DEV_REG_CMDLINE, (unsigned long)(cmd));\
}while(0)
/* exit */
#define TRACE_EXIT(retv)             DEV_WRITE_WORD(TRACE_DEV_REG_EXIT, (retv))

/* other commands */

/* stop emulation */
#define TRACE_STOP_EMU()             DEV_WRITE_WORD(TRACE_DEV_REG_STOP_EMU, 1)
/* enable/disable tracing */
#define TRACE_ENABLE_TRACING()       DEV_WRITE_WORD(TRACE_DEV_REG_ENABLE, 1)
#define TRACE_DISABLE_TRACING()      DEV_WRITE_WORD(TRACE_DEV_REG_DISABLE, 1)
/* dynamic symbols */
#define TRACE_DYN_SYM(addr,sym)\
do {\
    DEV_WRITE_WORD(TRACE_DEV_REG_DYN_SYM_ADDR, (addr));\
    DEV_WRITE_WORD(TRACE_DEV_REG_DYN_SYM, (unsigned long)(sym));\
}while(0)
/* prints */
#define PRINT_STR(str)         DEV_WRITE_WORD(TRACE_DEV_REG_PRINT_STR, (unsigned long)(str))
#define PRINT_NUM_DEC(num)     DEV_WRITE_WORD(TRACE_DEV_REG_PRINT_NUM_DEC, (num))
#define PRINT_NUM_HEX(num)     DEV_WRITE_WORD(TRACE_DEV_REG_PRINT_NUM_HEX, (num))

#endif
