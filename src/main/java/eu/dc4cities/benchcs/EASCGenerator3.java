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
import static javax.measure.unit.Unit.ONE;

import javax.measure.unit.UnitFormat;

import eu.dc4cities.benchcs.mock.MockWMManager;
import eu.dc4cities.benchcs.mock.SimMonitor;
import eu.dc4cities.controlsystem.model.unit.Units;
import eu.dc4cities.easc.EASC;
import eu.dc4cities.easc.activity.Activity;
import eu.dc4cities.easc.activity.DataCenterWorkingModes;
import eu.dc4cities.easc.configuration.ConfigReader;
import eu.dc4cities.easc.energyservice.EnergyService;
import eu.dc4cities.easc.workingmode.WorkingMode;
import eu.dc4cities.easc.workingmode.WorkingModeActuator;

/**
 * This allows to generate EASC from predefined templates available in a folder.
 */
public class EASCGenerator3 {
	
    private boolean mockWMM = false;
    

    public EASCGenerator3() {
    	
    	
    }

    /**
     * Build an EASC.
     *
     * @param workingDir the template folder to rely on
     * @return the resulting EASC
     */
    public EASC build(String workingDir, EnergyService es) {

        EASC easc = null;

        //Declare amount needed to parse yaml files.
        Units.init();
        UnitFormat.getInstance().label(ONE.alternate("Exam"), "Exam");
    	UnitFormat.getInstance().label(ONE.alternate("Page"), "Page");
    	UnitFormat.getInstance().label(ONE.alternate("Req"), "Req");
        UnitFormat.getInstance().label(PCPU_USAGE, "pCPU_usage");
        

        SimMonitor monitor = new SimMonitor(ConfigReader.readAppConfig(workingDir).get());
        easc = new EASC(workingDir, monitor, es);

        //Needs to execute several activities.
        for (Activity activity : easc.getAppConfig().getActivities()) {
            //Each activities include determining the working mode on several datacenter.
            for (DataCenterWorkingModes dataCenter : activity.getDataCenters()) {
                //Each datacenter has a list of work to do.
                if (mockWMM) {
                    easc.setWorkingModeManager(
                            new MockWMManager(easc.getEnergyService(), easc.getAppConfig())
                    );
                } else {
                    easc.setDefaultWorkingModeManager();
                    MockWorkingModeActuator wmActuator = new MockWorkingModeActuator();
                    for (WorkingMode wm : easc.getWorkingModeManager().getWorkingModes(activity.getName(), dataCenter.getDataCenterName())) {
                    	// Replace the default actuator (that executes an asynchronous command) with a mock one that
                    	// instantly changes the current working mode. This is required for getting correct metrics in
                    	// fast simulated iterations.
                    	wm.setActuator(wmActuator);
                    }
                }
            }
        }

        //Initialize the EASC true here is to use default working modes.
        easc.init(true);

        return easc;
    }

    public void mockWMM(boolean b) {
        mockWMM = b;
    }
    
    private static class MockWorkingModeActuator extends WorkingModeActuator {

		@Override
		public boolean activateWorkingMode(DataCenterWorkingModes dcwms, WorkingMode wm) {
			dcwms.setCurrentWorkingMode(wm);
			return true;
		}
    	
    }
    
}

