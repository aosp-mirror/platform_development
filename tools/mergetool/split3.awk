#!/usr/bin/awk -f

# Supports only "simple" diff3 style conflicts. Criss-cross conflicts are not supported.

BEGIN {
  if (TARGET !~ /^(LOCAL|BASE|REMOTE)$/) {
    print "Usage: ./split3.awk <file_with_diff3_conflict_markers -v TARGET={LOCAL,BASE,REMOTE}"
    exit 1
  }

  PRINT = 1
}

/^<{7}( .+)?$/ {
  PRINT = (TARGET == "LOCAL")
  next
}

/^\|{7}( .+)?$/ {
  PRINT = (TARGET == "BASE")
  next
}

/^={7}( .+)?$/ {
  PRINT = (TARGET == "REMOTE")
  next
}

/^>{7}( .+)?$/ {
  PRINT = 1
  next
}

PRINT { print }
