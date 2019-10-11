# DumpViewer

DumpViewer is an on-device logcat / dumpsys viewer.

## Build

```
m -j DumpViewer
```

## Installation

DumpViewer requires some development permissions in order to read logcat and dumpsys.
To install, pass the `-g` option to `adb install` to give the needed permissions:
```
adb install -r -g DumpViewer.apk

# More precisely:
adb install -r -g ${ANDROID_PRODUCT_OUT}/data/app/DumpViewer/DumpViewer.apk
```

Alternatively, you can grant the permissions with `pm grant`:
```
pm grant com.android.dumpviewer android.permission.PACKAGE_USAGE_STATS
pm grant com.android.dumpviewer android.permission.READ_LOGS
pm grant com.android.dumpviewer android.permission.DUMP
```

## TODOs

 - Add UID / PID lookup
 - Add color on logcat (by severity)
 - Auto-shrink the header.
