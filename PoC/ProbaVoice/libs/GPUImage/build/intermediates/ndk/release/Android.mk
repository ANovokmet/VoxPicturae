LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := gpuimage-library
LOCAL_LDLIBS := \
	-llog \

LOCAL_SRC_FILES := \
	C:\Users\Ante\Desktop\VoxPicturae\PoC\ProbaVoice\libs\GPUImage\jni\yuv-decoder.c \

LOCAL_C_INCLUDES += C:\Users\Ante\Desktop\VoxPicturae\PoC\ProbaVoice\libs\GPUImage\jni
LOCAL_C_INCLUDES += C:\Users\Ante\Desktop\VoxPicturae\PoC\ProbaVoice\libs\GPUImage\src\release\jni

include $(BUILD_SHARED_LIBRARY)
