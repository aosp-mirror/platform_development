#
# Copyright (C) 2014 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# ART debugging.  ART uses SIGSEGV signals for internal purposes.  To allow
# gdb to debug programs using ART we need to treat this signal specially.  We
# also set a breakpoint in a libart.so function to stop when the program
# hits an unexpected breakpoint
set $art_debug_enabled = 0
define art-on
    if $art_debug_enabled == 0
        # deal with SIGSEGV signals
        handle SIGSEGV noprint nostop pass

        # set a breakpoint and record its number
        set breakpoint pending on
        break art_sigsegv_fault
        set $art_bpnum = $bpnum
        commands $art_bpnum
        silent
        printf "Caught SIGSEGV in user program\n"
        end
        set breakpoint pending auto

        printf "ART debugging mode is enabled.\n"
        printf "If you are debugging a native only process, you need to\n"
        printf "re-enable normal SIGSEGV handling using this command:\n"
        printf "  handle SIGSEGV print stop\n"
        set $art_debug_enabled = 1
    else
        printf "ART debugging mode is already enabled.\n"
    end
end

document art-on
    Enter ART debugging mode. In ART debugging mode, SIGSEGV signals are ignored
    by gdb unless they are not handled by ART itself.  A breakpoint is
    set to stop the program when an unexpected SIGSEGV signal is
    encountered.

    To switch ART debugging mode off, use "art-off"
end

define art-off
    if $art_debug_enabled == 1
        # restore SIGSEGV to its default
        handle SIGSEGV print stop pass

        # delete our breakpoint
        delete $art_bpnum

        set $art_debug_enabled = 0
        printf "ART debugging mode is disabled.\n"
    end
end

document art-off
    Leave ART debugging mode.  Signal handling is restored to default settings.

    Use the command "art-on" to enable ART debugging mode.
end
