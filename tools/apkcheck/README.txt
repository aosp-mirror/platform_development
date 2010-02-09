APK Checker

This compares the set of classes, fields, and methods used by an Android
application against the published API.

The public API description files live in the source tree, in
frameworks/base/api/.  The dependency set for an APK can be generated with
"dexdeps".

Use "apkcheck --help" to see a list of available options.


Due to limitations and omissions in the API description files, there may
be false-positives and false-negatives.  When possible these are emitted
as warnings rather than errors.  (You may need to specify "--warn" to
see them.)

In some cases involving generic signatures it may not be possible to
accurately reconstruct the public API.  Some popular cases have been
hard-coded into the program.  They can be included by adding the following
to the command line:

  --uses-library=BUILTIN

The "--uses-library" option allows you to specify additional API source
material.  In the future this may be useful for applications that include
libraries with the "uses-library" directive.


Example use:

% dexdeps out/target/product/sapphire/system/app/Gmail.apk > Gmail.apk.xml
% apkcheck --uses-library=BUILTIN frameworks/base/api/current.xml Gmail.apk.xml
Gmail.apk.xml: summary: 0 errors, 15 warnings


By using the numbered API files (1.xml, 2.xml) instead of current.xml you
can test the APK against a specific release.

