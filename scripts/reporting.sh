#!/bin/bash
mkdir -p ./html/${BUILD_NUMBER} > /dev/null
./scripts/quick-report.sh quickCheck > ./html/${BUILD_NUMBER}/quick-reports.txt
mv quickCheck ./html/${BUILD_NUMBER}/
prev=$[ ${BUILD_NUMBER} - 1 ]
./scripts/quick-diff.sh ./html/${prev}/quickCheck ./html/${BUILD_NUMBER}/quickCheck > ./html/${BUILD_NUMBER}/quick-diff.txt
