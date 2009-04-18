#!/bin/bash
# $1 = output directory of generated docs: out/target/product/generic/obj/PACKAGING/pdkdocs_intermediates/generatedDocs/html
# fix a bug in doxygen 1.5.6 and higher...
# insert the line: '</div>\n' after line 25 in each generated source file: 
echo \</div\> > $1/div.tmp
for f in `find $1 -name '*-source.html' -print`
do
  head -n 25 $f > $f.head.tmp
  let count=$(wc -l $f  | cut -d\  -f 1 )
  count=$(($count-25))
  tail -n $count $f > $f.tail.tmp
  cat $f.head.tmp $1/div.tmp $f.tail.tmp > $f
done
rm $1/*.tmp
