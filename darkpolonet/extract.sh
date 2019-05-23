#!/bin/bash

if [ "$#" -ne 3 ]; then
    echo "Usage: extract.sh path_to_images path_to_tags path_to_txt_output"
    exit
fi

path=$1
tagsp=$2
outdir=$3
imgs="$path*.jpg"
i=1


size=$(ls -l  $imgs | wc -l)
echo "Going to extract bbox and tags for $size images"
for f in $imgs; do
	filename=$(basename $f)
	filename="${filename%.*}"
	tagsf="tags$(echo $filename | grep -o -E '[0-9]+')"
	dest="$outdir/$filename.txt"
	echo "($i/$size) extracting from $filename..."
	./darknet detect cfg/yolov3-tiny.cfg ./yolov3-tiny.weights $f 2> /dev/null  > $dest
	echo Fetching human tags from $filename
	source_tag="$tagsp/$tagsf.txt"
	tags_content=$(tr -s '\r\n' ',' < $source_tag | sed -e 's/,$/\n/')
	echo $tags_content >> $dest
	echo Extracted to $dest
	((i++))
done
