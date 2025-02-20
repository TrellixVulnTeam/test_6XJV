/*
 * Copyright (C) 2018 Spreadtrum Communications Inc.
 *
 * This file is dual-licensed: you can use it either under the terms
 * of the GPL or the X11 license, at your option. Note that this dual
 * licensing only applies to this file, and not this project as a
 * whole.
 *
 * updated at 2018-05-08 16:01:53
 *
 */


#ifndef ANLG_PHY_G3_H
#define ANLG_PHY_G3_H

#define CTL_BASE_ANLG_PHY_G3 0x323C0000


#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_CTRL0                 ( CTL_BASE_ANLG_PHY_G3 + 0x0000 )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_CTRL1                 ( CTL_BASE_ANLG_PHY_G3 + 0x0004 )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_PLL_CLK_CTRL               ( CTL_BASE_ANLG_PHY_G3 + 0x0008 )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_PERFOR                ( CTL_BASE_ANLG_PHY_G3 + 0x000C )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_CTRL2                 ( CTL_BASE_ANLG_PHY_G3 + 0x0010 )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_RESERVED              ( CTL_BASE_ANLG_PHY_G3 + 0x0014 )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_BIST_CTRL             ( CTL_BASE_ANLG_PHY_G3 + 0x0018 )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_ANA_BB_PWR_CTRL            ( CTL_BASE_ANLG_PHY_G3 + 0x001C )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_ANA_BB_26M_BUF             ( CTL_BASE_ANLG_PHY_G3 + 0x0020 )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_ANA_BB_EN_CTRL             ( CTL_BASE_ANLG_PHY_G3 + 0x0024 )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_ANALOG_TEST                ( CTL_BASE_ANLG_PHY_G3 + 0x0028 )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_AAPC_CTRL1                 ( CTL_BASE_ANLG_PHY_G3 + 0x002C )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_AAPC_CTRL2                 ( CTL_BASE_ANLG_PHY_G3 + 0x0030 )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_ANA_BB_RSVD                ( CTL_BASE_ANLG_PHY_G3 + 0x0034 )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_RTC100M_CTRL               ( CTL_BASE_ANLG_PHY_G3 + 0x0038 )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_TEST_CLK_CTRL              ( CTL_BASE_ANLG_PHY_G3 + 0x003C )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_BB_TOP_TEST_0              ( CTL_BASE_ANLG_PHY_G3 + 0x0040 )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_BB_TOP_TEST_1              ( CTL_BASE_ANLG_PHY_G3 + 0x0044 )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_ISPPLL_CTRL0               ( CTL_BASE_ANLG_PHY_G3 + 0x0048 )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_REG_SEL_CFG_0              ( CTL_BASE_ANLG_PHY_G3 + 0x004C )
#define REG_ANLG_PHY_G3_ANALOG_BB_TOP_REG_SEL_CFG_1              ( CTL_BASE_ANLG_PHY_G3 + 0x0050 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CTRL0                 ( CTL_BASE_ANLG_PHY_G3 + 0x0054 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CTRL1                 ( CTL_BASE_ANLG_PHY_G3 + 0x0058 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CTRL2                 ( CTL_BASE_ANLG_PHY_G3 + 0x005C )
#define REG_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CTRL3                 ( CTL_BASE_ANLG_PHY_G3 + 0x0060 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CTRL4                 ( CTL_BASE_ANLG_PHY_G3 + 0x0064 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CTRL5                 ( CTL_BASE_ANLG_PHY_G3 + 0x0068 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CTRL6                 ( CTL_BASE_ANLG_PHY_G3 + 0x006C )
#define REG_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CTRL7                 ( CTL_BASE_ANLG_PHY_G3 + 0x0070 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CTRL8                 ( CTL_BASE_ANLG_PHY_G3 + 0x0074 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL0_ANANKE_BIG_DVFS_INDEX0      ( CTL_BASE_ANLG_PHY_G3 + 0x0078 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL0_ANANKE_BIG_DVFS_INDEX1      ( CTL_BASE_ANLG_PHY_G3 + 0x007C )
#define REG_ANLG_PHY_G3_ANALOG_MPLL0_ANANKE_BIG_DVFS_INDEX2      ( CTL_BASE_ANLG_PHY_G3 + 0x0080 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL0_ANANKE_BIG_DVFS_INDEX3      ( CTL_BASE_ANLG_PHY_G3 + 0x0084 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL0_ANANKE_BIG_DVFS_INDEX4      ( CTL_BASE_ANLG_PHY_G3 + 0x0088 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL0_ANANKE_BIG_DVFS_INDEX5      ( CTL_BASE_ANLG_PHY_G3 + 0x008C )
#define REG_ANLG_PHY_G3_ANALOG_MPLL0_ANANKE_BIG_DVFS_INDEX6      ( CTL_BASE_ANLG_PHY_G3 + 0x0090 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL0_ANANKE_BIG_DVFS_INDEX7      ( CTL_BASE_ANLG_PHY_G3 + 0x0094 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL0_REG_SEL_CFG_0               ( CTL_BASE_ANLG_PHY_G3 + 0x0098 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CTRL0                 ( CTL_BASE_ANLG_PHY_G3 + 0x009C )
#define REG_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CTRL1                 ( CTL_BASE_ANLG_PHY_G3 + 0x00A0 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CTRL2                 ( CTL_BASE_ANLG_PHY_G3 + 0x00A4 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CTRL3                 ( CTL_BASE_ANLG_PHY_G3 + 0x00A8 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CTRL4                 ( CTL_BASE_ANLG_PHY_G3 + 0x00AC )
#define REG_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CTRL5                 ( CTL_BASE_ANLG_PHY_G3 + 0x00B0 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CTRL6                 ( CTL_BASE_ANLG_PHY_G3 + 0x00B4 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CTRL7                 ( CTL_BASE_ANLG_PHY_G3 + 0x00B8 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL2_SCU_DVFS_INDEX0             ( CTL_BASE_ANLG_PHY_G3 + 0x00BC )
#define REG_ANLG_PHY_G3_ANALOG_MPLL2_SCU_DVFS_INDEX1             ( CTL_BASE_ANLG_PHY_G3 + 0x00C0 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL2_SCU_DVFS_INDEX2             ( CTL_BASE_ANLG_PHY_G3 + 0x00C4 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL2_SCU_DVFS_INDEX3             ( CTL_BASE_ANLG_PHY_G3 + 0x00C8 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL2_SCU_DVFS_INDEX4             ( CTL_BASE_ANLG_PHY_G3 + 0x00CC )
#define REG_ANLG_PHY_G3_ANALOG_MPLL2_SCU_DVFS_INDEX5             ( CTL_BASE_ANLG_PHY_G3 + 0x00D0 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL2_SCU_DVFS_INDEX6             ( CTL_BASE_ANLG_PHY_G3 + 0x00D4 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL2_SCU_DVFS_INDEX7             ( CTL_BASE_ANLG_PHY_G3 + 0x00D8 )
#define REG_ANLG_PHY_G3_ANALOG_MPLL2_REG_SEL_CFG_0               ( CTL_BASE_ANLG_PHY_G3 + 0x00DC )

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_CTRL0 */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_LOCK_DONE              BIT(18)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_REF_SEL(x)             (((x) & 0x3) << 16)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_N(x)                   (((x) & 0x7FF) << 5)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_ICP(x)                 (((x) & 0x7) << 2)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_SDM_EN                 BIT(1)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_DIV_S                  BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_CTRL1 */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_NINT(x)                (((x) & 0x7F) << 23)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_KINT(x)                (((x) & 0x7FFFFF))

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_PLL_CLK_CTRL */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_SINEO_RPLL_CLKOUT_EN        BIT(25)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_26M_SEL                BIT(24)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_VSET(x)                (((x) & 0x7) << 21)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_26M_DIV(x)             (((x) & 0x3F) << 15)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_DIV_EN                 BIT(14)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_POSTDIV                BIT(13)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_FREQ_DOUBLE_EN         BIT(12)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_SSC_CTRL(x)            (((x) & 0xFF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_MOD_EN                 BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_CLKOUT_EN              BIT(2)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_RST                    BIT(1)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_PD                     BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_PERFOR */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_CP_EN                  BIT(8)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_LDO_TRIM(x)            (((x) & 0xF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_VCO_TEST_EN            BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_FBDIV_EN               BIT(2)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_CP_OFFSET(x)           (((x) & 0x3))

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_CTRL2 */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_IBIAS(x)               (((x) & 0x3) << 3)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_LPF(x)                 (((x) & 0x7))

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_RESERVED */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_RESERVED(x)            (((x) & 0x1FFFFF))

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_BIST_CTRL */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_BIST_CTRL(x)           (((x) & 0xFF) << 17)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_BIST_EN                BIT(16)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_BIST_CNT(x)            (((x) & 0xFFFF))

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_ANA_BB_PWR_CTRL */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_S_AAPC_PD                   BIT(1)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_M_AAPC_PD                   BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_ANA_BB_26M_BUF */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_REC_26MHZ_0_BUF_PD          BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_ANA_BB_EN_CTRL */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_CLK26MHZ_AUD_EN             BIT(20)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_PROBE_SEL(x)                (((x) & 0x3F) << 14)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_SINDRV_ENA                  BIT(13)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_SINDRV_ENA_SQUARE           BIT(12)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_REC_26MHZ_SR_TRIM(x)        (((x) & 0xF) << 8)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_REC_26MHZ_0_CUR_SEL(x)      (((x) & 0x3) << 6)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_CLK26M_RESERVED(x)          (((x) & 0xF) << 2)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_SINE_DRV_SEL                BIT(1)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_BB_CON_BG                   BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_ANALOG_TEST */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_ANALOG_TESTMUX(x)           (((x) & 0xFFFF))

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_AAPC_CTRL1 */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_AAPC_G0(x)                  (((x) & 0x3) << 24)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_AAPC_G1(x)                  (((x) & 0x3) << 22)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_AAPC_G2(x)                  (((x) & 0x3) << 20)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_S_AAPC_LOW_V_CON            BIT(19)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_S_AAPC_D(x)                 (((x) & 0x3FFF) << 5)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_S_AAPC_BPRES                BIT(4)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_S_APCOUT_SEL(x)             (((x) & 0x3) << 2)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_S_AAPC_RESERVED(x)          (((x) & 0x3))

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_AAPC_CTRL2 */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_M_AAPC_D(x)                 (((x) & 0x3FFF) << 6)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_M_AAPC_LOW_V_CON            BIT(5)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_M_AAPC_BPRES                BIT(4)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_M_APCOUT_SEL(x)             (((x) & 0x3) << 2)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_M_AAPC_RESERVED(x)          (((x) & 0x3))

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_ANA_BB_RSVD */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_ANA_BB_RESERVED(x)          (((x) & 0xFFFF) << 2)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_TO_LVDS_SEL(x)              (((x) & 0x3))

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_RTC100M_CTRL */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RTC100M_RSTB                BIT(22)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RTC100M_EN                  BIT(21)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RTC100M_RESERVED(x)         (((x) & 0xFF) << 13)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RTC100M_LDO_EN              BIT(12)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RTC100M_LDO_BYPASS          BIT(11)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RTC100M_LDO_VOLTAGE_SEL(x)  (((x) & 0x3) << 9)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RTC100M_LDO_BIAS_SEL(x)     (((x) & 0x3) << 7)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RTC100M_RC_C(x)             (((x) & 0x7F))

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_TEST_CLK_CTRL */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_TEST_CLK_EN                 BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_TEST_CLK_OD                 BIT(2)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_TEST_CLK_DIV(x)             (((x) & 0x3))

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_BB_TOP_TEST_0 */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_AAPC_MAX_RANGE(x)           (((x) & 0x3FFF) << 15)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_AAPC_MIN_RANGE(x)           (((x) & 0x3FFF) << 1)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_AAPC_RSTN                   BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_BB_TOP_TEST_1 */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_AAPC_STEP_SEL(x)            (((x) & 0xF) << 6)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_AAPC_RAMP_GEN_SEL           BIT(5)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_PD_SEL_RFTI_OR_PMU     BIT(4)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RPLL_RST_SEL_RFTI_OR_PMU    BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_DBG_SEL_RTC100M_LDO_EN      BIT(2)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_DBG_SEL_RTC100M_LDO_BYPASS  BIT(1)
#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_RCO100M_POWER_MODE_SEL      BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_ISPPLL_CTRL0 */

#define BIT_ANLG_PHY_G3_ANALOG_BB_TOP_BB_BG_RBIAS_MODE            BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RPLL_REF_SEL        BIT(31)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RPLL_N              BIT(30)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RPLL_SDM_EN         BIT(29)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RPLL_DIV_S          BIT(28)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RPLL_NINT           BIT(27)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RPLL_KINT           BIT(26)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RPLL_26M_DIV        BIT(25)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RPLL_DIV_EN         BIT(24)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RPLL_MOD_EN         BIT(23)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RPLL_CLKOUT_EN      BIT(22)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RPLL_RST            BIT(21)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RPLL_PD             BIT(20)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RPLL_IBIAS          BIT(19)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RPLL_LPF            BIT(18)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RPLL_RESERVED       BIT(17)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RPLL_BIST_CTRL      BIT(16)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RPLL_BIST_EN        BIT(15)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_S_AAPC_PD           BIT(14)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_M_AAPC_PD           BIT(13)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_REC_26MHZ_0_BUF_PD  BIT(12)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_CLK26MHZ_AUD_EN     BIT(11)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_SINDRV_ENA          BIT(10)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_SINDRV_ENA_SQUARE   BIT(9)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_AAPC_G0             BIT(8)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_AAPC_G1             BIT(7)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_AAPC_G2             BIT(6)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_S_AAPC_LOW_V_CON    BIT(5)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_S_AAPC_D            BIT(4)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_S_AAPC_BPRES        BIT(3)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_S_APCOUT_SEL        BIT(2)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_S_AAPC_RESERVED     BIT(1)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_M_AAPC_D            BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_BB_TOP_REG_SEL_CFG_1 */

#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_M_AAPC_LOW_V_CON    BIT(6)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_M_AAPC_BPRES        BIT(5)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_M_APCOUT_SEL        BIT(4)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_M_AAPC_RESERVED     BIT(3)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RTC100M_RSTB        BIT(2)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RTC100M_EN          BIT(1)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_BB_TOP_RTC100M_RC_C        BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CTRL0 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_LOCK_DONE              BIT(17)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CLKIN_SEL              BIT(16)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_N(x)                   (((x) & 0x7FF) << 5)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_ICP(x)                 (((x) & 0x7) << 2)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_SDM_EN                 BIT(1)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_DIV_S                  BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CTRL1 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_NINT(x)                (((x) & 0x7F) << 23)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_KINT(x)                (((x) & 0x7FFFFF))

/* REG_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CTRL2 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_DIV32_EN               BIT(14)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_POSTDIV                BIT(13)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_FREQ_DOUBLE_EN         BIT(12)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CCS_CTRL(x)            (((x) & 0xFF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_MOD_EN                 BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CLKOUT_EN              BIT(2)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_RST                    BIT(1)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_PD                     BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CTRL3 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_R2_SEL(x)              (((x) & 0x3) << 22)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_R3_SEL(x)              (((x) & 0x3) << 20)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_C1_SEL(x)              (((x) & 0x3) << 18)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_C2_SEL(x)              (((x) & 0x3) << 16)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_KVCO_SEL(x)            (((x) & 0x3) << 14)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_VCO_TEST_INTSEL(x)     (((x) & 0x7) << 11)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_VCO_TEST_INT           BIT(10)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CP_EN                  BIT(9)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_LDO_TRIM(x)            (((x) & 0xF) << 5)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_VCO_TEST_EN            BIT(4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_FBDIV_EN               BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CP_OFFSET(x)           (((x) & 0x3) << 1)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_VCOBUF_EN              BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CTRL4 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_RESERVED(x)            (((x) & 0xFFFF))

/* REG_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CTRL5 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_BIST_CTRL(x)           (((x) & 0xFF) << 17)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_BIST_EN                BIT(16)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_BIST_CNT(x)            (((x) & 0xFFFF))

/* REG_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CTRL6 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_26MBUFFER_PD           BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CTRL7 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CALI_MODE(x)           (((x) & 0x3) << 17)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CALI_INI(x)            (((x) & 0x1F) << 12)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CALI_TRIG              BIT(11)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_FREQ_DIFF_EN           BIT(10)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CALI_WAITCNT(x)        (((x) & 0x3) << 8)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CALI_POLARITY          BIT(7)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CALI_DONE              BIT(6)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CALI_OUT(x)            (((x) & 0x1F) << 1)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CALI_CPPD              BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CTRL8 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_VCTRLH_SEL(x)          (((x) & 0x7) << 17)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_VCTRLL_SEL(x)          (((x) & 0x7) << 14)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_RG_CLOSELOOP_EN        BIT(13)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_VCO_BANK_SEL(x)        (((x) & 0x1F) << 8)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CALI_VCTRL_HIGH        BIT(7)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_CALI_VCTRL_LOW         BIT(6)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_VCO_BANK_SEL_OFFSET    BIT(5)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_ADJ_MANUAL_PD          BIT(4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_ISO_SW_EN              BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_TEST_CLK_EN                  BIT(2)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_TEST_SEL                     BIT(1)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_26MBUFFER_CLKOUT_EN    BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_MPLL0_ANANKE_BIG_DVFS_INDEX0 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_N_INDEX0(x)            (((x) & 0x7FF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_POSTDIV_INDEX0         BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_ICP_INDEX0(x)          (((x) & 0x7))

/* REG_ANLG_PHY_G3_ANALOG_MPLL0_ANANKE_BIG_DVFS_INDEX1 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_N_INDEX1(x)            (((x) & 0x7FF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_POSTDIV_INDEX1         BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_ICP_INDEX1(x)          (((x) & 0x7))

/* REG_ANLG_PHY_G3_ANALOG_MPLL0_ANANKE_BIG_DVFS_INDEX2 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_N_INDEX2(x)            (((x) & 0x7FF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_POSTDIV_INDEX2         BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_ICP_INDEX2(x)          (((x) & 0x7))

/* REG_ANLG_PHY_G3_ANALOG_MPLL0_ANANKE_BIG_DVFS_INDEX3 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_N_INDEX3(x)            (((x) & 0x7FF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_POSTDIV_INDEX3         BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_ICP_INDEX3(x)          (((x) & 0x7))

/* REG_ANLG_PHY_G3_ANALOG_MPLL0_ANANKE_BIG_DVFS_INDEX4 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_N_INDEX4(x)            (((x) & 0x7FF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_POSTDIV_INDEX4         BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_ICP_INDEX4(x)          (((x) & 0x7))

/* REG_ANLG_PHY_G3_ANALOG_MPLL0_ANANKE_BIG_DVFS_INDEX5 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_N_INDEX5(x)            (((x) & 0x7FF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_POSTDIV_INDEX5         BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_ICP_INDEX5(x)          (((x) & 0x7))

/* REG_ANLG_PHY_G3_ANALOG_MPLL0_ANANKE_BIG_DVFS_INDEX6 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_N_INDEX6(x)            (((x) & 0x7FF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_POSTDIV_INDEX6         BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_ICP_INDEX6(x)          (((x) & 0x7))

/* REG_ANLG_PHY_G3_ANALOG_MPLL0_ANANKE_BIG_DVFS_INDEX7 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_N_INDEX7(x)            (((x) & 0x7FF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_POSTDIV_INDEX7         BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL0_MPLL0_ICP_INDEX7(x)          (((x) & 0x7))

/* REG_ANLG_PHY_G3_ANALOG_MPLL0_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_MPLL0_MPLL0_N              BIT(7)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_MPLL0_MPLL0_ICP            BIT(6)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_MPLL0_MPLL0_DIV32_EN       BIT(5)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_MPLL0_MPLL0_POSTDIV        BIT(4)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_MPLL0_MPLL0_CLKOUT_EN      BIT(3)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_MPLL0_MPLL0_RST            BIT(2)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_MPLL0_MPLL0_PD             BIT(1)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_MPLL0_MPLL0_26MBUFFER_PD   BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CTRL0 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_LOCK_DONE              BIT(16)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_N(x)                   (((x) & 0x7FF) << 5)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_ICP(x)                 (((x) & 0x7) << 2)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_SDM_EN                 BIT(1)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_DIV_S                  BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CTRL1 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_NINT(x)                (((x) & 0x7F) << 23)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_KINT(x)                (((x) & 0x7FFFFF))

/* REG_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CTRL2 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_DIV32_EN               BIT(14)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_POSTDIV                BIT(13)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_FREQ_DOUBLE_EN         BIT(12)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CCS_CTRL(x)            (((x) & 0xFF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_MOD_EN                 BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CLKOUT_EN              BIT(2)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_RST                    BIT(1)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_PD                     BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CTRL3 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_R2_SEL(x)              (((x) & 0x3) << 22)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_R3_SEL(x)              (((x) & 0x3) << 20)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_C1_SEL(x)              (((x) & 0x3) << 18)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_C2_SEL(x)              (((x) & 0x3) << 16)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_KVCO_SEL(x)            (((x) & 0x3) << 14)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_VCO_TEST_INTSEL(x)     (((x) & 0x7) << 11)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_VCO_TEST_INT           BIT(10)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CP_EN                  BIT(9)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_LDO_TRIM(x)            (((x) & 0xF) << 5)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_VCO_TEST_EN            BIT(4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_FBDIV_EN               BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CP_OFFSET(x)           (((x) & 0x3) << 1)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_VCOBUF_EN              BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CTRL4 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_RESERVED(x)            (((x) & 0xFFFF))

/* REG_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CTRL5 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_BIST_EN                BIT(24)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_BIST_CTRL(x)           (((x) & 0xFF) << 16)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_BIST_CNT(x)            (((x) & 0xFFFF))

/* REG_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CTRL6 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CALI_MODE(x)           (((x) & 0x3) << 17)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CALI_INI(x)            (((x) & 0x1F) << 12)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CALI_TRIG              BIT(11)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_FREQ_DIFF_EN           BIT(10)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CALI_WAITCNT(x)        (((x) & 0x3) << 8)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CALI_POLARITY          BIT(7)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CALI_DONE              BIT(6)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CALI_OUT(x)            (((x) & 0x1F) << 1)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CALI_CPPD              BIT(0)

/* REG_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CTRL7 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_VCTRLH_SEL(x)          (((x) & 0x7) << 19)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_VCTRLL_SEL(x)          (((x) & 0x7) << 16)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_RG_CLOSELOOP_EN        BIT(15)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_VCO_BANK_SEL(x)        (((x) & 0x1F) << 10)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CALI_VCTRL_HIGH        BIT(9)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_CALI_VCTRL_LOW         BIT(8)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_VCO_BANK_SEL_OFFSET    BIT(7)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_ADJ_MANUAL_PD          BIT(6)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_ISO_SW_EN              BIT(5)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_TEST_CLK_EN                  BIT(4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_TEST_SEL                     BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL_CLK_JITTER_MON_EN       BIT(2)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL_CLK_JITTER_MON_SEL(x)   (((x) & 0x3))

/* REG_ANLG_PHY_G3_ANALOG_MPLL2_SCU_DVFS_INDEX0 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_N_INDEX0(x)            (((x) & 0x7FF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_POSTDIV_INDEX0         BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_ICP_INDEX0(x)          (((x) & 0x7))

/* REG_ANLG_PHY_G3_ANALOG_MPLL2_SCU_DVFS_INDEX1 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_N_INDEX1(x)            (((x) & 0x7FF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_POSTDIV_INDEX1         BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_ICP_INDEX1(x)          (((x) & 0x7))

/* REG_ANLG_PHY_G3_ANALOG_MPLL2_SCU_DVFS_INDEX2 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_N_INDEX2(x)            (((x) & 0x7FF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_POSTDIV_INDEX2         BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_ICP_INDEX2(x)          (((x) & 0x7))

/* REG_ANLG_PHY_G3_ANALOG_MPLL2_SCU_DVFS_INDEX3 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_N_INDEX3(x)            (((x) & 0x7FF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_POSTDIV_INDEX3         BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_ICP_INDEX3(x)          (((x) & 0x7))

/* REG_ANLG_PHY_G3_ANALOG_MPLL2_SCU_DVFS_INDEX4 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_N_INDEX4(x)            (((x) & 0x7FF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_POSTDIV_INDEX4         BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_ICP_INDEX4(x)          (((x) & 0x7))

/* REG_ANLG_PHY_G3_ANALOG_MPLL2_SCU_DVFS_INDEX5 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_N_INDEX5(x)            (((x) & 0x7FF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_POSTDIV_INDEX5         BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_ICP_INDEX5(x)          (((x) & 0x7))

/* REG_ANLG_PHY_G3_ANALOG_MPLL2_SCU_DVFS_INDEX6 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_N_INDEX6(x)            (((x) & 0x7FF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_POSTDIV_INDEX6         BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_ICP_INDEX6(x)          (((x) & 0x7))

/* REG_ANLG_PHY_G3_ANALOG_MPLL2_SCU_DVFS_INDEX7 */

#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_N_INDEX7(x)            (((x) & 0x7FF) << 4)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_POSTDIV_INDEX7         BIT(3)
#define BIT_ANLG_PHY_G3_ANALOG_MPLL2_MPLL2_ICP_INDEX7(x)          (((x) & 0x7))

/* REG_ANLG_PHY_G3_ANALOG_MPLL2_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_MPLL2_MPLL2_N              BIT(6)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_MPLL2_MPLL2_ICP            BIT(5)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_MPLL2_MPLL2_DIV32_EN       BIT(4)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_MPLL2_MPLL2_POSTDIV        BIT(3)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_MPLL2_MPLL2_CLKOUT_EN      BIT(2)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_MPLL2_MPLL2_RST            BIT(1)
#define BIT_ANLG_PHY_G3_DBG_SEL_ANALOG_MPLL2_MPLL2_PD             BIT(0)


#endif /* ANLG_PHY_G3_H */


