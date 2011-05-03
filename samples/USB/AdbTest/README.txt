AdbTest is a sample program that implements a subset of the adb USB protocol.
Currently it only implements the "adb logcat" command and displays the log
output in a text view and only allows connecting to one device at a time.
However the support classes are structured in a way that would allow
connecting to multiple devices and running multiple adb commands simultaneously.

This program serves as an example of the following USB host features:

- Matching devices based on interface class, subclass and protocol (see device_filter.xml)

- Asynchronous IO on bulk endpoints