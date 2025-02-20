/*
 * Copyright (C) 2018 Spreadtrum Communications Inc.
 *
 * This file is dual-licensed: you can use it either under the terms
 * of the GPL or the X11 license, at your option. Note that this dual
 * licensing only applies to this file, and not this project as a
 * whole.
 *
 * updated at 2018-09-05 09:54:03
 *
 */

#ifndef ANLG_PHY_TOP
#define ANLG_PHY_TOP

#define CTL_BASE_ANLG_PHY_TOP 0x63460000

#define REG_ANLG_PHY_TOP_ANALOG_RCO100M_RTC100M_CTRL            ( CTL_BASE_ANLG_PHY_TOP + 0x0000 )
#define REG_ANLG_PHY_TOP_ANALOG_RCO100M_REG_SEL_CFG_0           ( CTL_BASE_ANLG_PHY_TOP + 0x0004 )
#define REG_ANLG_PHY_TOP_ANALOG_EFUSE4K_EFUSE_PIN_PW_CTL        ( CTL_BASE_ANLG_PHY_TOP + 0x0008 )
#define REG_ANLG_PHY_TOP_ANALOG_EFUSE4K_REG_SEL_CFG_0           ( CTL_BASE_ANLG_PHY_TOP + 0x000C )
#define REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_R8PLL_CTRL0             ( CTL_BASE_ANLG_PHY_TOP + 0x0010 )
#define REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_R8PLL_CTRL2             ( CTL_BASE_ANLG_PHY_TOP + 0x0014 )
#define REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_V3PLL_CTRL0             ( CTL_BASE_ANLG_PHY_TOP + 0x0018 )
#define REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_V3PLL_CTRL2             ( CTL_BASE_ANLG_PHY_TOP + 0x001C )
#define REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_CPUPLL_CTRL0         ( CTL_BASE_ANLG_PHY_TOP + 0x0020 )
#define REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_CPUPLL_CTRL2         ( CTL_BASE_ANLG_PHY_TOP + 0x0024 )
#define REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_DSPPLL_CTRL0         ( CTL_BASE_ANLG_PHY_TOP + 0x0028 )
#define REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_DSPPLL_CTRL2         ( CTL_BASE_ANLG_PHY_TOP + 0x002C )
#define REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_NRPLL_CTRL0             ( CTL_BASE_ANLG_PHY_TOP + 0x0030 )
#define REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_NRPLL_CTRL2             ( CTL_BASE_ANLG_PHY_TOP + 0x0034 )
#define REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_R5PLL_CTRL0             ( CTL_BASE_ANLG_PHY_TOP + 0x0038 )
#define REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_R5PLL_CTRL2             ( CTL_BASE_ANLG_PHY_TOP + 0x003C )
#define REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_REG_SEL_CFG_0           ( CTL_BASE_ANLG_PHY_TOP + 0x0040 )
#define REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_REG_SEL_CFG_1           ( CTL_BASE_ANLG_PHY_TOP + 0x0044 )
#define REG_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_THM_CTRL0          ( CTL_BASE_ANLG_PHY_TOP + 0x0048 )
#define REG_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_THM_CTRL1          ( CTL_BASE_ANLG_PHY_TOP + 0x004C )
#define REG_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_MPLL0_CTRL0        ( CTL_BASE_ANLG_PHY_TOP + 0x0050 )
#define REG_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_MPLL0_CTRL2        ( CTL_BASE_ANLG_PHY_TOP + 0x0054 )
#define REG_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_MPLL1_CTRL0        ( CTL_BASE_ANLG_PHY_TOP + 0x0058 )
#define REG_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_MPLL1_CTRL2        ( CTL_BASE_ANLG_PHY_TOP + 0x005C )
#define REG_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_REG_SEL_CFG_0      ( CTL_BASE_ANLG_PHY_TOP + 0x0060 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_ANA_BB_PWR_CTRL          ( CTL_BASE_ANLG_PHY_TOP + 0x0064 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_ANA_BB_SINE_CTRL1        ( CTL_BASE_ANLG_PHY_TOP + 0x0068 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_CTRL0            ( CTL_BASE_ANLG_PHY_TOP + 0x006C )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_CTRL1            ( CTL_BASE_ANLG_PHY_TOP + 0x0070 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_CTRL2            ( CTL_BASE_ANLG_PHY_TOP + 0x0074 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_CTRL3            ( CTL_BASE_ANLG_PHY_TOP + 0x0078 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_CTRL4            ( CTL_BASE_ANLG_PHY_TOP + 0x007C )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_CTRL6            ( CTL_BASE_ANLG_PHY_TOP + 0x0080 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_CTRL0            ( CTL_BASE_ANLG_PHY_TOP + 0x0084 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_CTRL1            ( CTL_BASE_ANLG_PHY_TOP + 0x0088 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_CTRL2            ( CTL_BASE_ANLG_PHY_TOP + 0x008C )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_CTRL3            ( CTL_BASE_ANLG_PHY_TOP + 0x0090 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_CTRL4            ( CTL_BASE_ANLG_PHY_TOP + 0x0094 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_CTRL6            ( CTL_BASE_ANLG_PHY_TOP + 0x0098 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_DLPLL_CTRL0              ( CTL_BASE_ANLG_PHY_TOP + 0x009C )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_DLPLL_CTRL2              ( CTL_BASE_ANLG_PHY_TOP + 0x00A0 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_M_5G_APC_CTRL1           ( CTL_BASE_ANLG_PHY_TOP + 0x00A4 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_M_N5G_APC_CTRL1          ( CTL_BASE_ANLG_PHY_TOP + 0x00A8 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_S_5G_APC_CTRL1           ( CTL_BASE_ANLG_PHY_TOP + 0x00AC )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_S_N5G_APC_CTRL1          ( CTL_BASE_ANLG_PHY_TOP + 0x00B0 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_AAPC_CTRL1               ( CTL_BASE_ANLG_PHY_TOP + 0x00B4 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_TEST_CLK_CTRL            ( CTL_BASE_ANLG_PHY_TOP + 0x00B8 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_REG_SEL_CFG_0            ( CTL_BASE_ANLG_PHY_TOP + 0x00BC )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_REG_SEL_CFG_1            ( CTL_BASE_ANLG_PHY_TOP + 0x00C0 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_REG_SEL_CFG_2            ( CTL_BASE_ANLG_PHY_TOP + 0x00C4 )

/* REG_ANLG_PHY_TOP_ANALOG_RCO100M_RTC100M_CTRL */

#define BIT_ANLG_PHY_TOP_ANALOG_RCO100M_RCO100M_EN                     BIT(8)
#define BIT_ANLG_PHY_TOP_ANALOG_RCO100M_RCO100M_RSTB                   BIT(7)
#define BIT_ANLG_PHY_TOP_ANALOG_RCO100M_RCO100M_RC_C(x)                (((x) & 0x7F))

/* REG_ANLG_PHY_TOP_ANALOG_RCO100M_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_RCO100M_RCO100M_EN             BIT(2)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_RCO100M_RCO100M_RSTB           BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_RCO100M_RCO100M_RC_C           BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_EFUSE4K_EFUSE_PIN_PW_CTL */

#define BIT_ANLG_PHY_TOP_ANALOG_EFUSE4K_EFS_ENK1                       BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_EFUSE4K_EFS_ENK2                       BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_EFUSE4K_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_EFUSE4K_EFS_ENK1               BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_EFUSE4K_EFS_ENK2               BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_R8PLL_CTRL0 */

#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_R8PLL_LOCK_DONE                BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_R8PLL_CTRL2 */

#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_R8PLL_RST                      BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_R8PLL_PD                       BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_R8PLL_REF_SEL                  BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_R8PLL_DIV32_EN                 BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_R8PLL_CLKOUT_EN                BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_V3PLL_CTRL0 */

#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_V3PLL_LOCK_DONE                BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_V3PLL_CTRL2 */

#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_V3PLL_RST                      BIT(8)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_V3PLL_PD                       BIT(7)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_V3PLL_REF_SEL(x)               (((x) & 0x3) << 5)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_V3PLL_DIV32_EN                 BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_V3PLL_DIV2P5_EN                BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_V3PLL_DIV1P5_EN                BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_V3PLL_DIV1_EN                  BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_V3PLL_CLKOUT_EN                BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_CPUPLL_CTRL0 */

#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_CPUPLL_LOCK_DONE            BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_CPUPLL_CTRL2 */

#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_CPUPLL_PD                   BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_CPUPLL_RST                  BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_CPUPLL_REF_SEL              BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_CPUPLL_DIV32_EN             BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_CPUPLL_CLKOUT_EN            BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_DSPPLL_CTRL0 */

#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_DSPPLL_LOCK_DONE            BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_DSPPLL_CTRL2 */

#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_DSPPLL_RST                  BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_DSPPLL_PD                   BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_DSPPLL_REF_SEL              BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_DSPPLL_DIV32_EN             BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NR_DSPPLL_CLKOUT_EN            BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_NRPLL_CTRL0 */

#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NRPLL_LOCK_DONE                BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_NRPLL_CTRL2 */

#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NRPLL_RST                      BIT(8)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NRPLL_PD                       BIT(7)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NRPLL_REF_SEL(x)               (((x) & 0x3) << 5)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NRPLL_DIV32_EN                 BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NRPLL_DIV3_EN                  BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NRPLL_DIV2_EN                  BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NRPLL_DIV1_EN                  BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_NRPLL_CLKOUT_EN                BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_R5PLL_CTRL0 */

#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_R5PLL_LOCK_DONE                BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_R5PLL_CTRL2 */

#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_R5PLL_PD                       BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_R5PLL_RST                      BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_R5PLL_REF_SEL                  BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_R5PLL_DIV32_EN                 BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_PLL_TOP_R5PLL_CLKOUT_EN                BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_R8PLL_RST              BIT(31)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_R8PLL_PD               BIT(30)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_R8PLL_REF_SEL          BIT(29)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_R8PLL_DIV32_EN         BIT(28)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_R8PLL_CLKOUT_EN        BIT(27)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_V3PLL_RST              BIT(26)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_V3PLL_PD               BIT(25)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_V3PLL_REF_SEL          BIT(24)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_V3PLL_DIV32_EN         BIT(23)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_V3PLL_DIV2P5_EN        BIT(22)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_V3PLL_DIV1P5_EN        BIT(21)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_V3PLL_DIV1_EN          BIT(20)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_V3PLL_CLKOUT_EN        BIT(19)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_NR_CPUPLL_PD           BIT(18)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_NR_CPUPLL_RST          BIT(17)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_NR_CPUPLL_REF_SEL      BIT(16)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_NR_CPUPLL_DIV32_EN     BIT(15)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_NR_CPUPLL_CLKOUT_EN    BIT(14)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_NR_DSPPLL_RST          BIT(13)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_NR_DSPPLL_PD           BIT(12)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_NR_DSPPLL_REF_SEL      BIT(11)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_NR_DSPPLL_DIV32_EN     BIT(10)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_NR_DSPPLL_CLKOUT_EN    BIT(9)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_NRPLL_RST              BIT(8)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_NRPLL_PD               BIT(7)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_NRPLL_REF_SEL          BIT(6)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_NRPLL_DIV32_EN         BIT(5)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_NRPLL_DIV3_EN          BIT(4)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_NRPLL_DIV2_EN          BIT(3)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_NRPLL_DIV1_EN          BIT(2)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_NRPLL_CLKOUT_EN        BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_R5PLL_PD               BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_PLL_TOP_REG_SEL_CFG_1 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_R5PLL_RST              BIT(3)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_R5PLL_REF_SEL          BIT(2)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_R5PLL_DIV32_EN         BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_PLL_TOP_R5PLL_CLKOUT_EN        BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_THM_CTRL0 */

#define BIT_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_THM_RSTN                  BIT(13)
#define BIT_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_THM_RUN                   BIT(12)
#define BIT_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_THM_PD                    BIT(11)
#define BIT_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_THM_DATA(x)               (((x) & 0x3FF) << 1)
#define BIT_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_THM_VALID                 BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_THM_CTRL1 */

#define BIT_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_THM_RESERVED(x)           (((x) & 0xFFFF))

/* REG_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_MPLL0_CTRL0 */

#define BIT_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_MPLL0_LOCK_DONE           BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_MPLL0_CTRL2 */

#define BIT_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_MPLL0_RST                 BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_MPLL0_PD                  BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_MPLL0_DIV32_EN            BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_MPLL0_CLKOUTEN            BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_MPLL1_CTRL0 */

#define BIT_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_MPLL1_LOCK_DONE           BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_MPLL1_CTRL2 */

#define BIT_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_MPLL1_RST                 BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_MPLL1_PD                  BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_MPLL1_DIV32_EN            BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_MPLL1_CLKOUTEN            BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MPLL_THM_TOP_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MPLL_THM_TOP_THM_RSTN          BIT(11)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MPLL_THM_TOP_THM_RUN           BIT(10)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MPLL_THM_TOP_THM_PD            BIT(9)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MPLL_THM_TOP_THM_RESERVED      BIT(8)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MPLL_THM_TOP_MPLL0_RST         BIT(7)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MPLL_THM_TOP_MPLL0_PD          BIT(6)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MPLL_THM_TOP_MPLL0_DIV32_EN    BIT(5)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MPLL_THM_TOP_MPLL0_CLKOUTEN    BIT(4)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MPLL_THM_TOP_MPLL1_RST         BIT(3)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MPLL_THM_TOP_MPLL1_PD          BIT(2)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MPLL_THM_TOP_MPLL1_DIV32_EN    BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MPLL_THM_TOP_MPLL1_CLKOUTEN    BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_ANA_BB_PWR_CTRL */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_REC_26MHZ_0_BUF_PD              BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_S_APC5G_PD                      BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_S_APCNON5G_PD                   BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_M_APCNON5G_PD                   BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_M_APC5G_PD                      BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_ANA_BB_SINE_CTRL1 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_SINDRV_ENA_SQUARE               BIT(11)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_PROBE_SEL(x)                    (((x) & 0xFF) << 3)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_SINDRV_0_ENA                    BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_SINDRV_1_ENA                    BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_SINDRV_2_ENA                    BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_CTRL0 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_LOCK_DONE               BIT(17)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_N(x)                    (((x) & 0x7FF) << 6)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_ICP(x)                  (((x) & 0x7) << 3)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_SDM_EN                  BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_MOD_EN                  BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_DIV_S                   BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_CTRL1 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_NINT(x)                 (((x) & 0x7F) << 23)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_KINT(x)                 (((x) & 0x7FFFFF))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_CTRL2 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_FREQ_DOUBLE_EN          BIT(7)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_RST                     BIT(6)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_PD                      BIT(5)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_REF_SEL(x)              (((x) & 0x3) << 3)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_POSTDIV                 BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_DIV_EN                  BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_CLKOUT_EN               BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_CTRL3 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_VCO_TEST_EN             BIT(8)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_SSC_CTRL(x)             (((x) & 0xFF))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_CTRL4 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_LPF(x)                  (((x) & 0x7) << 10)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_LDO_TRIM(x)             (((x) & 0xF) << 6)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_IBIAS(x)                (((x) & 0x3) << 4)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_FBDIV_EN                BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_CP_OFFSET(x)            (((x) & 0x3) << 1)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_CP_EN                   BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_CTRL6 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_VSET(x)                 (((x) & 0x7) << 27)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_26M_DIV(x)              (((x) & 0x3F) << 21)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_V3_RPLL_RESERVED(x)             (((x) & 0x1FFFFF))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_CTRL0 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_LOCK_DONE               BIT(17)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_N(x)                    (((x) & 0x7FF) << 6)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_ICP(x)                  (((x) & 0x7) << 3)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_SDM_EN                  BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_MOD_EN                  BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_DIV_S                   BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_CTRL1 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_NINT(x)                 (((x) & 0x7F) << 23)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_KINT(x)                 (((x) & 0x7FFFFF))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_CTRL2 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_FREQ_DOUBLE_EN          BIT(7)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_RST                     BIT(6)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_PD                      BIT(5)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_REF_SEL(x)              (((x) & 0x3) << 3)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_POSTDIV                 BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_DIV_EN                  BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_CLKOUT_EN               BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_CTRL3 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_VCO_TEST_EN             BIT(8)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_SSC_CTRL(x)             (((x) & 0xFF))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_CTRL4 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_LPF(x)                  (((x) & 0x7) << 10)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_LDO_TRIM(x)             (((x) & 0xF) << 6)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_IBIAS(x)                (((x) & 0x3) << 4)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_FBDIV_EN                BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_CP_OFFSET(x)            (((x) & 0x3) << 1)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_CP_EN                   BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_CTRL6 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_VSET(x)                 (((x) & 0x7) << 27)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_26M_DIV(x)              (((x) & 0x3F) << 21)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_NR_RPLL_RESERVED(x)             (((x) & 0x1FFFFF))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_DLPLL_CTRL0 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_DLPLL_LOCK_DONE                 BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_DLPLL_CTRL2 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_DLPLL_RST                       BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_DLPLL_PD                        BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_DLPLL_DIV32_EN                  BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_DLPLL_CLKOUT_EN                 BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_M_5G_APC_CTRL1 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_M_APC5G_RESERVED(x)             (((x) & 0x3) << 18)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_M_APC5GOUT_SEL(x)               (((x) & 0x3) << 16)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_M_APC5G_LOW_V_CON               BIT(15)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_M_APC5G_BPRES                   BIT(14)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_M_APC5G_D(x)                    (((x) & 0x3FFF))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_M_N5G_APC_CTRL1 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_M_APCNON5G_RESERVED(x)          (((x) & 0x3) << 18)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_M_APCNON5GOUT_SEL(x)            (((x) & 0x3) << 16)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_M_APCNON5G_LOW_V_CON            BIT(15)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_M_APCNON5G_BPRES                BIT(14)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_M_APCNON5G_D(x)                 (((x) & 0x3FFF))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_S_5G_APC_CTRL1 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_S_APC5G_RESERVED(x)             (((x) & 0x3) << 18)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_S_APC5GOUT_SEL(x)               (((x) & 0x3) << 16)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_S_APC5G_LOW_V_CON               BIT(15)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_S_APC5G_BPRES                   BIT(14)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_S_APC5G_D(x)                    (((x) & 0x3FFF))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_S_N5G_APC_CTRL1 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_S_APCNON5G_RESERVED(x)          (((x) & 0x3) << 18)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_S_APCNON5GOUT_SEL(x)            (((x) & 0x3) << 16)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_S_APCNON5G_LOW_V_CON            BIT(15)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_S_APCNON5G_BPRES                BIT(14)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_S_APCNON5G_D(x)                 (((x) & 0x3FFF))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_AAPC_CTRL1 */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_APC5G_G0(x)                     (((x) & 0x3) << 6)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_APC5G_G1(x)                     (((x) & 0x3) << 4)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_APCNON5G_G0(x)                  (((x) & 0x3) << 2)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_APCNON5G_G1(x)                  (((x) & 0x3))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_TEST_CLK_CTRL */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_U2U3_REF_SEL(x)                 (((x) & 0x3))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_REC_26MHZ_0_BUF_PD      BIT(31)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_S_APC5G_PD              BIT(30)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_S_APCNON5G_PD           BIT(29)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_M_APCNON5G_PD           BIT(28)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_M_APC5G_PD              BIT(27)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_SINDRV_ENA_SQUARE       BIT(26)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_PROBE_SEL               BIT(25)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_SINDRV_0_ENA            BIT(24)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_SINDRV_1_ENA            BIT(23)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_SINDRV_2_ENA            BIT(22)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_N               BIT(21)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_ICP             BIT(20)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_SDM_EN          BIT(19)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_MOD_EN          BIT(18)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_DIV_S           BIT(17)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_NINT            BIT(16)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_KINT            BIT(15)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_FREQ_DOUBLE_EN  BIT(14)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_RST             BIT(13)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_PD              BIT(12)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_REF_SEL         BIT(11)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_POSTDIV         BIT(10)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_DIV_EN          BIT(9)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_CLKOUT_EN       BIT(8)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_VCO_TEST_EN     BIT(7)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_SSC_CTRL        BIT(6)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_LPF             BIT(5)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_LDO_TRIM        BIT(4)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_IBIAS           BIT(3)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_FBDIV_EN        BIT(2)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_CP_OFFSET       BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_CP_EN           BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_REG_SEL_CFG_1 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_VSET            BIT(31)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_26M_DIV         BIT(30)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_V3_RPLL_RESERVED        BIT(29)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_N               BIT(28)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_ICP             BIT(27)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_SDM_EN          BIT(26)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_MOD_EN          BIT(25)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_DIV_S           BIT(24)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_NINT            BIT(23)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_KINT            BIT(22)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_FREQ_DOUBLE_EN  BIT(21)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_RST             BIT(20)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_PD              BIT(19)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_REF_SEL         BIT(18)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_POSTDIV         BIT(17)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_DIV_EN          BIT(16)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_CLKOUT_EN       BIT(15)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_VCO_TEST_EN     BIT(14)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_SSC_CTRL        BIT(13)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_LPF             BIT(12)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_LDO_TRIM        BIT(11)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_IBIAS           BIT(10)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_FBDIV_EN        BIT(9)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_CP_OFFSET       BIT(8)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_CP_EN           BIT(7)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_VSET            BIT(6)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_26M_DIV         BIT(5)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_NR_RPLL_RESERVED        BIT(4)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_DLPLL_RST               BIT(3)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_DLPLL_PD                BIT(2)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_DLPLL_DIV32_EN          BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_DLPLL_CLKOUT_EN         BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_REG_SEL_CFG_2 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_M_APC5G_RESERVED        BIT(24)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_M_APC5GOUT_SEL          BIT(23)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_M_APC5G_LOW_V_CON       BIT(22)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_M_APC5G_BPRES           BIT(21)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_M_APC5G_D               BIT(20)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_M_APCNON5G_RESERVED     BIT(19)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_M_APCNON5GOUT_SEL       BIT(18)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_M_APCNON5G_LOW_V_CON    BIT(17)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_M_APCNON5G_BPRES        BIT(16)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_M_APCNON5G_D            BIT(15)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_S_APC5G_RESERVED        BIT(14)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_S_APC5GOUT_SEL          BIT(13)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_S_APC5G_LOW_V_CON       BIT(12)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_S_APC5G_BPRES           BIT(11)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_S_APC5G_D               BIT(10)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_S_APCNON5G_RESERVED     BIT(9)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_S_APCNON5GOUT_SEL       BIT(8)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_S_APCNON5G_LOW_V_CON    BIT(7)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_S_APCNON5G_BPRES        BIT(6)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_S_APCNON5G_D            BIT(5)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_APC5G_G0                BIT(4)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_APC5G_G1                BIT(3)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_APCNON5G_G0             BIT(2)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_APCNON5G_G1             BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_U2U3_REF_SEL            BIT(0)

#endif

