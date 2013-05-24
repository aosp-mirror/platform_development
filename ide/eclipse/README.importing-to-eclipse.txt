(Java)

To import the formatter, go to the preferences, section Java > Code Style >
formatter, then click on import and choose
development/ide/eclipse/android-formatting.xml

To import the import order, to go into Java > Code Style > Organize Import,
then click on import and choose development/ide/eclipse/android.importorder

(C++)

To import the include paths, go to Project > Properties > C/C++ General >
Paths and Symbols, then click on "Includes" and then click on "Import Settings".
Choose development/ide/eclipse/android-include-paths.xml and hit Finish.
You will need to re-index for the changes to get picked up (right click project
in Package Explorer, then Index > Rebuild).

To import the symbols, go to Project > Properties > C/C++ General >
Paths and Symbols, then click on "Symbols" and then click on "Import Settings".
Choose development/ide/eclipse/android-symbols.xml and hit Finish.
You will need to re-index for the changes to get picked up (right click project
in Package Explorer, then Index > Rebuild).

In addition, you will need to add some include files (no way to import this
from an XML file) by hand. Go to Project > Properties > C/C++ General >
Paths and Symbols, then click on "Include Files" and click on "Add". Check
"Add to all configurations" and "Add to all languages". Repeat for these files:

    ${ProjDirPath}/build/core/combo/include/arch/linux-arm/AndroidConfig.h

If you are having trouble seeing the "Include Files" tab, you will need to
enable it in the global preference panel under "C/C++" /
"Property Pages Settings".
