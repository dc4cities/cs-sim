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

import static eu.dc4cities.easc.resource.Units.PCPU_USAGE;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.measure.unit.UnitFormat;

import eu.dc4cities.benchcs.mock.MockWMManager;
import eu.dc4cities.easc.EASC;
import eu.dc4cities.easc.activity.Activity;
import eu.dc4cities.easc.activity.DataCenterWorkingModes;
import eu.dc4cities.easc.configuration.Configuration;
import eu.dc4cities.easc.monitoring.MockMonitor;
import eu.dc4cities.easc.workingmode.WorkingMode;
import eu.dc4cities.easc.workingmode.WorkingModeManager;

/**
 * This allows to generate EASC from predefined templates available in a folder.
 */
public class EASCGenerator2 {

    private String root;
    private Map<String, Configuration> templates;
    private int from;
    private boolean mockWMM = false;

    /**
     * Make a new generator
     *
     * @param root the folder containing the EASC templates.
     */
    public EASCGenerator2(String root) {
        this.root = root;
        from = 1;
        templates = new HashMap<>();
    }

    /**
     * Make a new generator.
     * Templates are loaded from folder {@code template}
     */
    public EASCGenerator2() {
        this("env");
    }

    /**
     * Build an EASC.
     *
     * @param tpl the template to rely on
     * @return the resulting EASC
     */
    public EASC build(String tpl) {

        String workingDir = null;
        File f = null;
        EASC easc = null;

        //Declare amount needed to parse yaml files.
        UnitFormat.getInstance().label(org.jscience.economics.money.Currency.EUR, "EUR");
        UnitFormat.getInstance().label(PCPU_USAGE, "pCPU_usage");

//        Configuration config = null;
//        config = templates.get(tpl);
//        f = new File(root + "/env/" + tpl + "/easc");
//        if (config == null) {
//            config = ConfigReader.readConfiguration(f.getPath()).get();
//            templates.put(tpl, config);
//        }
        workingDir = root + "/env/" + tpl + "/easc";

//        if(tpl.equals("cfapp")) {
//            //Read the PaaS exclusive configuraiton file.
//            ESConfig ESConfig = ESConfigReader.readESConfig(workingDir).get();
//
//            //Create special energy service for CloudFoundry.
//            CFEnergyService ES = new CFEnergyService(ESConfig);
//
//            easc = new EASC(workingDir, config, ES, new MockWorkDone(), new MockMonitor());
//
//            easc.init(true);
//
//            //registering the easc with the Energy Service
//            ES.addWmm(easc.getWorkingModeManager());
//
//            easc.start();
//        }else {
            //Generate the EASC, use the working dir
//            easc = new EASC(workingDir,       config,
//                    new DefaultEnergyService(),
//                    new MockWorkDone(
//                            (Amount<Dimensionless>)
//                                    Amount.valueOf(15, NonSI.G).
//                                            times(Amount.valueOf(15, MINUTE).
//                                                    divide(Amount.valueOf(15, SI.SECOND)))
//                    ),
//                    new MockMonitor()
//            );

        easc = new EASC(workingDir, new MockMonitor());


//        }

        //Needs to execute several activities.
        for (Activity activity : easc.getAppConfig().getActivities()) {
            //Each activities include determining the working mode on several datacenter.
            for (DataCenterWorkingModes dataCenter : activity.getDataCenters()) {
                //Each datacenter has a list of work to do.
                WorkingModeManager wmm = easc.getWorkingModeManager();
                //Bypass the shell command to execute for "true". Hope this is windows compliant
                if (mockWMM) {
                    easc.setWorkingModeManager(
                            new MockWMManager(easc.getEnergyService(), easc.getAppConfig())
                    );
                } else {
                    easc.setDefaultWorkingModeManager();
                    for (WorkingMode wm : easc.getWorkingModeManager().getWorkingModes(activity.getName(), dataCenter.getDataCenterName())) {
                        wm.getActuator().setSystemCommand("true");
                    }
                }
            }
        }

        //Initialize the EASC true here is to use default working modes.
        easc.init(true);

        from++;
        return easc;
    }

    /**
     * Duplicate a configuration.
     *
     * @param old  the basic configuration
     * @param name the new easc name
     * @param workDoneClassName name of the workDoneClass
     * @return the resulting configuration
     */
    private static Configuration rename(Configuration old, String name, String workDoneClassName) {
        /*Configuration c = new Configuration(name,
                old.getEnergisPort(),
                old.getEnergisURL(),
                old.getEnergisPort(),
                old.getEnergisExecuteURL(),
                old.getEnergisPort(),
                workDoneClassName);*/
        Configuration c = new Configuration(name,
                old.getPort(),
                old.getEnergisURL(),
                old.getEnergisPort(),
                old.getEnergisExecuteURL(),
                old.getEnergisExecutePort());
        c.setSiteCode(old.getSiteCode());
        return c;
    }

    public void mockWMM(boolean b) {
        mockWMM = b;
    }
}

