//
// Copyright 2005 The Android Open Source Project
//
// A "user event" for wxWidgets.
//
#ifndef _SIM_USER_EVENT_H
#define _SIM_USER_EVENT_H

/*
 * Event declaration.  The book says to use DECLARE_EVENT_TYPE, but that
 * causes a compiler warning and a link failure with gcc under MinGW.
 *
 * It looks like the "magic number", in this case 12345, is just picked
 * by hand.  There may be a better mechanism in this version of
 * wxWidgets, but the documentation and sample code doesn't reflect it.
 */
BEGIN_DECLARE_EVENT_TYPES()
    DECLARE_LOCAL_EVENT_TYPE(wxEVT_USER_EVENT, 12345)
END_DECLARE_EVENT_TYPES()

/*
 * A "user event" class.  This can be used like any other wxWidgets
 * event, but we get to stuff anything we want to in it.
 */
class UserEvent : public wxEvent {
public:
    UserEvent(int id = 0, void* data = (void*) 0)
        : wxEvent(id, wxEVT_USER_EVENT), mData(data)
        {}
    UserEvent(const UserEvent& event)
        : wxEvent(event), mData(event.mData)
        {}

    virtual wxEvent* Clone() const {
        return new UserEvent(*this);
    }

    void* GetData(void) const { return mData; }

    DECLARE_DYNAMIC_CLASS(UserEvent);

private:
    UserEvent& operator=(const UserEvent&);     // not implemented
    void*   mData;
};

typedef void (wxEvtHandler::*UserEventFunction)(UserEvent&);

#define EVT_USER_EVENT(fn) \
        DECLARE_EVENT_TABLE_ENTRY(wxEVT_USER_EVENT, wxID_ANY, wxID_ANY, \
            (wxObjectEventFunction)(wxEventFunction)(UserEventFunction)&fn, \
            (wxObject*) NULL ),

#endif // _SIM_USER_EVENT_H
