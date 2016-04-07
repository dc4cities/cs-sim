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

import eu.dc4cities.easc.Application;
import eu.dc4cities.easc.activity.Activity;
import eu.dc4cities.easc.activity.DataCenterWorkingModes;
import eu.dc4cities.easc.energyservice.EnergyService;
import eu.dc4cities.easc.workingmode.DefaultWorkingModeManager;

/**
 * Class to mock a WorkingModeManager functions on this working mode manager
 * now need to receive the dataCenter and the activity so it can locate
 * this activity. As example below there is a code to loop on the application
 * object to set particular properties of all (activity, dataCenter) pair.
 *
 * {@code
 * <pre>
 * //Foreach activity
 * for( Activity activity : application.getActivities() ){
 *     //Foreach datacenter
 *     for( DataCenter dataCenter : activity.getDataCenters()){
 *         //call function for DataCenter and Activity
 *         setDefaultWorkingMode(activity, dataCenter);
 *     }
 * }
 * </pre>
 * }
 */

public class MockWMManager extends DefaultWorkingModeManager {

    /**
     * Creates a new MockWMManager to mock a working mode manager object.
     * It is registered from the application that now has support for
     * multiple datacenters. Previous trials should use no datacenter
     * specific value here indicated by an empty String when calling
     * getDefaultWorkingMode. This manager is used to switch working modes
     * and also to come back to the first working mode.
     *
     * @param es an object to the energy service manager.
     * @param application the application itself.
     */
    public MockWMManager(EnergyService es,
                         Application application) {
        super(application);

        //Initialize all activities in all datacenters
        for(Activity activity : application.getActivities()){
            for (DataCenterWorkingModes dataCenter : activity.getDataCenters()) {
                dataCenter.setDefaultWorkingMode(dataCenter.getDefaultWorkingMode());
            }
        }
    }

}
