#!/bin/bash
tagsp=$1
for f in *.txt; do
	fname=$(basename $f)
        filename=$(basename $f)
        filename="${filename%.*}"
        tagsf="tags$(echo $filename | grep -o -E '[0-9]+')"
	echo Processing $source_tag
        source_tag="$tagsp/$tagsf.txt"
        tags_content=$(tr -s '\r\n' ',' < $source_tag | sed -e 's/,$/\n/')
	echo $tags_content >> $f
done
