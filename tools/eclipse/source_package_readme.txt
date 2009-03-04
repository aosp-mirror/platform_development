HOW TO PACKAGE THE SOURCE OF THE PLUGINS FOR RELEASE.

Note: this is only useful before we move to the public source repository, after which this will
be obsolete.

The source archive must contains:
1/ Source of the EPL plugins that are released.
2/ Any closed source dependencies that were created by Google.
3/ The readme file explaining how to build the plugins.


1/ PLUGIN SOURCE

The Plugins that are currently released and that are EPL are:
- Android Developer Tools => com.android.ide.eclipse.adt
- Common                  => com.android.ide.eclipse.common
- Android Editors         => com.android.ide.eclipse.editors

All three plugins are located in
    device/tools/eclipse/plugins/

Before packing them up, it is important to:
- remove the bin directory if it exists
- remove any symlinks to jar files from the top level folder of each plugin

2/ PLUGIN DEPENDENCIES

The plugin dependencies are jar files embedded in some of the plugins. Some of those jar files
are android libraries for which the source code is not yet being released (They will be released
under the APL).

Those libraries are not part of the SDK, and need to be taken from a engineering build.
They will be located in
    device/out/host/<platform>/framework/

The libraries to copy are:
 - layoutlib_api.jar
 - layoutlib_utils.jar
 - ninepatch.jar

They should be placed in a "libs" folder in the source archive.

3/ README

In the source archive, at the top level, needs to be present a file explaining how to compile
the plugins.

This file is located at:
    device/tools/eclipse/plugins/README.txt