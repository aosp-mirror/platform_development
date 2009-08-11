SIMPLE PROTOCOL FOR AUTOMATED NETWORK CONTROL

The Simple Protocol for Automated Network Control was designed to be a
low-level way to programmability inject KeyEvents and MotionEvents
into the input system.  The idea is that a process will run on a host
computer that will support higher-level operations (like conditionals,
etc.) and will talk (via TCP over ADB) to the device in Simple
Protocol for Automated Network Control.  For security reasons, the
Monkey only binds to localhost, so you will need to use adb to setup
port forwarding to actually talk to the device.

INITIAL SETUP

Setup port forwarding from a local port on your machine to a port on
the device:

$ adb forward tcp:1080 tcp:1080

Start the monkey server

$ adb shell monkey --port 1080

Now you're ready to run commands

COMMAND LIST

Individual commands are separated by newlines.  The Monkey will
respond to every command with a line starting with OK for commands
that executed without a problem, or a line starting with ERROR for
commands that had problems being run.  The Monkey may decide to return
more information about command execution.  That information would come
on the same line after the OK or ERROR.  A possible example:

key down menu
OK
touch monkey
ERROR: monkey not a number

The complete list of commands follows:

key [down|up] keycode

This command injects KeyEvent's into the input system.  The keycode
parameter refers to the KEYCODE list in the KeyEvent class
(http://developer.android.com/reference/android/view/KeyEvent.html).
The format of that parameter is quite flexible.  Using the menu key as
an example, it can be 82 (the integer value of the keycode),
KEYCODE_MENU (the name of the keycode), or just menu (and the Monkey
will add the KEYCODE part).  Do note that this last part doesn't work
for things like KEYCODE_1 for obvious reasons.

Note that sending a full button press requires sending both the down
and the up event for that key

touch [down|up|move] x y

This command injects a MotionEvent into the input system that
simulates a user touching the touchscreen (or a pointer event).  x and
y specify coordinates on the display (0 0 being the upper left) for
the touch event to happen.  Just like key events, touch events at a
single location require both a down and an up.  To simulate dragging,
send a "touch down", then a series of "touch move" events (to simulate
the drag), followed by a "touch up" at the final location.

trackball dx dy

This command injects a MotionEvent into the input system that
simulates a user using the trackball. dx and dy indicates the amount
of change in the trackball location (as opposed to exact coordinates
that the touch events use)

flip [open|close]

This simulates the opening or closing the keyboard (like on dream).

wake

This command will wake the device up from sleep and allow user input.

tap x y
The tap command is a shortcut for the touch command.  It will
automatically send both the up and the down event.

press keycode

The press command is a shortcut for the key command.  The keycode
paramter works just like the key command and will automatically send
both the up and the down event.

OTHER NOTES

There are some convenience features added to allow running without
needing a host process.

Lines starting with a # character are considered comments.  The Monkey
eats them and returns no indication that it did anything (no ERROR and
no OK).

You can put the Monkey to sleep by using the "sleep" command with a
single argument, how many ms to sleep.
