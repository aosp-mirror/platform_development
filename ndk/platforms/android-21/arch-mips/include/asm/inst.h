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
#ifndef _UAPI_ASM_INST_H
#define _UAPI_ASM_INST_H
enum major_op {
 spec_op, bcond_op, j_op, jal_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 beq_op, bne_op, blez_op, bgtz_op,
 addi_op, addiu_op, slti_op, sltiu_op,
 andi_op, ori_op, xori_op, lui_op,
 cop0_op, cop1_op, cop2_op, cop1x_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 beql_op, bnel_op, blezl_op, bgtzl_op,
 daddi_op, daddiu_op, ldl_op, ldr_op,
 spec2_op, jalx_op, mdmx_op, spec3_op,
 lb_op, lh_op, lwl_op, lw_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 lbu_op, lhu_op, lwr_op, lwu_op,
 sb_op, sh_op, swl_op, sw_op,
 sdl_op, sdr_op, swr_op, cache_op,
 ll_op, lwc1_op, lwc2_op, pref_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 lld_op, ldc1_op, ldc2_op, ld_op,
 sc_op, swc1_op, swc2_op, major_3b_op,
 scd_op, sdc1_op, sdc2_op, sd_op
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum spec_op {
 sll_op, movc_op, srl_op, sra_op,
 sllv_op, pmon_op, srlv_op, srav_op,
 jr_op, jalr_op, movz_op, movn_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 syscall_op, break_op, spim_op, sync_op,
 mfhi_op, mthi_op, mflo_op, mtlo_op,
 dsllv_op, spec2_unused_op, dsrlv_op, dsrav_op,
 mult_op, multu_op, div_op, divu_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 dmult_op, dmultu_op, ddiv_op, ddivu_op,
 add_op, addu_op, sub_op, subu_op,
 and_op, or_op, xor_op, nor_op,
 spec3_unused_op, spec4_unused_op, slt_op, sltu_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 dadd_op, daddu_op, dsub_op, dsubu_op,
 tge_op, tgeu_op, tlt_op, tltu_op,
 teq_op, spec5_unused_op, tne_op, spec6_unused_op,
 dsll_op, spec7_unused_op, dsrl_op, dsra_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 dsll32_op, spec8_unused_op, dsrl32_op, dsra32_op
};
enum spec2_op {
 madd_op, maddu_op, mul_op, spec2_3_unused_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 msub_op, msubu_op,
 clz_op = 0x20, clo_op,
 dclz_op = 0x24, dclo_op,
 sdbpp_op = 0x3f
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
enum spec3_op {
 ext_op, dextm_op, dextu_op, dext_op,
 ins_op, dinsm_op, dinsu_op, dins_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 lx_op = 0x0a,
 bshfl_op = 0x20,
 dbshfl_op = 0x24,
 rdhwr_op = 0x3b
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
enum rt_op {
 bltz_op, bgez_op, bltzl_op, bgezl_op,
 spimi_op, unused_rt_op_0x05, unused_rt_op_0x06, unused_rt_op_0x07,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 tgei_op, tgeiu_op, tlti_op, tltiu_op,
 teqi_op, unused_0x0d_rt_op, tnei_op, unused_0x0f_rt_op,
 bltzal_op, bgezal_op, bltzall_op, bgezall_op,
 rt_op_0x14, rt_op_0x15, rt_op_0x16, rt_op_0x17,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 rt_op_0x18, rt_op_0x19, rt_op_0x1a, rt_op_0x1b,
 bposge32_op, rt_op_0x1d, rt_op_0x1e, rt_op_0x1f
};
enum cop_op {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mfc_op = 0x00, dmfc_op = 0x01,
 cfc_op = 0x02, mfhc_op = 0x03,
 mtc_op = 0x04, dmtc_op = 0x05,
 ctc_op = 0x06, mthc_op = 0x07,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 bc_op = 0x08, cop_op = 0x10,
 copm_op = 0x18
};
enum bcop_op {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 bcf_op, bct_op, bcfl_op, bctl_op
};
enum cop0_coi_func {
 tlbr_op = 0x01, tlbwi_op = 0x02,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 tlbwr_op = 0x06, tlbp_op = 0x08,
 rfe_op = 0x10, eret_op = 0x18
};
enum cop0_com_func {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 tlbr1_op = 0x01, tlbw_op = 0x02,
 tlbp1_op = 0x08, dctr_op = 0x09,
 dctw_op = 0x0a
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum cop1_fmt {
 s_fmt, d_fmt, e_fmt, q_fmt,
 w_fmt, l_fmt
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum cop1_sdw_func {
 fadd_op = 0x00, fsub_op = 0x01,
 fmul_op = 0x02, fdiv_op = 0x03,
 fsqrt_op = 0x04, fabs_op = 0x05,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 fmov_op = 0x06, fneg_op = 0x07,
 froundl_op = 0x08, ftruncl_op = 0x09,
 fceill_op = 0x0a, ffloorl_op = 0x0b,
 fround_op = 0x0c, ftrunc_op = 0x0d,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 fceil_op = 0x0e, ffloor_op = 0x0f,
 fmovc_op = 0x11, fmovz_op = 0x12,
 fmovn_op = 0x13, frecip_op = 0x15,
 frsqrt_op = 0x16, fcvts_op = 0x20,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 fcvtd_op = 0x21, fcvte_op = 0x22,
 fcvtw_op = 0x24, fcvtl_op = 0x25,
 fcmp_op = 0x30
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum cop1x_func {
 lwxc1_op = 0x00, ldxc1_op = 0x01,
 swxc1_op = 0x08, sdxc1_op = 0x09,
 pfetch_op = 0x0f, madd_s_op = 0x20,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 madd_d_op = 0x21, madd_e_op = 0x22,
 msub_s_op = 0x28, msub_d_op = 0x29,
 msub_e_op = 0x2a, nmadd_s_op = 0x30,
 nmadd_d_op = 0x31, nmadd_e_op = 0x32,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 nmsub_s_op = 0x38, nmsub_d_op = 0x39,
 nmsub_e_op = 0x3a
};
enum mad_func {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 madd_fp_op = 0x08, msub_fp_op = 0x0a,
 nmadd_fp_op = 0x0c, nmsub_fp_op = 0x0e
};
enum lx_func {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 lwx_op = 0x00,
 lhx_op = 0x04,
 lbux_op = 0x06,
 ldx_op = 0x08,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 lwux_op = 0x10,
 lhux_op = 0x14,
 lbx_op = 0x16,
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum mm_major_op {
 mm_pool32a_op, mm_pool16a_op, mm_lbu16_op, mm_move16_op,
 mm_addi32_op, mm_lbu32_op, mm_sb32_op, mm_lb32_op,
 mm_pool32b_op, mm_pool16b_op, mm_lhu16_op, mm_andi16_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_addiu32_op, mm_lhu32_op, mm_sh32_op, mm_lh32_op,
 mm_pool32i_op, mm_pool16c_op, mm_lwsp16_op, mm_pool16d_op,
 mm_ori32_op, mm_pool32f_op, mm_reserved1_op, mm_reserved2_op,
 mm_pool32c_op, mm_lwgp16_op, mm_lw16_op, mm_pool16e_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_xori32_op, mm_jals32_op, mm_addiupc_op, mm_reserved3_op,
 mm_reserved4_op, mm_pool16f_op, mm_sb16_op, mm_beqz16_op,
 mm_slti32_op, mm_beq32_op, mm_swc132_op, mm_lwc132_op,
 mm_reserved5_op, mm_reserved6_op, mm_sh16_op, mm_bnez16_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_sltiu32_op, mm_bne32_op, mm_sdc132_op, mm_ldc132_op,
 mm_reserved7_op, mm_reserved8_op, mm_swsp16_op, mm_b16_op,
 mm_andi32_op, mm_j32_op, mm_sd32_op, mm_ld32_op,
 mm_reserved11_op, mm_reserved12_op, mm_sw16_op, mm_li16_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_jalx32_op, mm_jal32_op, mm_sw32_op, mm_lw32_op,
};
enum mm_32i_minor_op {
 mm_bltz_op, mm_bltzal_op, mm_bgez_op, mm_bgezal_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_blez_op, mm_bnezc_op, mm_bgtz_op, mm_beqzc_op,
 mm_tlti_op, mm_tgei_op, mm_tltiu_op, mm_tgeiu_op,
 mm_tnei_op, mm_lui_op, mm_teqi_op, mm_reserved13_op,
 mm_synci_op, mm_bltzals_op, mm_reserved14_op, mm_bgezals_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_bc2f_op, mm_bc2t_op, mm_reserved15_op, mm_reserved16_op,
 mm_reserved17_op, mm_reserved18_op, mm_bposge64_op, mm_bposge32_op,
 mm_bc1f_op, mm_bc1t_op, mm_reserved19_op, mm_reserved20_op,
 mm_bc1any2f_op, mm_bc1any2t_op, mm_bc1any4f_op, mm_bc1any4t_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
enum mm_32a_minor_op {
 mm_sll32_op = 0x000,
 mm_ins_op = 0x00c,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_ext_op = 0x02c,
 mm_pool32axf_op = 0x03c,
 mm_srl32_op = 0x040,
 mm_sra_op = 0x080,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_rotr_op = 0x0c0,
 mm_lwxs_op = 0x118,
 mm_addu32_op = 0x150,
 mm_subu32_op = 0x1d0,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_and_op = 0x250,
 mm_or32_op = 0x290,
 mm_xor32_op = 0x310,
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum mm_32b_func {
 mm_lwc2_func = 0x0,
 mm_lwp_func = 0x1,
 mm_ldc2_func = 0x2,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_ldp_func = 0x4,
 mm_lwm32_func = 0x5,
 mm_cache_func = 0x6,
 mm_ldm_func = 0x7,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_swc2_func = 0x8,
 mm_swp_func = 0x9,
 mm_sdc2_func = 0xa,
 mm_sdp_func = 0xc,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_swm32_func = 0xd,
 mm_sdm_func = 0xf,
};
enum mm_32c_func {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_pref_func = 0x2,
 mm_ll_func = 0x3,
 mm_swr_func = 0x9,
 mm_sc_func = 0xb,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_lwu_func = 0xe,
};
enum mm_32axf_minor_op {
 mm_mfc0_op = 0x003,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_mtc0_op = 0x00b,
 mm_tlbp_op = 0x00d,
 mm_jalr_op = 0x03c,
 mm_tlbr_op = 0x04d,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_jalrhb_op = 0x07c,
 mm_tlbwi_op = 0x08d,
 mm_tlbwr_op = 0x0cd,
 mm_jalrs_op = 0x13c,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_jalrshb_op = 0x17c,
 mm_syscall_op = 0x22d,
 mm_eret_op = 0x3cd,
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum mm_32f_minor_op {
 mm_32f_00_op = 0x00,
 mm_32f_01_op = 0x01,
 mm_32f_02_op = 0x02,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_32f_10_op = 0x08,
 mm_32f_11_op = 0x09,
 mm_32f_12_op = 0x0a,
 mm_32f_20_op = 0x10,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_32f_30_op = 0x18,
 mm_32f_40_op = 0x20,
 mm_32f_41_op = 0x21,
 mm_32f_42_op = 0x22,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_32f_50_op = 0x28,
 mm_32f_51_op = 0x29,
 mm_32f_52_op = 0x2a,
 mm_32f_60_op = 0x30,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_32f_70_op = 0x38,
 mm_32f_73_op = 0x3b,
 mm_32f_74_op = 0x3c,
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum mm_32f_10_minor_op {
 mm_lwxc1_op = 0x1,
 mm_swxc1_op,
 mm_ldxc1_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_sdxc1_op,
 mm_luxc1_op,
 mm_suxc1_op,
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum mm_32f_func {
 mm_lwxc1_func = 0x048,
 mm_swxc1_func = 0x088,
 mm_ldxc1_func = 0x0c8,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_sdxc1_func = 0x108,
};
enum mm_32f_40_minor_op {
 mm_fmovf_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_fmovt_op,
};
enum mm_32f_60_minor_op {
 mm_fadd_op,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_fsub_op,
 mm_fmul_op,
 mm_fdiv_op,
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum mm_32f_70_minor_op {
 mm_fmovn_op,
 mm_fmovz_op,
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum mm_32f_73_minor_op {
 mm_fmov0_op = 0x01,
 mm_fcvtl_op = 0x04,
 mm_movf0_op = 0x05,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_frsqrt_op = 0x08,
 mm_ffloorl_op = 0x0c,
 mm_fabs0_op = 0x0d,
 mm_fcvtw_op = 0x24,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_movt0_op = 0x25,
 mm_fsqrt_op = 0x28,
 mm_ffloorw_op = 0x2c,
 mm_fneg0_op = 0x2d,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_cfc1_op = 0x40,
 mm_frecip_op = 0x48,
 mm_fceill_op = 0x4c,
 mm_fcvtd0_op = 0x4d,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_ctc1_op = 0x60,
 mm_fceilw_op = 0x6c,
 mm_fcvts0_op = 0x6d,
 mm_mfc1_op = 0x80,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_fmov1_op = 0x81,
 mm_movf1_op = 0x85,
 mm_ftruncl_op = 0x8c,
 mm_fabs1_op = 0x8d,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_mtc1_op = 0xa0,
 mm_movt1_op = 0xa5,
 mm_ftruncw_op = 0xac,
 mm_fneg1_op = 0xad,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_mfhc1_op = 0xc0,
 mm_froundl_op = 0xcc,
 mm_fcvtd1_op = 0xcd,
 mm_mthc1_op = 0xe0,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_froundw_op = 0xec,
 mm_fcvts1_op = 0xed,
};
enum mm_16c_minor_op {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_lwm16_op = 0x04,
 mm_swm16_op = 0x05,
 mm_jr16_op = 0x0c,
 mm_jrc_op = 0x0d,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 mm_jalr16_op = 0x0e,
 mm_jalrs16_op = 0x0f,
 mm_jraddiusp_op = 0x18,
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum mm_16d_minor_op {
 mm_addius5_func,
 mm_addiusp_func,
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum MIPS16e_ops {
 MIPS16e_jal_op = 003,
 MIPS16e_ld_op = 007,
 MIPS16e_i8_op = 014,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 MIPS16e_sd_op = 017,
 MIPS16e_lb_op = 020,
 MIPS16e_lh_op = 021,
 MIPS16e_lwsp_op = 022,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 MIPS16e_lw_op = 023,
 MIPS16e_lbu_op = 024,
 MIPS16e_lhu_op = 025,
 MIPS16e_lwpc_op = 026,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 MIPS16e_lwu_op = 027,
 MIPS16e_sb_op = 030,
 MIPS16e_sh_op = 031,
 MIPS16e_swsp_op = 032,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 MIPS16e_sw_op = 033,
 MIPS16e_rr_op = 035,
 MIPS16e_extend_op = 036,
 MIPS16e_i64_op = 037,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
enum MIPS16e_i64_func {
 MIPS16e_ldsp_func,
 MIPS16e_sdsp_func,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 MIPS16e_sdrasp_func,
 MIPS16e_dadjsp_func,
 MIPS16e_ldpc_func,
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum MIPS16e_rr_func {
 MIPS16e_jr_func,
};
enum MIPS6e_i8_func {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 MIPS16e_swrasp_func = 02,
};
#define MM_NOP16 0x0c00
#define BITFIELD_FIELD(field, more)   more   field;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct j_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int target : 26,
 ;))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct i_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int rs : 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int rt : 5,
 BITFIELD_FIELD(signed int simmediate : 16,
 ;))))
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct u_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int rs : 5,
 BITFIELD_FIELD(unsigned int rt : 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int uimmediate : 16,
 ;))))
};
struct c_format {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int rs : 5,
 BITFIELD_FIELD(unsigned int c_op : 3,
 BITFIELD_FIELD(unsigned int cache : 2,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int simmediate : 16,
 ;)))))
};
struct r_format {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int rs : 5,
 BITFIELD_FIELD(unsigned int rt : 5,
 BITFIELD_FIELD(unsigned int rd : 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int re : 5,
 BITFIELD_FIELD(unsigned int func : 6,
 ;))))))
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct p_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int rs : 5,
 BITFIELD_FIELD(unsigned int rt : 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int rd : 5,
 BITFIELD_FIELD(unsigned int re : 5,
 BITFIELD_FIELD(unsigned int func : 6,
 ;))))))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct f_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int : 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int fmt : 4,
 BITFIELD_FIELD(unsigned int rt : 5,
 BITFIELD_FIELD(unsigned int rd : 5,
 BITFIELD_FIELD(unsigned int re : 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int func : 6,
 ;)))))))
};
struct ma_format {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int fr : 5,
 BITFIELD_FIELD(unsigned int ft : 5,
 BITFIELD_FIELD(unsigned int fs : 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int fd : 5,
 BITFIELD_FIELD(unsigned int func : 4,
 BITFIELD_FIELD(unsigned int fmt : 2,
 ;)))))))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct b_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int code : 20,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int func : 6,
 ;)))
};
struct ps_format {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int rs : 5,
 BITFIELD_FIELD(unsigned int ft : 5,
 BITFIELD_FIELD(unsigned int fs : 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int fd : 5,
 BITFIELD_FIELD(unsigned int func : 6,
 ;))))))
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct v_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int sel : 4,
 BITFIELD_FIELD(unsigned int fmt : 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int vt : 5,
 BITFIELD_FIELD(unsigned int vs : 5,
 BITFIELD_FIELD(unsigned int vd : 5,
 BITFIELD_FIELD(unsigned int func : 6,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ;)))))))
};
struct fb_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int bc : 5,
 BITFIELD_FIELD(unsigned int cc : 3,
 BITFIELD_FIELD(unsigned int flag : 2,
 BITFIELD_FIELD(signed int simmediate : 16,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ;)))))
};
struct fp0_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int fmt : 5,
 BITFIELD_FIELD(unsigned int ft : 5,
 BITFIELD_FIELD(unsigned int fs : 5,
 BITFIELD_FIELD(unsigned int fd : 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int func : 6,
 ;))))))
};
struct mm_fp0_format {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int ft : 5,
 BITFIELD_FIELD(unsigned int fs : 5,
 BITFIELD_FIELD(unsigned int fd : 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int fmt : 3,
 BITFIELD_FIELD(unsigned int op : 2,
 BITFIELD_FIELD(unsigned int func : 6,
 ;)))))))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct fp1_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int op : 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int rt : 5,
 BITFIELD_FIELD(unsigned int fs : 5,
 BITFIELD_FIELD(unsigned int fd : 5,
 BITFIELD_FIELD(unsigned int func : 6,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ;))))))
};
struct mm_fp1_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int rt : 5,
 BITFIELD_FIELD(unsigned int fs : 5,
 BITFIELD_FIELD(unsigned int fmt : 2,
 BITFIELD_FIELD(unsigned int op : 8,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int func : 6,
 ;))))))
};
struct mm_fp2_format {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int fd : 5,
 BITFIELD_FIELD(unsigned int fs : 5,
 BITFIELD_FIELD(unsigned int cc : 3,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int zero : 2,
 BITFIELD_FIELD(unsigned int fmt : 2,
 BITFIELD_FIELD(unsigned int op : 3,
 BITFIELD_FIELD(unsigned int func : 6,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ;))))))))
};
struct mm_fp3_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int rt : 5,
 BITFIELD_FIELD(unsigned int fs : 5,
 BITFIELD_FIELD(unsigned int fmt : 3,
 BITFIELD_FIELD(unsigned int op : 7,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int func : 6,
 ;))))))
};
struct mm_fp4_format {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int rt : 5,
 BITFIELD_FIELD(unsigned int fs : 5,
 BITFIELD_FIELD(unsigned int cc : 3,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int fmt : 3,
 BITFIELD_FIELD(unsigned int cond : 4,
 BITFIELD_FIELD(unsigned int func : 6,
 ;)))))))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct mm_fp5_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int index : 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int base : 5,
 BITFIELD_FIELD(unsigned int fd : 5,
 BITFIELD_FIELD(unsigned int op : 5,
 BITFIELD_FIELD(unsigned int func : 6,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ;))))))
};
struct fp6_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int fr : 5,
 BITFIELD_FIELD(unsigned int ft : 5,
 BITFIELD_FIELD(unsigned int fs : 5,
 BITFIELD_FIELD(unsigned int fd : 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int func : 6,
 ;))))))
};
struct mm_fp6_format {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int ft : 5,
 BITFIELD_FIELD(unsigned int fs : 5,
 BITFIELD_FIELD(unsigned int fd : 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int fr : 5,
 BITFIELD_FIELD(unsigned int func : 6,
 ;))))))
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct mm_i_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int rt : 5,
 BITFIELD_FIELD(unsigned int rs : 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(signed int simmediate : 16,
 ;))))
};
struct mm_m_format {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int rd : 5,
 BITFIELD_FIELD(unsigned int base : 5,
 BITFIELD_FIELD(unsigned int func : 4,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(signed int simmediate : 12,
 ;)))))
};
struct mm_x_format {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int index : 5,
 BITFIELD_FIELD(unsigned int base : 5,
 BITFIELD_FIELD(unsigned int rd : 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int func : 11,
 ;)))))
};
struct mm_b0_format {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(signed int simmediate : 10,
 BITFIELD_FIELD(unsigned int : 16,
 ;)))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct mm_b1_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int rs : 3,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(signed int simmediate : 7,
 BITFIELD_FIELD(unsigned int : 16,
 ;))))
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct mm16_m_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int func : 4,
 BITFIELD_FIELD(unsigned int rlist : 2,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int imm : 4,
 BITFIELD_FIELD(unsigned int : 16,
 ;)))))
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct mm16_rb_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int rt : 3,
 BITFIELD_FIELD(unsigned int base : 3,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(signed int simmediate : 4,
 BITFIELD_FIELD(unsigned int : 16,
 ;)))))
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct mm16_r3_format {
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int rt : 3,
 BITFIELD_FIELD(signed int simmediate : 7,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int : 16,
 ;))))
};
struct mm16_r5_format {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int opcode : 6,
 BITFIELD_FIELD(unsigned int rt : 5,
 BITFIELD_FIELD(signed int simmediate : 5,
 BITFIELD_FIELD(unsigned int : 16,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ;))))
};
struct m16e_rr {
 BITFIELD_FIELD(unsigned int opcode : 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int rx : 3,
 BITFIELD_FIELD(unsigned int nd : 1,
 BITFIELD_FIELD(unsigned int l : 1,
 BITFIELD_FIELD(unsigned int ra : 1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int func : 5,
 ;))))))
};
struct m16e_jal {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int opcode : 5,
 BITFIELD_FIELD(unsigned int x : 1,
 BITFIELD_FIELD(unsigned int imm20_16 : 5,
 BITFIELD_FIELD(signed int imm25_21 : 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ;))))
};
struct m16e_i64 {
 BITFIELD_FIELD(unsigned int opcode : 5,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int func : 3,
 BITFIELD_FIELD(unsigned int imm : 8,
 ;)))
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct m16e_ri64 {
 BITFIELD_FIELD(unsigned int opcode : 5,
 BITFIELD_FIELD(unsigned int func : 3,
 BITFIELD_FIELD(unsigned int ry : 3,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int imm : 5,
 ;))))
};
struct m16e_ri {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int opcode : 5,
 BITFIELD_FIELD(unsigned int rx : 3,
 BITFIELD_FIELD(unsigned int imm : 8,
 ;)))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct m16e_rri {
 BITFIELD_FIELD(unsigned int opcode : 5,
 BITFIELD_FIELD(unsigned int rx : 3,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 BITFIELD_FIELD(unsigned int ry : 3,
 BITFIELD_FIELD(unsigned int imm : 5,
 ;))))
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct m16e_i8 {
 BITFIELD_FIELD(unsigned int opcode : 5,
 BITFIELD_FIELD(unsigned int func : 3,
 BITFIELD_FIELD(unsigned int imm : 8,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 ;)))
};
union mips_instruction {
 unsigned int word;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned short halfword[2];
 unsigned char byte[4];
 struct j_format j_format;
 struct i_format i_format;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct u_format u_format;
 struct c_format c_format;
 struct r_format r_format;
 struct p_format p_format;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct f_format f_format;
 struct ma_format ma_format;
 struct b_format b_format;
 struct ps_format ps_format;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct v_format v_format;
 struct fb_format fb_format;
 struct fp0_format fp0_format;
 struct mm_fp0_format mm_fp0_format;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct fp1_format fp1_format;
 struct mm_fp1_format mm_fp1_format;
 struct mm_fp2_format mm_fp2_format;
 struct mm_fp3_format mm_fp3_format;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct mm_fp4_format mm_fp4_format;
 struct mm_fp5_format mm_fp5_format;
 struct fp6_format fp6_format;
 struct mm_fp6_format mm_fp6_format;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct mm_i_format mm_i_format;
 struct mm_m_format mm_m_format;
 struct mm_x_format mm_x_format;
 struct mm_b0_format mm_b0_format;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct mm_b1_format mm_b1_format;
 struct mm16_m_format mm16_m_format ;
 struct mm16_rb_format mm16_rb_format;
 struct mm16_r3_format mm16_r3_format;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct mm16_r5_format mm16_r5_format;
};
union mips16e_instruction {
 unsigned int full : 16;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct m16e_rr rr;
 struct m16e_jal jal;
 struct m16e_i64 i64;
 struct m16e_ri64 ri64;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct m16e_ri ri;
 struct m16e_rri rri;
 struct m16e_i8 i8;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
