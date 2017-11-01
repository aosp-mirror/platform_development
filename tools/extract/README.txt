This tool can be used to extract platform annotations, for use by
Android Studio, IntelliJ, and lint. It creates a .zip file with
external annotations in a format that IntelliJ and lint can read. This
allows annotations to live separately from the actual library's .class
files.  This is particularly useful for annotations that have source
retention that we still want to allow the IDE to be aware
of. Furthermore, for the typedef annotations in particular, compiled
annotations cannot hold all the information we want to capture (e.g. a
reference to the actual field that is part of the typedef, not its
inlined value.)

To build it, run "gradle installApp", then look in build/install for
the extract command.
