if [ "$#" -ne 2 ]; then
    echo "Usage: extract.sh path_to_images path_to_txt_output"
    exit
fi

path=$1
outdir=$2
imgs="$path*.jpg"


size=$(ls -l  $imgs | wc -l)
echo "Going to extract bbox and tags for $size images"
for f in $imgs; do
	filename=$(basename $f)
	filename="${filename%.*}"
	dest="$outdir$filename.txt"
	echo Extracting from $filename
	./darknet detect cfg/yolov3.cfg ./yolov3.weights $f 2> /dev/null  > $dest
	echo Extracted to $dest
done
