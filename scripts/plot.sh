#!/bin/sh
if [ $# -ne 1 ]; then
    echo "Usage: $0 output_folder"
    exit 1
fi
#scripts to reformat
./cons_durations2csv.pl $1/OptionConsolidatorImpl.log > $1/cons-durations.csv
./cons_quality2csv.pl $1/OptionConsolidatorImpl.log > $1/cons-quality.csv
#plot
for script in power wm-dist ren-efficiency perf cons-quality cons-durations; do
    echo "Plotting with ${script}"
    ./${script}.R $1
done
open $1/*.pdf