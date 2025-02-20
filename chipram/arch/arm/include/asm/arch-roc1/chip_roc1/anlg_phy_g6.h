/*
 * Copyright (C) 2018 Spreadtrum Communications Inc.
 *
 * This file is dual-licensed: you can use it either under the terms
 * of the GPL or the X11 license, at your option. Note that this dual
 * licensing only applies to this file, and not this project as a
 * whole.
 *
 * updated at 2018-08-09 17:05:53
 *
 */

#ifndef ANLG_PHY_G6_H
#define ANLG_PHY_G6_H

#define CTL_BASE_ANLG_PHY_G6 0x32434000

#define REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLL_V_CTRL0  ( CTL_BASE_ANLG_PHY_G6 + 0x0000 )
#define REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLL_V_CTRL1  ( CTL_BASE_ANLG_PHY_G6 + 0x0004 )
#define REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLL_V_CTRL2  ( CTL_BASE_ANLG_PHY_G6 + 0x0008 )
#define REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLL_V_CTRL3  ( CTL_BASE_ANLG_PHY_G6 + 0x000C )
#define REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLL_V_CTRL6  ( CTL_BASE_ANLG_PHY_G6 + 0x0010 )
#define REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLL_V_CTRL7  ( CTL_BASE_ANLG_PHY_G6 + 0x0014 )
#define REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLL_V_CTRL8  ( CTL_BASE_ANLG_PHY_G6 + 0x0018 )
#define REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLL_V_CTRL4  ( CTL_BASE_ANLG_PHY_G6 + 0x001C )
#define REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLL_V_CTRL5  ( CTL_BASE_ANLG_PHY_G6 + 0x0020 )
#define REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_REG_SEL_CFG_0    ( CTL_BASE_ANLG_PHY_G6 + 0x0024 )

/* REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLL_V_CTRL0 */

#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_REFCK_SEL(x)        (((x) & 0x7) << 29)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_CLKOUTEXT_EN        BIT(28)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_PD                  BIT(27)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_RST                 BIT(26)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_N(x)                (((x) & 0x7FF) << 15)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_CLKOUTEN            BIT(14)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_DIVN(x)             (((x) & 0x1F) << 9)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_BIST_CTRL(x)        (((x) & 0xFF) << 1)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_DIFF_OR_SING_SEL                BIT(0)

/* REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLL_V_CTRL1 */

#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_NINT(x)             (((x) & 0xFF) << 20)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_KINT(x)             (((x) & 0xFFFFF))

/* REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLL_V_CTRL2 */

#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_SSC_DIV(x)          (((x) & 0x7) << 16)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_KDELTA(x)           (((x) & 0xFFFF))

/* REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLL_V_CTRL3 */

#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_KSTEP(x)            (((x) & 0x7FFFF) << 6)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_SDM_EN              BIT(5)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_MOD_EN              BIT(4)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_DIV_S               BIT(3)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_BG_RBIAS_MODE                   BIT(2)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_PREDIV              BIT(1)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_POSTDIV             BIT(0)

/* REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLL_V_CTRL6 */

#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_RESERVED(x)         (((x) & 0x1FFFFF))

/* REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLL_V_CTRL7 */

#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_BIST_EN             BIT(20)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_BIST_CNT(x)         (((x) & 0xFFFF) << 4)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_LOCKDONE            BIT(3)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLLV_DUTY_FIT_EN            BIT(2)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLLV_26MBUFFER_BIASSEL(x)   (((x) & 0x3))

/* REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLL_V_CTRL8 */

#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLLV_26MBUFFER_PD           BIT(7)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLLV_26MBUFFER_TEST_EN      BIT(6)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLLV_LDOOUT_SEL(x)          (((x) & 0x3) << 4)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_ICP(x)              (((x) & 0x7) << 1)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_CP_EN               BIT(0)

/* REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLL_V_CTRL4 */

#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_LDO_TRIM(x)         (((x) & 0xF) << 26)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_R2_SEL(x)           (((x) & 0x7) << 23)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_R3_SEL(x)           (((x) & 0x3) << 21)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_C2_SEL(x)           (((x) & 0x7) << 18)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_KVCO_SEL(x)         (((x) & 0x7) << 15)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_VCO_TEST_EN         BIT(14)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_VCO_TEST_INT        BIT(13)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_VCO_TEST_INTSEL(x)  (((x) & 0x7) << 10)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_SHORT_CSR_EN        BIT(9)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_ALLOP_PD            BIT(8)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_TEST_VCO_PN         BIT(7)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_MANUAL_ADJ_PD       BIT(6)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_FBDIV_EN            BIT(5)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_VCOBUF_EN           BIT(4)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_CP_OFFSET(x)        (((x) & 0x7) << 1)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_DOUBLER_EN          BIT(0)

/* REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_PCIEPLL_V_CTRL5 */

#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_CALI_MODE(x)        (((x) & 0x3) << 25)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_CALI_INI(x)         (((x) & 0x1F) << 20)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_CALI_TRIG           BIT(19)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_CALI_WAITCNT(x)     (((x) & 0x3) << 17)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_CALI_POLARITY       BIT(16)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_VCTRLH_SEL(x)       (((x) & 0x7) << 13)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_VCTRLL_SEL(x)       (((x) & 0x7) << 10)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_CALI_DONE           BIT(9)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_CALI_OUT(x)         (((x) & 0x1F) << 4)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_CALI_CPPD           BIT(3)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_FREQ_DIFF_EN        BIT(2)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_CALI_VCTRL_HIGH     BIT(1)
#define BIT_ANLG_PHY_G6_ANALOG_PCIEPLL_V_RG_PCIEPLLV_CALI_VCTRL_LOW      BIT(0)

/* REG_ANLG_PHY_G6_ANALOG_PCIEPLL_V_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_G6_DBG_SEL_ANALOG_PCIEPLL_V_RG_PCIEPLLV_REFCK_SEL   BIT(4)
#define BIT_ANLG_PHY_G6_DBG_SEL_ANALOG_PCIEPLL_V_RG_PCIEPLLV_PD          BIT(3)
#define BIT_ANLG_PHY_G6_DBG_SEL_ANALOG_PCIEPLL_V_RG_PCIEPLLV_RST         BIT(2)
#define BIT_ANLG_PHY_G6_DBG_SEL_ANALOG_PCIEPLL_V_RG_PCIEPLLV_CLKOUTEN    BIT(1)
#define BIT_ANLG_PHY_G6_DBG_SEL_ANALOG_PCIEPLL_V_PCIEPLLV_26MBUFFER_PD   BIT(0)

#endif
