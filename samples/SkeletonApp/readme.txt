Android Skeleton App
~~~~~~~~~~~~~~~~~~~~


This directory contains the full implementation of a basic application for
the Android platform, demonstrating the basic facilities that applications
will use.  You can run the application either directly from the "test"
list in the app launcher (it is named Skeleton App) or by selecting it from
the top list in the Sample Code app.

The files contained here:


AndroidManifest.xml

This XML file describes to the Android platform what your application can do.
It is a required file, and is the mechanism you use to show your application
to the user (in the app launcher's list), handle data types, etc.


src/*

Under this directory is the Java source for for your application.


src/com/android/skeletonapp/SkeletonActivity.java

This is the implementation of the "activity" feature described in
AndroidManifest.xml.  The path each class implementation is
{src/PACKAGE/CLASS.java}, where PACKAGE comes from the name in the <package>
tag and CLASS comes from the class in the <activity> tag.


res/*

Under this directory are the resources for your application.


res/layout/skeleton_activity.xml

The res/layout/ directory contains XML files describing user interface
view hierarchies.  The skeleton_activity.xml file here is used by
SkeletonActivity.java to construct its UI.  The base name of each file
(all text before a '.' character) is taken as the resource name;
it must be lower-case.


res/drawable/violet.png

The res/drawable/ directory contains images and other things that can be
drawn to the screen.  These can be bitmaps (in .png or .jpeg format) or
special XML files describing more complex drawings.  The violet.png file
here is used as the image to display in one of the views in
skeleton_activity.xml.  Like layout files, the base name is used for the
resulting resource name.


res/values/colors.xml
res/values/strings.xml
res/values/styles.xml

These XML files describe additional resources included in the application.
They all use the same syntax; all of these resources could be defined in one
file, but we generally split them apart as shown here to keep things organized.

