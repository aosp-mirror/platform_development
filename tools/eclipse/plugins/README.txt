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

Because the DDMS plugin source code is not yet released, compiling the
ADT/Common/Editors plugins requires to install the DDMS plugin in eclipse.

Basic requirements:
- Eclipse 3.3 or 3.4 with JDT and PDE.
- DDMS plugin installed and running.


--------------------------
1- Install the DDMS plugin
--------------------------

The easiest way to setup the DDMS plugin in your Eclipse environment is to
install the ADT features (see SDK documentation for details) and then remove
the following features and plugins:

- <eclipse-directory>/features/com.android.ide.eclipse.adt_x.x.x.jar
- <eclipse-directory>/plugins/com.android.ide.eclipse.adt_x.x.x.jar
- <eclipse-directory>/plugins/com.android.ide.eclipse.common_x.x.x.jar
- <eclipse-directory>/plugins/com.android.ide.eclipse.editors_x.x.x.jar

This will leave you with only the DDMS plugin installed in your Eclipse
distribution.


-------------------------------------
2- Setting up the ADT/Common project
-------------------------------------

- Download the ADT/Common/Editors source.

- From the SDK, copy the following jars:
   * androidprefs.jar    => com.android.ide.eclipse.adt folder.
   * jarutils.jar        => com.android.ide.eclipse.adt folder.
   * ping.jar            => com.android.ide.eclipse.common folder.
   * androidprefs.jar    => com.android.ide.eclipse.common folder.

- Create a java project from existing source for both the ADT plugin and the
  common plugin.

- In the Package Explorer, right click the projects and choose
     PDE Tools > Convert Projects to Plug-in Project...

- Select your projects in the dialog box and click OK.

- In the Package Explorer, for ADT and common, right click the jar files mentioned above
  and choose Build Path > Add to Build Path

At this point the projects will compile.

To launch the projects, open the Run/Debug Dialog and create an "Eclipse
Application" launch configuration.

Additionnaly, another feature containing the Android Editors Plugin
(com.android.ide.eclipse.editors) is available.

- Make sure the common project is present in your workspace as the Editors
  plugin depends on this plugin. Alternatively, you can have the offical ADT
  feature installed in your Eclipse distribution.
- Create a java project from existing source for the Editors project.
- In the Package Explorer, right click the project and choose
     PDE Tools > Convert Projects to Plug-in Project...
- Select your project in the dialog box and click OK.

Create an "Eclipse Application" launch configuration to test the plugin.

-------------------------------------
3- Setting up the Editors project
-------------------------------------

The "editors" plugin is optional. You can use ADT to develop Android
applications without the XML editor support. When this plugin is present, it
offers several customized form-based XML editors and one graphical layout
editor.

At the time of this release (Android 0.9 SDK), some of the supporting libraries
still need some cleanup and are currently only provided as JAR files.

- Download the ADT/Common/Editors source.

- From the source archives, copy the following jars:
   * ninepatch.jar       => com.android.ide.eclipse.editors folder.
   * layoutlib_utils.jar => com.android.ide.eclipse.editors folder.
   * layoutlib_api.jar   => com.android.ide.eclipse.editors folder.

- From http://kxml.sourceforge.net/ download:
   * kXML2-2.3.0.jar     => com.android.ide.eclipse.editors folder.

- Create a java project from existing source for both the editors plugin.

- In the Package Explorer, right click the project and choose
     PDE Tools > Convert Projects to Plug-in Project...

- Select your project in the dialog box and click OK.

- In the Package Explorer for editors, right click the jar files mentioned
  above and choose Build Path > Add to Build Path

To launch the projects, reuse the "Eclipse Application" launch configuration
created for ADT.

