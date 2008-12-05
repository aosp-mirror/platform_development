Compiling and deploying the Android Development Toolkit (ADT) feature.

The ADT feature is composed of four plugins:
- com.android.ide.eclipse.adt:
    The ADT plugin, which provides support for compiling and debugging android
    applications.
- com.android.ide.eclipse.common:
    A common plugin providing utility services to the other plugins.
- com.android.ide.eclipse.editors:
    A plugin providing optional XML editors.
- com.android.ide.eclipse.ddms:
    A plugin version of the tool DDMS

Each of these live in development/tools/eclipse/plugins/

2 Features are used to distribute the plugins:
ADT, which contains ADT, ddms, common
Editors, which contains Editors, and requires the ADT feature.

The feature projects are located in development/tools/eclipse/features/

Finally 2 site projects are located in development/tools/eclipse/sites/
internal: is a site containing the features mentioned above as well as a test feature.
external: contains only the ADT and Editors features.


Basic requirements to develop on the plugins:
- Eclipse 3.3 or 3.4 with JDT and PDE.


----------------------------------
1- Loading the projects in Eclipse
----------------------------------

The plugins projects depend on jar files located in the Android source tree,
or, in some cases, built by the Android source.

Also, some source code (ddms) is located in a different location and needs to
be linked into the DDMS plugin source.

To automatize all of this, cd into development/tools/eclipse/scripts/
and run create_all_symlinks.sh

Once this has been done successfully, use the import project action in Eclipse
and point it to development/tools/eclipse. It will find all the projects in the
sub folder.


-----------------------------------------------
2- Launching/Debugging the plugins from Eclipse
-----------------------------------------------

- Open Debug Dialog.
- Create an "Eclipse Application" configuration.
- in the "Plug-ins" tab, make sure the plugins are selected (you may
  want to disable the test plugin if you just want to run ADT)

-----------------------------
3- Building a new update site
-----------------------------

- From Eclipse, open the site.xml of the site project you want to build and click
  "Build All" from the "Site Map" tab.
