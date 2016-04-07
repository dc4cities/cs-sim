#!/bin/sh

dir=$1
format="%-10s %-10s %-10s\n"
printf "$format" Day RenPct Profit
for d in `cat $dir/objectives.csv|grep -v slot`; do
    slot=`echo $d|cut -d';' -f1`
    pct=`echo $d|cut -d';' -f3`
    slot=$( echo "$slot / 96" |bc )
    #catch the profit
    profit=`cat $dir/optionConsolidator.csv|grep "$slot;"|tail -n1|cut -d';' -f3`
    printf "$format" $slot $pct% $profit
done