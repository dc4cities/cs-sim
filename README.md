# Central system simulator

This project allows to simulate the central system.
It is convenient to check the behavior of the EASC, the IPP, the option plan consolidation, and more, simulating
the energy forecasts. The projects folder has several scripts to plot posteriori to build graphs that show the
expected behavior on many trials. 


## Quick start

Compile the project.

`mvn compile`

Do some basic tests.

`mvn test`

This generates several files on test (located at maven ${base_dir}). Six folders should appear each
containing the results of an hypothetical Trial. The detailed command line parameters for each simulation
can be seen below.

- testCsucPv 

    - `tc ./env/hp/ctrl-backend/technical-configuration.json`
    - `gc ./env/hp/ctrl-backend/goal-configuration.json`
    - `eascs csuc:1`
    - `source grid:./env/hp/dc4es-service/grid-large.csv`
    - `source pv:./env/hp/dc4es-service/pv.csv`
    - `slots 1:441`
    - `./test/testCsucPv`

- testCsucTrial

    - `tc ./env/csuc/ctrl-backend/technical-configuration.json`
    - `gc ./env/csuc/ctrl-backend/goal-configuration.json`
    - `eascs csuc:1`
    - `source ./env/csuc/dc4es-service/grid.csv`
    - `slots 1:441`
    - `./test/testCsucTrial`

- testHpNoPv
    
    - `tc ./env/hp/ctrl-backend/technical-configuration.json`
    - `gc ./env/hp/ctrl-backend/goal-configuration.json`
    - `eascs hp:1`
    - `source grid:./env/hp/dc4es-service/grid.csv`
    - `slots 1:481`
    - `./test/testHpNoPv`

- testHpTrial
    
    - `tc ./env/hp/ctrl-backend/technical-configuration.json`
    - `gc ./env/hp/ctrl-backend/goal-configuration.json`
    - `eascs hp:1`
    - `source grid:./env/hp/dc4es-service/grid.csv`
    - `source pv:./env/hp/dc4es-service/pv.csv`
    - `slots 1:481`
    - `./test/testHpTrial`

- testTrentoPv
    
    - `tc ./env/hp/ctrl-backend/technical-configuration.json`
    - `gc ./env/hp/ctrl-backend/goal-configuration.json`
    - `eascs trento:1`
    - `source grid:./env/hp/dc4es-service/grid-large.csv`
    - `source pv:./env/hp/dc4es-service/pv.csv`
    - `slots 1:481`
    - `./test/testTrentoPv`

- testTrentoTrial

    - `tc ./env/trento/ctrl-backend/technical-configuration.json`
    - `gc ./env/trento/ctrl-backend/goal-configuration.json`
    - `eascs trento:1`
    - `source ./env/trento/dc4es-service/grid.csv`
    - `slots 1:577`
    - `./test/testTrentoTrial`



You can now generate the continuous integration report using the post-build script on test mode. This is the 
same script run by jenkins at each build.

`./scripts/post-build-jenkins-generate-graphs.sh -testmode`

Several images will be generated and put inside ./html/21. Access ./html/21/index.html to see 
the graphs. You can also navigate on the ./html/21 directory to see pdf files in vectorial
format.



## How to play?

- You can Run the simulator from your IDE through the `Main` class. Play with arguments when needed or see the Test 
class `CentralSystemSimulatorTest` to replay phase-1 trials.

- You can also simulator from a shell using the following commands.

    - build the distribution: `mvn package -DskipTests`.

    - unzip the resulting archive and run `sim.sh`.

Once a simulation is terminated several .csv and .log files are available in the output dir stated initially.

For plotting, [R](http://www.r-project.org/) is used. Launch `plot.sh` to plot everything.
In practice, each R script takes as parameter the output folder. Resulting graphs will be stored in the output folder

- script `power.R` to plot in `power.pdf` the power usage in terms of kWatts (cumulative and the proportion issued from a renewable source). Having the `any` line stalling might denote the lack of adaptation from the EASC
- script `ren-efficiency.R` to plot in `ren-efficiency.pdf` the consumption of renewable energy with regards to its capacity. This threshold is computed as the usage from non-pure source plus the capacity of pure sources.
- script `wm-dist.R` to plot in `wm.pdf` the output folder in parameter to plot the working mode distribution per template.
- script `perf.R` to plot in `perf.pdf` the performance per template.
- script `cons-duration.R` to plot in `cons-duration.pdf` the progress of the solving progress in terms of duration
- script `cons-quality.R` to plot in `cons-quality.pdf` the additional saving due to the optimization process with regards to the first solution.

## Uploading to KairosDB

In addition to running in standalone mode as explained above, the simulator can be configured to upload metrics produced during the simulation to an instance of KairosDB. This is used in the demo kit to integrate with the Energis dashboard, but it is also useful to verify test results based on the charts produced by the KairosDB as an alternative to PDF files generated with the above scripts.

In order to use:

- Download the latest KairosDB from https://github.com/kairosdb/kairosdb/releases
- Install KairosDB as explained at http://kairosdb.github.io/website/docs/build/html/GettingStarted.html#install
- Start KairosDB. In the following we will assume KairosDB is running at http://localhost:8080
- Run the simulator as usual, but add the `hdb` command line switch with the address of KairosDB: `--hdb http://localhost:8080`
- After the simulation is finished, open the KairosDB UI at http://localhost:8080 and check the desired charts

**WARNING:** do not add the `--hdb` switch in the `CentralSystemSimulator` class as there is no KairosDB running on Jenkins. Use if for local testing only.

The simulator uploads all metrics supported by the Control System. Of course the simulator cannot measure actual data, so all metrics with reference ACTUAL are derived based on forecast CSV files and the EASC AppConfig file for working mode performance and consumption.

## Escalation Manager and alerts

Status analysis via the escalation manager is not executed by default, but it can be enabled using the `--escalation-manager` command line switch. When enabled, the escalation manager produces a list of alerts related to the status of the system (e.g. usage of renewables below the objective in a certain time slot). If the historical database is enabled with the `--hdb` switch, these alerts are uploaded together with the other metrics. Furthermore, some textual messages describing the alerts are uploaded to Energis (because the historical DB cannot hold text values). This requires two additional command line switches with Energis parameters: `--energis` (the Energis API URL) and `--energis-api-key` (the authentication key for the API).
So an example of command line with both the historical DB and the escalation manager enabled is:
`--hdb http://localhost:8081 --escalation-manager --energis http://localhost/energiscloud-gateway/restful/api --energis-api-key abcd1234efgh5678`.

## Todo

- evaluation of the OptionPlanConsolidator
    - vary the number of EASCS: 1 - 10 - 50 - 75 - 100 .... (1minutes) what is the percentage wrt. timeslot width || context-switch duration
    - vary the number of alternatives per EASC
    - evaluation metrics: absolute duration, relative duration (% of a loop), proved optimality or not
