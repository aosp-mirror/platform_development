# Script to start "monkey" on the device, which has a very rudimentary
# shell.
#
base=/system
export CLASSPATH=$base/framework/monkey.jar
trap "" HUP
for a in "$@"; do
    echo "  bash arg:" $a
done
exec app_process $base/bin com.android.commands.monkey.Monkey "$@"

