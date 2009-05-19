# Package a shared library into an APK

NDK_BASE   := ../..

# Assume ANDROID_SDK_BASE is defined to point to the base of the Android SDK
# Assumes PREBUILT is defined to point to the prebuilt directory of the Android source

include $(NDK_BASE)/config/config.mk

SOURCES    := native.cpp
OBJECTS    := $(SOURCES:.cpp=.o)
LIBS       := -lc -lm
ALIB       := $(PREBUILT)/toolchain/arm-eabi-4.2.1/lib/gcc/arm-eabi/4.2.1/interwork/libgcc.a

APKBUILDER := $(ANDROID_SDK_BASE)/tools/apkbuilder

APK_INPUT_DIR := bin
APK_OUTPUT_DIR := bin/full

APK_NAME   := JNIExample.apk
INPUT_APK  := $(APK_INPUT_DIR)/$(APK_NAME)
OUTPUT_APK := $(APK_OUTPUT_DIR)/$(APK_NAME)
APK_STAGE  := $(APK_OUTPUT_DIR)/temp

JNI_LIBS := libnative.so

all: $(OUTPUT_APK)

$(OUTPUT_APK) : $(INPUT_APK) $(JNI_LIBS)
	rm -rf $(APK_STAGE)
	mkdir -p $(APK_STAGE)
	unzip $(INPUT_APK) -d $(APK_STAGE)
	mkdir -p $(APK_STAGE)/lib/armeabi
	cp $(JNI_LIBS) $(APK_STAGE)/lib/armeabi
	$(APKBUILDER) $(OUTPUT_APK) -v -rf $(APK_STAGE)
	rm -rf $(APK_STAGE)

libnative.so: native.o
	$(CC) -nostdlib -Wl,-soname,libhello-shared.so -Wl,-shared,-Bsymbolic -L$(NDK_BASE)/lib $^ $(LIBS) -o $@ -Wl,--no-undefined $(ALIB)
	
native.o: native.cpp
	$(CC) -c -fpic $(INC) -I$(NDK_BASE)/include/nativehelper -o $@ $^ 

clean:
	rm -rf *.o libnative.so $(APK_OUTPUT_DIR)
