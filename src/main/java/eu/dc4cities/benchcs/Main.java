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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;

import eu.dc4cities.easc.configuration.ConfigReader;
import eu.dc4cities.easc.configuration.ServerConfig;
import eu.dc4cities.easc.energyservice.MultiEASCEnergyService;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.joda.time.DateTime;

import eu.dc4cities.configuration.goal.GoalConfiguration;
import eu.dc4cities.configuration.technical.TechnicalConfiguration;
import eu.dc4cities.controlsystem.model.json.JsonUtils;
import eu.dc4cities.controlsystem.model.unit.Units;
import eu.dc4cities.controlsystem.modules.optionconsolidator.opcp2.OPCP2;
import eu.dc4cities.controlsystem.modules.optionconsolidator.opcp2.reducer.Pass;
import eu.dc4cities.controlsystem.modules.optionconsolidator.opcp2.reducer.RangeFactorReducer;
import eu.dc4cities.controlsystem.modules.optionconsolidator.opcp2.reducer.RangeHorizonReducer;
import eu.dc4cities.controlsystem.modules.optionconsolidator.opcp2.reducer.RangeOffsetReducer;
import eu.dc4cities.controlsystem.modules.processcontroller.ControlLoop;
import eu.dc4cities.easc.EASC;

/**
 * CLI launcher for the simulator. Instantiate several fake handlers to
 * imitate the behavior as objects are communicating with the central system.
 *
 * TODO It should use the network to test scalability and REST API.
 */
public class Main {

    //Number of timeslots, format x:y indicate that uses integers from x up to y.
    public static final String DEFAULT_SLOTS = "1:96";

    public static final int DEFAULT_TIMEOUT = 9;

    /**
     * The idea is to create EASC objects and connect this objects with the
     * central system.
     *  @param sim object to the simulator.
     * @param eg object used to generate EASC based on their name.
     * @param labels the command line string which should have the format name:x where name is the EASC type name
 *              and x is the number of EASCs of type name to initialize.
     * @param serverConfigPath the folder where resides the ServerConfig.yaml needed for shared infrastructure
     */
    private static void injectEASCs(CentralSystemSimulator sim, EASCGenerator3 eg, List<String> labels, Optional<String> serverConfigPath) {


        List<String> eascsPaths = new ArrayList<>();
        for(String label : labels) {

            int sep = label.lastIndexOf(':');
            String eascPath = label.substring(0, sep);

            //Second token is an integer of amount of EASCs to instantiate.
            for (int i = 0; i < Integer.parseInt(label.substring(sep + 1)); i++) {

                eascsPaths.add(eascPath);
            }
        }

        MultiEASCEnergyService multiEnergyService;
		if(serverConfigPath.isPresent()) {
			Units.init();
            Optional<ServerConfig> serverConfig = ConfigReader.readServerConfig(serverConfigPath.get());
            if(serverConfig.isPresent()) {
                multiEnergyService = new MultiEASCEnergyService(false, true);
                multiEnergyService.setServerConfig(serverConfig.get());
            } else {
                multiEnergyService = new MultiEASCEnergyService(false, false);
            }
        } else {
            multiEnergyService = new MultiEASCEnergyService(false, false);
        }


        //configure the EASCs with the shared energy service
        for(String eascPath : eascsPaths) {

            //Build an EASC of type name.
            EASC a = eg.build(eascPath, multiEnergyService);

            //Finally add the EASC to the simulator list.
            sim.getEascs().add(a);
        }
    }

    /**
     * Register at which timeslot the simulator should start:finish.
     *
     * @param sim object ot the simulator.
     * @param label the command line string on format x:y where x is the start time
     *              and y is the finish timeslot.
     */
    private static void injectBounds(CentralSystemSimulator sim, String label) {
        String[] toks = label.split(":");
        int i, j;

        //Test if start timeslot is a parsable int.
        if (toks[0].length() > 0) {
            i = Integer.parseInt(toks[0]);
        }else{
            throw new IllegalArgumentException("Start timeslot is not a parsable int!");
        }

        //Test if finish timeslot is a parsable int.
        if (toks[1].length() > 0) {
            j = Integer.parseInt(toks[1]);
        }else{
            throw new IllegalArgumentException("Finish timeslot is not a parsable int!");
        }

        //Test if the interval respect basic rules.
        if(i > j){
            throw new IllegalArgumentException("Invalid arguments "+label+" start should be less than finish "+i+" > "+j);
        }

        //Set start and finish timeslots.
        sim.startAt(i);
        sim.stopAfter(j);

    }

    /**
     * Used to register a power source file that will be used to inject power availability.
     *
     * @param sim object ot the simulator.
     * @param label the command line string on format dcId:name:path.
     */
    private static void injectSource(CentralSystemSimulator sim, String label) {
        String[] toks = label.split(":", 3);
        if (toks.length != 3) {
            throw new IllegalArgumentException("Invalid format for the power source. Must be 'dc:label:csv_path'");
        }
        String dc = toks[0];
        String name = toks[1];
        String path = toks[2];
        sim.addPowerSource(dc, name, path);
    }
    
    /**
     * Small help to show before finishing the program when given parameters are wrong.
     */
    private static void usage() {
        System.err.println("Usage: CentralSystemSim [--debug] [--interval i] [--save-coms] [--slots from?:to? | --begin timestamp --end timestamp] [--tc technicalConf] [--gc goalConf] [--eascs tpl:nb]+ [--source name?:path]+ output?");
        System.err.println("--tc\n\ttechnicalConf the technical configuration file");
        System.err.println("--gc\n\tthe goal configuration file");
        System.err.println("--eascs\n\tthe number of eascs to spawn for a given template. See folder 'env' for the available ones");
        System.err.println("--source\n\tthe power source to use (csv file). Format is 'dc:label:path'");
        System.err.println("--slots\n\tthe study interval (Inclusive). Default is " + DEFAULT_SLOTS);
        System.err.println("--interval\n\tthe number of slots between two optimization runs. Default is 1");
        System.err.println("--to\n\tthe timeout value in seconds for the consolidator. Default is " + DEFAULT_TIMEOUT);
        System.err.println("--mockWMM\n\tmock the working mode manager for speedup reasons (before --eascs)");
        System.err.println("--debug\n\tdebug level for the system (info by default)");
        System.err.println("--save-coms\n\tstore every communication between the EASCs");
        System.err.println("--begin the timestamp where the simulation should begin (inclusive)");
        System.err.println("--end the timestamp where the simulation should end (exclusive)");
        System.err.println("--hdb the URL of the historical DB to write to, e.g. http://localhost:8080");
        System.err.println("--energis the URL of the Energis API to write alerts to, e.g. http://localhost/energiscloud-gateway/restful/api");
        System.err.println("--energis-api-key the authentication key for the Energis API");
        System.err.println("--escalation-manager enable plan analysis with the escalation manager");
        System.err.println("output\n\tthe output folder (must not exists). Default is Epoch time");
    }

    /**
     * Launch the simulator with parameters given as command line arguments.
     *
     */
    public static void main(String [] args) {
        //Default configuration paths.
        String techConfPath = null;
        String goalConfPath = null;
        List<String> eascs = new ArrayList<>();
        Optional<String> eascServerConfig = Optional.absent();

        //Default slot bounds and timeout.
        String slotBounds = DEFAULT_SLOTS;
        int to = DEFAULT_TIMEOUT;

        //Instantiate an EASC generator object.
        System.out.println(System.getProperty("user.dir"));
        EASCGenerator3 eg = new EASCGenerator3();//System.getProperty("user.dir"));
        boolean debug = false, decisions = false;
        
        //Parse all arguments of the command line.
        try {
            //First needs to create a simulator object.
            CentralSystemSimulator sim = new CentralSystemSimulator();

            int i = 0;
            String output = null;
            for (; i < args.length; i++) {
                switch (args[i]) {
                    case "--begin":
                        sim.setDateFrom(new DateTime(args[++i]));
                        break;
                    case "--end":
                        sim.setDateTo(new DateTime(args[++i]));
                        break;
                    case "--source":
                        injectSource(sim, args[++i]);
                        break;
                    case "--eascs":
                        eascs.add(args[++i]);
                        break;
                    case "--eascs-shared-energy-service":
                        eascServerConfig = Optional.of(args[++i]);
                        break;
                    case "--tc":
                        techConfPath = args[++i];
                        break;
                    case "--gc":
                        goalConfPath = args[++i];
                        break;
                    case "--slots":
                        slotBounds = args[++i];
                        break;
                    case "--save-coms":
                        sim.saveCommunications(true);
                        break;
                    case "--interval":
                        sim.interval(Integer.parseInt(args[++i]));
                        break;
                    case "--reduceFactor":
                        sim.reduceWith(new RangeFactorReducer(Double.parseDouble(args[++i])));
                        break;
                    case "--reduceOffset":
                        sim.reduceWith(new RangeOffsetReducer(Integer.parseInt(args[++i])));
                        break;
                    case "--reduceHorizon":
                        String[] buf = args[++i].split(":");
                        sim.reduceWith(new RangeHorizonReducer(Integer.parseInt(buf[0]), Integer.parseInt(buf[1])));
                        break;
                    case "--no-reducer":
                        sim.reduceWith(new Pass());
                        break;
                    case "--mockWMM":
                        eg.mockWMM(true);
                        break;
                    case "--debug":
                        debug = true;
                        break;
                    case "--decisions":
                        decisions = true;
                        break;
                    case "--timeout":
                        sim.consolidatorTimeout(Integer.parseInt(args[++i]));
                        break;
                    case "--split":
                        sim.consolidatorSplit(Boolean.parseBoolean(args[++i]));
                        break;
                    case "--no-opt":
                        sim.doOptimize(false);
                        break;
                    case "--hdb":
                    	sim.setHistoricalDbUrl(args[++i]);
                        break;
                    case "--energis":
                    	sim.setEnergisUrl(args[++i]);
                    	break;
                    case "--energis-api-key":
                    	sim.setEnergisApiKey(args[++i]);
                    	break;
                    case "--replay":
                        String id = args[i + 1].substring(0, args[i + 1].lastIndexOf(":"));
                        String path = args[i + 1].substring(args[i + 1].lastIndexOf(":") + 1);
                        sim.replay(id, path);
                        i++;
                        break;
                    case "--ippBasedHeuristic":
                        sim.ippBasedHeuristic(Boolean.parseBoolean(args[++i]));
                        break;
                    case "--escalation-manager":
                        sim.setEscalationManagerEnabled(true);
                        break;
                    case "--profitBased":
                        sim.profitBased = Boolean.parseBoolean(args[++i]);
                        break;
                    case "--long":
                        sim.longOutput = true;
                        break;
                    case "-h":
                    case "--help":
                        usage();
                        System.exit(0);
                    default:
                        output = args[i];
                }
            }

            if (eascs.isEmpty() || techConfPath == null || goalConfPath == null || sim.getSources().isEmpty()) {
                usage();
                System.exit(1);
            } else if (sim.getHistoricalDbUrl() != null && sim.isEscalationManagerEnabled() 
            		&& (sim.getEnergisUrl() == null || sim.getEnergisApiKey() == null)) {
            	System.err.println("Energis API URL and API key are required when historical DB and escalation manager "
            			+ "are enabled");
            	System.exit(1);
            }

            injectEASCs(sim, eg, eascs, eascServerConfig);

            //First load the 2 configurations, some parameters move from on to the other thi
            TechnicalConfiguration tc = JsonUtils.load(new FileInputStream(techConfPath), TechnicalConfiguration.class);
            GoalConfiguration gc = JsonUtils.load(new FileInputStream(goalConfPath), GoalConfiguration.class);

            //tc.getProcessConfigList().get(0).setEascRegistryItems(pc.getEascRegistryItems());

            sim.set(tc);
            sim.set(gc);
            injectBounds(sim, slotBounds);

            if (output == null) {
                output = "" + System.currentTimeMillis();
            }
            System.out.println("** slots: "+ slotBounds + "; interval between optimizations: " + sim.interval() + " **");
            System.out.println("** " + sim.getEascs().size() + " EASC(s); " + sim.getSources().size() + " sources; output=" + output + " **");
            long from = System.currentTimeMillis();
            sim.output(output);

            initLogging(debug, decisions, output);
            sim.run();
            long end = System.currentTimeMillis();
            System.out.println("** Simulation done in " + (end - from) + "ms **");
        } catch (IOException e) {
            e.printStackTrace();
            error(e.getMessage());
        }
    }

    private static void initLogging(boolean debug, boolean decisions, String output) {
        Logger.getRootLogger().removeAllAppenders();
        FileAppender a = new FileAppender();
        a.setName("FileLogger");
        a.setFile(output + "/output.log");
        a.setLayout(new PatternLayout("%d %-5p [%c{1}] %m%n"));
        a.setThreshold(debug ? Level.DEBUG : Level.INFO);
        a.setAppend(true);
        a.activateOptions();
        Logger.getRootLogger().addAppender(a);
        if (debug) {
        	Logger.getLogger(ControlLoop.class).setLevel(Level.DEBUG);
            Logger.getLogger(OPCP2.class).setLevel(Level.DEBUG);
        }
        if (decisions) {
            Logger.getLogger(OPCP2.class).setLevel(Level.TRACE);
        }
    }

    private static void error(String msg) {
        System.err.println(msg);
        System.exit(1);
    }
}
