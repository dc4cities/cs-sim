#!/bin/sh
#Define the classpath
JARS=`ls lib/*.jar`

for JAR in $JARS; do
 CLASSPATH=$JAR:$CLASSPATH
done

Main="java $JAVA_OPTS -cp $CLASSPATH eu.dc4cities.benchcs.Main"

if [ $# -eq 0 ]; then
    echo "Usage: $0 (trento|hp|csuc|...)"
    echo "trento, hp, csuc are the preconfigured scenarios to reproduce trials"
    echo "for a custom scenario, use '-h' to get the CLI options"
    exit 1
fi
case $1 in
Trento)
    ${Main} --tc env/Trento/ctrl-backend/technical-configuration.json\
        --gc env/Trento/ctrl-backend/goal-configuration.json\
        --eascs Trento:1\
        --source :env/Trento/dc4es-service/grid.csv\
        --slots 1:384
        ;;
CSUC)
    ${Main} --tc env/CSUC/ctrl-backend/technical-configuration.json\
        --gc env/CSUC/ctrl-backend/goal-configuration.json\
        --eascs CSUC:1\
        --source :env/CSUC/dc4es-service/grid.csv\
        --slots 1:384
        ;;
HP)
    ${Main} --tc env/HP/ctrl-backend/technical-configuration.json\
        --gc env/HP/ctrl-backend/goal-configuration.json\
        --eascs HP:1\
        --source :env/HP/dc4es-grid/grid.csv\
        --source :env/HP/dc4es-pv/pv.csv\
        --slots 1:384
        ;;
*)
    ${Main} $*
esac