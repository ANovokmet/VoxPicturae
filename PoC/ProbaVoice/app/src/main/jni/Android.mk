LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := proba
LOCAL_SRC_FILES := test.cpp
LOCAL_SRC_FILES := testiranje.c
include $(BUILD_SHARED_LIBRARY)