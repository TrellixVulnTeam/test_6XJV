/*
 * Copyright (C) 2018 Spreadtrum Communications Inc.
 *
 * This file is dual-licensed: you can use it either under the terms
 * of the GPL or the X11 license, at your option. Note that this dual
 * licensing only applies to this file, and not this project as a
 * whole.
 *
 * updated at 2018-08-13 18:52:21
 *
 */

#ifndef REG_FW2_AON_H
#define REG_FW2_AON_H

#define CTL_BASE_REG_FW2_AON 0x32806000

#define REG_REG_FW2_AON_REG_RD_CTRL_0         ( CTL_BASE_REG_FW2_AON + 0x0000 )
#define REG_REG_FW2_AON_REG_RD_CTRL_1         ( CTL_BASE_REG_FW2_AON + 0x0004 )
#define REG_REG_FW2_AON_REG_RD_CTRL_2         ( CTL_BASE_REG_FW2_AON + 0x0008 )
#define REG_REG_FW2_AON_REG_RD_CTRL_3         ( CTL_BASE_REG_FW2_AON + 0x000C )
#define REG_REG_FW2_AON_REG_RD_CTRL_4         ( CTL_BASE_REG_FW2_AON + 0x0010 )
#define REG_REG_FW2_AON_REG_RD_CTRL_5         ( CTL_BASE_REG_FW2_AON + 0x0014 )
#define REG_REG_FW2_AON_REG_RD_CTRL_6         ( CTL_BASE_REG_FW2_AON + 0x0018 )
#define REG_REG_FW2_AON_REG_WR_CTRL_0         ( CTL_BASE_REG_FW2_AON + 0x001C )
#define REG_REG_FW2_AON_REG_WR_CTRL_1         ( CTL_BASE_REG_FW2_AON + 0x0020 )
#define REG_REG_FW2_AON_REG_WR_CTRL_2         ( CTL_BASE_REG_FW2_AON + 0x0024 )
#define REG_REG_FW2_AON_REG_WR_CTRL_3         ( CTL_BASE_REG_FW2_AON + 0x0028 )
#define REG_REG_FW2_AON_REG_WR_CTRL_4         ( CTL_BASE_REG_FW2_AON + 0x002C )
#define REG_REG_FW2_AON_REG_WR_CTRL_5         ( CTL_BASE_REG_FW2_AON + 0x0030 )
#define REG_REG_FW2_AON_REG_WR_CTRL_6         ( CTL_BASE_REG_FW2_AON + 0x0034 )
#define REG_REG_FW2_AON_BIT_CTRL_ADDR_ARRAY0  ( CTL_BASE_REG_FW2_AON + 0x0038 )
#define REG_REG_FW2_AON_BIT_CTRL_ARRAY0       ( CTL_BASE_REG_FW2_AON + 0x003C )

/* REG_REG_FW2_AON_REG_RD_CTRL_0 */

#define BIT_REG_FW2_AON_AUD_SCLK_RD_SEC              BIT(31)
#define BIT_REG_FW2_AON_LCM_RSTN_RD_SEC              BIT(30)
#define BIT_REG_FW2_AON_AUD_DASYNC_RD_SEC            BIT(29)
#define BIT_REG_FW2_AON_PTEST_RD_SEC                 BIT(28)
#define BIT_REG_FW2_AON_DCDC_ARM1_EN_RD_SEC          BIT(27)
#define BIT_REG_FW2_AON_AUD_ADSYNC_RD_SEC            BIT(26)
#define BIT_REG_FW2_AON_CHIP_SLEEP_RD_SEC            BIT(25)
#define BIT_REG_FW2_AON_EMMC_D7_RD_SEC               BIT(24)
#define BIT_REG_FW2_AON_EMMC_D6_RD_SEC               BIT(23)
#define BIT_REG_FW2_AON_EMMC_D4_RD_SEC               BIT(22)
#define BIT_REG_FW2_AON_EMMC_D1_RD_SEC               BIT(21)
#define BIT_REG_FW2_AON_EMMC_DS_RD_SEC               BIT(20)
#define BIT_REG_FW2_AON_EMMC_CLK_RD_SEC              BIT(19)
#define BIT_REG_FW2_AON_EMMC_D5_RD_SEC               BIT(18)
#define BIT_REG_FW2_AON_EMMC_D2_RD_SEC               BIT(17)
#define BIT_REG_FW2_AON_EMMC_D3_RD_SEC               BIT(16)
#define BIT_REG_FW2_AON_EMMC_D0_RD_SEC               BIT(15)
#define BIT_REG_FW2_AON_EMMC_CMD_RD_SEC              BIT(14)
#define BIT_REG_FW2_AON_EMMC_RST_RD_SEC              BIT(13)
#define BIT_REG_FW2_AON_PIN_CTRL_REG5_RD_SEC         BIT(12)
#define BIT_REG_FW2_AON_PIN_CTRL_REG4_RD_SEC         BIT(11)
#define BIT_REG_FW2_AON_PIN_CTRL_REG3_RD_SEC         BIT(10)
#define BIT_REG_FW2_AON_PIN_CTRL_REG2_RD_SEC         BIT(9)
#define BIT_REG_FW2_AON_PIN_CTRL_REG1_RD_SEC         BIT(8)
#define BIT_REG_FW2_AON_PIN_CTRL_REG0_RD_SEC         BIT(7)
#define BIT_REG_FW2_AON_IIC_MATRIX_MTX_CFG_RD_SEC    BIT(6)
#define BIT_REG_FW2_AON_SPI_MATRIX_MTX_CFG_RD_SEC    BIT(5)
#define BIT_REG_FW2_AON_SIM_MATRIX_MTX_CFG_RD_SEC    BIT(4)
#define BIT_REG_FW2_AON_IIS_MATRIX_MTX_CFG_RD_SEC    BIT(3)
#define BIT_REG_FW2_AON_UART_MATRIX_MTX_CFG1_RD_SEC  BIT(2)
#define BIT_REG_FW2_AON_UART_MATRIX_MTX_CFG_RD_SEC   BIT(1)
#define BIT_REG_FW2_AON_PWR_PAD_CTL_RESERVED_RD_SEC  BIT(0)

/* REG_REG_FW2_AON_REG_RD_CTRL_1 */

#define BIT_REG_FW2_AON_SD2_CLK_RD_SEC               BIT(31)
#define BIT_REG_FW2_AON_SD2_D_1_RD_SEC               BIT(30)
#define BIT_REG_FW2_AON_SD2_D_0_RD_SEC               BIT(29)
#define BIT_REG_FW2_AON_SD2_CMD_RD_SEC               BIT(28)
#define BIT_REG_FW2_AON_SD0_CLK_RD_SEC               BIT(27)
#define BIT_REG_FW2_AON_SD0_D_1_RD_SEC               BIT(26)
#define BIT_REG_FW2_AON_SD0_D_0_RD_SEC               BIT(25)
#define BIT_REG_FW2_AON_SD0_CMD_RD_SEC               BIT(24)
#define BIT_REG_FW2_AON_SD0_D_2_RD_SEC               BIT(23)
#define BIT_REG_FW2_AON_SD0_D_3_RD_SEC               BIT(22)
#define BIT_REG_FW2_AON_SIMRST1_RD_SEC               BIT(21)
#define BIT_REG_FW2_AON_SIMDA1_RD_SEC                BIT(20)
#define BIT_REG_FW2_AON_SIMCLK1_RD_SEC               BIT(19)
#define BIT_REG_FW2_AON_SIMRST0_RD_SEC               BIT(18)
#define BIT_REG_FW2_AON_SIMDA0_RD_SEC                BIT(17)
#define BIT_REG_FW2_AON_SIMCLK0_RD_SEC               BIT(16)
#define BIT_REG_FW2_AON_SIMRST2_RD_SEC               BIT(15)
#define BIT_REG_FW2_AON_SIMDA2_RD_SEC                BIT(14)
#define BIT_REG_FW2_AON_SIMCLK2_RD_SEC               BIT(13)
#define BIT_REG_FW2_AON_AUD_DAD0_RD_SEC              BIT(12)
#define BIT_REG_FW2_AON_ADI_SCLK_RD_SEC              BIT(11)
#define BIT_REG_FW2_AON_AUD_DAD1_RD_SEC              BIT(10)
#define BIT_REG_FW2_AON_ANA_INT_RD_SEC               BIT(9)
#define BIT_REG_FW2_AON_EXT_RST_B_RD_SEC             BIT(8)
#define BIT_REG_FW2_AON_CLK_32K_RD_SEC               BIT(7)
#define BIT_REG_FW2_AON_AI_EN_RD_SEC                 BIT(6)
#define BIT_REG_FW2_AON_XTL_EN0_RD_SEC               BIT(5)
#define BIT_REG_FW2_AON_XTL_EN1_RD_SEC               BIT(4)
#define BIT_REG_FW2_AON_ADI_D_RD_SEC                 BIT(3)
#define BIT_REG_FW2_AON_DCDC_ARM0_EN_RD_SEC          BIT(2)
#define BIT_REG_FW2_AON_AUD_ADD0_RD_SEC              BIT(1)
#define BIT_REG_FW2_AON_DSI_TE_RD_SEC                BIT(0)

/* REG_REG_FW2_AON_REG_RD_CTRL_2 */

#define BIT_REG_FW2_AON_MTCK_ARM_RD_SEC              BIT(31)
#define BIT_REG_FW2_AON_SDA6_RD_SEC                  BIT(30)
#define BIT_REG_FW2_AON_SCL6_RD_SEC                  BIT(29)
#define BIT_REG_FW2_AON_U2RXD_RD_SEC                 BIT(28)
#define BIT_REG_FW2_AON_U2TXD_RD_SEC                 BIT(27)
#define BIT_REG_FW2_AON_DRTCK_RD_SEC                 BIT(26)
#define BIT_REG_FW2_AON_DTCK_RD_SEC                  BIT(25)
#define BIT_REG_FW2_AON_DTMS_RD_SEC                  BIT(24)
#define BIT_REG_FW2_AON_DTDI_RD_SEC                  BIT(23)
#define BIT_REG_FW2_AON_DTDO_RD_SEC                  BIT(22)
#define BIT_REG_FW2_AON_RFCTL_19_RD_SEC              BIT(21)
#define BIT_REG_FW2_AON_RFCTL_18_RD_SEC              BIT(20)
#define BIT_REG_FW2_AON_RFCTL_17_RD_SEC              BIT(19)
#define BIT_REG_FW2_AON_RFCTL_16_RD_SEC              BIT(18)
#define BIT_REG_FW2_AON_RFCTL_15_RD_SEC              BIT(17)
#define BIT_REG_FW2_AON_RFCTL_14_RD_SEC              BIT(16)
#define BIT_REG_FW2_AON_RFCTL_13_RD_SEC              BIT(15)
#define BIT_REG_FW2_AON_RFCTL_12_RD_SEC              BIT(14)
#define BIT_REG_FW2_AON_RFCTL_11_RD_SEC              BIT(13)
#define BIT_REG_FW2_AON_RFCTL_10_RD_SEC              BIT(12)
#define BIT_REG_FW2_AON_RFCTL_9_RD_SEC               BIT(11)
#define BIT_REG_FW2_AON_RFCTL_8_RD_SEC               BIT(10)
#define BIT_REG_FW2_AON_RFCTL_7_RD_SEC               BIT(9)
#define BIT_REG_FW2_AON_RFCTL_6_RD_SEC               BIT(8)
#define BIT_REG_FW2_AON_RFCTL_5_RD_SEC               BIT(7)
#define BIT_REG_FW2_AON_RFCTL_4_RD_SEC               BIT(6)
#define BIT_REG_FW2_AON_RFCTL_3_RD_SEC               BIT(5)
#define BIT_REG_FW2_AON_RFCTL_2_RD_SEC               BIT(4)
#define BIT_REG_FW2_AON_RFCTL_1_RD_SEC               BIT(3)
#define BIT_REG_FW2_AON_RFCTL_0_RD_SEC               BIT(2)
#define BIT_REG_FW2_AON_SD2_D_3_RD_SEC               BIT(1)
#define BIT_REG_FW2_AON_SD2_D_2_RD_SEC               BIT(0)

/* REG_REG_FW2_AON_REG_RD_CTRL_3 */

#define BIT_REG_FW2_AON_U1RXD_RD_SEC                 BIT(31)
#define BIT_REG_FW2_AON_U1TXD_RD_SEC                 BIT(30)
#define BIT_REG_FW2_AON_SPI3_DI_RD_SEC               BIT(29)
#define BIT_REG_FW2_AON_SPI3_CLK_RD_SEC              BIT(28)
#define BIT_REG_FW2_AON_SPI3_CSN_RD_SEC              BIT(27)
#define BIT_REG_FW2_AON_SPI3_DO_RD_SEC               BIT(26)
#define BIT_REG_FW2_AON_KEYIN_2_RD_SEC               BIT(25)
#define BIT_REG_FW2_AON_KEYIN_1_RD_SEC               BIT(24)
#define BIT_REG_FW2_AON_KEYIN_0_RD_SEC               BIT(23)
#define BIT_REG_FW2_AON_KEYOUT_2_RD_SEC              BIT(22)
#define BIT_REG_FW2_AON_KEYOUT_1_RD_SEC              BIT(21)
#define BIT_REG_FW2_AON_KEYOUT_0_RD_SEC              BIT(20)
#define BIT_REG_FW2_AON_SDA2_RD_SEC                  BIT(19)
#define BIT_REG_FW2_AON_SCL2_RD_SEC                  BIT(18)
#define BIT_REG_FW2_AON_RFFE1_SDA_RD_SEC             BIT(17)
#define BIT_REG_FW2_AON_RFFE1_SCK_RD_SEC             BIT(16)
#define BIT_REG_FW2_AON_RFFE0_SDA_RD_SEC             BIT(15)
#define BIT_REG_FW2_AON_RFFE0_SCK_RD_SEC             BIT(14)
#define BIT_REG_FW2_AON_LVDS0_DAC_ON_RD_SEC          BIT(13)
#define BIT_REG_FW2_AON_LVDS0_ADC_ON_RD_SEC          BIT(12)
#define BIT_REG_FW2_AON_RFSEN1_RD_SEC                BIT(11)
#define BIT_REG_FW2_AON_RFSDA1_RD_SEC                BIT(10)
#define BIT_REG_FW2_AON_RFSCK1_RD_SEC                BIT(9)
#define BIT_REG_FW2_AON_RFSEN0_RD_SEC                BIT(8)
#define BIT_REG_FW2_AON_RFSDA0_RD_SEC                BIT(7)
#define BIT_REG_FW2_AON_RFSCK0_RD_SEC                BIT(6)
#define BIT_REG_FW2_AON_USB30_SW_RD_SEC              BIT(5)
#define BIT_REG_FW2_AON_U0RTS_RD_SEC                 BIT(4)
#define BIT_REG_FW2_AON_U0CTS_RD_SEC                 BIT(3)
#define BIT_REG_FW2_AON_U0RXD_RD_SEC                 BIT(2)
#define BIT_REG_FW2_AON_U0TXD_RD_SEC                 BIT(1)
#define BIT_REG_FW2_AON_MTMS_ARM_RD_SEC              BIT(0)

/* REG_REG_FW2_AON_REG_RD_CTRL_4 */

#define BIT_REG_FW2_AON_CMPD1_RD_SEC                 BIT(31)
#define BIT_REG_FW2_AON_CMPD0_RD_SEC                 BIT(30)
#define BIT_REG_FW2_AON_CMRST1_RD_SEC                BIT(29)
#define BIT_REG_FW2_AON_CMRST0_RD_SEC                BIT(28)
#define BIT_REG_FW2_AON_CMMCLK1_RD_SEC               BIT(27)
#define BIT_REG_FW2_AON_CMMCLK0_RD_SEC               BIT(26)
#define BIT_REG_FW2_AON_SCL1_RD_SEC                  BIT(25)
#define BIT_REG_FW2_AON_SDA1_RD_SEC                  BIT(24)
#define BIT_REG_FW2_AON_SDA0_RD_SEC                  BIT(23)
#define BIT_REG_FW2_AON_SCL0_RD_SEC                  BIT(22)
#define BIT_REG_FW2_AON_SPI2_CLK_RD_SEC              BIT(21)
#define BIT_REG_FW2_AON_SPI2_DI_RD_SEC               BIT(20)
#define BIT_REG_FW2_AON_SPI2_DO_RD_SEC               BIT(19)
#define BIT_REG_FW2_AON_SPI2_CSN_RD_SEC              BIT(18)
#define BIT_REG_FW2_AON_SDA7_RD_SEC                  BIT(17)
#define BIT_REG_FW2_AON_SCL7_RD_SEC                  BIT(16)
#define BIT_REG_FW2_AON_IIS1LRCK_RD_SEC              BIT(15)
#define BIT_REG_FW2_AON_IIS1CLK_RD_SEC               BIT(14)
#define BIT_REG_FW2_AON_IIS1DO_RD_SEC                BIT(13)
#define BIT_REG_FW2_AON_IIS1DI_RD_SEC                BIT(12)
#define BIT_REG_FW2_AON_EXTINT10_RD_SEC              BIT(11)
#define BIT_REG_FW2_AON_EXTINT9_RD_SEC               BIT(10)
#define BIT_REG_FW2_AON_SPI0_CLK_RD_SEC              BIT(9)
#define BIT_REG_FW2_AON_SPI0_DI_RD_SEC               BIT(8)
#define BIT_REG_FW2_AON_SPI0_DO_RD_SEC               BIT(7)
#define BIT_REG_FW2_AON_SPI0_CSN_RD_SEC              BIT(6)
#define BIT_REG_FW2_AON_SCL3_RD_SEC                  BIT(5)
#define BIT_REG_FW2_AON_SDA3_RD_SEC                  BIT(4)
#define BIT_REG_FW2_AON_EXTINT1_RD_SEC               BIT(3)
#define BIT_REG_FW2_AON_EXTINT0_RD_SEC               BIT(2)
#define BIT_REG_FW2_AON_PWMC_RD_SEC                  BIT(1)
#define BIT_REG_FW2_AON_SPI_TE_RD_SEC                BIT(0)

/* REG_REG_FW2_AON_REG_RD_CTRL_5 */

#define BIT_REG_FW2_AON_ESESPI_CLK_RD_SEC            BIT(31)
#define BIT_REG_FW2_AON_ESESPI_CSN_RD_SEC            BIT(30)
#define BIT_REG_FW2_AON_ESE_SWP_RD_SEC               BIT(29)
#define BIT_REG_FW2_AON_ESE_GPIO_RD_SEC              BIT(28)
#define BIT_REG_FW2_AON_SD1_D_3_RD_SEC               BIT(27)
#define BIT_REG_FW2_AON_SD1_D_2_RD_SEC               BIT(26)
#define BIT_REG_FW2_AON_SD1_D_1_RD_SEC               BIT(25)
#define BIT_REG_FW2_AON_SD1_D_0_RD_SEC               BIT(24)
#define BIT_REG_FW2_AON_SD1_CMD_RD_SEC               BIT(23)
#define BIT_REG_FW2_AON_SD1_CLK_RD_SEC               BIT(22)
#define BIT_REG_FW2_AON_U7RTS_RD_SEC                 BIT(21)
#define BIT_REG_FW2_AON_U7CTS_RD_SEC                 BIT(20)
#define BIT_REG_FW2_AON_U7RXD_RD_SEC                 BIT(19)
#define BIT_REG_FW2_AON_U7TXD_RD_SEC                 BIT(18)
#define BIT_REG_FW2_AON_U4RTS_RD_SEC                 BIT(17)
#define BIT_REG_FW2_AON_U4CTS_RD_SEC                 BIT(16)
#define BIT_REG_FW2_AON_U4RXD_RD_SEC                 BIT(15)
#define BIT_REG_FW2_AON_U4TXD_RD_SEC                 BIT(14)
#define BIT_REG_FW2_AON_MEMS_MIC_DATA1_RD_SEC        BIT(13)
#define BIT_REG_FW2_AON_MEMS_MIC_CLK1_RD_SEC         BIT(12)
#define BIT_REG_FW2_AON_MEMS_MIC_DATA0_RD_SEC        BIT(11)
#define BIT_REG_FW2_AON_MEMS_MIC_CLK0_RD_SEC         BIT(10)
#define BIT_REG_FW2_AON_CLK_AUX0_RD_SEC              BIT(9)
#define BIT_REG_FW2_AON_U5RXD_RD_SEC                 BIT(8)
#define BIT_REG_FW2_AON_U5TXD_RD_SEC                 BIT(7)
#define BIT_REG_FW2_AON_IIS0LRCK_RD_SEC              BIT(6)
#define BIT_REG_FW2_AON_IIS0CLK_RD_SEC               BIT(5)
#define BIT_REG_FW2_AON_IIS0DO_RD_SEC                BIT(4)
#define BIT_REG_FW2_AON_IIS0DI_RD_SEC                BIT(3)
#define BIT_REG_FW2_AON_CMRST2_RD_SEC                BIT(2)
#define BIT_REG_FW2_AON_CMPD2_RD_SEC                 BIT(1)
#define BIT_REG_FW2_AON_CMMCLK2_RD_SEC               BIT(0)

/* REG_REG_FW2_AON_REG_RD_CTRL_6 */

#define BIT_REG_FW2_AON_ESESPI_D0_RD_SEC             BIT(7)
#define BIT_REG_FW2_AON_ESESPI_D1_RD_SEC             BIT(6)
#define BIT_REG_FW2_AON_ESESPI_D2_RD_SEC             BIT(5)
#define BIT_REG_FW2_AON_ESESPI_D3_RD_SEC             BIT(4)
#define BIT_REG_FW2_AON_ESESPI_D4_RD_SEC             BIT(3)
#define BIT_REG_FW2_AON_ESESPI_D5_RD_SEC             BIT(2)
#define BIT_REG_FW2_AON_ESESPI_D6_RD_SEC             BIT(1)
#define BIT_REG_FW2_AON_ESESPI_D7_RD_SEC             BIT(0)

/* REG_REG_FW2_AON_REG_WR_CTRL_0 */

#define BIT_REG_FW2_AON_AUD_SCLK_WR_SEC              BIT(31)
#define BIT_REG_FW2_AON_LCM_RSTN_WR_SEC              BIT(30)
#define BIT_REG_FW2_AON_AUD_DASYNC_WR_SEC            BIT(29)
#define BIT_REG_FW2_AON_PTEST_WR_SEC                 BIT(28)
#define BIT_REG_FW2_AON_DCDC_ARM1_EN_WR_SEC          BIT(27)
#define BIT_REG_FW2_AON_AUD_ADSYNC_WR_SEC            BIT(26)
#define BIT_REG_FW2_AON_CHIP_SLEEP_WR_SEC            BIT(25)
#define BIT_REG_FW2_AON_EMMC_D7_WR_SEC               BIT(24)
#define BIT_REG_FW2_AON_EMMC_D6_WR_SEC               BIT(23)
#define BIT_REG_FW2_AON_EMMC_D4_WR_SEC               BIT(22)
#define BIT_REG_FW2_AON_EMMC_D1_WR_SEC               BIT(21)
#define BIT_REG_FW2_AON_EMMC_DS_WR_SEC               BIT(20)
#define BIT_REG_FW2_AON_EMMC_CLK_WR_SEC              BIT(19)
#define BIT_REG_FW2_AON_EMMC_D5_WR_SEC               BIT(18)
#define BIT_REG_FW2_AON_EMMC_D2_WR_SEC               BIT(17)
#define BIT_REG_FW2_AON_EMMC_D3_WR_SEC               BIT(16)
#define BIT_REG_FW2_AON_EMMC_D0_WR_SEC               BIT(15)
#define BIT_REG_FW2_AON_EMMC_CMD_WR_SEC              BIT(14)
#define BIT_REG_FW2_AON_EMMC_RST_WR_SEC              BIT(13)
#define BIT_REG_FW2_AON_PIN_CTRL_REG5_WR_SEC         BIT(12)
#define BIT_REG_FW2_AON_PIN_CTRL_REG4_WR_SEC         BIT(11)
#define BIT_REG_FW2_AON_PIN_CTRL_REG3_WR_SEC         BIT(10)
#define BIT_REG_FW2_AON_PIN_CTRL_REG2_WR_SEC         BIT(9)
#define BIT_REG_FW2_AON_PIN_CTRL_REG1_WR_SEC         BIT(8)
#define BIT_REG_FW2_AON_PIN_CTRL_REG0_WR_SEC         BIT(7)
#define BIT_REG_FW2_AON_IIC_MATRIX_MTX_CFG_WR_SEC    BIT(6)
#define BIT_REG_FW2_AON_SPI_MATRIX_MTX_CFG_WR_SEC    BIT(5)
#define BIT_REG_FW2_AON_SIM_MATRIX_MTX_CFG_WR_SEC    BIT(4)
#define BIT_REG_FW2_AON_IIS_MATRIX_MTX_CFG_WR_SEC    BIT(3)
#define BIT_REG_FW2_AON_UART_MATRIX_MTX_CFG1_WR_SEC  BIT(2)
#define BIT_REG_FW2_AON_UART_MATRIX_MTX_CFG_WR_SEC   BIT(1)
#define BIT_REG_FW2_AON_PWR_PAD_CTL_RESERVED_WR_SEC  BIT(0)

/* REG_REG_FW2_AON_REG_WR_CTRL_1 */

#define BIT_REG_FW2_AON_SD2_CLK_WR_SEC               BIT(31)
#define BIT_REG_FW2_AON_SD2_D_1_WR_SEC               BIT(30)
#define BIT_REG_FW2_AON_SD2_D_0_WR_SEC               BIT(29)
#define BIT_REG_FW2_AON_SD2_CMD_WR_SEC               BIT(28)
#define BIT_REG_FW2_AON_SD0_CLK_WR_SEC               BIT(27)
#define BIT_REG_FW2_AON_SD0_D_1_WR_SEC               BIT(26)
#define BIT_REG_FW2_AON_SD0_D_0_WR_SEC               BIT(25)
#define BIT_REG_FW2_AON_SD0_CMD_WR_SEC               BIT(24)
#define BIT_REG_FW2_AON_SD0_D_2_WR_SEC               BIT(23)
#define BIT_REG_FW2_AON_SD0_D_3_WR_SEC               BIT(22)
#define BIT_REG_FW2_AON_SIMRST1_WR_SEC               BIT(21)
#define BIT_REG_FW2_AON_SIMDA1_WR_SEC                BIT(20)
#define BIT_REG_FW2_AON_SIMCLK1_WR_SEC               BIT(19)
#define BIT_REG_FW2_AON_SIMRST0_WR_SEC               BIT(18)
#define BIT_REG_FW2_AON_SIMDA0_WR_SEC                BIT(17)
#define BIT_REG_FW2_AON_SIMCLK0_WR_SEC               BIT(16)
#define BIT_REG_FW2_AON_SIMRST2_WR_SEC               BIT(15)
#define BIT_REG_FW2_AON_SIMDA2_WR_SEC                BIT(14)
#define BIT_REG_FW2_AON_SIMCLK2_WR_SEC               BIT(13)
#define BIT_REG_FW2_AON_AUD_DAD0_WR_SEC              BIT(12)
#define BIT_REG_FW2_AON_ADI_SCLK_WR_SEC              BIT(11)
#define BIT_REG_FW2_AON_AUD_DAD1_WR_SEC              BIT(10)
#define BIT_REG_FW2_AON_ANA_INT_WR_SEC               BIT(9)
#define BIT_REG_FW2_AON_EXT_RST_B_WR_SEC             BIT(8)
#define BIT_REG_FW2_AON_CLK_32K_WR_SEC               BIT(7)
#define BIT_REG_FW2_AON_AI_EN_WR_SEC                 BIT(6)
#define BIT_REG_FW2_AON_XTL_EN0_WR_SEC               BIT(5)
#define BIT_REG_FW2_AON_XTL_EN1_WR_SEC               BIT(4)
#define BIT_REG_FW2_AON_ADI_D_WR_SEC                 BIT(3)
#define BIT_REG_FW2_AON_DCDC_ARM0_EN_WR_SEC          BIT(2)
#define BIT_REG_FW2_AON_AUD_ADD0_WR_SEC              BIT(1)
#define BIT_REG_FW2_AON_DSI_TE_WR_SEC                BIT(0)

/* REG_REG_FW2_AON_REG_WR_CTRL_2 */

#define BIT_REG_FW2_AON_MTCK_ARM_WR_SEC              BIT(31)
#define BIT_REG_FW2_AON_SDA6_WR_SEC                  BIT(30)
#define BIT_REG_FW2_AON_SCL6_WR_SEC                  BIT(29)
#define BIT_REG_FW2_AON_U2RXD_WR_SEC                 BIT(28)
#define BIT_REG_FW2_AON_U2TXD_WR_SEC                 BIT(27)
#define BIT_REG_FW2_AON_DRTCK_WR_SEC                 BIT(26)
#define BIT_REG_FW2_AON_DTCK_WR_SEC                  BIT(25)
#define BIT_REG_FW2_AON_DTMS_WR_SEC                  BIT(24)
#define BIT_REG_FW2_AON_DTDI_WR_SEC                  BIT(23)
#define BIT_REG_FW2_AON_DTDO_WR_SEC                  BIT(22)
#define BIT_REG_FW2_AON_RFCTL_19_WR_SEC              BIT(21)
#define BIT_REG_FW2_AON_RFCTL_18_WR_SEC              BIT(20)
#define BIT_REG_FW2_AON_RFCTL_17_WR_SEC              BIT(19)
#define BIT_REG_FW2_AON_RFCTL_16_WR_SEC              BIT(18)
#define BIT_REG_FW2_AON_RFCTL_15_WR_SEC              BIT(17)
#define BIT_REG_FW2_AON_RFCTL_14_WR_SEC              BIT(16)
#define BIT_REG_FW2_AON_RFCTL_13_WR_SEC              BIT(15)
#define BIT_REG_FW2_AON_RFCTL_12_WR_SEC              BIT(14)
#define BIT_REG_FW2_AON_RFCTL_11_WR_SEC              BIT(13)
#define BIT_REG_FW2_AON_RFCTL_10_WR_SEC              BIT(12)
#define BIT_REG_FW2_AON_RFCTL_9_WR_SEC               BIT(11)
#define BIT_REG_FW2_AON_RFCTL_8_WR_SEC               BIT(10)
#define BIT_REG_FW2_AON_RFCTL_7_WR_SEC               BIT(9)
#define BIT_REG_FW2_AON_RFCTL_6_WR_SEC               BIT(8)
#define BIT_REG_FW2_AON_RFCTL_5_WR_SEC               BIT(7)
#define BIT_REG_FW2_AON_RFCTL_4_WR_SEC               BIT(6)
#define BIT_REG_FW2_AON_RFCTL_3_WR_SEC               BIT(5)
#define BIT_REG_FW2_AON_RFCTL_2_WR_SEC               BIT(4)
#define BIT_REG_FW2_AON_RFCTL_1_WR_SEC               BIT(3)
#define BIT_REG_FW2_AON_RFCTL_0_WR_SEC               BIT(2)
#define BIT_REG_FW2_AON_SD2_D_3_WR_SEC               BIT(1)
#define BIT_REG_FW2_AON_SD2_D_2_WR_SEC               BIT(0)

/* REG_REG_FW2_AON_REG_WR_CTRL_3 */

#define BIT_REG_FW2_AON_U1RXD_WR_SEC                 BIT(31)
#define BIT_REG_FW2_AON_U1TXD_WR_SEC                 BIT(30)
#define BIT_REG_FW2_AON_SPI3_DI_WR_SEC               BIT(29)
#define BIT_REG_FW2_AON_SPI3_CLK_WR_SEC              BIT(28)
#define BIT_REG_FW2_AON_SPI3_CSN_WR_SEC              BIT(27)
#define BIT_REG_FW2_AON_SPI3_DO_WR_SEC               BIT(26)
#define BIT_REG_FW2_AON_KEYIN_2_WR_SEC               BIT(25)
#define BIT_REG_FW2_AON_KEYIN_1_WR_SEC               BIT(24)
#define BIT_REG_FW2_AON_KEYIN_0_WR_SEC               BIT(23)
#define BIT_REG_FW2_AON_KEYOUT_2_WR_SEC              BIT(22)
#define BIT_REG_FW2_AON_KEYOUT_1_WR_SEC              BIT(21)
#define BIT_REG_FW2_AON_KEYOUT_0_WR_SEC              BIT(20)
#define BIT_REG_FW2_AON_SDA2_WR_SEC                  BIT(19)
#define BIT_REG_FW2_AON_SCL2_WR_SEC                  BIT(18)
#define BIT_REG_FW2_AON_RFFE1_SDA_WR_SEC             BIT(17)
#define BIT_REG_FW2_AON_RFFE1_SCK_WR_SEC             BIT(16)
#define BIT_REG_FW2_AON_RFFE0_SDA_WR_SEC             BIT(15)
#define BIT_REG_FW2_AON_RFFE0_SCK_WR_SEC             BIT(14)
#define BIT_REG_FW2_AON_LVDS0_DAC_ON_WR_SEC          BIT(13)
#define BIT_REG_FW2_AON_LVDS0_ADC_ON_WR_SEC          BIT(12)
#define BIT_REG_FW2_AON_RFSEN1_WR_SEC                BIT(11)
#define BIT_REG_FW2_AON_RFSDA1_WR_SEC                BIT(10)
#define BIT_REG_FW2_AON_RFSCK1_WR_SEC                BIT(9)
#define BIT_REG_FW2_AON_RFSEN0_WR_SEC                BIT(8)
#define BIT_REG_FW2_AON_RFSDA0_WR_SEC                BIT(7)
#define BIT_REG_FW2_AON_RFSCK0_WR_SEC                BIT(6)
#define BIT_REG_FW2_AON_USB30_SW_WR_SEC              BIT(5)
#define BIT_REG_FW2_AON_U0RTS_WR_SEC                 BIT(4)
#define BIT_REG_FW2_AON_U0CTS_WR_SEC                 BIT(3)
#define BIT_REG_FW2_AON_U0RXD_WR_SEC                 BIT(2)
#define BIT_REG_FW2_AON_U0TXD_WR_SEC                 BIT(1)
#define BIT_REG_FW2_AON_MTMS_ARM_WR_SEC              BIT(0)

/* REG_REG_FW2_AON_REG_WR_CTRL_4 */

#define BIT_REG_FW2_AON_CMPD1_WR_SEC                 BIT(31)
#define BIT_REG_FW2_AON_CMPD0_WR_SEC                 BIT(30)
#define BIT_REG_FW2_AON_CMRST1_WR_SEC                BIT(29)
#define BIT_REG_FW2_AON_CMRST0_WR_SEC                BIT(28)
#define BIT_REG_FW2_AON_CMMCLK1_WR_SEC               BIT(27)
#define BIT_REG_FW2_AON_CMMCLK0_WR_SEC               BIT(26)
#define BIT_REG_FW2_AON_SCL1_WR_SEC                  BIT(25)
#define BIT_REG_FW2_AON_SDA1_WR_SEC                  BIT(24)
#define BIT_REG_FW2_AON_SDA0_WR_SEC                  BIT(23)
#define BIT_REG_FW2_AON_SCL0_WR_SEC                  BIT(22)
#define BIT_REG_FW2_AON_SPI2_CLK_WR_SEC              BIT(21)
#define BIT_REG_FW2_AON_SPI2_DI_WR_SEC               BIT(20)
#define BIT_REG_FW2_AON_SPI2_DO_WR_SEC               BIT(19)
#define BIT_REG_FW2_AON_SPI2_CSN_WR_SEC              BIT(18)
#define BIT_REG_FW2_AON_SDA7_WR_SEC                  BIT(17)
#define BIT_REG_FW2_AON_SCL7_WR_SEC                  BIT(16)
#define BIT_REG_FW2_AON_IIS1LRCK_WR_SEC              BIT(15)
#define BIT_REG_FW2_AON_IIS1CLK_WR_SEC               BIT(14)
#define BIT_REG_FW2_AON_IIS1DO_WR_SEC                BIT(13)
#define BIT_REG_FW2_AON_IIS1DI_WR_SEC                BIT(12)
#define BIT_REG_FW2_AON_EXTINT10_WR_SEC              BIT(11)
#define BIT_REG_FW2_AON_EXTINT9_WR_SEC               BIT(10)
#define BIT_REG_FW2_AON_SPI0_CLK_WR_SEC              BIT(9)
#define BIT_REG_FW2_AON_SPI0_DI_WR_SEC               BIT(8)
#define BIT_REG_FW2_AON_SPI0_DO_WR_SEC               BIT(7)
#define BIT_REG_FW2_AON_SPI0_CSN_WR_SEC              BIT(6)
#define BIT_REG_FW2_AON_SCL3_WR_SEC                  BIT(5)
#define BIT_REG_FW2_AON_SDA3_WR_SEC                  BIT(4)
#define BIT_REG_FW2_AON_EXTINT1_WR_SEC               BIT(3)
#define BIT_REG_FW2_AON_EXTINT0_WR_SEC               BIT(2)
#define BIT_REG_FW2_AON_PWMC_WR_SEC                  BIT(1)
#define BIT_REG_FW2_AON_SPI_TE_WR_SEC                BIT(0)

/* REG_REG_FW2_AON_REG_WR_CTRL_5 */

#define BIT_REG_FW2_AON_ESESPI_CLK_WR_SEC            BIT(31)
#define BIT_REG_FW2_AON_ESESPI_CSN_WR_SEC            BIT(30)
#define BIT_REG_FW2_AON_ESE_SWP_WR_SEC               BIT(29)
#define BIT_REG_FW2_AON_ESE_GPIO_WR_SEC              BIT(28)
#define BIT_REG_FW2_AON_SD1_D_3_WR_SEC               BIT(27)
#define BIT_REG_FW2_AON_SD1_D_2_WR_SEC               BIT(26)
#define BIT_REG_FW2_AON_SD1_D_1_WR_SEC               BIT(25)
#define BIT_REG_FW2_AON_SD1_D_0_WR_SEC               BIT(24)
#define BIT_REG_FW2_AON_SD1_CMD_WR_SEC               BIT(23)
#define BIT_REG_FW2_AON_SD1_CLK_WR_SEC               BIT(22)
#define BIT_REG_FW2_AON_U7RTS_WR_SEC                 BIT(21)
#define BIT_REG_FW2_AON_U7CTS_WR_SEC                 BIT(20)
#define BIT_REG_FW2_AON_U7RXD_WR_SEC                 BIT(19)
#define BIT_REG_FW2_AON_U7TXD_WR_SEC                 BIT(18)
#define BIT_REG_FW2_AON_U4RTS_WR_SEC                 BIT(17)
#define BIT_REG_FW2_AON_U4CTS_WR_SEC                 BIT(16)
#define BIT_REG_FW2_AON_U4RXD_WR_SEC                 BIT(15)
#define BIT_REG_FW2_AON_U4TXD_WR_SEC                 BIT(14)
#define BIT_REG_FW2_AON_MEMS_MIC_DATA1_WR_SEC        BIT(13)
#define BIT_REG_FW2_AON_MEMS_MIC_CLK1_WR_SEC         BIT(12)
#define BIT_REG_FW2_AON_MEMS_MIC_DATA0_WR_SEC        BIT(11)
#define BIT_REG_FW2_AON_MEMS_MIC_CLK0_WR_SEC         BIT(10)
#define BIT_REG_FW2_AON_CLK_AUX0_WR_SEC              BIT(9)
#define BIT_REG_FW2_AON_U5RXD_WR_SEC                 BIT(8)
#define BIT_REG_FW2_AON_U5TXD_WR_SEC                 BIT(7)
#define BIT_REG_FW2_AON_IIS0LRCK_WR_SEC              BIT(6)
#define BIT_REG_FW2_AON_IIS0CLK_WR_SEC               BIT(5)
#define BIT_REG_FW2_AON_IIS0DO_WR_SEC                BIT(4)
#define BIT_REG_FW2_AON_IIS0DI_WR_SEC                BIT(3)
#define BIT_REG_FW2_AON_CMRST2_WR_SEC                BIT(2)
#define BIT_REG_FW2_AON_CMPD2_WR_SEC                 BIT(1)
#define BIT_REG_FW2_AON_CMMCLK2_WR_SEC               BIT(0)

/* REG_REG_FW2_AON_REG_WR_CTRL_6 */

#define BIT_REG_FW2_AON_ESESPI_D0_WR_SEC             BIT(7)
#define BIT_REG_FW2_AON_ESESPI_D1_WR_SEC             BIT(6)
#define BIT_REG_FW2_AON_ESESPI_D2_WR_SEC             BIT(5)
#define BIT_REG_FW2_AON_ESESPI_D3_WR_SEC             BIT(4)
#define BIT_REG_FW2_AON_ESESPI_D4_WR_SEC             BIT(3)
#define BIT_REG_FW2_AON_ESESPI_D5_WR_SEC             BIT(2)
#define BIT_REG_FW2_AON_ESESPI_D6_WR_SEC             BIT(1)
#define BIT_REG_FW2_AON_ESESPI_D7_WR_SEC             BIT(0)

/* REG_REG_FW2_AON_BIT_CTRL_ADDR_ARRAY0 */

#define BIT_REG_FW2_AON_BIT_CTRL_ADDR_ARRAY0(x)      (((x) & 0x3FFF))

/* REG_REG_FW2_AON_BIT_CTRL_ARRAY0 */

#define BIT_REG_FW2_AON_BIT_CTRL_ARRAY0(x)           (((x) & 0xFFFFFFFF))

#endif
