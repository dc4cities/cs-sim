#!/bin/sh
#evaluate the option plan consolidate when the number of easc is varying

function run {
echo "Evaluation started. See the progress in ${output}/out.log"
env=$1
output=$2
mkdir -p ${output}
JARS=`ls lib/*.jar`
for JAR in ${JARS}; do
 CLASSPATH=${JAR}:${CLASSPATH}
done
JAVA_OPTS="-server -Xmx2G -Xms2G"
Main="java ${JAVA_OPTS} -cp ${CLASSPATH} eu.dc4cities.benchcs.Main"
for step in 100 200 400 600 800 1000; do
    echo "### Step ${step} ###" >> ${output}/out.log
    myEnv=${output}/step-${step}
	./scale-env.sh ${env} ${myEnv} \*${step}
case ${env} in
Trento)
    ${Main} --tc ${myEnv}/ctrl-backend/technical-configuration.json\
        --gc ${myEnv}/ctrl-backend/goal-configuration.json\
        --mockWMM\
        --eascs Trento:${step}\
        --source :${myEnv}/dc4es-service/grid.csv\
        --slots 1:384\
        --timeout 60\
        ${output}/${step} >> ${output}/out.log
        ;;
CSUC)
    ${Main} --tc ${myEnv}/ctrl-backend/technical-configuration.json\
        --gc ${myEnv}/ctrl-backend/goal-configuration.json\
        --mockWMM\
        --eascs CSUC:${step}\
        --source :${myEnv}/dc4es-service/grid.csv\
        --slots 1:384\
        --timeout 60\
        ${output}/${step} >> ${output}/out.log
        ;;
HP)
    ${Main} --tc ${myEnv}/ctrl-backend/technical-configuration.json\
        --gc ${myEnv}/ctrl-backend/goal-configuration.json\
        --mockWMM\
        --eascs HP:${step}\
        --source :${myEnv}/dc4es-grid/grid.csv\
        --source :${myEnv}/dc4es-pv/pv.csv\
        --slots 1:384\
        --timeout 60\
        ${output}/${step} >> ${output}/out.log
        ;;
esac
done
}

function results {
input=$1
echo "eascs;slot;type;duration" > ${input}/durations.csv
for step in 100 200 400 600 800 1000; do
    ./cons_durations2csv.pl ${input}/${step}/OptionConsolidatorImpl.log| grep -v 'duration'| sed "s/^/${step};/" >> ${input}/durations.csv
done
./nbEascs-durations.R ${input}
}

OP=$1
case ${OP} in
run)
if [ $# -ne 3 ]; then
	echo "Usage: $0 run base_env output"
	exit 1
fi
run $2 $3
    ;;
results)
if [ $# -ne 2 ]; then
    echo "Usage: $0 results output"
    exit 1
fi
results $2
    ;;
*)
    echo "Unsupported operation ${OP}"
    exit 1
esac