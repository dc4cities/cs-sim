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

import eu.dc4cities.benchcs.CentralSystemSimulator;
import eu.dc4cities.controlsystem.model.TimeSlotBasedEntity;
import eu.dc4cities.controlsystem.model.erds.DataCenterForecast;
import eu.dc4cities.controlsystem.model.erds.ErdsForecast;
import eu.dc4cities.controlsystem.modules.ErdsHandler;
import eu.dc4cities.dc4es.controller.replay.ReplaySimControllerImpl;
import eu.dc4cities.dc4es.model.ForecastRequest;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.List;

public class MockErdsHandler implements ErdsHandler {

    private CentralSystemSimulator sim;

    private static final Logger log = LoggerFactory.getLogger(MockErdsHandler.class);

    private Map<String, ReplaySimControllerImpl> sources;

    private String delimiter;

    private String datePattern;

    private List<ErdsForecast> forecasts;

    public static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";//"\"dd-MMM-yy hh.mm.ss aa\"";

    public static final String SEP = ";";

    public static final double FUZZYNESS = 0;

    public MockErdsHandler(CentralSystemSimulator sim, String datePattern, String delimiter, double fuzzynesFactor) {
        this.sim = sim;
        this.datePattern = datePattern;
        this.delimiter = delimiter;
        sources = new HashMap<>();
        for (String id : sim.getSources()) {
            String path = sim.getSourceFile(id);
            ReplaySimControllerImpl from = new ReplaySimControllerImpl(path, datePattern, delimiter, fuzzynesFactor);
            sources.put(id, from);
        }
    }

    public MockErdsHandler(CentralSystemSimulator sim) {
        this(sim, DATE_PATTERN, SEP, FUZZYNESS);
    }


    public DateTime getFrom() throws IOException {
        DateTime min = null;
        for (String path : sim.getSources()) {
            DateTime t = getFrom(sim.getSourceFile(path));
            if (min == null || t.isAfter(min)) {
                min = t;
            }
        }
        return min;
    }

    /**
     * This mock the DC4ES module creating a forecast, read from csv file on the simulator. This
     * files has the format
     *
     * @param range timeslot range to work with.
     * @return non null list of forecast for each datacenter.
     */
    public List<DataCenterForecast> getEnergyForecasts(TimeSlotBasedEntity range) {
        //Increase the window size
        List<DataCenterForecast> forecasts = new ArrayList<>();
        ForecastRequest req = new ForecastRequest(range);

        for (Map.Entry<String, ReplaySimControllerImpl> e : sources.entrySet()) {
            String[] toks = e.getKey().split(":");
            String dc = toks[0];
            String id = toks[1];
            ReplaySimControllerImpl replay = e.getValue();
            req.setErdsName(id);
            ErdsForecast fc = replay.getForecast(req);
            getDc(forecasts, dc).getErdsForecasts().add(fc);
        }

        try {
            sim.getStatistics().storeForecasts(sim.now(), sim.interval(), forecasts);
        } catch (IOException e) {
            System.err.println("Unable to store forecasts: " + e.getMessage());
            System.exit(1);
        }

        if(forecasts.isEmpty()){
            throw new IllegalStateException("Returning a null list of forecasts check the ErdsHandler initialization!");
        }

        return forecasts;
    }

    private DataCenterForecast getDc(List<DataCenterForecast> forecasts, String dc) {
        for (DataCenterForecast fc : forecasts) {
            if (fc.getDataCenterName().equals(dc)) {
                return fc;
            }
        }
        DataCenterForecast fc = new DataCenterForecast(dc);
        fc.setErdsForecasts(new ArrayList<>());
        forecasts.add(fc);
        return fc;
    }


    /**
     * Please use getEnergyForecasts instead, this is deprecated.
     *
     * Function used on phase 1 to estimate the forecasts of several datacentres.
     *
     * @param range a time range.
     * @return forecasts list within the time range.
     * @deprecated use {@link #getEnergyForecasts(TimeSlotBasedEntity)} to support federation
     */
    @Override
    @Deprecated
    public List<ErdsForecast> collectEnergyForecasts(TimeSlotBasedEntity range) {
        ForecastRequest req = new ForecastRequest(range);
        forecasts = new ArrayList<>();
        for (Map.Entry<String, ReplaySimControllerImpl> e : sources.entrySet()) {
            String id = e.getKey();
            ReplaySimControllerImpl replay = e.getValue();
            req.setErdsName(id);
            ErdsForecast fc = replay.getForecast(req);
            forecasts.add(fc);
        }

        //For historic purpose
        DataCenterForecast dc = new DataCenterForecast("dc");
        dc.setErdsForecasts(forecasts);
        try {
            sim.getStatistics().storeForecasts(sim.now(), sim.interval(), Collections.singletonList(dc));
        } catch (IOException e) {
            System.err.println("Unable to store forecasts: " + e.getMessage());
            System.exit(1);
        }
        return forecasts;
    }

    private DateTime getFrom(String file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));

        String line = in.readLine();
        Scanner scanner = new Scanner(line);
        scanner.useDelimiter(delimiter);
        return DateTime.parse(scanner.next(), DateTimeFormat.forPattern(datePattern));
    }
}
