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

import javax.measure.quantity.Power;
import javax.measure.unit.SI;

import org.jscience.physics.amount.Amount;

import eu.dc4cities.easc.Application;
import eu.dc4cities.easc.monitoring.Monitor;
import eu.dc4cities.easc.resource.Resource;

public class SimMonitor extends Monitor {
	
	public SimMonitor(Application appConfig) {
		this.monitorActivityAndWorkingmode = new SimMonitorActivityWorkingMode(appConfig);
	}
	
	@Override
	public void init() {}

	@Override
	public void addResourceToMonitor(Resource res) {}

	@Override
	public Amount<Power> getTotalPower(String datacenter) {
		return Amount.valueOf(1000, SI.WATT);
	}

	@Override
	public int getRealtimeCumulativeBusinessObjective() {
		//Useful for Trento trial. To support producer/consumer in real trial.
		return 0;
	}

	@Override
	public int getActivityShareToPowerConsumption(String activityName, String datacenter) {
		//Useful for trial that would like to use a proportion mechanism to distribute total IT power between multiple activities
		return 0;
	}

}
