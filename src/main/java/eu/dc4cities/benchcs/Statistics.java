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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.measure.quantity.Duration;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.joda.time.DateTime;
import org.jscience.physics.amount.Amount;

import eu.dc4cities.controlsystem.model.TimeParameters;
import eu.dc4cities.controlsystem.model.datacenter.AlertType;
import eu.dc4cities.controlsystem.model.datacenter.DataCenterStatus;
import eu.dc4cities.controlsystem.model.datacenter.StatusAlert;
import eu.dc4cities.controlsystem.model.easc.Activity;
import eu.dc4cities.controlsystem.model.easc.ActivityDataCenter;
import eu.dc4cities.controlsystem.model.easc.EascActivityPlan;
import eu.dc4cities.controlsystem.model.easc.Work;
import eu.dc4cities.controlsystem.model.erds.DataCenterForecast;
import eu.dc4cities.controlsystem.model.erds.ErdsForecast;
import eu.dc4cities.controlsystem.model.erds.TimeSlotErdsForecast;
import eu.dc4cities.controlsystem.model.unit.Units;
import eu.dc4cities.controlsystem.modules.optionconsolidator.opcp2.Score;
import eu.dc4cities.controlsystem.modules.optionconsolidator.opcp2.State;
import eu.dc4cities.controlsystem.modules.processcontroller.HdbAlertState;

/**
 * Generate statistics of the EASC execution this is important to state
 * the health of builds.
 */
public class Statistics {

	private static final int MINUTES_PER_DAY = 24 * 60;
	
    private File root;

    private static final String SEP = ";";

    // Variables to plot header only when necessary.
    private boolean optionPlanHeader, eascHeader, forecastHeader, alertsHeader, objHeader;

    /**
     * Initialize the header boolean variables to false.
     *
     * @param root path where to store csv files.
     */
    public Statistics(File root) {
        this.root = root;
        eascHeader = false;
        forecastHeader = false;
        objHeader = false;
    }

    /**
     *
     * Store the EASC state at each timeslot, log information into eascs.csv file to use it to produce
     * output graph to report the health. This function is mandatory to continous integration reports
     * automatically generated on jenkins.
     *
     * @param now current time slot integer id.
     * @param length how many time slots to write starting from the current one
     * @param time current time.
     * @param plans power plan from the option consolidator.
     *
     * @throws IOException writes results to files, hence IO exception may occur due to disk space or SO constraints.
     */
    public void storePlan(int now, int length, TimeParameters time, List<EascActivityPlan> plans) throws IOException {
        try (BufferedWriter out = new BufferedWriter(new FileWriter(root.getPath() + "/eascs.csv", true))) {
            if (!eascHeader) {
                eascHeader = true;
                out.append(
                		"easc" + SEP
                        + "activity" + SEP
                        + "datacenter" + SEP
                        + "slot" + SEP
                        + "mode" + SEP
                        + "performance" + SEP
                        + "watts" + SEP
                        + "time" + "\n"
                );
            }
            StringBuilder[] timeSlotLines = new StringBuilder[length];
            for (int i = 0; i < length; i++) {
            	timeSlotLines[i] = new StringBuilder();
            }
            for (EascActivityPlan plan : plans) {
                for (Activity a : plan.getActivities()) {
                    for (ActivityDataCenter dc : a.getDataCenters()) {
                    	boolean lengthReached = false;
                        for (Work w : dc.getWorks()) {
                        	for (int ts = w.getStartTimeSlot(); ts < w.getEndTimeSlot(); ts++) {
                        		if (ts < length) {


                        			int absoluteTs = now + ts;
                        			DateTime tsDate = getTimeSlotDate(ts, plan.getDateFrom(), 
                        					plan.getTimeSlotDuration());
                        			timeSlotLines[ts].append(
                        					plan.getEascName() + SEP 
                        					+ a.getName() + SEP 
                        					+ dc.getDataCenterName() + SEP 
                        					+ absoluteTs + SEP 
                        					+ w.getWorkingModeName() + SEP 
                        					+ w.getBusinessPerformance().getEstimatedValue() + SEP 
                        					+ w.getPower().doubleValue(SI.WATT) + SEP
                                            //use quotes here to avoid R parsing problems
                                            + "\""+ tsDate + "\"" + "\n");
                        		}
                        		if (ts >= length - 1) {
                        			lengthReached = true;
                        			break;
                        		}
                        	}
                        	if (lengthReached) {
                        		break;
                        	}
                        }
                    }
                }
            }
            for (StringBuilder timeSlot : timeSlotLines) {
            	out.append(timeSlot.toString());
            }
        }
    }

    private DateTime getTimeSlotDate(int timeSlot, DateTime dateFrom, Amount<Duration> duration) {
    	return getTimeSlotDate(timeSlot, dateFrom, (int) duration.longValue(NonSI.MINUTE));
    }
    
    private DateTime getTimeSlotDate(int timeSlot, DateTime dateFrom, int duration) {
    	return dateFrom.plusMinutes(duration * timeSlot);
    }
    
    /**
     * Store information of the forecasts this is called when the mockErdsHandler is generates energy forecasts.
     *
     * @param now id of current timeslot.
     * @param length how many time slots to write starting from the current one
     * @param plans forecast list produced by MockErdsHandler.
     * @throws IOException handle files so may throw exceptions related to disk full and other errors.
     *
     * @throws IOException writes results to files, hence IO exception may occur due to disk space or SO constraints.
     */
    public void storeForecasts(int now, int length, List<DataCenterForecast> plans) throws IOException {
    	// We store the prediction for "length" time slots starting from the current ones.
    	// As we replay a source with a known fuzziness factor
    	// it seems not useful to store forecasts over the time as it might just reveal the factor.
    	// Assume all forecasts (all DCs and ERDSes) are aligned (same start date, same length) and always start from
    	// the beginning of the day.
    	if (plans.size() == 0 || plans.get(0).getErdsForecasts().size() == 0) {
    		throw new IllegalArgumentException("Forecast list is empty");
    	}
    	ErdsForecast firstForecast = plans.get(0).getErdsForecasts().get(0);
    	DateTime dateFrom = firstForecast.getDateFrom();
    	int timeSlotDuration = (int) firstForecast.getTimeSlotDuration().longValue(NonSI.MINUTE);
    	int timeSlotsPerDay = MINUTES_PER_DAY / timeSlotDuration;
    	int startTimeSlot = now % timeSlotsPerDay;
        try (BufferedWriter out = new BufferedWriter(new FileWriter(root.getPath() + "/forecasts.csv", true))) {
            if (!forecastHeader) {
                forecastHeader = true;
                out.append("slot" + SEP
                         + "dc" + SEP
                         + "source" + SEP
                         + "at" + SEP
                         + "capacity" + SEP
                         + "renPct" + SEP
                         + "carbon" + SEP
                         + "price" + SEP
                         + "factor" + SEP
                         + "time" + "\n");
            }
            for (int tsOffset = 0; tsOffset < length; tsOffset++) {
            	int absoluteTs = now + tsOffset;
            	int dayTs = startTimeSlot + tsOffset;
            	DateTime tsDate = getTimeSlotDate(dayTs, dateFrom, timeSlotDuration);
            	for (DataCenterForecast dcfc : plans) {
                    for (ErdsForecast plan : dcfc.getErdsForecasts()) {
                    	TimeSlotErdsForecast forecast = plan.getTimeSlotForecasts().get(dayTs);
                        long pow = forecast.getPower().longValue(SI.WATT);
                        long ren = forecast.getRenewablePercentage().getExactValue();
                        long carbon = forecast.getCo2Factor().getExactValue();
                        double price = forecast.getConsumptionPrice() == null ? 
                        		-1 : forecast.getConsumptionPrice().doubleValue(Units.EUR_PER_KWH);
                        double primeEnergyFactor = forecast.getPrimaryEnergyFactor();
                        out.append(
                        		Integer.toString(absoluteTs) + SEP
                        		+ dcfc.getDataCenterName() + SEP
                        		+ plan.getErdsName() + SEP
                        		+ Integer.toString(dayTs) + SEP
                        		+ Long.toString(pow) + SEP
                        		+ Long.toString(ren) + SEP
                        		+ Long.toString(carbon) + SEP
                        		+ Double.toString(price) + SEP
                        		+ Double.toString(primeEnergyFactor) + SEP
                        		// use quotes here to avoid R parsing problems
                        		+ "\"" + tsDate + "\"" + "\n"
                        );
                    }
                }
            }
        }
    }

    public void storeReplay(eu.dc4cities.controlsystem.modules.optionconsolidator.opcp2.Statistics stats) throws IOException {
        for (EascActivityPlan p : stats.retainedPlan()) {
            for (Activity a : p.getActivities()) {
                for (ActivityDataCenter d : a.getDataCenters()) {
                    try (BufferedWriter out = new BufferedWriter(new FileWriter(root.getPath() + "/replay-" + a.getName() + ".csv"))) {
                        for (Work w : d.getWorks()) {
                            out.append(d.getDataCenterName()).append(":").append(w.getWorkingModeName()+":0").append("\n");
                        }
                    }
                }
            }


        }

    }
    public void storeLong(eu.dc4cities.controlsystem.modules.optionconsolidator.opcp2.Statistics stats, List<DataCenterForecast> plans) throws IOException {
        /*
            date;easc;kind(watt||perf);value
         */
        Map<DateTime, Double> usage = new HashMap<>();
        try (BufferedWriter out = new BufferedWriter(new FileWriter(root.getPath() + "/eascs-full.csv"))) {
            out.append("date;easc;metric;value\n");
            for (EascActivityPlan p : stats.retainedPlan()) {
                for (Activity a : p.getActivities()) {
                    for (ActivityDataCenter dc : a.getDataCenters()) {
                        for (Work w : dc.getWorks()) {
                            DateTime t = p.getDateFrom().plusMinutes(15*w.getStartTimeSlot());
                            double watts = w.getPower().doubleValue(SI.WATT);
                            double perf = w.getBusinessPerformance().getEstimatedValue();
                            String fmt = t.toString("d/MM/YYYY H:m:s");
                            out.append(fmt+";"+a.getName()+";watts;"+watts+"\n");
                            out.append(fmt+";"+a.getName()+";perf;"+perf+"\n");
                            double base = 0;
                            if (usage.containsKey(t)) {
                                base = usage.get(t);
                            }
                            usage.put(t, base + watts);
                        }
                    }
                }
            }
            for (Map.Entry<DateTime, Double> e : usage.entrySet()) {
                out.append(e.getKey().toString("d/MM/YYYY H:m:s") + ";_total;watts;" + e.getValue()+"\n");
            }
        }

        /*
           date;source;usage;pct;cost
         */
        try (BufferedWriter out = new BufferedWriter(new FileWriter(root.getPath() + "/power-full.csv"))){
            out.append("date;usage");
            for (DataCenterForecast dcfc : plans) {
                for (ErdsForecast plan : dcfc.getErdsForecasts()) {
                    out.append(";" + plan.getErdsName()+"_peak;" + plan.getErdsName()+"_pct");
                }
            }
            out.append("\n");
            Map<DateTime, List<Double>> power = new HashMap<>();
            Map<DateTime, List<Long>> ren = new HashMap<>();
            for (DataCenterForecast dcfc : plans) {
                for (ErdsForecast plan : dcfc.getErdsForecasts()) {
                    List<TimeSlotErdsForecast> forecasts = plan.getTimeSlotForecasts();
                    for (TimeSlotErdsForecast ts: forecasts) {
                        DateTime t = plan.getDateFrom().plusMinutes(15 * ts.getTimeSlot());
                        double p = ts.getPower().doubleValue(SI.WATT);
                        long r = ts.getRenewablePercentage().getExactValue();
                        String erds = plan.getErdsName();
                        List<Double> pp = power.get(t);
                        if (!power.containsKey(t)) {
                            power.put(t, new ArrayList<>());
                            ren.put(t, new ArrayList<>());
                        }
                        power.get(t).add(p);
                        ren.get(t).add(r);
                    }
                }
            }
            for (DateTime t : usage.keySet()) {
                out.append(t.toString("d/MM/YYYY H:m:s")+";"+usage.get(t));
                List<Double> pp = power.get(t);
                for (int i = 0; i < pp.size(); i++) {
                    out.append(";" + power.get(t).get(i).toString());
                    out.append(";" + ren.get(t).get(i).toString());
                }
                out.append("\n");
            }
        }
    }

    /**
     * Store alerts generated by the escalation manager.
     * 
     * @param now current time slot number
     * @param length how many time slots to write starting from the current one
     * @param statuses the list of statuses produced by the escalation manager
     * @throws IOException 
     */
    public void storeAlerts(int now, int length, List<DataCenterStatus> statuses) throws IOException {
    	// Assume all statuses (all DCs) are aligned (same start date, same length) and always start from the
    	// beginning of the day.
    	DataCenterStatus firstStatus = statuses.get(0);
    	DateTime dateFrom = firstStatus.getDateFrom();
    	int timeSlotDuration = (int) firstStatus.getTimeSlotDuration().longValue(NonSI.MINUTE);
    	int timeSlotsPerDay = MINUTES_PER_DAY / timeSlotDuration;
    	int firstStatusSlot = now % timeSlotsPerDay;
    	int lastStatusSlot = firstStatusSlot + length;
    	try (BufferedWriter out = new BufferedWriter(new FileWriter(root.getPath() + "/alerts.csv", true))) {
            if (!alertsHeader) {
                alertsHeader = true;
                out.append(
                		"slot" + SEP
                		+ "time" + SEP
                        + "datacenter" + SEP
                        + "alert\n"
                );
            }
            int[][] alertStates = new int[statuses.size()][length];
    		int dcIndex = 0;
    		for (DataCenterStatus status : statuses) {
    			Arrays.fill(alertStates[dcIndex], HdbAlertState.NONE);
    			for (StatusAlert alert : status.getAlerts()) {
        			if (alert.getType().equals(AlertType.RENPCT)) {
        				int startTs = alert.getStartTimeSlot();
        				int endTs = alert.getEndTimeSlot();
        				if (startTs >= lastStatusSlot) {
        					break;
        				} else if (startTs >= firstStatusSlot || firstStatusSlot < endTs) {
        					int fromIndex = Math.max(startTs, firstStatusSlot) - firstStatusSlot;
        					int toIndex = Math.min(endTs, lastStatusSlot) - firstStatusSlot;
        					int alertState = HdbAlertState.from(alert.getSeverity());
    						Arrays.fill(alertStates[dcIndex], fromIndex, toIndex, alertState);
    						if (endTs >= lastStatusSlot) {
    							break;
    						}
        				}
        			}
        		}
    			dcIndex++;
    		}
    		for (int tsOffset = 0; tsOffset < length; tsOffset++) {
            	int absoluteTs = now + tsOffset;
            	int relativeTs = firstStatusSlot + tsOffset;
            	DateTime tsDate = getTimeSlotDate(relativeTs, dateFrom, timeSlotDuration);
            	dcIndex = 0;
            	for (DataCenterStatus status : statuses) {
            		String dataCenterName = status.getDataCenterName();
            		out.append(
                    		absoluteTs + SEP
                    		+ "\"" + tsDate + "\"" + SEP
                    		+ dataCenterName + SEP
                    		+ alertStates[dcIndex][tsOffset] + "\n"
                    );
            		dcIndex++;
            	}
    		}
        }
    }
    
    /**
     * Store optionConsolidator statistics.
     *
     * @param t the id of current timeslot.
     * @param stats special stats from the optionConsolidator object.
     *
     * @throws IOException writes results to files, hence IO exception may occur due to disk space or SO constraints.
     */
    public void store(int t, eu.dc4cities.controlsystem.modules.optionconsolidator.opcp2.Statistics stats) throws IOException {

        try (BufferedWriter out = new BufferedWriter(new FileWriter(root.getPath() + "/optionConsolidator.csv", true))) {

            if (!optionPlanHeader) {
                optionPlanHeader = true;
                out.append("slot" + SEP + "duration" + SEP + "profit\n");
            }
            for (Score s : stats.scores()) {
                out.append(Integer.toString(t)).append(SEP).append(Long.toString(s.timestamp)).append(SEP).append(s.value.toString()).append(SEP).append(stats.status().equals("to") ? "" + 0 : "" + 1).append("\n");
            }
        }

        try (BufferedWriter out = new BufferedWriter(new FileWriter(root.getPath() + "/objectives.csv", true))) {
            if (!objHeader) {
                objHeader = true;
                out.append("slot" + SEP + "key" + SEP + "value\n");
            }
            for (Map.Entry<String, Double> e : stats.objectives().entrySet()) {
                out.append(Integer.toString(t)).append(SEP).append(e.getKey()).append(SEP).append(e.getValue().toString()).append("\n");
            }
        }

        for (Map.Entry<String, List<State>> e : stats.states().entrySet()) {
        	String path = root.getPath() + "/perfs-" + normalizeKey(e.getKey()) + "-" + t + ".csv";
            try (BufferedWriter out = new BufferedWriter(new FileWriter(path, true))) {
                out.append("slot" + SEP + "state" + SEP + "perf\n");
                for (int i = 0; i < e.getValue().size(); i++) {
                    State st = e.getValue().get(i);
                    out.append(Integer.toString(i)).append(SEP).append(st.name()).append(SEP).append(Integer.toString(st.perf())).append("\n");
                }
            }
        }
    }

    public String normalizeKey(String key) {
    	return key.replace(':', '-');
    }
    
}
