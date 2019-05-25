#!/bin/bash
extrp=$1

for tagsf in *.txt; do
  filename=$(basename $tagsf)
  filename="${filename%.*}"
  metafname="im$(echo $filename | grep -o -E '[0-9]+').txt"
  metaf="$extrp/$metafname";
  if [ -f "$metaf" ]; then
	echo $metaf exists
  else
	echo Filling $metaf
	tags_content=$(tr -s '\r\n' ',' < $tagsf | sed -e 's/,$/\n/')
        echo $tags_content >> $metaf
  fi


done
