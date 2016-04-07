#!/bin/bash
################################################################################
##
## Script to generate several graphs if the build succeeds or is unstable.
## The script call mvn test to generate several samples, each test will be
## stored in a directory in ./test . The script must parse cvs
## results on all directories to create graph files using .R scripts
## available on the scripts folder.
##
##
##
################################################################################

echo "========> STARTING POST BUILD GRAPH GENERATION <========"

# After a build 'mvn test' was already called so no need to do it again.

# To test the script use the -testmode, otherwise the needed variables will
# not be set and the script will fail.
if [ "x$1" = "x-testmode" ] ; then
	DATEOFBUILD="Thu May 28 15:44:13 CEST 2015"
	BUILD_ID="2015-05-28_15-42-44"
	BUILD_NUMBER="XX"
	BUILD_URL="hackmeplz/jenkins/job/CS%20Simulator/21/"
	JOB_NAME="CS Simulator"
	JOB_URL="hackmeplz/jenkins/job/CS%20Simulator/"
fi

# If the variable of build id is empty, use fake values for test.
if [ x$BUILD_ID = "x" ] ; then
	DATEOFBUILD="Thu May 28 15:44:13 CEST 2015"
	BUILD_ID="2015-05-28_15-42-44"
	BUILD_NUMBER="XX"
	BUILD_URL="http://dc4cities.inria.fr:8080/jenkins/job/CS%20Simulator/21/"
	JOB_NAME="CS Simulator"
	JOB_URL="http://dc4cities.inria.fr:8080/jenkins/job/CS%20Simulator/"
fi



# Create a new directory to store this build plots.
mkdir -p ./html/${BUILD_NUMBER}

# Save the date to put on the HTML file.
DATEOFBUILD=`date`

# Dump the values to debug.
echo "DATEOFBUILD = ${DATEOFBUILD}"
echo "BUILD_ID= ${BUILD_ID}"
echo "BUILD_NUMBER = ${BUILD_NUMBER}"
echo "BUILD_URL = ${BUILD_URL}"
echo "JOB_NAME = ${JOB_NAME}"
echo "JOB_URL = ${JOB_URL}"


# For each test folder.
ls -1 ./test/ |
while read trial ; do
	echo "**************** Working on trial $trial **************** " ;

	# Plot, one R script per plot.
	for script in `ls -1 ./scripts/*.R` ; do
		echo "=====> Plotting with ${script}"
		./${script} ./test/${trial}
	done

	echo "=====> Generating png images if R lack png support"
	ls -1 ./test/${trial}/*.pdf  | sed -e 's/\.[^\.]*$//' |
		while read image_name ;  do
			if [ ! -f $image_name".png"  ]; then
				echo "Working on file ${image_name}"
				convert -trim ${image_name}.pdf \
					-resize 200% -quality 50 ${image_name}.png
			fi
	done

done

#Generate the daily numbers
for t in `ls ./test/`; do
    ./scripts/dailyNumbers.sh ./test/$t > ./test/$t/daily.txt
done

#The index file
scripts/make_index.pl "${DATEOFBUILD}" "${BUILD_ID}" "${BUILD_NUMBER}" "${BUILD_URL}" "${JOB_NAME}" "${JOB_URL}" > ./html/${BUILD_NUMBER}/index.html

# Copy the complete test directories to preserve build history.
cp -R ./test ./html/${BUILD_NUMBER}

# Create a link to last build report
echo "Linking last report on workspace/html/last"
rm -f ./html/last
ln -sfv ${BUILD_NUMBER} ./html/last
if [ -e ./html/last/index.html ]; then
    echo "Link to last report created succesfully!"
else
    echo  "Fail to link last report!"
fi

echo "========> FINISHED POST BUILD GRAPH GENERATION <========"