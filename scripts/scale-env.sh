#!/bin/sh
#Scale the power sources of an environment by a factor
#A new environment is then created
if [ $# -ne 3 ]; then
    echo "Usage: $0 source_env output constant"
    echo "Create a new environment from an existing one in 'env'."
    echo "the power production will be altered by a constant. See `set_capacity.sh` for more details"
    exit 1
fi
SRC=$1
DST=$2
CONSTANT=$3
if [ -d ${DST} ]; then
    echo "Output environment ${DST} already exists"
    exit 1
fi
cp -r env/${SRC} ${DST} || exit 1
#scan every power files. There should be in dc4es-*
for CSV in `ls env/${SRC}/dc4es-*/*.csv`; do    
    file=`basename ${CSV}`
    dir=`dirname ${CSV}|cut -d '/' -f3`
    ./set-capacity.sh ${CSV} ${CONSTANT} > ${DST}/${dir}/${file}
done

