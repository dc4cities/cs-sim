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

package eu.dc4cities.benchcs.mock;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Power;
import javax.measure.unit.Dimension;
import javax.measure.unit.ProductUnit;
import javax.measure.unit.Unit;

import org.apache.commons.lang3.NotImplementedException;
import org.joda.time.DateTime;
import org.jscience.physics.amount.Amount;

import eu.dc4cities.benchcs.CentralSystemSimulator;
import eu.dc4cities.controlsystem.model.TimeParameters;
import eu.dc4cities.controlsystem.model.easc.Activity;
import eu.dc4cities.controlsystem.model.easc.ActivityDataCenter;
import eu.dc4cities.controlsystem.model.easc.ActivityDataCenterMetrics;
import eu.dc4cities.controlsystem.model.easc.ActivityMetrics;
import eu.dc4cities.controlsystem.model.easc.ActivitySpecification;
import eu.dc4cities.controlsystem.model.easc.DataCenterSpecification;
import eu.dc4cities.controlsystem.model.easc.EascActivityPlan;
import eu.dc4cities.controlsystem.model.easc.EascActivitySpecifications;
import eu.dc4cities.controlsystem.model.easc.EascMetrics;
import eu.dc4cities.controlsystem.model.easc.EascOptionPlan;
import eu.dc4cities.controlsystem.model.easc.EascPowerPlan;
import eu.dc4cities.controlsystem.model.easc.ServiceLevelObjective;
import eu.dc4cities.controlsystem.model.easc.Work;
import eu.dc4cities.controlsystem.model.json.JsonUtils;
import eu.dc4cities.controlsystem.model.util.AmountUtils;
import eu.dc4cities.controlsystem.modules.EascHandler;
import eu.dc4cities.easc.EASC;
import eu.dc4cities.easc.workingmode.WorkingMode;

/**
 * Simulate exchange between the central system and EASCs.
 *
 */
public class MockEascHandler implements EascHandler {

    private CentralSystemSimulator sim;
    private Map<String, Amount<Power>> currentWorkingModePowers = new HashMap<>();

    /**
     * Create an EASC handler pointing to the simulator object.
     *
     * @param sim simulator object.
     */
    public MockEascHandler(CentralSystemSimulator sim) {
        this.sim = sim;
    }

    /**
     * Return the metrics of this EASC, this is the business metrics available on the EASC AppConfig-v2.yaml
     * file.
     *
     * @param timeSlot date to start, finish and duration.
     * @return list of metrics.
     */
    @Override
    public List<EascMetrics> getMetrics(TimeParameters timeSlot){
        List<EascMetrics> eascMetricsList = new ArrayList<>();
        // For each easc add the metrics to the list on the given timeSlot.
        for(EASC e : sim.getEascs()){
        	EascMetrics metrics = e.getCtrlCom().getEascService().getMonitoringMetrics(timeSlot);
        	// Replace power consumption values with the expected ones otherwise the EASC returns 0 when the
        	// shared energy service is enabled since the simulator doesn't provide total DC power and activity share
        	// of total power.
        	fixWorkingModePowers(metrics);
        	// Temporary workaround to have correct units in cumulative performance, will need to fix this at the EASC
        	// level.
        	fixUnits(metrics);
            eascMetricsList.add(metrics);
        }
        if(eascMetricsList.isEmpty()){
            throw new IllegalStateException("EascMetricsList is empty, this is not normal, maybe there is no eascs on the system!");
        }
        return eascMetricsList;
    }

    private void fixUnits(EascMetrics metrics) {
    	for (ActivityMetrics activity : metrics.getActivities()) {
    		for (ActivityDataCenterMetrics dataCenter : activity.getDataCenters()) {
    			Amount<?> instantPerf = dataCenter.getInstantBusinessPerformance();
    			if (isQuantityPerTime(instantPerf)) {
    				Unit<?> workUnit = ((ProductUnit<?>) instantPerf.getUnit()).getUnit(0);
    				Amount<?> cumulativePerf = dataCenter.getCumulativeBusinessPerformance();
    				if (!(cumulativePerf.getUnit().equals(workUnit))) {
    					if (cumulativePerf.isExact()) {
    						cumulativePerf = Amount.valueOf(cumulativePerf.getExactValue(), workUnit);
    					} else {
    						cumulativePerf = Amount.valueOf(cumulativePerf.getEstimatedValue(), workUnit);
    					}
    					dataCenter.setCumulativeBusinessPerformance(cumulativePerf);
    				}
    			}
    		}
    	}
    }
    
    private void fixWorkingModePowers(EascMetrics metrics) {
    	if (currentWorkingModePowers.size() == 0) {
    		return;
    	}
    	String eascName = metrics.getEascName();
    	for (ActivityMetrics activity : metrics.getActivities()) {
    		String activityName = activity.getName();
    		for (ActivityDataCenterMetrics dataCenter : activity.getDataCenters()) {
    			String dataCenterName = dataCenter.getDataCenterName();
    			String workingModeName = dataCenter.getWorkingModeName();
    			String key = getWorkingModeKey(eascName, activityName, dataCenterName, workingModeName);
    			Amount<Power> expectedPower = currentWorkingModePowers.get(key);
    			if (expectedPower != null) {
    				dataCenter.setPower(expectedPower);
    			}
    		}
    	}
    }
    
    /**
     * Return the metrics of this EASC, this is the bussiness metrics available on the EASC AppConfig-v2.yaml
     * file.
     *
     * @param timeSlot date to start, finish and duration.
     * @return list of metrics.
     */
    @Override
    public List<EascActivitySpecifications> getActivitySpecifications(TimeParameters timeSlot) {
        List<EascActivitySpecifications> eascActivitySpecificationsList = new ArrayList<>();
        for (EASC e : sim.getEascs()) {
        	EascActivitySpecifications specs = e.getCtrlCom().getEascService().getActivitySpecifications(timeSlot);
        	for (ActivitySpecification a : specs.getActivitySpecifications()) {
        		String activityName = a.getActivityName();
                if (a.getServiceLevelObjectives().isEmpty()) {
                    throw new IllegalStateException(specs.getEascName() + ":" + activityName + 
                    		" did not specify any SLO for the following timerange:\n" + timeSlot);
                }
                DateTime dateNow = timeSlot.getDateNow();
                for (ServiceLevelObjective slo : a.getServiceLevelObjectives()) {
                	DateTime sloStart = slo.getDateFrom();
                	if (dateNow.isEqual(sloStart)) {
                		// We are at the beginning of a new SLO, reset the cumulative performance
                		//System.out.println(" We are at the beginning of a new SLO, reset the cumulative performance");

                    	for (DataCenterSpecification dc : a.getDataCenters())  {
                    		String dataCenterName = dc.getDataCenterName();
                    		e.getMonitor().initWorkDone(activityName + "." + dataCenterName);
                    	}
                    	break;
                	} else if (dateNow.isBefore(sloStart)) {
                		break;
                	}
                }
            }
            eascActivitySpecificationsList.add(specs);
        }
        if (eascActivitySpecificationsList.isEmpty()) {
            throw new IllegalStateException("EascActivitySpecificationsList is empty, this is not normal, maybe there is no eascs on the system!");
        }
        return eascActivitySpecificationsList;
    }

    /**
     * Old method that return several power plans for this EASC, now with inversion of control the EASC return
     * the ActivitySpecification and Metrics. Use methods getActivitySpecifications and getMetrics instead.
     *
     * @param eascPowerPlans list of power plans for this EASC.
     * @return list of option plans to execute the EASC.
     *
     */
    @Deprecated
    public List<EascOptionPlan> collectOptionPlans(List<EascPowerPlan> eascPowerPlans){
        throw new NotImplementedException("EascOptionPlan became deprecated please use the getActivitySpecifications or getMetrics!");
    }

    @Override
    public void sendActivityPlans(List<EascActivityPlan> eascActivityPlans) {
        String root = sim.output() + "/activityPlans/" + sim.now();
        if (sim.saveCommunications()) {
            if (!new File(root).mkdirs()) {
                System.err.println("Unable to create " + root);
                System.exit(1);
            }
        }
        for (EascActivityPlan plan : eascActivityPlans) {
            EASC easc = sim.getEascByAppName(plan.getEascName());
            if (sim.saveCommunications()) {
                try {
                    JsonUtils.save(plan, new FileOutputStream(root + "/" + plan.getEascName() + ".json"));
                } catch (Exception e) {
                    System.err.println("Unable to store activity plans: " + e.getMessage());
                    System.exit(1);
                }
            }
            easc.getActivityPlanExecutor().executeActivityPlan(plan, true);
            // Simulate work done for the next getMetrics call
            updateCumulativePerformance(easc);
        }
        // Save the expected WM power consumption for the next getMetrics call
        // (otherwise the EASC returns power = 0 when the shared energy service is enabled due to invoking methods
        // that should return the total DC power consumption and power share for each activity; however this data is
        // is not available in the simulation)
        rememberWorkingModePowers(eascActivityPlans);
    }
    
    private void updateCumulativePerformance(EASC easc) {
    	EascActivityPlan plan = easc.getActivityPlanExecutor().getActivityPlan();
    	for (Activity activity : plan.getActivities()) {
        	String activityName = activity.getName();
            for (ActivityDataCenter dataCenter : activity.getDataCenters()) {
            	String dataCenterName = dataCenter.getDataCenterName();
            	String workingModeName = dataCenter.getWorks().get(0).getWorkingModeName();
            	Amount<?> bizPerf = getWorkingModePerf(easc, activityName, dataCenterName, workingModeName);
            	// Update the cumulative performance only if the instant performance is something that accumulates
            	// over time, such as page/s or req/s, and not something like CPU usage. 
            	if (isQuantityPerTime(bizPerf)) {
            		Amount<?> workDoneInTimeSlot = 
            				AmountUtils.calcCumulativePerformance(bizPerf, plan.getTimeSlotDuration());
                	easc.getMonitor().addActivityCumulativeBusinessItems(activityName, dataCenterName, 
                			(Amount<Dimensionless>) workDoneInTimeSlot);
            	}
            }
        }
    }
    
    private boolean isQuantityPerTime(Amount<?> amount) {
    	Unit<?> unit = amount.getUnit();
    	if (unit instanceof ProductUnit) {
    		return ((ProductUnit<?>) unit).getUnit(1).getDimension().equals(Dimension.TIME);
    	}
    	return false;
    }
    
    private Amount<?> getWorkingModePerf(EASC easc, String activityName, String dataCenterName, 
    		String workingModeName) {
    	for (WorkingMode workingMode : easc.getWorkingModeManager().getWorkingModes(activityName, dataCenterName)) {
    		if (workingMode.getName().equals(workingModeName)) {
    			return workingMode.getPerformanceLevels().get(0).getBusinessPerformance();
    		}
    	}
    	return null;
    }
    
    private void rememberWorkingModePowers(List<EascActivityPlan> eascActivityPlans) {
    	currentWorkingModePowers.clear();
    	for (EascActivityPlan easc : eascActivityPlans) {
			String eascName = easc.getEascName();
			for (Activity activity : easc.getActivities()) {
				String activityName = activity.getName();
				for (ActivityDataCenter dataCenter : activity.getDataCenters()) {
					String dataCenterName = dataCenter.getDataCenterName();
					Work work = dataCenter.getWorks().get(0);
					String workingModeName = work.getWorkingModeName();
					Amount<Power> workingModePower = work.getPower();
					String key = getWorkingModeKey(eascName, activityName, dataCenterName, workingModeName);
					currentWorkingModePowers.put(key, workingModePower);
				}
			}
		}
    }
	
	private String getWorkingModeKey(String easc, String activity, String dataCenter, String workingMode) {
		return easc + "." + activity + "." + dataCenter + "." + workingMode;
	}
    
}
