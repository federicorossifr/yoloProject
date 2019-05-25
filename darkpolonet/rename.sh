#!/bin/bash
for file in *.jpg.txt; do
    mv "$file" "$(basename "$file" .jpg.txt).txt"
done
