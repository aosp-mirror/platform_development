Platform Library Example
~~~~~~~~~~~~~~~~~~~~~~~~


This directory contains a full example of writing your own Android platform
shared library, without changing the Android framework.  It also shows how to
write JNI code for incorporating native code into the library, and a client
application that uses the library.

This example is ONLY for people working with the open source platform to
create a system image that will be delivered on a device which will include
a custom library as shown here.  It can not be used to create a third party
shared library, which is not currently supported in Android.

To declare your library to the framework, you must place a file with a .xml
extension in the /system/etc/permissions directory with the following contents:

<?xml version="1.0" encoding="utf-8"?>
<permissions>
    <library name="com.example.android.platform_library"
            file="/system/framework/com.example.android.platform_library.jar"/>
</permissions>

There are three major parts of this example, supplying three distinct
build targets and corresponding build outputs:


com.example.android.platform_library
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The top-level Android.mk defines the rules to build the shared library itself,
whose target is "com.example.android.platform_library".  The code for this
library lives under java/.

Note that the product for this library is a raw .jar file, NOT a .apk, which
means there is no manifest or resources associated with the library.
Unfortunately this means that if you need any resources for the library, such
as drawables or layout files, you will need to add these to the core framework
resources under frameworks/base/res.  Please make sure when doing this that
you do not make any of these resources public, they should not become part of
the Android API.  In the future we will allow shared libraries to have their
own resources.

Other than that, the library is very straight-forward, and you can write
basically whatever code you want.  You can also put code in other Java
namespaces -- the namespace given in the <library> tag above is just the
public unique name by which clients will link to your library, but once this
link happens all of the Java namespaces in that library will be available
to the client.


libplatform_library_jni
~~~~~~~~~~~~~~~~~~~~~~~

This is an optional example of how to write JNI code associated with a
shared library.  This code lives under jni/.  The jni/Android.mk file defines
the rules for building the final .so in which the code lives.  This example
provides everything needed to hook up the native code with the Java library
and call through to it, plus a very simple JNI call.


PlatformLibraryClient
~~~~~~~~~~~~~~~~~~~~~

This shows an example of how you can write client applications for your new
shared library.  This code lives under client/.  Note that the example is
simply a regular Android .apk, like all of the other .apks created by the
build system.  The only two special things needed to use your library are:

- A LOCAL_JAVA_LIBRARIES line in the Android.mk to have the build system link
against your shared library.

- A <uses-library> line in the AndroidManifest.xml to have the runtime load
your library into the application.
