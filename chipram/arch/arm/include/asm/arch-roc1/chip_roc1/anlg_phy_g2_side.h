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

#ifndef ANLG_PHY_G2_SIDE_H
#define ANLG_PHY_G2_SIDE_H

#define CTL_BASE_ANLG_PHY_G2_SIDE 0x323A0000

#define REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXCLKLANE      ( CTL_BASE_ANLG_PHY_G2_SIDE + 0x0000 )
#define REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATA_0       ( CTL_BASE_ANLG_PHY_G2_SIDE + 0x0004 )
#define REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATA_1       ( CTL_BASE_ANLG_PHY_G2_SIDE + 0x0008 )
#define REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATA_2       ( CTL_BASE_ANLG_PHY_G2_SIDE + 0x000C )
#define REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATA_3       ( CTL_BASE_ANLG_PHY_G2_SIDE + 0x0010 )
#define REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATAESC      ( CTL_BASE_ANLG_PHY_G2_SIDE + 0x0014 )
#define REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_STATE_RX       ( CTL_BASE_ANLG_PHY_G2_SIDE + 0x0018 )
#define REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_ERR            ( CTL_BASE_ANLG_PHY_G2_SIDE + 0x001C )
#define REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_CTRL           ( CTL_BASE_ANLG_PHY_G2_SIDE + 0x0020 )
#define REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_RSVD           ( CTL_BASE_ANLG_PHY_G2_SIDE + 0x0024 )
#define REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_TEST           ( CTL_BASE_ANLG_PHY_G2_SIDE + 0x0028 )
#define REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_DATALANE_CTRL  ( CTL_BASE_ANLG_PHY_G2_SIDE + 0x002C )
#define REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_DUMY_CTRL         ( CTL_BASE_ANLG_PHY_G2_SIDE + 0x0030 )
#define REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_MIPI_CTRL7            ( CTL_BASE_ANLG_PHY_G2_SIDE + 0x0034 )

/* REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATA_1 */

#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TXLPDTESC_1         BIT(8)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TXTRIGGERESC_1(x)   (((x) & 0xF) << 2)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TXVALIDESC_1        BIT(1)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TXREADYESC_1        BIT(0)

/* REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATA_2 */

#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TXLPDTESC_2         BIT(8)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TXTRIGGERESC_2(x)   (((x) & 0xF) << 2)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TXVALIDESC_2        BIT(1)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TXREADYESC_2        BIT(0)

/* REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATA_3 */

#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TXLPDTESC_3         BIT(8)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TXTRIGGERESC_3(x)   (((x) & 0xF) << 2)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TXVALIDESC_3        BIT(1)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TXREADYESC_3        BIT(0)

/* REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_TXDATAESC */

#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TXDATAESC_1(x)      (((x) & 0xFF) << 16)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TXDATAESC_2(x)      (((x) & 0xFF) << 8)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TXDATAESC_3(x)      (((x) & 0xFF))

/* REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_STATE_RX */

#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_RXLPDTESC_1         BIT(17)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_RXTRIGGERESC_1(x)   (((x) & 0xF) << 13)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_RXVALIDESC_1        BIT(12)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_RXLPDTESC_2         BIT(11)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_RXTRIGGERESC_2(x)   (((x) & 0xF) << 7)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_RXVALIDESC_2        BIT(6)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_RXLPDTESC_3         BIT(5)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_RXTRIGGERESC_3(x)   (((x) & 0xF) << 1)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_RXVALIDESC_3        BIT(0)

/* REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_ERR */

#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_ERRESC_1            BIT(14)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_ERRSYNCESC_1        BIT(13)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_ERRCONTROL_1        BIT(12)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_ERRCONTENTIONLP0_1  BIT(11)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_ERRCONTENTIONLP1_1  BIT(10)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_ERRESC_2            BIT(9)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_ERRSYNCESC_2        BIT(8)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_ERRCONTROL_2        BIT(7)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_ERRCONTENTIONLP0_2  BIT(6)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_ERRCONTENTIONLP1_2  BIT(5)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_ERRESC_3            BIT(4)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_ERRSYNCESC_3        BIT(3)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_ERRCONTROL_3        BIT(2)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_ERRCONTENTIONLP0_3  BIT(1)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_ERRCONTENTIONLP1_3  BIT(0)

/* REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_CTRL */

#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_BISTON              BIT(10)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_BISTDONE            BIT(9)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_IF_SEL              BIT(8)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TRIMBG(x)           (((x) & 0xF) << 4)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TX_RCTL(x)          (((x) & 0xF))

/* REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_RSVD */

#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_RESERVED(x)         (((x) & 0xFF) << 8)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_RESERVEDO(x)        (((x) & 0xFF))

/* REG_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_4L_DATALANE_CTRL */

#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TURNDISABLE_0       BIT(20)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_FORCERXMODE_0       BIT(19)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_FORCETXSTOPMODE_0   BIT(18)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TURNREQUEST_1       BIT(16)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TURNDISABLE_1       BIT(15)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_FORCERXMODE_1       BIT(14)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_FORCETXSTOPMODE_1   BIT(13)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TURNREQUEST_2       BIT(11)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_DIRECTION_2         BIT(10)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TURNDISABLE_2       BIT(9)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_FORCERXMODE_2       BIT(8)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_FORCETXSTOPMODE_2   BIT(7)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TURNREQUEST_3       BIT(5)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_DIRECTION_3         BIT(4)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_TURNDISABLE_3       BIT(3)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_FORCERXMODE_3       BIT(2)
#define BIT_ANLG_PHY_G2_SIDE_ANALOG_MIPI_DSI_4LANE_DSI_FORCETXSTOPMODE_3   BIT(1)

#endif
