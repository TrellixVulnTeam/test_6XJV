# The default product packages these packages will set to trunk/prime products without feature configs
PRODUCT_PACKAGES += \
    FMPlayer \
    SprdRamOptimizer \
    Flashlight \
    LogManager \
    ValidationTools \
    DrmProvider \
    CellBroadcastReceiver \
    SprdQuickSearchBox \
    Carddav-Sync \
    SystemUpdate \
    Caldav-Sync.apk

#NoteBook Not supported on Go Version
ifneq ($(strip $(PRODUCT_GO_DEVICE)),true)
PRODUCT_PACKAGES += NoteBook
endif

PRODUCT_PACKAGES += \
    libsprd_agps_agent\
    FMRadio \
    CarrierConfig \
    UASetting \
    DreamFMRadio \
    DreamSoundRecorder \
    SprdMediaProvider \
    DreamCamera2 \
    QuickCamera \
    Omacp \
    NewGallery2 \
    NewMusic

# TODO Add for not block ptr1
PRODUCT_PACKAGES += \
    SimLockSupport

# add for secnfc
PRODUCT_PACKAGES += \
    com.android.secnfc

AUTOTEST_PACKAGES += \
    AdcTest \
    SprdSlt
PRODUCT_PACKAGES_ENG += $(AUTOTEST_PACKAGES)
PRODUCT_PACKAGES_DEBUG += $(AUTOTEST_PACKAGES)

ifneq ($(strip $(PDK_FUSION_PLATFORM_ZIP)),)
PRODUCT_PACKAGES += \
	PrebuildCalculator \
	PrebuildCalendar \
	PrebuildCalendarProvider \
	PrebuildContacts \
	PrebuildDeskClock \
	PrebuildDialer \
	PrebuildEmail \
	PrebuildExchange2 \
	PrebuildGallery2 \
	PrebuildMms \
	PrebuildLauncher3 \
	PrebuildMusic \
	PrebuildMusicFX \
	PrebuildTeleService \
	PrebuildChrome \
	PrebuildSoundRecorder \
	PrebuildFileExplorer \
	PrebuildTelecom \
	PrebuildGmail \
	PrebuildKeyChain \
	PrebuildMmsService \
	LegacyCamera.apk \
	CustomLocale.apk \
	Development.apk \
	DevelopmentSettings.apk \
        libjni_legacymosaic \
	Prebuildbootanimation \
	PrebuildDocumentsUI
endif

#add nfc test for aabt
PRODUCT_PACKAGES += autotestnfc

