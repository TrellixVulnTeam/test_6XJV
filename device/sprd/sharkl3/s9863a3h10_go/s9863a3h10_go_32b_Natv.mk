#
# Copyright 2015 The Android Open-Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
KERNEL_PATH := kernel4.4
export KERNEL_PATH
BOARD_PATH=$(KERNEL_PATH)/sprd-board-config/sharkl3/sp9863a_3h10_go/sp9863a_3h10_go_32b_native
include $(BOARD_PATH)
PRODUCT_GO_DEVICE := true
$(call inherit-product, device/sprd/sharkl3/s9863a3h10_go/s9863a3h10_go_Base.mk)
PLATDIR := device/sprd/sharkl3
TARGET_BOARD := s9863a3h10_go
BOARDDIR := $(PLATDIR)/$(TARGET_BOARD)
PLATCOMM := $(PLATDIR)/common
ROOTDIR := $(BOARDDIR)/rootdir
WPDIR := vendor/sprd/resource/wallpapers/HD
TARGET_BOARD_PLATFORM := sp9863a
TARGET_GPU_PLATFORM := rogue

TARGET_BOOTLOADER_BOARD_NAME := sp9863a_3h10
CHIPRAM_DEFCONFIG := sp9863a_3h10_32b
UBOOT_DEFCONFIG := sp9863a_3h10_32b
UBOOT_TARGET_DTB := sp9863a_3h10_32b


PRODUCT_NAME := s9863a3h10_go_32b_Natv
PRODUCT_DEVICE := s9863a3h10_go
PRODUCT_BRAND := SPRD
PRODUCT_MODEL := s9863a3h10_go_32b_Natv
PRODUCT_WIFI_DEVICE := sprd
PRODUCT_MANUFACTURER := sprd


TAGET_BOARD_SPEC_CONFIG := device/sprd/sharkl3/sp9863a1h10_go/$(PRODUCT_NAME).userdiffconfig)
DEVICE_PACKAGE_OVERLAYS := $(BOARDDIR)/overlay $(PLATDIR)/overlay $(PLATCOMM)/overlay $(WPDIR)/overlay
#Runtime Overlay Packages
PRODUCT_ENFORCE_RRO_TARGETS := \
    framework-res


TARGET_ARCH := arm
TARGET_ARCH_VARIANT := armv7-a-neon
TARGET_CPU_ABI := armeabi-v7a
TARGET_CPU_ABI2 := armeabi
TARGET_CPU_VARIANT := generic
 
TARGET_KERNEL_ARCH = arm
TARGET_USES_64_BIT_BINDER := true

#add for microarray fingerprint
BOARD_FINGERPRINT_CONFIG := microarray

#secure boot
BOARD_SECBOOT_CONFIG := true


$(call inherit-product, $(PLATCOMM)/security_feature.mk)

#enable blur for front camera
PRODUCT_PROPERTY_OVERRIDES += \
    persist.vendor.cam.fr.blur.version=1
#enable blur for back camera
#enable cnr
PRODUCT_PROPERTY_OVERRIDES += persist.vendor.cam.cnr.mode=1
#enable ai
PRODUCT_PROPERTY_OVERRIDES += persist.vendor.cam.ai.scence.enable=true
#enable resolution
PRODUCT_PROPERTY_OVERRIDES += persist.vendor.cam.res.blur.ba=RES_2M
#enable beauty
PRODUCT_PROPERTY_OVERRIDES += persist.vendor.cam.facebeauty.corp=2
#faceid version    0--disable  1--single_camera  2--dual_camera
PRODUCT_PROPERTY_OVERRIDES += persist.vendor.cam.faceid.version=1
PRODUCT_COPY_FILES += \
    $(BOARDDIR)/s9863a3h10_go.xml:$(PRODUCT_OUT)/s9863a3h10_go.xml

PRODUCT_AAPT_CONFIG := normal large xlarge mdpi 420dpi xxhdpi
PRODUCT_AAPT_PREF_CONFIG := xhdpi
PRODUCT_AAPT_PREBUILT_DPI := hdpi  xhdpi

$(call inherit-product, $(SRC_TARGET_DIR)/product/languages_full.mk)
include build/make/target/product/go_defaults_common_speed_compile.mk
PRODUCT_PACKAGES += SprdDialerGo
#Preset TouchPal InputMethod
PRODUCT_REVISION := oversea multi-lang

CHIPRAM_DDR_1G_LIMITED := ture
CHIPRAM_DDR_CUSTOMIZE_LIMITED := true

CHIPRAM_DDR_CUSTOMIZE_SIZE := 0x40000000
CHIPRAM_DDR_1G_LIMITED := true

#enable VoWiFi
VOWIFI_SERVICE_ENABLE := true
ifeq ($(strip $(VOWIFI_SERVICE_ENABLE)), true)
# enable the Wi-Fi calling menu in settings.
PRODUCT_PROPERTY_OVERRIDES += persist.dbg.wfc_avail_ovr=1
endif
