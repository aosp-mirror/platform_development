#ifndef _JBOFFSETS_H_
#define _JBOFFSETS_H_

/*
 * Normally the same layout is used for saving the registers in jmp_buf
 * as that used in struct sigcontext. For portability all of the registers need
 * to be stored in the space available in a portable jmp_buf so this code
 * packs the register together.
 */

#define JB_MASK         (0*REGSZ)
#define JB_PC           (1*REGSZ)
#define JB_MAGIC        (2*REGSZ)
#define JB_S0           (3*REGSZ)
#define JB_S1           (4*REGSZ)
#define JB_S2           (5*REGSZ)
#define JB_S3           (6*REGSZ)
#define JB_S4           (7*REGSZ)
#define JB_S5           (8*REGSZ)
#define JB_S6           (9*REGSZ)
#define JB_S7           (10*REGSZ)
#define JB_S8           (11*REGSZ)
#define JB_GP           (12*REGSZ)
#define JB_SP           (13*REGSZ)
#define JB_SAVEMASK     (14*REGSZ)
#define JB_FPUSED       (15*REGSZ)
#define JB_FSR          (16*REGSZ)
#define JB_FPBASE       (18*REGSZ)
#define JB_F20          (JB_FPBASE+0*REGSZ_FP)
#define JB_F21          (JB_FPBASE+1*REGSZ_FP)
#define JB_F22          (JB_FPBASE+2*REGSZ_FP)
#define JB_F23          (JB_FPBASE+3*REGSZ_FP)
#define JB_F24          (JB_FPBASE+4*REGSZ_FP)
#define JB_F25          (JB_FPBASE+5*REGSZ_FP)
#define JB_F26          (JB_FPBASE+6*REGSZ_FP)
#define JB_F27          (JB_FPBASE+7*REGSZ_FP)
#define JB_F28          (JB_FPBASE+8*REGSZ_FP)
#define JB_F29          (JB_FPBASE+9*REGSZ_FP)
#define JB_F30          (JB_FPBASE+10*REGSZ_FP)
#define JB_F31          (JB_FPBASE+11*REGSZ_FP)

/* Use different magic numbers to avoid misuse of native vs portable contexts */
#define MAGIC_SETJMP    0xACEDBEAD
#define MAGIC__SETJMP   0xBEAD1CAB

#endif /* _JBOFFSETS_H_ */
