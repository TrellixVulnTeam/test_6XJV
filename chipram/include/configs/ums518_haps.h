#ifndef __CONFIG_H
#define __CONFIG_H

#include <asm/arch/soc_config.h>
#include <asm/sizes.h>

/* only used in fdl2.in uart download, the debug infors from serial will break the download process. */
#define CONFIG_LOAD_PARTITION
#define CONFIG_CHIP_ENV_SET
#define UBOOT_HASH_SIZE        0x200

//#define SPRD_EVM_TAG_ON 1
#ifdef SPRD_EVM_TAG_ON
#define SPRD_EVM_ADDR_START 0x00026000
#define SPRD_EVM_TAG(_x) (*(((unsigned long *)SPRD_EVM_ADDR_START)+_x) = *(volatile unsigned long *)0x87003004)
#endif

/* SPREADTRUM BIGPHONE board - SoC Configuration */
#define CONFIG_FPGA
#define CONFIG_SOC_SHARKL5PRO
#define BOOT_FLAG_ADDRESS 0x5028

//dual-backup, sansung demand
#define CONFIG_DUAL_BACKUP /*to be add*/

#define CHIPRAM_ENV_ADDR	0x82000000
#define CHIP_ENDIAN_LITTLE
#define _LITTLE_ENDIAN 1

#define CONFIG_SYS_64BIT_LBA
#define CONFIG_EMMC_BOOT
#define CONFIG_SD_BOOT

#define SDCARD_BOOT_SECTOR	200

#ifdef  CONFIG_EMMC_BOOT
#define EMMC_SECTOR_SIZE 512
#define CONFIG_SYS_EMMC_U_BOOT_SECTOR_NUM ((CONFIG_SYS_NAND_U_BOOT_SIZE+EMMC_SECTOR_SIZE-1)/EMMC_SECTOR_SIZE)
#endif

/*
 * MMC definition
 */
#define CONFIG_CMD_MMC
#ifdef  CONFIG_CMD_MMC
#define CONFIG_EFI_PARTITION		1
#endif

//#define CONFIG_SYS_HUSH_PARSER
#define CONFIG_SPL_LOAD_LEN	(0x6000)

/* NAND BOOT is the only boot method */
#define CONFIG_NAND_U_BOOT
/* Start copying real U-boot from the second page */
#define CONFIG_SYS_NAND_U_BOOT_OFFS	0x40000
#define CONFIG_SYS_NAND_U_BOOT_SIZE	0x8A000
//#define FPGA_TRACE_DOWNLOAD //for download image from trace

/*when ddr channel function open,fdl2/uboot start address can be set intertwine addr*/
//#define CONFIG_UBOOT_INTERTWINE_ADDR

/* Load U-Boot to this address */
#define CONFIG_UBOOT_MAX_SIZE 0x100000
#define CONFIG_SYS_NAND_U_BOOT_DST	0x9f000000
#define CONFIG_SYS_NAND_U_BOOT_START	CONFIG_SYS_NAND_U_BOOT_DST
#define CONFIG_SYS_SDRAM_BASE 0x80000000
#define CONFIG_SYS_SDRAM_END (CONFIG_SYS_SDRAM_BASE + 256*1024*1024)

//#define CONFIG_SMLBOOT          1 /*to be add*/
#define SML_LOAD_MAX_SIZE       (64*1024)
#define TOS_LOAD_MAX_SIZE       (2*1024*1024)
#define IMAGE_HEAD_SIZE		(512)

#ifndef CONFIG_NAND_SPL
#define CONFIG_SYS_INIT_SP_ADDR     \
	(CONFIG_SYS_SDRAM_END - 0x10000 - GENERATED_GBL_DATA_SIZE)
#endif

#define CONFIG_HW_WATCHDOG
//#define CONFIG_AUTOBOOT //used for FPGA test, auto boot other image
//#define CONFIG_DISPLAY_CPUINFO


/*
 * Memory Info
 */
/* malloc() len */
#define CONFIG_SYS_MALLOC_LEN		(2 << 20)	/* 1 MiB */
/*
 * Board has 2 32MB banks of DRAM but there is a bug when using
 * both so only the first is configured
 */
#define CONFIG_NR_DRAM_BANKS	1

/*
 * Serial Info
 */
#define CONFIG_SYS_SC8800X_UART1	1

/*
 * Flash & Environment
 */
/* No NOR flash present */
#define CONFIG_SYS_MONITOR_LEN ((CONFIG_SYS_NAND_U_BOOT_OFFS)+(CONFIG_SYS_NAND_U_BOOT_SIZE))
#define CONFIG_SYS_NO_FLASH	1
#define CONFIG_ENV_SIZE		(128 * 1024)

/*
#define	CONFIG_ENV_OFFSET	CONFIG_SYS_MONITOR_LEN
#define CONFIG_ENV_OFFSET_REDUND	(CONFIG_ENV_OFFSET + CONFIG_ENV_SIZE)
*/

/* DDR */
#define CONFIG_CLK_PARA
//#define CONFIG_DUAL_DDR fpga verify status
//#define CONFIG_DUAL_DDR_3G

#ifndef CONFIG_CLK_PARA
#define DDR_CLK 464
#else
#define MAGIC_HEADER	0x5555AAAA
#define MAGIC_END	0xAAAA5555
#define CONFIG_PARA_VERSION 2

#define CLK_CA53_CORE0	CLK_1300M

#define CLK_CA53_AXI    CLK_CA53_CORE0/2
#define CLK_DGB0_FREQ	CLK_CA53_CORE0/2
#define CLK_ACE0_FREQ   CLK_CA53_CORE0/2
#define CLK_ATB0_FREQ   CLK_CA53_CORE0/2

#ifdef CONFIG_NAND_SPL
#define CLK_DDR_FREQ        667000000
#else
#define CLK_DDR_FREQ        166000000
#endif

#ifdef CONFIG_NAND_SPL
#define DCDC_ARM	1100
#else
#define DCDC_ARM	1100
#endif

#define DCDC_CORE	900


//clk_list
#define CLK_LEVEL_COUNT_MAX  6
#define CLK_AXI_LEVEL 4
#define CLK_AXI_LEVEL_ID 3
#define CLK_CCI_LEVEL 4
#define CLK_CCI_LEVEL_ID 3
#define CLK_APB_LEVEL 4
#define CLK_APB_LEVEL_ID 3
#define CLK_AON_APB_LEVEL 4
#define CLK_AON_APB_LEVEL_ID 3

#define CONFIG_VOL_PARA
#endif
//--- for DDR
#define CFG_DRAM_TYPE	DRAM_LPDDR3
#define DDR_AUTO_DETECT

//#define DDR_DFS_SUPPORT
//#define DDR_DFS_VAL_BASE 0X1c00
//#define DDR_SCAN_SUPPORT

#define DDR_DFTYPE_LP4 DRAM_LPDDR4_1CS_12G_X32
#define DDR_DFTYPE_LP3 DRAM_LPDDR3_2CS_16G_X32_X16X4

/* NAND */
//#define CONFIG_JFFS2_NAND
//#define CONFIG_SPRD_NAND_HWECC
//#define CONFIG_SYS_NAND_5_ADDR_CYCLE

#define CONFIG_MTD_PARTITIONS

/* U-Boot general configuration */
#define CONFIG_SYS_PROMPT	"=> "	/* Monitor Command Prompt */
#define CONFIG_SYS_CBSIZE	1024	/* Console I/O Buffer Size  */
/* Print buffer sz */
#define CONFIG_SYS_PBSIZE	(CONFIG_SYS_CBSIZE + \
		sizeof(CONFIG_SYS_PROMPT) + 16)

/* EFUSE */
#define CONFIG_EFUSE
#define CONFIG_ADIE_SC2730

#ifdef CONFIG_ADIE_SC2730
#define ADI_15BIT_MODE
#endif
#endif /* __CONFIG_H */
