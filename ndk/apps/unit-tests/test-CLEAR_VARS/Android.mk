# This test is used to check that include $(CLEAR_VARS) does
# indeed clear all variables we care for.

LOCAL_PATH := $(call my-dir)

# The list of LOCAL_XXX variables documented by docs/ANDROID-MK.TXT
# Note that LOCAL_PATH is not cleared
VARS_LOCAL := \
    MODULE \
    SRC_FILES \
    CPP_EXTENSION \
    C_INCLUDES \
    CFLAGS \
    CPPFLAGS \
    CXXFLAGS \
    STATIC_LIBRARIES \
    SHARED_LIBRARIES \
    LDLIBS \
    ALLOW_UNDEFINED_SYMBOLS \
    ARM_MODE \

include $(CLEAR_VARS)

$(for _var,$(VARS_LOCAL),\
  $(eval LOCAL_$(_var) := 1)\
)

include $(CLEAR_VARS)

STATUS := ok
$(foreach _var,$(VARS_LOCAL),\
    $(if $(LOCAL_$(_var)),\
      $(info variable LOCAL_$(_var) is not cleared by CLEAR_VARS)\
      $(eval STATUS := ko)\
    ,)\
)

ifeq ($(STATUS),ko)
  $(error Aborting: CLEAR_VARS does not work !)
endif

VARS_LOCAL := $(empty)
STATUS     := $(empty)
