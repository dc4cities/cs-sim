#/bin/bash

dir="quickCheck"


if [ $# -ne 0 ]; then
    dir=$1
fi


format="%-30s %-10s %-5s %-10s %-2s\n"
printf "$format" Test Duration RenPct Profit "Optimal?"
for test in `ls $dir`; do
    out="$dir/$test"
    data="$out/optionConsolidator.csv"
    if [ -e $data ]; then
        pct=`tail -n 1 $out/objectives.csv|cut -d';' -f3`
        t=`tail -n 1 $data|cut -d';' -f 2`
        p=`tail -n 1 $data|cut -d';' -f 3`
        optim=`tail -n 1 $data|cut -d';' -f 4`
        printf "$format" $test $t $pct $p $optim
    fi
done