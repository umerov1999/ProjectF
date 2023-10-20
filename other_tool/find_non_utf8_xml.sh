find . -name "*.xml" -exec grep -rl 'encoding="utf-8"' --files-without-match {} \; > 1.txt

cat 1.txt | while read y
do
sed -i '1s/^/<?xml version="1.0" encoding="utf-8"?>\n/' $y
done
