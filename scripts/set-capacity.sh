#!/bin/sh
#Update the power capacity by a constant
if [ $# -ne 2 ]; then
	echo "Usage: $0 file factor"
	"factor must be of the following form: (+|-|*|/) number"
	"example: '/2'; '/$2*100000' to set to 100000"
	exit 1
fi
awk -F\; "{print \$1,\$2$2,\$3,\$4}" OFS=\; $1