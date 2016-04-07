#/bin/sh

dir="quickCheck"

profit() {
    id=$1
    t=$2
    data=${id}/$t/optionConsolidator.csv
    if [ -e ${data} ]; then
        tail -n 1 $data|cut -d';' -f 3
        exit 0
    fi
    echo 0
}

if [ $# -ne 0 ]; then
    dir=$1
fi
baseline=$1
now=$2
format="%-30s %-20s %-20s %-10s %-5s\n"
printf "$format" Test ${baseline} ${now} Diff Ratio

for t in `ls ${now}`; do
    baseProfit=$( profit ${baseline} $t )
    nowProfit=$( profit ${now} $t )
    #echo $baseProfit $nowProfit
    diff=$( echo ${nowProfit} - ${baseProfit} |bc -l )
    ratio=$( echo "scale=2; ( ${nowProfit} - ${baseProfit} ) * 100 / ${baseProfit}" | bc -l )
    if [ ${nowProfit} -gt 0 -a ${baseProfit} -lt 0 ]; then
        ratio="n/a"
    elif [ ${baseProfit} -gt 0 -a ${nowProfit} -lt 0 ]; then
        ratio="n/a"
    else
        ratio=${ratio}%
    fi
    printf "$format" $t $baseProfit $nowProfit $diff $ratio
done