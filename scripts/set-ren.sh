#!/bin/sh
#Update the ren pct
if [ $# -ne 2 ]; then
	echo "Usage: $0 file factor"
	"factor must be of the following form: (+|-|*|/) number"
	"example: '/2'; '/$3*100' to set to 100"
	exit 1
fi
awk -F\; "{print \$1,\$2,\$3$2,\$4}" OFS=\; $1