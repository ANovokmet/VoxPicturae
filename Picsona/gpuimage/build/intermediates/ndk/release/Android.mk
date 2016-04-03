LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := gpuimage-library
LOCAL_LDLIBS := \
	-llog \

LOCAL_SRC_FILES := \
	C:\Users\Ante\Documents\GitHub\VoxPicturae\Picsona\gpuimage\jni\yuv-decoder.c \

LOCAL_C_INCLUDES += C:\Users\Ante\Documents\GitHub\VoxPicturae\Picsona\gpuimage\jni
LOCAL_C_INCLUDES += C:\Users\Ante\Documents\GitHub\VoxPicturae\Picsona\gpuimage\src\release\jni

include $(BUILD_SHARED_LIBRARY)
