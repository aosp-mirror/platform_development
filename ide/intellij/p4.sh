#!/bin/sh

if [ $1 == "fstat" ] && [ $2 =~ ".*/out/.*" ]; then
  echo "$2 - file(s) not in client view." >&2
  exit 0
fi

exec /opt/local/bin/p4 $*
