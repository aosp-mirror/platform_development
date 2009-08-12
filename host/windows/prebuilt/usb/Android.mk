LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_PREBUILT_LIBS := \
	AdbWinApi.a

LOCAL_PREBUILT_EXECUTABLES := \
	AdbWinApi.dll  \
	AdbWinUsbApi.dll
	
.PHONY : kill-adb
	
$(LOCAL_PATH)/AdbWinApi.dll : kill-adb

kill-adb:
	@echo "Killing adb server so we can replace AdbWinApi.dll"
	@adb kill-server || echo "adb appears to be missing"

# generate AdbWinApi stub library
#$(LOCAL_PATH)/AdbWinApi.a: $(LOCAL_PATH)/AdbWinApi.def
#	dlltool --def $(LOCAL_PATH)/AdbWinApi.def --dllname AdbWinApi.dll --output-lib $(LOCAL_PATH)/AdbWinApi.a

include $(BUILD_HOST_PREBUILT)
