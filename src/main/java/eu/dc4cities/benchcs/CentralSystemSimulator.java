/*
 * Copyright 2012 The DC4Cities author.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dc4cities.benchcs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.measure.unit.NonSI;

import org.joda.time.DateTime;
import org.jscience.physics.amount.Amount;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import eu.dc4cities.benchcs.mock.MockEascHandler;
import eu.dc4cities.benchcs.mock.MockErdsHandler;
import eu.dc4cities.configuration.goal.GoalConfiguration;
import eu.dc4cities.configuration.loader.JsonLoader;
import eu.dc4cities.configuration.repository.ConfigurationRepository;
import eu.dc4cities.configuration.repository.LocalResourceConfigurationRepository;
import eu.dc4cities.configuration.technical.DataCenterConfiguration;
import eu.dc4cities.configuration.technical.TechnicalConfiguration;
import eu.dc4cities.controlsystem.model.TimeParameters;
import eu.dc4cities.controlsystem.model.TimeSlotBasedEntity;
import eu.dc4cities.controlsystem.model.datacenter.DataCenterExecutionPlan;
import eu.dc4cities.controlsystem.model.datacenter.DataCenterStatus;
import eu.dc4cities.controlsystem.model.easc.Activity;
import eu.dc4cities.controlsystem.model.easc.ActivityDataCenter;
import eu.dc4cities.controlsystem.model.easc.EascActivityPlan;
import eu.dc4cities.controlsystem.model.easc.EascMetrics;
import eu.dc4cities.controlsystem.model.easc.Work;
import eu.dc4cities.controlsystem.model.erds.DataCenterForecast;
import eu.dc4cities.controlsystem.model.json.JsonUtils;
import eu.dc4cities.controlsystem.modules.EascHandler;
import eu.dc4cities.controlsystem.modules.EscalationManager;
import eu.dc4cities.controlsystem.modules.PowerPlanner;
import eu.dc4cities.controlsystem.modules.PowerSplitter;
import eu.dc4cities.controlsystem.modules.escalationmanager.EscalationManagerImpl;
import eu.dc4cities.controlsystem.modules.optionconsolidator.OptionConsolidatorImpl;
import eu.dc4cities.controlsystem.modules.optionconsolidator.opcp2.reducer.Pass;
import eu.dc4cities.controlsystem.modules.optionconsolidator.opcp2.reducer.Reducer;
import eu.dc4cities.controlsystem.modules.powerplanner.AggressivAdaptPowerPlannerImpl;
import eu.dc4cities.controlsystem.modules.powersplitter.PowerSplitterImpl;
import eu.dc4cities.controlsystem.modules.processcontroller.ControlLoop;
import eu.dc4cities.controlsystem.modules.processcontroller.DataCenterOptimization;
import eu.dc4cities.controlsystem.modules.processcontroller.FederationStatus;
import eu.dc4cities.controlsystem.modules.processcontroller.HdbDataCenterMetrics;
import eu.dc4cities.controlsystem.modules.processcontroller.HistoricalDbDao;
import eu.dc4cities.easc.EASC;

/**
 * Simulate a central system that coordinate EASCs. For now it works creating directly Java objects that communicate
 * with each other using the network. To provide continuous integration on the future this simulator will use the
 * network to be more close to a real execution.
 *
 *
 * See CentralSystemSimulatorTest to simulate trials
 */
public class CentralSystemSimulator {

    private List<EASC> eascs;

    private Map<String, String> sources;

    private TechnicalConfiguration technicalConfiguration;

    private GoalConfiguration goalConfiguration;

    private Statistics stats;

    private DateTime from, to;

    public boolean longOutput = false;
    public boolean profitBased = true;
    private boolean optimize = true;
    //Variables to control simulation timeslot.
    private int startAt;
    private int stopAt;
    private int now;

    private String output;

    private boolean saveCommunications;

    private boolean ippBasedHeuristic = true;
    private Reducer reducer = new Pass();
    /**
     * The consolidator timeout value in secons. 9 by default (1% of a timeslot.)
     */
    private int timeOut = 9;

    /**
     * Every slot by default.
     */
    private int interval = 1;

    private boolean split = true;

    private String historicalDbUrl =  null;
    private HistoricalDbDao historicalDbDao = null;
    private String energisUrl = null;
    private String energisApiKey = null;
    private boolean escalationManagerEnabled = false;

    private Map<String, List<String>> replays;

    public CentralSystemSimulator() {
        eascs = new ArrayList<>();
        sources = new HashMap<>();
        startAt = 0;
        replays = new HashMap<>();
    }

    /**
     * Sets the interval between two optimization runs.
     * Value is provided in terms of number of slots.
     *
     * @param interval the number of slots between two optimization runs.
     */
    public void interval(int interval) {
        this.interval = interval;
    }

    /**
     * @return the number of slots between two optimization phases.
     */
    public int interval() {
        return interval;
    }

    /**
     * Set the technical configuration file.
     *
     * @param technicalConfiguration new configuration object.
     */
    public void set(TechnicalConfiguration technicalConfiguration) {
        this.technicalConfiguration = technicalConfiguration;
    }

    /**
     * Change the goalConfiguration.
     *
     * @param goalConfiguration new configuration object.
     */
    public void set(GoalConfiguration goalConfiguration) {
        this.goalConfiguration = goalConfiguration;
    }

    /**
     * Get the registered eascs.
     *
     * @return an initialized list.
     */
    public List<EASC> getEascs() {
        return eascs;
    }

    /**
     * Get an easc from its app name.
     *
     * @param name the easc name.
     * @return the easc object or {@code null} if there is no match.
     */
    public EASC getEascByAppName(String name) {
        for (EASC e : eascs) {
            if (e.getAppConfig().getName().equals(name)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Simple function to retrieve the current simulation timeslot.
     *
     * @return current simulation timeslot.
     */
    public int now() {
        return now;
    }

    /**
     * Add a new power  source csv file.
     *
     * The resulting source identifier will be formatted as 'dc:name'
     * @param dc the datacenter identifier
     * @param name name of this power source.
     * @param path path to csv file with power availability histogram.
     */
    public void addPowerSource(String dc, String name, String path) {
        sources.put(dc + ":" + name, path);
    }

    public Set<String> getSources() {
        return sources.keySet();
    }

    public String getSourceFile(String n) {
        return sources.get(n);
    }

    /**
     * Return the output directory of this simulation, this is normally given as parameter to Main.
     *
     * @return the output directory with path.
     */
    public String output() {
        return output;
    }

    /**
     * Set the output directory.
     *
     * @param output new output directory with path.
     */
    public void output(String output) {
        this.output = output;
    }

    public void setDateFrom(DateTime d) {
        this.from = d;
    }

    public void setDateTo(DateTime d) {
        this.to = d;
    }
    /**
     * Set the starting timeslot to the simulation, generally passed through command line.
     *
     * @param startAt timeslot to start the simulation.
     */
    public void startAt(int startAt) {
        this.startAt = startAt;
    }

    /**
     * Set the finishing timeslot (exclusive).
     *
     * @param s timeslot to stop the execution.
     */
    public void stopAfter(int s) {
        this.stopAt = s;
    }

	public String getHistoricalDbUrl() {
		return historicalDbUrl;
	}

	/**
     * Enable uploading of data to the given historical DB instance. Note that hdbX properties in
     * technical-configuration.json are ignored (even when hdbEnabled = true) to protect from accidental overwriting of
     * data by the simulator. Use of the historical DB must be enabled explicitly via the --hdb command line switch.
     * <p>
     * The companyCode attribute must be set in technical-configuration.json for the upload to work.
     * 
     * @param url the URL of historical DB host, e.g. http://localhost:8080
     */
    public void setHistoricalDbUrl(String url) {
    	this.historicalDbUrl = url;
    }
    
	public String getEnergisUrl() {
		return energisUrl;
	}

	/**
     * Sets the Energis API URL used to upload alerts generated by the escalation manager. This parameter is required
     * when both the historical DB and the escalation manager are enabled, and ignored otherwise.
     * 
     * @param url the URL of the Energis API, e.g. http://localhost/energiscloud-gateway/restful/api
     */
    public void setEnergisUrl(String url) {
    	this.energisUrl = url;
    }
    
	public String getEnergisApiKey() {
		return energisApiKey;
	}

	/**
	 * Sets the key to use with the Energis API.
	 * 
	 * @param energisApiKey the Energis API key
	 */
	public void setEnergisApiKey(String energisApiKey) {
		this.energisApiKey = energisApiKey;
	}

	/**
     * Set a flag to log/unlog communications.
     *
     * @param saveCommunications use true to turn communication log on, false to turn off.
     */
    public void saveCommunications(boolean saveCommunications) {
        this.saveCommunications = saveCommunications;
    }

    /**
     * Get the value of log communications flag.
     *
     * @return value of saveCommunicaitons flag, true means logging is ON, false means logging OFF.
     */
    public boolean saveCommunications() {
        return saveCommunications;
    }

	public boolean isEscalationManagerEnabled() {
		return escalationManagerEnabled;
	}

	/**
     * Sets whether plan analysis with the escalation manager should be enabled.
     *
     * @param enabled true to enable the escalation manager, false otherwise
     */
    public void setEscalationManagerEnabled(boolean enabled) {
        this.escalationManagerEnabled = enabled;
    }

    /**
     * Method to initialize a configuration repository, the configuration
     * repository must have two files:
     *
     * goal-configuration.json
     * technical-configuration.json
     *
     * @return configuration properly instantiated.
     */
    public ConfigurationRepository configurationRepository() {
        ClassPathResource goalResource = new ClassPathResource("goal-configuration.json");
        ClassPathResource technicalResource = new ClassPathResource("technical-configuration.json");
        return new LocalResourceConfigurationRepository(new JsonLoader(), goalResource, technicalResource);
    }

    /**
     * Imitate the behavior of a CentralSystem, some of the code here
     * is stolen from centralsystem because the centralsystem package is private.
     *
     * @throws IOException
     */
    public void run() throws IOException {
    	
    	if (historicalDbUrl != null) {
    		if (escalationManagerEnabled) {
    			historicalDbDao = new HistoricalDbDao(historicalDbUrl, energisUrl, energisApiKey, 
    					technicalConfiguration.getCompanyCode(), newEnergisRestTemplate());
    		} else {
    			historicalDbDao = new HistoricalDbDao(historicalDbUrl, technicalConfiguration.getCompanyCode());
    		}
    	}
        
        File root = new File(output);
        if (!root.exists() && !root.mkdirs()) {
            throw new IOException("Unable to create folder '" + output + "'");
        }
        stats = new Statistics(root);

        OptionConsolidatorImpl optionConsolidator = new OptionConsolidatorImpl();
        optionConsolidator.doOptimize(optimize);
        optionConsolidator.split(split);
        optionConsolidator.reducer(reduceWith());
        optionConsolidator.profitBased = profitBased;
        for (Map.Entry<String, List<String>> e : replays.entrySet()) {
            optionConsolidator.replay(e.getKey(), e.getValue());
        }
        //Set the objectives.
        //optionConsolidator.setPowerConfig(goalConfiguration.getGoals().get(0).getObjectives());
        optionConsolidator.setTimeout(timeOut);
        String heuristic = technicalConfiguration.getConsolidationHeuristic();
		if (heuristic != null) {
			optionConsolidator.setHeuristic(heuristic);
		}

        if (heuristic == null) {
            if (ippBasedHeuristic) {
                optionConsolidator.setHeuristic(OptionConsolidatorImpl.HEURISTIC_IPP);
            } else {
                optionConsolidator.setHeuristic(OptionConsolidatorImpl.HEURISTIC_INTERNAL);
            }
        }
        //Create mock to handle this EASC.
        MockEascHandler eascHandler = new MockEascHandler(this);

        //Create a mock to handle ERDS.
        MockErdsHandler erdsHandler = new MockErdsHandler(this, MockErdsHandler.DATE_PATTERN, ";", 1.0);

        //Create empty list for planners and splitters, each datacenter must have a dedicated one.
        Map<String, PowerPlanner> powerPlanners = new HashMap<>();
        Map<String, PowerSplitter> powerSplitters = new HashMap<>();
        // Escalation Managers are created only if enabled, a null map means escalation is disabled
        Map<String, EscalationManager> escalationManagerMap = null;
        if (escalationManagerEnabled) {
            escalationManagerMap = new HashMap<>();
        }

        // For each datacenter add a planner and a splitter.
        for (DataCenterConfiguration dataCenterConfig : technicalConfiguration.getDataCenters()) {
            String dataCenterName = dataCenterConfig.getName();
            AggressivAdaptPowerPlannerImpl powerPlanner = new AggressivAdaptPowerPlannerImpl();
            powerPlanner.setDCMinPower(dataCenterConfig.getMinPower());
            powerPlanner.setDCMaxPower(dataCenterConfig.getMaxPower());
            if (dataCenterConfig.getAggressiveness() != null) {
            	powerPlanner.setAlpha(dataCenterConfig.getAggressiveness());
            }
            powerPlanners.put(dataCenterName, powerPlanner);
            powerSplitters.put(
                    dataCenterName,
                    new PowerSplitterImpl(dataCenterName, technicalConfiguration.getDataCenter(dataCenterName).getEascGroups())
            );
            if (escalationManagerEnabled) {
                EscalationManagerImpl escalationManager = new EscalationManagerImpl();
                escalationManager.setWarnThreshold(75);
                escalationManagerMap.put(dataCenterName, escalationManager);
            }
        }

        //Instantiate a controlLoop to manage EASCs on centra_system level.
        ControlLoop controlLoop = new ControlLoop(
                technicalConfiguration,
                goalConfiguration,
                powerPlanners,
                powerSplitters,
                eascHandler,
                optionConsolidator,
                escalationManagerMap);

        List<DataCenterExecutionPlan> currentPlans = null;

        //Set the time using the new TimeParameters class.
        TimeParameters timeSlot = new TimeParameters();
        int timeSlotWidthMinutes = 15;
        timeSlot.setTimeSlotDuration(Amount.valueOf(timeSlotWidthMinutes, NonSI.MINUTE));
        if (from != null && to != null) {
            timeSlot.setDateFrom(from);
            timeSlot.setDateNow(from);
            startAt = 0;
            stopAt = (int) ((to.getMillis() - from.getMillis()) / (1000 * timeSlotWidthMinutes * 60));
        } else {
            DateTime startTime = erdsHandler.getFrom();
            startTime = startTime.plusMinutes(startAt);
            timeSlot.setDateFrom(startTime);
            timeSlot.setDateNow(startTime);
        }
        timeSlot.setDateTo(timeSlot.getDateFrom().plusDays(1));

        // FederationStatus holds the state of the system, with metrics from the past part of the day
        FederationStatus federationStatus = new FederationStatus(timeSlot.getDateFrom(), technicalConfiguration);

        for (now = startAt; now < stopAt; now += interval) {
            System.out.println("Timeslot " + now + " - " + timeSlot.getDateFrom() + " -> " + timeSlot.getDateTo());
            extendToEndOfDay(timeSlot);
            
			TimeSlotBasedEntity forecastEntity = new TimeSlotBasedEntity(timeSlot);
            forecastEntity.setDateFrom(timeSlot.getDateFrom().withTimeAtStartOfDay());
            List<DataCenterForecast> forecasts = erdsHandler.getEnergyForecasts(forecastEntity);
            federationStatus.setDataCenterForecasts(forecasts);
            
            // Update easc metrics.
            List<EascMetrics> eascMetrics = updateEascMetrics(timeSlot, eascHandler, federationStatus, now == startAt);

            //It's time to perform an optimisation
            List<DataCenterOptimization> optimizations = controlLoop.execute(
                    timeSlot,
                    forecasts,
                    federationStatus.getDataCenterPowerActuals(),
                    federationStatus.getEascServiceLevels(),
                    eascMetrics,
                    currentPlans);
            currentPlans = DataCenterOptimization.getExecutionPlans(optimizations);
            List<DataCenterStatus> statuses = DataCenterOptimization.getStatuses(optimizations);
            List<EascActivityPlan> activityPlans = optionConsolidator.getLastStatistics().retainedPlan();
            
            if (historicalDbDao != null) {
                historicalDbDao.writeExecutionPlanMetrics(currentPlans);
                if (statuses.size() > 0) {
                	historicalDbDao.writeStatusMetrics(statuses);
                }
            }
            
            //Log the eascs.csv file containing easc usage per timeslot and datacenter.
            stats.storePlan(now, interval, timeSlot, activityPlans);
            //Log information of messages exchange for debug on .log file.
            stats.store(now, optionConsolidator.getLastStatistics());
            if (statuses.size() > 0) {
            	stats.storeAlerts(now, interval, statuses);
            }

            if (longOutput) {
                stats.storeReplay(optionConsolidator.getLastStatistics());
                stats.storeLong(optionConsolidator.getLastStatistics(), forecasts);
            }
            
            if (interval > 1) {
            	// Simulate progress of activities at each time slot to update easc metrics and the federation status
            	// consistently, including upload of metrics to HDB when enabled.
            	fastForwardActivityPlans(activityPlans, 1, interval, eascHandler, federationStatus);
            }
            
            //Prepare next step.
            moveOn(
                    timeSlot,
                    timeSlot.getDateFrom().plusMinutes(technicalConfiguration.getTimeSlotWidth() * interval)
            );
        }
    }

    public Reducer reduceWith() {
        return reducer;
    }

    public CentralSystemSimulator reduceWith(Reducer r) {
        reducer = r;
        return this;
    }

    private void extendToEndOfDay(TimeParameters range) {
        DateTime dateTo = range.getDateTo();
        if (dateTo.getMillisOfDay() != 0) {
            range.setDateTo(dateTo.withTimeAtStartOfDay().plusDays(1));
        }
    }

    private List<EascMetrics> updateEascMetrics(TimeParameters timeParameters, EascHandler eascHandler, 
    		FederationStatus federationStatus, boolean isFirstMeasurement) {
    	List<EascMetrics> eascMetrics = eascHandler.getMetrics(timeParameters);
    	DateTime dateNow = timeParameters.getDateNow();
        federationStatus.updateEascMetrics(dateNow, eascMetrics);
        if (historicalDbDao != null) {
        	writeEascMetricsToHistoricalDb(dateNow, federationStatus, isFirstMeasurement);
        }
        return eascMetrics;
    }
    
	private void writeEascMetricsToHistoricalDb(DateTime date, FederationStatus federationStatus,
			boolean isFirstMeasurement) {
    	List<HdbDataCenterMetrics> hdbMetrics = federationStatus.getHdbMetrics();
    	// Pretend we have a measurement every 5 minutes in the last time slot, unless we are at the first measurement
    	// (simulation just begun).
    	DateTime measurementTime = isFirstMeasurement ? date : date.minusMinutes(10);
    	while (measurementTime.compareTo(date) <= 0) {
    		historicalDbDao.writeDataCenterMetrics(measurementTime, hdbMetrics);
    		measurementTime = measurementTime.plusMinutes(5);
    	}
    }
    
	private void fastForwardActivityPlans(List<EascActivityPlan> activityPlans, int startTimeSlot, int endTimeSlot,
			EascHandler eascHandler, FederationStatus federationStatus) {
		Map<String, Work> workMap = buildWorkMap(activityPlans, startTimeSlot, endTimeSlot);
		List<EascActivityPlan> miniPlans = copyPlanStructure(activityPlans);
		TimeSlotBasedEntity timeRange = new TimeSlotBasedEntity(activityPlans.get(0));
		DateTime startDate = timeRange.getDateFrom();
		int timeSlotMinutes = (int) timeRange.getTimeSlotDuration().longValue(NonSI.MINUTE);
		for (int ts = startTimeSlot; ts < endTimeSlot; ts++) {
			timeRange.setDateFrom(startDate.plusMinutes(timeSlotMinutes * ts));
			timeRange.setDateTo(timeRange.getDateFrom().plusMinutes(timeSlotMinutes));
			TimeParameters timeParams = new TimeParameters(timeRange.getDateFrom(), timeRange);
			updateEascMetrics(timeParams, eascHandler, federationStatus, false);
			for (EascActivityPlan easc : miniPlans) {
				String eascName = easc.getEascName();
				easc.copyIntervalFrom(timeRange);
				for (Activity activity : easc.getActivities()) {
					String activityName = activity.getName();
					for (ActivityDataCenter dataCenter : activity.getDataCenters()) {
						String dataCenterName = dataCenter.getDataCenterName();
						Work referenceWork = workMap.get(getWorkKey(eascName, activityName, dataCenterName, ts));
						Work miniWork = new Work(0, 1, referenceWork.getWorkingModeName(), 
								referenceWork.getWorkingModeValue(), referenceWork.getPower(), 
								referenceWork.getBusinessPerformance());
						dataCenter.setWorks(Arrays.asList(new Work[]{miniWork}));
					}
				}
			}
			eascHandler.sendActivityPlans(miniPlans);
		}
	}
	
	private Map<String, Work> buildWorkMap(List<EascActivityPlan> activityPlans, int startTimeSlot, int endTimeSlot) {
		Map<String, Work> workMap = new HashMap<>();
		for (EascActivityPlan easc : activityPlans) {
			String eascName = easc.getEascName();
			for (Activity activity : easc.getActivities()) {
				String activityName = activity.getName();
				for (ActivityDataCenter dataCenter : activity.getDataCenters()) {
					String dataCenterName = dataCenter.getDataCenterName();
					for (Work work : dataCenter.getWorks()) {
						int ts;
						for (ts = work.getStartTimeSlot(); ts < work.getEndTimeSlot(); ts++) {
							if (ts >= startTimeSlot && ts < endTimeSlot) {
								workMap.put(getWorkKey(eascName, activityName, dataCenterName, ts), work);
							} else if (ts >= endTimeSlot) {
								break;
							}
						}
						if (ts >= endTimeSlot) {
							break;
						}
					}
				}
			}
		}
		return workMap;
	}
	
	private String getWorkKey(String easc, String activity, String dataCenter, int timeSlot) {
		return easc + "." + activity + "." + dataCenter + "." + timeSlot;
	}
	
	private List<EascActivityPlan> copyPlanStructure(List<EascActivityPlan> activityPlans) {
		List<EascActivityPlan> planCopies = new ArrayList<>(activityPlans.size());
		for (EascActivityPlan plan : activityPlans) {
			EascActivityPlan planCopy = new EascActivityPlan(plan.getEascName());
			for (Activity activity : plan.getActivities()) {
				Activity activityCopy = new Activity(activity.getName());
				for (ActivityDataCenter dataCenter : activity.getDataCenters()) {
					activityCopy.addDataCenter(new ActivityDataCenter(dataCenter.getDataCenterName()));
				}
				planCopy.addActivity(activityCopy);
			}
			planCopies.add(planCopy);
		}
		return planCopies;
	}
	
    /**
     * Advance simulation clock several timeslots from a given start point.
     *
     * @param timeSlot amount of timeslots to go in the future.
     * @param from starting timeslot.
     */
    private void moveOn(TimeParameters timeSlot, DateTime from) {
        timeSlot.setDateFrom(from);
        timeSlot.setDateNow(from);
        timeSlot.setDateTo(
                from.plusHours(technicalConfiguration.getTimeWindowWidth())
        );
    }

    private RestTemplate newEnergisRestTemplate() {
    	RestTemplate template = new RestTemplate();
    	MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setObjectMapper(JsonUtils.getDc4CitiesObjectMapper());
    	template.setMessageConverters(Arrays.asList(converter));
		return template;
    }
    
    /**
     * Essential method to get the Statistics object.
     *
     * @return Statistics object.
     */
    public Statistics getStatistics() {
        return stats;
    }

    /**
     * Set the consolidator timeout.
     *
     * @param timeOut a limiting timeout to get a consolidator response.
     */
    public void consolidatorTimeout(int timeOut) {
        this.timeOut = timeOut;
    }

    /**
     * Get the consolidator timeout.
     *
     * @return current value for consolidator timeout.
     */
    public int consolidatorTimeout() {
        return timeOut;
    }

    public CentralSystemSimulator consolidatorSplit(boolean b) {
        split = b;
        return this;
    }

    public void replay(String easc, String path) throws IOException {
        List<String> states = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(path))) {
            in.lines().forEach(states::add);
        }
        replays.put(easc, states);
    }

    public void doOptimize(boolean b) {
        optimize = b;
    }

    public void ippBasedHeuristic(boolean b) {
        ippBasedHeuristic = b;
    }
}

