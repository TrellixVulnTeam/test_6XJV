/*
 * Copyright (C) 2018 Spreadtrum Communications Inc.
 *
 * This file is dual-licensed: you can use it either under the terms
 * of the GPL or the X11 license, at your option. Note that this dual
 * licensing only applies to this file, and not this project as a
 * whole.
 *
 * updated at 2018-08-09 17:05:54
 *
 */


#ifndef ANLG_TOP_1_H
#define ANLG_TOP_1_H

#define CTL_BASE_ANLG_TOP_1 0x32424000


#define REG_ANLG_TOP_1_ANALOG_MPLL_CLK_JITTER_CTRL  ( CTL_BASE_ANLG_TOP_1 + 0x0000 )
#define REG_ANLG_TOP_1_ANALOG_PHY_POWER_DOWN_CTRL0  ( CTL_BASE_ANLG_TOP_1 + 0x0004 )
#define REG_ANLG_TOP_1_ANALOG_PHY_POWER_DOWN_CTRL1  ( CTL_BASE_ANLG_TOP_1 + 0x0008 )

/* REG_ANLG_TOP_1_ANALOG_MPLL_CLK_JITTER_CTRL */

#define BIT_ANLG_TOP_1_R2G_ANALOG_USB20_USB20_BISTRAM_EN            BIT(6)
#define BIT_ANLG_TOP_1_MPLL1_CLK_JITTER_MON_SEL(x)                  (((x) & 0x3) << 4)
#define BIT_ANLG_TOP_1_MPLL1_CLK_JITTER_MON_EN                      BIT(3)
#define BIT_ANLG_TOP_1_MPLL0_CLK_JITTER_MON_SEL(x)                  (((x) & 0x3) << 1)
#define BIT_ANLG_TOP_1_MPLL0_CLK_JITTER_MON_EN                      BIT(0)

/* REG_ANLG_TOP_1_ANALOG_PHY_POWER_DOWN_CTRL0 */

#define BIT_ANLG_TOP_1_R2G_USB20_ISO_SW_EN                          BIT(11)
#define BIT_ANLG_TOP_1_R2G_ANALOG_USB20_USB20_PS_PD_S               BIT(10)
#define BIT_ANLG_TOP_1_R2G_ANALOG_USB20_USB20_PS_PD_L               BIT(9)
#define BIT_ANLG_TOP_1_DBG_SEL_USB20_ISO_SW_EN                      BIT(8)
#define BIT_ANLG_TOP_1_DBG_SEL_ANALOG_USB20_USB20_PS_PD_S           BIT(7)
#define BIT_ANLG_TOP_1_DBG_SEL_ANALOG_USB20_USB20_PS_PD_L           BIT(6)
#define BIT_ANLG_TOP_1_DBG_SEL_ANALOG_PCIE_GEN2_ISO_SW_EN           BIT(5)
#define BIT_ANLG_TOP_1_DBG_SEL_ANALOG_PCIE_GEN2_PS_PD_L             BIT(4)
#define BIT_ANLG_TOP_1_DBG_SEL_ANALOG_PCIE_GEN2_PS_PD_S             BIT(3)
#define BIT_ANLG_TOP_1_R2G_ANALOG_PCIE_GEN2_ISO_SW_EN               BIT(2)
#define BIT_ANLG_TOP_1_R2G_ANALOG_PCIE_GEN2_PS_PD_L                 BIT(1)
#define BIT_ANLG_TOP_1_R2G_ANALOG_PCIE_GEN2_PS_PD_S                 BIT(0)

/* REG_ANLG_TOP_1_ANALOG_PHY_POWER_DOWN_CTRL1 */

#define BIT_ANLG_TOP_1_R2G_CSI_2P2LANE_ISO_SW_EN                    BIT(23)
#define BIT_ANLG_TOP_1_R2G_ANALOG_MIPI_CSI_2P2LANE_CSI_PS_PD_S      BIT(22)
#define BIT_ANLG_TOP_1_R2G_ANALOG_MIPI_CSI_2P2LANE_CSI_PS_PD_L      BIT(21)
#define BIT_ANLG_TOP_1_DBG_SEL_CSI_2P2LANE_ISO_SW_EN                BIT(20)
#define BIT_ANLG_TOP_1_DBG_SEL_ANALOG_MIPI_CSI_2P2LANE_CSI_PS_PD_S  BIT(19)
#define BIT_ANLG_TOP_1_DBG_SEL_ANALOG_MIPI_CSI_2P2LANE_CSI_PS_PD_L  BIT(18)
#define BIT_ANLG_TOP_1_R2G_CSI_4LANE_1_ISO_SW_EN                    BIT(17)
#define BIT_ANLG_TOP_1_R2G_ANALOG_MIPI_CSI_4LANE_1_CSI_PS_PD_S      BIT(16)
#define BIT_ANLG_TOP_1_R2G_ANALOG_MIPI_CSI_4LANE_1_CSI_PS_PD_L      BIT(15)
#define BIT_ANLG_TOP_1_DBG_SEL_CSI_4LANE_1_ISO_SW_EN                BIT(14)
#define BIT_ANLG_TOP_1_DBG_SEL_ANALOG_MIPI_CSI_4LANE_1_CSI_PS_PD_S  BIT(13)
#define BIT_ANLG_TOP_1_DBG_SEL_ANALOG_MIPI_CSI_4LANE_1_CSI_PS_PD_L  BIT(12)
#define BIT_ANLG_TOP_1_R2G_CSI_4LANE_0_ISO_SW_EN                    BIT(11)
#define BIT_ANLG_TOP_1_R2G_ANALOG_MIPI_CSI_4LANE_0_CSI_PS_PD_S      BIT(10)
#define BIT_ANLG_TOP_1_R2G_ANALOG_MIPI_CSI_4LANE_0_CSI_PS_PD_L      BIT(9)
#define BIT_ANLG_TOP_1_DBG_SEL_CSI_4LANE_0_ISO_SW_EN                BIT(8)
#define BIT_ANLG_TOP_1_DBG_SEL_ANALOG_MIPI_CSI_4LANE_0_CSI_PS_PD_S  BIT(7)
#define BIT_ANLG_TOP_1_DBG_SEL_ANALOG_MIPI_CSI_4LANE_0_CSI_PS_PD_L  BIT(6)
#define BIT_ANLG_TOP_1_R2G_DSI_4LANE_ISO_SW_EN                      BIT(5)
#define BIT_ANLG_TOP_1_R2G_ANALOG_MIPI_DSI_4LANE_DSI_PS_PD_S        BIT(4)
#define BIT_ANLG_TOP_1_R2G_ANALOG_MIPI_DSI_4LANE_DSI_PS_PD_L        BIT(3)
#define BIT_ANLG_TOP_1_DBG_SEL_DSI_4LANE_ISO_SW_EN                  BIT(2)
#define BIT_ANLG_TOP_1_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_PS_PD_S    BIT(1)
#define BIT_ANLG_TOP_1_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_PS_PD_L    BIT(0)


#endif /* ANLG_TOP_1_H */


