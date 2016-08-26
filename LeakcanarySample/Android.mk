#
# Copyright (C) 2008 The Android Open Source Project
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

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := \
	lf-haha-1.3 \
	lf-leakcanary-watcher-1.4-beta2 \
	lf-leakcanary-analyzer-1.4-beta2

LOCAL_STATIC_JAVA_AAR_LIBRARIES:= \
	lf-leakcanary-android-1.4-beta2	

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages com.squareup.leakcanary
	
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_SDK_VERSION := current

LOCAL_PACKAGE_NAME := LeakcanarySample

include $(BUILD_PACKAGE)
##################################################
include $(CLEAR_VARS)


LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
    lf-leakcanary-android-1.4-beta2:libs/leakcanary-android-1.4-beta2.aar \
    lf-haha-1.3:libs/haha-1.3.jar \
    lf-leakcanary-analyzer-1.4-beta2:libs/leakcanary-analyzer-1.4-beta2.jar \
    lf-leakcanary-watcher-1.4-beta2:libs/leakcanary-watcher-1.4-beta2.jar

include $(BUILD_MULTI_PREBUILT)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
