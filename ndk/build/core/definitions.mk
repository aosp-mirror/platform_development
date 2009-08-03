# Copyright (C) 2009 The Android Open Source Project
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
# Common definitions for the Android NDK build system
#

# We use the GNU Make Standard Library
include $(BUILD_SYSTEM)/../gmsl/gmsl

# This is the Android NDK version number as a list of three items:
# major, minor, revision
#
ndk_version := 1 0 0

# Used to output warnings and error from the library, it's possible to
# disable any warnings or errors by overriding these definitions
# manually or by setting GMSL_NO_WARNINGS or GMSL_NO_ERRORS

__ndk_name    := Android NDK
__ndk_info     = $(info $(__ndk_name): $1 $2 $3 $4 $5)
__ndk_warning  = $(warning $(__ndk_name): $1 $2 $3 $4 $5)
__ndk_error    = $(error $(__ndk_name): $1 $2 $3 $4 $5)

ifdef NDK_NO_WARNINGS
__ndk_warning :=
endif
ifdef NDK_NO_ERRORS
__ndk_error :=
endif

# If NDK_TRACE is enabled then calls to the library functions are
# traced to stdout using warning messages with their arguments

ifdef NDK_TRACE
__ndk_tr1 = $(warning $0('$1'))
__ndk_tr2 = $(warning $0('$1','$2'))
__ndk_tr3 = $(warning $0('$1','$2','$3'))
else
__ndk_tr1 :=
__ndk_tr2 :=
__ndk_tr3 :=
endif

# -----------------------------------------------------------------------------
# Function : ndk_log
# Arguments: 1: text to print when NDK_LOG is defined
# Returns  : None
# Usage    : $(call ndk_log,<some text>)
# -----------------------------------------------------------------------------
ifdef NDK_LOG
ndk_log = $(info $(__ndk_name): $1)
else
ndk_log :=
endif


# -----------------------------------------------------------------------------
# Macro    : empty
# Returns  : an empty macro
# Usage    : $(empty)
# -----------------------------------------------------------------------------
empty :=

# -----------------------------------------------------------------------------
# Macro    : space
# Returns  : a single space
# Usage    : $(space)
# -----------------------------------------------------------------------------
space  := $(empty) $(empty)

# -----------------------------------------------------------------------------
# Function : last2
# Arguments: a list
# Returns  : the penultimate (next-to-last) element of a list
# Usage    : $(call last2, <LIST>)
# -----------------------------------------------------------------------------
last2 = $(word $(words $1), x $1)

# -----------------------------------------------------------------------------
# Function : last3
# Arguments: a list
# Returns  : the antepenultimate (second-next-to-last) element of a list
# Usage    : $(call last3, <LIST>)
# -----------------------------------------------------------------------------
last3 = $(word $(words $1), x x $1)

# -----------------------------------------------------------------------------
# Macro    : this-makefile
# Returns  : the name of the current Makefile in the inclusion stack
# Usage    : $(this-makefile)
# -----------------------------------------------------------------------------
this-makefile = $(lastword $(MAKEFILE_LIST))

# -----------------------------------------------------------------------------
# Macro    : local-makefile
# Returns  : the name of the last parsed Android.mk file
# Usage    : $(local-makefile)
# -----------------------------------------------------------------------------
local-makefile = $(lastword $(filter %Android.mk,$(MAKEFILE_LIST)))

# -----------------------------------------------------------------------------
# Function : assert-defined
# Arguments: 1: list of variable names
# Returns  : None
# Usage    : $(call assert-defined, VAR1 VAR2 VAR3...)
# Rationale: Checks that all variables listed in $1 are defined, or abort the
#            build
# -----------------------------------------------------------------------------
assert-defined = $(foreach __varname,$(strip $1),\
  $(if $(strip $($(__varname))),,\
    $(call __ndk_error, Assertion failure: $(__varname) is not defined)\
  )\
)

# -----------------------------------------------------------------------------
# Function : clear-vars
# Arguments: 1: list of variable names
#            2: file where the variable should be defined
# Returns  : None
# Usage    : $(call clear-vars, VAR1 VAR2 VAR3...)
# Rationale: Clears/undefines all variables in argument list
# -----------------------------------------------------------------------------
clear-vars = $(foreach __varname,$1,$(eval $(__varname) := $(empty)))

# -----------------------------------------------------------------------------
# Function : check-required-vars
# Arguments: 1: list of variable names
#            2: file where the variable(s) should be defined
# Returns  : None
# Usage    : $(call check-required-vars, VAR1 VAR2 VAR3..., <file>)
# Rationale: Checks that all required vars listed in $1 were defined by $2
#            or abort the build with an error
# -----------------------------------------------------------------------------
check-required-vars = $(foreach __varname,$1,\
  $(if $(strip $($(__varname))),,\
    $(call __ndk_info, Required variable $(__varname) is not defined by $2)\
    $(call __ndk_error,Aborting)\
  )\
)

# -----------------------------------------------------------------------------
# Function : modules-clear
# Arguments: None
# Returns  : None
# Usage    : $(call modules-clear)
# Rationale: clears the list of defined modules known by the build system
# -----------------------------------------------------------------------------
modules-clear = $(eval __ndk_modules := $(empty_set))

# -----------------------------------------------------------------------------
# Function : modules-add
# Arguments: 1: module name
#            2: path to Android.mk where the module is defined
# Returns  : None
# Usage    : $(call modules-add,<modulename>,<Android.mk path>)
# Rationale: add a new module. If it is already defined, print an error message
#            and abort.
# -----------------------------------------------------------------------------
modules-add = \
  $(if $(call set_is_member,$(__ndk_modules),$1),\
       $(call __ndk_info,Trying to define local module '$1' in $2.)\
       $(call __ndk_info,But this module was already defined by $(__ndk_modules.$1).)\
       $(call __ndk_error,Aborting.)\
  )\
  $(eval __ndk_modules := $(call set_insert,$(__ndk_modules),$1))\
  $(eval __ndk_modules.$1 := $2)\

# -----------------------------------------------------------------------------
# Function : check-user-define
# Arguments: 1: name of variable that must be defined by the user
#            2: name of Makefile where the variable should be defined
#            3: name/description of the Makefile where the check is done, which
#               must be included by $2
# Returns  : None
# -----------------------------------------------------------------------------
check-user-define = $(if $(strip $($1)),,\
  $(call __ndk_error,Missing $1 before including $3 in $2))

# -----------------------------------------------------------------------------
# This is used to check that LOCAL_MODULE is properly defined by an Android.mk
# file before including one of the $(BUILD_SHARED_LIBRARY), etc... files.
#
# Function : check-user-LOCAL_MODULE
# Arguments: 1: name/description of the included build Makefile where the
#               check is done
# Returns  : None
# Usage    : $(call check-user-LOCAL_MODULE, BUILD_SHARED_LIBRARY)
# -----------------------------------------------------------------------------
check-defined-LOCAL_MODULE = \
  $(call check-user-define,LOCAL_MODULE,$(local-makefile),$(1)) \
  $(if $(call seq,$(words $(LOCAL_MODULE)),1),,\
    $(call __ndk_info,LOCAL_MODULE definition in $(local-makefile) must not contain space)\
    $(call __ndk_error,Please correct error. Aborting)\
  )

# -----------------------------------------------------------------------------
# Strip any 'lib' prefix in front of a given string.
#
# Function : strip-lib-prefix
# Arguments: 1: module name
# Returns  : module name, without any 'lib' prefix if any
# Usage    : $(call strip-lib-prefix,$(LOCAL_MODULE))
# -----------------------------------------------------------------------------
strip-lib-prefix = $(1:lib%=%)

# -----------------------------------------------------------------------------
# This is used to strip any lib prefix from LOCAL_MODULE, then check that
# the corresponding module name is not already defined.
#
# Function : check-user-LOCAL_MODULE
# Arguments: 1: path of Android.mk where this LOCAL_MODULE is defined
# Returns  : None
# Usage    : $(call check-LOCAL_MODULE,$(LOCAL_MAKEFILE))
# -----------------------------------------------------------------------------
check-LOCAL_MODULE = \
  $(eval LOCAL_MODULE := $$(call strip-lib-prefix,$$(LOCAL_MODULE)))\
  $(call modules-add,$(LOCAL_MODULE),$1)

# -----------------------------------------------------------------------------
# Macro    : my-dir
# Returns  : the directory of the current Makefile
# Usage    : $(my-dir)
# -----------------------------------------------------------------------------
my-dir = $(patsubst %/,%,$(dir $(lastword $(MAKEFILE_LIST))))

# -----------------------------------------------------------------------------
# Function : all-makefiles-under
# Arguments: 1: directory path
# Returns  : a list of all makefiles immediately below some directory
# Usage    : $(call all-makefiles-under, <some path>)
# -----------------------------------------------------------------------------
all-makefiles-under = $(wildcard $1/*/Android.mk)

# -----------------------------------------------------------------------------
# Macro    : all-subdir-makefiles
# Returns  : list of all makefiles in subdirectories of the current Makefile's
#            location
# Usage    : $(all-subdir-makefiles)
# -----------------------------------------------------------------------------
all-subdir-makefiles = $(call all-makefiles-under,$(call my-dir))


# =============================================================================
#
# Application.mk support
#
# =============================================================================

# the list of variables that *must* be defined in Application.mk files
NDK_APP_VARS_REQUIRED := APP_MODULES APP_PROJECT_PATH

# the list of variables that *may* be defined in Application.mk files
NDK_APP_VARS_OPTIONAL := APP_OPTIM APP_CPPFLAGS APP_CFLAGS APP_CXXFLAGS \
                         APP_PLATFORM APP_BUILD_SCRIPT

# the list of all variables that may appear in an Application.mk file
NDK_APP_VARS := $(NDK_APP_VARS_REQUIRED) $(NDK_APP_VARS_OPTIONAL)

# =============================================================================
#
# Android.mk support
#
# =============================================================================


# =============================================================================
#
# Generated files support
#
# =============================================================================


# -----------------------------------------------------------------------------
# Function  : host-static-library-path
# Arguments : 1: library module name (e.g. 'foo')
# Returns   : location of generated host library name (e.g. '..../libfoo.a)
# Usage     : $(call host-static-library-path,<modulename>)
# -----------------------------------------------------------------------------
host-static-library-path = $(HOST_OUT)/lib$1.a

# -----------------------------------------------------------------------------
# Function  : host-executable-path
# Arguments : 1: executable module name (e.g. 'foo')
# Returns   : location of generated host executable name (e.g. '..../foo)
# Usage     : $(call host-executable-path,<modulename>)
# -----------------------------------------------------------------------------
host-executable-path = $(HOST_OUT)/$1$(HOST_EXE)

# -----------------------------------------------------------------------------
# Function  : static-library-path
# Arguments : 1: library module name (e.g. 'foo')
# Returns   : location of generated static library name (e.g. '..../libfoo.a)
# Usage     : $(call static-library-path,<modulename>)
# -----------------------------------------------------------------------------
static-library-path = $(TARGET_OUT)/lib$1.a

# -----------------------------------------------------------------------------
# Function  : shared-library-path
# Arguments : 1: library module name (e.g. 'foo')
# Returns   : location of generated shared library name (e.g. '..../libfoo.so)
# Usage     : $(call shared-library-path,<modulename>)
# -----------------------------------------------------------------------------
shared-library-path = $(TARGET_OUT)/lib$1.so

# -----------------------------------------------------------------------------
# Function  : executable-path
# Arguments : 1: executable module name (e.g. 'foo')
# Returns   : location of generated exectuable name (e.g. '..../foo)
# Usage     : $(call executable-path,<modulename>)
# -----------------------------------------------------------------------------
executable-path = $(TARGET_OUT)/$1

# =============================================================================
#
# Build commands support
#
# =============================================================================

# -----------------------------------------------------------------------------
# Macro    : hide
# Returns  : nothing
# Usage    : $(hide)<make commands>
# Rationale: To be used as a prefix for Make build commands to hide them
#            by default during the build. To show them, set V=1 in your
#            environment or command-line.
#
#            For example:
#
#                foo.o: foo.c
#                -->|$(hide) <build-commands>
#
#            Where '-->|' stands for a single tab character.
#
# -----------------------------------------------------------------------------
ifeq ($(V),1)
hide = $(empty)
else
hide = @
endif

# -----------------------------------------------------------------------------
# Template  : ev-compile-c-source
# Arguments : 1: single C source file name (relative to LOCAL_PATH)
#             2: target object file (without path)
# Returns   : None
# Usage     : $(eval $(call ev-compile-c-source,<srcfile>,<objfile>)
# Rationale : Internal template evaluated by compile-c-source and
#             compile-s-source
# -----------------------------------------------------------------------------
define  ev-compile-c-source
_SRC:=$$(LOCAL_PATH)/$(1)
_OBJ:=$$(LOCAL_OBJS_DIR)/$(2)

$$(_OBJ): PRIVATE_SRC      := $$(_SRC)
$$(_OBJ): PRIVATE_OBJ      := $$(_OBJ)
$$(_OBJ): PRIVATE_MODULE   := $$(LOCAL_MODULE)
$$(_OBJ): PRIVATE_ARM_MODE := $$(LOCAL_ARM_MODE)
$$(_OBJ): PRIVATE_ARM_TEXT := $$(LOCAL_ARM_TEXT)
$$(_OBJ): PRIVATE_CC       := $$($$(my)CC)
$$(_OBJ): PRIVATE_CFLAGS   := $$($$(my)CFLAGS) \
                              $$($$(my)$(LOCAL_ARM_MODE)_$(LOCAL_BUILD_MODE)_CFLAGS) \
                              $$(LOCAL_C_INCLUDES:%=-I%) \
                              -I$$(LOCAL_PATH) \
                              $$(LOCAL_CFLAGS) \
                              $$(NDK_APP_CFLAGS)

$$(_OBJ): $$(_SRC) $$(LOCAL_MAKEFILE) $$(NDK_APP_APPLICATION_MK)
	@mkdir -p $$(dir $$(PRIVATE_OBJ))
	@echo "Compile $$(PRIVATE_ARM_TEXT)  : $$(PRIVATE_MODULE) <= $$(PRIVATE_SRC)"
	$(hide) $$(PRIVATE_CC) $$(PRIVATE_CFLAGS) -c \
	-MMD -MP -MF $$(PRIVATE_OBJ).d.tmp \
	$$(PRIVATE_SRC) \
	-o $$(PRIVATE_OBJ)
	$$(call cmd-process-deps,$$(PRIVATE_OBJ))

LOCAL_OBJECTS         += $$(_OBJ)
LOCAL_DEPENDENCY_DIRS += $$(dir $$(_OBJ))
endef

# -----------------------------------------------------------------------------
# Function  : compile-c-source
# Arguments : 1: single C source file name (relative to LOCAL_PATH)
# Returns   : None
# Usage     : $(call compile-c-source,<srcfile>)
# Rationale : Setup everything required to build a single C source file
# -----------------------------------------------------------------------------
compile-c-source = $(eval $(call ev-compile-c-source,$1,$(1:%.c=%.o)))

# -----------------------------------------------------------------------------
# Function  : compile-s-source
# Arguments : 1: single Assembly source file name (relative to LOCAL_PATH)
# Returns   : None
# Usage     : $(call compile-s-source,<srcfile>)
# Rationale : Setup everything required to build a single Assembly source file
# -----------------------------------------------------------------------------
compile-s-source = $(eval $(call ev-compile-c-source,$1,$(1:%.S=%.o)))


# -----------------------------------------------------------------------------
# Template  : ev-compile-cpp-source
# Arguments : 1: single C++ source file name (relative to LOCAL_PATH)
#             2: target object file (without path)
# Returns   : None
# Usage     : $(eval $(call ev-compile-cpp-source,<srcfile>,<objfile>)
# Rationale : Internal template evaluated by compile-cpp-source
# -----------------------------------------------------------------------------

define  ev-compile-cpp-source
_SRC:=$$(LOCAL_PATH)/$(1)
_OBJ:=$$(LOCAL_OBJS_DIR)/$(2)

$$(_OBJ): PRIVATE_SRC      := $$(_SRC)
$$(_OBJ): PRIVATE_OBJ      := $$(_OBJ)
$$(_OBJ): PRIVATE_MODULE   := $$(LOCAL_MODULE)
$$(_OBJ): PRIVATE_ARM_MODE := $$(LOCAL_ARM_MODE)
$$(_OBJ): PRIVATE_ARM_TEXT := $$(LOCAL_ARM_TEXT)
$$(_OBJ): PRIVATE_CXX      := $$($$(my)CXX)
$$(_OBJ): PRIVATE_CXXFLAGS := $$($$(my)CXXFLAGS) \
                              $$($$(my)$(LOCAL_ARM_MODE)_$(LOCAL_BUILD_MODE)_CFLAGS) \
                              $$(LOCAL_C_INCLUDES:%=-I%) \
                              -I$$(LOCAL_PATH) \
                              $$(LOCAL_CFLAGS) \
                              $$(LOCAL_CPPFLAGS) \
                              $$(LOCAL_CXXFLAGS) \
                              $$(NDK_APP_CFLAGS) \
                              $$(NDK_APP_CPPFLAGS) \
                              $$(NDK_APP_CXXFLAGS) \

$$(_OBJ): $$(_SRC) $$(LOCAL_MAKEFILE) $$(NDK_APP_APPLICATION_MK)
	@mkdir -p $$(dir $$(PRIVATE_OBJ))
	@echo "Compile++ $$(PRIVATE_ARM_TEXT): $$(PRIVATE_MODULE) <= $$(PRIVATE_SRC)"
	$(hide) $$(PRIVATE_CXX) $$(PRIVATE_CXXFLAGS) -c \
	-MMD -MP -MF $$(PRIVATE_OBJ).d.tmp \
	$$(PRIVATE_SRC) \
	-o $$(PRIVATE_OBJ)
	$$(call cmd-process-deps,$$(PRIVATE_OBJ))

LOCAL_OBJECTS         += $$(_OBJ)
LOCAL_DEPENDENCY_DIRS += $$(dir $$(_OBJ))
endef

# -----------------------------------------------------------------------------
# Function  : compile-cpp-source
# Arguments : 1: single C++ source file name (relative to LOCAL_PATH)
# Returns   : None
# Usage     : $(call compile-c-source,<srcfile>)
# Rationale : Setup everything required to build a single C++ source file
# -----------------------------------------------------------------------------
compile-cpp-source = $(eval $(call ev-compile-cpp-source,$1,$(1:%$(LOCAL_CPP_EXTENSION)=%.o)))

# -----------------------------------------------------------------------------
# Command   : cmd-process-deps
# Arguments : 1: object file path
# Returns   : None
# Usage     : $(call cmd-process-deps,<objectfile>)
# Rationale : To be used as a Make build command to process the dependencies
#             generated by the compiler (in <obj>.d.tmp) into ones suited
#             for our build system. See the comments in build/core/mkdeps.sh
#             for more details.
# -----------------------------------------------------------------------------
cmd-process-deps = $(hide) $(BUILD_SYSTEM)/mkdeps.sh $(1) $(1).d.tmp $(1).d

# -----------------------------------------------------------------------------
# Command   : cmd-install-file
# Arguments : 1: source file
#             2: destination file
# Returns   : None
# Usage     : $(call cmd-install-file,<srcfile>,<dstfile>)
# Rationale : To be used as a Make build command to copy/install a file to
#             a given location.
# -----------------------------------------------------------------------------
define cmd-install-file
@mkdir -p $(dir $2)
$(hide) cp -fp $1 $2
endef
