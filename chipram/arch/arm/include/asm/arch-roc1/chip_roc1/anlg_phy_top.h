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


#ifndef ANLG_PHY_TOP_H
#define ANLG_PHY_TOP_H

#define CTL_BASE_ANLG_PHY_TOP 0x32420000


#define REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXCLKLANE_TOP      ( CTL_BASE_ANLG_PHY_TOP + 0x0000 )
#define REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATA_0_TOP       ( CTL_BASE_ANLG_PHY_TOP + 0x0004 )
#define REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATA_1_TOP       ( CTL_BASE_ANLG_PHY_TOP + 0x0008 )
#define REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATA_2_TOP       ( CTL_BASE_ANLG_PHY_TOP + 0x000C )
#define REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATA_3_TOP       ( CTL_BASE_ANLG_PHY_TOP + 0x0010 )
#define REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATAESC_TOP      ( CTL_BASE_ANLG_PHY_TOP + 0x0014 )
#define REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_STATE_RX_TOP       ( CTL_BASE_ANLG_PHY_TOP + 0x0018 )
#define REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_ERR_TOP            ( CTL_BASE_ANLG_PHY_TOP + 0x001C )
#define REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_CTRL_TOP           ( CTL_BASE_ANLG_PHY_TOP + 0x0020 )
#define REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_TEST_TOP           ( CTL_BASE_ANLG_PHY_TOP + 0x0024 )
#define REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_DATALANE_CTRL_TOP  ( CTL_BASE_ANLG_PHY_TOP + 0x0028 )
#define REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_MIPI_CTRL7_TOP            ( CTL_BASE_ANLG_PHY_TOP + 0x002C )
#define REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_REG_SEL_CFG_0             ( CTL_BASE_ANLG_PHY_TOP + 0x0030 )
#define REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_REG_SEL_CFG_1             ( CTL_BASE_ANLG_PHY_TOP + 0x0034 )
#define REG_ANLG_PHY_TOP_ANALOG_26MBUF_26MBUF_CTRL0_TOP                  ( CTL_BASE_ANLG_PHY_TOP + 0x0038 )
#define REG_ANLG_PHY_TOP_ANALOG_26MBUF_REG_SEL_CFG_0                     ( CTL_BASE_ANLG_PHY_TOP + 0x003C )
#define REG_ANLG_PHY_TOP_ANALOG_USB20_USB20_TEST_PIN_TOP                 ( CTL_BASE_ANLG_PHY_TOP + 0x0040 )
#define REG_ANLG_PHY_TOP_ANALOG_USB20_USB20_UTMI_CTL1_TOP                ( CTL_BASE_ANLG_PHY_TOP + 0x0044 )
#define REG_ANLG_PHY_TOP_ANALOG_USB20_USB20_UTMI_CTL2_TOP                ( CTL_BASE_ANLG_PHY_TOP + 0x0048 )
#define REG_ANLG_PHY_TOP_ANALOG_USB20_REG_SEL_CFG_0                      ( CTL_BASE_ANLG_PHY_TOP + 0x004C )
#define REG_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_ANA_USB30_CTRL0_TOP           ( CTL_BASE_ANLG_PHY_TOP + 0x0050 )
#define REG_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_ANA_USB30_CTRL1_TOP           ( CTL_BASE_ANLG_PHY_TOP + 0x0054 )
#define REG_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_ANA_USB30_CTRL3_TOP           ( CTL_BASE_ANLG_PHY_TOP + 0x0058 )
#define REG_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_REG_SEL_CFG_0                 ( CTL_BASE_ANLG_PHY_TOP + 0x005C )
#define REG_ANLG_PHY_TOP_ANALOG_MPLL1_MPLL1_CTRL0_TOP                    ( CTL_BASE_ANLG_PHY_TOP + 0x0060 )
#define REG_ANLG_PHY_TOP_ANALOG_MPLL1_REG_SEL_CFG_0                      ( CTL_BASE_ANLG_PHY_TOP + 0x0064 )
#define REG_ANLG_PHY_TOP_ANALOG_THM2_0_THM2_CTL_TOP                      ( CTL_BASE_ANLG_PHY_TOP + 0x0068 )
#define REG_ANLG_PHY_TOP_ANALOG_THM2_0_THM2_RESERVED_CTL_TOP             ( CTL_BASE_ANLG_PHY_TOP + 0x006C )
#define REG_ANLG_PHY_TOP_ANALOG_THM2_0_REG_SEL_CFG_0                     ( CTL_BASE_ANLG_PHY_TOP + 0x0070 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_ANA_BB_PWR_CTRL_TOP               ( CTL_BASE_ANLG_PHY_TOP + 0x0074 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_ANA_BB_RST_CTRL_TOP               ( CTL_BASE_ANLG_PHY_TOP + 0x0078 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_ANA_BB_EN_CTRL_TOP                ( CTL_BASE_ANLG_PHY_TOP + 0x007C )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_CTRL0_TOP                    ( CTL_BASE_ANLG_PHY_TOP + 0x0080 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_CTRL1_TOP                    ( CTL_BASE_ANLG_PHY_TOP + 0x0084 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_CTRL2_TOP                    ( CTL_BASE_ANLG_PHY_TOP + 0x0088 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_BIST_CTRL_TOP                ( CTL_BASE_ANLG_PHY_TOP + 0x008C )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_RESERVED_TOP                 ( CTL_BASE_ANLG_PHY_TOP + 0x0090 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_DLPLL_CALI_TOP                    ( CTL_BASE_ANLG_PHY_TOP + 0x0094 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_AAPC_CTRL1_TOP                    ( CTL_BASE_ANLG_PHY_TOP + 0x0098 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_AAPC_CTRL2_TOP                    ( CTL_BASE_ANLG_PHY_TOP + 0x009C )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_RTC4M_CTRL_TOP                    ( CTL_BASE_ANLG_PHY_TOP + 0x00A0 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_REG_SEL_CFG_0                     ( CTL_BASE_ANLG_PHY_TOP + 0x00A4 )
#define REG_ANLG_PHY_TOP_ANALOG_BB_TOP_REG_SEL_CFG_1                     ( CTL_BASE_ANLG_PHY_TOP + 0x00A8 )
#define REG_ANLG_PHY_TOP_ANALOG_THM1_THM1_CTL_TOP                        ( CTL_BASE_ANLG_PHY_TOP + 0x00AC )
#define REG_ANLG_PHY_TOP_ANALOG_THM1_THM1_RESERVED_CTL_TOP               ( CTL_BASE_ANLG_PHY_TOP + 0x00B0 )
#define REG_ANLG_PHY_TOP_ANALOG_THM1_REG_SEL_CFG_0                       ( CTL_BASE_ANLG_PHY_TOP + 0x00B4 )
#define REG_ANLG_PHY_TOP_ANALOG_EFUSE0_1_EFUSE_PIN_PW_CTL_TOP            ( CTL_BASE_ANLG_PHY_TOP + 0x00B8 )
#define REG_ANLG_PHY_TOP_ANALOG_EFUSE0_1_REG_SEL_CFG_0                   ( CTL_BASE_ANLG_PHY_TOP + 0x00BC )
#define REG_ANLG_PHY_TOP_ANALOG_EFUSE1_EFUSE_PIN_PW_CTL_TOP              ( CTL_BASE_ANLG_PHY_TOP + 0x00C0 )
#define REG_ANLG_PHY_TOP_ANALOG_EFUSE1_REG_SEL_CFG_0                     ( CTL_BASE_ANLG_PHY_TOP + 0x00C4 )
#define REG_ANLG_PHY_TOP_ANALOG_THM2_1_THM2_CTL_TOP                      ( CTL_BASE_ANLG_PHY_TOP + 0x00C8 )
#define REG_ANLG_PHY_TOP_ANALOG_THM2_1_THM2_RESERVED_CTL_TOP             ( CTL_BASE_ANLG_PHY_TOP + 0x00CC )
#define REG_ANLG_PHY_TOP_ANALOG_THM2_1_REG_SEL_CFG_0                     ( CTL_BASE_ANLG_PHY_TOP + 0x00D0 )

/* REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXCLKLANE_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXREQUESTHSCLK             BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSCLK                  BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSEXITCLK              BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_STOPSTATECLK               BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_ULPSACTIVENOTCLK           BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATA_0_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXREQUESTDATAHS_0          BIT(10)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXREQUESTESC_0             BIT(9)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXLPDTESC_0                BIT(8)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSESC_0                BIT(7)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSEXIT_0               BIT(6)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXTRIGGERESC_0(x)          (((x) & 0xF) << 2)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXVALIDESC_0               BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXREADYESC_0               BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATA_1_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXREQUESTDATAHS_1          BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXREQUESTESC_1             BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSESC_1                BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSEXIT_1               BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATA_2_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXREQUESTDATAHS_2          BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXREQUESTESC_2             BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSESC_2                BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSEXIT_2               BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATA_3_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXREQUESTDATAHS_3          BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXREQUESTESC_3             BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSESC_3                BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSEXIT_3               BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATAESC_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXDATAESC_0(x)             (((x) & 0xFF))

/* REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_STATE_RX_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_RXLPDTESC_0                BIT(5)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_RXTRIGGERESC_0(x)          (((x) & 0xF) << 1)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_RXVALIDESC_0               BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_ERR_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_ERRESC_0                   BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_ERRSYNCESC_0               BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_ERRCONTROL_0               BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_ERRCONTENTIONLP0_0         BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_ERRCONTENTIONLP1_0         BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_CTRL_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_PS_PD_S                    BIT(12)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_PS_PD_L                    BIT(11)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_SHUTDOWNZ                  BIT(10)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_RSTZ                       BIT(9)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_ENABLE_0                   BIT(8)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_ENABLE_1                   BIT(7)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_ENABLE_2                   BIT(6)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_ENABLE_3                   BIT(5)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_ENABLECLK                  BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_FORCEPLL                   BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_PLLLOCK                    BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_BISTON                     BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_BISTDONE                   BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_TEST_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TESTDIN(x)                 (((x) & 0xFF) << 11)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TESTDOUT(x)                (((x) & 0xFF) << 3)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TESTEN                     BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TESTCLK                    BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TESTCLR                    BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_4L_DATALANE_CTRL_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TURNREQUEST_0              BIT(5)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_DIRECTION_0                BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_STOPSTATEDATA_0            BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_STOPSTATEDATA_1            BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_STOPSTATEDATA_2            BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_STOPSTATEDATA_3            BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_MIPI_CTRL7_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXSKEWCALHS_0              BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXSKEWCALHS_1              BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXSKEWCALHS_2              BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_DSI_TXSKEWCALHS_3              BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXREQUESTHSCLK     BIT(31)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSCLK          BIT(30)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSEXITCLK      BIT(29)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXREQUESTDATAHS_0  BIT(28)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXREQUESTESC_0     BIT(27)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXLPDTESC_0        BIT(26)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSESC_0        BIT(25)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSEXIT_0       BIT(24)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXTRIGGERESC_0     BIT(23)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXVALIDESC_0       BIT(22)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXREQUESTDATAHS_1  BIT(21)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXREQUESTESC_1     BIT(20)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSESC_1        BIT(19)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSEXIT_1       BIT(18)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXREQUESTDATAHS_2  BIT(17)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXREQUESTESC_2     BIT(16)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSESC_2        BIT(15)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSEXIT_2       BIT(14)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXREQUESTDATAHS_3  BIT(13)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXREQUESTESC_3     BIT(12)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSESC_3        BIT(11)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXULPSEXIT_3       BIT(10)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXDATAESC_0        BIT(9)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_PS_PD_S            BIT(8)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_PS_PD_L            BIT(7)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_SHUTDOWNZ          BIT(6)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_RSTZ               BIT(5)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_ENABLE_0           BIT(4)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_ENABLE_1           BIT(3)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_ENABLE_2           BIT(2)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_ENABLE_3           BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_ENABLECLK          BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MIPI_DSI_4LANE_REG_SEL_CFG_1 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_FORCEPLL           BIT(9)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TESTDIN            BIT(8)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TESTEN             BIT(7)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TESTCLK            BIT(6)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TESTCLR            BIT(5)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TURNREQUEST_0      BIT(4)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXSKEWCALHS_0      BIT(3)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXSKEWCALHS_1      BIT(2)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXSKEWCALHS_2      BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MIPI_DSI_4LANE_DSI_TXSKEWCALHS_3      BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_26MBUF_26MBUF_CTRL0_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_26MBUF_REC_26MHZ_BUF_PD                       BIT(10)
#define BIT_ANLG_PHY_TOP_ANALOG_26MBUF_REC_26MHZ_RESERVED(x)                  (((x) & 0x3FF))

/* REG_ANLG_PHY_TOP_ANALOG_26MBUF_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_26MBUF_REC_26MHZ_BUF_PD               BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_26MBUF_REC_26MHZ_RESERVED             BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_USB20_USB20_TEST_PIN_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_USB20_USB20_VBUSVALID                         BIT(6)
#define BIT_ANLG_PHY_TOP_ANALOG_USB20_USB20_TESTDATAOUT(x)                    (((x) & 0xF) << 2)
#define BIT_ANLG_PHY_TOP_ANALOG_USB20_USB20_T2RCOMP                           BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_USB20_USB20_LPBK_END                          BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_USB20_USB20_UTMI_CTL1_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_USB20_USB20_SUSPENDM                          BIT(6)
#define BIT_ANLG_PHY_TOP_ANALOG_USB20_USB20_PORN                              BIT(5)
#define BIT_ANLG_PHY_TOP_ANALOG_USB20_USB20_RESET                             BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_USB20_USB20_RXERROR                           BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_USB20_USB20_BYPASS_FS                         BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_USB20_USB20_BYPASS_IN_DM                      BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_USB20_USB20_BYPASS_OUT_DP                     BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_USB20_USB20_UTMI_CTL2_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_USB20_USB20_DPPULLDOWN                        BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_USB20_USB20_DMPULLDOWN                        BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_USB20_USB20_SLEEPM                            BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_USB20_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_USB20_USB20_SUSPENDM                  BIT(7)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_USB20_USB20_PORN                      BIT(6)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_USB20_USB20_RESET                     BIT(5)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_USB20_USB20_BYPASS_FS                 BIT(4)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_USB20_USB20_BYPASS_IN_DM              BIT(3)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_USB20_USB20_DPPULLDOWN                BIT(2)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_USB20_USB20_DMPULLDOWN                BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_USB20_USB20_SLEEPM                    BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_ANA_USB30_CTRL0_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_USB30_BIST_EN                      BIT(6)
#define BIT_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_USB30_BIST_DONE                    BIT(5)
#define BIT_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_USB30_BIST_OK                      BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_USB30_TOUT_0                       BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_USB30_TOUT_1                       BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_USB30_TOUT_2                       BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_USB30_TOUT_3                       BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_ANA_USB30_CTRL1_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_USB30_RESET_N                      BIT(5)
#define BIT_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_USB30_TXMARGIN(x)                  (((x) & 0x7) << 2)
#define BIT_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_USB30_TXSWING                      BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_USB30_SW_SEL                       BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_ANA_USB30_CTRL3_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_USB30_PS_PD_S                      BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_USB30_PS_PD_L                      BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_USB30_ISO_SW_EN                    BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_USB3_TYPEC_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_USB3_TYPEC_USB30_RESET_N              BIT(6)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_USB3_TYPEC_USB30_TXMARGIN             BIT(5)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_USB3_TYPEC_USB30_TXSWING              BIT(4)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_USB3_TYPEC_USB30_SW_SEL               BIT(3)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_USB3_TYPEC_USB30_PS_PD_S              BIT(2)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_USB3_TYPEC_USB30_PS_PD_L              BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_USB3_TYPEC_USB30_ISO_SW_EN            BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MPLL1_MPLL1_CTRL0_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_MPLL1_MPLL1_PD                                BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_MPLL1_MPLL1_RST                               BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_MPLL1_MPLL1_CLKOUT_EN                         BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_MPLL1_MPLL1_DIV32_EN                          BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_MPLL1_MPLL1_LOCK_DONE                         BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_MPLL1_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MPLL1_MPLL1_PD                        BIT(3)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MPLL1_MPLL1_RST                       BIT(2)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MPLL1_MPLL1_CLKOUT_EN                 BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_MPLL1_MPLL1_DIV32_EN                  BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_THM2_0_THM2_CTL_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_THM2_0_THM_RSTN                               BIT(13)
#define BIT_ANLG_PHY_TOP_ANALOG_THM2_0_THM_RUN                                BIT(12)
#define BIT_ANLG_PHY_TOP_ANALOG_THM2_0_THM_PD                                 BIT(11)
#define BIT_ANLG_PHY_TOP_ANALOG_THM2_0_THM_VALID                              BIT(10)
#define BIT_ANLG_PHY_TOP_ANALOG_THM2_0_THM_DATA(x)                            (((x) & 0x3FF))

/* REG_ANLG_PHY_TOP_ANALOG_THM2_0_THM2_RESERVED_CTL_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_THM2_0_THM_RESERVED(x)                        (((x) & 0xFFFF))

/* REG_ANLG_PHY_TOP_ANALOG_THM2_0_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_THM2_0_THM_RSTN                       BIT(3)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_THM2_0_THM_RUN                        BIT(2)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_THM2_0_THM_PD                         BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_THM2_0_THM_RESERVED                   BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_ANA_BB_PWR_CTRL_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_PD                                BIT(5)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_REC_26MHZ_0_BUF_PD                     BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_REC_26MHZ_1_BUF_PD                     BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_S_AAPC_PD                              BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_M_AAPC_PD                              BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_DLPLL_PD                               BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_ANA_BB_RST_CTRL_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_RST                               BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RTC100M_RSTB                           BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_DLPLL_RST                              BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_ANA_BB_EN_CTRL_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_DIV_EN                            BIT(9)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_CLKOUT_EN                         BIT(8)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_CLK26MHZ_AUD_EN                        BIT(7)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RTC100M_EN                             BIT(6)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_SINDRV_ENA                             BIT(5)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_SINDRV_ENA_SQUARE                      BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_CLK26M_RESERVED(x)                     (((x) & 0xF))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_CTRL0_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_N(x)                              (((x) & 0x7FF) << 10)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_IBIAS(x)                          (((x) & 0x3) << 8)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_LPF(x)                            (((x) & 0x7) << 5)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_SDM_EN                            BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_MOD_EN                            BIT(3)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_DIV_S                             BIT(2)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_REF_SEL(x)                        (((x) & 0x3))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_CTRL1_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_NINT(x)                           (((x) & 0x7F) << 23)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_KINT(x)                           (((x) & 0x7FFFFF))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_CTRL2_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_BIST_EN                           BIT(7)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_26M_DIV(x)                        (((x) & 0x3F) << 1)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_LOCK_DONE                         BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_BIST_CTRL_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_BIST_CTRL(x)                      (((x) & 0xFF) << 16)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_BIST_CNT(x)                       (((x) & 0xFFFF))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_RESERVED_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RPLL_RESERVED(x)                       (((x) & 0x1FFFFF))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_DLPLL_CALI_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_DLPLL_TYPE_SEL                         BIT(17)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_DLPLL_BIST_CNT(x)                      (((x) & 0xFFFF) << 1)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_DLPLL_LOCK_DONE                        BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_AAPC_CTRL1_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_AAPC_G0(x)                             (((x) & 0x3) << 24)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_AAPC_G1(x)                             (((x) & 0x3) << 22)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_AAPC_G2(x)                             (((x) & 0x3) << 20)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_S_AAPC_LOW_V_CON                       BIT(19)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_S_AAPC_D(x)                            (((x) & 0x3FFF) << 5)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_S_AAPC_BPRES                           BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_S_APCOUT_SEL(x)                        (((x) & 0x3) << 2)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_S_AAPC_RESERVED(x)                     (((x) & 0x3))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_AAPC_CTRL2_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_M_AAPC_D(x)                            (((x) & 0x3FFF) << 6)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_M_AAPC_LOW_V_CON                       BIT(5)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_M_AAPC_BPRES                           BIT(4)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_M_APCOUT_SEL(x)                        (((x) & 0x3) << 2)
#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_M_AAPC_RESERVED(x)                     (((x) & 0x3))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_RTC4M_CTRL_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_BB_TOP_RTC100M_RC_C(x)                        (((x) & 0x7F))

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RPLL_PD                        BIT(31)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_REC_26MHZ_0_BUF_PD             BIT(30)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_REC_26MHZ_1_BUF_PD             BIT(29)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_S_AAPC_PD                      BIT(28)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_M_AAPC_PD                      BIT(27)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_DLPLL_PD                       BIT(26)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RPLL_RST                       BIT(25)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RTC100M_RSTB                   BIT(24)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_DLPLL_RST                      BIT(23)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RPLL_DIV_EN                    BIT(22)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RPLL_CLKOUT_EN                 BIT(21)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_CLK26MHZ_AUD_EN                BIT(20)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RTC100M_EN                     BIT(19)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_SINDRV_ENA                     BIT(18)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_SINDRV_ENA_SQUARE              BIT(17)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_CLK26M_RESERVED                BIT(16)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RPLL_N                         BIT(15)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RPLL_IBIAS                     BIT(14)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RPLL_LPF                       BIT(13)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RPLL_SDM_EN                    BIT(12)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RPLL_MOD_EN                    BIT(11)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RPLL_DIV_S                     BIT(10)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RPLL_REF_SEL                   BIT(9)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RPLL_NINT                      BIT(8)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RPLL_KINT                      BIT(7)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RPLL_BIST_EN                   BIT(6)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RPLL_26M_DIV                   BIT(5)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RPLL_BIST_CTRL                 BIT(4)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RPLL_RESERVED                  BIT(3)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_DLPLL_TYPE_SEL                 BIT(2)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_AAPC_G0                        BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_AAPC_G1                        BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_BB_TOP_REG_SEL_CFG_1 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_AAPC_G2                        BIT(11)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_S_AAPC_LOW_V_CON               BIT(10)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_S_AAPC_D                       BIT(9)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_S_AAPC_BPRES                   BIT(8)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_S_APCOUT_SEL                   BIT(7)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_S_AAPC_RESERVED                BIT(6)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_M_AAPC_D                       BIT(5)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_M_AAPC_LOW_V_CON               BIT(4)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_M_AAPC_BPRES                   BIT(3)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_M_APCOUT_SEL                   BIT(2)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_M_AAPC_RESERVED                BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_BB_TOP_RTC100M_RC_C                   BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_THM1_THM1_CTL_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_THM1_THM_RSTN                                 BIT(13)
#define BIT_ANLG_PHY_TOP_ANALOG_THM1_THM_RUN                                  BIT(12)
#define BIT_ANLG_PHY_TOP_ANALOG_THM1_THM_PD                                   BIT(11)
#define BIT_ANLG_PHY_TOP_ANALOG_THM1_THM_VALID                                BIT(10)
#define BIT_ANLG_PHY_TOP_ANALOG_THM1_THM_DATA(x)                              (((x) & 0x3FF))

/* REG_ANLG_PHY_TOP_ANALOG_THM1_THM1_RESERVED_CTL_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_THM1_THM_RESERVED(x)                          (((x) & 0xFFFF))

/* REG_ANLG_PHY_TOP_ANALOG_THM1_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_THM1_THM_RSTN                         BIT(3)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_THM1_THM_RUN                          BIT(2)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_THM1_THM_PD                           BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_THM1_THM_RESERVED                     BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_EFUSE0_1_EFUSE_PIN_PW_CTL_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_EFUSE0_1_EFS_ENK1                             BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_EFUSE0_1_EFS_ENK2                             BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_EFUSE0_1_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_EFUSE0_1_EFS_ENK1                     BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_EFUSE0_1_EFS_ENK2                     BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_EFUSE1_EFUSE_PIN_PW_CTL_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_EFUSE1_EFS_ENK1                               BIT(1)
#define BIT_ANLG_PHY_TOP_ANALOG_EFUSE1_EFS_ENK2                               BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_EFUSE1_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_EFUSE1_EFS_ENK1                       BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_EFUSE1_EFS_ENK2                       BIT(0)

/* REG_ANLG_PHY_TOP_ANALOG_THM2_1_THM2_CTL_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_THM2_1_THM_RSTN                               BIT(13)
#define BIT_ANLG_PHY_TOP_ANALOG_THM2_1_THM_RUN                                BIT(12)
#define BIT_ANLG_PHY_TOP_ANALOG_THM2_1_THM_PD                                 BIT(11)
#define BIT_ANLG_PHY_TOP_ANALOG_THM2_1_THM_VALID                              BIT(10)
#define BIT_ANLG_PHY_TOP_ANALOG_THM2_1_THM_DATA(x)                            (((x) & 0x3FF))

/* REG_ANLG_PHY_TOP_ANALOG_THM2_1_THM2_RESERVED_CTL_TOP */

#define BIT_ANLG_PHY_TOP_ANALOG_THM2_1_THM_RESERVED(x)                        (((x) & 0xFFFF))

/* REG_ANLG_PHY_TOP_ANALOG_THM2_1_REG_SEL_CFG_0 */

#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_THM2_1_THM_RSTN                       BIT(3)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_THM2_1_THM_RUN                        BIT(2)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_THM2_1_THM_PD                         BIT(1)
#define BIT_ANLG_PHY_TOP_DBG_SEL_ANALOG_THM2_1_THM_RESERVED                   BIT(0)


#endif /* ANLG_PHY_TOP_H */


