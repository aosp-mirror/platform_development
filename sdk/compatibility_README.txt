Compatibility Libraries for Android.

This SDK component contains static libraries providing access to newer APIs
on older platforms. To use those libraries, simply copy them as static libraries
into your project.

"v4" provides support for using new APIs on Android API 4 (1.6 - Donut) and above.

v4/android-support-v4.jar contains:
- Fragment API. New in API 11 (3.0 - Honeycomb). http://developer.android.com/reference/android/app/Fragment.html
- Loader API. New in API 11 (3.0 - Honeycomb). http://developer.android.com/reference/android/app/LoaderManager.html
- CursorAdapter / ResourceCursorAdapter / SimpleCursorAdapter. These are the API 11 versions.
- MenuCompat allows calling MenuItem.setShowAsAction which only exists on API 11.

v4/src/ is the source code for the compatibility library
v4/samples/ provides a version of ApiDemos using the library.