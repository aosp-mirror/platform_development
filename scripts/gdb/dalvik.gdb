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
