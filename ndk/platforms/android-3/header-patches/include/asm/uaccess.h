#ifndef _ASM_UACCESS_H
#define _ASM_UACCESS_H

#include <linux/sched.h>
#include <asm/errno.h>
#include <asm/memory.h>
#include <asm/system.h>

#define VERIFY_READ 0
#define VERIFY_WRITE 1

struct exception_table_entry
{
 unsigned long insn, fixup;
};

#define KERNEL_DS 0x00000000
#define get_ds() (KERNEL_DS)

#define USER_DS KERNEL_DS

#define segment_eq(a,b) (1)
#define __addr_ok(addr) (1)
#define __range_ok(addr,size) (0)
#define get_fs() (KERNEL_DS)

#define get_user(x,p)
#define put_user(x,p)
#define access_ok(type,addr,size) (__range_ok(addr,size) == 0)
#define __copy_from_user(to,from,n) (memcpy(to, (void __force *)from, n), 0)
#define __copy_to_user(to,from,n) (memcpy((void __force *)to, from, n), 0)
#define __clear_user(addr,n) (memset((void __force *)addr, 0, n), 0)

#define __copy_to_user_inatomic __copy_to_user
#define __copy_from_user_inatomic __copy_from_user
#define strlen_user(s) strnlen_user(s, ~0UL >> 1)
#endif
