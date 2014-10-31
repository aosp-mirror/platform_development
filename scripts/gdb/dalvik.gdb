#  dump dalvik backtrace
define dbt
    if $argc == 1
        set $FP = $arg0
    else
        set $FP = $r5
    end

    set $frame = 0
    set $savedPC = 0
    while $FP
        set $stackSave = $FP - sizeof(StackSaveArea)
        set $savedPC = ((StackSaveArea *)$stackSave)->savedPc
        set $method = ((StackSaveArea *)$stackSave)->method
        printf "#%d\n", $frame
        printf "    FP = %#x\n", $FP
        printf "    stack save = %#x\n", $stackSave
        printf "    Curr pc = %#x\n", ((StackSaveArea *) $stackSave)->xtra.currentPc
        printf "    FP prev = %#x\n", ((StackSaveArea *) $stackSave)->prevFrame
        if $method != 0
            printf "    returnAddr: 0x%x\n", \
                   ((StackSaveArea *)$stackSave)->returnAddr
            printf "    class = %s\n", ((Method *) $method)->clazz->descriptor
            printf "    method = %s (%#08x)\n", ((Method *) $method)->name, $method
            printf "    signature = %s\n", ((Method *) $method)->shorty
            printf "    bytecode offset = 0x%x\n", (short *) (((StackSaveArea *) $stackSave)->xtra.currentPc) - (short *) (((Method *) $method)->insns)
            set $regSize = ((Method *) $method)->registersSize
            set $insSize = ((Method *) $method)->insSize
            set $index = 0
            while $index < $regSize
                printf "    v%d = %d", $index, ((int *)$FP)[$index]
                if $regSize - $index <= $insSize
                    printf " (in%d)\n", $insSize - $regSize + $index
                else
                    printf " (local%d)\n", $index
                end
                set $index = $index + 1
            end
        else
            printf "    break frame\n"
        end
        set $FP = (int) ((StackSaveArea *)$stackSave)->prevFrame
        set $frame = $frame + 1
    end
end

document dbt
    Unwind Dalvik stack frames. Argument 0 is the frame address of the top
    frame. If omitted r5 will be used as the default (as the case in the
    interpreter and JIT'ed code).
end

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
