#!/bin/sh
mkdir -p $1 > /dev/null
for d in 17 18 19 20; do
	for p in 60 70; do
		Rscript graph-fed.R --args ../../quickCheck/testHPTrialFederation70-d${d}-${p}-50pen ${p} -50 ${d}
		mv ../../quickCheck/testHPTrialFederation70-d${d}-${p}-50pen/*.png $1/
	done
done
