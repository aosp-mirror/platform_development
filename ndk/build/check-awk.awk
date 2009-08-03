# This script is used to check that a given awk executable
# implements the match() and substr() functions appropriately.
#
# These were introduced in nawk/gawk, but the original awk
# does not have them.
#
END {
    RSTART=0
    RLENGTH=0
    s1="A real world example"
    if (! match(s1,"world")) {
        print "Fail match"
    } else if (RSTART != 8) {
        print "Fail RSTART ="RSTART
    } else if (RLENGTH != 5) {
        print "Fail RLENGTH ="RLENGTH
    } else {
        s2=substr(s1,RSTART,RLENGTH)
        if (s2 != "world") {
            print "Fail substr="s2
        } else {
            print "Pass"
        }
    }
}
