# Copyright 2005 The Android Open Source Project
#

ifeq ($(TARGET_SIMULATOR),true)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	DeviceManager.cpp \
	DeviceWindow.cpp \
	ExternalRuntime.cpp \
	LoadableImage.cpp \
	LocalBiChannel.cpp \
	LogMessage.cpp \
	LogPool.cpp \
	LogPrefsDialog.cpp \
	LogWindow.cpp \
	MainFrame.cpp \
	MessageStream.cpp \
	MyApp.cpp \
	PhoneButton.cpp \
	PhoneCollection.cpp \
	PhoneData.cpp \
	PhoneWindow.cpp \
	Pipe.cpp \
	Preferences.cpp \
	PrefsDialog.cpp \
	PropertyServer.cpp \
	Semaphore.cpp \
	Shmem.cpp \
	UserEvent.cpp \
	executablepath_linux.cpp \
	ported.cpp

LOCAL_STATIC_LIBRARIES := \
	libtinyxml
LOCAL_WHOLE_STATIC_LIBRARIES := \
	libutils\
	libcutils
LOCAL_MODULE := simulator

LOCAL_LDLIBS += -lpthread

LOCAL_CFLAGS := -UNDEBUG
#LOCAL_LDFLAGS :=

LOCAL_C_INCLUDES += \
	external/tinyxml \
	commands/runtime

# wxWidgets defines
LOCAL_C_INCLUDES += \
	/usr/include/wx-2.6 \
	/usr/lib/wx/include/gtk2-unicode-release-2.6

ifeq ($(HOST_OS),linux)
	# You can install wxWidgets with "sudo apt-get libwxgtk2.6-dev"
	LOCAL_LDFLAGS += -lwx_baseu-2.6 \
		-lwx_baseu_net-2.6 \
		-lwx_baseu_xml-2.6 \
		-lwx_gtk2u_adv-2.6 \
		-lwx_gtk2u_core-2.6 \
		-lwx_gtk2u_html-2.6 \
		-lwx_gtk2u_qa-2.6 \
		-lwx_gtk2u_xrc-2.6

	# this next line makes the simulator able to find its shared libraries
	# without us explicitly setting the LD_LIBRARY_PATH environment variable
	LOCAL_LDLIBS += -Wl,-z,origin
	LOCAL_CFLAGS += -DGTK_NO_CHECK_CASTS -D__WXGTK__ -D_FILE_OFFSET_BITS=64 \
   					-D_LARGE_FILES -D_LARGEFILE_SOURCE=1 
	LOCAL_LDLIBS += -lrt
endif
ifeq ($(HOST_OS),darwin)
	# NOTE: OS X is no longer supported
	LOCAL_C_INCLUDES += prebuilt/$(HOST_PREBUILT_TAG)/wxwidgets 
	LOCAL_LDLIBS += \
				-framework QuickTime \
				-framework IOKit \
				-framework Carbon \
				-framework Cocoa \
				-framework System \
				-lwx_mac_xrc-2.6 \
				-lwx_mac_qa-2.6 \
				-lwx_mac_html-2.6 \
				-lwx_mac_adv-2.6 \
				-lwx_mac_core-2.6 \
				-lwx_base_carbon_xml-2.6 \
				-lwx_base_carbon_net-2.6 \
				-lwx_base_carbon-2.6 \
				-lwxexpat-2.6 \
				-lwxtiff-2.6 \
				-lwxjpeg-2.6 \
				-lwxpng-2.6 \
				-lz \
				-lpthread \
				-liconv
	LOCAL_CFLAGS += \
				-D__WXMAC__ \
				-D_FILE_OFFSET_BITS=64 \
				-D_LARGE_FILES \
				-DNO_GCC_PRAGMA
endif


include $(BUILD_HOST_EXECUTABLE)

ifeq ($(HOST_OS),darwin)
# Add the carbon resources to the executable.
$(LOCAL_BUILT_MODULE): PRIVATE_POST_PROCESS_COMMAND := \
        /Developer/Tools/Rez -d __DARWIN__ -t APPL \
        -d __WXMAC__ -o $(LOCAL_BUILT_MODULE) Carbon.r
endif

# also, we need to copy our assets.  We place these by hand now, because
# I'd like to clean this up as part of some pdk cleanup I want to do.

asset_files := $(addprefix $(LOCAL_PATH)/assets/,$(call find-subdir-assets,$(LOCAL_PATH)/assets))
asset_target := $(HOST_COMMON_OUT_ROOT)/sim-assets/simulator$(COMMON_PACKAGE_SUFFIX)
$(asset_target): PRIVATE_ASSET_ROOT := $(LOCAL_PATH)/assets

$(asset_target) : $(asset_files) $(AAPT)
	@echo host Package $@
	$(hide) mkdir -p $(dir $@)
	$(hide) $(AAPT) package -u -A $(PRIVATE_ASSET_ROOT) -F $@

$(LOCAL_INSTALLED_MODULE): | $(asset_target)

ALL_DEFAULT_INSTALLED_MODULES += $(asset_target)

endif # $(TARGET_SIMULATOR) == true
