//
// Copyright 2005 The Android Open Source Project
//
// Resource enumeration.
//
#ifndef _SIM_RESOURCE_H
#define _SIM_RESOURCE_H

/*
 * IDs for dialogs, controls, menu items, and whatever else comes along.
 *
 * Some standard defs are in "wx/defs.h".  They run from 5000 to 6000.
 */
enum {
    // common stuff
    //ID_ANONYMOUS = -1,        // use wxID_ANY(-1) or wxID_STATIC(5105)


    // Menu item IDs
    IDM_FILE_PREFERENCES = 100,
    IDM_FILE_EXIT,

    IDM_RUNTIME_START,
    IDM_RUNTIME_STOP,
    IDM_RUNTIME_RESTART,
    IDM_RUNTIME_KILL,

    IDM_DEVICE,
    IDM_DEVICE_SEL0,
    // leave space; each phone model gets a menu item ID
    IDM_DEVICE_SELN = IDM_DEVICE_SEL0 + 32,
    IDM_DEVICE_RESCAN,

    IDM_DEBUG_SHOW_LOG,

    IDM_HELP_CONTENTS,
    IDM_HELP_ABOUT,


    // Dialog IDs
    IDD_PREFS,
    IDD_LOG_PREFS,

    // Control IDs
    IDC_MODE_SELECT,            // main - combobox
    IDC_USE_GDB,                // main - checkbox
    IDC_USE_VALGRIND,           // main - checkbox
    IDC_CHECK_JNI,              // main - checkbox
    IDC_JAVA_APP_NAME,          // main - combobox
    IDC_JAVA_VM,                // main - combobox
    IDC_OVERLAY_ONION_SKIN,		// main - combobox
    IDC_ONION_SKIN_FILE_NAME,	// main - textctrl
    IDC_ONION_SKIN_BUTTON,		// main - button
    IDC_ONION_SKIN_ALPHA_VAL,	// main - slider
	
    IDC_SPREFS_CONFIG_NAME,     // sim prefs page - textctrl
    IDC_SPREFS_DEBUGGER,        // sim prefs page - textctrl
    IDC_SPREFS_VALGRINDER,      // sim prefs page - textctrl
    IDC_SPREFS_AUTO_POWER_ON,   // sim prefs page - checkbox

    IDC_RPREFS_GAMMA,           // runtime prefs page - textctrl
    IDC_RPREFS_ENABLE_SOUND,    // runtime prefs page - checkbox
    IDC_RPREFS_ENABLE_FAKE_CAMERA,// runtime prefs page - checkbox

    IDC_LOG_TEXT,               // log window - textctrl
    IDC_LOG_LEVEL,              // log window - combobox
    IDC_LOG_CLEAR,              // log window - button
    IDC_LOG_PAUSE,              // log window - button
    IDC_LOG_PREFS,              // log window - button

    IDC_LOG_PREFS_FMT_FULL,     // log prefs - radio button
    IDC_LOG_PREFS_FMT_BRIEF,    // log prefs - radio button
    IDC_LOG_PREFS_FMT_MINIMAL,  // log prefs - radio button
    IDC_LOG_PREFS_SINGLE_LINE,  // log prefs - checkbox
    IDC_LOG_PREFS_EXTRA_SPACING, // log prefs - combobox
    IDC_LOG_PREFS_POINT_SIZE,   // log prefs - textctrl
    IDC_LOG_PREFS_USE_COLOR,    // log prefs - checkbox
    IDC_LOG_PREFS_FONT_MONO,    // log prefs - checkbox

    IDC_LOG_PREFS_DISPLAY_MAX,  // log prefs - textctrl
    IDC_LOG_PREFS_POOL_SIZE,    // log prefs - textctrl

    IDC_LOG_PREFS_WRITE_FILE,   // log prefs - checkbox
    IDC_LOG_PREFS_FILENAME,     // log prefs - textctrl
    IDC_LOG_PREFS_TRUNCATE_OLD, // log prefs - textctrl
};

/*
 * Common definitions for control spacing.
 *
 * Doesn't really belong here, but it'll do.
 */
enum {
    kEdgeSpacing = 4,       // padding at edge of prefs pages, in pixels
    kInterSpacing = 5,      // padding between controls, in pixels
};

#endif // _SIM_RESOURCE_H
