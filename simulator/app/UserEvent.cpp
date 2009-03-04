//
// Copyright 2005 The Android Open Source Project
//
// Some additional glue for "user event" type.
//

// For compilers that support precompilation, include "wx/wx.h".
#include "wx/wxprec.h"

// Otherwise, include all standard headers
#ifndef WX_PRECOMP
# include "wx/wx.h"
#endif

#include "UserEvent.h"

DEFINE_EVENT_TYPE(wxEVT_USER_EVENT)

IMPLEMENT_DYNAMIC_CLASS(UserEvent, wxEvent)

