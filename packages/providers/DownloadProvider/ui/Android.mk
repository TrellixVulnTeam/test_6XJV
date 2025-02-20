LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_DEX_PREOPT := false

LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_SRC_FILES := $(call all-java-files-under, src) \
    ../src/com/android/providers/downloads/OpenHelper.java \
    ../src/com/android/providers/downloads/Constants.java \
    ../src/com/android/providers/downloads/DownloadDrmHelper.java \
    ../src/com/android/providers/downloads/RawDocumentsHelper.java \
    ../src/com/android/providers/downloadsplugin/OpenHelperDRMUtil.java
LOCAL_AAPT_FLAGS += --extra-packages com.android.providers.downloads
LOCAL_PACKAGE_NAME := DownloadProviderUi
LOCAL_PRIVATE_PLATFORM_APIS := true
LOCAL_CERTIFICATE := media
LOCAL_PRIVILEGED_MODULE := true

include $(BUILD_PACKAGE)
