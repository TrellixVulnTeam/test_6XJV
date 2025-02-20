/*
 * Copyright (C) 2018 Spreadtrum Communications Inc.
 *
 * This file is dual-licensed: you can use it either under the terms
 * of the GPL or the X11 license, at your option. Note that this dual
 * licensing only applies to this file, and not this project as a
 * whole.
 *
 * updated at 2018-09-04 11:00:16
 *
 */

#ifndef DPLL_PRE_DIV_GEN
#define DPLL_PRE_DIV_GEN

#define CTL_BASE_DPLL_PRE_DIV_GEN 0x00000000

#define REG_DPLL_PRE_DIV_GEN_SOFT_CNT_DONE0_CFG                ( CTL_BASE_DPLL_PRE_DIV_GEN + 0x0020 )
#define REG_DPLL_PRE_DIV_GEN_PLL_WAIT_SEL0_CFG                 ( CTL_BASE_DPLL_PRE_DIV_GEN + 0x0024 )
#define REG_DPLL_PRE_DIV_GEN_PLL_WAIT_SW_CTL0_CFG              ( CTL_BASE_DPLL_PRE_DIV_GEN + 0x0028 )
#define REG_DPLL_PRE_DIV_GEN_GATE_EN_SEL0_CFG                  ( CTL_BASE_DPLL_PRE_DIV_GEN + 0x0034 )
#define REG_DPLL_PRE_DIV_GEN_GATE_EN_SW_CTL0_CFG               ( CTL_BASE_DPLL_PRE_DIV_GEN + 0x0038 )
#define REG_DPLL_PRE_DIV_GEN_MONITOR_WAIT_EN_STATUS0_CFG       ( CTL_BASE_DPLL_PRE_DIV_GEN + 0x003C )
#define REG_DPLL_PRE_DIV_GEN_MONITOR_GATE_AUTO_EN_STATUS0_CFG  ( CTL_BASE_DPLL_PRE_DIV_GEN + 0x0040 )

/* REG_DPLL_PRE_DIV_GEN_SOFT_CNT_DONE0_CFG */

#define BIT_DPLL_PRE_DIV_GEN_SOFT_CNT_DONE0_CFG_DPLL0_SOFT_CNT_DONE                           BIT(1)
#define BIT_DPLL_PRE_DIV_GEN_SOFT_CNT_DONE0_CFG_DPLL1_SOFT_CNT_DONE                           BIT(0)

/* REG_DPLL_PRE_DIV_GEN_PLL_WAIT_SEL0_CFG */

#define BIT_DPLL_PRE_DIV_GEN_PLL_WAIT_SEL0_CFG_DPLL0_WAIT_AUTO_GATE_SEL                       BIT(1)
#define BIT_DPLL_PRE_DIV_GEN_PLL_WAIT_SEL0_CFG_DPLL1_WAIT_AUTO_GATE_SEL                       BIT(0)

/* REG_DPLL_PRE_DIV_GEN_PLL_WAIT_SW_CTL0_CFG */

#define BIT_DPLL_PRE_DIV_GEN_PLL_WAIT_SW_CTL0_CFG_DPLL0_WAIT_FORCE_EN                         BIT(1)
#define BIT_DPLL_PRE_DIV_GEN_PLL_WAIT_SW_CTL0_CFG_DPLL1_WAIT_FORCE_EN                         BIT(0)

/* REG_DPLL_PRE_DIV_GEN_GATE_EN_SEL0_CFG */

#define BIT_DPLL_PRE_DIV_GEN_GATE_EN_SEL0_CFG_CGM_DPLL0_1333M_PUB_AUTO_GATE_SEL               BIT(1)
#define BIT_DPLL_PRE_DIV_GEN_GATE_EN_SEL0_CFG_CGM_DPLL1_1866M_PUB_AUTO_GATE_SEL               BIT(0)

/* REG_DPLL_PRE_DIV_GEN_GATE_EN_SW_CTL0_CFG */

#define BIT_DPLL_PRE_DIV_GEN_GATE_EN_SW_CTL0_CFG_CGM_DPLL0_1333M_PUB_FORCE_EN                 BIT(1)
#define BIT_DPLL_PRE_DIV_GEN_GATE_EN_SW_CTL0_CFG_CGM_DPLL1_1866M_PUB_FORCE_EN                 BIT(0)

/* REG_DPLL_PRE_DIV_GEN_MONITOR_WAIT_EN_STATUS0_CFG */

#define BIT_DPLL_PRE_DIV_GEN_MONITOR_WAIT_EN_STATUS0_CFG_MONITOR_WAIT_EN_STATUS(x)            (((x) & 0x3))

/* REG_DPLL_PRE_DIV_GEN_MONITOR_GATE_AUTO_EN_STATUS0_CFG */

#define BIT_DPLL_PRE_DIV_GEN_MONITOR_GATE_AUTO_EN_STATUS0_CFG_MONITOR_GATE_AUTO_EN_STATUS(x)  (((x) & 0x3))

#endif

