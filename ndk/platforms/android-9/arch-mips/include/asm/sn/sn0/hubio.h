/****************************************************************************
 ****************************************************************************
 ***
 ***   This header was automatically generated from a Linux kernel header
 ***   of the same name, to make information necessary for userspace to
 ***   call into the kernel available to libc.  It contains only constants,
 ***   structures, and macros generated from the original header, and thus,
 ***   contains no copyrightable information.
 ***
 ***   To edit the content of this header, modify the corresponding
 ***   source file (e.g. under external/kernel-headers/original/) then
 ***   run bionic/libc/kernel/tools/update_all.py
 ***
 ***   Any manual change here will be lost the next time this script will
 ***   be run. You've been warned!
 ***
 ****************************************************************************
 ****************************************************************************/
#ifndef _ASM_SGI_SN_SN0_HUBIO_H
#define _ASM_SGI_SN_SN0_HUBIO_H
#define IIO_WIDGET IIO_WID  
#define IIO_WIDGET_STAT IIO_WSTAT  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_WIDGET_CTRL IIO_WCR  
#define IIO_WIDGET_TOUT IIO_WRTO  
#define IIO_WIDGET_FLUSH IIO_WTFR  
#define IIO_PROTECT IIO_ILAPR  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_PROTECT_OVRRD IIO_ILAPO  
#define IIO_OUTWIDGET_ACCESS IIO_IOWA  
#define IIO_INWIDGET_ACCESS IIO_IIWA  
#define IIO_INDEV_ERR_MASK IIO_IIDEM  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_LLP_CSR IIO_ILCSR  
#define IIO_LLP_LOG IIO_ILLR  
#define IIO_XTALKCC_TOUT IIO_IXCC  
#define IIO_XTALKTT_TOUT IIO_IXTT  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_IO_ERR_CLR IIO_IECLR  
#define IIO_BTE_CRB_CNT IIO_IBCN  
#define IIO_LLP_CSR_IS_UP 0x00002000
#define IIO_LLP_CSR_LLP_STAT_MASK 0x00003000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_LLP_CSR_LLP_STAT_SHFT 12
#define IIO_PROTECT_OVRRD_KEY 0x53474972756c6573ull  
#define IIO_BTE_STAT_0 IIO_IBLS_0  
#define IIO_BTE_SRC_0 IIO_IBSA_0  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_BTE_DEST_0 IIO_IBDA_0  
#define IIO_BTE_CTRL_0 IIO_IBCT_0  
#define IIO_BTE_NOTIFY_0 IIO_IBNA_0  
#define IIO_BTE_INT_0 IIO_IBIA_0  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_BTE_OFF_0 0  
#define IIO_BTE_OFF_1 IIO_IBLS_1 - IIO_IBLS_0  
#define BTEOFF_STAT 0
#define BTEOFF_SRC (IIO_BTE_SRC_0 - IIO_BTE_STAT_0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define BTEOFF_DEST (IIO_BTE_DEST_0 - IIO_BTE_STAT_0)
#define BTEOFF_CTRL (IIO_BTE_CTRL_0 - IIO_BTE_STAT_0)
#define BTEOFF_NOTIFY (IIO_BTE_NOTIFY_0 - IIO_BTE_STAT_0)
#define BTEOFF_INT (IIO_BTE_INT_0 - IIO_BTE_STAT_0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_BASE 0x400000
#define IIO_BASE_BTE0 0x410000
#define IIO_BASE_BTE1 0x420000
#define IIO_BASE_PERF 0x430000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_PERF_CNT 0x430008
#define IO_PERF_SETS 32
#define IIO_WID 0x400000  
#define IIO_WSTAT 0x400008  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_WCR 0x400020  
#define IIO_WSTAT_ECRAZY (1ULL << 32)  
#define IIO_WSTAT_TXRETRY (1ULL << 9)  
#define IIO_WSTAT_TXRETRY_MASK (0x7F)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_WSTAT_TXRETRY_SHFT (16)
#define IIO_WSTAT_TXRETRY_CNT(w) (((w) >> IIO_WSTAT_TXRETRY_SHFT) &   IIO_WSTAT_TXRETRY_MASK)
#define IIO_ILAPR 0x400100  
#define IIO_ILAPO 0x400108  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_IOWA 0x400110  
#define IIO_IIWA 0x400118  
#define IIO_IIDEM 0x400120  
#define IIO_ILCSR 0x400128  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ILLR 0x400130  
#define IIO_IIDSR 0x400138  
#define IIO_IIBUSERR 0x1400208  
#define IIO_IIDSR_SENT_SHIFT 28
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_IIDSR_SENT_MASK 0x10000000
#define IIO_IIDSR_ENB_SHIFT 24
#define IIO_IIDSR_ENB_MASK 0x01000000
#define IIO_IIDSR_NODE_SHIFT 8
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_IIDSR_NODE_MASK 0x0000ff00
#define IIO_IIDSR_LVL_SHIFT 0
#define IIO_IIDSR_LVL_MASK 0x0000003f
#define IIO_IGFX_0 0x400140  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_IGFX_1 0x400148  
#define IIO_IGFX_W_NUM_BITS 4  
#define IIO_IGFX_W_NUM_MASK ((1<<IIO_IGFX_W_NUM_BITS)-1)
#define IIO_IGFX_W_NUM_SHIFT 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_IGFX_N_NUM_BITS 9  
#define IIO_IGFX_N_NUM_MASK ((1<<IIO_IGFX_N_NUM_BITS)-1)
#define IIO_IGFX_N_NUM_SHIFT 4
#define IIO_IGFX_P_NUM_BITS 1  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_IGFX_P_NUM_MASK ((1<<IIO_IGFX_P_NUM_BITS)-1)
#define IIO_IGFX_P_NUM_SHIFT 16
#define IIO_IGFX_VLD_BITS 1  
#define IIO_IGFX_VLD_MASK ((1<<IIO_IGFX_VLD_BITS)-1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_IGFX_VLD_SHIFT 20
#define IIO_IGFX_INIT(widget, node, cpu, valid) (  (((widget) & IIO_IGFX_W_NUM_MASK) << IIO_IGFX_W_NUM_SHIFT) |   (((node) & IIO_IGFX_N_NUM_MASK) << IIO_IGFX_N_NUM_SHIFT) |   (((cpu) & IIO_IGFX_P_NUM_MASK) << IIO_IGFX_P_NUM_SHIFT) |   (((valid) & IIO_IGFX_VLD_MASK) << IIO_IGFX_VLD_SHIFT) )
#define IIO_SCRATCH_REG0 0x400150
#define IIO_SCRATCH_REG1 0x400158
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_SCRATCH_MASK 0x0000000f00f11fff
#define IIO_SCRATCH_BIT0_0 0x0000000800000000
#define IIO_SCRATCH_BIT0_1 0x0000000400000000
#define IIO_SCRATCH_BIT0_2 0x0000000200000000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_SCRATCH_BIT0_3 0x0000000100000000
#define IIO_SCRATCH_BIT0_4 0x0000000000800000
#define IIO_SCRATCH_BIT0_5 0x0000000000400000
#define IIO_SCRATCH_BIT0_6 0x0000000000200000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_SCRATCH_BIT0_7 0x0000000000100000
#define IIO_SCRATCH_BIT0_8 0x0000000000010000
#define IIO_SCRATCH_BIT0_9 0x0000000000001000
#define IIO_SCRATCH_BIT0_R 0x0000000000000fff
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_NUM_ITTES 7  
#define HUB_NUM_BIG_WINDOW IIO_NUM_ITTES - 1
#define SWIN0_BIGWIN HUB_NUM_BIG_WINDOW
#define ILCSR_WARM_RESET 0x100
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifndef __ASSEMBLY__
typedef union hubii_wid_u {
 u64 wid_reg_value;
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u64 wid_rsvd: 32,
 wid_rev_num: 4,
 wid_part_num: 16,
 wid_mfg_num: 11,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 wid_rsvd1: 1;
 } wid_fields_s;
} hubii_wid_t;
typedef union hubii_wcr_u {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u64 wcr_reg_value;
 struct {
 u64 wcr_rsvd: 41,
 wcr_e_thresh: 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 wcr_dir_con: 1,
 wcr_f_bad_pkt: 1,
 wcr_xbar_crd: 3,
 wcr_rsvd1: 8,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 wcr_tag_mode: 1,
 wcr_widget_id: 4;
 } wcr_fields_s;
} hubii_wcr_t;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define iwcr_dir_con wcr_fields_s.wcr_dir_con
typedef union hubii_wstat_u {
 u64 reg_value;
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u64 rsvd1: 31,
 crazy: 1,
 rsvd2: 8,
 llp_tx_cnt: 8,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 rsvd3: 6,
 tx_max_rtry: 1,
 rsvd4: 2,
 xt_tail_to: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 xt_crd_to: 1,
 pending: 4;
 } wstat_fields_s;
} hubii_wstat_t;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef union hubii_ilcsr_u {
 u64 icsr_reg_value;
 struct {
 u64 icsr_rsvd: 22,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 icsr_max_burst: 10,
 icsr_rsvd4: 6,
 icsr_max_retry: 10,
 icsr_rsvd3: 2,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 icsr_lnk_stat: 2,
 icsr_bm8: 1,
 icsr_llp_en: 1,
 icsr_rsvd2: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 icsr_wrm_reset: 1,
 icsr_rsvd1: 2,
 icsr_null_to: 6;
 } icsr_fields_s;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} hubii_ilcsr_t;
typedef union hubii_iowa_u {
 u64 iowa_reg_value;
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u64 iowa_rsvd: 48,
 iowa_wxoac: 8,
 iowa_rsvd1: 7,
 iowa_w0oac: 1;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 } iowa_fields_s;
} hubii_iowa_t;
typedef union hubii_iiwa_u {
 u64 iiwa_reg_value;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct {
 u64 iiwa_rsvd: 48,
 iiwa_wxiac: 8,
 iiwa_rsvd1: 7,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 iiwa_w0iac: 1;
 } iiwa_fields_s;
} hubii_iiwa_t;
typedef union hubii_illr_u {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u64 illr_reg_value;
 struct {
 u64 illr_rsvd: 32,
 illr_cb_cnt: 16,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 illr_sn_cnt: 16;
 } illr_fields_s;
} hubii_illr_t;
typedef union io_perf_sel {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u64 perf_sel_reg;
 struct {
 u64 perf_rsvd : 48,
 perf_icct : 8,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 perf_ippr1 : 4,
 perf_ippr0 : 4;
 } perf_sel_bits;
} io_perf_sel_t;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef union io_perf_cnt {
 u64 perf_cnt;
 struct {
 u64 perf_rsvd1 : 32,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 perf_rsvd2 : 12,
 perf_cnt : 20;
 } perf_cnt_bits;
} io_perf_cnt_t;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#define LNK_STAT_WORKING 0x2
#define IIO_LLP_CB_MAX 0xffff
#define IIO_LLP_SN_MAX 0xffff
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_NUM_IPRBS (9)
#define IIO_IOPRB_0 0x400198  
#define IIO_IOPRB_8 0x4001a0  
#define IIO_IOPRB_9 0x4001a8  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_IOPRB_A 0x4001b0  
#define IIO_IOPRB_B 0x4001b8  
#define IIO_IOPRB_C 0x4001c0  
#define IIO_IOPRB_D 0x4001c8  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_IOPRB_E 0x4001d0  
#define IIO_IOPRB_F 0x4001d8  
#define IIO_IXCC 0x4001e0  
#define IIO_IXTCC IIO_IXCC
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_IMEM 0x4001e8  
#define IIO_IXTT 0x4001f0  
#define IIO_IECLR 0x4001f8  
#define IIO_IBCN 0x400200  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_IMEM_W0ESD 0x1  
#define IIO_IMEM_B0ESD (1 << 4)  
#define IIO_IMEM_B1ESD (1 << 8)  
#define IIO_IPCA 0x400300  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_NUM_PRTES 8  
#define IIO_PRTE_0 0x400308  
#define IIO_PRTE(_x) (IIO_PRTE_0 + (8 * (_x)))
#define IIO_WIDPRTE(x) IIO_PRTE(((x) - 8))  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_IPDR 0x400388  
#define IIO_ICDR 0x400390  
#define IIO_IFDR 0x400398  
#define IIO_IIAP 0x4003a0  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_IMMR IIO_IIAP
#define IIO_ICMR 0x4003a8  
#define IIO_ICCR 0x4003b0  
#define IIO_ICTO 0x4003b8  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICTP 0x4003c0  
#define IIO_ICMR_PC_VLD_SHFT 36
#define IIO_ICMR_PC_VLD_MASK (0x7fffUL << IIO_ICMR_PC_VLD_SHFT)
#define IIO_ICMR_CRB_VLD_SHFT 20
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICMR_CRB_VLD_MASK (0x7fffUL << IIO_ICMR_CRB_VLD_SHFT)
#define IIO_ICMR_FC_CNT_SHFT 16
#define IIO_ICMR_FC_CNT_MASK (0xf << IIO_ICMR_FC_CNT_SHFT)
#define IIO_ICMR_C_CNT_SHFT 4
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICMR_C_CNT_MASK (0xf << IIO_ICMR_C_CNT_SHFT)
#define IIO_ICMR_P_CNT_SHFT 0
#define IIO_ICMR_P_CNT_MASK (0xf << IIO_ICMR_P_CNT_SHFT)
#define IIO_ICMR_PRECISE (1UL << 52)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICMR_CLR_RPPD (1UL << 13)
#define IIO_ICMR_CLR_RQPD (1UL << 12)
#define IIO_IPDR_PND (1 << 4)
#define IIO_ICDR_PND (1 << 4)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICCR_PENDING (0x10000)
#define IIO_ICCR_CMD_MASK (0xFF)
#define IIO_ICCR_CMD_SHFT (7)
#define IIO_ICCR_CMD_NOP (0x0)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICCR_CMD_WAKE (0x100)  
#define IIO_ICCR_CMD_TIMEOUT (0x200)  
#define IIO_ICCR_CMD_EJECT (0x400)  
#define IIO_ICCR_CMD_FLUSH (0x800)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_NUM_CRBS 15  
#define IIO_NUM_NORMAL_CRBS 12  
#define IIO_NUM_PC_CRBS 4  
#define IIO_ICRB_OFFSET 8
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICRB_0 0x400400
#define IIO_ICRB_A(_x) (IIO_ICRB_0 + (4 * IIO_ICRB_OFFSET * (_x)))
#define IIO_ICRB_B(_x) (IIO_ICRB_A(_x) + 1*IIO_ICRB_OFFSET)
#define IIO_ICRB_C(_x) (IIO_ICRB_A(_x) + 2*IIO_ICRB_OFFSET)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICRB_D(_x) (IIO_ICRB_A(_x) + 3*IIO_ICRB_OFFSET)
#ifndef __ASSEMBLY__
typedef union icrba_u {
 u64 reg_value;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct {
 u64 resvd: 6,
 stall_bte0: 1,
 stall_bte1: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 error: 1,
 ecode: 3,
 lnetuce: 1,
 mark: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 xerr: 1,
 sidn: 4,
 tnum: 5,
 addr: 38,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 valid: 1,
 iow: 1;
 } icrba_fields_s;
} icrba_t;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef union h1_icrba_u {
 u64 reg_value;
 struct {
 u64 resvd: 6,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unused: 1,
 error: 1,
 ecode: 4,
 lnetuce: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mark: 1,
 xerr: 1,
 sidn: 4,
 tnum: 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 addr: 38,
 valid: 1,
 iow: 1;
 } h1_icrba_fields_s;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} h1_icrba_t;
#define ICRBN_A_CERR_SHFT 54
#define ICRBN_A_ERR_MASK 0x3ff
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICRB_ADDR_SHFT 2  
#define IIO_ICRB_ECODE_DERR 0  
#define IIO_ICRB_ECODE_PERR 1  
#define IIO_ICRB_ECODE_WERR 2  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICRB_ECODE_AERR 3  
#define IIO_ICRB_ECODE_PWERR 4  
#define IIO_ICRB_ECODE_PRERR 5  
#define IIO_ICRB_ECODE_TOUT 6  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICRB_ECODE_XTERR 7  
#ifndef __ASSEMBLY__
typedef union icrbb_u {
 u64 reg_value;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct {
 u64 rsvd1: 5,
 btenum: 1,
 cohtrans: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 xtsize: 2,
 srcnode: 9,
 srcinit: 2,
 useold: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 imsgtype: 2,
 imsg: 8,
 initator: 3,
 reqtype: 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 rsvd2: 7,
 ackcnt: 11,
 resp: 1,
 ack: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 hold: 1,
 wb_pend:1,
 intvn: 1,
 stall_ib: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 stall_intr: 1;
 } icrbb_field_s;
} icrbb_t;
typedef union h1_icrbb_u {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u64 reg_value;
 struct {
 u64 rsvd1: 5,
 btenum: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 cohtrans: 1,
 xtsize: 2,
 srcnode: 9,
 srcinit: 2,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 useold: 1,
 imsgtype: 2,
 imsg: 8,
 initator: 3,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 rsvd2: 1,
 pcache: 1,
 reqtype: 5,
 stl_ib: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 stl_intr: 1,
 stl_bte0: 1,
 stl_bte1: 1,
 intrvn: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ackcnt: 11,
 resp: 1,
 ack: 1,
 hold: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 wb_pend:1,
 sleep: 1,
 pnd_reply: 1,
 pnd_req: 1;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 } h1_icrbb_field_s;
} h1_icrbb_t;
#define b_imsgtype icrbb_field_s.imsgtype
#define b_btenum icrbb_field_s.btenum
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define b_cohtrans icrbb_field_s.cohtrans
#define b_xtsize icrbb_field_s.xtsize
#define b_srcnode icrbb_field_s.srcnode
#define b_srcinit icrbb_field_s.srcinit
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define b_imsgtype icrbb_field_s.imsgtype
#define b_imsg icrbb_field_s.imsg
#define b_initiator icrbb_field_s.initiator
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICRB_XTSIZE_DW 0  
#define IIO_ICRB_XTSIZE_32 1  
#define IIO_ICRB_XTSIZE_128 2  
#define IIO_ICRB_PROC0 0  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICRB_PROC1 1  
#define IIO_ICRB_GB_REQ 2  
#define IIO_ICRB_IO_REQ 3  
#define IIO_ICRB_IMSGT_XTALK 0  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICRB_IMSGT_BTE 1  
#define IIO_ICRB_IMSGT_SN0NET 2  
#define IIO_ICRB_IMSGT_CRB 3  
#define IIO_ICRB_INIT_XTALK 0  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICRB_INIT_BTE0 0x1  
#define IIO_ICRB_INIT_SN0NET 0x2  
#define IIO_ICRB_INIT_CRB 0x3  
#define IIO_ICRB_INIT_BTE1 0x5  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICRB_REQ_DWRD 0  
#define IIO_ICRB_REQ_QCLRD 1  
#define IIO_ICRB_REQ_BLKRD 2  
#define IIO_ICRB_REQ_RSHU 6  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICRB_REQ_REXU 7  
#define IIO_ICRB_REQ_RDEX 8  
#define IIO_ICRB_REQ_WINC 9  
#define IIO_ICRB_REQ_BWINV 10  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICRB_REQ_PIORD 11  
#define IIO_ICRB_REQ_PIOWR 12  
#define IIO_ICRB_REQ_PRDM 13  
#define IIO_ICRB_REQ_PWRM 14  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ICRB_REQ_PTPWR 15  
#define IIO_ICRB_REQ_WB 16  
#define IIO_ICRB_REQ_DEX 17  
#ifndef __ASSEMBLY__
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef union icrbc_s {
 u64 reg_value;
 struct {
 u64 rsvd: 6,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 sleep: 1,
 pricnt: 4,
 pripsc: 4,
 bteop: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 push_be: 34,
 suppl: 11,
 barrop: 1,
 doresp: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 gbr: 1;
 } icrbc_field_s;
} icrbc_t;
#define c_pricnt icrbc_field_s.pricnt
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define c_pripsc icrbc_field_s.pripsc
#define c_bteop icrbc_field_s.bteop
#define c_bteaddr icrbc_field_s.push_be  
#define c_benable icrbc_field_s.push_be  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define c_suppl icrbc_field_s.suppl
#define c_barrop icrbc_field_s.barrop
#define c_doresp icrbc_field_s.doresp
#define c_gbr icrbc_field_s.gbr
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#ifndef __ASSEMBLY__
typedef union icrbd_s {
 u64 reg_value;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct {
 u64 rsvd: 38,
 toutvld: 1,
 ctxtvld: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 rsvd2: 1,
 context: 15,
 timeout: 8;
 } icrbd_field_s;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} icrbd_t;
#define icrbd_toutvld icrbd_field_s.toutvld
#define icrbd_ctxtvld icrbd_field_s.ctxtvld
#define icrbd_context icrbd_field_s.context
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef union hubii_ifdr_u {
 u64 hi_ifdr_value;
 struct {
 u64 ifdr_rsvd: 49,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ifdr_maxrp: 7,
 ifdr_rsvd1: 1,
 ifdr_maxrq: 7;
 } hi_ifdr_fields;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} hubii_ifdr_t;
#endif
#define IIO_IBLS_0 0x410000  
#define IIO_IBSA_0 0x410008  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_IBDA_0 0x410010  
#define IIO_IBCT_0 0x410018  
#define IIO_IBNA_0 0x410020  
#define IIO_IBNR_0 IIO_IBNA_0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_IBIA_0 0x410028  
#define IIO_IBLS_1 0x420000  
#define IIO_IBSA_1 0x420008  
#define IIO_IBDA_1 0x420010  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_IBCT_1 0x420018  
#define IIO_IBNA_1 0x420020  
#define IIO_IBNR_1 IIO_IBNA_1
#define IIO_IBIA_1 0x420028  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_IPCR 0x430000  
#define IIO_IPPR 0x430008  
#define IECLR_BTE1 (1 << 18)  
#define IECLR_BTE0 (1 << 17)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IECLR_CRAZY (1 << 16)  
#define IECLR_PRB_F (1 << 15)  
#define IECLR_PRB_E (1 << 14)  
#define IECLR_PRB_D (1 << 13)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IECLR_PRB_C (1 << 12)  
#define IECLR_PRB_B (1 << 11)  
#define IECLR_PRB_A (1 << 10)  
#define IECLR_PRB_9 (1 << 9)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IECLR_PRB_8 (1 << 8)  
#define IECLR_PRB_0 (1 << 0)  
#ifndef __ASSEMBLY__
typedef union iprte_a {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u64 entry;
 struct {
 u64 rsvd1 : 7,
 valid : 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 rsvd2 : 1,
 srcnode : 9,
 initiator : 2,
 rsvd3 : 3,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 addr : 38,
 rsvd4 : 3;
 } iprte_fields;
} iprte_a_t;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define iprte_valid iprte_fields.valid
#define iprte_timeout iprte_fields.timeout
#define iprte_srcnode iprte_fields.srcnode
#define iprte_init iprte_fields.initiator
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define iprte_addr iprte_fields.addr
#endif
#define IPRTE_ADDRSHFT 3
#ifndef __ASSEMBLY__
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef union iprb_u {
 u64 reg_value;
 struct {
 u64 rsvd1: 15,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 error: 1,
 ovflow: 5,
 fire_and_forget: 1,
 mode: 2,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 rsvd2: 2,
 bnakctr: 14,
 rsvd3: 2,
 anakctr: 14,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 xtalkctr: 8;
 } iprb_fields_s;
} iprb_t;
#define iprb_regval reg_value
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define iprb_error iprb_fields_s.error
#define iprb_ovflow iprb_fields_s.ovflow
#define iprb_ff iprb_fields_s.fire_and_forget
#define iprb_mode iprb_fields_s.mode
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define iprb_bnakctr iprb_fields_s.bnakctr
#define iprb_anakctr iprb_fields_s.anakctr
#define iprb_xtalkctr iprb_fields_s.xtalkctr
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IPRB_MODE_NORMAL (0)
#define IPRB_MODE_COLLECT_A (1)  
#define IPRB_MODE_SERVICE_A (2)  
#define IPRB_MODE_SERVICE_B (3)  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifndef __ASSEMBLY__
typedef union icrbp_a {
 u64 ip_reg;
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u64 error: 1,
 ln_uce: 1,
 ln_ae: 1,
 ln_werr:1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ln_aerr:1,
 ln_perr:1,
 timeout:1,
 l_bdpkt:1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 c_bdpkt:1,
 c_err: 1,
 rsvd1: 12,
 valid: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 sidn: 4,
 tnum: 5,
 bo: 1,
 resprqd:1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 gbr: 1,
 size: 2,
 excl: 4,
 stall: 3,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 intvn: 1,
 resp: 1,
 ack: 1,
 hold: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 wb: 1,
 ack_cnt:11,
 tscaler:4;
 } ip_fmt;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} icrbp_a_t;
#endif
#define ICRBP_A_CERR_SHFT 54
#define ICRBP_A_ERR_MASK 0x3ff
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifndef __ASSEMBLY__
typedef union hubii_idsr {
 u64 iin_reg;
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 u64 rsvd1 : 35,
 isent : 1,
 rsvd2 : 3,
 ienable: 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 rsvd : 7,
 node : 9,
 rsvd4 : 1,
 level : 7;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 } iin_fmt;
} hubii_idsr_t;
#endif
#define IBLS_BUSY (0x1 << 20)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IBLS_ERROR_SHFT 16
#define IBLS_ERROR (0x1 << IBLS_ERROR_SHFT)
#define IBLS_LENGTH_MASK 0xffff
#define IBCT_POISON (0x1 << 8)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IBCT_NOTIFY (0x1 << 4)
#define IBCT_ZFIL_MODE (0x1 << 0)
#define IBIA_LEVEL_SHFT 16
#define IBIA_LEVEL_MASK (0x7f << IBIA_LEVEL_SHFT)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IBIA_NODE_ID_SHFT 0
#define IBIA_NODE_ID_MASK (0x1ff)
#define HUB_NUM_WIDGET 9
#define HUB_WIDGET_ID_MIN 0x8
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define HUB_WIDGET_ID_MAX 0xf
#define HUB_WIDGET_PART_NUM 0xc101
#define MAX_HUBS_PER_XBOW 2
#define IIO_WCR_WID_GET(nasid) (REMOTE_HUB_L(nasid, III_WCR) & 0xf)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_WST_ERROR_MASK (UINT64_CAST 1 << 32)  
#define HUBII_XBOW_CREDIT 3
#define HUBII_XBOW_REV2_CREDIT 4
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
