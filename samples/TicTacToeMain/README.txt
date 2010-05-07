Sample: TicTacToeLib and TicTacToeMain.

--------
Summary:
--------

These two projects work together. They demonstrate how to use the ability to
split an APK into multiple projects.

Build is supported both via Ant (command-line tools) or via ADT (the Android
plugin for Eclipse).

--------
Details:
--------

TicTacToeMain is the main project. It defines a main activity that is first
displayed to the user. When one of the start buttons is selected, an
activity defined in TicTacToeLib is started.

To define that TicTacToeMain uses TicTacToeLib as a "project library", the
file TicTacToeMain/default.properties contains the special line:
  android.library.reference.1=../TicTacToeLib/


TicTacToeLib is the "project library". It can contain both source code (.java)
and Android resources (anything under /res) that will be merged in the final
APK. To define this is a library, the file TicTacToeLib/default.project
contains the special line:
  android.library=true


One important thing to realize is that the library is not a separately-compiled
JAR file: the source and resources from the library are _actually_ merged in
the main project and the result is used to generate the APK. This means that
the main project can either use or redefine behavior from the libraries.


To use the main vs library project:
- In ADT, just open import both projects and launch the main project.
- In Ant, use 'android update project' to create the build files and set the SDK location,
  and then run 'ant debug' on the main project.


For more details on the purpose of this feature, its limitations and detailed usage,
please read the SDK guide at
  http://developer.android.com/guide/developing/eclipse-adt.html

