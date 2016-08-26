ifeq ($(strip $(WIND_DEF_ASUS_SETTINGS)),yes)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

ifeq ($(strip $(MTK_CLEARMOTION_SUPPORT)),no)
# if not support clearmotion, load a small video for clearmotion
LOCAL_ASSET_DIR := $(LOCAL_PATH)/assets_no_clearmotion
else
LOCAL_ASSET_DIR := $(LOCAL_PATH)/assets_clearmotion
endif
# wangyan@wind-mobi.com add 2016/04/02 for Feature #100625 -e

# wangyan@wind-mobi.com add 2016/07/05 start
LOCAL_ASSET_DIR += $(LOCAL_PATH)/assets
# wangyan@wind-mobi.com add 2016/07/05 end

LOCAL_JAVA_LIBRARIES := bouncycastle conscrypt telephony-common ims-common \
        mediatek-framework
#mohongwu@wind-mobi.com begin Feature#110147
LOCAL_STATIC_JAVA_LIBRARIES := \
    jsr305 \
    android-support-v4 \
    android-support-v13 \
    com.mediatek.lbs.em2.utils \
    com.mediatek.settings.ext \
    libGooglePlayServicesRev28 \
    libGA4ODM \
	dpt-haha-1.3 \
	dpt-leakcanary-watcher-1.4-beta2 \
	dpt-leakcanary-analyzer-1.4-beta2

#mohongwu@wind-mobi.com end Feature#110147

LOCAL_STATIC_JAVA_AAR_LIBRARIES:= \
	dpt-leakcanary-android-1.4-beta2

LOCAL_STATIC_JAVA_LIBRARIES += asus-common-ui
LOCAL_STATIC_JAVA_LIBRARIES += libweibo4android
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src) \
        src/com/android/settings/EventLogTags.logtags

# xiongshigui@wind-mobi.com add begin
LOCAL_SRC_FILES += \
        src/com/android/settings/IDeviceAdmin.aidl \
        src/com/asus/DLNA/DMS/IDmsService.aidl
# xiongshigui@wind-mobi.com add end
#xuyi@wind-mobi.com 20160525 add for BlueLightFilter begin
LOCAL_SRC_FILES += \
        src/com/asus/splendidcommandagent/ISplendidCommandAgentService.aidl
#xuyi@wind-mobi.com 20160525 add for BlueLightFilter end

ifneq (,$(filter CN CUCC CTA CMCC IQY, $(TARGET_SKU)))
LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/AsusUi/res \
    $(LOCAL_PATH)/CN_Overlay_res \
    $(LOCAL_PATH)/Overlay_res \
    $(LOCAL_PATH)/res_ext \
    $(LOCAL_PATH)/res
else
LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/res \
    $(LOCAL_PATH)/AsusUi/res \
    $(LOCAL_PATH)/res_ext \
    $(LOCAL_PATH)/Overlay_res
endif

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages com.asus.commonui
LOCAL_AAPT_FLAGS += --extra-packages com.squareup.leakcanary

LOCAL_PACKAGE_NAME := AsusSettings
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_PROGUARD_FLAGS := -include $(LOCAL_PATH)/AsusUi/proguard.flags

ifneq (,$(filter CN CUCC CTA IQY, $(TARGET_SKU)))
LOCAL_PROGUARD_FLAGS += -include $(LOCAL_PATH)/proguard.cfg
endif

LOCAL_OVERRIDES_PACKAGES := Settings

ifneq ($(INCREMENTAL_BUILDS),)
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_JACK_ENABLED := incremental
endif

include frameworks/opt/setupwizard/navigationbar/common.mk
include frameworks/opt/setupwizard/library/common.mk
include frameworks/base/packages/SettingsLib/common.mk

include $(BUILD_PACKAGE)
##################################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
    libweibo4android:libs/weibo4android.jar \
    asus-common-ui:AsusUi/libs/asus-common-ui_v0.8.29.jar \
    libGooglePlayServicesRev28:libs/google-play-services.jar \
    libGA4ODM:libs/AndroidPluginForODM.jar \
    dpt-leakcanary-android-1.4-beta2:libs/leakcanary-android-1.4-beta2.aar \
    dpt-haha-1.3:libs/haha-1.3.jar \
    dpt-leakcanary-analyzer-1.4-beta2:libs/leakcanary-analyzer-1.4-beta2.jar \
    dpt-leakcanary-watcher-1.4-beta2:libs/leakcanary-watcher-1.4-beta2.jar
include $(BUILD_MULTI_PREBUILT)
# Use the following include to make our test apk.
ifeq (,$(ONE_SHOT_MAKEFILE))
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
endif
