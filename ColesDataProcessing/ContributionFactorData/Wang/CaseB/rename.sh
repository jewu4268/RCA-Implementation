#!/bin/bash



for f in *.csv;
do sed '1 i\
node, cf\
 ' ${f}
 echo ${f}
done
