LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
        $(call all-logtags-files-under, src)

LOCAL_MODULE := settings-logtags

include $(BUILD_STATIC_JAVA_LIBRARY)

# Build the Settings APK
include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := Settings
LOCAL_PRIVATE_PLATFORM_APIS := true
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
LOCAL_MODULE_TAGS := optional
LOCAL_USE_AAPT2 := true

src_dirs := src
res_dirs := res

ifneq ($(strip $(wildcard $(LOCAL_PATH)/../../../vendor/sprd/platform/packages/apps/AudioProfile)),)

audioprofile_dir := ../../../vendor/sprd/platform/packages/apps/AudioProfile

src_dirs += $(audioprofile_dir)/src

res_dirs += $(audioprofile_dir)/res

LOCAL_AAPT_FLAGS := \
   --auto-add-overlay \
   --extra-packages com.sprd.audioprofile
endif

LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))
LOCAL_SRC_FILES += src/com/sprd/settings/wifi/cmcc/IWifiSettings.aidl
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))

LOCAL_STATIC_ANDROID_LIBRARIES := \
    android-slices-builders \
    android-slices-core \
    android-slices-view \
    android-support-compat \
    android-support-v4 \
    android-support-v13 \
    android-support-v7-appcompat \
    android-support-v7-cardview \
    android-support-v7-preference \
    android-support-v7-recyclerview \
    android-support-v14-preference \

LOCAL_JAVA_LIBRARIES := \
    bouncycastle \
    telephony-common \
    ims-common \
    radio_interactor_common

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-arch-lifecycle-runtime \
    android-arch-lifecycle-extensions \
    guava \
    jsr305 \
    settings-logtags \
	vendor.sprd.hardware.connmgr-V1.0-java \

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

ifneq ($(INCREMENTAL_BUILDS),)
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_JACK_ENABLED := incremental
    LOCAL_JACK_FLAGS := --multi-dex native
endif

include frameworks/opt/setupwizard/library/common-gingerbread.mk
include frameworks/base/packages/SettingsLib/common.mk

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
ifeq (,$(ONE_SHOT_MAKEFILE))
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
