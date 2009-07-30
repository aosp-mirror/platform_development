!IF 0

Copyright (C) 2007 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Module Name:

    sources.

Abstract:

    This file specifies the target component being built and the list of
    sources files needed to build that driver.  Also specifies optional
    compiler switches and libraries that are unique for the component being
    built.

!ENDIF

TARGETNAME=androidusb
!IF "$(DDKBUILDENV)"=="chk"
TARGETPATH=..\build\Debug
!ELSE
TARGETPATH=..\build\Release
!ENDIF
TARGETTYPE=DRIVER
KMDF_VERSION=1
USECXX_FLAG=/TP
USER_C_FLAGS=$(USER_C_FLAGS) /wd4100 /wd4002 /wd4509 /wd4390 /TP

INCLUDES=$(INCLUDES); \
    	 $(IFSKIT_INC_PATH); \
    	 ..\..\api;

TARGETLIBS=$(DDK_LIB_PATH)\usbd.lib

MSC_WARNING_LEVEL=/W4 /WX /Wp64
MSC_OPTIMIZATION = /Oi /Ob1
C_DEFINES=$(C_DEFINES) -DEXPLODE_POOLTAGS -DRTL_USE_AVL_TABLES

RCOPTIONS=$(RCOPTIONS) /dVER_COMPANYNAME_STR="\"Google Inc\"" 
RCOPTIONS=$(RCOPTIONS) /dVER_LEGALCOPYRIGHT_YEARS="\"2007\"" 
RCOPTIONS=$(RCOPTIONS) /dVER_LEGALCOPYRIGHT_STR="\"\251 Google Inc. All rights reserved.\"" 
RCOPTIONS=$(RCOPTIONS) /dVER_PRODUCTNAME_STR="\"Google Android USB Driver\"" 
RCOPTIONS=$(RCOPTIONS) /dVER_PRODUCTVERSION="1,00,01,001" 
RCOPTIONS=$(RCOPTIONS) /dVER_PRODUCTVERSION_STR="\"1.00\""

!IF 0

By overriding .rsrc section properties (!D removes Discardable attribute)
we make sure that all our vtables will be placed properly into non-discardable
data segment. Because of the nature of this driver we don't need to have
vtables in NonPaged data sections because all our objects can be paged.
Otherwise we may want to add /SECTION:.rsrc,X option that locks section in memory

!ENDIF

LINKER_FLAGS=$(LINKER_FLAGS) /MAP /MAPINFO:LINES /SECTION:.rsrc,!D

MOST_SOURCES=  \
  android_usb_driver_object.cpp \
  android_usb_wdf_object.cpp \
  android_usb_device_object.cpp \
  android_usb_file_object.cpp \
  android_usb_device_file_object.cpp \
  android_usb_pipe_file_object.cpp \
  android_usb_bulk_file_object.cpp \
  android_usb_interrupt_file_object.cpp

PRECOMPILED_INCLUDE=precomp.h
PRECOMPILED_PCH=precomp.pch
PRECOMPILED_OBJ=precomp.obj

